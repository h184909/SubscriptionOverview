package no.hvl.subscriptionapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import no.hvl.subscriptionapp.domain.BankConsent;
import no.hvl.subscriptionapp.openbanking.Account;
import no.hvl.subscriptionapp.openbanking.YapilyDtos;
import no.hvl.subscriptionapp.openbanking.YapilyHttp;
import no.hvl.subscriptionapp.repository.BankConsentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ImportJobService {

    public enum State {
        IDLE, RUNNING, DONE, FAILED
    }

    /**
     * Status-objekt som SuggestionsController/JSP bruker
     */
    public record ImportState(State state, String message, Instant updatedAt) {}

    private final YapilyHttp yapily;
    private final BankConsentRepository consentRepo;
    private final TransactionImportService importService;

    // En tråd er nok for MVP (hindrer at du starter mange imports samtidig)
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "import-job");
        t.setDaemon(true);
        return t;
    });

    // Status per bruker
    private final ConcurrentHashMap<String, AtomicReference<ImportState>> status = new ConcurrentHashMap<>();
    // Lås per bruker (så du ikke starter import to ganger)
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public ImportJobService(
            YapilyHttp yapily,
            BankConsentRepository consentRepo,
            TransactionImportService importService
    ) {
        this.yapily = yapily;
        this.consentRepo = consentRepo;
        this.importService = importService;
    }

    /**
     * Starter import hvis bruker ikke allerede har RUNNING.
     * @return true hvis import ble startet, false hvis den allerede kjører
     */
    public boolean startIfIdle(String userEmail) {
        Object lock = locks.computeIfAbsent(userEmail, k -> new Object());

        synchronized (lock) {
            ImportState current = getState(userEmail);
            if (current.state() == State.RUNNING) return false;

            setState(userEmail, State.RUNNING, "Henter kontoer fra bank...");
            executor.submit(() -> runJob(userEmail));
            return true;
        }
    }

    /**
     * Henter nåværende status (default IDLE)
     */
    public ImportState getState(String userEmail) {
        AtomicReference<ImportState> ref = status.get(userEmail);
        if (ref == null || ref.get() == null) {
            return new ImportState(State.IDLE, "", Instant.now());
        }
        return ref.get();
    }

    private void setState(String userEmail, State s, String msg) {
        status.computeIfAbsent(userEmail, k -> new AtomicReference<>())
                .set(new ImportState(s, safeMsg(msg), Instant.now()));
    }

    private void runJob(String userEmail) {
        try {
            BankConsent consent = consentRepo.findTopByUserEmailOrderByCreatedAtDesc(userEmail).orElse(null);
            if (consent == null) {
                setState(userEmail, State.FAILED, "Ingen banktilkobling. Koble til bank først.");
                return;
            }

            // 1) hent accounts
            var accountsRes = yapily.get(
                    "/accounts",
                    YapilyHttp.consentHeader(consent.getConsentToken()),
                    new TypeReference<YapilyDtos.ApiListResponse<Account>>() {}
            );

            List<Account> accounts = accountsRes.data();
            if (accounts == null || accounts.isEmpty()) {
                setState(userEmail, State.DONE, "Fant ingen kontoer å importere fra.");
                return;
            }

            int totalFetched = 0;
            int totalInserted = 0;

            // 2) importer transaksjoner for hver konto
            for (int i = 0; i < accounts.size(); i++) {
                Account a = accounts.get(i);
                if (a == null || a.getId() == null || a.getId().isBlank()) continue;

                String accountId = a.getId();
                setState(userEmail, State.RUNNING,
                        "Importerer transaksjoner (" + (i + 1) + "/" + accounts.size() + ")...");

                TransactionImportService.ImportResult r =
                        importService.importLatestForAccount(userEmail, accountId);

                totalFetched += r.fetched();
                totalInserted += r.inserted();
            }

            setState(userEmail, State.DONE,
                    "Ferdig. Fetched=" + totalFetched + ", nye lagret=" + totalInserted + ".");
        } catch (Exception e) {
            // Viktig: hold meldingen ASCII-ish for safety (men dette er ikke header nå, så det er mest for debug)
            setState(userEmail, State.FAILED, "Import feilet: " + safeMsg(e.getMessage()));
        }
    }

    /**
     * Sørger for at status-meldinger ikke inneholder "rare" tegn.
     * (Ikke strengt nødvendig nå, men greit for robustness.)
     */
    private static String safeMsg(String s) {
        if (s == null) return "";
        // fjern tegn som ofte skaper trøbbel i eldre encoding-situasjoner
        return s
                .replace("…", "...")
                .replace("⏳", "")
                .replace("\u00A0", " ")
                .trim();
    }
}

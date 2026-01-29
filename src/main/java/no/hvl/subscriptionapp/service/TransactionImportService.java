package no.hvl.subscriptionapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import no.hvl.subscriptionapp.domain.BankConsent;
import no.hvl.subscriptionapp.domain.BankTransaction;
import no.hvl.subscriptionapp.openbanking.Transaction;
import no.hvl.subscriptionapp.openbanking.YapilyDtos;
import no.hvl.subscriptionapp.openbanking.YapilyHttp;
import no.hvl.subscriptionapp.repository.BankConsentRepository;
import no.hvl.subscriptionapp.repository.BankTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class TransactionImportService {

    private final YapilyHttp yapily;
    private final BankConsentRepository consentRepo;
    private final BankTransactionRepository txRepo;
    private final ImportStatusService importStatus;

    public TransactionImportService(
            YapilyHttp yapily,
            BankConsentRepository consentRepo,
            BankTransactionRepository txRepo,
            ImportStatusService importStatus
    ) {
        this.yapily = yapily;
        this.consentRepo = consentRepo;
        this.txRepo = txRepo;
        this.importStatus = importStatus;
    }

    public record ImportResult(int fetched, int inserted) {}

    @Transactional
    public ImportResult importLatestForAccount(String userEmail, String accountId) {
        importStatus.set(userEmail, ImportStatusService.State.RUNNING, "Henter transaksjoner fra bank…");

        try {
            BankConsent consent = consentRepo.findTopByUserEmailOrderByCreatedAtDesc(userEmail).orElse(null);
            if (consent == null) {
                importStatus.set(userEmail, ImportStatusService.State.FAILED, "Ingen banktilkobling funnet.");
                throw new IllegalStateException("Ingen banktilkobling funnet. Koble til bank først.");
            }

            var res = yapily.get(
                    "/accounts/" + accountId + "/transactions",
                    YapilyHttp.consentHeader(consent.getConsentToken()),
                    new TypeReference<YapilyDtos.ApiListResponse<Transaction>>() {}
            );

            List<Transaction> fetched = res.data();
            if (fetched == null || fetched.isEmpty()) {
                importStatus.set(userEmail, ImportStatusService.State.DONE, "Ingen nye transaksjoner.");
                return new ImportResult(0, 0);
            }

            int inserted = 0;

            for (Transaction t : fetched) {
                if (t == null) continue;

                String txId = t.getId();
                if (txId == null || txId.isBlank()) continue;

                boolean exists = txRepo.existsByUserEmailAndTxId(userEmail, txId);
                if (exists) continue;

                OffsetDateTime parsedDate = tryParseOffsetDateTime(t.getDate());
                BigDecimal amount = toBigDecimal2(t.getAmount());

                BankTransaction entity = new BankTransaction(
                        userEmail,
                        accountId,
                        txId,
                        parsedDate,
                        t.getDate(),
                        t.getDescription(),
                        t.getReference(),
                        amount,
                        t.getCurrency()
                );

                txRepo.save(entity);
                inserted++;
            }

            importStatus.set(
                    userEmail,
                    ImportStatusService.State.DONE,
                    "Ferdig. Hentet " + fetched.size() + ", lagret " + inserted + "."
            );

            return new ImportResult(fetched.size(), inserted);

        } catch (Exception e) {
            importStatus.set(userEmail, ImportStatusService.State.FAILED, "Import feilet: " + safeMsg(e));
            throw e;
        }
    }

    private static String safeMsg(Exception e) {
        String m = e.getMessage();
        if (m == null) return e.getClass().getSimpleName();
        return m.length() > 180 ? m.substring(0, 180) + "…" : m;
    }

    private static OffsetDateTime tryParseOffsetDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return OffsetDateTime.parse(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static BigDecimal toBigDecimal2(Double d) {
        if (d == null) return null;
        return BigDecimal.valueOf(d).setScale(2, RoundingMode.HALF_UP);
    }
}

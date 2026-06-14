package no.hvl.subscriptionapp.web;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpSession;
import no.hvl.subscriptionapp.domain.BankConsent;
import no.hvl.subscriptionapp.domain.BankTransaction;
import no.hvl.subscriptionapp.openbanking.Institution;
import no.hvl.subscriptionapp.openbanking.YapilyDtos;
import no.hvl.subscriptionapp.openbanking.YapilyHttp;
import no.hvl.subscriptionapp.openbanking.YapilyProperties;
import no.hvl.subscriptionapp.repository.BankConsentRepository;
import no.hvl.subscriptionapp.repository.BankTransactionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Controller
public class OpenBankingController {

    private final YapilyHttp yapily;
    private final BankConsentRepository consentRepo;
    private final BankTransactionRepository txRepo;
    private final YapilyProperties props;

    public OpenBankingController(
            YapilyHttp yapily,
            BankConsentRepository consentRepo,
            BankTransactionRepository txRepo,
            YapilyProperties props
    ) {
        this.yapily = yapily;
        this.consentRepo = consentRepo;
        this.txRepo = txRepo;
        this.props = props;
    }

    /**
     * 1) Vis bankliste
     */
    @GetMapping("/openbanking/institutions")
    public String institutions(HttpSession session, Model model) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        YapilyDtos.ApiListResponse<Institution> res =
                yapily.get("/institutions", null, new TypeReference<>() {});

        model.addAttribute("institutions", res.data());
        model.addAttribute("env", props.getEnvironment());
        return "institutions";
    }

    /**
     * 2) Start connect mot valgt institutionId
     */
    /**
     * 2) Start connect mot valgt institutionId
     */
    @GetMapping("/openbanking/connect")
    public String connect(
            @RequestParam("institutionId") String institutionId,
            HttpSession session
    ) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        String callback = "https://subscriptionoverview.com/openbanking/callback";

        System.out.println("================================");
        System.out.println("YAPILY CALLBACK URL USED = " + callback);
        System.out.println("YAPILY INSTITUTION ID = " + institutionId);
        System.out.println("================================");

        var body = new YapilyDtos.CreateAccountAuthRequestBody(email, institutionId, callback);

        YapilyDtos.ApiSingleResponse<YapilyDtos.AccountAuthRequestData> res =
                yapily.postJson("/account-auth-requests", body, new TypeReference<>() {});

        String authUrl = res.data().authorisationUrl();

        System.out.println("================================");
        System.out.println("YAPILY AUTH URL = " + authUrl);
        System.out.println("================================");

        return "redirect:" + authUrl;
    }

    /**
     * 3) Callback fra Yapily
     */
    @GetMapping("/openbanking/callback")
    public String callback(
            @RequestParam(name = "consent", required = false) String consentToken,
            @RequestParam(name = "institution", required = false) String institutionId,
            HttpSession session,
            Model model
    ) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        if (consentToken == null || consentToken.isBlank()) {
            model.addAttribute("msg", "Mangler consentToken fra callback (kansellert eller feil).");
            return "openbanking_result";
        }

        consentRepo.save(new BankConsent(
                email,
                institutionId != null ? institutionId : "unknown",
                consentToken
        ));

        try {
            int imported = importTransactions(email, consentToken);
            session.setAttribute("flashMsg", "Bank koblet til. Importerte " + imported + " nye transaksjoner.");
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("flashMsg", "Bank koblet til, men import av transaksjoner feilet.");
        }

        return "redirect:/app/suggestions";
    }

    /**
     * Henter kontoer + transaksjoner fra Yapily og lagrer nye i DB.
     */
    private int importTransactions(String userEmail, String consentToken) {
        int imported = 0;

        // Konto-endepunktet parses løst som Map for å slippe å være avhengig av en egen Account DTO akkurat nå
        YapilyDtos.ApiListResponse<Map<String, Object>> accountsRes =
                yapily.get(
                        "/accounts",
                        YapilyHttp.consentHeader(consentToken),
                        new TypeReference<>() {}
                );

        List<Map<String, Object>> accounts = accountsRes.data();
        if (accounts == null || accounts.isEmpty()) {
            return 0;
        }

        for (Map<String, Object> acc : accounts) {
            String accountId = asString(acc.get("id"));
            if (accountId == null || accountId.isBlank()) {
                accountId = asString(acc.get("accountId"));
            }
            if (accountId == null || accountId.isBlank()) {
                continue;
            }

            YapilyDtos.ApiListResponse<YapilyDtos.Transaction> txRes =
                    yapily.get(
                            "/accounts/" + accountId + "/transactions",
                            YapilyHttp.consentHeader(consentToken),
                            new TypeReference<>() {}
                    );

            List<YapilyDtos.Transaction> txs = txRes.data();
            if (txs == null || txs.isEmpty()) {
                continue;
            }

            for (YapilyDtos.Transaction tx : txs) {
                String txId = tx.id();
                if (txId == null || txId.isBlank()) {
                    continue;
                }

                if (txRepo.existsByUserEmailAndAccountIdAndTxId(userEmail, accountId, txId)) {
                    continue;
                }

                OffsetDateTime txDate = parseYapilyDate(tx.date());
                BigDecimal amount = tx.amount() == null
                        ? null
                        : BigDecimal.valueOf(tx.amount());

                BankTransaction entity = new BankTransaction(
                        userEmail,
                        accountId,
                        txId,
                        txDate,
                        tx.date(),
                        tx.description(),
                        tx.reference(),
                        amount,
                        tx.currency()
                );

                txRepo.save(entity);
                imported++;
            }
        }

        return imported;
    }

    private OffsetDateTime parseYapilyDate(String raw) {
        if (raw == null || raw.isBlank()) return null;

        try {
            return OffsetDateTime.parse(raw);
        } catch (Exception ignored) {
        }

        try {
            return LocalDate.parse(raw).atStartOfDay().atOffset(ZoneOffset.UTC);
        } catch (Exception ignored) {
        }

        return null;
    }

    private String asString(Object x) {
        return x == null ? null : String.valueOf(x).trim();
    }
}
package no.hvl.subscriptionapp.openbanking.lunchflow;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import no.hvl.subscriptionapp.domain.BankTransaction;
import no.hvl.subscriptionapp.domain.LunchFlowConnection;
import no.hvl.subscriptionapp.repository.BankTransactionRepository;
import no.hvl.subscriptionapp.repository.LunchFlowConnectionRepository;
import no.hvl.subscriptionapp.web.LoginController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Controller
public class LunchFlowController {

    private static final String SESSION_FLASH = "flashMsg";

    private final LunchFlowProperties props;
    private final LunchFlowHttp lunchFlow;
    private final BankTransactionRepository txRepo;
    private final LunchFlowConnectionRepository connectionRepo;

    public LunchFlowController(
            LunchFlowProperties props,
            LunchFlowHttp lunchFlow,
            BankTransactionRepository txRepo,
            LunchFlowConnectionRepository connectionRepo
    ) {
        this.props = props;
        this.lunchFlow = lunchFlow;
        this.txRepo = txRepo;
        this.connectionRepo = connectionRepo;
    }

    @GetMapping("/lunchflow/connect")
    public String connect(HttpSession session) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        String state = UUID.randomUUID().toString();
        session.setAttribute("lunchflow_oauth_state", state);

        String url = UriComponentsBuilder
                .fromHttpUrl(props.getAuthorizeUrl())
                .queryParam("client_id", props.getClientId())
                .queryParam("redirect_uri", props.getRedirectUri())
                .queryParam("state", state)
                .queryParam("email", email)
                .encode()
                .build()
                .toUriString();

        return "redirect:" + url;
    }

    @PostMapping("/lunchflow/sync")
    public String sync(HttpSession session) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        try {
            LunchFlowConnection connection = connectionRepo
                    .findFirstByUserEmailOrderByUpdatedAtDesc(email)
                    .orElse(null);

            if (connection == null) {
                session.setAttribute(SESSION_FLASH, "No bank connection found. Connect your bank first.");
                return "redirect:/app";
            }

            ImportResult result = importTransactions(email, connection.getAccessToken());

            session.setAttribute(
                    SESSION_FLASH,
                    "Sync complete. Found " + result.accountsFound +
                            " account(s), " + result.transactionsFound +
                            " transaction(s), imported " + result.transactionsImported + " new."
            );

            return "redirect:/app/suggestions";

        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute(SESSION_FLASH, "Lunch Flow sync failed: " + e.getMessage());
            return "redirect:/app";
        }
    }

    @GetMapping("/lunchflow/callback")
    public String callback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "error", required = false) String error,
            HttpSession session
    ) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        if (error != null && !error.isBlank()) {
            session.setAttribute(SESSION_FLASH, "Lunch Flow-feil: " + error);
            return "redirect:/app/profile";
        }

        if (code == null || code.isBlank()) {
            session.setAttribute(SESSION_FLASH, "Lunch Flow callback manglet code.");
            return "redirect:/app/profile";
        }

        String expectedState = (String) session.getAttribute("lunchflow_oauth_state");
        if (expectedState == null || state == null || !expectedState.equals(state)) {
            session.setAttribute(SESSION_FLASH, "Lunch Flow state stemmer ikke.");
            return "redirect:/app/profile";
        }

        LunchFlowDtos.TokenRequest tokenRequest = new LunchFlowDtos.TokenRequest(
                "authorization_code",
                code,
                props.getRedirectUri(),
                props.getClientId(),
                props.getClientSecret(),
                null
        );

        try {
            LunchFlowDtos.TokenResponse token = lunchFlow.exchangeCode(tokenRequest);

            connectionRepo.findFirstByUserEmailOrderByUpdatedAtDesc(email)
                    .ifPresentOrElse(
                            existing -> {
                                existing.updateTokens(
                                        token.user_id(),
                                        token.access_token(),
                                        token.refresh_token()
                                );
                                connectionRepo.save(existing);
                            },
                            () -> connectionRepo.save(new LunchFlowConnection(
                                    email,
                                    token.user_id(),
                                    token.access_token(),
                                    token.refresh_token()
                            ))
                    );

            session.setAttribute("lunchflow_access_token", token.access_token());
            session.setAttribute("lunchflow_refresh_token", token.refresh_token());
            session.setAttribute("lunchflow_user_id", token.user_id());

            ImportResult result = importTransactions(email, token.access_token());

            session.setAttribute(
                    SESSION_FLASH,
                    "Lunch Flow koblet til. Fant " + result.accountsFound +
                            " konto(er), " + result.transactionsFound +
                            " transaksjon(er), importerte " + result.transactionsImported + " nye."
            );

            return "redirect:/app/suggestions";

        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute(SESSION_FLASH, "Lunch Flow-feil: " + e.getMessage());
            return "redirect:/app/profile";
        }
    }

    private ImportResult importTransactions(String userEmail, String accessToken) {
        LunchFlowDtos.AccountsResponse accountsRes = lunchFlow.getAccounts(accessToken);

        if (accountsRes == null || accountsRes.accounts() == null || accountsRes.accounts().isEmpty()) {
            return new ImportResult(0, 0, 0);
        }

        int accountsFound = accountsRes.accounts().size();
        int transactionsFound = 0;
        int transactionsImported = 0;

        for (LunchFlowDtos.Account account : accountsRes.accounts()) {
            if (account.id() == null || account.id().isBlank()) {
                continue;
            }

            LunchFlowDtos.TransactionsResponse txRes =
                    lunchFlow.getTransactions(accessToken, account.id());

            if (txRes == null || txRes.transactions() == null || txRes.transactions().isEmpty()) {
                continue;
            }

            transactionsFound += txRes.transactions().size();

            Set<String> seenInThisImport = new HashSet<>();

            for (LunchFlowDtos.Transaction tx : txRes.transactions()) {
                String txId = normalizeTxId(account.id(), tx);

                if (txId == null || txId.isBlank()) {
                    continue;
                }

                if (!seenInThisImport.add(txId)) {
                    continue;
                }

                if (txRepo.existsByUserEmailAndAccountIdAndTxId(userEmail, account.id(), txId)) {
                    continue;
                }

                BankTransaction entity = new BankTransaction(
                        userEmail,
                        account.id(),
                        txId,
                        parseLunchFlowDate(tx.date()),
                        tx.date(),
                        firstNonBlank(tx.description(), tx.merchant()),
                        tx.merchant(),
                        tx.amount(),
                        tx.currency()
                );

                txRepo.save(entity);
                transactionsImported++;
            }
        }

        return new ImportResult(accountsFound, transactionsFound, transactionsImported);
    }

    private String normalizeTxId(String accountId, LunchFlowDtos.Transaction tx) {
        if (tx == null) return null;

        if (tx.id() != null && !tx.id().isBlank() && !"0".equals(tx.id().trim())) {
            return tx.id().trim();
        }

        return accountId + "|" +
                safe(tx.date()) + "|" +
                safe(tx.amount() == null ? null : tx.amount().toPlainString()) + "|" +
                safe(tx.description()) + "|" +
                safe(tx.merchant());
    }

    private OffsetDateTime parseLunchFlowDate(String raw) {
        if (raw == null || raw.isBlank()) return null;

        try {
            return OffsetDateTime.parse(raw);
        } catch (Exception ignored) {}

        try {
            return LocalDate.parse(raw).atStartOfDay().atOffset(ZoneOffset.UTC);
        } catch (Exception ignored) {}

        return null;
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private record ImportResult(
            int accountsFound,
            int transactionsFound,
            int transactionsImported
    ) {}

    @PostConstruct
    public void init() {
        System.out.println("✅ LunchFlowController loaded");
    }
}
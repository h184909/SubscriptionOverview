package no.hvl.subscriptionapp.openbanking.lunchflow;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import no.hvl.subscriptionapp.domain.LunchFlowConnection;
import no.hvl.subscriptionapp.repository.LunchFlowConnectionRepository;
import no.hvl.subscriptionapp.service.LunchFlowSyncService;
import no.hvl.subscriptionapp.web.LoginController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Controller
public class LunchFlowController {

    private static final String SESSION_FLASH = "flashMsg";

    private final LunchFlowProperties props;
    private final LunchFlowHttp lunchFlow;
    private final LunchFlowConnectionRepository connectionRepo;
    private final LunchFlowSyncService syncService;

    public LunchFlowController(
            LunchFlowProperties props,
            LunchFlowHttp lunchFlow,
            LunchFlowConnectionRepository connectionRepo,
            LunchFlowSyncService syncService
    ) {
        this.props = props;
        this.lunchFlow = lunchFlow;
        this.connectionRepo = connectionRepo;
        this.syncService = syncService;
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
            LunchFlowSyncService.ImportResult result = syncService.syncNow(email);

            session.setAttribute(
                    SESSION_FLASH,
                    "Sync complete. Found " + result.accountsFound() +
                            " account(s), " + result.transactionsFound() +
                            " transaction(s), imported " + result.transactionsImported() + " new."
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

            LunchFlowSyncService.ImportResult result = syncService.syncNow(email);

            session.setAttribute(
                    SESSION_FLASH,
                    "Lunch Flow koblet til. Fant " + result.accountsFound() +
                            " konto(er), " + result.transactionsFound() +
                            " transaksjon(er), importerte " + result.transactionsImported() + " nye."
            );

            return "redirect:/app/suggestions";

        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute(SESSION_FLASH, "Lunch Flow-feil: " + e.getMessage());
            return "redirect:/app/profile";
        }
    }

    @PostConstruct
    public void init() {
        System.out.println("✅ LunchFlowController loaded");
    }
}
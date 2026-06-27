package no.hvl.subscriptionapp.openbanking.lunchflow;

import jakarta.servlet.http.HttpSession;
import no.hvl.subscriptionapp.web.LoginController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Controller
public class LunchFlowController {

    private static final String SESSION_FLASH = "flashMsg";

    private final LunchFlowProperties props;
    private final LunchFlowHttp lunchFlow;

    public LunchFlowController(LunchFlowProperties props, LunchFlowHttp lunchFlow) {
        this.props = props;
        this.lunchFlow = lunchFlow;
    }

    @GetMapping("/lunchflow/connect")
    public String connect(HttpSession session) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        String state = UUID.randomUUID().toString();
        session.setAttribute("lunchflow_oauth_state", state);

        String url = UriComponentsBuilder
                .fromHttpUrl(props.getBaseUrl() + "/oauth/authorize")
                .queryParam("client_id", props.getClientId())
                .queryParam("redirect_uri", props.getRedirectUri())
                .queryParam("state", state)
                .queryParam("email", email)
                .build()
                .toUriString();

        return "redirect:" + url;
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

            session.setAttribute("lunchflow_access_token", token.access_token());
            session.setAttribute("lunchflow_refresh_token", token.refresh_token());
            session.setAttribute("lunchflow_user_id", token.user_id());

            LunchFlowDtos.AccountsResponse accounts = lunchFlow.getAccounts(token.access_token());

            int count = accounts == null || accounts.accounts() == null
                    ? 0
                    : accounts.accounts().size();

            session.setAttribute(SESSION_FLASH, "Lunch Flow koblet til. Fant " + count + " konto(er).");
            return "redirect:/app/profile";

        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute(SESSION_FLASH, "Lunch Flow token/account-henting feilet.");
            return "redirect:/app/profile";
        }
    }
}
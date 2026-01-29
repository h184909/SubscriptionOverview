package no.hvl.subscriptionapp.web;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpSession;
import no.hvl.subscriptionapp.domain.BankConsent;
import no.hvl.subscriptionapp.openbanking.Institution;
import no.hvl.subscriptionapp.openbanking.YapilyDtos;
import no.hvl.subscriptionapp.openbanking.YapilyHttp;
import no.hvl.subscriptionapp.openbanking.YapilyProperties;
import no.hvl.subscriptionapp.repository.BankConsentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class OpenBankingController {

    private final YapilyHttp yapily;
    private final BankConsentRepository consentRepo;
    private final YapilyProperties props;

    public OpenBankingController(YapilyHttp yapily, BankConsentRepository consentRepo, YapilyProperties props) {
        this.yapily = yapily;
        this.consentRepo = consentRepo;
        this.props = props;
    }

    /**
     * 1) Vis bankliste (institutions)
     */
    @GetMapping("/openbanking/institutions")
    public String institutions(HttpSession session, Model model) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        // ✅ Bruk POJO-klasssen Institution (har getName/getId for JSP)
        YapilyDtos.ApiListResponse<Institution> res =
                yapily.get("/institutions", null, new TypeReference<>() {});

        List<Institution> list = res.data();
        model.addAttribute("institutions", list);
        model.addAttribute("env", props.getEnvironment());
        return "institutions";
    }

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

        String callback = props.getCallbackUrl();

        var body = new YapilyDtos.CreateAccountAuthRequestBody(email, institutionId, callback);

        YapilyDtos.ApiSingleResponse<YapilyDtos.AccountAuthRequestData> res =
                yapily.postJson("/account-auth-requests", body, new TypeReference<>() {});

        String authUrl = res.data().authorisationUrl();
        return "redirect:" + authUrl;
    }

    /**
     * Callback fra Yapily
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
            model.addAttribute("msg", "Mangler consentToken fra callback (kansellert/feil).");
            return "openbanking_result";
        }

        consentRepo.save(new BankConsent(email, institutionId != null ? institutionId : "unknown", consentToken));
        return "redirect:/app";
    }
}

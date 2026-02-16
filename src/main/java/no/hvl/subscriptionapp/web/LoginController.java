package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import no.hvl.subscriptionapp.domain.Person;
import no.hvl.subscriptionapp.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LoginController {

    public static final String SESSION_USER_EMAIL = "userEmail";

    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String showLogin(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(
            @Valid @ModelAttribute("loginForm") LoginForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "login";
        }

        return authService.authenticate(form.getEmail(), form.getPassord())
                .map(p -> {
                    session.setAttribute(SESSION_USER_EMAIL, p.getEmail());

                    // ✅ hvis bruker har foretrukket språk: set session-locale
                    String pl = p.getPreferredLanguage();
                    if (pl != null && !pl.isBlank()) {
                        var locale = "nb".equalsIgnoreCase(pl)
                                ? java.util.Locale.forLanguageTag("nb-NO")
                                : java.util.Locale.forLanguageTag("en-US");
                        session.setAttribute(
                                org.springframework.web.servlet.i18n.SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME,
                                locale
                        );
                    }

                    return "redirect:/app";
                })
                .orElseGet(() -> {
                    model.addAttribute("loginError", "Feil e-post eller passord");
                    return "login";
                });
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}

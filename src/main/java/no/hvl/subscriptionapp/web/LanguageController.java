package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import no.hvl.subscriptionapp.repository.PersonRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

@Controller
public class LanguageController {

    private final PersonRepository personRepository;

    public LanguageController(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @GetMapping("/lang")
    public String setLang(
            HttpServletRequest request,
            HttpSession session,
            @RequestParam("v") String v
    ) {
        String vv = (v == null ? "" : v.trim().toLowerCase());
        Locale locale = switch (vv) {
            case "nb", "no", "nor", "norsk" -> Locale.forLanguageTag("nb-NO");
            default -> Locale.forLanguageTag("en-US");
        };

        session.setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, locale);

        // ✅ Hvis bruker er innlogget: lagre preferanse i DB
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email != null && !email.isBlank()) {
            String lang = locale.getLanguage(); // "nb" eller "en"
            personRepository.findById(email).ifPresent(p -> {
                p.setPreferredLanguage(lang);
                personRepository.save(p);
            });
        }

        String ref = request.getHeader("Referer");
        if (ref == null || ref.isBlank()) return "redirect:/app";
        if (ref.contains("/WEB-INF/")) return "redirect:/app";
        return "redirect:" + ref;
    }
}
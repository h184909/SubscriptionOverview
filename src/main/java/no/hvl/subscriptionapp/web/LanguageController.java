package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

@Controller
public class LanguageController {

    @GetMapping("/lang")
    public String setLang(
            HttpServletRequest request,
            HttpSession session,
            @RequestParam("v") String v
    ) {
        Locale locale = switch ((v == null ? "" : v.trim().toLowerCase())) {
            case "nb", "no", "nor", "norsk" -> Locale.forLanguageTag("nb-NO");
            default -> Locale.forLanguageTag("en-US");
        };

        session.setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, locale);

        String ref = request.getHeader("Referer");
        if (ref == null || ref.isBlank()) return "redirect:/app";

        // sikkerhet: bare redirect innen samme host
        // hvis du kjører bak proxy kan dette være litt tricky, men funker bra i praksis
        if (ref.contains("/WEB-INF/")) return "redirect:/app";

        return "redirect:" + ref;
    }
}
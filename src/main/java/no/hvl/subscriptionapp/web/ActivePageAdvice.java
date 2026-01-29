package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Setter model-attributt: activePage
 * Brukes i JSP for å gi aktiv knapp annen farge i topbaren.
 *
 * Verdier:
 * - dashboard
 * - subscriptions
 * - suggestions
 * - importcsv
 * - institutions
 * - openbanking
 * - auth
 */
@ControllerAdvice
public class ActivePageAdvice {

    @ModelAttribute
    public void addActivePage(HttpServletRequest req, Model model) {
        String uri = req.getRequestURI();
        if (uri == null) {
            model.addAttribute("activePage", "");
            return;
        }

        // App-områder
        if (uri.equals("/app") || uri.equals("/app/")) {
            model.addAttribute("activePage", "dashboard");
            return;
        }
        if (uri.startsWith("/app/subscriptions")) {
            model.addAttribute("activePage", "subscriptions");
            return;
        }
        if (uri.startsWith("/app/suggestions")) {
            model.addAttribute("activePage", "suggestions");
            return;
        }
        if (uri.startsWith("/app/transactions/import-csv")) {
            model.addAttribute("activePage", "importcsv");
            return;
        }

        // OpenBanking
        if (uri.startsWith("/openbanking/institutions")) {
            model.addAttribute("activePage", "institutions");
            return;
        }
        if (uri.startsWith("/openbanking")) {
            model.addAttribute("activePage", "openbanking");
            return;
        }

        // Auth
        if (uri.startsWith("/login") || uri.startsWith("/auth") || uri.equals("/")) {
            model.addAttribute("activePage", "auth");
            return;
        }

        model.addAttribute("activePage", "");
    }
}

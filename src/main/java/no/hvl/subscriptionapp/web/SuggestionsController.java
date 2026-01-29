package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpSession;
import no.hvl.subscriptionapp.domain.Subscription;
import no.hvl.subscriptionapp.domain.SuggestionDecision;
import no.hvl.subscriptionapp.repository.SubscriptionRepository;
import no.hvl.subscriptionapp.repository.SuggestionDecisionRepository;
import no.hvl.subscriptionapp.service.SubscriptionDetectorService;
import no.hvl.subscriptionapp.service.SubscriptionSuggestion;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@Controller
public class SuggestionsController {

    private static final String SESSION_FLASH = "flashMsg";

    private final SubscriptionDetectorService detector;
    private final SubscriptionRepository subscriptionRepository;
    private final SuggestionDecisionRepository decisionRepository;

    public SuggestionsController(
            SubscriptionDetectorService detector,
            SubscriptionRepository subscriptionRepository,
            SuggestionDecisionRepository decisionRepository
    ) {
        this.detector = detector;
        this.subscriptionRepository = subscriptionRepository;
        this.decisionRepository = decisionRepository;
    }

    @GetMapping("/app/suggestions")
    public String list(HttpSession session, Model model) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        String flash = (String) session.getAttribute(SESSION_FLASH);
        if (flash != null) session.removeAttribute(SESSION_FLASH);
        model.addAttribute("flashMsg", flash);

        model.addAttribute("suggestions", detector.detect(email));
        return "suggestions";
    }

    @PostMapping("/app/suggestions/accept")
    public String accept(HttpSession session, @RequestParam("key") String key) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        Optional<SubscriptionSuggestion> found = detector.findOne(email, key);
        if (found.isEmpty()) {
            session.setAttribute(SESSION_FLASH, "Fant ikke forslaget (kan være filtrert bort).");
            return "redirect:/app/suggestions";
        }

        SubscriptionSuggestion s = found.get();

        // lag abonnement
        LocalDate next = s.getNextExpectedDate();
        if (next != null && next.isBefore(LocalDate.now().minusDays(7))) {
            // hvis gammel “neste”, sett til frem i tid (best effort)
            next = LocalDate.now().plusMonths(1);
        }

        Subscription sub = new Subscription(
                email,
                s.getName(),
                s.getAmount(),
                s.getCurrency(),
                s.getInterval(),
                next,
                null
        );
        subscriptionRepository.save(sub);

        // markér som accepted (så den ikke vises igjen)
        decisionRepository.findByUserEmailAndSuggestionKey(email, key)
                .orElseGet(() -> decisionRepository.save(new SuggestionDecision(email, key, "ACCEPTED")));

        session.setAttribute(SESSION_FLASH, "Abonnement lagt til: " + s.getName());
        return "redirect:/app/subscriptions";
    }

    @PostMapping("/app/suggestions/reject")
    public String reject(HttpSession session, @RequestParam("key") String key) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        decisionRepository.findByUserEmailAndSuggestionKey(email, key)
                .orElseGet(() -> decisionRepository.save(new SuggestionDecision(email, key, "REJECTED")));

        session.setAttribute(SESSION_FLASH, "Forslag avvist.");
        return "redirect:/app/suggestions";
    }
}

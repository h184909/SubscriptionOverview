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
import java.util.*;

@Controller
public class SuggestionsController {

    private static final String SESSION_FLASH = "flashMsg";
    private static final String SESSION_HIDDEN_KEYS = "hiddenSuggestionKeys"; // <-- NYTT

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

        // Hent forslag fra detektor
        List<SubscriptionSuggestion> suggestions = new ArrayList<>(detector.detect(email));

        // ✅ NYTT: skjul "avviste" midlertidig (session-basert)
        Set<String> hidden = getHiddenKeys(session);
        if (!hidden.isEmpty()) {
            suggestions.removeIf(s -> hidden.contains(s.getKey()));
        }

        model.addAttribute("suggestions", suggestions);
        model.addAttribute("hiddenCount", hidden.size()); // kan brukes i JSP om du vil
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

        // best effort: hvis next er gammel, flytt frem
        LocalDate next = s.getNextExpectedDate();
        if (next != null && next.isBefore(LocalDate.now().minusDays(7))) {
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

        // ✅ Accepted skal være permanent (skjules i fremtiden)
        decisionRepository.findByUserEmailAndSuggestionKey(email, key)
                .orElseGet(() -> decisionRepository.save(new SuggestionDecision(email, key, "ACCEPTED")));

        // Fjern også fra session-hidden i tilfelle den ligger der
        getHiddenKeys(session).remove(key);

        session.setAttribute(SESSION_FLASH, "Abonnement lagt til: " + s.getName());
        return "redirect:/app/subscriptions";
    }

    @PostMapping("/app/suggestions/reject")
    public String reject(HttpSession session, @RequestParam("key") String key) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        // ✅ NYTT: IKKE lagre REJECTED i DB (da kan du teste CSV igjen)
        // Hvis den finnes fra før (fra tidligere versjon), slett den:
        decisionRepository.findByUserEmailAndSuggestionKey(email, key)
                .ifPresent(decisionRepository::delete);

        // Skjul midlertidig i session så UI fortsatt føles riktig
        getHiddenKeys(session).add(key);

        session.setAttribute(SESSION_FLASH, "Forslag avvist (midlertidig skjult).");
        return "redirect:/app/suggestions";
    }

    // ✅ Valgfritt, men supernyttig for testing:
    // En knapp du kan legge i JSP for å få tilbake alt du har avvist i session.
    @PostMapping("/app/suggestions/reset-hidden")
    public String resetHidden(HttpSession session) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        getHiddenKeys(session).clear();
        session.setAttribute(SESSION_FLASH, "Midlertidig skjulte forslag er tilbakestilt.");
        return "redirect:/app/suggestions";
    }

    @SuppressWarnings("unchecked")
    private Set<String> getHiddenKeys(HttpSession session) {
        Object o = session.getAttribute(SESSION_HIDDEN_KEYS);
        if (o instanceof Set) return (Set<String>) o;

        Set<String> created = new HashSet<>();
        session.setAttribute(SESSION_HIDDEN_KEYS, created);
        return created;
    }
}
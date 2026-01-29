package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpSession;
import no.hvl.subscriptionapp.domain.Subscription;
import no.hvl.subscriptionapp.repository.SubscriptionRepository;
import no.hvl.subscriptionapp.service.SubscriptionDetectorService;
import no.hvl.subscriptionapp.service.SubscriptionSuggestion;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class SuggestionsController {

    private static final String SESSION_FLASH = "flashMsg";
    private static final String SESSION_REJECTED_KEYS = "rejectedSuggestionKeys";

    private final SubscriptionDetectorService detector;
    private final SubscriptionRepository subscriptionRepository;

    public SuggestionsController(SubscriptionDetectorService detector, SubscriptionRepository subscriptionRepository) {
        this.detector = detector;
        this.subscriptionRepository = subscriptionRepository;
    }

    @GetMapping("/app/suggestions")
    public String list(HttpSession session, Model model) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        // flash
        String flash = (String) session.getAttribute(SESSION_FLASH);
        if (flash != null) session.removeAttribute(SESSION_FLASH);
        model.addAttribute("flashMsg", flash);

        // hent avviste keys fra session
        Set<String> rejected = getRejectedKeys(session);

        // eksisterende subs (for filtrering)
        List<Subscription> subs = subscriptionRepository.findByUserEmailOrderByCreatedAtDesc(email);
        Set<String> existingNames = subs.stream()
                .map(s -> safe(s.getName()))
                .collect(Collectors.toSet());

        // forslag
        List<SubscriptionSuggestion> suggestions = new ArrayList<>(detector.detect(email));

        // 1) fjern avviste
        suggestions.removeIf(s -> rejected.contains(s.getKey()));

        // 2) fjern hvis allerede finnes som abonnement (samme navn)
        suggestions.removeIf(s -> existingNames.contains(safe(s.getName())));

        // 3) fjern “utdatert”: hvis neste forventede dato er langt tilbake
        // (juster terskel om du vil: 45 dager pleier funke greit)
        LocalDate cutoff = LocalDate.now().minusDays(45);
        suggestions.removeIf(s -> s.getNextExpectedDate() != null && s.getNextExpectedDate().isBefore(cutoff));

        model.addAttribute("suggestions", suggestions);
        return "suggestions";
    }

    @PostMapping("/app/suggestions/accept")
    public String accept(HttpSession session, @RequestParam("key") String key) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        // Finn forslaget “nå” (samme key)
        SubscriptionSuggestion sug = detector.detect(email).stream()
                .filter(s -> Objects.equals(s.getKey(), key))
                .findFirst()
                .orElse(null);

        if (sug == null) {
            session.setAttribute(SESSION_FLASH, "Fant ikke forslaget (kanskje det allerede er filtrert bort).");
            return "redirect:/app/suggestions";
        }

        // Opprett abonnement
        LocalDate next = sug.getNextExpectedDate(); // kan være null
        Subscription sub = new Subscription(
                email,
                sug.getName(),
                sug.getAmount(),
                sug.getCurrency(),
                sug.getInterval(),
                next,
                null
        );

        subscriptionRepository.save(sub);

        session.setAttribute(SESSION_FLASH, "Godtatt: " + sug.getName() + " ble lagt til i abonnement.");
        return "redirect:/app/subscriptions";
    }

    @PostMapping("/app/suggestions/reject")
    public String reject(HttpSession session, @RequestParam("key") String key) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        Set<String> rejected = getRejectedKeys(session);
        rejected.add(key);
        session.setAttribute(SESSION_REJECTED_KEYS, rejected);

        session.setAttribute(SESSION_FLASH, "Avvist forslag.");
        return "redirect:/app/suggestions";
    }

    // ---------- helpers ----------

    @SuppressWarnings("unchecked")
    private Set<String> getRejectedKeys(HttpSession session) {
        Object o = session.getAttribute(SESSION_REJECTED_KEYS);
        if (o instanceof Set) return (Set<String>) o;
        return new HashSet<>();
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}

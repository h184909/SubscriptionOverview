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
    private static final String SESSION_HIDDEN_KEYS = "hiddenSuggestionKeys";

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

        List<SubscriptionSuggestion> suggestions = new ArrayList<>(detector.detect(email));

        // skjul "avviste" midlertidig (session)
        Set<String> hidden = getHiddenKeys(session);
        if (!hidden.isEmpty()) suggestions.removeIf(s -> hidden.contains(s.getKey()));

        model.addAttribute("suggestions", suggestions);
        model.addAttribute("hiddenCount", hidden.size());

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

        addSubscriptionFromSuggestion(email, found.get());

        // Accepted = permanent blokk
        decisionRepository.findByUserEmailAndSuggestionKey(email, key)
                .orElseGet(() -> decisionRepository.save(new SuggestionDecision(email, key, "ACCEPTED")));

        // fjern fra session-hidden hvis den ligger der
        getHiddenKeys(session).remove(key);

        session.setAttribute(SESSION_FLASH, "Abonnement lagt til: " + found.get().getName());
        return "redirect:/app/subscriptions";
    }

    @PostMapping("/app/suggestions/reject")
    public String reject(HttpSession session, @RequestParam("key") String key) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        // Ikke permanent: slett evt tidligere decision
        decisionRepository.findByUserEmailAndSuggestionKey(email, key)
                .ifPresent(decisionRepository::delete);

        // skjul midlertidig i session
        getHiddenKeys(session).add(key);

        session.setAttribute(SESSION_FLASH, "Forslag avvist (midlertidig skjult).");
        return "redirect:/app/suggestions";
    }

    @PostMapping("/app/suggestions/accept-bulk")
    public String acceptBulk(HttpSession session, @RequestParam(value = "keys", required = false) List<String> keys) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        if (keys == null || keys.isEmpty()) {
            session.setAttribute(SESSION_FLASH, "Velg minst ett forslag først.");
            return "redirect:/app/suggestions";
        }

        int added = 0;
        int missing = 0;

        for (String key : keys) {
            Optional<SubscriptionSuggestion> found = detector.findOne(email, key);
            if (found.isEmpty()) {
                missing++;
                continue;
            }

            addSubscriptionFromSuggestion(email, found.get());
            added++;

            decisionRepository.findByUserEmailAndSuggestionKey(email, key)
                    .orElseGet(() -> decisionRepository.save(new SuggestionDecision(email, key, "ACCEPTED")));

            getHiddenKeys(session).remove(key);
        }

        if (missing > 0) {
            session.setAttribute(SESSION_FLASH, "Godkjent " + added + " forslag. (" + missing + " ble ikke funnet)");
        } else {
            session.setAttribute(SESSION_FLASH, "Godkjent " + added + " forslag.");
        }

        return "redirect:/app/subscriptions";
    }

    @PostMapping("/app/suggestions/reject-bulk")
    public String rejectBulk(HttpSession session, @RequestParam(value = "keys", required = false) List<String> keys) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        if (keys == null || keys.isEmpty()) {
            session.setAttribute(SESSION_FLASH, "Velg minst ett forslag først.");
            return "redirect:/app/suggestions";
        }

        int rejected = 0;

        for (String key : keys) {
            decisionRepository.findByUserEmailAndSuggestionKey(email, key)
                    .ifPresent(decisionRepository::delete);

            getHiddenKeys(session).add(key);
            rejected++;
        }

        session.setAttribute(SESSION_FLASH, "Avvist " + rejected + " forslag (midlertidig skjult).");
        return "redirect:/app/suggestions";
    }

    @PostMapping("/app/suggestions/reset-hidden")
    public String resetHidden(HttpSession session) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        getHiddenKeys(session).clear();
        session.setAttribute(SESSION_FLASH, "Midlertidig skjulte forslag er tilbakestilt.");
        return "redirect:/app/suggestions";
    }

    // ✅ Viktig: nå lagrer vi providerKey og cancelUrl dersom entity støtter det
    private void addSubscriptionFromSuggestion(String email, SubscriptionSuggestion s) {
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

        // disse setterene må finnes på Subscription-entity:
        // setProviderKey(String) og setCancelUrl(String)
        try {
            if (s.getProviderKey() != null && !s.getProviderKey().isBlank()) {
                sub.setProviderKey(s.getProviderKey());
            }
            if (s.getCancelUrl() != null && !s.getCancelUrl().isBlank()) {
                sub.setCancelUrl(s.getCancelUrl());
            }
        } catch (Exception ignored) {
            // hvis entity ikke har feltene enda, krasjer vi ikke
        }

        subscriptionRepository.save(sub);
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
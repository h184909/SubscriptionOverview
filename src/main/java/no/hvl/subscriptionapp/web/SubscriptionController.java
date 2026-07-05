package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import no.hvl.subscriptionapp.domain.Subscription;
import no.hvl.subscriptionapp.domain.SuggestionDecision;
import no.hvl.subscriptionapp.repository.SubscriptionRepository;
import no.hvl.subscriptionapp.repository.SuggestionDecisionRepository;
import no.hvl.subscriptionapp.service.KnownMerchants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Controller
public class SubscriptionController {

    private final SubscriptionRepository subscriptionRepository;
    private final SuggestionDecisionRepository decisionRepository;

    public SubscriptionController(
            SubscriptionRepository subscriptionRepository,
            SuggestionDecisionRepository decisionRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.decisionRepository = decisionRepository;
    }

    @GetMapping("/app/subscriptions")
    public String list(HttpSession session, Model model) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        List<Subscription> subs = subscriptionRepository.findByUserEmailOrderByCreatedAtDesc(email);

        LocalDate today = LocalDate.now();
        boolean changed = false;

        for (Subscription s : subs) {
            if (!s.isActive()) continue;
            if (s.getNextChargeDate() == null) continue;

            LocalDate next = s.getNextChargeDate();
            if (!next.isAfter(today)) {
                LocalDate rolled = rollForward(next, s.getInterval(), today);
                if (!rolled.equals(next)) {
                    s.setNextChargeDate(rolled);
                    changed = true;
                }
            }
        }

        if (changed) subscriptionRepository.saveAll(subs);

        model.addAttribute("subs", subs);

        Map<String, String> cancelLinks = new HashMap<>();
        for (Subscription s : subs) {
            String key = (s.getProviderKey() != null && !s.getProviderKey().isBlank())
                    ? s.getProviderKey()
                    : s.getName();

            var m = KnownMerchants.match(key, s.getName());
            m.ifPresent(match -> cancelLinks.put(s.getId().toString(), match.cancelUrl()));
        }
        model.addAttribute("cancelLinks", cancelLinks);

        return "subscriptions";
    }

    @PostMapping("/app/subscriptions/category")
    public String updateCategory(
            HttpSession session,
            @RequestParam("id") String id,
            @RequestParam(value = "category", required = false) String category
    ) {
        Subscription sub = findOwnedSubscription(session, id);
        if (sub == null) return "redirect:/app/subscriptions";

        String cleaned = normalizeCategory(category);
        sub.setCategory(cleaned);
        subscriptionRepository.save(sub);

        return "redirect:/app/subscriptions";
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) return null;

        return switch (category.trim()) {
            case "Entertainment" -> "Entertainment";
            case "Telecom" -> "Telecom";
            case "Utilities" -> "Utilities";
            case "Health & Fitness" -> "Health & Fitness";
            case "News" -> "News";
            case "Shopping & Food" -> "Shopping & Food";
            case "Other" -> "Other";
            default -> null;
        };
    }

    private LocalDate rollForward(LocalDate next, String interval, LocalDate today) {
        LocalDate d = next;
        int guard = 0;
        while (!d.isAfter(today) && guard++ < 500) {
            d = switch (interval) {
                case "WEEKLY" -> d.plusWeeks(1);
                case "MONTHLY" -> d.plusMonths(1);
                case "QUARTERLY" -> d.plusMonths(3);
                case "YEARLY" -> d.plusYears(1);
                default -> d.plusMonths(1);
            };
        }
        return d;
    }

    @GetMapping("/app/subscriptions/new")
    public String showNew(HttpSession session, Model model) {
        if (session.getAttribute(LoginController.SESSION_USER_EMAIL) == null) {
            return "redirect:/login";
        }
        model.addAttribute("form", new SubscriptionForm());
        return "subscription_new";
    }

    @PostMapping("/app/subscriptions/new")
    public String doNew(
            HttpSession session,
            @Valid @ModelAttribute("form") SubscriptionForm form,
            BindingResult binding
    ) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        if (binding.hasErrors()) return "subscription_new";

        LocalDate nextDate = null;
        if (form.getNextChargeDate() != null && !form.getNextChargeDate().isBlank()) {
            try {
                nextDate = LocalDate.parse(form.getNextChargeDate());
            } catch (Exception e) {
                binding.rejectValue("nextChargeDate", "date.invalid", "Ugyldig datoformat");
                return "subscription_new";
            }
        }

        Subscription sub = new Subscription(
                email,
                form.getName().trim(),
                form.getAmount(),
                form.getCurrency(),
                form.getInterval(),
                nextDate,
                form.getBillingEmail()
        );

        subscriptionRepository.save(sub);
        return "redirect:/app/subscriptions";
    }

    @PostMapping("/app/subscriptions/rename")
    public String rename(HttpSession session,
                         @RequestParam("id") String id,
                         @RequestParam("name") String name) {

        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        if (name == null || name.trim().isBlank()) return "redirect:/app/subscriptions";
        String newName = name.trim();
        if (newName.length() > 80) newName = newName.substring(0, 80);

        Subscription sub = findOwnedSubscription(session, id);
        if (sub == null) return "redirect:/app/subscriptions";

        sub.setName(newName);
        subscriptionRepository.save(sub);

        return "redirect:/app/subscriptions";
    }

    @PostMapping("/app/subscriptions/cancel")
    public String cancel(HttpSession session, @RequestParam("id") String id) {
        Subscription sub = findOwnedSubscription(session, id);
        if (sub == null) return "redirect:/app/subscriptions";

        sub.setActive(false);
        subscriptionRepository.save(sub);
        return "redirect:/app/subscriptions";
    }

    @PostMapping("/app/subscriptions/reactivate")
    public String reactivate(HttpSession session, @RequestParam("id") String id) {
        Subscription sub = findOwnedSubscription(session, id);
        if (sub == null) return "redirect:/app/subscriptions";

        sub.setActive(true);
        subscriptionRepository.save(sub);
        return "redirect:/app/subscriptions";
    }

    @PostMapping("/app/subscriptions/delete")
    public String delete(HttpSession session, @RequestParam("id") String id) {
        Subscription sub = findOwnedSubscription(session, id);
        if (sub == null) return "redirect:/app/subscriptions";

        String email = sub.getUserEmail();

        String providerKey = norm(firstNonBlank(sub.getProviderKey(), sub.getName()));
        if (!providerKey.isBlank()) {
            String prefix = providerKey + "|";

            List<SuggestionDecision> decisions = decisionRepository.findByUserEmail(email);
            for (SuggestionDecision d : decisions) {
                String sk = d.getSuggestionKey();
                if (sk != null && sk.startsWith(prefix)) {
                    decisionRepository.delete(d);
                }
            }
        }

        subscriptionRepository.delete(sub);
        return "redirect:/app/subscriptions";
    }

    private Subscription findOwnedSubscription(HttpSession session, String id) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return null;

        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (Exception e) {
            return null;
        }

        Subscription sub = subscriptionRepository.findById(uuid).orElse(null);
        if (sub == null) return null;

        if (!email.equals(sub.getUserEmail())) return null;

        return sub;
    }

    private static String firstNonBlank(String... xs) {
        for (String x : xs) if (x != null && !x.isBlank()) return x;
        return "";
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
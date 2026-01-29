package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import no.hvl.subscriptionapp.domain.Subscription;
import no.hvl.subscriptionapp.repository.SubscriptionRepository;
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

    public SubscriptionController(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @GetMapping("/app/subscriptions")
    public String list(HttpSession session, Model model) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        List<Subscription> subs = subscriptionRepository.findByUserEmailOrderByCreatedAtDesc(email);
        model.addAttribute("subs", subs);

        // ✅ cancel-links pr subscription (uten DB-endring)
        // ✅ bruk UUID som nøkkel (matcher JSP: cancelLinks[s.id])
        Map<UUID, String> cancelLinks = new HashMap<>();
        for (Subscription s : subs) {
            var m = KnownMerchants.match(s.getName(), s.getName());
            m.ifPresent(match -> cancelLinks.put(s.getId(), match.cancelUrl()));
        }
        model.addAttribute("cancelLinks", cancelLinks);

        return "subscriptions";
    }

    @GetMapping("/app/subscriptions/new")
    public String showNew(HttpSession session, Model model) {
        if (session.getAttribute(LoginController.SESSION_USER_EMAIL) == null) {
            return "redirect:/login";
        }
        model.addAttribute("activePage", "subscriptions");
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
}

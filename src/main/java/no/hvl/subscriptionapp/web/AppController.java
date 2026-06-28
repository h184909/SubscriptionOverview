package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpSession;
import no.hvl.subscriptionapp.domain.Subscription;
import no.hvl.subscriptionapp.repository.LunchFlowConnectionRepository;
import no.hvl.subscriptionapp.repository.SubscriptionRepository;
import no.hvl.subscriptionapp.service.ExchangeRateService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Controller
public class AppController {

    private final SubscriptionRepository subscriptionRepo;
    private final LunchFlowConnectionRepository lunchFlowConnectionRepo;
    private final ExchangeRateService fx;

    public AppController(
            SubscriptionRepository subscriptionRepo,
            LunchFlowConnectionRepository lunchFlowConnectionRepo,
            ExchangeRateService fx
    ) {
        this.subscriptionRepo = subscriptionRepo;
        this.lunchFlowConnectionRepo = lunchFlowConnectionRepo;
        this.fx = fx;
    }

    @GetMapping("/app")
    public String dashboard(HttpSession session, Model model) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        model.addAttribute("email", email);

        boolean bankConnected = lunchFlowConnectionRepo.existsByUserEmail(email);
        model.addAttribute("bankConnected", bankConnected);

        List<Subscription> allSubs = subscriptionRepo.findByUserEmailOrderByCreatedAtDesc(email);

        LocalDate today = LocalDate.now();
        boolean changed = false;

        for (Subscription s : allSubs) {
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

        if (changed) subscriptionRepo.saveAll(allSubs);

        List<Subscription> activeSubs = allSubs.stream()
                .filter(Subscription::isActive)
                .toList();

        model.addAttribute("subs", activeSubs);

        LocalDate end = today.plusDays(7);
        List<Subscription> dueSoon = activeSubs.stream()
                .filter(s -> s.getNextChargeDate() != null)
                .filter(s -> !s.getNextChargeDate().isBefore(today))
                .filter(s -> !s.getNextChargeDate().isAfter(end))
                .sorted(Comparator.comparing(Subscription::getNextChargeDate))
                .toList();

        model.addAttribute("dueSoon", dueSoon);

        BigDecimal totalMonthlyNok = BigDecimal.ZERO;
        for (Subscription s : activeSubs) {
            BigDecimal monthly = s.getMonthlyCost();
            if (monthly == null) continue;

            BigDecimal inNok = fx.convertToNok(monthly, s.getCurrency());
            if (inNok != null) totalMonthlyNok = totalMonthlyNok.add(inNok);
        }

        model.addAttribute("totalMonthlyNok", totalMonthlyNok);
        model.addAttribute("showDevLinks", false);

        return "app";
    }

    private LocalDate rollForward(LocalDate next, String interval, LocalDate today) {
        LocalDate d = next;
        while (!d.isAfter(today)) {
            d = switch (interval) {
                case "WEEKLY" -> d.plusWeeks(1);
                case "MONTHLY" -> d.plusMonths(1);
                case "YEARLY" -> d.plusYears(1);
                default -> d.plusMonths(1);
            };
        }
        return d;
    }
}
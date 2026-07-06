package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpSession;
import no.hvl.subscriptionapp.domain.LunchFlowConnection;
import no.hvl.subscriptionapp.domain.Subscription;
import no.hvl.subscriptionapp.repository.LunchFlowConnectionRepository;
import no.hvl.subscriptionapp.repository.SubscriptionRepository;
import no.hvl.subscriptionapp.service.ExchangeRateService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

        LunchFlowConnection connection =
                lunchFlowConnectionRepo.findFirstByUserEmailOrderByUpdatedAtDesc(email).orElse(null);

        model.addAttribute("bankConnected", connection != null);
        model.addAttribute("bankInstitutionName", connection != null ? connection.getInstitutionName() : null);
        model.addAttribute("bankAccountCount", connection != null ? connection.getAccountCount() : null);
        model.addAttribute("bankAccountNames", connection != null ? connection.getAccountNames() : null);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        ZoneId userZone = ZoneId.of("Europe/Oslo");

        model.addAttribute(
                "bankLastSynced",
                connection != null && connection.getLastSyncedAt() != null
                        ? connection.getLastSyncedAt().atZoneSameInstant(userZone).format(dateTimeFormatter)
                        : null
        );

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
        model.addAttribute("activeSubscriptionCount", activeSubs.size());

        LocalDate end = today.plusDays(7);
        List<Subscription> dueSoon = activeSubs.stream()
                .filter(s -> s.getNextChargeDate() != null)
                .filter(s -> !s.getNextChargeDate().isBefore(today))
                .filter(s -> !s.getNextChargeDate().isAfter(end))
                .sorted(Comparator.comparing(Subscription::getNextChargeDate))
                .toList();

        model.addAttribute("dueSoon", dueSoon);
        model.addAttribute("dueSoonCount", dueSoon.size());

        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        List<Subscription> dueThisMonth = activeSubs.stream()
                .filter(s -> s.getNextChargeDate() != null)
                .filter(s -> !s.getNextChargeDate().isBefore(today))
                .filter(s -> !s.getNextChargeDate().isAfter(monthEnd))
                .sorted(Comparator.comparing(Subscription::getNextChargeDate))
                .toList();

        model.addAttribute("dueThisMonth", dueThisMonth);

        BigDecimal totalMonthlyNok = BigDecimal.ZERO;

        Map<String, BigDecimal> categoryTotals = new HashMap<>();
        Map<UUID, BigDecimal> monthlyNokBySubId = new HashMap<>();

        for (Subscription s : activeSubs) {
            BigDecimal monthly = s.getMonthlyCost();
            if (monthly == null) continue;

            BigDecimal inNok = fx.convertToNok(monthly, s.getCurrency());
            if (inNok == null) continue;

            inNok = inNok.setScale(2, RoundingMode.HALF_UP);
            totalMonthlyNok = totalMonthlyNok.add(inNok);
            monthlyNokBySubId.put(s.getId(), inNok);

            String category = cleanCategory(s.getCategory());
            categoryTotals.put(category, categoryTotals.getOrDefault(category, BigDecimal.ZERO).add(inNok));
        }

        BigDecimal finalTotalMonthlyNok = totalMonthlyNok.setScale(2, RoundingMode.HALF_UP);
        BigDecimal yearlyTotalNok = finalTotalMonthlyNok.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP);

        model.addAttribute("totalMonthlyNok", finalTotalMonthlyNok);
        model.addAttribute("yearlyTotalNok", yearlyTotalNok);
        model.addAttribute("monthlyNokBySubId", monthlyNokBySubId);

        List<CategoryInsight> categoryInsights = categoryTotals.entrySet().stream()
                .map(e -> {
                    int p = percent(e.getValue(), finalTotalMonthlyNok);
                    return new CategoryInsight(
                            e.getKey(),
                            e.getValue().setScale(2, RoundingMode.HALF_UP),
                            p,
                            barWidth(p)
                    );
                })
                .sorted(Comparator.comparing(CategoryInsight::getAmount).reversed())
                .toList();

        model.addAttribute("categoryInsights", categoryInsights);

        Optional<CategoryInsight> largestCategory = categoryInsights.stream().findFirst();
        model.addAttribute("largestCategory", largestCategory.orElse(null));

        List<Subscription> topSubscriptions = activeSubs.stream()
                .sorted((a, b) -> monthlyNokBySubId.getOrDefault(b.getId(), BigDecimal.ZERO)
                        .compareTo(monthlyNokBySubId.getOrDefault(a.getId(), BigDecimal.ZERO)))
                .limit(5)
                .toList();

        model.addAttribute("topSubscriptions", topSubscriptions);

        Subscription largestSubscription = topSubscriptions.isEmpty() ? null : topSubscriptions.get(0);
        model.addAttribute("largestSubscription", largestSubscription);
        model.addAttribute(
                "largestSubscriptionMonthly",
                largestSubscription == null ? null : monthlyNokBySubId.get(largestSubscription.getId())
        );

        model.addAttribute("smartInsight", buildSmartInsight(
                activeSubs.size(),
                finalTotalMonthlyNok,
                largestCategory.orElse(null),
                largestSubscription,
                largestSubscription == null ? null : monthlyNokBySubId.get(largestSubscription.getId())
        ));

        model.addAttribute("showDevLinks", false);

        return "app";
    }

    private String cleanCategory(String category) {
        if (category == null || category.isBlank() || "Other".equalsIgnoreCase(category.trim())) {
            return "Uncategorized";
        }
        return category.trim();
    }

    private int percent(BigDecimal part, BigDecimal total) {
        if (part == null || total == null || total.compareTo(BigDecimal.ZERO) <= 0) return 0;
        return part.multiply(BigDecimal.valueOf(100))
                .divide(total, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private int barWidth(int percent) {
        if (percent <= 0) return 4;
        return Math.max(4, Math.min(100, percent));
    }

    private String buildSmartInsight(
            int count,
            BigDecimal totalMonthly,
            CategoryInsight largestCategory,
            Subscription largestSubscription,
            BigDecimal largestSubscriptionMonthly
    ) {
        if (count == 0) {
            return "Connect your bank or add subscriptions manually to start getting insights.";
        }

        if (largestCategory != null && largestCategory.getPercent() >= 50) {
            return largestCategory.getCategory() + " makes up " +
                    largestCategory.getPercent() +
                    "% of your monthly subscription spending.";
        }

        if (largestSubscription != null && largestSubscriptionMonthly != null) {
            return largestSubscription.getName() + " is your largest subscription at " +
                    largestSubscriptionMonthly + " NOK/month.";
        }

        return "You currently spend about " + totalMonthly + " NOK per month across " + count + " subscriptions.";
    }

    private LocalDate rollForward(LocalDate next, String interval, LocalDate today) {
        LocalDate d = next;
        while (!d.isAfter(today)) {
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

    public static class CategoryInsight {
        private final String category;
        private final BigDecimal amount;
        private final int percent;
        private final int barWidth;

        public CategoryInsight(String category, BigDecimal amount, int percent, int barWidth) {
            this.category = category;
            this.amount = amount;
            this.percent = percent;
            this.barWidth = barWidth;
        }

        public String getCategory() {
            return category;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public int getPercent() {
            return percent;
        }

        public int getBarWidth() {
            return barWidth;
        }
    }
}
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
import java.time.YearMonth;
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
        model.addAttribute("categoryChartCss", buildCategoryChartCss(categoryInsights));

        CategoryInsight largestCategory = categoryInsights.isEmpty() ? null : categoryInsights.get(0);
        model.addAttribute("largestCategory", largestCategory);

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

        model.addAttribute("projectionMonths", buildProjectionMonths(activeSubs, today));
        model.addAttribute("smartInsight", buildSmartInsight(
                activeSubs.size(),
                finalTotalMonthlyNok,
                largestCategory,
                largestSubscription,
                largestSubscription == null ? null : monthlyNokBySubId.get(largestSubscription.getId()),
                dueThisMonth.size()
        ));

        model.addAttribute("showDevLinks", false);

        return "app";
    }

    private List<MonthProjection> buildProjectionMonths(List<Subscription> activeSubs, LocalDate today) {
        YearMonth startMonth = YearMonth.from(today);
        Map<YearMonth, BigDecimal> totals = new LinkedHashMap<>();

        for (int i = 0; i < 6; i++) {
            totals.put(startMonth.plusMonths(i), BigDecimal.ZERO);
        }

        LocalDate startDate = startMonth.atDay(1);
        LocalDate endDate = startMonth.plusMonths(5).atEndOfMonth();

        for (Subscription s : activeSubs) {
            if (s.getNextChargeDate() == null || s.getAmount() == null) continue;

            BigDecimal chargeNok = fx.convertToNok(s.getAmount(), s.getCurrency());
            if (chargeNok == null) continue;

            LocalDate chargeDate = s.getNextChargeDate();

            while (chargeDate.isBefore(startDate)) {
                chargeDate = advance(chargeDate, s.getInterval());
            }

            int guard = 0;
            while (!chargeDate.isAfter(endDate) && guard++ < 100) {
                YearMonth ym = YearMonth.from(chargeDate);
                if (totals.containsKey(ym)) {
                    totals.put(ym, totals.get(ym).add(chargeNok));
                }
                chargeDate = advance(chargeDate, s.getInterval());
            }
        }

        BigDecimal max = totals.values().stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

        List<MonthProjection> result = new ArrayList<>();
        for (var e : totals.entrySet()) {
            BigDecimal amount = e.getValue().setScale(2, RoundingMode.HALF_UP);
            int width = max.compareTo(BigDecimal.ZERO) <= 0
                    ? 4
                    : amount.multiply(BigDecimal.valueOf(100)).divide(max, 0, RoundingMode.HALF_UP).intValue();

            result.add(new MonthProjection(
                    e.getKey().atDay(1).format(labelFmt),
                    amount,
                    Math.max(4, Math.min(100, width))
            ));
        }

        return result;
    }

    private String buildCategoryChartCss(List<CategoryInsight> insights) {
        if (insights == null || insights.isEmpty()) {
            return "conic-gradient(rgba(255,255,255,.12) 0 100%)";
        }

        StringBuilder sb = new StringBuilder("conic-gradient(");
        int current = 0;

        for (int i = 0; i < insights.size(); i++) {
            CategoryInsight c = insights.get(i);
            int next = (i == insights.size() - 1) ? 100 : Math.min(100, current + c.getPercent());

            if (i > 0) sb.append(", ");
            sb.append(colorForCategory(c.getCategory()))
                    .append(" ")
                    .append(current)
                    .append("% ")
                    .append(next)
                    .append("%");

            current = next;
        }

        sb.append(")");
        return sb.toString();
    }

    private String colorForCategory(String category) {
        if (category == null) return "#64748b";

        return switch (category) {
            case "Entertainment" -> "#60a5fa";
            case "Utilities" -> "#34d399";
            case "Telecom" -> "#f59e0b";
            case "Health & Fitness" -> "#fb7185";
            case "News" -> "#a78bfa";
            case "Shopping & Food" -> "#f97316";
            case "Uncategorized" -> "#64748b";
            default -> "#94a3b8";
        };
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
            BigDecimal largestSubscriptionMonthly,
            int dueThisMonthCount
    ) {
        if (count == 0) {
            return "Add subscriptions manually or import transactions to start getting insights.";
        }

        if (largestCategory != null && largestCategory.getPercent() >= 50) {
            return largestCategory.getCategory() + " makes up " +
                    largestCategory.getPercent() +
                    "% of your monthly subscription spending.";
        }

        if (dueThisMonthCount > 0) {
            return "You have " + dueThisMonthCount + " subscription payment(s) coming up this month.";
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
            d = advance(d, interval);
        }
        return d;
    }

    private LocalDate advance(LocalDate d, String interval) {
        return switch (interval == null ? "MONTHLY" : interval.trim().toUpperCase()) {
            case "WEEKLY" -> d.plusWeeks(1);
            case "QUARTERLY" -> d.plusMonths(3);
            case "YEARLY" -> d.plusYears(1);
            default -> d.plusMonths(1);
        };
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

        public String getCategory() { return category; }
        public BigDecimal getAmount() { return amount; }
        public int getPercent() { return percent; }
        public int getBarWidth() { return barWidth; }
    }

    public static class MonthProjection {
        private final String label;
        private final BigDecimal amount;
        private final int barWidth;

        public MonthProjection(String label, BigDecimal amount, int barWidth) {
            this.label = label;
            this.amount = amount;
            this.barWidth = barWidth;
        }

        public String getLabel() { return label; }
        public BigDecimal getAmount() { return amount; }
        public int getBarWidth() { return barWidth; }
    }
}
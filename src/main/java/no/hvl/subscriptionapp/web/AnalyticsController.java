package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpSession;
import no.hvl.subscriptionapp.domain.Subscription;
import no.hvl.subscriptionapp.repository.SubscriptionRepository;
import no.hvl.subscriptionapp.service.ExchangeRateService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Controller
public class AnalyticsController {

    private final SubscriptionRepository subscriptionRepository;
    private final ExchangeRateService exchangeRateService;
    private final MessageSource messageSource;

    public AnalyticsController(
            SubscriptionRepository subscriptionRepository,
            ExchangeRateService exchangeRateService,
            MessageSource messageSource
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.exchangeRateService = exchangeRateService;
        this.messageSource = messageSource;
    }

    @GetMapping("/app/analytics")
    public String analytics(HttpSession session, Model model, Locale locale) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        List<Subscription> allSubscriptions =
                subscriptionRepository.findByUserEmailOrderByCreatedAtDesc(email);

        List<Subscription> activeSubscriptions = allSubscriptions.stream()
                .filter(Subscription::isActive)
                .toList();

        List<Subscription> endedSubscriptions = allSubscriptions.stream()
                .filter(subscription -> !subscription.isActive())
                .toList();

        Map<UUID, BigDecimal> monthlyNokBySubscriptionId = new LinkedHashMap<>();
        BigDecimal activeMonthlyNok = BigDecimal.ZERO;
        BigDecimal savedMonthlyNok = BigDecimal.ZERO;

        for (Subscription subscription : allSubscriptions) {
            BigDecimal monthlyNok = monthlyCostInNok(subscription);
            monthlyNokBySubscriptionId.put(subscription.getId(), monthlyNok);

            if (subscription.isActive()) {
                activeMonthlyNok = activeMonthlyNok.add(monthlyNok);
            } else {
                savedMonthlyNok = savedMonthlyNok.add(monthlyNok);
            }
        }

        activeMonthlyNok = money(activeMonthlyNok);
        savedMonthlyNok = money(savedMonthlyNok);

        BigDecimal yearlyNok = money(activeMonthlyNok.multiply(BigDecimal.valueOf(12)));
        BigDecimal savedYearlyNok = money(savedMonthlyNok.multiply(BigDecimal.valueOf(12)));
        BigDecimal averageMonthlyNok = activeSubscriptions.isEmpty()
                ? money(BigDecimal.ZERO)
                : activeMonthlyNok.divide(
                BigDecimal.valueOf(activeSubscriptions.size()),
                2,
                RoundingMode.HALF_UP
        );

        int uncategorizedCount = (int) activeSubscriptions.stream()
                .filter(subscription -> cleanCategory(subscription.getCategory()).equals("Uncategorized"))
                .count();

        int missingNextChargeCount = (int) activeSubscriptions.stream()
                .filter(subscription -> subscription.getNextChargeDate() == null)
                .count();

        int dataQualityScore = calculateDataQualityScore(
                activeSubscriptions.size(),
                uncategorizedCount,
                missingNextChargeCount
        );

        List<CategoryAnalytics> categoryAnalytics = buildCategoryAnalytics(
                activeSubscriptions,
                monthlyNokBySubscriptionId,
                activeMonthlyNok
        );

        List<IntervalAnalytics> intervalAnalytics =
                buildIntervalAnalytics(activeSubscriptions);

        List<Subscription> topSubscriptions = activeSubscriptions.stream()
                .sorted((first, second) ->
                        monthlyNokBySubscriptionId
                                .getOrDefault(second.getId(), BigDecimal.ZERO)
                                .compareTo(monthlyNokBySubscriptionId
                                        .getOrDefault(first.getId(), BigDecimal.ZERO)))
                .limit(10)
                .toList();

        List<Subscription> endedSorted = endedSubscriptions.stream()
                .sorted((first, second) ->
                        monthlyNokBySubscriptionId
                                .getOrDefault(second.getId(), BigDecimal.ZERO)
                                .compareTo(monthlyNokBySubscriptionId
                                        .getOrDefault(first.getId(), BigDecimal.ZERO)))
                .limit(10)
                .toList();

        List<MonthAnalytics> forecastMonths =
                buildForecastMonths(activeSubscriptions, LocalDate.now(), locale);

        List<String> insights = buildInsights(
                activeSubscriptions,
                endedSubscriptions,
                categoryAnalytics,
                topSubscriptions,
                monthlyNokBySubscriptionId,
                activeMonthlyNok,
                savedMonthlyNok,
                uncategorizedCount,
                locale
        );

        model.addAttribute("email", email);
        model.addAttribute("activeSubscriptions", activeSubscriptions);
        model.addAttribute("endedSubscriptions", endedSorted);
        model.addAttribute("topSubscriptions", topSubscriptions);
        model.addAttribute("monthlyNokBySubscriptionId", monthlyNokBySubscriptionId);

        model.addAttribute("activeMonthlyNok", activeMonthlyNok);
        model.addAttribute("yearlyNok", yearlyNok);
        model.addAttribute("averageMonthlyNok", averageMonthlyNok);
        model.addAttribute("savedMonthlyNok", savedMonthlyNok);
        model.addAttribute("savedYearlyNok", savedYearlyNok);

        model.addAttribute("activeCount", activeSubscriptions.size());
        model.addAttribute("endedCount", endedSubscriptions.size());
        model.addAttribute("uncategorizedCount", uncategorizedCount);
        model.addAttribute("missingNextChargeCount", missingNextChargeCount);
        model.addAttribute("dataQualityScore", dataQualityScore);

        model.addAttribute("categoryAnalytics", categoryAnalytics);
        model.addAttribute("categoryChartCss", buildCategoryChartCss(categoryAnalytics));
        model.addAttribute("intervalAnalytics", intervalAnalytics);
        model.addAttribute("forecastMonths", forecastMonths);
        model.addAttribute("insights", insights);

        return "analytics";
    }

    private BigDecimal monthlyCostInNok(Subscription subscription) {
        if (subscription == null || subscription.getMonthlyCost() == null) {
            return money(BigDecimal.ZERO);
        }

        BigDecimal converted = exchangeRateService.convertToNok(
                subscription.getMonthlyCost(),
                subscription.getCurrency()
        );

        return money(converted == null ? BigDecimal.ZERO : converted);
    }

    private List<CategoryAnalytics> buildCategoryAnalytics(
            List<Subscription> activeSubscriptions,
            Map<UUID, BigDecimal> monthlyNokBySubscriptionId,
            BigDecimal totalMonthlyNok
    ) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();

        for (Subscription subscription : activeSubscriptions) {
            String category = cleanCategory(subscription.getCategory());
            BigDecimal monthlyNok = monthlyNokBySubscriptionId
                    .getOrDefault(subscription.getId(), BigDecimal.ZERO);
            totals.merge(category, monthlyNok, BigDecimal::add);
        }

        return totals.entrySet().stream()
                .map(entry -> {
                    BigDecimal amount = money(entry.getValue());
                    int percentage = percentage(amount, totalMonthlyNok);
                    return new CategoryAnalytics(
                            entry.getKey(),
                            amount,
                            percentage,
                            barWidth(percentage)
                    );
                })
                .sorted(Comparator.comparing(CategoryAnalytics::getAmount).reversed())
                .toList();
    }

    private List<IntervalAnalytics> buildIntervalAnalytics(
            List<Subscription> activeSubscriptions
    ) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("WEEKLY", 0);
        counts.put("MONTHLY", 0);
        counts.put("QUARTERLY", 0);
        counts.put("YEARLY", 0);
        counts.put("OTHER", 0);

        for (Subscription subscription : activeSubscriptions) {
            counts.merge(normalizeInterval(subscription.getInterval()), 1, Integer::sum);
        }

        int total = activeSubscriptions.size();

        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> {
                    int percentage = total == 0
                            ? 0
                            : (int) Math.round((entry.getValue() * 100.0) / total);
                    return new IntervalAnalytics(
                            entry.getKey(),
                            entry.getValue(),
                            percentage,
                            barWidth(percentage)
                    );
                })
                .sorted(Comparator.comparing(IntervalAnalytics::getCount).reversed())
                .toList();
    }

    private List<MonthAnalytics> buildForecastMonths(
            List<Subscription> activeSubscriptions,
            LocalDate today,
            Locale locale
    ) {
        YearMonth firstMonth = YearMonth.from(today);
        Map<YearMonth, BigDecimal> totals = new LinkedHashMap<>();

        for (int index = 0; index < 12; index++) {
            totals.put(firstMonth.plusMonths(index), BigDecimal.ZERO);
        }

        LocalDate firstDate = firstMonth.atDay(1);
        LocalDate lastDate = firstMonth.plusMonths(11).atEndOfMonth();

        for (Subscription subscription : activeSubscriptions) {
            if (subscription.getNextChargeDate() == null
                    || subscription.getAmount() == null) continue;

            BigDecimal chargeNok = exchangeRateService.convertToNok(
                    subscription.getAmount(),
                    subscription.getCurrency()
            );
            if (chargeNok == null) continue;

            LocalDate chargeDate = subscription.getNextChargeDate();

            int rewindGuard = 0;
            while (chargeDate.isBefore(firstDate) && rewindGuard++ < 500) {
                chargeDate = advance(chargeDate, subscription.getInterval());
            }

            int forecastGuard = 0;
            while (!chargeDate.isAfter(lastDate) && forecastGuard++ < 500) {
                YearMonth yearMonth = YearMonth.from(chargeDate);
                if (totals.containsKey(yearMonth)) {
                    totals.merge(yearMonth, chargeNok, BigDecimal::add);
                }
                chargeDate = advance(chargeDate, subscription.getInterval());
            }
        }

        BigDecimal maximum = totals.values().stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy", locale);
        List<MonthAnalytics> result = new ArrayList<>();

        for (Map.Entry<YearMonth, BigDecimal> entry : totals.entrySet()) {
            BigDecimal amount = money(entry.getValue());
            int width = maximum.compareTo(BigDecimal.ZERO) <= 0
                    ? 4
                    : amount.multiply(BigDecimal.valueOf(100))
                    .divide(maximum, 0, RoundingMode.HALF_UP)
                    .intValue();

            result.add(new MonthAnalytics(
                    entry.getKey().atDay(1).format(formatter),
                    amount,
                    Math.max(4, Math.min(100, width))
            ));
        }

        return result;
    }

    private List<String> buildInsights(
            List<Subscription> activeSubscriptions,
            List<Subscription> endedSubscriptions,
            List<CategoryAnalytics> categoryAnalytics,
            List<Subscription> topSubscriptions,
            Map<UUID, BigDecimal> monthlyNokBySubscriptionId,
            BigDecimal totalMonthlyNok,
            BigDecimal savedMonthlyNok,
            int uncategorizedCount,
            Locale locale
    ) {
        List<String> insights = new ArrayList<>();

        if (activeSubscriptions.isEmpty()) {
            insights.add(messageSource.getMessage("analytics.insight.empty", null, locale));
            return insights;
        }

        if (!categoryAnalytics.isEmpty()) {
            CategoryAnalytics largestCategory = categoryAnalytics.get(0);
            if (largestCategory.getPercentage() >= 40) {
                insights.add(messageSource.getMessage(
                        "analytics.insight.category",
                        new Object[]{
                                largestCategory.getCategory(),
                                largestCategory.getPercentage()
                        },
                        locale
                ));
            }
        }

        if (!topSubscriptions.isEmpty()) {
            Subscription largestSubscription = topSubscriptions.get(0);
            BigDecimal largestMonthly = monthlyNokBySubscriptionId
                    .getOrDefault(largestSubscription.getId(), BigDecimal.ZERO);

            insights.add(messageSource.getMessage(
                    "analytics.insight.largest",
                    new Object[]{largestSubscription.getName(), largestMonthly},
                    locale
            ));
        }

        if (!endedSubscriptions.isEmpty()
                && savedMonthlyNok.compareTo(BigDecimal.ZERO) > 0) {
            insights.add(messageSource.getMessage(
                    "analytics.insight.saved",
                    new Object[]{endedSubscriptions.size(), savedMonthlyNok},
                    locale
            ));
        }

        if (uncategorizedCount > 0) {
            insights.add(messageSource.getMessage(
                    "analytics.insight.uncategorized",
                    new Object[]{uncategorizedCount},
                    locale
            ));
        }

        long monthlyCount = activeSubscriptions.stream()
                .filter(subscription ->
                        "MONTHLY".equals(normalizeInterval(subscription.getInterval())))
                .count();

        if (monthlyCount == activeSubscriptions.size()
                && activeSubscriptions.size() > 1) {
            insights.add(messageSource.getMessage(
                    "analytics.insight.allMonthly",
                    new Object[]{monthlyCount},
                    locale
            ));
        }

        if (insights.isEmpty()) {
            insights.add(messageSource.getMessage(
                    "analytics.insight.summary",
                    new Object[]{totalMonthlyNok, activeSubscriptions.size()},
                    locale
            ));
        }

        return insights.stream().limit(5).toList();
    }

    private int calculateDataQualityScore(
            int activeCount,
            int uncategorizedCount,
            int missingNextChargeCount
    ) {
        if (activeCount == 0) return 0;

        int categoryPenalty = Math.min(
                40,
                (int) Math.round((uncategorizedCount * 40.0) / activeCount)
        );
        int datePenalty = Math.min(
                40,
                (int) Math.round((missingNextChargeCount * 40.0) / activeCount)
        );

        return Math.max(0, 100 - categoryPenalty - datePenalty);
    }

    private String buildCategoryChartCss(List<CategoryAnalytics> analytics) {
        if (analytics == null || analytics.isEmpty()) {
            return "conic-gradient(rgba(255,255,255,.12) 0 100%)";
        }

        StringBuilder css = new StringBuilder("conic-gradient(");
        int current = 0;

        for (int index = 0; index < analytics.size(); index++) {
            CategoryAnalytics category = analytics.get(index);
            int next = index == analytics.size() - 1
                    ? 100
                    : Math.min(100, current + category.getPercentage());

            if (index > 0) css.append(", ");

            css.append(colorForIndex(index))
                    .append(" ")
                    .append(current)
                    .append("% ")
                    .append(next)
                    .append("%");

            current = next;
        }

        return css.append(")").toString();
    }

    private String colorForIndex(int index) {
        String[] colors = {
                "#60a5fa", "#34d399", "#fb7185", "#f59e0b",
                "#a78bfa", "#22d3ee", "#f97316", "#94a3b8"
        };
        return colors[Math.floorMod(index, colors.length)];
    }

    private String cleanCategory(String category) {
        if (category == null
                || category.isBlank()
                || "Other".equalsIgnoreCase(category.trim())) {
            return "Uncategorized";
        }
        return category.trim();
    }

    private String normalizeInterval(String interval) {
        if (interval == null || interval.isBlank()) return "OTHER";

        return switch (interval.trim().toUpperCase(Locale.ROOT)) {
            case "WEEKLY" -> "WEEKLY";
            case "MONTHLY" -> "MONTHLY";
            case "QUARTERLY" -> "QUARTERLY";
            case "YEARLY" -> "YEARLY";
            default -> "OTHER";
        };
    }

    private LocalDate advance(LocalDate date, String interval) {
        return switch (normalizeInterval(interval)) {
            case "WEEKLY" -> date.plusWeeks(1);
            case "QUARTERLY" -> date.plusMonths(3);
            case "YEARLY" -> date.plusYears(1);
            default -> date.plusMonths(1);
        };
    }

    private BigDecimal money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int percentage(BigDecimal part, BigDecimal total) {
        if (part == null || total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        return part.multiply(BigDecimal.valueOf(100))
                .divide(total, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private int barWidth(int percentage) {
        return percentage <= 0 ? 4 : Math.max(4, Math.min(100, percentage));
    }

    public static class CategoryAnalytics {
        private final String category;
        private final BigDecimal amount;
        private final int percentage;
        private final int barWidth;

        public CategoryAnalytics(
                String category,
                BigDecimal amount,
                int percentage,
                int barWidth
        ) {
            this.category = category;
            this.amount = amount;
            this.percentage = percentage;
            this.barWidth = barWidth;
        }

        public String getCategory() { return category; }
        public BigDecimal getAmount() { return amount; }
        public int getPercentage() { return percentage; }
        public int getBarWidth() { return barWidth; }
    }

    public static class IntervalAnalytics {
        private final String interval;
        private final int count;
        private final int percentage;
        private final int barWidth;

        public IntervalAnalytics(
                String interval,
                int count,
                int percentage,
                int barWidth
        ) {
            this.interval = interval;
            this.count = count;
            this.percentage = percentage;
            this.barWidth = barWidth;
        }

        public String getInterval() { return interval; }
        public int getCount() { return count; }
        public int getPercentage() { return percentage; }
        public int getBarWidth() { return barWidth; }
    }

    public static class MonthAnalytics {
        private final String label;
        private final BigDecimal amount;
        private final int barWidth;

        public MonthAnalytics(String label, BigDecimal amount, int barWidth) {
            this.label = label;
            this.amount = amount;
            this.barWidth = barWidth;
        }

        public String getLabel() { return label; }
        public BigDecimal getAmount() { return amount; }
        public int getBarWidth() { return barWidth; }
    }
}

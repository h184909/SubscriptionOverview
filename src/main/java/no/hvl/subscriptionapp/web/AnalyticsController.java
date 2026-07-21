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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        List<Subscription> all = subscriptionRepository.findByUserEmailOrderByCreatedAtDesc(email);
        List<Subscription> active = all.stream().filter(Subscription::isActive).toList();
        List<Subscription> ended = all.stream().filter(s -> !s.isActive()).toList();

        Map<UUID, BigDecimal> monthlyById = new LinkedHashMap<>();
        for (Subscription s : all) monthlyById.put(s.getId(), monthlyCostInNok(s));

        BigDecimal monthlyTotal = sumMonthly(active, monthlyById);
        BigDecimal savedMonthly = sumMonthly(ended, monthlyById);
        BigDecimal yearlyTotal = money(monthlyTotal.multiply(BigDecimal.valueOf(12)));
        BigDecimal savedYearly = money(savedMonthly.multiply(BigDecimal.valueOf(12)));
        BigDecimal averageMonthly = active.isEmpty()
                ? money(BigDecimal.ZERO)
                : monthlyTotal.divide(BigDecimal.valueOf(active.size()), 2, RoundingMode.HALF_UP);
        BigDecimal medianMonthly = calculateMedian(active.stream()
                .map(s -> monthlyById.getOrDefault(s.getId(), BigDecimal.ZERO))
                .sorted()
                .toList());

        LocalDate today = LocalDate.now();
        LocalDate in30Days = today.plusDays(30);
        List<Subscription> upcoming = active.stream()
                .filter(s -> s.getNextChargeDate() != null)
                .filter(s -> !s.getNextChargeDate().isBefore(today))
                .filter(s -> !s.getNextChargeDate().isAfter(in30Days))
                .sorted(Comparator.comparing(Subscription::getNextChargeDate))
                .toList();
        BigDecimal upcoming30Days = money(upcoming.stream()
                .map(this::chargeAmountInNok)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        int uncategorized = (int) active.stream()
                .filter(s -> "Uncategorized".equals(cleanCategory(s.getCategory())))
                .count();
        int missingDates = (int) active.stream()
                .filter(s -> s.getNextChargeDate() == null)
                .count();
        int dataQualityScore = calculateDataQualityScore(active.size(), uncategorized, missingDates);

        List<CategoryAnalytics> categories = buildCategoryAnalytics(active, monthlyById, monthlyTotal);
        List<IntervalAnalytics> intervals = buildIntervalAnalytics(active);
        List<CurrencyAnalytics> currencies = buildCurrencyAnalytics(active);

        List<Subscription> topSubscriptions = active.stream()
                .sorted(byMonthlyCostDescending(monthlyById))
                .limit(10)
                .toList();
        List<Subscription> endedSorted = ended.stream()
                .sorted(byMonthlyCostDescending(monthlyById))
                .limit(10)
                .toList();

        Subscription largest = topSubscriptions.isEmpty() ? null : topSubscriptions.get(0);
        Subscription cheapest = active.stream()
                .min(Comparator.comparing(s -> monthlyById.getOrDefault(s.getId(), BigDecimal.ZERO)))
                .orElse(null);
        CategoryAnalytics largestCategory = categories.isEmpty() ? null : categories.get(0);
        int largestShare = largest == null ? 0 : percentage(
                monthlyById.getOrDefault(largest.getId(), BigDecimal.ZERO), monthlyTotal);

        List<MonthAnalytics> forecast = buildForecastMonths(active, today, locale);
        BigDecimal forecastAverage = forecast.isEmpty()
                ? money(BigDecimal.ZERO)
                : forecast.stream().map(MonthAnalytics::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(forecast.size()), 2, RoundingMode.HALF_UP);

        model.addAttribute("email", email);
        model.addAttribute("activeSubscriptions", active);
        model.addAttribute("endedSubscriptions", endedSorted);
        model.addAttribute("topSubscriptions", topSubscriptions);
        model.addAttribute("upcomingSubscriptions", upcoming);
        model.addAttribute("monthlyNokBySubscriptionId", monthlyById);

        model.addAttribute("activeMonthlyNok", monthlyTotal);
        model.addAttribute("yearlyNok", yearlyTotal);
        model.addAttribute("averageMonthlyNok", averageMonthly);
        model.addAttribute("medianMonthlyNok", medianMonthly);
        model.addAttribute("savedMonthlyNok", savedMonthly);
        model.addAttribute("savedYearlyNok", savedYearly);
        model.addAttribute("upcoming30DaysNok", upcoming30Days);
        model.addAttribute("forecastAverage", forecastAverage);

        model.addAttribute("activeCount", active.size());
        model.addAttribute("endedCount", ended.size());
        model.addAttribute("upcomingCount", upcoming.size());
        model.addAttribute("uncategorizedCount", uncategorized);
        model.addAttribute("missingNextChargeCount", missingDates);
        model.addAttribute("dataQualityScore", dataQualityScore);
        model.addAttribute("largestSubscriptionShare", largestShare);

        model.addAttribute("categoryAnalytics", categories);
        model.addAttribute("categoryChartCss", buildCategoryChartCss(categories));
        model.addAttribute("intervalAnalytics", intervals);
        model.addAttribute("currencyAnalytics", currencies);
        model.addAttribute("forecastMonths", forecast);
        model.addAttribute("awards", buildAwards(largest, cheapest, largestCategory, monthlyById, savedMonthly, locale));
        model.addAttribute("insights", buildInsights(
                active, ended, categories, topSubscriptions, monthlyById,
                monthlyTotal, savedMonthly, uncategorized, upcoming30Days,
                largestShare, locale));

        return "analytics";
    }

    private BigDecimal sumMonthly(List<Subscription> subscriptions, Map<UUID, BigDecimal> monthlyById) {
        return money(subscriptions.stream()
                .map(s -> monthlyById.getOrDefault(s.getId(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private Comparator<Subscription> byMonthlyCostDescending(Map<UUID, BigDecimal> monthlyById) {
        return (a, b) -> monthlyById.getOrDefault(b.getId(), BigDecimal.ZERO)
                .compareTo(monthlyById.getOrDefault(a.getId(), BigDecimal.ZERO));
    }

    private BigDecimal monthlyCostInNok(Subscription s) {
        if (s == null || s.getMonthlyCost() == null) return money(BigDecimal.ZERO);
        BigDecimal converted = exchangeRateService.convertToNok(s.getMonthlyCost(), s.getCurrency());
        return money(converted == null ? BigDecimal.ZERO : converted);
    }

    private BigDecimal chargeAmountInNok(Subscription s) {
        if (s == null || s.getAmount() == null) return money(BigDecimal.ZERO);
        BigDecimal converted = exchangeRateService.convertToNok(s.getAmount(), s.getCurrency());
        return money(converted == null ? BigDecimal.ZERO : converted);
    }

    private BigDecimal calculateMedian(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return money(BigDecimal.ZERO);
        int middle = values.size() / 2;
        if (values.size() % 2 == 1) return money(values.get(middle));
        return values.get(middle - 1).add(values.get(middle))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private List<CategoryAnalytics> buildCategoryAnalytics(
            List<Subscription> active,
            Map<UUID, BigDecimal> monthlyById,
            BigDecimal total
    ) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Subscription s : active) {
            totals.merge(cleanCategory(s.getCategory()),
                    monthlyById.getOrDefault(s.getId(), BigDecimal.ZERO), BigDecimal::add);
        }
        return totals.entrySet().stream()
                .map(e -> {
                    BigDecimal amount = money(e.getValue());
                    int pct = percentage(amount, total);
                    return new CategoryAnalytics(e.getKey(), amount, pct, barWidth(pct));
                })
                .sorted(Comparator.comparing(CategoryAnalytics::getAmount).reversed())
                .toList();
    }

    private List<IntervalAnalytics> buildIntervalAnalytics(List<Subscription> active) {
        Map<String, Long> counts = active.stream()
                .map(s -> normalizeInterval(s.getInterval()))
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        int total = active.size();
        return counts.entrySet().stream()
                .map(e -> {
                    int count = e.getValue().intValue();
                    int pct = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
                    return new IntervalAnalytics(e.getKey(), count, pct, barWidth(pct));
                })
                .sorted(Comparator.comparing(IntervalAnalytics::getCount).reversed())
                .toList();
    }

    private List<CurrencyAnalytics> buildCurrencyAnalytics(List<Subscription> active) {
        Map<String, Long> counts = active.stream()
                .map(s -> normalizeCurrency(s.getCurrency()))
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        int total = active.size();
        return counts.entrySet().stream()
                .map(e -> {
                    int count = e.getValue().intValue();
                    int pct = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
                    return new CurrencyAnalytics(e.getKey(), count, pct, barWidth(pct));
                })
                .sorted(Comparator.comparing(CurrencyAnalytics::getCount).reversed())
                .toList();
    }

    private List<MonthAnalytics> buildForecastMonths(
            List<Subscription> active,
            LocalDate today,
            Locale locale
    ) {
        YearMonth firstMonth = YearMonth.from(today);

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MMM yyyy", locale);

        /*
         * Grafen viser estimert abonnementskostnad per kalendermåned,
         * ikke bare trekk som gjenstår etter dagens dato.
         *
         * Derfor bruker vi abonnementets månedlige kostnad:
         * - WEEKLY fordeles til gjennomsnitt per måned
         * - MONTHLY brukes direkte
         * - QUARTERLY fordeles over tre måneder
         * - YEARLY fordeles over tolv måneder
         */
        BigDecimal estimatedMonthlyTotal = active.stream()
                .map(this::monthlyCostInNok)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        estimatedMonthlyTotal = money(estimatedMonthlyTotal);

        List<MonthAnalytics> result = new ArrayList<>();

        for (int index = 0; index < 12; index++) {
            YearMonth month = firstMonth.plusMonths(index);

            result.add(new MonthAnalytics(
                    month.atDay(1).format(formatter),
                    estimatedMonthlyTotal,
                    estimatedMonthlyTotal.compareTo(BigDecimal.ZERO) > 0 ? 100 : 4
            ));
        }

        return result;
    }

    private List<AnalyticsAward> buildAwards(
            Subscription largest,
            Subscription cheapest,
            CategoryAnalytics largestCategory,
            Map<UUID, BigDecimal> monthlyById,
            BigDecimal savedMonthly,
            Locale locale
    ) {
        List<AnalyticsAward> awards = new ArrayList<>();
        if (largest != null) awards.add(new AnalyticsAward("💸",
                messageSource.getMessage("analytics.award.biggest.title", null, locale),
                messageSource.getMessage("analytics.award.biggest.text",
                        new Object[]{largest.getName(), monthlyById.getOrDefault(largest.getId(), BigDecimal.ZERO)}, locale)));
        if (cheapest != null) awards.add(new AnalyticsAward("🌱",
                messageSource.getMessage("analytics.award.cheapest.title", null, locale),
                messageSource.getMessage("analytics.award.cheapest.text",
                        new Object[]{cheapest.getName(), monthlyById.getOrDefault(cheapest.getId(), BigDecimal.ZERO)}, locale)));
        if (largestCategory != null) awards.add(new AnalyticsAward("🏆",
                messageSource.getMessage("analytics.award.category.title", null, locale),
                messageSource.getMessage("analytics.award.category.text",
                        new Object[]{largestCategory.getCategory(), largestCategory.getPercentage()}, locale)));
        if (savedMonthly.compareTo(BigDecimal.ZERO) > 0) awards.add(new AnalyticsAward("✂️",
                messageSource.getMessage("analytics.award.savings.title", null, locale),
                messageSource.getMessage("analytics.award.savings.text", new Object[]{savedMonthly}, locale)));
        return awards.stream().limit(4).toList();
    }

    private List<String> buildInsights(
            List<Subscription> active,
            List<Subscription> ended,
            List<CategoryAnalytics> categories,
            List<Subscription> topSubscriptions,
            Map<UUID, BigDecimal> monthlyById,
            BigDecimal totalMonthly,
            BigDecimal savedMonthly,
            int uncategorized,
            BigDecimal upcoming30Days,
            int largestShare,
            Locale locale
    ) {
        List<String> insights = new ArrayList<>();
        if (active.isEmpty()) {
            insights.add(messageSource.getMessage("analytics.insight.empty", null, locale));
            return insights;
        }
        if (!categories.isEmpty() && categories.get(0).getPercentage() >= 40) {
            CategoryAnalytics c = categories.get(0);
            insights.add(messageSource.getMessage("analytics.insight.category",
                    new Object[]{c.getCategory(), c.getPercentage()}, locale));
        }
        if (!topSubscriptions.isEmpty()) {
            Subscription s = topSubscriptions.get(0);
            insights.add(messageSource.getMessage("analytics.insight.largest",
                    new Object[]{s.getName(), monthlyById.getOrDefault(s.getId(), BigDecimal.ZERO)}, locale));
        }
        if (largestShare >= 40) insights.add(messageSource.getMessage(
                "analytics.insight.concentration", new Object[]{largestShare}, locale));
        if (!ended.isEmpty() && savedMonthly.compareTo(BigDecimal.ZERO) > 0) {
            insights.add(messageSource.getMessage("analytics.insight.saved",
                    new Object[]{ended.size(), savedMonthly}, locale));
        }
        if (upcoming30Days.compareTo(BigDecimal.ZERO) > 0) insights.add(messageSource.getMessage(
                "analytics.insight.upcoming", new Object[]{upcoming30Days}, locale));
        if (uncategorized > 0) insights.add(messageSource.getMessage(
                "analytics.insight.uncategorized", new Object[]{uncategorized}, locale));

        long monthlyCount = active.stream()
                .filter(s -> "MONTHLY".equals(normalizeInterval(s.getInterval())))
                .count();
        if (monthlyCount == active.size() && active.size() > 1) {
            insights.add(messageSource.getMessage("analytics.insight.allMonthly",
                    new Object[]{monthlyCount}, locale));
        }
        if (insights.isEmpty()) insights.add(messageSource.getMessage(
                "analytics.insight.summary", new Object[]{totalMonthly, active.size()}, locale));
        return insights.stream().limit(6).toList();
    }

    private int calculateDataQualityScore(int activeCount, int uncategorized, int missingDates) {
        if (activeCount == 0) return 0;
        int categoryPenalty = Math.min(40, (int) Math.round(uncategorized * 40.0 / activeCount));
        int datePenalty = Math.min(40, (int) Math.round(missingDates * 40.0 / activeCount));
        return Math.max(0, 100 - categoryPenalty - datePenalty);
    }

    private String buildCategoryChartCss(List<CategoryAnalytics> categories) {
        if (categories == null || categories.isEmpty())
            return "conic-gradient(rgba(255,255,255,.12) 0 100%)";
        StringBuilder css = new StringBuilder("conic-gradient(");
        int current = 0;
        for (int i = 0; i < categories.size(); i++) {
            CategoryAnalytics c = categories.get(i);
            int next = i == categories.size() - 1 ? 100 : Math.min(100, current + c.getPercentage());
            if (i > 0) css.append(", ");
            css.append(colorForIndex(i)).append(" ").append(current).append("% ").append(next).append("%");
            current = next;
        }
        return css.append(")").toString();
    }

    private String colorForIndex(int index) {
        String[] colors = {"#60a5fa", "#34d399", "#fb7185", "#f59e0b",
                "#a78bfa", "#22d3ee", "#f97316", "#94a3b8"};
        return colors[Math.floorMod(index, colors.length)];
    }

    private String cleanCategory(String category) {
        if (category == null || category.isBlank() || "Other".equalsIgnoreCase(category.trim()))
            return "Uncategorized";
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

    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? "NOK" : currency.trim().toUpperCase(Locale.ROOT);
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
        return (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
    }

    private int percentage(BigDecimal part, BigDecimal total) {
        if (part == null || total == null || total.compareTo(BigDecimal.ZERO) <= 0) return 0;
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
    }

    private int barWidth(int percentage) {
        return percentage <= 0 ? 4 : Math.max(4, Math.min(100, percentage));
    }

    public static class CategoryAnalytics {
        private final String category;
        private final BigDecimal amount;
        private final int percentage;
        private final int barWidth;
        public CategoryAnalytics(String category, BigDecimal amount, int percentage, int barWidth) {
            this.category = category; this.amount = amount; this.percentage = percentage; this.barWidth = barWidth;
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
        public IntervalAnalytics(String interval, int count, int percentage, int barWidth) {
            this.interval = interval; this.count = count; this.percentage = percentage; this.barWidth = barWidth;
        }
        public String getInterval() { return interval; }
        public int getCount() { return count; }
        public int getPercentage() { return percentage; }
        public int getBarWidth() { return barWidth; }
    }

    public static class CurrencyAnalytics {
        private final String currency;
        private final int count;
        private final int percentage;
        private final int barWidth;
        public CurrencyAnalytics(String currency, int count, int percentage, int barWidth) {
            this.currency = currency; this.count = count; this.percentage = percentage; this.barWidth = barWidth;
        }
        public String getCurrency() { return currency; }
        public int getCount() { return count; }
        public int getPercentage() { return percentage; }
        public int getBarWidth() { return barWidth; }
    }

    public static class MonthAnalytics {
        private final String label;
        private final BigDecimal amount;
        private final int barWidth;
        public MonthAnalytics(String label, BigDecimal amount, int barWidth) {
            this.label = label; this.amount = amount; this.barWidth = barWidth;
        }
        public String getLabel() { return label; }
        public BigDecimal getAmount() { return amount; }
        public int getBarWidth() { return barWidth; }
    }

    public static class AnalyticsAward {
        private final String icon;
        private final String title;
        private final String text;
        public AnalyticsAward(String icon, String title, String text) {
            this.icon = icon; this.title = title; this.text = text;
        }
        public String getIcon() { return icon; }
        public String getTitle() { return title; }
        public String getText() { return text; }
    }
}

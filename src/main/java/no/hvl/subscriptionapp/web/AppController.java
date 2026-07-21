package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpSession;
import no.hvl.subscriptionapp.domain.LunchFlowConnection;
import no.hvl.subscriptionapp.domain.Subscription;
import no.hvl.subscriptionapp.repository.LunchFlowConnectionRepository;
import no.hvl.subscriptionapp.repository.SubscriptionRepository;
import no.hvl.subscriptionapp.service.ExchangeRateService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Controller
public class AppController {

    private final SubscriptionRepository subscriptionRepo;
    private final LunchFlowConnectionRepository lunchFlowConnectionRepo;
    private final ExchangeRateService fx;
    private final MessageSource messageSource;

    public AppController(
            SubscriptionRepository subscriptionRepo,
            LunchFlowConnectionRepository lunchFlowConnectionRepo,
            ExchangeRateService fx,
            MessageSource messageSource
    ) {
        this.subscriptionRepo = subscriptionRepo;
        this.lunchFlowConnectionRepo = lunchFlowConnectionRepo;
        this.fx = fx;
        this.messageSource = messageSource;
    }

    @GetMapping("/app")
    public String dashboard(HttpSession session, Model model, Locale locale) {
        String email = (String) session.getAttribute(LoginController.SESSION_USER_EMAIL);
        if (email == null) return "redirect:/login";

        LocalDate today = LocalDate.now();

        model.addAttribute("email", email);
        model.addAttribute("displayName", displayNameFromEmail(email));
        model.addAttribute("greetingKey", greetingKey(LocalDateTime.now().getHour()));

        LunchFlowConnection connection = lunchFlowConnectionRepo
                .findFirstByUserEmailOrderByUpdatedAtDesc(email)
                .orElse(null);
        addBankModel(model, connection);

        List<Subscription> all = subscriptionRepo.findByUserEmailOrderByCreatedAtDesc(email);
        rollForwardChargeDates(all, today);

        List<Subscription> active = all.stream().filter(Subscription::isActive).toList();
        List<Subscription> ended = all.stream().filter(s -> !s.isActive()).toList();

        Map<UUID, BigDecimal> monthlyNokBySubId = new LinkedHashMap<>();
        for (Subscription s : all) {
            monthlyNokBySubId.put(s.getId(), monthlyCostInNok(s));
        }

        BigDecimal totalMonthlyNok = sumMonthly(active, monthlyNokBySubId);
        BigDecimal yearlyTotalNok = money(totalMonthlyNok.multiply(BigDecimal.valueOf(12)));
        BigDecimal savedMonthlyNok = sumMonthly(ended, monthlyNokBySubId);
        BigDecimal savedYearlyNok = money(savedMonthlyNok.multiply(BigDecimal.valueOf(12)));

        List<Subscription> dueSoon = active.stream()
                .filter(s -> s.getNextChargeDate() != null)
                .filter(s -> !s.getNextChargeDate().isBefore(today))
                .filter(s -> !s.getNextChargeDate().isAfter(today.plusDays(7)))
                .sorted(Comparator.comparing(Subscription::getNextChargeDate))
                .toList();

        List<Subscription> upcoming = active.stream()
                .filter(s -> s.getNextChargeDate() != null)
                .filter(s -> !s.getNextChargeDate().isBefore(today))
                .sorted(Comparator.comparing(Subscription::getNextChargeDate))
                .limit(6)
                .toList();

        Subscription nextPayment = upcoming.isEmpty() ? null : upcoming.get(0);
        BigDecimal nextPaymentNok = nextPayment == null ? null : chargeAmountInNok(nextPayment);
        Long nextPaymentDays = nextPayment == null ? null
                : ChronoUnit.DAYS.between(today, nextPayment.getNextChargeDate());

        List<Subscription> topSubscriptions = active.stream()
                .sorted((a, b) -> monthlyNokBySubId.getOrDefault(b.getId(), BigDecimal.ZERO)
                        .compareTo(monthlyNokBySubId.getOrDefault(a.getId(), BigDecimal.ZERO)))
                .limit(4)
                .toList();

        model.addAttribute("subs", active);
        model.addAttribute("activeSubscriptionCount", active.size());
        model.addAttribute("endedSubscriptionCount", ended.size());
        model.addAttribute("totalMonthlyNok", totalMonthlyNok);
        model.addAttribute("yearlyTotalNok", yearlyTotalNok);
        model.addAttribute("savedMonthlyNok", savedMonthlyNok);
        model.addAttribute("savedYearlyNok", savedYearlyNok);
        model.addAttribute("monthlyNokBySubId", monthlyNokBySubId);
        model.addAttribute("dueSoon", dueSoon);
        model.addAttribute("dueSoonCount", dueSoon.size());
        model.addAttribute("upcomingPayments", upcoming);
        model.addAttribute("nextPayment", nextPayment);
        model.addAttribute("nextPaymentNok", nextPaymentNok);
        model.addAttribute("nextPaymentDays", nextPaymentDays);
        model.addAttribute("topSubscriptions", topSubscriptions);
        model.addAttribute("recentActivity", buildRecentActivity(active, ended, connection, locale));
        model.addAttribute("alerts", buildAlerts(active, dueSoon, savedMonthlyNok, locale));
        model.addAttribute("showDevLinks", false);

        return "app";
    }

    private void addBankModel(Model model, LunchFlowConnection connection) {
        model.addAttribute("bankConnected", connection != null);
        model.addAttribute("bankInstitutionName", connection == null ? null : connection.getInstitutionName());
        model.addAttribute("bankAccountCount", connection == null ? null : connection.getAccountCount());
        model.addAttribute("bankAccountNames", connection == null ? null : connection.getAccountNames());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        ZoneId zone = ZoneId.of("Europe/Oslo");

        model.addAttribute("bankLastSynced",
                connection != null && connection.getLastSyncedAt() != null
                        ? connection.getLastSyncedAt().atZoneSameInstant(zone).format(formatter)
                        : null);
    }

    private List<DashboardActivity> buildRecentActivity(
            List<Subscription> active,
            List<Subscription> ended,
            LunchFlowConnection connection,
            Locale locale
    ) {
        List<DashboardActivity> result = new ArrayList<>();

        if (connection != null && connection.getLastSyncedAt() != null) {
            result.add(new DashboardActivity("↻",
                    messageSource.getMessage("dash.activity.synced.title", null, locale),
                    messageSource.getMessage("dash.activity.synced.text", null, locale)));
        }

        active.stream()
                .filter(s -> s.getCreatedAt() != null)
                .sorted(Comparator.comparing(Subscription::getCreatedAt).reversed())
                .limit(3)
                .forEach(s -> result.add(new DashboardActivity("＋",
                        messageSource.getMessage("dash.activity.added.title",
                                new Object[]{s.getName()}, locale),
                        s.getCreatedAt().toLocalDate().toString())));

        ended.stream()
                .filter(s -> s.getCreatedAt() != null)
                .sorted(Comparator.comparing(Subscription::getCreatedAt).reversed())
                .limit(2)
                .forEach(s -> result.add(new DashboardActivity("✓",
                        messageSource.getMessage("dash.activity.ended.title",
                                new Object[]{s.getName()}, locale),
                        messageSource.getMessage("dash.activity.ended.text", null, locale))));

        if (result.isEmpty()) {
            result.add(new DashboardActivity("✦",
                    messageSource.getMessage("dash.activity.empty.title", null, locale),
                    messageSource.getMessage("dash.activity.empty.text", null, locale)));
        }

        return result.stream().limit(6).toList();
    }

    private List<DashboardAlert> buildAlerts(
            List<Subscription> active,
            List<Subscription> dueSoon,
            BigDecimal savedMonthlyNok,
            Locale locale
    ) {
        List<DashboardAlert> result = new ArrayList<>();

        if (!dueSoon.isEmpty()) {
            result.add(new DashboardAlert("warning", "📅",
                    messageSource.getMessage("dash.alert.dueSoon.title",
                            new Object[]{dueSoon.size()}, locale),
                    messageSource.getMessage("dash.alert.dueSoon.text", null, locale)));
        }

        long missingCategory = active.stream()
                .filter(s -> s.getCategory() == null || s.getCategory().isBlank()
                        || "Other".equalsIgnoreCase(s.getCategory().trim()))
                .count();

        if (missingCategory > 0) {
            result.add(new DashboardAlert("info", "🏷",
                    messageSource.getMessage("dash.alert.category.title",
                            new Object[]{missingCategory}, locale),
                    messageSource.getMessage("dash.alert.category.text", null, locale)));
        }

        if (savedMonthlyNok.compareTo(BigDecimal.ZERO) > 0) {
            result.add(new DashboardAlert("good", "💰",
                    messageSource.getMessage("dash.alert.saved.title",
                            new Object[]{savedMonthlyNok}, locale),
                    messageSource.getMessage("dash.alert.saved.text", null, locale)));
        }

        if (active.isEmpty()) {
            result.add(new DashboardAlert("info", "✦",
                    messageSource.getMessage("dash.alert.empty.title", null, locale),
                    messageSource.getMessage("dash.alert.empty.text", null, locale)));
        }

        return result.stream().limit(4).toList();
    }

    private void rollForwardChargeDates(List<Subscription> subscriptions, LocalDate today) {
        boolean changed = false;

        for (Subscription s : subscriptions) {
            if (!s.isActive() || s.getNextChargeDate() == null) continue;

            LocalDate current = s.getNextChargeDate();
            if (!current.isAfter(today)) {
                LocalDate rolled = rollForward(current, s.getInterval(), today);
                if (!rolled.equals(current)) {
                    s.setNextChargeDate(rolled);
                    changed = true;
                }
            }
        }

        if (changed) subscriptionRepo.saveAll(subscriptions);
    }

    private String displayNameFromEmail(String email) {
        String local = email == null ? "" : email.split("@")[0]
                .replace(".", " ").replace("_", " ").replace("-", " ").trim();
        if (local.isBlank()) return email == null ? "" : email;

        StringBuilder result = new StringBuilder();
        for (String word : local.split("\\s+")) {
            if (!result.isEmpty()) result.append(" ");
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) result.append(word.substring(1));
        }
        return result.toString();
    }

    private String greetingKey(int hour) {
        if (hour < 12) return "dash.greeting.morning";
        if (hour < 18) return "dash.greeting.afternoon";
        return "dash.greeting.evening";
    }

    private BigDecimal sumMonthly(List<Subscription> subs, Map<UUID, BigDecimal> amounts) {
        return money(subs.stream()
                .map(s -> amounts.getOrDefault(s.getId(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal monthlyCostInNok(Subscription s) {
        if (s == null || s.getMonthlyCost() == null) return money(BigDecimal.ZERO);
        BigDecimal converted = fx.convertToNok(s.getMonthlyCost(), s.getCurrency());
        return money(converted == null ? BigDecimal.ZERO : converted);
    }

    private BigDecimal chargeAmountInNok(Subscription s) {
        if (s == null || s.getAmount() == null) return money(BigDecimal.ZERO);
        BigDecimal converted = fx.convertToNok(s.getAmount(), s.getCurrency());
        return money(converted == null ? BigDecimal.ZERO : converted);
    }

    private BigDecimal money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDate rollForward(LocalDate date, String interval, LocalDate today) {
        int guard = 0;
        while (!date.isAfter(today) && guard++ < 500) date = advance(date, interval);
        return date;
    }

    private LocalDate advance(LocalDate date, String interval) {
        return switch (interval == null ? "MONTHLY" : interval.trim().toUpperCase(Locale.ROOT)) {
            case "WEEKLY" -> date.plusWeeks(1);
            case "QUARTERLY" -> date.plusMonths(3);
            case "YEARLY" -> date.plusYears(1);
            default -> date.plusMonths(1);
        };
    }

    public static class DashboardActivity {
        private final String icon;
        private final String title;
        private final String text;

        public DashboardActivity(String icon, String title, String text) {
            this.icon = icon;
            this.title = title;
            this.text = text;
        }

        public String getIcon() { return icon; }
        public String getTitle() { return title; }
        public String getText() { return text; }
    }

    public static class DashboardAlert {
        private final String type;
        private final String icon;
        private final String title;
        private final String text;

        public DashboardAlert(String type, String icon, String title, String text) {
            this.type = type;
            this.icon = icon;
            this.title = title;
            this.text = text;
        }

        public String getType() { return type; }
        public String getIcon() { return icon; }
        public String getTitle() { return title; }
        public String getText() { return text; }
    }
}

package no.hvl.subscriptionapp.web;

import jakarta.servlet.http.HttpSession;
import no.hvl.subscriptionapp.domain.BankTransaction;
import no.hvl.subscriptionapp.domain.Subscription;
import no.hvl.subscriptionapp.repository.BankTransactionRepository;
import no.hvl.subscriptionapp.repository.SubscriptionRepository;
import no.hvl.subscriptionapp.service.ExchangeRateService;
import no.hvl.subscriptionapp.service.KnownMerchants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/app/subscriptions")
public class SubscriptionDetailsController {

    private static final int MAX_HISTORY_ROWS = 36;

    private final SubscriptionRepository subscriptionRepository;
    private final BankTransactionRepository transactionRepository;
    private final ExchangeRateService exchangeRateService;

    public SubscriptionDetailsController(
            SubscriptionRepository subscriptionRepository,
            BankTransactionRepository transactionRepository,
            ExchangeRateService exchangeRateService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.transactionRepository = transactionRepository;
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<?> details(
            HttpSession session,
            @PathVariable UUID id,
            Locale locale
    ) {
        String email = (String) session.getAttribute(
                LoginController.SESSION_USER_EMAIL
        );

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not signed in"));
        }

        Subscription subscription =
                subscriptionRepository.findById(id).orElse(null);

        if (subscription == null
                || !email.equalsIgnoreCase(subscription.getUserEmail())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Subscription not found"));
        }

        List<BankTransaction> matchingTransactions =
                findMatchingTransactions(email, subscription);

        List<PaymentHistoryItem> history = matchingTransactions.stream()
                .sorted(Comparator.comparing(
                        BankTransaction::getTxDate,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(MAX_HISTORY_ROWS)
                .map(this::toHistoryItem)
                .toList();

        BigDecimal monthlyNok = convertToNok(
                subscription.getMonthlyCost(),
                subscription.getCurrency()
        );

        BigDecimal yearlyNok = money(
                monthlyNok.multiply(BigDecimal.valueOf(12))
        );

        BigDecimal totalSpentNok = matchingTransactions.stream()
                .map(this::transactionAmountInNok)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averagePaymentNok = matchingTransactions.isEmpty()
                ? BigDecimal.ZERO
                : totalSpentNok.divide(
                BigDecimal.valueOf(matchingTransactions.size()),
                2,
                RoundingMode.HALF_UP
        );

        BigDecimal smallestPaymentNok = matchingTransactions.stream()
                .map(this::transactionAmountInNok)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal largestPaymentNok = matchingTransactions.stream()
                .map(this::transactionAmountInNok)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        LocalDate firstPaymentDate = matchingTransactions.stream()
                .map(BankTransaction::getTxLocalDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate lastPaymentDate = matchingTransactions.stream()
                .map(BankTransaction::getTxLocalDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        PriceChange priceChange =
                calculatePriceChange(matchingTransactions);

        String cancelUrl = firstNonBlank(
                subscription.getCancelUrl(),
                KnownMerchants.match(
                                firstNonBlank(
                                        subscription.getProviderKey(),
                                        subscription.getName()
                                ),
                                subscription.getName()
                        )
                        .map(KnownMerchants.Match::cancelUrl)
                        .orElse(null)
        );

        List<String> insights = buildInsights(
                subscription,
                matchingTransactions,
                monthlyNok,
                yearlyNok,
                totalSpentNok,
                priceChange
        );

        String language = locale != null
                && "nb".equalsIgnoreCase(locale.getLanguage())
                ? "nb"
                : "en";

        return ResponseEntity.ok(new SubscriptionDetailsResponse(
                language,
                subscription.getId(),
                subscription.getName(),
                blankToNull(subscription.getCategory()),
                subscription.isActive(),
                subscription.getAmount(),
                normalizeCurrency(subscription.getCurrency()),
                normalizeInterval(subscription.getInterval()),
                subscription.getNextChargeDate(),
                subscription.getCreatedAt(),
                blankToNull(subscription.getBillingEmail()),
                blankToNull(subscription.getProviderKey()),
                blankToNull(cancelUrl),
                monthlyNok,
                yearlyNok,
                money(totalSpentNok),
                matchingTransactions.size(),
                money(averagePaymentNok),
                money(smallestPaymentNok),
                money(largestPaymentNok),
                firstPaymentDate,
                lastPaymentDate,
                priceChange,
                insights,
                history
        ));
    }

    private List<BankTransaction> findMatchingTransactions(
            String email,
            Subscription subscription
    ) {
        List<BankTransaction> transactions =
                transactionRepository.findByUserEmailOrderByTxDateDesc(email);

        String providerKey = firstNonBlank(
                subscription.getProviderKey(),
                KnownMerchants.match(
                                subscription.getName(),
                                subscription.getName()
                        )
                        .map(KnownMerchants.Match::providerKey)
                        .orElse(null)
        );

        boolean knownProvider =
                KnownMerchants.isKnownProviderKey(providerKey);

        Set<String> nameTokens =
                significantTokens(subscription.getName());

        return transactions.stream()
                .filter(transaction -> isOutgoing(transaction.getAmount()))
                .filter(transaction -> transactionMatches(
                        transaction,
                        providerKey,
                        knownProvider,
                        nameTokens
                ))
                .toList();
    }

    private boolean transactionMatches(
            BankTransaction transaction,
            String subscriptionProvider,
            boolean knownProvider,
            Set<String> nameTokens
    ) {
        String raw = (
                firstNonBlank(transaction.getDescription(), "")
                        + " "
                        + firstNonBlank(transaction.getReference(), "")
        ).trim();

        Optional<KnownMerchants.Match> known =
                KnownMerchants.match(raw, raw);

        if (knownProvider) {
            return known.isPresent()
                    && known.get().providerKey()
                    .equalsIgnoreCase(subscriptionProvider);
        }

        if (KnownMerchants.isGenericPaymentWrapper(raw)) {
            return false;
        }

        String transactionText = normalize(raw);

        if (transactionText.isBlank() || nameTokens.isEmpty()) {
            return false;
        }

        long matchingTokens = nameTokens.stream()
                .filter(token -> containsWholeToken(
                        transactionText,
                        token
                ))
                .count();

        int required = nameTokens.size() == 1 ? 1 : 2;

        return matchingTokens >= Math.min(
                required,
                nameTokens.size()
        );
    }

    private boolean containsWholeToken(
            String text,
            String token
    ) {
        return Arrays.asList(text.split("\\s+")).contains(token);
    }

    private Set<String> significantTokens(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }

        Set<String> ignored = Set.of(
                "abonnement",
                "subscription",
                "premium",
                "plus",
                "mobil",
                "mobile",
                "norge",
                "norway",
                "service",
                "services",
                "apple",
                "google"
        );

        return Arrays.stream(normalize(value).split("\\s+"))
                .map(String::trim)
                .filter(token -> token.length() >= 3)
                .filter(token -> !ignored.contains(token))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> buildInsights(
            Subscription subscription,
            List<BankTransaction> transactions,
            BigDecimal monthlyNok,
            BigDecimal yearlyNok,
            BigDecimal totalSpentNok,
            PriceChange priceChange
    ) {
        List<String> insights = new ArrayList<>();

        insights.add("MONTHLY|" + monthlyNok);
        insights.add("YEARLY|" + yearlyNok);

        if (!transactions.isEmpty()) {
            insights.add("TOTAL_SPENT|" + money(totalSpentNok));
            insights.add("PAYMENT_COUNT|" + transactions.size());
        }

        if (subscription.getNextChargeDate() != null) {
            long days = ChronoUnit.DAYS.between(
                    LocalDate.now(),
                    subscription.getNextChargeDate()
            );

            insights.add("NEXT_PAYMENT|" + days);
        }

        if (priceChange != null
                && priceChange.changeAmountNok() != null
                && priceChange.changeAmountNok()
                .compareTo(BigDecimal.ZERO) != 0) {
            insights.add(
                    "PRICE_CHANGE|"
                            + priceChange.changeAmountNok()
                            + "|"
                            + priceChange.changePercent()
            );
        }

        if (transactions.isEmpty()) {
            insights.add("NO_HISTORY");
        }

        return insights;
    }

    private PriceChange calculatePriceChange(
            List<BankTransaction> transactions
    ) {
        List<BankTransaction> dated = transactions.stream()
                .filter(transaction ->
                        transaction.getTxLocalDate() != null
                )
                .sorted(Comparator.comparing(
                        BankTransaction::getTxLocalDate
                ))
                .toList();

        if (dated.size() < 2) return null;

        BigDecimal first =
                transactionAmountInNok(dated.get(0));

        BigDecimal latest =
                transactionAmountInNok(
                        dated.get(dated.size() - 1)
                );

        if (first.compareTo(BigDecimal.ZERO) == 0) return null;

        BigDecimal change = money(latest.subtract(first));

        BigDecimal percent = change
                .multiply(BigDecimal.valueOf(100))
                .divide(first, 1, RoundingMode.HALF_UP);

        return new PriceChange(
                first,
                latest,
                change,
                percent
        );
    }

    private PaymentHistoryItem toHistoryItem(
            BankTransaction transaction
    ) {
        return new PaymentHistoryItem(
                transaction.getId(),
                transaction.getTxLocalDate(),
                positive(transaction.getAmount()),
                normalizeCurrency(transaction.getCurrency()),
                transactionAmountInNok(transaction),
                blankToNull(transaction.getDescription()),
                blankToNull(transaction.getReference())
        );
    }

    private BigDecimal transactionAmountInNok(
            BankTransaction transaction
    ) {
        return convertToNok(
                positive(transaction.getAmount()),
                transaction.getCurrency()
        );
    }

    private BigDecimal convertToNok(
            BigDecimal amount,
            String currency
    ) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        BigDecimal converted = exchangeRateService.convertToNok(
                amount,
                currency
        );

        return money(converted == null ? amount : converted);
    }

    private boolean isOutgoing(BigDecimal amount) {
        return amount != null
                && amount.compareTo(BigDecimal.ZERO) < 0;
    }

    private BigDecimal positive(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO;
        return amount.abs();
    }

    private BigDecimal money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) return "NOK";
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeInterval(String value) {
        if (value == null || value.isBlank()) return "MONTHLY";
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalize(String value) {
        if (value == null) return "";

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", " ")
                .replace("-", " ")
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return "";
    }

    public record SubscriptionDetailsResponse(
            String language,
            UUID id,
            String name,
            String category,
            boolean active,
            BigDecimal amount,
            String currency,
            String interval,
            LocalDate nextChargeDate,
            LocalDateTime createdAt,
            String billingEmail,
            String providerKey,
            String cancelUrl,
            BigDecimal monthlyNok,
            BigDecimal yearlyNok,
            BigDecimal totalSpentNok,
            int paymentCount,
            BigDecimal averagePaymentNok,
            BigDecimal smallestPaymentNok,
            BigDecimal largestPaymentNok,
            LocalDate firstPaymentDate,
            LocalDate lastPaymentDate,
            PriceChange priceChange,
            List<String> insights,
            List<PaymentHistoryItem> history
    ) {}

    public record PaymentHistoryItem(
            UUID id,
            LocalDate date,
            BigDecimal amount,
            String currency,
            BigDecimal amountNok,
            String description,
            String reference
    ) {}

    public record PriceChange(
            BigDecimal firstAmountNok,
            BigDecimal latestAmountNok,
            BigDecimal changeAmountNok,
            BigDecimal changePercent
    ) {}
}

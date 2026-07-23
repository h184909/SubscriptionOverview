package no.hvl.subscriptionapp.service;

import no.hvl.subscriptionapp.domain.BankTransaction;
import no.hvl.subscriptionapp.domain.Subscription;
import no.hvl.subscriptionapp.domain.SuggestionDecision;
import no.hvl.subscriptionapp.repository.BankTransactionRepository;
import no.hvl.subscriptionapp.repository.SubscriptionRepository;
import no.hvl.subscriptionapp.repository.SuggestionDecisionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SubscriptionDetectorService {

    private static final int MAX_LAST_AGE_DAYS = 365;

    private static final BigDecimal LOW_VARIATION = new BigDecimal("0.03");
    private static final BigDecimal MEDIUM_VARIATION = new BigDecimal("0.10");
    private static final BigDecimal HIGH_VARIATION = new BigDecimal("0.20");

    private static final Set<String> GENERIC_TOKENS = Set.of(
            "kortkjop", "kortkjøp", "visa", "mastercard", "debit", "credit",
            "betaling", "purchase", "card", "butikk", "butikkjop", "kjop",
            "kjøp", "abonnement", "subscription", "monthly", "month",
            "faktura", "avtalegiro", "service", "services", "applepay",
            "googlepay", "paypal", "varekjop", "varekjøp", "proprietarybanktransactiontext",
            "terminal", "contactless", "mobilepay"
    );

    private static final Pattern NOISE = Pattern.compile(
            ".*\\b(" +
                    "overf(ø|o)ring|mellom\\s*egne|l(ø|o)nn|skatt|atm|uttak|kontant|" +
                    "refund|tilbakebetaling|vipps\\s*til\\s*privat|straks?overf(ø|o)ring|" +
                    "bankoverf(ø|o)ring|sparing|gebyr|renter|debetrenter|provisjon|" +
                    "kredittkortbetaling|kredittkort\\s*innbetaling|rentebetaling|" +
                    "klarna\\s*innbetaling|avdrag|betaling\\s*av\\s*l(å|a)n" +
                    ")\\b.*",
            Pattern.CASE_INSENSITIVE
    );

    private final BankTransactionRepository txRepo;
    private final SubscriptionRepository subRepo;
    private final SuggestionDecisionRepository decisionRepo;

    public SubscriptionDetectorService(
            BankTransactionRepository txRepo,
            SubscriptionRepository subRepo,
            SuggestionDecisionRepository decisionRepo
    ) {
        this.txRepo = txRepo;
        this.subRepo = subRepo;
        this.decisionRepo = decisionRepo;
    }

    public List<SubscriptionSuggestion> detect(String userEmail) {
        List<SubscriptionSuggestion> all = computeSuggestions(userEmail);

        Set<String> blocked = decisionRepo.findByUserEmail(userEmail).stream()
                .map(SuggestionDecision::getSuggestionKey)
                .collect(Collectors.toSet());

        List<Subscription> subscriptions =
                subRepo.findByUserEmailOrderByCreatedAtDesc(userEmail);

        Set<String> providerKeys = subscriptions.stream()
                .map(subscription -> norm(firstNonBlank(
                        subscription.getProviderKey(),
                        subscription.getName()
                )))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());

        Set<String> names = subscriptions.stream()
                .map(subscription -> norm(subscription.getName()))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());

        return all.stream()
                .filter(suggestion -> !blocked.contains(suggestion.getKey()))
                .filter(suggestion -> {
                    String provider = norm(firstNonBlank(
                            suggestion.getProviderKey(),
                            suggestion.getName()
                    ));
                    return provider.isBlank() || !providerKeys.contains(provider);
                })
                .filter(suggestion -> !names.contains(norm(suggestion.getName())))
                .sorted(
                        Comparator.comparingInt(
                                        SubscriptionSuggestion::getConfidence
                                )
                                .reversed()
                                .thenComparing(
                                        SubscriptionSuggestion::getOccurrences,
                                        Comparator.reverseOrder()
                                )
                                .thenComparing(
                                        SubscriptionSuggestion::getLastChargeDate,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                )
                .limit(120)
                .toList();
    }

    public Optional<SubscriptionSuggestion> findOne(
            String userEmail,
            String key
    ) {
        return computeSuggestions(userEmail).stream()
                .filter(suggestion -> suggestion.getKey().equals(key))
                .findFirst();
    }

    private List<SubscriptionSuggestion> computeSuggestions(
            String userEmail
    ) {
        OffsetDateTime after = OffsetDateTime.now().minusMonths(36);

        List<BankTransaction> transactions =
                txRepo.findByUserEmailAndTxDateAfterOrderByTxDateAsc(
                        userEmail,
                        after
                );

        List<BankTransaction> outgoing = transactions.stream()
                .filter(transaction ->
                        transaction.getAmount() != null
                                && transaction.getAmount()
                                .compareTo(BigDecimal.ZERO) < 0
                )
                .filter(transaction -> transaction.getTxDate() != null)
                .filter(transaction -> !isNoise(transaction))
                .toList();

        Map<String, List<BankTransaction>> groups = outgoing.stream()
                .collect(Collectors.groupingBy(this::groupKey));

        List<SubscriptionSuggestion> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Map.Entry<String, List<BankTransaction>> entry : groups.entrySet()) {
            String groupKey = entry.getKey();
            List<BankTransaction> originalGroup = entry.getValue();

            if (groupKey == null || groupKey.isBlank()) continue;
            if (originalGroup.size() < 2) continue;

            boolean known = groupKey.startsWith("prov:");

            List<BankTransaction> group = originalGroup.stream()
                    .sorted(Comparator.comparing(BankTransaction::getTxDate))
                    .toList();

            List<LocalDate> dates = group.stream()
                    .map(BankTransaction::getTxLocalDate)
                    .filter(Objects::nonNull)
                    .toList();

            if (dates.size() < 2) continue;

            IntervalGuess intervalGuess = guessInterval(dates);
            if (intervalGuess.interval == null) continue;

            LocalDate last = dates.get(dates.size() - 1);
            if (last.isBefore(today.minusDays(MAX_LAST_AGE_DAYS))) continue;

            List<BigDecimal> amounts = group.stream()
                    .map(transaction -> transaction.getAmount().abs())
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();

            if (amounts.size() < 2) continue;

            BigDecimal median = median(amounts);
            BigDecimal mad = medianAbsDev(amounts, median);

            if (median.compareTo(new BigDecimal("5.00")) < 0) continue;

            LocalDate next = rollForward(
                    addInterval(last, intervalGuess.interval),
                    intervalGuess.interval,
                    today
            );

            String providerKey;
            String displayName;
            String cancelUrl = null;
            String category;

            if (known) {
                providerKey = groupKey.substring("prov:".length());

                Optional<KnownMerchants.Match> match =
                        KnownMerchants.match(providerKey, rawText(group));

                displayName = match
                        .map(KnownMerchants.Match::displayName)
                        .orElse(prettyName(providerKey));

                cancelUrl = match
                        .map(KnownMerchants.Match::cancelUrl)
                        .orElse(null);

                category = match
                        .map(KnownMerchants.Match::category)
                        .orElse(KnownMerchants.categoryForProvider(providerKey));
            } else {
                providerKey = norm(groupKey);
                displayName = prettyName(groupKey);
                category = "Other";
            }

            if (!known && isWeakUnknownCandidate(
                    displayName,
                    group,
                    intervalGuess,
                    median,
                    mad
            )) {
                continue;
            }

            int confidence = scoreConfidence(
                    group.size(),
                    intervalGuess,
                    median,
                    mad,
                    known,
                    last,
                    today
            );

            if (!known && confidence < 70) continue;
            if (known && confidence < 55) continue;

            String amountKey = median
                    .setScale(0, RoundingMode.HALF_UP)
                    .toPlainString();

            String suggestionKey =
                    providerKey + "|" + intervalGuess.interval + "|" + amountKey;

            result.add(new SubscriptionSuggestion(
                    suggestionKey,
                    displayName,
                    median.setScale(2, RoundingMode.HALF_UP),
                    firstCurrency(group),
                    intervalGuess.interval,
                    last,
                    next,
                    group.size(),
                    confidence,
                    known,
                    providerKey,
                    cancelUrl,
                    category
            ));
        }

        return dedupeByProvider(result);
    }

    private List<SubscriptionSuggestion> dedupeByProvider(
            List<SubscriptionSuggestion> suggestions
    ) {
        Map<String, SubscriptionSuggestion> best = new LinkedHashMap<>();

        for (SubscriptionSuggestion suggestion : suggestions) {
            String key = norm(firstNonBlank(
                    suggestion.getProviderKey(),
                    suggestion.getName()
            ));

            SubscriptionSuggestion existing = best.get(key);

            if (existing == null || isBetter(suggestion, existing)) {
                best.put(key, suggestion);
            }
        }

        return new ArrayList<>(best.values());
    }

    private boolean isBetter(
            SubscriptionSuggestion candidate,
            SubscriptionSuggestion existing
    ) {
        if (candidate.getConfidence() != existing.getConfidence()) {
            return candidate.getConfidence() > existing.getConfidence();
        }

        if (!candidate.getOccurrences().equals(existing.getOccurrences())) {
            return candidate.getOccurrences() > existing.getOccurrences();
        }

        LocalDate candidateDate = candidate.getLastChargeDate();
        LocalDate existingDate = existing.getLastChargeDate();

        return candidateDate != null
                && (existingDate == null || candidateDate.isAfter(existingDate));
    }

    private String groupKey(BankTransaction transaction) {
        String raw = rawText(transaction);

        Optional<KnownMerchants.Match> known =
                KnownMerchants.match(raw, raw);

        if (known.isPresent()) {
            return "prov:" + known.get().providerKey();
        }

        if (KnownMerchants.isGenericPaymentWrapper(raw)) {
            String withoutWrapper = removePaymentWrappers(raw);
            String normalized = normalizeMerchantText(withoutWrapper);
            String token = bestUnknownToken(normalized);
            return token == null ? normalized : token;
        }

        String normalized = normalizeMerchantText(raw);

        if (normalized.isBlank()) return "";

        String token = bestUnknownToken(normalized);
        return token == null ? normalized : token;
    }

    private String removePaymentWrappers(String raw) {
        return safe(raw)
                .replaceAll("(?i)apple\\s*pay", " ")
                .replaceAll("(?i)google\\s*pay", " ")
                .replaceAll("(?i)samsung\\s*pay", " ")
                .replaceAll("(?i)paypal", " ")
                .replaceAll("(?i)proprietarybanktransactiontext", " ")
                .replaceAll("(?i)visa\\s*varekj(ø|o)p", " ")
                .replaceAll("(?i)mastercard\\s*varekj(ø|o)p", " ");
    }

    private String bestUnknownToken(String normalized) {
        if (normalized == null || normalized.isBlank()) return null;

        List<String> tokens = Arrays.stream(normalized.split("\\s+"))
                .map(String::trim)
                .filter(token -> token.length() >= 4)
                .filter(token -> !GENERIC_TOKENS.contains(token))
                .limit(3)
                .toList();

        if (tokens.isEmpty()) return null;
        return String.join(" ", tokens);
    }

    private boolean isNoise(BankTransaction transaction) {
        String raw = rawText(transaction);

        if (NOISE.matcher(raw).matches()) return true;

        String normalized = normalizeMerchantText(raw);

        return normalized.isBlank()
                || normalized.equals("visa")
                || normalized.equals("mastercard")
                || normalized.equals("applepay")
                || normalized.equals("googlepay")
                || normalized.equals("paypal");
    }

    private IntervalGuess guessInterval(List<LocalDate> dates) {
        if (dates.size() < 2) {
            return new IntervalGuess(null, 0, 0);
        }

        List<Integer> differences = new ArrayList<>();

        for (int i = 1; i < dates.size(); i++) {
            long days = ChronoUnit.DAYS.between(
                    dates.get(i - 1),
                    dates.get(i)
            );

            if (days > 0) differences.add((int) days);
        }

        if (differences.isEmpty()) {
            return new IntervalGuess(null, 0, 0);
        }

        Collections.sort(differences);

        int median = differences.get(differences.size() / 2);

        int weeklyHits = countInRange(differences, 5, 10);
        int monthlyHits = countInRange(differences, 24, 40);
        int quarterlyHits = countInRange(differences, 75, 105);
        int yearlyHits = countInRange(differences, 330, 390);

        if (differences.size() == 1) {
            int days = differences.get(0);

            if (days >= 5 && days <= 10) {
                return new IntervalGuess("WEEKLY", days, 88);
            }
            if (days >= 20 && days <= 45) {
                return new IntervalGuess("MONTHLY", days, 74);
            }
            if (days >= 75 && days <= 105) {
                return new IntervalGuess("QUARTERLY", days, 74);
            }
            if (days >= 330 && days <= 390) {
                return new IntervalGuess("YEARLY", days, 74);
            }

            return new IntervalGuess(null, 0, 0);
        }

        if (monthlyHits >= 2
                && monthlyHits >= weeklyHits
                && monthlyHits >= quarterlyHits
                && monthlyHits >= yearlyHits) {
            return new IntervalGuess("MONTHLY", median, 92);
        }

        if (weeklyHits >= 2
                && weeklyHits >= quarterlyHits
                && weeklyHits >= yearlyHits) {
            return new IntervalGuess("WEEKLY", median, 90);
        }

        if (quarterlyHits >= 2 && quarterlyHits >= yearlyHits) {
            return new IntervalGuess("QUARTERLY", median, 86);
        }

        if (yearlyHits >= 2) {
            return new IntervalGuess("YEARLY", median, 86);
        }

        if (median >= 5 && median <= 10) {
            return new IntervalGuess("WEEKLY", median, 72);
        }
        if (median >= 20 && median <= 45) {
            return new IntervalGuess("MONTHLY", median, 70);
        }
        if (median >= 75 && median <= 105) {
            return new IntervalGuess("QUARTERLY", median, 70);
        }
        if (median >= 330 && median <= 390) {
            return new IntervalGuess("YEARLY", median, 70);
        }

        return new IntervalGuess(null, 0, 0);
    }

    private int scoreConfidence(
            int occurrences,
            IntervalGuess intervalGuess,
            BigDecimal median,
            BigDecimal mad,
            boolean known,
            LocalDate last,
            LocalDate today
    ) {
        int score = 30;

        if (known) score += 24;

        score += Math.min(24, occurrences * 6);
        score += Math.min(12, intervalGuess.varianceScore / 10);

        BigDecimal relativeVariation = median.signum() == 0
                ? BigDecimal.ONE
                : mad.divide(median, 4, RoundingMode.HALF_UP);

        if (relativeVariation.compareTo(LOW_VARIATION) <= 0) score += 18;
        else if (relativeVariation.compareTo(MEDIUM_VARIATION) <= 0) score += 12;
        else if (relativeVariation.compareTo(HIGH_VARIATION) <= 0) score += 5;
        else score -= 10;

        if ("MONTHLY".equals(intervalGuess.interval)) score += 6;
        if ("YEARLY".equals(intervalGuess.interval)
                || "QUARTERLY".equals(intervalGuess.interval)) {
            score += 2;
        }

        long ageDays = ChronoUnit.DAYS.between(last, today);

        if (ageDays <= 45) score += 7;
        else if (ageDays <= 90) score += 3;
        else if (ageDays > 180) score -= 7;

        return Math.max(0, Math.min(99, score));
    }

    private boolean isWeakUnknownCandidate(
            String displayName,
            List<BankTransaction> group,
            IntervalGuess intervalGuess,
            BigDecimal median,
            BigDecimal mad
    ) {
        if (displayName == null || displayName.isBlank()) return true;
        if (group.size() < 2) return true;

        String normalizedName = norm(displayName);

        if (normalizedName.length() < 4) return true;
        if (KnownMerchants.isGenericPaymentWrapper(normalizedName)) return true;

        BigDecimal relativeVariation = median.signum() == 0
                ? BigDecimal.ONE
                : mad.divide(median, 4, RoundingMode.HALF_UP);

        if (relativeVariation.compareTo(new BigDecimal("0.30")) > 0) {
            return true;
        }

        return !"MONTHLY".equals(intervalGuess.interval)
                && group.size() < 3;
    }

    private int countInRange(
            List<Integer> values,
            int min,
            int max
    ) {
        int count = 0;

        for (int value : values) {
            if (value >= min && value <= max) count++;
        }

        return count;
    }

    private LocalDate addInterval(
            LocalDate date,
            String interval
    ) {
        return switch (interval) {
            case "WEEKLY" -> date.plusWeeks(1);
            case "MONTHLY" -> date.plusMonths(1);
            case "QUARTERLY" -> date.plusMonths(3);
            case "YEARLY" -> date.plusYears(1);
            default -> date.plusMonths(1);
        };
    }

    private LocalDate rollForward(
            LocalDate next,
            String interval,
            LocalDate today
    ) {
        if (next == null) return null;

        LocalDate date = next;
        int guard = 0;

        while (!date.isAfter(today) && guard++ < 500) {
            date = addInterval(date, interval);
        }

        return date;
    }

    private String firstCurrency(List<BankTransaction> group) {
        for (BankTransaction transaction : group) {
            String currency = transaction.getCurrency();

            if (currency != null && !currency.isBlank()) {
                return currency.trim().toUpperCase(Locale.ROOT);
            }
        }

        return "NOK";
    }

    private String rawText(List<BankTransaction> group) {
        StringBuilder builder = new StringBuilder();

        for (BankTransaction transaction : group) {
            builder.append(rawText(transaction)).append(' ');
        }

        return builder.toString();
    }

    private String rawText(BankTransaction transaction) {
        return (
                safe(transaction.getDescription())
                        + " "
                        + safe(transaction.getReference())
        )
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeMerchantText(String raw) {
        String value = safe(raw).toLowerCase(Locale.ROOT);

        value = value.replaceAll("https?://", " ");
        value = value.replaceAll("\\bwww\\.", " ");
        value = value.replaceAll(
                "\\b(apl|pos|visa|mc|mcc|trx|trans|purchase|betaling|kortkjøp|kortkjop)\\b",
                " "
        );
        value = value.replaceAll(
                "\\b(avtalegiro|faktura|e-faktura|efaktura|nettbank|belastning)\\b",
                " "
        );
        value = value.replaceAll(
                "\\b(stockholm|oslo|bergen|trondheim|london|dublin|se|no|dk|fi)\\b",
                " "
        );
        value = value.replaceAll(
                "\\b(proprietarybanktransactiontext|applepay|googlepay|paypal)\\b",
                " "
        );
        value = value.replaceAll("\\d+", " ");
        value = value.replace("*", " ");
        value = value.replace("_", " ");
        value = value.replace("-", " ");
        value = value.replaceAll("[^a-zæøå./ ]", " ");
        value = value.replaceAll("\\s+", " ").trim();

        return value;
    }

    private BigDecimal median(List<BigDecimal> values) {
        List<BigDecimal> sorted = values.stream()
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        if (sorted.isEmpty()) return BigDecimal.ZERO;

        return sorted.get(sorted.size() / 2);
    }

    private BigDecimal medianAbsDev(
            List<BigDecimal> values,
            BigDecimal median
    ) {
        List<BigDecimal> deviations = values.stream()
                .filter(Objects::nonNull)
                .map(value -> value.subtract(median).abs())
                .sorted()
                .toList();

        if (deviations.isEmpty()) return BigDecimal.ZERO;

        return deviations.get(deviations.size() / 2);
    }

    private String prettyName(String key) {
        if (key == null || key.isBlank()) return "Ukjent";

        String cleaned = key
                .replace(".", " ")
                .replace("_", " ")
                .replace("-", " ")
                .replaceAll("\\s+", " ")
                .trim();

        StringBuilder result = new StringBuilder();

        for (String part : cleaned.split(" ")) {
            if (part.isBlank()) continue;

            result.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(' ');
        }

        return result.toString().trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String norm(String value) {
        return safe(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }

        return "";
    }

    private record IntervalGuess(
            String interval,
            int typicalDays,
            int varianceScore
    ) {}
}

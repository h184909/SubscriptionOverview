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

    private final BankTransactionRepository txRepo;
    private final SubscriptionRepository subRepo;
    private final SuggestionDecisionRepository decisionRepo;

    // hvor gammel siste betaling kan være før vi skjuler forslaget
    private static final int MAX_LAST_AGE_DAYS = 365;

    // hvor mye beløp kan variere før vi mister confidence
    private static final BigDecimal LOW_VARIATION = new BigDecimal("0.03");
    private static final BigDecimal MEDIUM_VARIATION = new BigDecimal("0.10");
    private static final BigDecimal HIGH_VARIATION = new BigDecimal("0.20");

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

        List<Subscription> subs = subRepo.findByUserEmailOrderByCreatedAtDesc(userEmail);

        Set<String> subProviderKeys = subs.stream()
                .map(s -> norm(firstNonBlank(s.getProviderKey(), s.getName())))
                .filter(x -> !x.isBlank())
                .collect(Collectors.toSet());

        Set<String> subNames = subs.stream()
                .map(s -> norm(s.getName()))
                .filter(x -> !x.isBlank())
                .collect(Collectors.toSet());

        return all.stream()
                .filter(s -> !blocked.contains(s.getKey()))
                .filter(s -> {
                    String pk = norm(firstNonBlank(s.getProviderKey(), s.getName()));
                    return pk.isBlank() || !subProviderKeys.contains(pk);
                })
                .filter(s -> !subNames.contains(norm(s.getName())))
                .sorted(
                        Comparator.comparingInt(SubscriptionSuggestion::getConfidence).reversed()
                                .thenComparing(SubscriptionSuggestion::getOccurrences, Comparator.reverseOrder())
                                .thenComparing(SubscriptionSuggestion::getLastChargeDate, Comparator.reverseOrder())
                )
                .limit(120)
                .toList();
    }

    public Optional<SubscriptionSuggestion> findOne(String userEmail, String key) {
        return computeSuggestions(userEmail).stream()
                .filter(s -> s.getKey().equals(key))
                .findFirst();
    }

    private List<SubscriptionSuggestion> computeSuggestions(String userEmail) {
        OffsetDateTime after = OffsetDateTime.now().minusMonths(36);

        List<BankTransaction> txs =
                txRepo.findByUserEmailAndTxDateAfterOrderByTxDateAsc(userEmail, after);

        List<BankTransaction> outgoing = txs.stream()
                .filter(t -> t.getAmount() != null && t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .filter(t -> t.getTxDate() != null)
                .filter(t -> !isNoise(t))
                .toList();

        Map<String, List<BankTransaction>> groups =
                outgoing.stream().collect(Collectors.groupingBy(this::groupKey));

        List<SubscriptionSuggestion> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (var entry : groups.entrySet()) {
            String gKey = entry.getKey();
            List<BankTransaction> group = entry.getValue();

            if (group.size() < 2) continue;
            if (gKey == null || gKey.isBlank()) continue;

            boolean known = gKey.startsWith("prov:");

            List<BankTransaction> g = group.stream()
                    .sorted(Comparator.comparing(BankTransaction::getTxDate))
                    .toList();

            List<LocalDate> dates = g.stream()
                    .map(BankTransaction::getTxLocalDate)
                    .filter(Objects::nonNull)
                    .toList();

            if (dates.size() < 2) continue;

            IntervalGuess intervalGuess = guessInterval(dates);
            if (intervalGuess.interval == null) continue;

            LocalDate last = dates.get(dates.size() - 1);
            if (last.isBefore(today.minusDays(MAX_LAST_AGE_DAYS))) continue;

            List<BigDecimal> amounts = g.stream()
                    .map(t -> t.getAmount().abs())
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();

            if (amounts.size() < 2) continue;

            BigDecimal med = median(amounts);
            BigDecimal mad = medianAbsDev(amounts, med);

            LocalDate next = addInterval(last, intervalGuess.interval);
            next = rollForward(next, intervalGuess.interval, today);

            String providerKey;
            String displayName;
            String cancelUrl = null;

            if (known) {
                providerKey = gKey.substring("prov:".length());
                var match = KnownMerchants.match(providerKey, rawText(g));
                displayName = match.map(KnownMerchants.Match::displayName).orElse(prettyName(providerKey));
                cancelUrl = match.map(KnownMerchants.Match::cancelUrl).orElse(null);
            } else {
                providerKey = norm(gKey);
                displayName = prettyName(gKey);
            }

            // ekstra filter: ukjente grupper må ha litt mer substans
            if (!known && isWeakUnknownCandidate(displayName, g, intervalGuess, med, mad)) {
                continue;
            }

            int confidence = scoreConfidence(g.size(), intervalGuess, med, mad, known, last, today);

            String amountKey = med.setScale(0, RoundingMode.HALF_UP).toPlainString();
            String key = providerKey + "|" + intervalGuess.interval + "|" + amountKey;

            result.add(new SubscriptionSuggestion(
                    key,
                    displayName,
                    med.setScale(2, RoundingMode.HALF_UP),
                    firstCurrency(g),
                    intervalGuess.interval,
                    last,
                    next,
                    g.size(),
                    confidence,
                    known,
                    providerKey,
                    cancelUrl
            ));
        }

        return dedupeByProvider(result);
    }

    private List<SubscriptionSuggestion> dedupeByProvider(List<SubscriptionSuggestion> xs) {
        Map<String, SubscriptionSuggestion> best = new LinkedHashMap<>();
        for (SubscriptionSuggestion s : xs) {
            String k = norm(firstNonBlank(s.getProviderKey(), s.getName()));
            SubscriptionSuggestion existing = best.get(k);
            if (existing == null) {
                best.put(k, s);
                continue;
            }

            boolean replace =
                    s.getConfidence() > existing.getConfidence()
                            || (s.getConfidence() == existing.getConfidence() && s.getOccurrences() > existing.getOccurrences())
                            || (s.getConfidence() == existing.getConfidence()
                            && s.getOccurrences() == existing.getOccurrences()
                            && s.getLastChargeDate() != null
                            && existing.getLastChargeDate() != null
                            && s.getLastChargeDate().isAfter(existing.getLastChargeDate()));

            if (replace) best.put(k, s);
        }
        return new ArrayList<>(best.values());
    }

    private String groupKey(BankTransaction t) {
        String raw = rawText(t);
        String normalized = normalizeMerchantText(raw);

        Optional<KnownMerchants.Match> match = KnownMerchants.match(normalized, raw);
        if (match.isPresent()) return "prov:" + match.get().providerKey();

        // fallback på klare merchants
        if (normalized.contains("viaplay")) return "prov:viaplay";
        if (normalized.contains("spotify")) return "prov:spotify";
        if (normalized.contains("netflix")) return "prov:netflix";
        if (normalized.contains("disney")) return "prov:disney_plus";
        if (normalized.contains("tv2") || normalized.contains("tv 2")) return "prov:tv2_play";
        if (normalized.contains("primevideo") || normalized.contains("prime video") || normalized.contains("amazon prime")) {
            return "prov:prime_video";
        }
        if (normalized.contains("apple.com/bill") || normalized.contains("itunes")) {
            return "prov:apple_subscriptions";
        }
        if (normalized.contains("google one")) return "prov:google_one";
        if (normalized.contains("google play")) return "prov:google_play";

        String token = bestUnknownToken(normalized);
        return token == null ? normalized : token;
    }

    private String bestUnknownToken(String normalized) {
        if (normalized == null || normalized.isBlank()) return null;

        for (String p : normalized.split(" ")) {
            String x = p.trim();
            if (x.isBlank()) continue;
            if (x.length() < 4) continue;
            if (GENERIC_TOKENS.contains(x)) continue;
            if (x.contains(".")) return x;
        }

        List<String> tokens = Arrays.stream(normalized.split(" "))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> s.length() >= 4)
                .filter(s -> !GENERIC_TOKENS.contains(s))
                .limit(3)
                .toList();

        if (tokens.isEmpty()) return normalized;
        return String.join(" ", tokens);
    }

    private static final Set<String> GENERIC_TOKENS = Set.of(
            "kortkjop", "kortkjøp", "visa", "mastercard", "debit", "credit", "betaling",
            "purchase", "card", "butikk", "butikkjop", "kjop", "kjøp", "abonnement",
            "subscription", "monthly", "month", "faktura", "avtalegiro", "service"
    );

    private static final Pattern NOISE = Pattern.compile(
            ".*\\b(" +
                    "overf(ø|o)ring|mellom\\s*egne|l(ø|o)nn|skatt|atm|uttak|kontant|refund|tilbakebetaling|" +
                    "vipps\\s*til\\s*privat|straks?overf(ø|o)ring|bankoverf(ø|o)ring|sparing|gebyr|renter|" +
                    "debetrenter|provisjon|kredittkortbetaling|kredittkort\\s*innbetaling|rentebetaling|" +
                    "klarna\\s*innbetaling|avdrag|betaling\\s*av\\s*l(å|a)n" +
                    ")\\b.*",
            Pattern.CASE_INSENSITIVE
    );

    private boolean isNoise(BankTransaction t) {
        String s = rawText(t);
        return NOISE.matcher(s).matches();
    }

    private record IntervalGuess(String interval, int typicalDays, int varianceScore) {}

    private IntervalGuess guessInterval(List<LocalDate> dates) {
        if (dates.size() < 2) return new IntervalGuess(null, 0, 0);

        List<Integer> diffs = new ArrayList<>();
        for (int i = 1; i < dates.size(); i++) {
            long d = ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i));
            if (d > 0) diffs.add((int) d);
        }

        if (diffs.isEmpty()) return new IntervalGuess(null, 0, 0);

        Collections.sort(diffs);
        int median = diffs.get(diffs.size() / 2);

        // tell hvor mange som matcher omtrent samme rytme
        int monthlyHits = countInRange(diffs, 24, 40);
        int weeklyHits = countInRange(diffs, 5, 10);
        int quarterlyHits = countInRange(diffs, 75, 105);
        int yearlyHits = countInRange(diffs, 330, 390);

        // 2 trekk: best-effort
        if (diffs.size() == 1) {
            int d = diffs.get(0);
            if (d >= 5 && d <= 10) return new IntervalGuess("WEEKLY", d, 90);
            if (d >= 20 && d <= 45) return new IntervalGuess("MONTHLY", d, 75);
            if (d >= 75 && d <= 105) return new IntervalGuess("QUARTERLY", d, 75);
            if (d >= 330 && d <= 390) return new IntervalGuess("YEARLY", d, 75);
            return new IntervalGuess(null, 0, 0);
        }

        // foretrekk rytme med flest treff
        if (monthlyHits >= 2 && monthlyHits >= weeklyHits && monthlyHits >= quarterlyHits && monthlyHits >= yearlyHits) {
            return new IntervalGuess("MONTHLY", median, 90);
        }
        if (weeklyHits >= 2 && weeklyHits >= quarterlyHits && weeklyHits >= yearlyHits) {
            return new IntervalGuess("WEEKLY", median, 90);
        }
        if (quarterlyHits >= 2 && quarterlyHits >= yearlyHits) {
            return new IntervalGuess("QUARTERLY", median, 85);
        }
        if (yearlyHits >= 2) {
            return new IntervalGuess("YEARLY", median, 85);
        }

        // fallback
        if (median >= 5 && median <= 10) return new IntervalGuess("WEEKLY", median, 75);
        if (median >= 20 && median <= 45) return new IntervalGuess("MONTHLY", median, 70);
        if (median >= 75 && median <= 105) return new IntervalGuess("QUARTERLY", median, 70);
        if (median >= 330 && median <= 390) return new IntervalGuess("YEARLY", median, 70);

        return new IntervalGuess(null, 0, 0);
    }

    private int countInRange(List<Integer> xs, int min, int max) {
        int n = 0;
        for (int x : xs) if (x >= min && x <= max) n++;
        return n;
    }

    private LocalDate addInterval(LocalDate d, String interval) {
        return switch (interval) {
            case "WEEKLY" -> d.plusWeeks(1);
            case "MONTHLY" -> d.plusMonths(1);
            case "QUARTERLY" -> d.plusMonths(3);
            case "YEARLY" -> d.plusYears(1);
            default -> d.plusMonths(1);
        };
    }

    private LocalDate rollForward(LocalDate next, String interval, LocalDate today) {
        if (next == null) return null;

        LocalDate d = next;
        int guard = 0;
        while (!d.isAfter(today) && guard++ < 500) {
            d = addInterval(d, interval);
        }
        return d;
    }

    private int scoreConfidence(
            int occurrences,
            IntervalGuess ig,
            BigDecimal median,
            BigDecimal mad,
            boolean known,
            LocalDate last,
            LocalDate today
    ) {
        int score = 35;

        if (known) score += 22;

        score += Math.min(24, occurrences * 6);
        score += Math.min(12, ig.varianceScore / 10);

        BigDecimal rel = median.signum() == 0
                ? BigDecimal.ONE
                : mad.divide(median, 4, RoundingMode.HALF_UP);

        if (rel.compareTo(LOW_VARIATION) <= 0) score += 18;
        else if (rel.compareTo(MEDIUM_VARIATION) <= 0) score += 12;
        else if (rel.compareTo(HIGH_VARIATION) <= 0) score += 6;
        else score -= 6;

        if ("MONTHLY".equals(ig.interval)) score += 5;
        if ("YEARLY".equals(ig.interval) || "QUARTERLY".equals(ig.interval)) score += 2;

        long ageDays = ChronoUnit.DAYS.between(last, today);
        if (ageDays <= 45) score += 6;
        else if (ageDays <= 90) score += 3;
        else if (ageDays > 180) score -= 5;

        return Math.max(0, Math.min(99, score));
    }

    private boolean isWeakUnknownCandidate(
            String displayName,
            List<BankTransaction> g,
            IntervalGuess ig,
            BigDecimal median,
            BigDecimal mad
    ) {
        if (displayName == null || displayName.isBlank()) return true;
        if (g.size() < 2) return true;

        String dn = norm(displayName);
        if (dn.length() < 4) return true;

        BigDecimal rel = median.signum() == 0
                ? BigDecimal.ONE
                : mad.divide(median, 4, RoundingMode.HALF_UP);

        // ukjente må være litt mer stabile, ellers mye støy
        return !"MONTHLY".equals(ig.interval) && g.size() < 3
                || rel.compareTo(new BigDecimal("0.35")) > 0;
    }

    private String firstCurrency(List<BankTransaction> g) {
        for (BankTransaction t : g) {
            String c = t.getCurrency();
            if (c != null && !c.isBlank()) return c.trim().toUpperCase(Locale.ROOT);
        }
        return "NOK";
    }

    private String rawText(List<BankTransaction> g) {
        StringBuilder sb = new StringBuilder();
        for (BankTransaction t : g) {
            sb.append(rawText(t)).append(' ');
        }
        return sb.toString();
    }

    private String rawText(BankTransaction t) {
        return (safe(t.getDescription()) + " " + safe(t.getReference())).trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeMerchantText(String raw) {
        String s = safe(raw).toLowerCase(Locale.ROOT);

        s = s.replaceAll("https?://", " ");
        s = s.replaceAll("\\bwww\\.", " ");
        s = s.replaceAll("\\b(apl|pos|visa|mc|mcc|trx|trans|purchase|betaling|kortkjøp|kortkjop)\\b", " ");
        s = s.replaceAll("\\b(avtalegiro|faktura|e-faktura|efaktura|nettbank|belastning)\\b", " ");
        s = s.replaceAll("\\b(stockholm|oslo|bergen|trondheim|london|dublin|se|no|dk|fi)\\b", " ");
        s = s.replaceAll("\\d+", " ");
        s = s.replace("*", " ");
        s = s.replace("_", " ");
        s = s.replace("-", " ");
        s = s.replaceAll("[^a-zæøå./ ]", " ");
        s = s.replaceAll("\\s+", " ").trim();

        return s;
    }

    private BigDecimal median(List<BigDecimal> xs) {
        List<BigDecimal> s = xs.stream()
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        if (s.isEmpty()) return BigDecimal.ZERO;
        return s.get(s.size() / 2);
    }

    private BigDecimal medianAbsDev(List<BigDecimal> xs, BigDecimal med) {
        List<BigDecimal> dev = xs.stream()
                .filter(Objects::nonNull)
                .map(x -> x.subtract(med).abs())
                .sorted()
                .toList();
        if (dev.isEmpty()) return BigDecimal.ZERO;
        return dev.get(dev.size() / 2);
    }

    private String prettyName(String key) {
        if (key == null || key.isBlank()) return "Ukjent";

        String cleaned = key
                .replace(".", " ")
                .replace("_", " ")
                .replace("-", " ")
                .replaceAll("\\s+", " ")
                .trim();

        String[] parts = cleaned.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)))
                    .append(p.substring(1))
                    .append(' ');
        }
        return sb.toString().trim();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String norm(String s) {
        return safe(s).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static String firstNonBlank(String... xs) {
        for (String x : xs) {
            if (x != null && !x.isBlank()) return x;
        }
        return "";
    }
}
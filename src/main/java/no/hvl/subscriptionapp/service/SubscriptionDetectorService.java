package no.hvl.subscriptionapp.service;

import no.hvl.subscriptionapp.domain.BankTransaction;
import no.hvl.subscriptionapp.domain.Subscription;
import no.hvl.subscriptionapp.repository.BankTransactionRepository;
import no.hvl.subscriptionapp.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubscriptionDetectorService {

    private final BankTransactionRepository txRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final ProviderMatcherService matcher;

    public SubscriptionDetectorService(
            BankTransactionRepository txRepo,
            SubscriptionRepository subscriptionRepo,
            ProviderMatcherService matcher
    ) {
        this.txRepo = txRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.matcher = matcher;
    }

    // Juster disse hvis du vil
    private static final int LOOKBACK_DAYS = 420;          // hvor langt tilbake vi analyserer
    private static final int STALE_AFTER_DAYS = 120;       // hvis siste trekk er eldre enn dette => ikke foreslå (for månedlig)
    private static final int GRACE_DAYS_FOR_NEXT = 14;     // “neste” kan være litt bakover pga variasjon

    // nøkkelord som typisk IKKE er abonnement
    private static final List<String> EXCLUDE_KEYWORDS = List.of(
            "vipps", "overforing", "overføring", "straksbetaling", "lonn", "lønn",
            "renter", "gebyr", "provisjon", "debetrenter", "kredittkort innbetaling",
            "til:", "fra:" // ofte personoverføringer
    );

    public List<SubscriptionSuggestion> detect(String userEmail) {
        LocalDate today = LocalDate.now();

        OffsetDateTime after = today.minusDays(LOOKBACK_DAYS).atStartOfDay().atOffset(ZoneOffset.UTC);
        List<BankTransaction> txs = txRepo.findByUserEmailAndTxDateAfterOrderByTxDateAsc(userEmail, after);

        // aktive subscriptions -> brukes for å filtrere bort “godtatt”
        List<Subscription> subs = subscriptionRepo.findByUserEmailOrderByCreatedAtDesc(userEmail);
        Set<String> activeProviderKeys = subs.stream()
                .filter(Subscription::isActive)
                .map(s -> matcher.match(s.getName()))
                .filter(ProviderMatcherService.Match::knownProvider)
                .map(ProviderMatcherService.Match::providerKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> activeNameKeys = subs.stream()
                .filter(Subscription::isActive)
                .map(Subscription::getName)
                .map(ProviderMatcherService::normalizeMerchantKey)
                .collect(Collectors.toSet());

        // 1) prefilter: kun negative beløp (utgifter) og ikke støy
        List<TxLite> candidates = new ArrayList<>();
        for (BankTransaction t : txs) {
            if (t.getAmount() == null) continue;
            if (t.getAmount().compareTo(BigDecimal.ZERO) >= 0) continue;

            String raw = safe(t.getDescription());
            String key = ProviderMatcherService.normalizeMerchantKey(raw);

            if (key.isBlank()) continue;

            boolean known = matcher.match(raw).knownProvider();

            // støy-filter (men ikke for kjente providers – de får lov å slippe gjennom)
            if (!known && looksLikeNoise(raw, key)) {
                continue;
            }

            candidates.add(new TxLite(
                    t.getTxDate().toLocalDate(),
                    raw,
                    key,
                    t.getAmount().abs(),
                    safe(t.getCurrency())
            ));
        }

        // 2) group by providerKey (hvis kjent) ellers merchantKey
        Map<String, List<TxLite>> groups = new HashMap<>();
        Map<String, ProviderMatcherService.Match> groupMatch = new HashMap<>();

        for (TxLite tx : candidates) {
            ProviderMatcherService.Match m = matcher.match(tx.rawDescription);
            String groupKey = (m.knownProvider() && m.providerKey() != null)
                    ? ("p:" + m.providerKey())
                    : ("m:" + tx.merchantKey);

            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(tx);
            groupMatch.putIfAbsent(groupKey, m);
        }

        // 3) build suggestions
        List<SubscriptionSuggestion> out = new ArrayList<>();

        for (var e : groups.entrySet()) {
            String gk = e.getKey();
            List<TxLite> list = e.getValue();
            if (list.size() < 2) continue;

            list.sort(Comparator.comparing(a -> a.date));
            ProviderMatcherService.Match m = groupMatch.get(gk);

            // ekskluder hvis allerede aktivt abonnement
            if (m.knownProvider() && m.providerKey() != null && activeProviderKeys.contains(m.providerKey())) {
                continue;
            }
            if (!m.knownProvider() && activeNameKeys.contains(ProviderMatcherService.normalizeMerchantKey(m.displayName()))) {
                continue;
            }

            // beregn intervall via median av dag-differanser
            List<Integer> diffs = new ArrayList<>();
            for (int i = 1; i < list.size(); i++) {
                diffs.add((int) (list.get(i).date.toEpochDay() - list.get(i - 1).date.toEpochDay()));
            }

            int medianDiff = medianInt(diffs);
            Interval interval = classifyInterval(medianDiff);
            if (interval == Interval.UNKNOWN) continue;

            // “stabilitet”: hvor mange diffs ligger nær median?
            int tol = interval.toleranceDays;
            long stableCount = diffs.stream().filter(d -> Math.abs(d - medianDiff) <= tol).count();
            double stability = diffs.isEmpty() ? 0 : (stableCount * 1.0 / diffs.size());

            // beløpsstabilitet (median og relativ variasjon)
            BigDecimal medianAmount = medianAmount(list.stream().map(x -> x.amount).collect(Collectors.toList()));
            double relVar = relativeVariation(list, medianAmount);

            // krav: kjente providers kan være litt “løsere”
            double minStability = m.knownProvider() ? 0.45 : 0.65;
            double maxRelVar = m.knownProvider() ? 0.35 : 0.18;

            if (stability < minStability) continue;
            if (relVar > maxRelVar) continue;

            LocalDate last = list.get(list.size() - 1).date;
            LocalDate next = last.plusDays(medianDiff);

            // recency-filter: ikke foreslå ting som er “døde”
            if (interval == Interval.MONTHLY || interval == Interval.WEEKLY || interval == Interval.BIWEEKLY) {
                if (last.isBefore(today.minusDays(STALE_AFTER_DAYS))) continue;
                if (next.isBefore(today.minusDays(GRACE_DAYS_FOR_NEXT))) continue;
            }
            if (interval == Interval.YEARLY) {
                if (last.isBefore(today.minusDays(500))) continue;
            }

            int confidence = score(list.size(), stability, relVar, m.knownProvider());

            String name = m.displayName();
            String currency = list.get(list.size() - 1).currency;

            out.add(new SubscriptionSuggestion(
                    gk, // key
                    name,
                    medianAmount.setScale(2, RoundingMode.HALF_UP),
                    currency,
                    interval.label,
                    last,
                    next,
                    list.size(),
                    confidence,
                    m.knownProvider(),
                    m.providerKey(),
                    m.cancelUrl()
            ));
        }

        // sort: known først, så confidence
        out.sort(Comparator
                .comparing(SubscriptionSuggestion::isKnownProvider).reversed()
                .thenComparing(SubscriptionSuggestion::getConfidence).reversed()
                .thenComparing(SubscriptionSuggestion::getOccurrences).reversed()
        );

        // topp N (så UI ikke drukner)
        if (out.size() > 50) return out.subList(0, 50);
        return out;
    }

    private static boolean looksLikeNoise(String raw, String key) {
        String a = raw.toLowerCase(Locale.ROOT);
        String k = key.toLowerCase(Locale.ROOT);

        for (String w : EXCLUDE_KEYWORDS) {
            if (a.contains(w) || k.contains(w)) return true;
        }

        // Personoverføringer: “fra navn” / “til navn” uten firma-indikator
        if (k.startsWith("fra ") || k.startsWith("til ")) return true;

        // Typiske avgifter/renter (filene dine har mye av dette)
        if (a.contains("sms varsling") || a.contains("pris efaktura") || a.contains("arspris") || a.contains("årspris")) return true;

        return false;
    }

    private enum Interval {
        WEEKLY("WEEKLY", 7, 1),
        BIWEEKLY("BIWEEKLY", 14, 2),
        MONTHLY("MONTHLY", 30, 3),
        QUARTERLY("QUARTERLY", 91, 7),
        YEARLY("YEARLY", 365, 14),
        UNKNOWN("UNKNOWN", 0, 0);

        final String label;
        final int approxDays;
        final int toleranceDays;

        Interval(String label, int approxDays, int toleranceDays) {
            this.label = label;
            this.approxDays = approxDays;
            this.toleranceDays = toleranceDays;
        }
    }

    private static Interval classifyInterval(int d) {
        if (between(d, 6, 8)) return Interval.WEEKLY;
        if (between(d, 12, 16)) return Interval.BIWEEKLY;
        if (between(d, 27, 33)) return Interval.MONTHLY;
        if (between(d, 80, 105)) return Interval.QUARTERLY;
        if (between(d, 350, 380)) return Interval.YEARLY;
        return Interval.UNKNOWN;
    }

    private static boolean between(int v, int a, int b) {
        return v >= a && v <= b;
    }

    private static int score(int occurrences, double stability, double relVar, boolean known) {
        int c = 0;
        c += Math.min(occurrences, 12) * 6;                 // opptil ~72
        c += (int) Math.round(stability * 20);              // opptil 20
        c += (int) Math.round((1.0 - Math.min(relVar, 1.0)) * 10); // opptil 10
        if (known) c += 10;
        if (c > 99) c = 99;
        if (c < 1) c = 1;
        return c;
    }

    private static int medianInt(List<Integer> xs) {
        if (xs == null || xs.isEmpty()) return 0;
        List<Integer> s = new ArrayList<>(xs);
        s.sort(Integer::compareTo);
        int mid = s.size() / 2;
        return (s.size() % 2 == 1) ? s.get(mid) : (s.get(mid - 1) + s.get(mid)) / 2;
    }

    private static BigDecimal medianAmount(List<BigDecimal> xs) {
        if (xs == null || xs.isEmpty()) return BigDecimal.ZERO;
        List<BigDecimal> s = new ArrayList<>(xs);
        s.sort(Comparator.naturalOrder());
        int mid = s.size() / 2;
        if (s.size() % 2 == 1) return s.get(mid);
        return s.get(mid - 1).add(s.get(mid)).divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
    }

    private static double relativeVariation(List<TxLite> list, BigDecimal median) {
        if (median == null || median.compareTo(BigDecimal.ZERO) == 0) return 1.0;
        double m = median.doubleValue();
        double avgAbsDiff = 0.0;
        for (TxLite t : list) {
            avgAbsDiff += Math.abs(t.amount.doubleValue() - m);
        }
        avgAbsDiff /= Math.max(1, list.size());
        return avgAbsDiff / m;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private record TxLite(LocalDate date, String rawDescription, String merchantKey, BigDecimal amount, String currency) {}
}

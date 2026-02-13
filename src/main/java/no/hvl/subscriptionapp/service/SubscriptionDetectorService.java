package no.hvl.subscriptionapp.service;

import no.hvl.subscriptionapp.domain.BankTransaction;
import no.hvl.subscriptionapp.domain.SuggestionDecision;
import no.hvl.subscriptionapp.domain.Subscription;
import no.hvl.subscriptionapp.repository.BankTransactionRepository;
import no.hvl.subscriptionapp.repository.SuggestionDecisionRepository;
import no.hvl.subscriptionapp.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SubscriptionDetectorService {

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

        List<Subscription> subs = subRepo.findByUserEmailOrderByCreatedAtDesc(userEmail);

        Set<String> subProviderKeys = subs.stream()
                .map(s -> norm(firstNonBlank(s.getProviderKey(), s.getName())))
                .collect(Collectors.toSet());

        return all.stream()
                .filter(s -> !blocked.contains(s.getKey()))
                .filter(s -> s.getProviderKey() == null || !subProviderKeys.contains(norm(s.getProviderKey())))
                .sorted(Comparator.comparingInt(SubscriptionSuggestion::getConfidence).reversed())
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
                .filter(t -> !isNoise(t))
                .toList();

        Map<String, List<BankTransaction>> groups =
                outgoing.stream().collect(Collectors.groupingBy(this::groupKey));

        List<SubscriptionSuggestion> result = new ArrayList<>();

        for (var entry : groups.entrySet()) {
            String gKey = entry.getKey();
            List<BankTransaction> g = entry.getValue();

            boolean known = gKey.startsWith("prov:");

            // ✅ Endring: 2 forekomster er nok
            if (g.size() < 2) continue;

            g = g.stream().sorted(Comparator.comparing(BankTransaction::getTxDate)).toList();

            List<BigDecimal> amounts = g.stream().map(t -> t.getAmount().abs()).toList();
            BigDecimal med = median(amounts);
            BigDecimal mad = medianAbsDev(amounts, med);

            List<LocalDate> dates = g.stream().map(t -> t.getTxDate().toLocalDate()).toList();

            IntervalGuess ig = guessInterval(dates);
            if (ig.interval == null) continue;

            LocalDate last = dates.get(dates.size() - 1);
            LocalDate next = addInterval(last, ig.interval);

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

            int confidence = scoreConfidence(g.size(), ig, med, mad, known);

            String key = providerKey + "|" + ig.interval + "|" +
                    med.setScale(0, RoundingMode.HALF_UP).toPlainString();

            result.add(new SubscriptionSuggestion(
                    key,
                    displayName,
                    med.setScale(2, RoundingMode.HALF_UP),
                    firstCurrency(g),
                    ig.interval,
                    last,
                    next,
                    g.size(),
                    confidence,
                    known,
                    providerKey,
                    cancelUrl
            ));
        }

        return result;
    }

    // ---------- grouping / heuristikk ----------

    private String groupKey(BankTransaction t) {
        String raw = (safe(t.getDescription()) + " " + safe(t.getReference())).toLowerCase(Locale.ROOT);

        // prøv kjente leverandører først (samler Netflix/Spotify osv)
        Optional<KnownMerchants.Match> match = KnownMerchants.match(raw, raw);
        if (match.isPresent()) return "prov:" + match.get().providerKey();

        // normaliser "ukjent" uten å kappe til 4 ord
        raw = raw.replaceAll("\\b(kortkjøp|trans\\s*type|sms\\s*varsling|vipps|straksoverføring|avtalegiro|faktura)\\b", " ");
        raw = raw.replaceAll("\\b(provisjon|renter|debetrenter|gebyr|kredittkort)\\b", " ");
        raw = raw.replaceAll("\\d+", " ");
        raw = raw.replaceAll("[^a-zæøå. ]", " ");
        raw = raw.replaceAll("\\s+", " ").trim();

        // prøv å bruke domenet hvis det finnes
        for (String p : raw.split(" ")) {
            if (p.contains(".") && p.length() >= 4) return p;
        }

        return raw;
    }

    private static final Pattern NOISE =
            Pattern.compile(".*\\b(overføring|mellom\\s*egne|lønn|skatt|atm|uttak|kontant|refund|tilbakebetaling)\\b.*",
                    Pattern.CASE_INSENSITIVE);

    private boolean isNoise(BankTransaction t) {
        String s = (safe(t.getDescription()) + " " + safe(t.getReference())).toLowerCase(Locale.ROOT);
        return NOISE.matcher(s).matches();
    }

    private record IntervalGuess(String interval) {}

    private IntervalGuess guessInterval(List<LocalDate> dates) {
        if (dates.size() < 2) return new IntervalGuess(null);

        // bygg diff-liste
        List<Integer> diffs = new ArrayList<>();
        for (int i = 1; i < dates.size(); i++) {
            int d = (int) Duration.between(
                    dates.get(i - 1).atStartOfDay(),
                    dates.get(i).atStartOfDay()
            ).toDays();
            if (d > 0) diffs.add(d);
        }
        if (diffs.isEmpty()) return new IntervalGuess(null);

        diffs.sort(Integer::compareTo);

        // ✅ NYTT: hvis vi bare har 2 forekomster, bruk "best effort"
        if (diffs.size() == 1) {
            int d = diffs.get(0);

            if (d >= 5 && d <= 12) return new IntervalGuess("WEEKLY");
            // mer tolerant monthly (Viaplay/streaming kan variere litt pga betalingsdato)
            if (d >= 15 && d <= 75) return new IntervalGuess("MONTHLY");
            if (d >= 70 && d <= 120) return new IntervalGuess("QUARTERLY");
            if (d >= 300 && d <= 430) return new IntervalGuess("YEARLY");

            return new IntervalGuess(null);
        }

        // 3+ forekomster: median
        int median = diffs.get(diffs.size() / 2);

        if (median >= 6 && median <= 10) return new IntervalGuess("WEEKLY");
        // ✅ litt bredere her også
        if (median >= 18 && median <= 75) return new IntervalGuess("MONTHLY");
        if (median >= 70 && median <= 120) return new IntervalGuess("QUARTERLY");
        if (median >= 300 && median <= 430) return new IntervalGuess("YEARLY");

        return new IntervalGuess(null);
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

    private int scoreConfidence(int occurrences, IntervalGuess ig, BigDecimal median, BigDecimal mad, boolean known) {
        int score = 40;
        if (known) score += 25;

        score += Math.min(25, occurrences * 7);

        BigDecimal rel = median.signum() == 0 ? BigDecimal.ONE : mad.divide(median, 4, RoundingMode.HALF_UP);
        if (rel.compareTo(new BigDecimal("0.03")) <= 0) score += 20;
        else if (rel.compareTo(new BigDecimal("0.08")) <= 0) score += 12;
        else score += 5;

        if ("MONTHLY".equals(ig.interval)) score += 5;

        return Math.max(0, Math.min(99, score));
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
        for (var t : g) {
            if (t.getDescription() != null) sb.append(t.getDescription()).append(" ");
            if (t.getReference() != null) sb.append(t.getReference()).append(" ");
        }
        return sb.toString();
    }

    private BigDecimal median(List<BigDecimal> xs) {
        List<BigDecimal> s = xs.stream().filter(Objects::nonNull).sorted().toList();
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
        String[] p = key.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : p) {
            if (w.isBlank()) continue;
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
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
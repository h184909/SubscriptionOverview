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
import java.time.*;
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

    // --- Offentlig API ---

    public List<SubscriptionSuggestion> detect(String userEmail) {
        List<SubscriptionSuggestion> all = computeSuggestions(userEmail);

        // fjern accepted/rejected
        Set<String> blocked = decisionRepo.findByUserEmail(userEmail).stream()
                .map(SuggestionDecision::getSuggestionKey)
                .collect(Collectors.toSet());

        // fjern de som allerede er abonnement
        List<Subscription> subs = subRepo.findByUserEmailOrderByCreatedAtDesc(userEmail);

        Set<String> subNames = subs.stream()
                .map(s -> norm(s.getName()))
                .collect(Collectors.toSet());

        // viktig: bruk providerKey når den finnes, ellers fall tilbake til navn
        Set<String> subProviderKeys = subs.stream()
                .map(s -> norm(firstNonBlank(s.getProviderKey(), s.getName())))
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();

        return all.stream()
                .filter(s -> !blocked.contains(s.getKey()))
                .filter(s -> s.getProviderKey() == null || !subProviderKeys.contains(norm(s.getProviderKey())))
                .filter(s -> !subNames.contains(norm(s.getName())))
                // fjern “irrelevante” gamle forslag
                .filter(s -> s.getLastChargeDate() != null && !s.getLastChargeDate().isBefore(today.minusDays(180)))
                .filter(s -> s.getNextExpectedDate() == null || !s.getNextExpectedDate().isBefore(today.minusDays(10)))
                .sorted(Comparator.comparingInt(SubscriptionSuggestion::getConfidence).reversed())
                .limit(60)
                .toList();
    }

    /** Brukes av controller (accept) for å finne ett forslag selv om listen er filtrert/sortert */
    public Optional<SubscriptionSuggestion> findOne(String userEmail, String key) {
        return computeSuggestions(userEmail).stream().filter(s -> s.getKey().equals(key)).findFirst();
    }

    // --- Intern: beregning ---

    private List<SubscriptionSuggestion> computeSuggestions(String userEmail) {
        OffsetDateTime after = OffsetDateTime.now().minusMonths(24);
        List<BankTransaction> txs = txRepo.findByUserEmailAndTxDateAfterOrderByTxDateAsc(userEmail, after);

        // kun “utgående” (abonnement trekk)
        List<BankTransaction> outgoing = txs.stream()
                .filter(t -> t.getAmount() != null && t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .filter(t -> !isNoise(t))
                .toList();

        // ✅ NYTT: grupper på KnownMerchants.providerKey når den matcher (samler “Netflix.com Los Gatos” + “NETFLIX.COM Amsterdam”)
        Map<String, List<BankTransaction>> groups = outgoing.stream()
                .collect(Collectors.groupingBy(this::groupKey, LinkedHashMap::new, Collectors.toList()));

        LocalDate today = LocalDate.now();
        List<SubscriptionSuggestion> result = new ArrayList<>();

        for (var e : groups.entrySet()) {
            String gKey = e.getKey();
            List<BankTransaction> g = e.getValue();

            // kjent provider? (groupKey starter med "prov:")
            boolean known = gKey.startsWith("prov:");

            // ✅ NYTT: kjente providers kan slippe gjennom med 2 forekomster
            if (known) {
                if (g.size() < 2) continue;
            } else {
                if (g.size() < 3) continue;
            }

            // sorter på dato
            g = g.stream().sorted(Comparator.comparing(BankTransaction::getTxDate)).toList();

            // amount stats (bruk absolutt)
            List<BigDecimal> amounts = g.stream().map(t -> t.getAmount().abs()).toList();
            BigDecimal med = median(amounts);
            BigDecimal mad = medianAbsDev(amounts, med);

            // datoer / intervall
            List<LocalDate> dates = g.stream().map(t -> t.getTxDate().toLocalDate()).toList();
            IntervalGuess ig = guessInterval(dates);

            // ✅ NYTT: kjente providers får en “best effort” MONTHLY hvis vi har minst 2 trekk ~ månedsavstand
            if (ig.interval == null && known && dates.size() >= 2) {
                int d = (int) Duration.between(dates.get(dates.size() - 2).atStartOfDay(), dates.get(dates.size() - 1).atStartOfDay()).toDays();
                if (d >= 20 && d <= 45) ig = new IntervalGuess("MONTHLY", d, 0);
            }

            if (ig.interval == null) continue;

            LocalDate last = dates.get(dates.size() - 1);
            LocalDate next = addInterval(last, ig.interval);

            // filtrer veldig gammel “last”
            if (last.isBefore(today.minusDays(240))) continue;

            // confidence
            int conf = scoreConfidence(g.size(), ig, med, mad, gKey);

            // display + provider info
            String raw = rawText(g);

            // hvis gKey er "prov:netflix" osv:
            String providerKey;
            String displayName;
            String cancelUrl;

            if (known) {
                providerKey = gKey.substring("prov:".length());
                // finn “displayName/cancelUrl” fra KnownMerchants (kjør match på raw)
                var match = KnownMerchants.match(providerKey, raw);
                displayName = match.map(KnownMerchants.Match::displayName).orElse(prettyName(providerKey));
                cancelUrl = match.map(KnownMerchants.Match::cancelUrl).orElse(null);
            } else {
                // ukjent: bruk pen merchantKey
                providerKey = norm(prettyName(gKey));
                displayName = prettyName(gKey);
                cancelUrl = null;
            }

            // thresholds
            if (!known && conf < 70) continue;
            if (known && conf < 45) continue;

            // stabil key (avrundet beløp)
            BigDecimal rounded = med.setScale(0, RoundingMode.HALF_UP);
            String key = providerKey + "|" + ig.interval + "|" + rounded.toPlainString();

            result.add(new SubscriptionSuggestion(
                    key,
                    displayName,
                    med.setScale(2, RoundingMode.HALF_UP),
                    firstCurrency(g),
                    ig.interval,
                    last,
                    next,
                    g.size(),
                    conf,
                    known,
                    providerKey,
                    cancelUrl
            ));
        }

        return result;
    }

    // ✅ gruppér på providerKey hvis kjent, ellers merchantKey
    private String groupKey(BankTransaction t) {
        String mKey = merchantKey(t);
        String raw = (safe(t.getDescription()) + " " + safe(t.getReference())).toLowerCase(Locale.ROOT);

        // prøv match på merchantKey + råtekst
        Optional<KnownMerchants.Match> match = KnownMerchants.match(mKey, raw);
        if (match.isPresent()) return "prov:" + match.get().providerKey();

        return mKey;
    }

    // --- Heuristikk / hjelpere ---

    private String firstCurrency(List<BankTransaction> g) {
        for (var t : g) {
            if (t.getCurrency() != null && !t.getCurrency().isBlank()) return t.getCurrency().trim().toUpperCase(Locale.ROOT);
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

    private String merchantKey(BankTransaction t) {
        String s = safe(t.getDescription()) + " " + safe(t.getReference());
        s = s.toLowerCase(Locale.ROOT);

        // fjern støyord
        s = s.replaceAll("\\b(kortkjøp|trans\\s*type|sms\\s*varsling|vipps|straksoverføring|avtalegiro|faktura)\\b", " ");
        s = s.replaceAll("\\b(provisjon|renter|debetrenter|gebyr|kredittkort)\\b", " ");
        s = s.replaceAll("\\b(til\\s*betalt|fra\\s*betalt)\\b", " ");

        // fjern tall/koder
        s = s.replaceAll("\\d{2,}", " ");
        s = s.replaceAll("[^a-zæøå ]+", " ");
        s = s.replaceAll("\\s+", " ").trim();

        // behold de første 4 ordene (stabil)
        String[] parts = s.split(" ");
        if (parts.length <= 4) return s;
        return String.join(" ", Arrays.copyOfRange(parts, 0, 4));
    }

    private static final Pattern NOISE =
            Pattern.compile(".*\\b(overføring|mellom\\s*egne|lønn|skatt|atm|uttak|kontant|refund|tilbakebetaling)\\b.*",
                    Pattern.CASE_INSENSITIVE);

    private boolean isNoise(BankTransaction t) {
        String s = (safe(t.getDescription()) + " " + safe(t.getReference())).toLowerCase(Locale.ROOT);
        if (NOISE.matcher(s).matches()) return true;
        return false;
    }

    private record IntervalGuess(String interval, int medianDays, int spreadDays) {}

    private IntervalGuess guessInterval(List<LocalDate> dates) {
        if (dates.size() < 2) return new IntervalGuess(null, 0, 0);

        List<Integer> diffs = new ArrayList<>();
        for (int i = 1; i < dates.size(); i++) {
            int d = (int) Duration.between(dates.get(i - 1).atStartOfDay(), dates.get(i).atStartOfDay()).toDays();
            if (d > 0) diffs.add(d);
        }
        if (diffs.isEmpty()) return new IntervalGuess(null, 0, 0);

        diffs.sort(Integer::compareTo);
        int med = diffs.get(diffs.size() / 2);
        int spread = diffs.get(diffs.size() - 1) - diffs.get(0);

        if (med >= 6 && med <= 8) return new IntervalGuess("WEEKLY", med, spread);
        if (med >= 20 && med <= 45) return new IntervalGuess("MONTHLY", med, spread);
        if (med >= 80 && med <= 110) return new IntervalGuess("QUARTERLY", med, spread);
        if (med >= 330 && med <= 400) return new IntervalGuess("YEARLY", med, spread);

        return new IntervalGuess(null, med, spread);
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

    private int scoreConfidence(int occurrences, IntervalGuess ig, BigDecimal median, BigDecimal mad, String groupKey) {
        int score = 0;

        score += Math.min(45, occurrences * 8); // litt mer vekt på forekomster
        score += (ig.spreadDays <= 5 ? 35 : ig.spreadDays <= 12 ? 22 : 10);

        // kjente providers (prov:*) får litt bonus
        if (groupKey.startsWith("prov:")) score += 12;

        BigDecimal rel = median.signum() == 0 ? BigDecimal.ONE : mad.divide(median, 4, RoundingMode.HALF_UP);
        if (rel.compareTo(new BigDecimal("0.02")) <= 0) score += 22;
        else if (rel.compareTo(new BigDecimal("0.07")) <= 0) score += 15;
        else score += 6;

        return Math.max(0, Math.min(99, score));
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

    private static String safe(String s) { return s == null ? "" : s.trim(); }
    private static String norm(String s) { return safe(s).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim(); }

    private static String firstNonBlank(String... xs) {
        for (String x : xs) if (x != null && !x.isBlank()) return x;
        return "";
    }
}
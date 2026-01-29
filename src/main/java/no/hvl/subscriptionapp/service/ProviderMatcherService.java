package no.hvl.subscriptionapp.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class ProviderMatcherService {

    // Fjerner “støy” typisk i norske bankbeskrivelser
    private static final Pattern NOISE = Pattern.compile(
            "\\b(visa\\s*varekj[oø]p|varekj[oø]p|str[oø]mmetjenester|internettjenester|avtalegiro|efaktura|vipps)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TRAILING_COUNTRY = Pattern.compile("\\s+[A-Z]{2}\\s*$");
    private static final Pattern MULTISPACE = Pattern.compile("\\s+");
    private static final Pattern DIGIT_HEAVY = Pattern.compile("\\b\\d{3,}\\b"); // fjerner lange id’er

    public record Match(
            boolean knownProvider,
            String providerKey,
            String displayName,
            String cancelUrl,
            String merchantKey
    ) {}

    public Match match(String rawDescription) {
        String merchantKey = normalizeMerchantKey(rawDescription);

        Optional<KnownMerchants.Match> known = KnownMerchants.match(merchantKey, rawDescription);
        if (known.isPresent()) {
            var m = known.get();
            return new Match(true, m.providerKey(), m.displayName(), m.cancelUrl(), merchantKey);
        }

        // Ukjent: vi bruker merchantKey som “navn”
        String display = merchantKey.isBlank() ? "Ukjent" : capitalizeNice(merchantKey);
        return new Match(false, null, display, null, merchantKey);
    }

    public static String normalizeMerchantKey(String raw) {
        if (raw == null) return "";

        String s = raw.trim();

        // ta bort diakritikk/rare tegn og lower
        s = Normalizer.normalize(s, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "");
        s = s.toLowerCase(Locale.ROOT);

        // bort med "noise"-ord
        s = NOISE.matcher(s).replaceAll(" ");

        // bort med lange tall-IDer
        s = DIGIT_HEAVY.matcher(s).replaceAll(" ");

        // bort med “SE/NL/US” på slutten osv
        s = TRAILING_COUNTRY.matcher(s.toUpperCase(Locale.ROOT)).replaceAll("").toLowerCase(Locale.ROOT);

        // dropp ekstra tegn som ofte varierer
        s = s.replaceAll("[^a-z0-9&+./\\- ]", " ");
        s = MULTISPACE.matcher(s).replaceAll(" ").trim();

        // ofte holder første 6-8 ord
        String[] parts = s.split(" ");
        if (parts.length > 8) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                if (i > 0) sb.append(' ');
                sb.append(parts[i]);
            }
            s = sb.toString();
        }

        return s.trim();
    }

    private static String capitalizeNice(String s) {
        if (s == null || s.isBlank()) return s;
        String[] p = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < p.length; i++) {
            if (p[i].isBlank()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p[i].charAt(0))).append(p[i].substring(1));
        }
        return sb.toString();
    }
}

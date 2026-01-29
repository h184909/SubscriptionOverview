package no.hvl.subscriptionapp.service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Hardkodet katalog over populære abonnementstjenester.
 * - key: stabil id (brukes i matcher/suggestions)
 * - displayName: navnet vi viser i UI
 * - cancelUrl: lenke som vises ETTER at abonnementet er godtatt (i subscriptions.jsp)
 * - patterns: regexer som matcher transaksjonsbeskrivelse
 */
public final class ProviderDirectory {

    public record Provider(
            String key,
            String displayName,
            String cancelUrl,
            List<Pattern> patterns
    ) {}

    private static Provider p(String key, String name, String cancelUrl, String... regexes) {
        List<Pattern> ps = new ArrayList<>();
        for (String r : regexes) {
            ps.add(Pattern.compile(r, Pattern.CASE_INSENSITIVE));
        }
        return new Provider(key, name, cancelUrl, List.copyOf(ps));
    }

    private static final List<Provider> PROVIDERS = List.of(
            // Streaming
            p("netflix", "Netflix", "https://www.netflix.com/cancelplan",
                    ".*\\bnetflix\\b.*"),
            p("spotify", "Spotify", "https://www.spotify.com/account/subscription/",
                    ".*\\bspotify\\b.*"),
            p("disney_plus", "Disney+", "https://www.disneyplus.com/account/subscription",
                    ".*\\bdisney\\s*plus\\b.*", ".*\\bdisney\\+\\b.*"),
            p("hbo_max", "Max / HBO", "https://www.max.com/account",
                    ".*\\bhbo\\b.*", ".*\\bmax\\b.*"),
            p("viaplay", "Viaplay", "https://viaplay.com/no-no/account",
                    ".*\\bviaplay\\b.*"),
            p("amazon_prime", "Amazon Prime", "https://www.amazon.com/amazonprime",
                    ".*\\bamazon\\b.*\\bprime\\b.*", ".*\\bprime\\b.*", ".*\\bamazon\\b.*"),
            p("youtube_premium", "YouTube Premium", "https://www.youtube.com/paid_memberships",
                    ".*\\byoutube\\b.*", ".*\\byoutube\\s*premium\\b.*"),

            // Apple / Google / Microsoft
            p("apple_subscriptions", "Apple (Abonnement)", "https://apps.apple.com/account/subscriptions",
                    ".*\\bapple\\b.*", ".*\\bitunes\\b.*", ".*\\bicloud\\b.*"),
            p("google_subscriptions", "Google (Abonnement)", "https://play.google.com/store/account/subscriptions",
                    ".*\\bgoogle\\b.*", ".*\\bgoogle\\s*one\\b.*", ".*\\bgoogle\\s*play\\b.*", ".*\\bplay\\s*store\\b.*"),
            p("microsoft_services", "Microsoft (Abonnement)", "https://account.microsoft.com/services/",
                    ".*\\bmicrosoft\\b.*", ".*\\bo365\\b.*", ".*\\boffice\\b.*", ".*\\bxbox\\b.*", ".*\\bgame\\s*pass\\b.*"),

            // Software / cloud
            p("adobe", "Adobe", "https://account.adobe.com/plans",
                    ".*\\badobe\\b.*"),
            p("dropbox", "Dropbox", "https://www.dropbox.com/account/billing",
                    ".*\\bdropbox\\b.*"),
            p("github", "GitHub", "https://github.com/settings/billing",
                    ".*\\bgithub\\b.*"),
            p("notion", "Notion", "https://www.notion.so/my-account",
                    ".*\\bnotion\\b.*"),

            // Gaming
            p("playstation_plus", "PlayStation Plus", "https://www.playstation.com/account/subscriptions/",
                    ".*\\bplaystation\\b.*", ".*\\bpsn\\b.*"),
            p("nintendo", "Nintendo", "https://accounts.nintendo.com/",
                    ".*\\bnintendo\\b.*")
    );

    private static final Map<String, Provider> BY_KEY;
    static {
        Map<String, Provider> m = new LinkedHashMap<>();
        for (Provider p : PROVIDERS) m.put(p.key(), p);
        BY_KEY = Collections.unmodifiableMap(m);
    }

    private ProviderDirectory() {}

    public static List<Provider> all() {
        return PROVIDERS;
    }

    public static Optional<Provider> byKey(String key) {
        if (key == null) return Optional.empty();
        return Optional.ofNullable(BY_KEY.get(key));
    }
}

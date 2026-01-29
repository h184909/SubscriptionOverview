package no.hvl.subscriptionapp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * En enkel "katalog" over populære abonnement/leverandører.
 * Match skjer på merchantKey/description (lowercase).
 */
public final class PopularProviders {

    private PopularProviders() {}

    public record Provider(
            String key,          // f.eks. "netflix"
            String displayName,  // f.eks. "Netflix"
            List<Pattern> patterns,
            String cancelUrl,
            String manageUrl
    ) {}

    private static Pattern p(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    private static final List<Provider> PROVIDERS = List.of(
            new Provider(
                    "netflix",
                    "Netflix",
                    List.of(p("netflix")),
                    "https://www.netflix.com/cancelplan",
                    "https://www.netflix.com/YourAccount"
            ),
            new Provider(
                    "spotify",
                    "Spotify",
                    List.of(p("spotify")),
                    "https://www.spotify.com/account/subscription/",
                    "https://www.spotify.com/account/"
            ),
            new Provider(
                    "apple",
                    "Apple (Subscriptions)",
                    List.of(p("apple\\.com"), p("itunes"), p("icloud"), p("apple services")),
                    "https://support.apple.com/en-us/HT202039", // guide: cancel subscriptions
                    "https://appleid.apple.com/"
            ),
            new Provider(
                    "google",
                    "Google (Subscriptions)",
                    List.of(p("google"), p("g\\.co"), p("youtube"), p("google play")),
                    "https://support.google.com/googleplay/answer/7018481",
                    "https://play.google.com/store/account/subscriptions"
            ),
            new Provider(
                    "microsoft",
                    "Microsoft",
                    List.of(p("microsoft"), p("xbox"), p("office"), p("onedrive")),
                    "https://account.microsoft.com/services",
                    "https://account.microsoft.com/"
            ),
            new Provider(
                    "amazon",
                    "Amazon",
                    List.of(p("amazon"), p("prime video"), p("prime")),
                    "https://www.amazon.com/mc/your-account",
                    "https://www.amazon.com/your-account"
            ),
            new Provider(
                    "hbo",
                    "Max / HBO",
                    List.of(p("\\bhbo\\b"), p("\\bmax\\b"), p("hbomax")),
                    "https://help.max.com/us/Answer/Detail/000002521",
                    "https://help.max.com/"
            )
    );

    public static Optional<Provider> match(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        String s = text.toLowerCase(Locale.ROOT);

        for (Provider pr : PROVIDERS) {
            for (Pattern pat : pr.patterns()) {
                if (pat.matcher(s).find()) {
                    return Optional.of(pr);
                }
            }
        }
        return Optional.empty();
    }

    public static List<Provider> all() {
        return new ArrayList<>(PROVIDERS);
    }

    public static Optional<Provider> byKey(String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        String k = key.trim().toLowerCase(Locale.ROOT);
        return PROVIDERS.stream().filter(p -> p.key().equals(k)).findFirst();
    }
}

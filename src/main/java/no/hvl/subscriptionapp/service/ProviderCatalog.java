package no.hvl.subscriptionapp.service;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ProviderCatalog {

    public record Provider(
            String key,
            String displayName,
            List<String> matchContains, // tekstbiter som kan finnes i merchantKey
            String cancelUrl
    ) {}

    // Enkle, trygge lenker (ofte “cancel subscription” / account)
    private final List<Provider> providers = List.of(
            new Provider("netflix", "Netflix", List.of("netflix"), "https://www.netflix.com/CancelPlan"),
            new Provider("spotify", "Spotify", List.of("spotify"), "https://www.spotify.com/account/"),
            new Provider("apple", "Apple (Subscriptions)", List.of("apple.com", "apple", "itunes"), "https://support.apple.com/en-us/118428"),
            new Provider("google", "Google (Subscriptions)", List.of("google", "g.co", "google play", "play.google"), "https://support.google.com/googleplay/answer/7018481"),
            new Provider("youtube", "YouTube Premium", List.of("youtube"), "https://www.youtube.com/paid_memberships"),
            new Provider("amazon", "Amazon / Prime", List.of("amazon", "amzn", "prime"), "https://www.amazon.com/gp/primecentral"),
            new Provider("microsoft", "Microsoft", List.of("microsoft", "msft", "xbox"), "https://account.microsoft.com/services/")
    );

    public Optional<Provider> match(String merchantKey) {
        if (merchantKey == null) return Optional.empty();
        String mk = merchantKey.toLowerCase(Locale.ROOT);

        for (Provider p : providers) {
            for (String token : p.matchContains()) {
                if (mk.contains(token)) return Optional.of(p);
            }
        }
        return Optional.empty();
    }
}

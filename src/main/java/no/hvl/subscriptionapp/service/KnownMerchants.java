package no.hvl.subscriptionapp.service;

import java.util.*;
import java.util.regex.Pattern;

public final class KnownMerchants {

    public record Match(String providerKey, String displayName, String cancelUrl) {}

    private record Rule(String providerKey, Pattern pattern, String displayName, String cancelUrl) {}

    private static final List<Rule> RULES = List.of(
            // Streaming
            rule("netflix", ".*\\bnetflix\\b.*|.*netflix\\.com.*", "Netflix", "https://www.netflix.com/cancelplan"),
            rule("spotify", ".*\\bspotify\\b.*", "Spotify", "https://www.spotify.com/account/subscription/"),
            rule("disney_plus", ".*\\bdisney\\s*plus\\b.*|.*\\bdisney\\+\\b.*", "Disney+", "https://www.disneyplus.com/account/subscription"),
            rule("tv2_play", ".*\\btv\\s*2\\b.*|.*\\btv2\\b.*", "TV 2 Play", "https://play.tv2.no/konto"),
            rule("viaplay", ".*\\bviaplay\\b.*", "Viaplay", "https://viaplay.com/no-no/account"),
            rule("prime_video", ".*\\bprime\\s*video\\b.*|.*primevideo\\..*|.*\\bamazon\\s*prime\\b.*", "Prime Video", "https://www.amazon.com/amazonprime"),
            rule("youtube_premium", ".*\\byoutube\\b.*|.*paid_memberships.*", "YouTube Premium", "https://www.youtube.com/paid_memberships"),

            // Apple / Google
            rule("apple_subscriptions", ".*apple\\.com/bill.*|.*itunes\\.com/bill.*|.*\\bitunes\\b.*|.*\\bicloud\\b.*", "Apple (Abonnement)", "https://apps.apple.com/account/subscriptions"),
            rule("google_play", ".*google\\s*play.*|.*\\bplay\\b.*\\bapps\\b.*|.*play\\.google\\.com.*", "Google Play (Abonnement)", "https://play.google.com/store/account/subscriptions"),

            // Telecom / strøm / lokale (fra filene dine)
            rule("tibber", ".*\\btibber\\b.*", "Tibber", "https://account.tibber.com/"),
            rule("talkmore", ".*\\btalkmore\\b.*", "Talkmore", "https://www.talkmore.no/minside/"),
            rule("kvinnherad_breiband", ".*\\bkvinnherad\\s*breiband\\b.*", "Kvinnherad Breiband", "https://kvinnheradbreiband.no/"),
            rule("flyt", ".*\\bflyt\\b.*", "Flyt (bom/parkering)", "https://flyt.no/"),

            // Software / cloud
            rule("adobe", ".*\\badobe\\b.*", "Adobe", "https://account.adobe.com/plans"),
            rule("dropbox", ".*\\bdropbox\\b.*", "Dropbox", "https://www.dropbox.com/account/billing"),
            rule("github", ".*\\bgithub\\b.*", "GitHub", "https://github.com/settings/billing"),

            // Gaming
            rule("playstation_plus", ".*\\bplaystation\\b.*|.*\\bpsn\\b.*", "PlayStation Plus", "https://www.playstation.com/account/subscriptions/"),
            rule("xbox_gamepass", ".*\\bxbox\\b.*|.*\\bgame\\s*pass\\b.*", "Xbox / Game Pass", "https://account.microsoft.com/services/")
    );

    private static Rule rule(String key, String regex, String displayName, String cancelUrl) {
        return new Rule(key, Pattern.compile(regex, Pattern.CASE_INSENSITIVE), displayName, cancelUrl);
    }

    private KnownMerchants() {}

    public static Optional<Match> match(String merchantKey, String rawDescription) {
        String a = safe(merchantKey);
        String b = safe(rawDescription);
        for (Rule r : RULES) {
            if (r.pattern.matcher(a).matches() || r.pattern.matcher(b).matches()) {
                return Optional.of(new Match(r.providerKey, r.displayName, r.cancelUrl));
            }
        }
        return Optional.empty();
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}

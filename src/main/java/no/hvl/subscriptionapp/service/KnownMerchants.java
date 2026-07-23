package no.hvl.subscriptionapp.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.Locale.ROOT;

public final class KnownMerchants {

    public record Match(
            String providerKey,
            String displayName,
            String cancelUrl,
            String category
    ) {}

    private record Rule(
            String providerKey,
            Pattern pattern,
            String displayName,
            String cancelUrl,
            String category
    ) {}

    private static Rule rule(
            String key,
            String regex,
            String displayName,
            String cancelUrl,
            String category
    ) {
        return new Rule(
                key,
                Pattern.compile(regex, Pattern.CASE_INSENSITIVE),
                displayName,
                cancelUrl,
                category
        );
    }

    private static final List<Rule> RULES = List.of(
            rule("netflix",
                    ".*\\bnetflix\\b.*|.*netflix\\.com.*",
                    "Netflix",
                    "https://www.netflix.com/cancelplan",
                    "Entertainment"),

            rule("spotify",
                    ".*\\bspotify\\b.*|.*spotifyab.*|.*spotify\\s*ab.*",
                    "Spotify",
                    "https://www.spotify.com/account/subscription/",
                    "Entertainment"),

            rule("disney_plus",
                    ".*\\bdisney\\s*plus\\b.*|.*\\bdisney\\+\\b.*|.*disneyplus.*",
                    "Disney+",
                    "https://www.disneyplus.com/account/subscription",
                    "Entertainment"),

            rule("max",
                    ".*\\bhbo\\s*max\\b.*|.*\\bmax\\.com\\b.*|.*\\bmax\\s*streaming\\b.*",
                    "Max",
                    "https://auth.max.com/account",
                    "Entertainment"),

            rule("tv2_play",
                    ".*\\btv\\s*2\\b.*|.*\\btv2\\b.*|.*tv2play.*|.*play\\.tv2.*",
                    "TV 2 Play",
                    "https://play.tv2.no/konto",
                    "Entertainment"),

            rule("viaplay",
                    ".*\\bvia\\s*play\\b.*|.*\\bviaplay\\b.*|.*viaplay\\.(com|no|se|dk).*|.*\\bviaplay\\s*group\\b.*",
                    "Viaplay",
                    "https://viaplay.com/no-no/account",
                    "Entertainment"),

            rule("prime_video",
                    ".*\\bprime\\s*video\\b.*|.*primevideo.*|.*\\bamazon\\s*prime\\b.*|.*amazon.*video.*",
                    "Prime Video",
                    "https://www.amazon.com/amazonprime",
                    "Entertainment"),

            rule("youtube_premium",
                    ".*\\byoutube\\s*premium\\b.*|.*google.*youtube.*|.*paid_memberships.*",
                    "YouTube Premium",
                    "https://www.youtube.com/paid_memberships",
                    "Entertainment"),

            rule("storytel",
                    ".*\\bstorytel\\b.*",
                    "Storytel",
                    "https://www.storytel.com/no/no/account",
                    "Entertainment"),

            rule("bookbeat",
                    ".*\\bbookbeat\\b.*",
                    "BookBeat",
                    "https://www.bookbeat.com/no/account",
                    "Entertainment"),

            rule("audible",
                    ".*\\baudible\\b.*",
                    "Audible",
                    "https://www.audible.com/account/cancel",
                    "Entertainment"),

            rule("telenor",
                    ".*\\btelenor\\b.*",
                    "Telenor",
                    "https://www.telenor.no/kundeservice/",
                    "Telecom"),

            rule("telia",
                    ".*\\btelia\\b.*",
                    "Telia",
                    "https://www.telia.no/kundeservice/",
                    "Telecom"),

            rule("ice",
                    ".*\\bice\\b.*",
                    "Ice",
                    "https://www.ice.no/kundeservice/",
                    "Telecom"),

            rule("talkmore",
                    ".*\\btalkmore\\b.*",
                    "Talkmore",
                    "https://www.talkmore.no/minside/",
                    "Telecom"),

            rule("altibox",
                    ".*\\baltibox\\b.*|.*\\blyse\\b.*altibox.*",
                    "Altibox",
                    "https://www.altibox.no/privat/kundeservice/",
                    "Telecom"),

            rule("chatgpt",
                    ".*\\bopenai\\b.*|.*\\bchatgpt\\b.*",
                    "ChatGPT",
                    "https://chatgpt.com/",
                    "Utilities"),

            rule("apple_subscriptions",
                    ".*apple\\.com/bill.*|.*itunes\\.com/bill.*|.*\\bitunes\\b.*|.*\\bicloud\\b.*|.*apple\\s*services.*|.*apple\\s*media\\s*services.*",
                    "Apple (Abonnement)",
                    "https://apps.apple.com/account/subscriptions",
                    "Utilities"),

            rule("google_play",
                    ".*google\\s*play.*|.*play\\.google\\.com.*|.*google\\s*payment.*",
                    "Google Play (Abonnement)",
                    "https://play.google.com/store/account/subscriptions",
                    "Utilities"),

            rule("google_one",
                    ".*\\bgoogle\\s*one\\b.*|.*google.*storage.*",
                    "Google One",
                    "https://one.google.com/",
                    "Utilities"),

            rule("microsoft_365",
                    ".*\\bmicrosoft\\b.*(365|office).*|.*\\boffice\\s*365\\b.*|.*\\boffice365\\b.*",
                    "Microsoft 365",
                    "https://account.microsoft.com/services/",
                    "Utilities"),

            rule("adobe",
                    ".*\\badobe\\b.*(creative\\s*cloud|acrobat|cc)?.*",
                    "Adobe",
                    "https://account.adobe.com/plans",
                    "Utilities"),

            rule("dropbox",
                    ".*\\bdropbox\\b.*",
                    "Dropbox",
                    "https://www.dropbox.com/account/billing",
                    "Utilities"),

            rule("github",
                    ".*\\bgithub\\b.*",
                    "GitHub",
                    "https://github.com/settings/billing",
                    "Utilities"),

            rule("notion",
                    ".*\\bnotion\\b.*",
                    "Notion",
                    "https://www.notion.so/my-account",
                    "Utilities"),

            rule("slack",
                    ".*\\bslack\\b.*",
                    "Slack",
                    "https://my.slack.com/account/billing",
                    "Utilities"),

            rule("zoom",
                    ".*\\bzoom\\b.*",
                    "Zoom",
                    "https://zoom.us/billing",
                    "Utilities"),

            rule("nordvpn",
                    ".*\\bnordvpn\\b.*|.*\\bnord\\s*vpn\\b.*",
                    "NordVPN",
                    "https://my.nordaccount.com/billing/",
                    "Utilities"),

            rule("surfshark",
                    ".*\\bsurfshark\\b.*",
                    "Surfshark",
                    "https://my.surfshark.com/billing",
                    "Utilities"),

            rule("tibber",
                    ".*\\btibber\\b.*",
                    "Tibber",
                    "https://account.tibber.com/",
                    "Utilities"),

            rule("sats",
                    ".*\\bsats\\b.*",
                    "SATS",
                    "https://www.sats.no/kundeservice/",
                    "Health & Fitness"),

            rule("fresh_fitness",
                    ".*\\bfresh\\s*fitness\\b.*|.*\\bfreshfitness\\b.*",
                    "Fresh Fitness",
                    "https://freshfitness.no/kundeservice/",
                    "Health & Fitness"),

            rule("sammen_trening",
                    ".*\\bsammen\\b.*\\btrening\\b.*|.*\\bsammen\\s*trening\\b.*",
                    "Sammen Trening",
                    "https://sammen.no/no/trening",
                    "Health & Fitness"),

            rule("strava",
                    ".*\\bstrava\\b.*",
                    "Strava",
                    "https://www.strava.com/settings/subscription",
                    "Health & Fitness"),

            rule("aftenposten",
                    ".*\\baftenposten\\b.*|.*\\bschibsted\\b.*aftenposten.*",
                    "Aftenposten",
                    "https://aftenposten.no/kundeservice",
                    "News"),

            rule("vg_plus",
                    ".*\\bvg\\+\\b.*|.*\\bvgpluss\\b.*|.*\\bverdens\\s*gang\\b.*",
                    "VG+",
                    "https://www.vg.no/kundeservice/",
                    "News"),

            rule("dn",
                    ".*\\bdagens\\s*næringsliv\\b.*|.*\\bdn\\b.*abonnement.*",
                    "DN",
                    "https://www.dn.no/kundeservice/",
                    "News"),

            rule("bt",
                    ".*\\bbergens\\s*tidende\\b.*|.*\\bbt\\b.*abonnement.*",
                    "Bergens Tidende",
                    "https://www.bt.no/kundeservice/",
                    "News"),

            rule("wolt_plus",
                    ".*\\bwolt\\b.*(plus|\\+).*|.*\\bwolt\\+\\b.*",
                    "Wolt+",
                    "https://wolt.com/",
                    "Shopping & Food"),

            rule("foodora_plus",
                    ".*\\bfoodora\\b.*(plus|\\+).*",
                    "foodora plus",
                    "https://www.foodora.no/",
                    "Shopping & Food")
    );

    private KnownMerchants() {}

    public static Optional<Match> match(
            String merchantKey,
            String rawDescription
    ) {
        String a = safe(merchantKey);
        String b = safe(rawDescription);

        for (Rule rule : RULES) {
            if (rule.pattern.matcher(a).matches()
                    || rule.pattern.matcher(b).matches()) {
                return Optional.of(new Match(
                        rule.providerKey,
                        rule.displayName,
                        rule.cancelUrl,
                        rule.category
                ));
            }
        }

        return Optional.empty();
    }

    public static boolean isGenericPaymentWrapper(String value) {
        String s = safe(value);

        boolean containsWrapper =
                s.contains("applepay")
                        || s.contains("apple pay")
                        || s.contains("googlepay")
                        || s.contains("google pay")
                        || s.contains("samsung pay")
                        || s.contains("paypal")
                        || s.contains("visa varekjøp")
                        || s.contains("visa varekjop")
                        || s.contains("mastercard varekjøp")
                        || s.contains("mastercard varekjop");

        if (!containsWrapper) return false;

        // Genuine subscription descriptors must still be accepted.
        return !(s.contains("apple.com/bill")
                || s.contains("itunes.com/bill")
                || s.contains("icloud")
                || s.contains("apple services")
                || s.contains("google play")
                || s.contains("google one")
                || s.contains("youtube premium"));
    }

    public static boolean isKnownProviderKey(String providerKey) {
        if (providerKey == null || providerKey.isBlank()) return false;

        return RULES.stream().anyMatch(rule ->
                rule.providerKey.equalsIgnoreCase(providerKey.trim())
        );
    }

    public static String categoryForProvider(String providerKey) {
        if (providerKey == null || providerKey.isBlank()) return "Other";

        String key = providerKey.trim().toLowerCase(ROOT);

        for (Rule rule : RULES) {
            if (rule.providerKey.equalsIgnoreCase(key)) {
                return rule.category;
            }
        }

        return "Other";
    }

    private static String safe(String value) {
        if (value == null) return "";

        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", " ")
                .replace("-", " ")
                .replaceAll("\\s+", " ");
    }
}

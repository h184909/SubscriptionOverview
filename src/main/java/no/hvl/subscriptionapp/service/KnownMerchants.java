package no.hvl.subscriptionapp.service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.Locale.ROOT;

public final class KnownMerchants {

    public record Match(String providerKey, String displayName, String cancelUrl, String category) {}

    private record Rule(String providerKey, Pattern pattern, String displayName, String cancelUrl, String category) {}

    private static Rule rule(String key, String regex, String displayName, String cancelUrl, String category) {
        return new Rule(key, Pattern.compile(regex, Pattern.CASE_INSENSITIVE), displayName, cancelUrl, category);
    }

    private static final List<Rule> RULES = List.of(
            rule("netflix", ".*\\bnetflix\\b.*|.*netflix\\.com.*", "Netflix", "https://www.netflix.com/cancelplan", "Entertainment"),
            rule("spotify", ".*\\bspotify\\b.*|.*spotifyab.*", "Spotify", "https://www.spotify.com/account/subscription/", "Entertainment"),
            rule("disney_plus", ".*\\bdisney\\s*plus\\b.*|.*\\bdisney\\+\\b.*|.*disneyplus.*", "Disney+", "https://www.disneyplus.com/account/subscription", "Entertainment"),
            rule("tv2_play", ".*\\btv\\s*2\\b.*|.*\\btv2\\b.*|.*tv2play.*|.*play\\.tv2.*", "TV 2 Play", "https://play.tv2.no/konto", "Entertainment"),
            rule("viaplay", ".*\\bvia\\s*play\\b.*|.*\\bviaplay\\b.*|.*viaplay\\.(com|no|se|dk).*|.*\\bviaplay\\s*group\\b.*", "Viaplay", "https://viaplay.com/no-no/account", "Entertainment"),
            rule("prime_video", ".*\\bprime\\s*video\\b.*|.*primevideo.*|.*\\bamazon\\s*prime\\b.*|.*amazon.*video.*", "Prime Video", "https://www.amazon.com/amazonprime", "Entertainment"),
            rule("youtube_premium", ".*\\byoutube\\b.*|.*paid_memberships.*", "YouTube Premium", "https://www.youtube.com/paid_memberships", "Entertainment"),
            rule("storytel", ".*\\bstorytel\\b.*", "Storytel", "https://www.storytel.com/no/no/account", "Entertainment"),
            rule("bookbeat", ".*\\bbookbeat\\b.*", "BookBeat", "https://www.bookbeat.com/no/account", "Entertainment"),
            rule("fable", ".*\\bfable\\b.*", "Fable", "https://fable.no/kundeservice", "Entertainment"),
            rule("audible", ".*\\baudible\\b.*", "Audible", "https://www.audible.com/account/cancel", "Entertainment"),
            rule("rikstv", ".*\\briks\\s*tv\\b.*|.*\\brikstv\\b.*", "RiksTV", "https://www.rikstv.no/kundeservice/", "Entertainment"),

            rule("talkmore", ".*\\btalkmore\\b.*", "Talkmore", "https://www.talkmore.no/minside/", "Telecom"),
            rule("telenor", ".*\\btelenor\\b.*", "Telenor", "https://www.telenor.no/kundeservice/", "Telecom"),
            rule("telia", ".*\\btelia\\b.*", "Telia", "https://www.telia.no/kundeservice/", "Telecom"),
            rule("ice", ".*\\bice\\b.*", "Ice", "https://www.ice.no/kundeservice/", "Telecom"),
            rule("altibox", ".*\\baltibox\\b.*|.*\\blyse\\b.*altibox.*", "Altibox", "https://www.altibox.no/privat/kundeservice/", "Telecom"),

            rule("chatgpt", ".*\\bopenai\\b.*|.*\\bchatgpt\\b.*", "ChatGPT", "https://chatgpt.com/", "Utilities"),
            rule("apple_subscriptions", ".*apple\\.com/bill.*|.*itunes\\.com/bill.*|.*\\bitunes\\b.*|.*\\bicloud\\b.*|.*apple\\s*services.*", "Apple (Abonnement)", "https://apps.apple.com/account/subscriptions", "Utilities"),
            rule("google_play", ".*google\\s*play.*|.*play\\.google\\.com.*|.*google\\s*payment.*", "Google Play (Abonnement)", "https://play.google.com/store/account/subscriptions", "Utilities"),
            rule("google_one", ".*\\bgoogle\\s*one\\b.*|.*google.*storage.*", "Google One", "https://one.google.com/", "Utilities"),
            rule("tibber", ".*\\btibber\\b.*", "Tibber", "https://account.tibber.com/", "Utilities"),
            rule("microsoft_365", ".*\\bmicrosoft\\b.*(365|office).*|.*\\boffice\\s*365\\b.*|.*\\boffice365\\b.*", "Microsoft 365", "https://account.microsoft.com/services/", "Utilities"),
            rule("adobe", ".*\\badobe\\b.*(creative\\s*cloud|cc|acrobat)?.*", "Adobe", "https://account.adobe.com/plans", "Utilities"),
            rule("dropbox", ".*\\bdropbox\\b.*", "Dropbox", "https://www.dropbox.com/account/billing", "Utilities"),
            rule("github", ".*\\bgithub\\b.*", "GitHub", "https://github.com/settings/billing", "Utilities"),
            rule("notion", ".*\\bnotion\\b.*", "Notion", "https://www.notion.so/my-account", "Utilities"),
            rule("slack", ".*\\bslack\\b.*", "Slack", "https://my.slack.com/account/billing", "Utilities"),
            rule("zoom", ".*\\bzoom\\b.*", "Zoom", "https://zoom.us/billing", "Utilities"),
            rule("nordvpn", ".*\\bnordvpn\\b.*|.*\\bnord\\s*vpn\\b.*", "NordVPN", "https://my.nordaccount.com/billing/", "Utilities"),
            rule("surfshark", ".*\\bsurfshark\\b.*", "Surfshark", "https://my.surfshark.com/billing", "Utilities"),

            rule("sats", ".*\\bsats\\b.*", "SATS", "https://www.sats.no/kundeservice/", "Health & Fitness"),
            rule("fresh_fitness", ".*\\bfresh\\s*fitness\\b.*|.*\\bfreshfitness\\b.*", "Fresh Fitness", "https://freshfitness.no/kundeservice/", "Health & Fitness"),
            rule("strava", ".*\\bstrava\\b.*", "Strava", "https://www.strava.com/settings/subscription", "Health & Fitness"),

            rule("aftenposten", ".*\\baftenposten\\b.*|.*\\bschibsted\\b.*aftenposten.*", "Aftenposten", "https://aftenposten.no/kundeservice", "News"),
            rule("vg_plus", ".*\\bvg\\+\\b.*|.*\\bvgpluss\\b.*|.*\\bverdens\\s*gang\\b.*", "VG+", "https://www.vg.no/kundeservice/", "News"),
            rule("dn", ".*\\bdagens\\s*næringsliv\\b.*|.*\\bdn\\b.*abonnement.*", "DN", "https://www.dn.no/kundeservice/", "News"),
            rule("bt", ".*\\bbergens\\s*tidende\\b.*|.*\\bbt\\b.*abonnement.*", "Bergens Tidende", "https://www.bt.no/kundeservice/", "News"),
            rule("adresseavisen", ".*\\badresseavisen\\b.*|.*\\badressa\\b.*", "Adresseavisen", "https://www.adressa.no/kundeservice", "News"),
            rule("morgenbladet", ".*\\bmorgenbladet\\b.*", "Morgenbladet", "https://www.morgenbladet.no/kundeservice", "News"),

            rule("wolt_plus", ".*\\bwolt\\b.*(plus|\\+).*|.*\\bwolt\\+\\b.*", "Wolt+", "https://wolt.com/", "Shopping & Food"),
            rule("foodora_plus", ".*\\bfoodora\\b.*(plus|\\+).*", "foodora plus", "https://www.foodora.no/", "Shopping & Food")
    );

    private KnownMerchants() {}

    public static Optional<Match> match(String merchantKey, String rawDescription) {
        String a = safe(merchantKey);
        String b = safe(rawDescription);

        for (Rule r : RULES) {
            if (r.pattern.matcher(a).matches() || r.pattern.matcher(b).matches()) {
                return Optional.of(new Match(r.providerKey, r.displayName, r.cancelUrl, r.category));
            }
        }
        return Optional.empty();
    }

    public static String categoryForProvider(String providerKey) {
        if (providerKey == null || providerKey.isBlank()) return "Other";

        String key = providerKey.trim().toLowerCase(ROOT);
        for (Rule r : RULES) {
            if (r.providerKey.equalsIgnoreCase(key)) {
                return r.category;
            }
        }

        return "Other";
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.trim()
                .toLowerCase(ROOT)
                .replace("_", " ")
                .replace("-", " ")
                .replaceAll("\\s+", " ");
    }
}
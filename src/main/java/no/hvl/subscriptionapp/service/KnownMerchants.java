package no.hvl.subscriptionapp.service;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.Locale.ROOT;

public final class KnownMerchants {

    public record Match(String providerKey, String displayName, String cancelUrl) {}

    private record Rule(String providerKey, Pattern pattern, String displayName, String cancelUrl) {}

    private static Rule rule(String key, String regex, String displayName, String cancelUrl) {
        return new Rule(key, Pattern.compile(regex, Pattern.CASE_INSENSITIVE), displayName, cancelUrl);
    }

    // Viktig: hold dette som én ren liste. Ikke legg rules inni andre metoder.
    private static final List<Rule> RULES = List.of(
            // --- Streaming ---
            rule("netflix", ".*\\bnetflix\\b.*|.*netflix\\.com.*", "Netflix", "https://www.netflix.com/cancelplan"),
            rule("spotify", ".*\\bspotify\\b.*", "Spotify", "https://www.spotify.com/account/subscription/"),
            rule("disney_plus", ".*\\bdisney\\s*plus\\b.*|.*\\bdisney\\+\\b.*", "Disney+", "https://www.disneyplus.com/account/subscription"),
            rule("tv2_play", ".*\\btv\\s*2\\b.*|.*\\btv2\\b.*", "TV 2 Play", "https://play.tv2.no/konto"),
            rule("viaplay", ".*\\bviaplay\\b.*", "Viaplay", "https://viaplay.com/no-no/account"),
            rule("prime_video", ".*\\bprime\\s*video\\b.*|.*primevideo\\..*|.*\\bamazon\\s*prime\\b.*", "Prime Video", "https://www.amazon.com/amazonprime"),
            rule("youtube_premium", ".*\\byoutube\\b.*|.*paid_memberships.*", "YouTube Premium", "https://www.youtube.com/paid_memberships"),

            // --- Apple / Google ---
            rule("apple_subscriptions", ".*apple\\.com/bill.*|.*itunes\\.com/bill.*|.*\\bitunes\\b.*|.*\\bicloud\\b.*",
                    "Apple (Abonnement)", "https://apps.apple.com/account/subscriptions"),
            rule("google_play", ".*google\\s*play.*|.*\\bplay\\b.*\\bapps\\b.*|.*play\\.google\\.com.*",
                    "Google Play (Abonnement)", "https://play.google.com/store/account/subscriptions"),
            rule("google_one", ".*\\bgoogle\\s*one\\b.*|.*\\bgoogle\\b.*storage.*",
                    "Google One", "https://one.google.com/"),

            // --- Strøm / bom / lokale ---
            rule("tibber", ".*\\btibber\\b.*", "Tibber", "https://account.tibber.com/"),
            rule("flyt", ".*\\bflyt\\b.*", "Flyt (bom/parkering)", "https://flyt.no/"),

            // --- Telecom / TV (Norge) ---
            rule("talkmore", ".*\\btalkmore\\b.*", "Talkmore", "https://www.talkmore.no/minside/"),
            rule("telenor", ".*\\btelenor\\b.*", "Telenor", "https://www.telenor.no/kundeservice/"),
            rule("telia", ".*\\btelia\\b.*", "Telia", "https://www.telia.no/kundeservice/"),
            rule("ice", ".*\\bice\\b.*", "Ice", "https://www.ice.no/kundeservice/"),
            rule("altibox", ".*\\baltibox\\b.*|.*\\blyse\\b.*altibox.*", "Altibox", "https://www.altibox.no/privat/kundeservice/"),
            rule("rikstv", ".*\\briks\\s*tv\\b.*|.*\\brikstv\\b.*", "RiksTV", "https://www.rikstv.no/kundeservice/"),
            rule("kvinnherad_breiband", ".*\\bkvinnherad\\s*breiband\\b.*", "Kvinnherad Breiband", "https://kvinnheradbreiband.no/"),

            // --- Productivity / Cloud ---
            rule("microsoft_365", ".*\\bmicrosoft\\b.*(365|office).*|.*\\boffice\\s*365\\b.*|.*\\boffice365\\b.*",
                    "Microsoft 365", "https://account.microsoft.com/services/"),
            rule("adobe", ".*\\badobe\\b.*(creative\\s*cloud|cc|acrobat).*|.*\\badobe\\b.*",
                    "Adobe", "https://account.adobe.com/plans"),
            rule("dropbox", ".*\\bdropbox\\b.*", "Dropbox", "https://www.dropbox.com/account/billing"),
            rule("github", ".*\\bgithub\\b.*", "GitHub", "https://github.com/settings/billing"),
            rule("chatgpt", ".*\\bopenai\\b.*|.*\\bchatgpt\\b.*", "ChatGPT", "https://chat.openai.com/"),
            rule("notion", ".*\\bnotion\\b.*", "Notion", "https://www.notion.so/my-account"),
            rule("slack", ".*\\bslack\\b.*", "Slack", "https://my.slack.com/account/billing"),
            rule("zoom", ".*\\bzoom\\b.*", "Zoom", "https://zoom.us/billing"),

            // --- News (Norge) ---
            rule("aftenposten", ".*\\baftenposten\\b.*|.*\\bschibsted\\b.*aftenposten.*",
                    "Aftenposten", "https://aftenposten.no/kundeservice"),
            rule("vg_plus", ".*\\bvg\\+\\b.*|.*\\bvgpluss\\b.*|.*\\bverdens\\s*gang\\b.*",
                    "VG+", "https://www.vg.no/kundeservice/"),
            rule("dn", ".*\\bdagens\\s*næringsliv\\b.*|.*\\bdn\\b.*abonnement.*",
                    "DN", "https://www.dn.no/kundeservice/"),
            rule("bt", ".*\\bbergens\\s*tidende\\b.*|.*\\bbt\\b.*abonnement.*",
                    "Bergens Tidende", "https://www.bt.no/kundeservice/"),
            rule("adresseavisen", ".*\\badresseavisen\\b.*|.*\\badressa\\b.*",
                    "Adresseavisen", "https://www.adressa.no/kundeservice"),
            rule("morgenbladet", ".*\\bmorgenbladet\\b.*",
                    "Morgenbladet", "https://www.morgenbladet.no/kundeservice"),

            // --- Fitness ---
            rule("sats", ".*\\bsats\\b.*", "SATS", "https://www.sats.no/kundeservice/"),
            rule("fresh_fitness", ".*\\bfresh\\s*fitness\\b.*|.*\\bfreshfitness\\b.*",
                    "Fresh Fitness", "https://freshfitness.no/kundeservice/"),

            // --- Other common subscriptions ---
            rule("strava", ".*\\bstrava\\b.*", "Strava", "https://www.strava.com/settings/subscription"),
            rule("storytel", ".*\\bstorytel\\b.*", "Storytel", "https://www.storytel.com/no/no/account"),
            rule("bookbeat", ".*\\bbookbeat\\b.*", "BookBeat", "https://www.bookbeat.com/no/account"),
            rule("fable", ".*\\bfable\\b.*", "Fable", "https://fable.no/kundeservice"),
            rule("audible", ".*\\baudible\\b.*", "Audible", "https://www.audible.com/account/cancel"),
            rule("nordvpn", ".*\\bnordvpn\\b.*|.*\\bnord\\s*vpn\\b.*", "NordVPN", "https://my.nordaccount.com/billing/"),
            rule("surfshark", ".*\\bsurfshark\\b.*", "Surfshark", "https://my.surfshark.com/billing"),
            rule("wolt_plus", ".*\\bwolt\\b.*(\\+|plus).*|.*\\bwolt\\+\\b.*", "Wolt+", "https://wolt.com/"),
            rule("foodora_plus", ".*\\bfoodora\\b.*(plus|\\+).*", "foodora plus", "https://www.foodora.no/")
    );

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
        return (s == null) ? "" : s.trim().toLowerCase(ROOT);
    }
}
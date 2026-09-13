package com.example.skyblockusd;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts Hypixel SkyBlock coin displays into their USD equivalent.
 *
 * The matcher deliberately requires a coin/currency context so ordinary
 * numbers such as damage, health, item counts, and coordinates are not
 * converted accidentally.
 */
public class SkyblockUsdMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("coins-to-money");

    /*
     * Supported forms include:
     *   1 Coins / 1,234 Coins / 1.5 Coins
     *   1K, 1.5K, 1M, 2.35B, 4T, 7Q
     *   1k Coins / 1.5M Coins / 4.2T coins
     *   Coins: 1,234 / Coins: 1.5M
     *   1,234 coins / 1,234 COINS
     *
     * Minecraft formatting codes are allowed between the number, suffix,
     * and the word Coins.
     */
    private static final String FORMAT_CODES = "(?:§[0-9a-fk-or])*(?:&[0-9a-fk-or])*";

    private static final Pattern COIN_PATTERN = Pattern.compile(
            "(?<![\d.])" +
            "(?:(?:coins?|coin)\s*[:=]?\s*)?" +
            "([\d]+(?:[,.][\d]+)*)" +
            FORMAT_CODES +
            "\s*([kmbtqKMBTQ])?" +
            FORMAT_CODES +
            "\s*" +
            "(?:coins?|coin)?" +
            "(?![A-Za-z])",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COINS_PREFIX_PATTERN = Pattern.compile(
            "(?i)\\bcoins?\\s*[:=]\\s*" +
            FORMAT_CODES +
            "\s*([\\d]+(?:[,.][\\d]+)*)\\s*" +
            "([kmbtq]?)\\b"
    );

    @Override
    public void onInitialize() {
        LOGGER.info("Coins-to-Money initializing...");
        CookiePriceFetcher.startPeriodicUpdates(5);
    }

    public static String replaceCoinsString(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // The generic matcher below requires a currency word unless a suffix
        // is present. This prevents ordinary GUI numbers from being converted.
        Matcher matcher = COIN_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        boolean changed = false;

        while (matcher.find()) {
            String whole = matcher.group(0);

            // Do not treat a plain number as money unless it is explicitly
            // adjacent to "coin(s)" or uses a recognized K/M/B/T/Q suffix.
            String suffix = matcher.group(2);
            boolean hasCoinWord = whole.toLowerCase(Locale.ROOT).matches(".*coins?.*");
            boolean hasSuffix = suffix != null && !suffix.isEmpty();
            if (!hasCoinWord && !hasSuffix) {
                continue;
            }

            try {
                double coins = parseCoins(matcher.group(1), suffix);
                if (!Double.isFinite(coins) || coins < 0) {
                    continue;
                }

                double usdValue = coins / CookiePriceFetcher.coinsPerUsd;
                String replacement = NumberFormat.getCurrencyInstance(Locale.US).format(usdValue);

                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                changed = true;
            } catch (NumberFormatException ignored) {
                // Leave malformed text untouched.
            }
        }

        matcher.appendTail(sb);
        return changed ? sb.toString() : text;
    }

    private static double parseCoins(String number, String suffix) {
        /*
         * Hypixel commonly displays large values with K/M/B-style suffixes;
         * T and Q are accepted as well so the parser remains useful for the
         * full large-number notation rather than silently failing at 1T+.
         *
         * For decimal-comma locales we cannot safely infer whether a comma
         * is decimal or thousands from the rendered Minecraft string, so
         * Hypixel's normal comma-separated integer form is treated as
         * thousands separators.
         */
        String normalized = number.replace(",", "");
        double value = Double.parseDouble(normalized);

        if (suffix == null || suffix.isEmpty()) {
            return value;
        }

        return switch (suffix.toLowerCase(Locale.ROOT)) {
            case "k" -> value * 1_000d;
            case "m" -> value * 1_000_000d;
            case "b" -> value * 1_000_000_000d;
            case "t" -> value * 1_000_000_000_000d;
            case "q" -> value * 1_000_000_000_000_000d;
            default -> value;
        };
    }
}

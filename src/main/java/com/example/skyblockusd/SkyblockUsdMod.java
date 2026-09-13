package com.example.skyblockusd;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts the coin amounts used throughout Hypixel SkyBlock into USD. */
public class SkyblockUsdMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("coins-to-money");

    private static final String NUMBER = "([\\d]+(?:[,.][\\d]+)*)";
    private static final String SUFFIX = "([kmbtqKMBTQ])?";
    private static final String FORMAT_CODES = "(?:§[0-9a-fk-or])*";

    /*
     * Scoreboard:
     *   Purse: 123,456
     *   Piggy: 123,456.7
     *   Coins: 123,456
     *
     * Auction House / Bazaar / other menus:
     *   text: 123,456 coins
     *   item for 123,456 coins
     *   - 123,456 coins text
     *   item 123,456 | 234,567
     *
     * The coin word is optional only for the documented scoreboard prefixes
     * and Bazaar price-pair form. This avoids converting unrelated numbers.
     */
    private static final Pattern SCOREBOARD_PATTERN = Pattern.compile(
            "(?i)(\\b(?:purse|piggy|coins)\\s*[:=]\\s*)" +
            FORMAT_CODES + NUMBER + FORMAT_CODES + "\\s*" + SUFFIX + FORMAT_CODES + "(?=$|[^A-Za-z])"
    );

    private static final Pattern COIN_WORD_PATTERN = Pattern.compile(
            "(?i)(?<![\\d.])" + NUMBER + FORMAT_CODES + "\\s*" + SUFFIX + FORMAT_CODES +
            "\\s*coins?\\b"
    );

    private static final Pattern BAZAAR_PAIR_PATTERN = Pattern.compile(
            "(?<![\\d.])" + NUMBER + FORMAT_CODES + "\\s*" +
            "\\|\\s*" + FORMAT_CODES + NUMBER + "(?![\\d.])"
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

        String result = replaceMatches(text, SCOREBOARD_PATTERN, 1, 2, 3, false);
        result = replaceMatches(result, COIN_WORD_PATTERN, 0, 1, 2, true);
        result = replaceBazaarPairs(result);
        return result;
    }

    private static String replaceMatches(String text, Pattern pattern, int prefixGroup, int numberGroup,
                                         int suffixGroup, boolean replaceWholeMatch) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer output = new StringBuffer();
        boolean changed = false;

        while (matcher.find()) {
            try {
                double coins = parseCoins(matcher.group(numberGroup), matcher.group(suffixGroup));
                String usd = formatUsd(coins);

                String replacement;
                if (replaceWholeMatch) {
                    replacement = usd;
                } else {
                    replacement = matcher.group(prefixGroup) + usd;
                }

                matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
                changed = true;
            } catch (RuntimeException ignored) {
                matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group(0)));
            }
        }

        matcher.appendTail(output);
        return changed ? output.toString() : text;
    }

    private static String replaceBazaarPairs(String text) {
        Matcher matcher = BAZAAR_PAIR_PATTERN.matcher(text);
        StringBuffer output = new StringBuffer();
        boolean changed = false;

        while (matcher.find()) {
            try {
                double leftCoins = parseCoins(matcher.group(1), null);
                double rightCoins = parseCoins(matcher.group(2), null);
                String replacement = formatUsd(leftCoins) + " | " + formatUsd(rightCoins);
                matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
                changed = true;
            } catch (RuntimeException ignored) {
                matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group(0)));
            }
        }

        matcher.appendTail(output);
        return changed ? output.toString() : text;
    }

    private static double parseCoins(String number, String suffix) {
        // Hypixel's normal large-number display uses commas as thousands separators.
        double value = Double.parseDouble(number.replace(",", ""));

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

    private static String formatUsd(double coins) {
        if (!Double.isFinite(coins) || coins < 0 || !Double.isFinite(CookiePriceFetcher.coinsPerUsd)
                || CookiePriceFetcher.coinsPerUsd <= 0) {
            throw new IllegalArgumentException("Invalid conversion rate or coin amount");
        }
        return NumberFormat.getCurrencyInstance(Locale.US).format(coins / CookiePriceFetcher.coinsPerUsd);
    }
}

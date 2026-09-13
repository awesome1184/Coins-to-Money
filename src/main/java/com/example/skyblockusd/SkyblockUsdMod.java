package com.example.skyblockusd;

import net.fabricmc.api.ModInitializer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts the coin amounts used throughout Hypixel SkyBlock into USD. */
public class SkyblockUsdMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("coins-to-money");

    private static final String NUMBER = "([\\d]+(?:[,.][\\d]+)*)";
    private static final String SUFFIX = "([kmbtqKMBTQ])?";
    private static final String FORMAT_CODES = "(?:§[0-9a-fk-or])*";

    private static final Pattern SCOREBOARD_PATTERN = Pattern.compile(
            "(?i)(\\b(?:purse|piggy|coins)\\s*[:=]\\s*)" + FORMAT_CODES + NUMBER + FORMAT_CODES + "\\s*" + SUFFIX + FORMAT_CODES + "(?=$|[^A-Za-z])"
    );

    private static final Pattern COIN_WORD_PATTERN = Pattern.compile(
            "(?i)(?<![\\d.])" + NUMBER + FORMAT_CODES + "\\s*" + SUFFIX + FORMAT_CODES + "\\s*coins?\\b"
    );

    private static final Pattern BAZAAR_PAIR_PATTERN = Pattern.compile(
            "(?<![\\d.])" + NUMBER + FORMAT_CODES + "\\s*\\|\\s*" + FORMAT_CODES + NUMBER + "(?![\\d.])"
    );

    @Override
    public void onInitialize() {
        LOGGER.info("Coins-to-Money initializing...");
        CookiePriceFetcher.startPeriodicUpdates(5);
    }

    public static String replaceCoinsString(String text) {
        if (text == null || text.isEmpty()) return text;

        String result = replaceMatches(text, SCOREBOARD_PATTERN, 1, 2, 3, false);
        result = replaceMatches(result, COIN_WORD_PATTERN, 0, 1, 2, true);
        return replaceBazaarPairs(result);
    }

    private static String replaceMatches(String text, Pattern pattern, int prefixGroup, int numberGroup,
                                         int suffixGroup, boolean replaceWholeMatch) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer output = new StringBuffer();
        boolean changed = false;

        while (matcher.find()) {
            try {
                String usd = formatUsd(parseCoins(matcher.group(numberGroup), matcher.group(suffixGroup)));
                String replacement = replaceWholeMatch ? usd : matcher.group(prefixGroup) + usd;
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
                String replacement = formatUsd(parseCoins(matcher.group(1), null)) + " | "
                        + formatUsd(parseCoins(matcher.group(2), null));
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
        double value = Double.parseDouble(number.replace(",", ""));
        if (suffix == null || suffix.isEmpty()) return value;
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

    /**
     * Preserves the effective style of every text segment. When a coin value
     * spans multiple styled segments, the replacement gets the style of the
     * first segment containing that value. Thus Bazaar prices stay red/green,
     * while scoreboard Purse/Piggy values stay gold.
     */
    public static Text replaceCoinsText(Text original) {
        if (original == null) return null;

        List<StyledPart> parts = new ArrayList<>();
        original.visit((style, string) -> {
            if (!string.isEmpty()) parts.add(new StyledPart(string, style));
            return Optional.empty();
        }, original.getStyle());

        if (parts.isEmpty()) return original;

        StringBuilder all = new StringBuilder();
        for (StyledPart part : parts) all.append(part.text());

        List<Replacement> replacements = new ArrayList<>();
        Matcher matcher = SCOREBOARD_PATTERN.matcher(all);
        collectReplacements(matcher, replacements);
        matcher = COIN_WORD_PATTERN.matcher(all);
        collectReplacements(matcher, replacements);
        matcher = BAZAAR_PAIR_PATTERN.matcher(all);
        collectBazaarReplacements(matcher, replacements);

        replacements.sort((a, b) -> Integer.compare(a.start(), b.start()));
        if (replacements.isEmpty()) return original;

        // Drop overlapping matches so a scoreboard/coin match isn't converted twice.
        List<Replacement> filtered = new ArrayList<>();
        int lastEnd = -1;
        for (Replacement replacement : replacements) {
            if (replacement.start() >= lastEnd) {
                filtered.add(replacement);
                lastEnd = replacement.end();
            }
        }

        MutableText rebuilt = Text.empty().setStyle(original.getStyle());
        int cursor = 0;
        for (Replacement replacement : filtered) {
            appendStyledRange(rebuilt, parts, cursor, replacement.start());
            rebuilt.append(Text.literal(replacement.replacement()).setStyle(styleAt(parts, replacement.start())));
            cursor = replacement.end();
        }
        appendStyledRange(rebuilt, parts, cursor, all.length());
        return rebuilt;
    }

    private static void collectReplacements(Matcher matcher, List<Replacement> output) {
        while (matcher.find()) {
            String suffix = matcher.groupCount() >= 3 ? matcher.group(matcher.groupCount()) : null;
            // SCOREBOARD and COIN_WORD both expose number at group 2/1 respectively;
            // identify the pattern by its first group's shape.
            try {
                if (matcher.groupCount() == 3) {
                    String number = matcher.group(2);
                    String suffixValue = matcher.group(3);
                    String usd = formatUsd(parseCoins(number, suffixValue));
                    String replacement = matcher.pattern() == SCOREBOARD_PATTERN
                            ? matcher.group(1) + usd : usd;
                    output.add(new Replacement(matcher.start(), matcher.end(), replacement));
                } else {
                    String usd = formatUsd(parseCoins(matcher.group(1), matcher.group(2)));
                    output.add(new Replacement(matcher.start(), matcher.end(), usd));
                }
            } catch (RuntimeException ignored) {
                // Leave malformed values untouched.
            }
        }
    }

    private static void collectBazaarReplacements(Matcher matcher, List<Replacement> output) {
        while (matcher.find()) {
            try {
                String replacement = formatUsd(parseCoins(matcher.group(1), null)) + " | "
                        + formatUsd(parseCoins(matcher.group(2), null));
                output.add(new Replacement(matcher.start(), matcher.end(), replacement));
            } catch (RuntimeException ignored) {
                // Leave malformed values untouched.
            }
        }
    }

    private static void appendStyledRange(MutableText destination, List<StyledPart> parts, int start, int end) {
        if (start >= end) return;
        int position = 0;
        for (StyledPart part : parts) {
            int partStart = position;
            int partEnd = position + part.text().length();
            int from = Math.max(start, partStart);
            int to = Math.min(end, partEnd);
            if (from < to) {
                String chunk = part.text().substring(from - partStart, to - partStart);
                destination.append(Text.literal(chunk).setStyle(part.style()));
            }
            position = partEnd;
            if (position >= end) break;
        }
    }

    private static Style styleAt(List<StyledPart> parts, int offset) {
        int position = 0;
        for (StyledPart part : parts) {
            int end = position + part.text().length();
            if (offset < end) return part.style();
            position = end;
        }
        return parts.get(parts.size() - 1).style();
    }

    private record StyledPart(String text, Style style) {}
    private record Replacement(int start, int end, String replacement) {}
}

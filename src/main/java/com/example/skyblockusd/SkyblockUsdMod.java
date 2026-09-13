package com.example.skyblockusd;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts explicit Hypixel SkyBlock coin amounts into USD. */
public class SkyblockUsdMod implements net.fabricmc.api.ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("coins-to-money");
    public static volatile boolean enabled = true;

    private static final String NUMBER = "([\\d]+(?:[,.][\\d]+)*)";
    private static final String SUFFIX = "([kmbtqKMBTQ])?";
    private static final String FORMAT_CODES = "(?:§[0-9a-fk-or])*";

    private static final Pattern SCOREBOARD_PATTERN = Pattern.compile(
            "(?i)\\b(?:purse|piggy|balance|coins)\\s*[:=]\\s*" +
            FORMAT_CODES + NUMBER + FORMAT_CODES + "\\s*" + SUFFIX + FORMAT_CODES +
            "(?=$|[^A-Za-z])"
    );

    private static final Pattern COIN_WORD_PATTERN = Pattern.compile(
            "(?i)(?<![\\d.])" + NUMBER + FORMAT_CODES + "\\s*" + SUFFIX + FORMAT_CODES +
            "\\s*coins?\\b"
    );

    private static final Pattern BAZAAR_PAIR_PATTERN = Pattern.compile(
            "(?<![\\d.])" + NUMBER + FORMAT_CODES + "\\s*\\|\\s*" +
            FORMAT_CODES + NUMBER + "(?![\\d.])"
    );

    @Override
    public void onInitialize() {
        ModConfig.load();
        LOGGER.info("Coins-to-Money initializing for Minecraft 26.1.2+");
        CookiePriceFetcher.startPeriodicUpdates(5);
    }

    public static String replaceCoinsString(String text) {
        if (!enabled || text == null || text.isEmpty()) return text;
        String result = replaceNumericSpans(text, SCOREBOARD_PATTERN, 1, 2);
        result = replaceNumericSpans(result, COIN_WORD_PATTERN, 1, 2);
        return replaceBazaarPairs(result);
    }

    private static String replaceNumericSpans(String text, Pattern pattern, int numberGroup, int suffixGroup) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer output = new StringBuffer();
        boolean changed = false;
        while (matcher.find()) {
            try {
                String usd = formatUsd(parseCoins(matcher.group(numberGroup), matcher.group(suffixGroup)));
                int numericStart = matcher.start(numberGroup);
                int numericEnd = matcher.group(suffixGroup) != null ? matcher.end(suffixGroup) : matcher.end(numberGroup);
                String replacement = text.substring(matcher.start(), numericStart) + usd + text.substring(numericEnd, matcher.end());
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
                String leftUsd = formatUsd(parseCoins(matcher.group(1), null));
                String rightUsd = formatUsd(parseCoins(matcher.group(2), null));
                String replacement = leftUsd + text.substring(matcher.end(1), matcher.start(2)) + rightUsd;
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
        if (!Double.isFinite(coins) || coins < 0 || !Double.isFinite(CookiePriceFetcher.coinsPerUsd) || CookiePriceFetcher.coinsPerUsd <= 0) {
            throw new IllegalArgumentException("Invalid conversion rate or coin amount");
        }
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        format.setMinimumFractionDigits(ModConfig.decimalPlaces);
        format.setMaximumFractionDigits(ModConfig.decimalPlaces);
        return format.format(coins / CookiePriceFetcher.coinsPerUsd);
    }

    public static Component replaceCoinsComponent(Component original) {
        if (!enabled || original == null) return original;
        List<StyledPart> parts = new ArrayList<>();
        original.visit((style, text) -> {
            if (!text.isEmpty()) parts.add(new StyledPart(text, style));
            return java.util.Optional.empty();
        }, original.getStyle());
        if (parts.isEmpty()) return original;

        StringBuilder combined = new StringBuilder();
        for (StyledPart part : parts) combined.append(part.text());

        List<Replacement> replacements = new ArrayList<>();
        collectScoreboardNumbers(combined, replacements);
        collectCoinWordNumbers(combined, replacements);
        collectBazaarNumbers(combined, replacements);

        replacements.sort((a, b) -> {
            int cmp = Integer.compare(a.start(), b.start());
            return cmp != 0 ? cmp : Integer.compare(a.end(), b.end());
        });

        List<Replacement> filtered = new ArrayList<>();
        int lastEnd = -1;
        for (Replacement replacement : replacements) {
            if (replacement.start() >= lastEnd) {
                filtered.add(replacement);
                lastEnd = replacement.end();
            }
        }

        if (filtered.isEmpty()) return original;

        MutableComponent rebuilt = Component.empty().withStyle(original.getStyle());
        int cursor = 0;
        for (Replacement replacement : filtered) {
            appendStyledRange(rebuilt, parts, cursor, replacement.start());
            rebuilt.append(Component.literal(replacement.replacement()).withStyle(styleAt(parts, replacement.start())));
            cursor = replacement.end();
        }
        appendStyledRange(rebuilt, parts, cursor, combined.length());
        return rebuilt;
    }

    /**
     * Handles scoreboard entries whose visible text is split between the team/owner text and
     * the scoreboard's separate numeric value. For example Hypixel can render
     * "Purse: 7,021,5" followed by a separate score of "56".
     */
    public static Component replaceScoreboardEntry(Component ownerName, int score) {
        if (!enabled || ownerName == null) return ownerName;

        String prefix = ownerName.getString();
        String lower = prefix.toLowerCase(Locale.ROOT);
        boolean coinContext = lower.contains("purse") || lower.contains("piggy") ||
                lower.contains("balance") || lower.contains("coins:");
        if (!coinContext) return ownerName;

        MutableComponent combined = Component.empty();
        combined.append(ownerName.copy());
        combined.append(Component.literal(Integer.toString(score)).withStyle(net.minecraft.ChatFormatting.GOLD));
        return replaceCoinsComponent(combined);
    }

    public static boolean isScoreboardCoinEntry(Component ownerName) {
        if (!enabled || ownerName == null) return false;
        String lower = ownerName.getString().toLowerCase(Locale.ROOT);
        return lower.contains("purse") || lower.contains("piggy") || lower.contains("balance") || lower.contains("coins:");
    }

    private static void collectScoreboardNumbers(CharSequence text, List<Replacement> output) {
        Matcher matcher = SCOREBOARD_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                int start = matcher.start(1);
                int end = matcher.group(2) != null ? matcher.end(2) : matcher.end(1);
                output.add(new Replacement(start, end, formatUsd(parseCoins(matcher.group(1), matcher.group(2)))));
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static void collectCoinWordNumbers(CharSequence text, List<Replacement> output) {
        Matcher matcher = COIN_WORD_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                int start = matcher.start(1);
                int end = matcher.group(2) != null ? matcher.end(2) : matcher.end(1);
                output.add(new Replacement(start, end, formatUsd(parseCoins(matcher.group(1), matcher.group(2)))));
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static void collectBazaarNumbers(CharSequence text, List<Replacement> output) {
        Matcher matcher = BAZAAR_PAIR_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                output.add(new Replacement(matcher.start(1), matcher.end(1), formatUsd(parseCoins(matcher.group(1), null))));
                output.add(new Replacement(matcher.start(2), matcher.end(2), formatUsd(parseCoins(matcher.group(2), null))));
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static void appendStyledRange(MutableComponent destination, List<StyledPart> parts, int start, int end) {
        if (start >= end) return;
        int position = 0;
        for (StyledPart part : parts) {
            int partStart = position;
            int partEnd = position + part.text().length();
            int from = Math.max(start, partStart);
            int to = Math.min(end, partEnd);
            if (from < to) {
                destination.append(Component.literal(part.text().substring(from - partStart, to - partStart)).withStyle(part.style()));
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

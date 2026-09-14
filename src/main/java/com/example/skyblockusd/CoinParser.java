package com.example.skyblockusd;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parse complete currency amounts, never a fragment of a malformed number. */
public final class CoinParser {
    private static final String NUMBER = "[+-]?(?:[0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.[0-9]+)?[kKmMbBtTqQ]?";
    private static final Pattern VALID_NUMBER = Pattern.compile("^(" + NUMBER + ")$");
    private static final Pattern COINS = Pattern.compile("(?<![\\w.,+$<\u00a3\u20ac-])(" + NUMBER + ")\\h*(?i:coins?)\\b");
    private static final Pattern BALANCE = Pattern.compile("(?i)\\b(Purse|Piggy(?: Bank)?|Bank|Balance|Coins):\\h*(" + NUMBER + ")(?![\\w.,])");
    private static final Pattern PRICE = Pattern.compile("(?i)\\b(?:Buy price|Sell price|Price per unit|Price|Cost|Starting bid|Top bid|Your bid|BIN price|Buy it now):\\h*(" + NUMBER + ")(?![\\w.,])");
    private static final Pattern OTHER_CURRENCY = Pattern.compile("(?i)^\\h*(?:cookies?|gems?|bits?|copper|motes?|tokens?|essence)\\b");
    private static final Pattern FORMATTING = Pattern.compile("(?i)§[0-9a-fk-orx]|\\p{Cf}");
    private static final Pattern COOKIE_ANNOTATION = Pattern.compile("\\[[+-]?<?[0-9,.]+ cookies(?:[ |\\]]|$)");
    private static final Pattern LAYOUT_SEPARATOR = Pattern.compile(" \\[| \\(| \\| | = ");
    private static final Pattern EQUIVALENT = Pattern.compile("\\p{Sc}[0-9,.]+|\\b([A-Z]{3}) <?[0-9,.]+|[0-9,.]+ cookies\\b");
    public record Amount(int start, int end, double coins, String source) { }

    private CoinParser() { }
    public static String plain(String value) { return value == null ? "" : FORMATTING.matcher(value).replaceAll(""); }

    public static double parseNumber(String value) {
        if (value == null || !VALID_NUMBER.matcher(value).matches()) return Double.NaN;
        String normalized = value.replace(",", "");
        char last = Character.toLowerCase(normalized.charAt(normalized.length() - 1));
        double multiplier = switch (last) {
            case 'k' -> 1_000d;
            case 'm' -> 1_000_000d;
            case 'b' -> 1_000_000_000d;
            case 't' -> 1_000_000_000_000d;
            case 'q' -> 1_000_000_000_000_000d;
            default -> 1d;
        };
        if (multiplier != 1d) normalized = normalized.substring(0, normalized.length() - 1);
        try {
            double result = Double.parseDouble(normalized) * multiplier;
            return Double.isFinite(result) ? result : Double.NaN;
        } catch (NumberFormatException ex) { return Double.NaN; }
    }

    public static List<Amount> find(String input, boolean includeBalances, boolean includePrices) {
        String text = plain(input);
        if (text.length() > 16_384 || isAnnotated(text)) return List.of();
        List<Amount> result = new ArrayList<>();
        collect(COINS.matcher(text), 1, result, text, false);
        if (includeBalances) collect(BALANCE.matcher(text), 2, result, text, true);
        if (includePrices) collect(PRICE.matcher(text), 1, result, text, true);
        result.sort(Comparator.comparingInt(Amount::start));
        return List.copyOf(result);
    }

    private static void collect(Matcher matcher, int group, List<Amount> result, String text, boolean checkUnit) {
        while (matcher.find()) {
            double value = parseNumber(matcher.group(group));
            int start = matcher.start(group), end = matcher.end(group);
            if (!Double.isFinite(value) || (checkUnit && OTHER_CURRENCY.matcher(text.substring(end)).find())) continue;
            if (result.stream().anyMatch(a -> start < a.end() && end > a.start())) continue;
            result.add(new Amount(start, end, value, matcher.group(group)));
        }
    }

    public static OptionalDouble purse(String input) {
        Matcher matcher = BALANCE.matcher(plain(input));
        while (matcher.find()) {
            String label = matcher.group(1).toLowerCase(Locale.ROOT);
            if (!(label.equals("purse") || label.startsWith("piggy"))) continue;
            double amount = parseNumber(matcher.group(2));
            if (Double.isFinite(amount) && amount >= 0) return OptionalDouble.of(amount);
        }
        return OptionalDouble.empty();
    }

    public static boolean isBalance(String input) { return BALANCE.matcher(plain(input)).find(); }
    public static boolean isAnnotated(String input) {
        if (input.contains(" cookies | $") || input.contains(" [$") || input.contains(" [<$")
                || input.contains(" [-$") || input.contains(" [-<$")
                || COOKIE_ANNOTATION.matcher(input).find()) return true;
        // Recognize our visible equivalents even after a component is copied, serialized,
        // or rebuilt. This is independent of object identity and of the current currency.
        if (!LAYOUT_SEPARATOR.matcher(input).find()) return false;
        Matcher marker = EQUIVALENT.matcher(input);
        while (marker.find()) {
            if (marker.group(1) == null) return true;
            try { MoneyCurrency.code(marker.group(1)); return true; }
            catch (IllegalArgumentException ignored) { } // BIN / RNG are not money currencies.
        }
        return false;
    }
}

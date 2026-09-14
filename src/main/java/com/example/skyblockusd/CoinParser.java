package com.example.skyblockusd;

import net.minecraft.network.chat.FormattedText;
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
    // Full labels only: never match Cost inside Mana Cost or Price inside Soulflow Price.
    // Generic Cost is ambiguous without an explicit coins unit.
    private static final String LABEL_START = "(?:^|[\\r\\n|;/])\\h*";
    private static final Pattern BALANCE = Pattern.compile("(?i)" + LABEL_START + "(Purse|Piggy(?: Bank)?|Bank|Balance|Coins):\\h*(" + NUMBER + ")(?![\\w.,])");
    private static final Pattern PRICE = Pattern.compile("(?i)" + LABEL_START + "(?:Buy price|Sell price|Price per unit|Price|Starting bid|Top bid|Your bid|BIN price|Buy it now):\\h*(" + NUMBER + ")(?![\\w.,])");
    // Unknown units, icons and percentages fail closed rather than being assumed to be coins.
    private static final Pattern IMPLICIT_TAIL = Pattern.compile("(?i)^\\h*(?:coins?\\b|$|[|;/\\r\\n]|\\([+-][0-9,]+(?:\\.[0-9]+)?\\)(?:\\h|$))");
    private static final Pattern KNOWN_PURSE_VALUE = Pattern.compile("^\\h*([0-9]+(?:[.,'’\\h][0-9]+)*[kKmMbBtTqQ]?)(?:\\h*\\([+-][0-9.,'’\\hkKmMbBtTqQ]+\\))?\\h*$");
    private static final Pattern COOKIE_ANNOTATION = Pattern.compile("\\[[+-]?<?[0-9,.]+ cookies(?:[ |\\]]|$)");
    private static final Pattern LAYOUT_SEPARATOR = Pattern.compile(" \\[| \\(| \\| | = ");
    private static final Pattern EQUIVALENT = Pattern.compile("\\p{Sc}[0-9,.]+|\\b([A-Z]{3}) <?[0-9,.]+|[0-9,.]+ cookies\\b");
    public record Amount(int start, int end, double coins, String source) { }
    private CoinParser() { }
    public static String plain(String value) {
        return value == null ? "" : VisibleText.plain(FormattedText.of(value));
    }
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
            if (!Double.isFinite(value) || (checkUnit && !IMPLICIT_TAIL.matcher(text.substring(end)).find())) continue;
            if (result.stream().anyMatch(a -> start < a.end() && end > a.start())) continue;
            result.add(new Amount(start, end, value, matcher.group(group)));
        }
    }
    /** CustomScoreboard's purse field is already typed. Use its raw value, not a
     * localized/abbreviated display string, as the conversion amount. */
    static List<Amount> knownPurseValue(String input, double exactCoins) {
        String text = plain(input);
        if (!Double.isFinite(exactCoins) || exactCoins < 0 || text.length() > 16_384 || isAnnotated(text)) return List.of();
        Matcher matcher = KNOWN_PURSE_VALUE.matcher(text);
        if (!matcher.matches()) return List.of();
        return List.of(new Amount(matcher.start(1), matcher.end(1), exactCoins, matcher.group(1)));
    }
    public static OptionalDouble purse(String input) {
        String text = plain(input);
        Matcher matcher = BALANCE.matcher(text);
        while (matcher.find()) {
            String label = matcher.group(1).toLowerCase(Locale.ROOT);
            if (!(label.equals("purse") || label.startsWith("piggy"))) continue;
            double amount = parseNumber(matcher.group(2));
            if (Double.isFinite(amount) && amount >= 0 && IMPLICIT_TAIL.matcher(text.substring(matcher.end(2))).find()) return OptionalDouble.of(amount);
        }
        return OptionalDouble.empty();
    }
    public static boolean isBalance(String input) { return BALANCE.matcher(plain(input)).find(); }
    public static boolean isAnnotated(String input) {
        if (input.contains(" cookies | $") || input.contains(" [$") || input.contains(" [<$")
                || input.contains(" [-$") || input.contains(" [-<$")
                || COOKIE_ANNOTATION.matcher(input).find()) return true;
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

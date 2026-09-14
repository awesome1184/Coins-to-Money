package com.example.skyblockusd;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Replace currency spans only, preserving surrounding text, styles and interactions. */
public final class CoinText {
    private static final Pattern COIN_UNIT = Pattern.compile("(?i)^\\h*coins?\\b");
    private record Part(int start, int end, String text, Style style) { }
    private CoinText() { }

    public static Component convert(Component original, boolean balances, boolean prices) {
        return convert(original, balances, prices, CookiePriceFetcher.state(), System.currentTimeMillis(), ModConfig.INSTANCE);
    }
    static Component convert(Component original, boolean balances, boolean prices,
                             CookiePriceFetcher.State state, long now, ModConfig config) {
        if (original == null || !config.enabled || !state.available(now) || (!config.showUsd && !config.showCookies)) return original;
        String raw = original.getString();
        if (raw.length() > 16_384) return original;
        // Match the same decoded component runs that will be used for replacements.
        // A regex over getString() rejected Hypixel's invisible section-p marker and
        // could pair a trailing section sign with the next component's first digit.
        var amounts = CoinParser.find(VisibleText.plain(original), balances, prices);
        if (amounts.isEmpty()) return original;
        return replace(original, amounts, state, now, config);
    }

    /** Only call this for values already known to be coins. Keeps localized/compact
     * original text but computes from the unrounded, read-only source amount. */
    public static Component convertKnownCoinValue(Component original, double coins) {
        return convertKnownCoinValue(original, coins, CookiePriceFetcher.state(), System.currentTimeMillis(), ModConfig.INSTANCE);
    }
    static Component convertKnownCoinValue(Component original, double coins,
            CookiePriceFetcher.State state, long now, ModConfig config) {
        if (original == null || !config.enabled || !state.available(now)
                || (!config.showUsd && !config.showCookies) || !Double.isFinite(coins)) return original;
        String text = VisibleText.plain(original);
        if (text.length() > 16_384 || CoinParser.isAnnotated(text)) return original;
        var matcher = KNOWN_VALUE.matcher(text);
        if (!matcher.matches()) return original;
        return replace(original, List.of(new CoinParser.Amount(matcher.start(1), matcher.end(1), coins, matcher.group(1))), state, now, config);
    }
    /** Fallback for an older optional API: only accept a complete US-format value. */
    public static Component convertKnownCoinValue(Component original) {
        if (original == null) return null;
        var matcher = KNOWN_VALUE.matcher(VisibleText.plain(original));
        return matcher.matches() ? convertKnownCoinValue(original, CoinParser.parseNumber(matcher.group(1))) : original;
    }
    private static final Pattern KNOWN_VALUE = Pattern.compile(
            "^\\h*([+-]?[0-9]+(?:[.,'’\\h][0-9]+)*[kKmMbBtTqQ]?)(?:\\h*\\([+-][0-9.,'’\\hkKmMbBtTqQ]+\\))?\\h*$");

    static Component convertProfitText(Component original) {
        var state = CookiePriceFetcher.state(); long now = System.currentTimeMillis(); var config = ModConfig.INSTANCE;
        if (original == null || !config.enabled || !state.available(now) || (!config.showUsd && !config.showCookies)) return original;
        String text = VisibleText.plain(original);
        if (text.length() > 16_384) return original;
        return replace(original, ProfitText.find(text), state, now, config);
    }

    private static Component replace(Component original, List<CoinParser.Amount> amounts,
                                     CookiePriceFetcher.State state, long now, ModConfig config) {
        // Component siblings and legacy formatting can split a number at ANY digit.
        List<Part> parts = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        VisibleText.visit(original, (index, characterStyle, codepoint) -> {
            int start = text.length();
            String value = new String(Character.toChars(codepoint));
            text.append(value);
            parts.add(new Part(start, text.length(), value, characterStyle));
            return true;
        });
        MutableComponent output = Component.empty().withStyle(original.getStyle());
        int cursor = 0;
        boolean changed = false;
        for (var amount : amounts) {
            CoinConversion value;
            try {
                value = CoinConversion.of(amount.coins(), state.quote().instantBuyPrice());
                if (config.showUsd) value.moneyText(config);
            }
            catch (IllegalArgumentException ex) { continue; }
            if (amount.start() < cursor) continue;
            int end = amount.end();
            var unit = COIN_UNIT.matcher(text.substring(end));
            if (unit.find()) end += unit.end();
            appendRange(output, parts, cursor, amount.start());
            List<DisplayOrder.Kind> visible = config.displayOrder.parts.stream().filter(kind -> switch (kind) {
                case COINS -> config.keepCoins;
                case MONEY -> config.showUsd;
                case COOKIES -> config.showCookies;
            }).toList();
            Style originalStyle = styleAt(parts, amount.start());
            Style green = originalStyle.withColor(ChatFormatting.GREEN);
            for (int i = 0; i < visible.size(); i++) {
                if (i > 0) output.append(Component.literal(i == 1 ? config.displayLayout.firstSeparator : config.displayLayout.separator)
                        .withStyle(originalStyle));
                switch (visible.get(i)) {
                    case COINS -> appendRange(output, parts, amount.start(), end);
                    case MONEY -> output.append(Component.literal(value.moneyText(config)).withStyle(green));
                    case COOKIES -> output.append(Component.literal(value.cookieText(config.cookieDecimalPlaces)).withStyle(green));
                }
            }
            if (visible.size() > 1) output.append(Component.literal(config.displayLayout.end).withStyle(originalStyle));
            if (state.stale(now)) output.append(Component.literal(" (stale)").withStyle(originalStyle.withColor(ChatFormatting.GRAY)));
            cursor = end;
            changed = true;
        }
        if (!changed) return original;
        appendRange(output, parts, cursor, text.length());
        return output;
    }
    private static Style styleAt(List<Part> parts, int position) {
        return parts.stream().filter(part -> position >= part.start && position < part.end)
                .findFirst().map(Part::style).orElse(Style.EMPTY);
    }
    private static void appendRange(MutableComponent output, List<Part> parts, int start, int end) {
        StringBuilder run = new StringBuilder();
        Style style = null;
        for (Part part : parts) {
            if (part.end <= start) continue;
            if (part.start >= end) break;
            if (style != null && !style.equals(part.style)) {
                output.append(Component.literal(run.toString()).withStyle(style));
                run.setLength(0);
            }
            style = part.style;
            run.append(part.text, Math.max(0, start - part.start), Math.min(part.text.length(), end - part.start));
        }
        if (!run.isEmpty()) output.append(Component.literal(run.toString()).withStyle(style));
    }
}

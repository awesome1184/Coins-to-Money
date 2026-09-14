package com.example.skyblockusd;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        if (raw.length() > 16_384 || CoinParser.find(raw, balances, prices).isEmpty()) return original;
        // Component siblings and legacy formatting can split a number at ANY digit.
        List<Part> parts = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        original.visit((style, literal) -> {
            StringDecomposer.iterateFormatted(literal, style, (index, characterStyle, codepoint) -> {
                if (Character.getType(codepoint) != Character.FORMAT) {
                    int start = text.length();
                    String value = new String(Character.toChars(codepoint));
                    text.append(value);
                    parts.add(new Part(start, text.length(), value, characterStyle));
                }
                return true;
            });
            return Optional.empty();
        }, Style.EMPTY);
        var amounts = CoinParser.find(text.toString(), balances, prices);
        if (amounts.isEmpty()) return original;
        MutableComponent output = Component.empty().withStyle(original.getStyle());
        int cursor = 0;
        boolean changed = false;
        for (var amount : amounts) {
            CoinConversion value;
            try {
                value = CoinConversion.of(amount.coins(), state.quote().instantBuyPrice());
                if (config.showUsd) value.moneyText(config); // Fail closed on invalid manual rates/overflow.
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

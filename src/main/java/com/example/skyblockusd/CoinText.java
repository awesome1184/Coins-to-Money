package com.example.skyblockusd;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Replace visible currency spans, retaining the styles/click/hover data of surrounding text. */
public final class CoinText {
    private static final Pattern COIN_UNIT = Pattern.compile("(?i)^\\h*coins?\\b");
    private CoinText() { }

    public static Component replace(Component original, boolean balances, boolean prices) {
        return replace(original, balances, prices, CookiePriceFetcher.state(), System.currentTimeMillis());
    }

    static Component replace(Component original, boolean balances, boolean prices,
                             CookiePriceFetcher.State state, long now) {
        if (original == null || !state.available(now)) return original;
        StyledText text = new StyledText(original);
        List<CoinParser.Amount> amounts = CoinParser.find(text.value(), balances, prices);
        if (amounts.isEmpty()) return original;
        MutableComponent result = Component.empty().withStyle(original.getStyle());
        int cursor = 0;
        for (CoinParser.Amount amount : amounts) {
            CoinConversion conversion;
            try { conversion = CoinConversion.of(amount.coins(), state.quote().instantBuyPrice()); }
            catch (IllegalArgumentException ex) { continue; }
            text.appendRange(result, cursor, amount.start());
            String dollars = conversion.dollars(ModConfig.INSTANCE.decimalPlaces);
            if (amount.source().startsWith("+") && amount.coins() >= 0) dollars = "+" + dollars;
            if (ModConfig.INSTANCE.showCookies) dollars += " [" + conversion.cookieCount() + " cookies]";
            if (state.stale(now)) dollars += " (stale)";
            result.append(Component.literal(dollars).withStyle(text.styleAt(amount.start())));
            cursor = amount.end();
            var unit = COIN_UNIT.matcher(text.value().substring(cursor));
            if (unit.find()) cursor += unit.end();
        }
        if (cursor == 0) return original;
        text.appendRange(result, cursor, text.value().length());
        return result;
    }

    /** The same formatted-character iterator used by vanilla; match offsets are visible UTF-16 offsets. */
    private static final class StyledText {
        private record Run(int start, int end, Style style) { }
        private final StringBuilder plain = new StringBuilder();
        private final List<Run> runs = new ArrayList<>();
        StyledText(Component component) {
            component.visit((style, text) -> {
                StringDecomposer.iterateFormatted(text, style, (index, effectiveStyle, codePoint) -> {
                    if (Character.getType(codePoint) != Character.FORMAT) {
                        int start = plain.length();
                        plain.appendCodePoint(codePoint);
                        if (!runs.isEmpty() && runs.getLast().style().equals(effectiveStyle)) {
                            Run last = runs.removeLast();
                            runs.add(new Run(last.start(), plain.length(), effectiveStyle));
                        } else runs.add(new Run(start, plain.length(), effectiveStyle));
                    }
                    return true;
                });
                return Optional.empty();
            }, Style.EMPTY);
        }
        String value() { return plain.toString(); }
        Style styleAt(int offset) {
            for (Run run : runs) if (offset >= run.start() && offset < run.end()) return run.style();
            return Style.EMPTY;
        }
        void appendRange(MutableComponent out, int start, int end) {
            for (Run run : runs) {
                int from = Math.max(start, run.start()), to = Math.min(end, run.end());
                if (from < to) out.append(Component.literal(plain.substring(from, to)).withStyle(run.style()));
            }
        }
    }
}

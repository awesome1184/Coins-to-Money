package com.example.skyblockusd;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.List;

/** Keep the original component tree, styles, hover data and click targets intact. */
public final class CoinText {
    private CoinText() { }
    public static Component annotate(Component original, boolean balances, boolean prices) {
        return annotate(original, balances, prices, CookiePriceFetcher.state(), System.currentTimeMillis());
    }
    static Component annotate(Component original, boolean balances, boolean prices, CookiePriceFetcher.State state, long now) {
        if (original == null || !state.available(now)) return original;
        List<CoinParser.Amount> amounts = CoinParser.find(original.getString(), balances, prices);
        if (amounts.isEmpty()) return original;
        MutableComponent result = original.copy();
        for (CoinParser.Amount amount : amounts) {
            CoinConversion value;
            try { value = CoinConversion.of(amount.coins(), state.quote().instantBuyPrice()); }
            catch (IllegalArgumentException ex) { continue; }
            String prefix = amounts.size() > 1 ? amount.source() + " coins = " : "";
            result.append(Component.literal(" [" + prefix + value.display() + (state.stale(now) ? "; stale rate" : "") + "]").withStyle(ChatFormatting.GREEN));
        }
        return result;
    }
}

package com.example.skyblockusd;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoinTextTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final CookiePriceFetcher.State LIVE = new CookiePriceFetcher.State(new BazaarQuote(10_000_000, NOW), null);
    @Test void completeStyledPursePreservesSuffixAndOriginalComponent() {
        Component raw = Component.literal("Purse: ").withStyle(ChatFormatting.GOLD).append(Component.literal("7,014,5")).append(Component.literal("56"));
        Component result = CoinText.annotate(raw, true, false, LIVE, NOW);
        assertTrue(result.getString().startsWith("Purse: 7,014,556 ["));
        assertTrue(result.getString().contains("0.701 cookies | $2.07 USD"));
        assertEquals(ChatFormatting.GOLD.getColor().intValue(), result.getStyle().getColor().getValue());
        assertEquals("Purse: 7,014,556", raw.getString());
        assertSame(result, CoinText.annotate(result, true, false, LIVE, NOW));
    }
    @Test void leavesOriginalUntouchedUntilAUsableQuoteExists() {
        Component raw = Component.literal("Price: 5,000,000 coins");
        assertSame(raw, CoinText.annotate(raw, true, true, new CookiePriceFetcher.State(null, null), NOW));
        assertSame(raw, CoinText.annotate(raw, true, true, LIVE, NOW + 31 * 60_000));
    }
    @Test void marksStaleRateAndConvertsEveryPrice() {
        String result = CoinText.annotate(Component.literal("Buy: 10,000,000 coins; Sell: 5,000,000 coins"), false, true, LIVE, NOW + 4 * 60_000).getString();
        assertTrue(result.contains("1.000 cookies | $2.95 USD; stale rate"));
        assertTrue(result.contains("0.500 cookies | $1.48 USD; stale rate"));
    }
}

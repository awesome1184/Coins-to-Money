package com.example.skyblockusd;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class IntegrationValuesTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final CookiePriceFetcher.State LIVE = new CookiePriceFetcher.State(new BazaarQuote(12_295_597.2, NOW), null);
    @ParameterizedTest @ValueSource(strings = {"7,416,701", "7.416.701", "7 416 701", "7\u202f416\u202f701", "7,42M", "7.42M", "7416701"})
    void localizedPurseUsesRawCoinsAndPreservesSource(String text) {
        var c = new ModConfig(); c.decimalPlaces = 8;
        var original = Component.literal(text).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" (+5)").withStyle(ChatFormatting.YELLOW));
        String expected = CoinConversion.of(7_416_701, LIVE.quote().instantBuyPrice()).moneyText(c);
        assertEquals(expected + " (+5)", CoinText.convertKnownCoinValue(original, 7_416_701, LIVE, NOW, c).getString());
        assertEquals(text + " (+5)", original.getString());
        c.keepCoins = true;
        assertEquals(text + " [" + expected + "] (+5)", CoinText.convertKnownCoinValue(original, 7_416_701, LIVE, NOW, c).getString());
    }
    @ParameterizedTest @ValueSource(strings = {"Session Profit: 7,416,701 coins", "Profit Per Hour: 7.42m coins", "Profit per catch: 200", "Profit per hour: -1.5m", "Coins/hr: 10M", "Coins made: 2,000", "Selling Rough Ruby for 3.5 each"})
    void profitLabelsAreRecognizedOnlyInTheProfitAdapter(String text) {
        assertEquals(1, ProfitText.find(text).size(), text);
    }
    @ParameterizedTest @ValueSource(strings = {"Mana Cost: 50", "Soulflow Cost: 1", "Health: 123", "Mana Balance: 50", "Profit: 50%", "Profit: 50 Mana", "Experience per hour: 123", "Crops per hour: 12M", "Skill XP: 1.2M", "Kills per hour: 5k", "Uptime: 01:02:03", "Farming Fortune: 1600", "RNG Meter: 50%"})
    void resourcesCountsAndRatiosNeverBecomeMoney(String text) { assertTrue(ProfitText.find(text).isEmpty(), text); }
    @Test void typedNegativeProfitAndForeignCurrencyUseExactAmount() {
        var c = new ModConfig(); c.currencyCode = "EUR"; c.currencyPerUsd = .92;
        assertEquals("-€2.72", CoinText.convertKnownCoinValue(Component.literal("§c-12.30M"), -12_295_597.2, LIVE, NOW, c).getString());
    }
    @Test void typedValuesFailClosed() {
        var c = new ModConfig();
        for (String text : new String[]{"50 mana", "50✎", "50%", "$2.95", "2 coins", "1/4"}) {
            var original = Component.literal(text);
            assertSame(original, CoinText.convertKnownCoinValue(original, 50, LIVE, NOW, c));
        }
        var original = Component.literal("50");
        assertSame(original, CoinText.convertKnownCoinValue(original, Double.NaN, LIVE, NOW, c));
        assertSame(original, CoinText.convertKnownCoinValue(original, 50, new CookiePriceFetcher.State(null, "offline"), NOW, c));
    }
    @Test void allTypedDisplayCombinationsAreIdempotent() {
        for (var order : DisplayOrder.values()) for (var layout : DisplayLayout.values()) for (int mask = 1; mask < 8; mask++) {
            var c = new ModConfig(); c.displayOrder = order; c.displayLayout = layout;
            c.keepCoins = (mask & 1) != 0; c.showUsd = (mask & 2) != 0; c.showCookies = (mask & 4) != 0;
            var input = Component.literal("7,42M (+5)");
            var out = CoinText.convertKnownCoinValue(input, 7_416_701, LIVE, NOW, c);
            assertTrue(out.getString().endsWith(" (+5)"));
            assertEquals(out.getString(), CoinText.convertKnownCoinValue(out.copy(), 7_416_701, LIVE, NOW, c).getString());
        }
    }
}

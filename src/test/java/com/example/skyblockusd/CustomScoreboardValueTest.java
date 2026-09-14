package com.example.skyblockusd;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CustomScoreboardValueTest {
    private static final long NOW = 1_800_000_000_000L;
    private final CookiePriceFetcher.State state = new CookiePriceFetcher.State(new BazaarQuote(12_295_597.2, NOW), null);
    @ParameterizedTest
    @ValueSource(strings = {"7,416,701", "7.416.701", "7 416 701", "7\u202f416\u202f701", "7.42M", "7,42M", "7416701"})
    void usesExactPurseNotAbbreviatedOrLocalizedText(String text) {
        Component input = Component.literal(text + " (+5)").withStyle(ChatFormatting.GOLD);
        ModConfig config = new ModConfig(); config.decimalPlaces = 8;
        String money = CoinConversion.of(7_416_701, state.quote().instantBuyPrice()).moneyText(config);
        assertEquals(money + " (+5)", CoinText.convertPurseValue(input, 7_416_701, state, NOW, config).getString());
        config.keepCoins = true;
        assertEquals(text + " [" + money + "] (+5)", CoinText.convertPurseValue(input, 7_416_701, state, NOW, config).getString());
    }
    @Test void knownPurseStillHonoursAllDisplayCombinations() {
        for (DisplayOrder order : DisplayOrder.values()) for (DisplayLayout layout : DisplayLayout.values()) for (int mask = 1; mask < 8; mask++) {
            ModConfig config = new ModConfig(); config.displayOrder = order; config.displayLayout = layout;
            config.keepCoins = (mask & 1) != 0; config.showUsd = (mask & 2) != 0; config.showCookies = (mask & 4) != 0;
            Component input = Component.literal("7.42M (+5)").withStyle(ChatFormatting.GOLD);
            Component result = CoinText.convertPurseValue(input, 7_416_701, state, NOW, config);
            assertTrue(result.getString().endsWith(" (+5)"));
            assertEquals(result.getString(), CoinText.convertPurseValue(result.copy(), 7_416_701, state, NOW, config).getString());
            assertEquals(config.keepCoins, result.getString().contains("7.42M"));
            assertEquals(config.showUsd, result.getString().contains("$"));
            assertEquals(config.showCookies, result.getString().contains("cookies"));
        }
    }
    @Test void missingRatesInvalidAmountsAndUnrecognizedValuesFailClosed() {
        ModConfig config = new ModConfig(); Component input = Component.literal("7,416,701");
        assertSame(input, CoinText.convertPurseValue(input, Double.NaN, state, NOW, config));
        assertSame(input, CoinText.convertPurseValue(input, -1, state, NOW, config));
        assertSame(input, CoinText.convertPurseValue(input, 7_416_701, new CookiePriceFetcher.State(null, "offline"), NOW, config));
        for (String text : new String[]{"50✎", "50 mana", "1/10", "IMMUNE", "$1.78", "50 coins"}) {
            Component value = Component.literal(text);
            assertSame(value, CoinText.convertPurseValue(value, 50, state, NOW, config));
        }
    }
    @Test void onlyPurseAndPiggyAreSemanticCoinFields() {
        assertTrue(CustomScoreboardCompat.coinLabel("Purse")); assertTrue(CustomScoreboardCompat.coinLabel("§6Piggy"));
        for (String label : new String[]{"Mana", "Bits", "Motes", "Gems", "Heat", "Copper", "Soulflow", "Bank"}) assertFalse(CustomScoreboardCompat.coinLabel(label));
    }
    @Test void chunkSerializerPreservesGreenValuesGoldCoinsAndGain() {
        Component input = Component.literal("§67,416,701 §e(+5)"); ModConfig config = new ModConfig();
        config.keepCoins = true; config.showCookies = true;
        Component result = CoinText.convertPurseValue(input, 7_416_701, state, NOW, config);
        String legacy = CustomScoreboardCompat.legacy(result, input);
        assertEquals(result.getString(), CoinParser.plain(legacy)); assertTrue(legacy.contains("§a"));
        assertTrue(legacy.contains("§6")); assertTrue(legacy.contains("§e"));
    }
}

package com.example.skyblockusd;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class CoinTextTest {
    static final long NOW = 1_800_000_000_000L;
    static final CookiePriceFetcher.State LIVE = new CookiePriceFetcher.State(new BazaarQuote(12_295_597.2, NOW), null);
    private Component convert(Component c, ModConfig cfg) { return CoinText.convert(c, true, true, LIVE, NOW, cfg); }

    @Test void screenshotExamplesReplaceAmountsAndUnitsInPlace() {
        for (String[] row : new String[][]{{"Worth 13.6M coins", "Worth $3.27"},
                {"Price per unit: 57,716.6 coins", "Price per unit: $0.01"}, {"Purse: 7,416,611", "Purse: $1.78"}}) {
            assertEquals(row[1], convert(Component.literal(row[0]), new ModConfig()).getString());
        }
    }
    @Test void completeStyledPursePreservesSuffixAndOriginalComponent() {
        Component raw = Component.literal("Purse: ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal("7,416,6").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("11").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" (+56)").withStyle(ChatFormatting.GREEN));
        Component result = convert(raw, new ModConfig());
        assertEquals("Purse: $1.78 (+56)", result.getString());
        assertEquals("Purse: 7,416,611 (+56)", raw.getString());
        var colors = new ArrayList<Integer>();
        result.visit((style, s) -> { if (s.contains("$")) colors.add(style.getColor().getValue()); return Optional.empty(); }, Style.EMPTY);
        assertEquals(ChatFormatting.GOLD.getColor(), colors.getFirst());
        assertSame(result, convert(result, new ModConfig()));
    }
    @Test void legacyCodesAndInvisibleCharactersDoNotCorruptOffsets() {
        assertEquals("Price: $1.78 each", convert(Component.literal("§7Price: §67,§e416,\u200b611 §6coins §aeach"), new ModConfig()).getString());
    }
    @Test void leavesOriginalUntouchedUntilAUsableQuoteExists() {
        Component raw = Component.literal("Price: 5,000,000 coins");
        assertSame(raw, CoinText.convert(raw, true, true, new CookiePriceFetcher.State(null, null), NOW, new ModConfig()));
        assertSame(raw, CoinText.convert(raw, true, true, LIVE, NOW + 31 * 60_000, new ModConfig()));
    }
    @Test void marksStaleRateAndConvertsEveryPrice() {
        String result = CoinText.convert(Component.literal("Buy: 12,295,597.2 coins; Sell: 6,147,798.6 coins"), false, true, LIVE, NOW + 4 * 60_000, new ModConfig()).getString();
        assertEquals("Buy: $2.95 (stale); Sell: $1.48 (stale)", result);
    }
    @Test void eachDisplayCombinationAndRepeatedConversionAreSafe() {
        for (int mask = 1; mask < 8; mask++) {
            var cfg = new ModConfig();
            cfg.showUsd = (mask & 1) != 0; cfg.showCookies = (mask & 2) != 0; cfg.keepCoins = (mask & 4) != 0;
            Component raw = Component.literal("Cost: 12,295,597.2 coins");
            Component result = convert(raw, cfg);
            assertEquals(cfg.showUsd, result.getString().contains("$"));
            assertEquals(cfg.showCookies, result.getString().contains("cookies"));
            assertEquals(cfg.keepCoins, result.getString().contains("12,295,597.2 coins"));
            assertSame(result, convert(result, cfg));
        }
    }
    @Test void customPrecisionAndNegativeTinyPrices() {
        var cfg = new ModConfig(); cfg.decimalPlaces = 4;
        assertEquals("Cost: $2.9545", convert(Component.literal("Cost: 12,295,597.2 coins"), cfg).getString());
        assertEquals("You lost -<$0.0001", convert(Component.literal("You lost -1 coins"), cfg).getString());
    }
    @Test void dollarsAndOtherCurrenciesAreNotConvertedAgain() {
        for (String s : new String[]{"Price: 325 Gems", "Bits: 7,120", "Cost: $500 coins", "Price: $1.00", "Amount: 246x", "Filled: 171/246 (69.5%)"}) {
            Component raw = Component.literal(s); assertSame(raw, convert(raw, new ModConfig()));
        }
    }
    @Test void disablingConversionReturnsTheSameComponent() {
        var cfg = new ModConfig(); cfg.enabled = false;
        Component raw = Component.literal("100 coins"); assertSame(raw, convert(raw, cfg));
    }
    @Test void configMigrationDropsOldHudAndClampsInvalidDisplaySettings() {
        ModConfig cfg = new com.google.gson.Gson().fromJson("{\"schemaVersion\":2,\"showGui\":true,\"decimalPlaces\":999,\"cookieDecimalPlaces\":-4}", ModConfig.class);
        cfg.normalize();
        assertEquals(3, cfg.schemaVersion); assertEquals(8, cfg.decimalPlaces); assertEquals(1, cfg.cookieDecimalPlaces);
        assertTrue(cfg.showUsd); assertFalse(cfg.showCookies); assertFalse(cfg.keepCoins);
        assertFalse(new com.google.gson.Gson().toJson(cfg).contains("showGui"));
        cfg.showUsd = false; cfg.normalize(); assertTrue(cfg.showUsd);
    }
}

package com.example.skyblockusd;

import com.google.gson.Gson;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.ArrayList;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class DisplayAndCurrencyTest {
    private Component convert(String text, ModConfig config) {
        return CoinText.convert(Component.literal(text).withStyle(ChatFormatting.GOLD), true, true,
                CoinTextTest.LIVE, CoinTextTest.NOW, config);
    }
    @Test void supportsAllOrdersLayoutsAndVisibilityMasksWithoutDoubleConversion() {
        for (DisplayOrder order : DisplayOrder.values()) for (DisplayLayout layout : DisplayLayout.values()) for (int mask = 1; mask < 8; mask++) {
            var c = new ModConfig(); c.displayOrder = order; c.displayLayout = layout;
            c.keepCoins = (mask & 1) != 0; c.showUsd = (mask & 2) != 0; c.showCookies = (mask & 4) != 0;
            Component result = convert("Cost: 12,295,597.2 coins", c);
            var selected = new ArrayList<String>();
            for (var kind : order.parts) switch (kind) {
                case COINS -> { if (c.keepCoins) selected.add("12,295,597.2 coins"); }
                case MONEY -> { if (c.showUsd) selected.add("$2.95"); }
                case COOKIES -> { if (c.showCookies) selected.add("1.000 cookies"); }
            }
            String expected = selected.getFirst();
            if (selected.size() > 1) expected += layout.firstSeparator + String.join(layout.separator, selected.subList(1, selected.size())) + layout.end;
            assertEquals("Cost: " + expected, result.getString(), order + "/" + layout + "/" + mask);
            assertSame(result, CoinText.convert(result, true, true, CoinTextTest.LIVE, CoinTextTest.NOW, c));
            Component copy = result.copy();
            assertSame(copy, CoinText.convert(copy, true, true, CoinTextTest.LIVE, CoinTextTest.NOW, c));
            Component rebuilt = Component.literal(result.getString());
            assertSame(rebuilt, CoinText.convert(rebuilt, true, true, CoinTextTest.LIVE, CoinTextTest.NOW, c));
        }
    }
    @ParameterizedTest @ValueSource(strings = {"USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "RSD", "CNY", "INR"})
    void reformattedTextDoesNotDependOnWeakObjectIdentity(String code) {
        for (var order : DisplayOrder.values()) for (var layout : DisplayLayout.values()) {
            var c = new ModConfig(); c.currencyCode = code; c.currencyPerUsd = code.equals("USD") ? 1 : .92;
            c.showCookies = false; c.keepCoins = true; c.displayOrder = order; c.displayLayout = layout;
            Component initial = convert("Purse: 12,295,597.2", c);
            Component rebuilt = Component.literal(initial.getString());
            assertSame(rebuilt, CoinText.convert(rebuilt, true, true, CoinTextTest.LIVE, CoinTextTest.NOW, c));
        }
    }
    @Test void convertedValuesAreGreenAndCoinsKeepTheirOwnColour() {
        var c = new ModConfig(); c.keepCoins = true; c.showCookies = true;
        c.currencyCode = "EUR"; c.currencyPerUsd = 0.92; c.displayOrder = DisplayOrder.MONEY_COINS_COOKIES;
        var result = convert("Cost: 12,295,597.2 coins", c);
        result.visit((style, text) -> {
            if (text.contains("€") || text.contains("cookies")) assertEquals(0x55FF55, style.getColor().getValue());
            if (text.contains("12,295,597.2 coins")) assertEquals(0xFFAA00, style.getColor().getValue());
            return Optional.empty();
        }, Style.EMPTY);
        assertEquals("Cost: €2.72 [12,295,597.2 coins | 1.000 cookies]", result.getString());
    }
    @Test void currencyConversionDoesNotChangeCookieCountOrUsdBasis() {
        var c = new ModConfig(); c.currencyCode = "EUR"; c.currencyPerUsd = 0.92;
        var value = CoinConversion.of(12_295_597.2, 12_295_597.2);
        assertEquals(1d, value.cookies()); assertEquals(325d * 100 / 11000, value.usd());
        assertEquals("€2.72", value.moneyText(c));
        assertEquals("EUR", MoneyCurrency.code(" eur "));
        assertEquals(0.92, MoneyCurrency.rate(" 0.92 "));
    }
    @ParameterizedTest @ValueSource(strings = {"USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "RSD", "CNY", "INR"})
    void currenciesSurviveSaveLoadAndFormat(String code) {
        var c = new ModConfig(); c.currencyCode = code; c.currencyPerUsd = code.equals("USD") ? 1 : 123.45;
        c.displayOrder = DisplayOrder.COOKIES_MONEY_COINS; c.displayLayout = DisplayLayout.EQUALS;
        c = new Gson().fromJson(new Gson().toJson(c), ModConfig.class); c.normalize();
        assertEquals(code, c.currencyCode); assertEquals(DisplayLayout.EQUALS, c.displayLayout);
        assertEquals(DisplayOrder.COOKIES_MONEY_COINS, c.displayOrder);
        assertTrue(CoinConversion.of(0, 1).moneyText(c).endsWith("0.00"));
    }
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "NaN", "Infinity", "", "1,5", "1e9", "0x1p2", "1000000000001"})
    void invalidRatesCannotBeApplied(String value) { assertThrows(IllegalArgumentException.class, () -> MoneyCurrency.rate(value)); }
    @Test void invalidSavedCurrencySettingsResetTogether() {
        var c = new ModConfig(); c.currencyCode = "EUR"; c.currencyPerUsd = Double.NaN;
        c.displayOrder = null; c.displayLayout = null; c.normalize();
        assertEquals("USD", c.currencyCode); assertEquals(1d, c.currencyPerUsd);
        assertNotNull(c.displayOrder); assertNotNull(c.displayLayout);
        assertThrows(IllegalArgumentException.class, () -> MoneyCurrency.code("<§a"));
        assertThrows(IllegalArgumentException.class, () -> MoneyCurrency.code("ZZZ"));
        assertThrows(IllegalArgumentException.class, () -> MoneyCurrency.validate("USD", 2));
        assertThrows(IllegalArgumentException.class, () -> MoneyCurrency.format(Double.MAX_VALUE, "EUR", 10, 2));
    }
    @Test void noIntermediateRoundingAndNegativeAmountsRemainCorrect() {
        assertEquals("€0.92", MoneyCurrency.format(1d, "EUR", .92, 2));
        assertEquals("-€0.92", MoneyCurrency.format(-1d, "EUR", .92, 2));
        assertEquals("<€0.01", MoneyCurrency.format(0.00001, "EUR", .92, 2));
        assertEquals("€0.01", MoneyCurrency.format(.006, "EUR", 2, 2));
    }
}

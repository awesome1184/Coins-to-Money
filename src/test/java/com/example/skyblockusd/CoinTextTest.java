package com.example.skyblockusd;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class CoinTextTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final CookiePriceFetcher.State LIVE = new CookiePriceFetcher.State(new BazaarQuote(10_000_000, NOW), null);
    @BeforeEach void defaults() { ModConfig.INSTANCE = new ModConfig(); }
    private Component replace(Component text) { return CoinText.replace(text, true, true, LIVE, NOW); }
    @Test void replacesWholeStyledPurseWithoutChangingOriginal() {
        Component raw = Component.literal("Purse: ").withStyle(ChatFormatting.GOLD).append(Component.literal("7,014,5")).append(Component.literal("56"));
        Component result = replace(raw);
        assertEquals("Purse: $2.07", result.getString());
        assertEquals("Purse: 7,014,556", raw.getString());
        assertSame(result, replace(result));
        result.visit((style, text) -> {
            assertEquals(ChatFormatting.GOLD.getColor().intValue(), style.getColor().getValue());
            return Optional.empty();
        }, Style.EMPTY);
    }
    @Test void replacesBazaarAndAhAmountsAndTheirUnitsInPlace() {
        assertEquals("Worth $4.02", replace(Component.literal("Worth 13.6M coins")).getString());
        assertEquals("Price per unit: $0.02", replace(Component.literal("Price per unit: 57,716.6 coins")).getString());
        assertEquals("Buy it now: $2.95", replace(Component.literal("Buy it now: 10,000,000 Coins")).getString());
        assertEquals("Buy: $2.95; Sell: $1.48", replace(Component.literal("Buy: 10,000,000 coins; Sell: 5,000,000 coins")).getString());
    }
    @Test void formattingCodesDoNotShiftReplacementOffsets() {
        Component input = Component.literal("§7Price: §610,§a000,000 coins§7 each");
        Component result = replace(input);
        assertEquals("Price: $2.95 each", result.getString());
        result.visit((style, text) -> {
            if (text.contains("$")) assertEquals(ChatFormatting.GOLD.getColor().intValue(), style.getColor().getValue());
            if (text.contains("each")) assertEquals(ChatFormatting.GRAY.getColor().intValue(), style.getColor().getValue());
            return Optional.empty();
        }, Style.EMPTY);
    }
    @Test void preservesComponentMetadataAndUnicode() {
        Style style = Style.EMPTY.withInsertion("original action").withBold(true);
        Component raw = Component.literal("\uD83D\uDCB0 ").append(Component.literal("10,000,000 coins").withStyle(style)).append(" remaining");
        Component result = replace(raw);
        assertEquals("\uD83D\uDCB0 $2.95 remaining", result.getString());
        result.visit((s, text) -> {
            if (text.contains("$")) { assertEquals("original action", s.getInsertion()); assertTrue(s.isBold()); }
            return Optional.empty();
        }, Style.EMPTY);
    }
    @Test void leavesOriginalUntouchedUntilAUsableQuoteExists() {
        Component raw = Component.literal("Price: 5,000,000 coins");
        assertSame(raw, CoinText.replace(raw, true, true, new CookiePriceFetcher.State(null, null), NOW));
        assertSame(raw, CoinText.replace(raw, true, true, LIVE, NOW + 31 * 60_000));
    }
    @Test void staleStatusAndOptionalCookiesAreNotADuplicateCoinPrice() {
        Component raw = Component.literal("10,000,000 coins");
        assertEquals("$2.95 (stale)", CoinText.replace(raw, false, false, LIVE, NOW + 4 * 60_000).getString());
        ModConfig.INSTANCE.showCookies = true;
        Component result = replace(raw);
        assertEquals("$2.95 [1.000 cookies]", result.getString());
        assertSame(result, replace(result));
    }
    @Test void preservesSignsAndDoesNotTurnSmallAmountsIntoFreeItems() {
        assertEquals("+<$0.01", replace(Component.literal("+1 coins")).getString());
        assertEquals("-$2.95", replace(Component.literal("-10,000,000 coins")).getString());
        assertEquals("$0.00", replace(Component.literal("0 coins")).getString());
        assertEquals("Purse: $2.95 (+56)", replace(Component.literal("Purse: 10,000,000 (+56)")).getString());
    }
    @Test void neverRewritesBitsGemsQuantitiesOrMalformedAmounts() {
        for (String value : new String[]{"Bits: 7,120", "Price: 325 Gems", "64x Stone", "Purse: 7,014,5"}) {
            Component original = Component.literal(value);
            assertSame(original, replace(original));
        }
    }
}

package com.example.skyblockusd;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

class CustomScoreboardCompatTest {
    private Object oldContext, oldState;
    private ModConfig oldConfig;
    @BeforeEach void setup() throws Exception {
        oldContext = field(SkyblockUsdModClient.class, "skyblock").get(null);
        oldState = field(CookiePriceFetcher.class, "state").get(null); oldConfig = ModConfig.INSTANCE;
        field(SkyblockUsdModClient.class, "skyblock").set(null, true);
        field(CookiePriceFetcher.class, "state").set(null, new CookiePriceFetcher.State(new BazaarQuote(12_295_597.2, System.currentTimeMillis()), null));
        ModConfig.INSTANCE = new ModConfig();
    }
    @AfterEach void restore() throws Exception {
        field(SkyblockUsdModClient.class, "skyblock").set(null, oldContext);
        field(CookiePriceFetcher.class, "state").set(null, oldState); ModConfig.INSTANCE = oldConfig;
    }
    private static Field field(Class<?> cls, String name) throws Exception {
        Field f = cls.getDeclaredField(name); f.setAccessible(true); return f;
    }
    @Test void onlyTypedPurseAndPiggyAreAdapted() {
        var value = Component.literal("12,295,597.2");
        for (String label : new String[]{"Purse", "Piggy", "Piggy Bank"})
            assertEquals("$2.95", CustomScoreboardCompat.number(Component.literal(label), value).getString());
        for (String label : new String[]{"Mana", "Soulflow", "Bits", "Motes", "Gems", "Copper", "Bank"})
            assertSame(value, CustomScoreboardCompat.number(Component.literal(label), value));
    }
    @Test void amountAndGainRemainSeparate() {
        var value = Component.literal("12,295,597.2").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" (+5)").withStyle(ChatFormatting.YELLOW));
        assertEquals("$2.95 (+5)", CustomScoreboardCompat.number(Component.literal("Purse"), value).getString());
        assertEquals("12,295,597.2 (+5)", value.getString());
    }
    @Test void convertedLegacyChunkKeepsGoldAndGreenAndDoesNotRepeat() {
        ModConfig.INSTANCE.keepCoins = true; ModConfig.INSTANCE.showCookies = true;
        String out = CustomScoreboardCompat.chunkedPurse("§612,295,597.2");
        assertEquals("12,295,597.2 [$2.95 | 1.000 cookies]", CoinParser.plain(out));
        assertTrue(out.contains("§6")); assertTrue(out.contains("§a"));
        assertEquals(out, CustomScoreboardCompat.chunkedPurse(out));
    }
    @Test void hypixelFallbackRetainsFormattingFix() {
        assertEquals("Purse: $1.78 (+5)", CustomScoreboardCompat.vanillaLine(Component.literal("Purse: §67,416,7§p§601 §e(+5)")).getString());
    }
    @Test void disabledAndOffServerLeaveTypedNumberUntouched() throws Exception {
        var value = Component.literal("123"); var label = Component.literal("Purse");
        ModConfig.INSTANCE.enablePurse = false; assertSame(value, CustomScoreboardCompat.number(label, value));
        ModConfig.INSTANCE.enablePurse = true; field(SkyblockUsdModClient.class, "skyblock").set(null, false);
        assertSame(value, CustomScoreboardCompat.number(label, value));
    }
    @Test void absentRateDoesNotInventAValue() throws Exception {
        field(CookiePriceFetcher.class, "state").set(null, new CookiePriceFetcher.State(null, "offline"));
        var value = Component.literal("123"); assertSame(value, CustomScoreboardCompat.number(Component.literal("Purse"), value));
    }
}

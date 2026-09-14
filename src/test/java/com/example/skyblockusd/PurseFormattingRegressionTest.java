package com.example.skyblockusd;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.util.StringDecomposer;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/** Exact v1.2.0 live diagnostic: the amount is in the name, not the blank score column. */
class PurseFormattingRegressionTest {
    static final String RAW = "Purse: §67,416,7§p§601 §e(+5)";
    static final String VISIBLE = "Purse: 7,416,701 (+5)";

    private static Component convert(Component text, ModConfig config) {
        return CoinText.convert(text, true, true, CoinTextTest.LIVE, CoinTextTest.NOW, config);
    }
    private static ScoreboardCoinHelper.Row row(Component name, ModConfig config) {
        return ScoreboardCoinHelper.convertRow(name, Component.empty(), false,
                CoinTextTest.LIVE, CoinTextTest.NOW, config);
    }
    private static String rendered(Component text) { return StringDecomposer.getPlainText(text); }

    @Test void exactDiagnosticMatchesMinecraftAndParsesAllSevenDigits() {
        assertEquals(VISIBLE, rendered(Component.literal(RAW)));
        assertEquals(VISIBLE, CoinParser.plain(RAW));
        assertEquals(7_416_701d, CoinParser.purse(RAW).orElseThrow());
        var amounts = CoinParser.find(RAW, true, false);
        assertEquals(1, amounts.size());
        assertEquals(7_416_701d, amounts.getFirst().coins());
        assertEquals("7,416,701", amounts.getFirst().source());
    }
    @ParameterizedTest
    @ValueSource(strings = {"p", "P", "z", "Z", "g", "j", "Q", "?", "§", "x"})
    void unsupportedCodesBetweenDigitsFollowVanilla(String code) {
        String raw = "Purse: §67,416,7§" + code + "§601 §e(+5)";
        assertEquals(VISIBLE, rendered(Component.literal(raw)));
        assertEquals(VISIBLE, CoinParser.plain(raw));
        assertEquals("Purse: $1.78 (+5)", convert(Component.literal(raw), new ModConfig()).getString());
    }
    @Test void blankFormatDoesNotSupplyTailDigitsOrTheGainAmount() {
        var entry = new PlayerScoreEntry("fixture", 5, Component.literal(RAW), BlankFormat.INSTANCE);
        Component name = ScoreboardCoinHelper.rawName(new Scoreboard(), entry);
        Component value = entry.formatValue(BlankFormat.INSTANCE);
        var result = ScoreboardCoinHelper.convertRow(name, value, false,
                CoinTextTest.LIVE, CoinTextTest.NOW, new ModConfig());
        assertEquals("", value.getString());
        assertEquals("Purse: $1.78 (+5)", result.name().getString());
        assertSame(value, result.value());
        assertEquals(5, entry.value());
        assertEquals(RAW, name.getString());
    }
    @Test void diagnosticVisibilityFlagsPreserveGoldCoinsYellowGainAndGreenEquivalents() {
        var config = new ModConfig(); config.showCookies = true; config.keepCoins = true;
        Component input = Component.literal(RAW);
        Component result = row(input, config).name();
        assertEquals("Purse: 7,416,701 [$1.78 | 0.603 cookies] (+5)", result.getString());
        result.visit((style, text) -> {
            if (text.contains("$1.78") || text.contains("cookies")) assertEquals(ChatFormatting.GREEN.getColor(), style.getColor().getValue());
            if (text.contains("7,416,701")) assertEquals(ChatFormatting.GOLD.getColor(), style.getColor().getValue());
            if (text.contains("(+5)")) assertEquals(ChatFormatting.YELLOW.getColor(), style.getColor().getValue());
            return Optional.empty();
        }, Style.EMPTY);
        assertEquals(RAW, input.getString());
        assertSame(result, convert(result, config));
    }
    @Test void unsupportedTeamOwnerAndDanglingSiblingMarkersMatchRenderedText() {
        Component split = Component.empty().append(Component.literal("Purse: §67,416,7"))
                .append(Component.literal("§p")).append(Component.literal("§601 §e(+5)"));
        Component dangling = Component.empty().append(Component.literal("Purse: §67,416,7§"))
                .append(Component.literal("01 §e(+5)"));
        for (Component input : new Component[]{split, dangling}) {
            assertEquals(VISIBLE, rendered(input));
            assertEquals("Purse: $1.78 (+5)", convert(input, new ModConfig()).getString());
            assertEquals("Purse: $1.78 (+5)", row(input, new ModConfig()).name().getString());
        }
    }
    @Test void formattingRemovalDoesNotRepairMalformedNumbersOrConvertBits() {
        for (String raw : new String[]{"Purse: §67,41,7§p§601", "Purse: §67,416,7§p", "Bits: §67,416,7§p§601"}) {
            Component input = Component.literal(raw);
            assertSame(input, convert(input, new ModConfig()));
            assertSame(input, row(input, new ModConfig()).name());
        }
    }
    @Test void missingQuoteAndDisabledSidebarKeepOriginalFormatting() {
        Component input = Component.literal(RAW);
        var config = new ModConfig(); config.enablePurse = false;
        assertSame(input, row(input, config).name());
        assertSame(input, CoinText.convert(input, true, true, new CookiePriceFetcher.State(null, null),
                CoinTextTest.NOW, new ModConfig()));
    }
    @Test void allOrdersLayoutsAndVisibilityFlagsUseTheSameCompletePurse() {
        for (DisplayOrder order : DisplayOrder.values()) for (DisplayLayout layout : DisplayLayout.values()) {
            for (int mask = 1; mask < 8; mask++) {
                var config = new ModConfig(); config.displayOrder = order; config.displayLayout = layout;
                config.showUsd = (mask & 1) != 0; config.showCookies = (mask & 2) != 0; config.keepCoins = (mask & 4) != 0;
                Component expected = convert(Component.literal(VISIBLE), config);
                Component actual = row(Component.literal(RAW), config).name();
                assertEquals(rendered(expected), rendered(actual), order + "/" + layout + "/" + mask);
                assertEquals(rendered(actual), rendered(convert(actual, config)));
            }
        }
    }
}

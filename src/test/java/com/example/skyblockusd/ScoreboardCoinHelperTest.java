package com.example.skyblockusd;

import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScoreboardCoinHelperTest {
    @Test void usesAllVisibleTeamPiecesAndNeverAddsTheOrderingScore() {
        Scoreboard board = new Scoreboard();
        var team = board.addPlayerTeam("purse");
        team.setPlayerPrefix(Component.literal("Purse: "));
        team.setPlayerSuffix(Component.literal("56"));
        board.addPlayerToTeam("entry", team);
        PlayerScoreEntry entry = new PlayerScoreEntry("entry", 17, Component.literal("7,014,5"), null);
        String complete = ScoreboardCoinHelper.rawName(board, entry).getString();
        assertEquals("Purse: 7,014,556", complete);
        assertEquals(7_014_556d, CoinParser.purse(complete).orElseThrow());
        assertEquals(17, entry.value());
    }
    @Test void doesNotInventTailDigitsOnTeamlessRows() {
        PlayerScoreEntry entry = new PlayerScoreEntry("Purse: 123,456", 9, null, null);
        assertEquals("Purse: 123,456", ScoreboardCoinHelper.rawName(new Scoreboard(), entry).getString());
    }

    private ScoreboardCoinHelper.Row row(String name, String value, boolean fixed) {
        return ScoreboardCoinHelper.convertRow(Component.literal(name), Component.literal(value), fixed,
                CoinTextTest.LIVE, CoinTextTest.NOW, new ModConfig());
    }
    @Test void reconstructsTheReportedFixedFormatTailNotTheRawScore() {
        var result = row("Purse: 7,416,6", "11", true);
        assertEquals("Purse: $1.78", result.name().getString()); assertEquals("", result.value().getString());
        result = row("Purse: 7,014,5", "56", true);
        assertEquals("Purse: $1.69", result.name().getString()); assertEquals("", result.value().getString());
    }
    @Test void supportsEntireAmountOrEntireRowInTheValueColumn() {
        assertEquals("Purse: $1.78", row("Purse: ", "7,416,611", true).name().getString());
        assertEquals("Purse: $1.78", row("", "Purse: 7,416,611", true).name().getString());
        assertEquals("Purse: $1.78", row("Purse: ", "7416611", false).name().getString());
    }
    @Test void neverMultipliesByOneHundredOrAppendsOrderingScores() {
        var result = row("Purse: 123", "8", false);
        assertEquals("Purse: <$0.01", result.name().getString()); assertEquals("8", result.value().getString());
        result = row("Purse: 7,416,611", "11", false);
        assertEquals("Purse: $1.78", result.name().getString()); assertEquals("11", result.value().getString());
    }
    @Test void unambiguousIncompleteGroupingDoesNotRequireFixedFormat() {
        var result = row("Purse: 7,416,6", "11", false);
        assertEquals("Purse: $1.78", result.name().getString());
        assertEquals("", result.value().getString());
        result = row("Purse: 7,014,5", "56", false);
        assertEquals("Purse: $1.69", result.name().getString());
        assertEquals("", result.value().getString());
        assertEquals("Purse: 7,41,6", row("Purse: 7,41,6", "11", false).name().getString());
        assertEquals("Purse: 7,416,", row("Purse: 7,416,", "11", false).name().getString());
        assertEquals("Purse: $1.78", row("Purse: 7,416,", "611", false).name().getString());
    }
    @Test void leadingZeroTailsAndGainSuffixesArePreserved() {
        assertEquals("Purse: $1.69", row("Purse: 7,014,5", "00 (+56)", true).name().getString().replace(" (+56)", ""));
        assertEquals("Purse: $0.00", row("Purse: ", "0", true).name().getString());
    }
    @Test void unrelatedRowsAndMalformedCurrencyStayUntouched() {
        assertEquals("Bits: ", row("Bits: ", "7,120", true).name().getString());
        assertEquals("7,120", row("Bits: ", "7,120", true).value().getString());
        assertEquals("Purse: 7,41,6", row("Purse: 7,41,6", "11", true).name().getString());
    }
    @Test void unavailableQuoteOrDisabledSidebarNeverStripsDigits() {
        var cfg = new ModConfig(); cfg.enablePurse = false;
        var result = ScoreboardCoinHelper.convertRow(Component.literal("Purse: 7,416,6"), Component.literal("11"), true,
                CoinTextTest.LIVE, CoinTextTest.NOW, cfg);
        assertEquals("Purse: 7,416,6", result.name().getString()); assertEquals("11", result.value().getString());
    }
}

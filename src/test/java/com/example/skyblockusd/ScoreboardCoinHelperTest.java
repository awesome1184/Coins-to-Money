package com.example.skyblockusd;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScoreboardCoinHelperTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final CookiePriceFetcher.State LIVE = new CookiePriceFetcher.State(new BazaarQuote(10_000_000, NOW), null);
    @BeforeEach void defaults() { ModConfig.INSTANCE = new ModConfig(); }
    private ScoreboardCoinHelper.Row row(String left, String right) {
        var format = new FixedFormat(Component.literal(right));
        var entry = new PlayerScoreEntry("entry", 8, Component.literal(left), format);
        return ScoreboardCoinHelper.replace(entry.ownerName(), entry.formatValue(BlankFormat.INSTANCE), entry, BlankFormat.INSTANCE, LIVE, NOW);
    }
    @Test void convertsPurseHeldEntirelyInFormattedScoreColumn() {
        var row = row("Purse: ", "7,416,611");
        assertEquals("Purse: $2.19", row.name().getString());
        assertEquals("", row.score().getString());
    }
    @Test void joinsSplitColumnsIncludingLeadingZerosRatherThanMultiplyingBy100() {
        assertEquals("Purse: $2.07", row("Purse: 7,014,5", "56").name().getString());
        assertEquals("Purse: $2.07", row("Purse: 7,014,", "056").name().getString());
        assertEquals("Purse: $2.07", row("Purse: 7,01", "4,556").name().getString());
        assertEquals("Purse: $2.07", row("Purse: 7,014,55", "6").name().getString());
    }
    @Test void customValueCanContainTheEntireLineOrBillions() {
        assertEquals("Purse: $2.19", row("", "Purse: 7,416,611").score().getString());
        assertEquals("Purse: $2,954.55", row("Purse: ", "10,000,000,000").name().getString());
        assertEquals("Piggy Bank: $2.95", row("Piggy Bank: ", "10m").name().getString());
    }
    @Test void anObjectiveLevelFixedFormatIsAlsoResolved() {
        var fallback = new FixedFormat(Component.literal("7,416,611"));
        var entry = new PlayerScoreEntry("entry", 8, Component.literal("Purse: "), null);
        var result = ScoreboardCoinHelper.replace(entry.ownerName(), entry.formatValue(fallback), entry, fallback, LIVE, NOW);
        assertEquals("Purse: $2.19", result.name().getString());
        assertEquals(8, entry.value());
    }
    @Test void neverAppendsAnOrdinaryOrderingScoreToACompletePurse() {
        var entry = new PlayerScoreEntry("entry", 17, Component.literal("Purse: 7,014,556"), null);
        Component score = entry.formatValue(StyledFormat.SIDEBAR_DEFAULT);
        var result = ScoreboardCoinHelper.replace(entry.ownerName(), score, entry, StyledFormat.SIDEBAR_DEFAULT, LIVE, NOW);
        assertEquals("Purse: $2.07", result.name().getString());
        assertSame(score, result.score());
        assertEquals(17, entry.value());
    }
    @Test void bareCurrencyLabelCanUseAnOrdinaryVisibleScore() {
        var entry = new PlayerScoreEntry("entry", 10_000_000, Component.literal("Purse: "), null);
        var result = ScoreboardCoinHelper.replace(entry.ownerName(), entry.formatValue(StyledFormat.SIDEBAR_DEFAULT), entry, StyledFormat.SIDEBAR_DEFAULT, LIVE, NOW);
        assertEquals("Purse: $2.95", result.name().getString());
    }
    @Test void incompleteValuesAndUnrelatedRowsStayUnchanged() {
        assertEquals("Purse: 7,014,", row("Purse: 7,014,", "5").name().getString());
        assertEquals("5", row("Purse: 7,014,", "5").score().getString());
        assertEquals("Bits: ", row("Bits: ", "7,120").name().getString());
        assertEquals("7,120", row("Bits: ", "7,120").score().getString());
    }
    @Test void joinsRealTeamComponentsAndNeverMutatesThem() {
        Scoreboard board = new Scoreboard();
        var team = board.addPlayerTeam("purse");
        team.setPlayerPrefix(Component.literal("Purse: "));
        team.setPlayerSuffix(Component.literal("56"));
        board.addPlayerToTeam("entry", team);
        var entry = new PlayerScoreEntry("entry", 17, Component.literal("7,014,5"), null);
        Component name = ScoreboardCoinHelper.rawName(board, entry);
        var result = ScoreboardCoinHelper.replace(name, entry.formatValue(BlankFormat.INSTANCE), entry, BlankFormat.INSTANCE, LIVE, NOW);
        assertEquals("Purse: $2.07", result.name().getString());
        assertEquals("Purse: 7,014,556", ScoreboardCoinHelper.rawName(board, entry).getString());
        assertEquals(17, entry.value());
    }
    @Test void missingQuoteAndRepeatedConversionKeepIdentity() {
        var entry = new PlayerScoreEntry("entry", 8, Component.literal("Purse: "), new FixedFormat(Component.literal("7,416,611")));
        Component name = entry.ownerName(), score = entry.formatValue(BlankFormat.INSTANCE);
        var unavailable = ScoreboardCoinHelper.replace(name, score, entry, BlankFormat.INSTANCE, new CookiePriceFetcher.State(null, null), NOW);
        assertSame(name, unavailable.name()); assertSame(score, unavailable.score());
        var once = row("Purse: ", "7,416,611");
        var twice = ScoreboardCoinHelper.replace(once.name(), once.score(), entry, BlankFormat.INSTANCE, LIVE, NOW);
        assertSame(once.name(), twice.name()); assertSame(once.score(), twice.score());
    }
}

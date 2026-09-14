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
}

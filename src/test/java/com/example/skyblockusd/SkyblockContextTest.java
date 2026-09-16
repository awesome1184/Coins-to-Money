package com.example.skyblockusd;

import net.minecraft.world.scores.TeamColor;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkyblockContextTest {
    @Test void selectsTheSameTeamColourSidebarAsVanilla() {
        var board = new Scoreboard();
        var fallback = board.addObjective("default", ObjectiveCriteria.DUMMY, Component.literal("LOBBY"), ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
        var skyblock = board.addObjective("skyblock", ObjectiveCriteria.DUMMY, Component.literal("§eSKYBLOCK"), ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
        board.setDisplayObjective(DisplaySlot.SIDEBAR, fallback);
        board.setDisplayObjective(TeamColor.RED.displaySlot(), skyblock);
        var team = board.addPlayerTeam("red"); team.setColor(Optional.of(TeamColor.RED)); board.addPlayerToTeam("local", team);
        assertSame(skyblock, SkyblockContext.sidebar(board, "local"));
        assertTrue(SkyblockContext.skyblock(SkyblockContext.sidebar(board, "local")));
        assertSame(fallback, SkyblockContext.sidebar(board, "other"));
        board.setDisplayObjective(TeamColor.RED.displaySlot(), null);
        assertSame(fallback, SkyblockContext.sidebar(board, "local"));
        team.setColor(Optional.empty());
        assertSame(fallback, SkyblockContext.sidebar(board, "local"));
        board.setDisplayObjective(DisplaySlot.SIDEBAR, null);
        assertNull(SkyblockContext.sidebar(board, "local"));
        assertFalse(SkyblockContext.skyblock(fallback)); assertFalse(SkyblockContext.skyblock(null));
    }
}

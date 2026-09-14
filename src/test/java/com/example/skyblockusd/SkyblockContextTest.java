package com.example.skyblockusd;

import net.minecraft.ChatFormatting;
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
        board.setDisplayObjective(DisplaySlot.teamColorToSlot(ChatFormatting.RED), skyblock);
        var team = board.addPlayerTeam("red"); team.setColor(ChatFormatting.RED); board.addPlayerToTeam("local", team);
        assertSame(skyblock, SkyblockContext.sidebar(board, "local"));
        assertTrue(SkyblockContext.skyblock(SkyblockContext.sidebar(board, "local")));
        assertSame(fallback, SkyblockContext.sidebar(board, "other"));
        board.setDisplayObjective(DisplaySlot.teamColorToSlot(ChatFormatting.RED), null);
        assertSame(fallback, SkyblockContext.sidebar(board, "local"));
        assertFalse(SkyblockContext.skyblock(fallback)); assertFalse(SkyblockContext.skyblock(null));
    }
}

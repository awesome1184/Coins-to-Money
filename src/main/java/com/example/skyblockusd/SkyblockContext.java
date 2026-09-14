package com.example.skyblockusd;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import java.util.Locale;

public final class SkyblockContext {
    private SkyblockContext() { }
    /** Same objective selection as Gui.extractScoreboardSidebar, including team-colour slots. */
    public static Objective sidebar(Scoreboard board, String playerName) {
        var team = board.getPlayersTeam(playerName);
        if (team != null) {
            DisplaySlot slot = DisplaySlot.teamColorToSlot(team.getColor());
            if (slot != null) {
                Objective objective = board.getDisplayObjective(slot);
                if (objective != null) return objective;
            }
        }
        return board.getDisplayObjective(DisplaySlot.SIDEBAR);
    }
    public static boolean hypixel(Minecraft client) {
        if (client.level == null || client.player == null || client.getCurrentServer() == null) return false;
        String host = client.getCurrentServer().ip.strip().toLowerCase(Locale.ROOT).split(":", 2)[0];
        if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        return host.equals("hypixel.net") || host.endsWith(".hypixel.net");
    }
    public static boolean skyblock(Objective sidebar) {
        return sidebar != null && CoinParser.plain(sidebar.getDisplayName().getString()).toUpperCase(Locale.ROOT).contains("SKYBLOCK");
    }
}

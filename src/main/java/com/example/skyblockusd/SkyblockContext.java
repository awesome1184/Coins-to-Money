package com.example.skyblockusd;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import java.util.Locale;

public final class SkyblockContext {
    private SkyblockContext() { }
    /** Match Minecraft 26.2 Hud's optional team-colour sidebar selection. */
    public static Objective sidebar(Scoreboard board, String playerName) {
        var team = board.getPlayersTeam(playerName);
        if (team != null && team.getColor().isPresent()) {
            Objective objective = board.getDisplayObjective(team.getColor().get().displaySlot());
            if (objective != null) return objective;
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
        return sidebar != null && VisibleText.plain(sidebar.getDisplayName()).toUpperCase(Locale.ROOT).contains("SKYBLOCK");
    }
}

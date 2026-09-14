package com.example.skyblockusd;

import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public final class ScoreboardCoinHelper {
    private ScoreboardCoinHelper() { }
    /** Match vanilla's visible prefix + name + suffix, without invoking our rendering hook. */
    public static Component rawName(Scoreboard board, PlayerScoreEntry entry) {
        PlayerTeam team = board.getPlayersTeam(entry.owner());
        if (team == null) return entry.ownerName();
        return Component.empty().append(team.getPlayerPrefix()).append(entry.ownerName()).append(team.getPlayerSuffix());
    }
    public static Component convertFullName(Component completeName) {
        if (!SkyblockUsdModClient.inSkyblock() || !ModConfig.INSTANCE.enabled || !ModConfig.INSTANCE.enablePurse
                || !CoinParser.isBalance(completeName.getString())) return completeName;
        return CoinText.annotate(completeName, true, false);
    }
}

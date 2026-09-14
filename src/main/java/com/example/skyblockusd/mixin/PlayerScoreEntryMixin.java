package com.example.skyblockusd.mixin;

import com.example.skyblockusd.ScoreboardCoinHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Converts the separate numeric scoreboard value for Hypixel's split coin rows. */
@Mixin(PlayerScoreEntry.class)
public class PlayerScoreEntryMixin {
    @Inject(method = "formatValue", at = @At("RETURN"), cancellable = true)
    private void coinsToUsdValue(NumberFormat format, CallbackInfoReturnable<MutableComponent> cir) {
        PlayerScoreEntry entry = (PlayerScoreEntry) (Object) this;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        PlayerTeam team = minecraft.level.getScoreboard().getPlayersTeam(entry.owner());
        Component teamFormattedName = ScoreboardCoinHelper.buildTeamFormattedName(team, entry.ownerName());
        if (!ScoreboardCoinHelper.isSplitScoreboardCoinOwner(teamFormattedName)) return;

        MutableComponent converted = ScoreboardCoinHelper.formatSplitScoreboardValue(
                teamFormattedName,
                entry.value(),
                cir.getReturnValue()
        );
        if (converted != null) cir.setReturnValue(converted);
    }
}

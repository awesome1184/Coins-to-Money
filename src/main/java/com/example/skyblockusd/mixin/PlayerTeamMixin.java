package com.example.skyblockusd.mixin;

import com.example.skyblockusd.ScoreboardCoinHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Removes Hypixel's partial coin number from the team-formatted scoreboard name. */
@Mixin(PlayerTeam.class)
public class PlayerTeamMixin {
    @Inject(method = "formatNameForTeam", at = @At("RETURN"), cancellable = true)
    private static void coinsToUsdTeamName(Team team, Component name, CallbackInfoReturnable<Component> cir) {
        Component formatted = cir.getReturnValue();
        if (ScoreboardCoinHelper.isSplitScoreboardCoinOwner(formatted)) {
            cir.setReturnValue(ScoreboardCoinHelper.stripSplitScoreboardNumber(formatted));
        }
    }
}

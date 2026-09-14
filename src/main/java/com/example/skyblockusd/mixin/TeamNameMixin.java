package com.example.skyblockusd.mixin;

import com.example.skyblockusd.ScoreboardCoinHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla calls this before measuring sidebar width, after joining ALL team fragments. */
@Mixin(PlayerTeam.class)
public abstract class TeamNameMixin {
    @Inject(method = "formatNameForTeam", at = @At("RETURN"), cancellable = true, require = 1)
    private static void ctm$completeTeamName(Team team, Component name, CallbackInfoReturnable<MutableComponent> cir) {
        MutableComponent original = cir.getReturnValue();
        Component converted = ScoreboardCoinHelper.convertFullName(original);
        if (converted != original) cir.setReturnValue(converted.copy());
    }
}

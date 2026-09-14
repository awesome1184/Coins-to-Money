package com.example.skyblockusd.mixin.compat;

import com.example.skyblockusd.CustomScoreboardCompat;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Convert semantic currency before CustomScoreboard arranges labels or measures widgets. */
@Pseudo
@Mixin(targets = "me.owdding.customscoreboard.core.CustomScoreboardRenderer", remap = false)
public abstract class CustomScoreboardNumbersMixin {
    @ModifyVariable(method = "formatNumberDisplayDisplay(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;I)Lnet/minecraft/network/chat/Component;",
            at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private Component coinsToMoney$number(Component value, Component label, Component original, int color) {
        return CustomScoreboardCompat.number(label, value);
    }
}

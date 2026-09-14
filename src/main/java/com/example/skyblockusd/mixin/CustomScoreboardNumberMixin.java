package com.example.skyblockusd.mixin;

import com.example.skyblockusd.CustomScoreboardCompat;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Convert the typed purse value BEFORE all four label/layout variants and width calculation. */
@Pseudo
@Mixin(targets = "me.owdding.customscoreboard.core.CustomScoreboardRenderer", remap = false)
public abstract class CustomScoreboardNumberMixin {
    @ModifyVariable(method = "formatNumberDisplayDisplay(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;I)Lnet/minecraft/network/chat/Component;",
            at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private Component coinsToMoney$number(Component number, Component label, Component original, int color) {
        return CustomScoreboardCompat.number(label, number);
    }
}

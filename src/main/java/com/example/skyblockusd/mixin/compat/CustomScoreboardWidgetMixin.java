package com.example.skyblockusd.mixin.compat;

import com.example.skyblockusd.CustomScoreboardCompat;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Includes CustomScoreboard's vanilla-lines mode without changing its source scoreboard. */
@Pseudo
@Mixin(targets = "me.owdding.customscoreboard.core.ScoreboardLineKt", remap = false)
public abstract class CustomScoreboardWidgetMixin {
    @ModifyVariable(method = "asTextWidget(Lnet/minecraft/network/chat/Component;)Learth/terrarium/olympus/client/components/string/TextWidget;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static Component coinsToMoney$widget(Component value) {
        return CustomScoreboardCompat.widget(value);
    }
}

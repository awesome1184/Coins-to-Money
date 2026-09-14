package com.example.skyblockusd.mixin;

import com.example.skyblockusd.CustomScoreboardCompat;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** A scoped widget input hook, not a global Font/text replacement. Retains actions and sizing. */
@Pseudo
@Mixin(targets = "me.owdding.customscoreboard.core.ScoreboardLineKt", remap = false)
public abstract class CustomScoreboardLineMixin {
    @ModifyVariable(method = "asTextWidget(Lnet/minecraft/network/chat/Component;)Learth/terrarium/olympus/client/components/string/TextWidget;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static Component coinsToMoney$line(Component line) {
        return CustomScoreboardCompat.vanillaLine(line);
    }
}

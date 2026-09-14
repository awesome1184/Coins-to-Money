package com.example.skyblockusd.mixin;

import com.example.skyblockusd.CustomScoreboardCompat;
import kotlin.jvm.functions.Function0;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Chunked stats omit the label; use the PURSE enum identity, never colour/position guessing. */
@Pseudo
@Mixin(targets = "me.owdding.customscoreboard.core.ChunkedStat", remap = false)
public abstract class CustomScoreboardChunkMixin {
    @Inject(method = "getDisplay()Lkotlin/jvm/functions/Function0;", at = @At("RETURN"), cancellable = true, require = 0)
    private void coinsToMoney$purse(CallbackInfoReturnable<Function0<String>> cir) {
        if (!((Object) this instanceof Enum<?> stat) || !stat.name().equals("PURSE")) return;
        Function0<String> source = cir.getReturnValue();
        cir.setReturnValue(() -> CustomScoreboardCompat.chunkedPurse(source.invoke()));
    }
}

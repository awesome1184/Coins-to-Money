package com.example.skyblockusd.mixin.compat;

import com.example.skyblockusd.CustomScoreboardCompat;
import kotlin.jvm.functions.Function0;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Enum identity supplies currency context even when the chunked display hides labels. */
@Pseudo
@Mixin(targets = "me.owdding.customscoreboard.core.ChunkedStat", remap = false)
public abstract class CustomScoreboardChunkMixin {
    @Inject(method = "getDisplay()Lkotlin/jvm/functions/Function0;", at = @At("RETURN"), cancellable = true, require = 0)
    private void coinsToMoney$chunk(CallbackInfoReturnable<Function0<String>> cir) {
        String kind = ((Enum<?>)(Object)this).name();
        if (!kind.equals("PURSE")) return;
        Function0<String> original = cir.getReturnValue();
        cir.setReturnValue(() -> CustomScoreboardCompat.chunk(kind, original.invoke()));
    }
}

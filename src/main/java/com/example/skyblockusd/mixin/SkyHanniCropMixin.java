package com.example.skyblockusd.mixin;

import com.example.skyblockusd.SkyHanniCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** This lambda formats ONLY a crop sale-price cell, never crop count, XP or farming fortune. */
@Pseudo
@Mixin(targets = "at.hannibal2.skyhanni.features.garden.farming.CropMoneyDisplay", remap = false)
public abstract class SkyHanniCropMixin {
    @Inject(method = "buildCropMoneyLine_Qx8j4vQ$lambda$0$0(DLjava/lang/String;D)Ljava/lang/CharSequence;", at = @At("RETURN"), cancellable = true, require = 0)
    private static void coinsToMoney$crop(double extra, String colour, double crop, CallbackInfoReturnable<CharSequence> cir) {
        cir.setReturnValue(SkyHanniCompat.crop(cir.getReturnValue(), extra, crop));
    }
}

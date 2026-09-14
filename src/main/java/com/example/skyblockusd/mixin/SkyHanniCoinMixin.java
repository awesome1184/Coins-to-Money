package com.example.skyblockusd.mixin;

import com.example.skyblockusd.SkyHanniCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The semantic coin formatter is used by chest profit and tracker item-value breakdowns. */
@Pseudo
@Mixin(targets = "at.hannibal2.skyhanni.utils.ItemPriceUtils", remap = false)
public abstract class SkyHanniCoinMixin {
    @Inject(method = "formatCoin(Ljava/lang/Number;Z)Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 0)
    private void coinsToMoney$coin(Number value, boolean gray, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(SkyHanniCompat.scalar(cir.getReturnValue(), value.doubleValue()));
    }
}

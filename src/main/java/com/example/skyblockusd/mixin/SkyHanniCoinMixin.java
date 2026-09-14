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
    // Kotlin value-class mangling contains a hyphen, which needs a regex target selector.
    // Anchors exclude the different static $default overload.
    @Inject(method = "/^getPriceName-0mM9I0c$/", at = @At("RETURN"), cancellable = true, require = 0)
    private void coinsToMoney$coinName(String internalName, Number amount, double pricePer, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(SkyHanniCompat.coinName(internalName, cir.getReturnValue()));
    }
}

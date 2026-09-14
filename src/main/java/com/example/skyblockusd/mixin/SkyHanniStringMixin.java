package com.example.skyblockusd.mixin;

import com.example.skyblockusd.SkyHanniCompat;
import net.minecraft.client.Minecraft;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Display and measurement use the same projection; the original text field stays untouched. */
@Pseudo
@Mixin(targets = "at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable", remap = false)
public abstract class SkyHanniStringMixin {
    @Shadow @Final private String text;
    @Shadow @Final private double scale;
    @Unique private long coinsToMoney$revision = Long.MIN_VALUE;
    @Unique private String coinsToMoney$view;
    @Unique private String coinsToMoney$view() {
        long revision = SkyHanniCompat.revision();
        if (coinsToMoney$revision != revision) {
            coinsToMoney$view = SkyHanniCompat.text(text);
            coinsToMoney$revision = revision;
        }
        return coinsToMoney$view;
    }
    @Inject(method = "getText", at = @At("HEAD"), cancellable = true, require = 0)
    private void coinsToMoney$text(CallbackInfoReturnable<String> cir) { cir.setReturnValue(coinsToMoney$view()); }
    @Inject(method = "getWidth", at = @At("HEAD"), cancellable = true, require = 0)
    private void coinsToMoney$width(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue((int)(Minecraft.getInstance().font.width(coinsToMoney$view()) * scale) + 1);
    }
    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lat/hannibal2/skyhanni/utils/renderables/primitives/StringRenderable;text:Ljava/lang/String;"), require = 0)
    private String coinsToMoney$render(String original) { return coinsToMoney$view(); }
}

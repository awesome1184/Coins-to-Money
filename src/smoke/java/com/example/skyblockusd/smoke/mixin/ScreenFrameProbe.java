package com.example.skyblockusd.smoke.mixin;

import com.example.skyblockusd.smoke.ClientSmoke;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Test-only probe. A frame counts only AFTER vanilla background, screen and tooltip extraction. */
@Mixin(Screen.class)
public abstract class ScreenFrameProbe {
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("RETURN"))
    private void smoke$frame(GuiGraphicsExtractor graphics, int x, int y, float delta, CallbackInfo ci) {
        ClientSmoke.frame((Screen) (Object) this);
    }
}

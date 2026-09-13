package com.example.skyblockusd.mixin;

import com.example.skyblockusd.SkyblockUsdMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DrawContext.class)
public class DrawContextMixin {

    @ModifyVariable(
        method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I", 
        at = @At("HEAD"), 
        argsOnly = true,
        ordinal = 0
    )
    private Text interceptModdedGuiText(Text text) {
        if (text == null) {
            return null;
        }
        String fullString = text.getString();
        if (fullString == null || !fullString.contains("Coins")) {
            return text;
        }
        String modified = SkyblockUsdMod.replaceCoinsString(fullString);
        if (!modified.equals(fullString)) {
            return Text.literal(modified).setStyle(text.getStyle());
        }
        return text;
    }
}

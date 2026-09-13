package com.example.skyblockusd.mixin;

import com.example.skyblockusd.SkyblockUsdMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
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
        if (fullString != null && fullString.contains("Coins")) {
            SkyblockUsdMod.LOGGER.info("Captured Text draw: '{}'", fullString);
        }
        
        String modified = SkyblockUsdMod.replaceCoinsString(fullString);
        if (modified != null && !modified.equals(fullString)) {
            return Text.literal(modified).setStyle(text.getStyle());
        }
        return text;
    }

    @ModifyVariable(
        method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)I", 
        at = @At("HEAD"), 
        argsOnly = true,
        ordinal = 0
    )
    private OrderedText interceptOrderedText(OrderedText orderedText) {
        if (orderedText == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        orderedText.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        
        String fullString = sb.toString();
        if (fullString.contains("Coins")) {
            SkyblockUsdMod.LOGGER.info("Captured OrderedText draw: '{}'", fullString);
        }

        String modified = SkyblockUsdMod.replaceCoinsString(fullString);
        if (!modified.equals(fullString)) {
            return Text.literal(modified).asOrderedText();
        }
        return orderedText;
    }
}

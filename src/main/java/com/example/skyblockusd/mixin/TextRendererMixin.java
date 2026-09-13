package com.example.skyblockusd.mixin;

import com.example.skyblockusd.SkyblockUsdMod;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextRenderer.class)
public class TextRendererMixin {

    @ModifyVariable(
        method = "draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Text interceptText(Text text) {
        if (text == null) return null;
        String fullString = text.getString();
        
        if (fullString != null && fullString.contains("Coins")) {
            SkyblockUsdMod.LOGGER.info("Captured TextRenderer draw: '{}'", fullString);
        }

        String modified = SkyblockUsdMod.replaceCoinsString(fullString);
        if (modified != null && !modified.equals(fullString)) {
            return Text.literal(modified).setStyle(text.getStyle());
        }
        return text;
    }
}

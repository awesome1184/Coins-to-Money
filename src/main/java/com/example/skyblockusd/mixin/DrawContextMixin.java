package com.example.skyblockusd.mixin;

import com.example.skyblockusd.SkyblockUsdMod;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DrawContext.class)
public class DrawContextMixin {
    @ModifyVariable(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Text coinsToUsd(Text text) {
        return SkyblockUsdMod.replaceCoinsText(text);
    }

    @ModifyVariable(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String coinsToUsd(String text) {
        return SkyblockUsdMod.replaceCoinsString(text);
    }

    @ModifyVariable(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private OrderedText coinsToUsd(OrderedText text) {
        return replaceOrderedText(text);
    }

    @ModifyVariable(method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Text coinsToUsdShadow(Text text) {
        return SkyblockUsdMod.replaceCoinsText(text);
    }

    @ModifyVariable(method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String coinsToUsdShadow(String text) {
        return SkyblockUsdMod.replaceCoinsString(text);
    }

    @ModifyVariable(method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private OrderedText coinsToUsdShadow(OrderedText text) {
        return replaceOrderedText(text);
    }

    @ModifyVariable(method = "drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Text coinsToUsdCentered(Text text) {
        return SkyblockUsdMod.replaceCoinsText(text);
    }

    @ModifyVariable(method = "drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String coinsToUsdCentered(String text) {
        return SkyblockUsdMod.replaceCoinsString(text);
    }

    @ModifyVariable(method = "drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private OrderedText coinsToUsdCentered(OrderedText text) {
        return replaceOrderedText(text);
    }

    private OrderedText replaceOrderedText(OrderedText text) {
        if (text == null) return null;

        StringBuilder raw = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            raw.appendCodePoint(codePoint);
            return true;
        });

        String converted = SkyblockUsdMod.replaceCoinsString(raw.toString());
        return converted.equals(raw.toString()) ? text : Text.literal(converted).asOrderedText();
    }
}

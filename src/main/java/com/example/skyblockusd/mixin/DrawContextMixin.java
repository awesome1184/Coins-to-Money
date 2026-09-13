package com.example.skyblockusd.mixin;

import com.example.skyblockusd.SkyblockUsdMod;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Hooks the 26.1 GUI text extraction API used by vanilla and modern clients. */
@Mixin(GuiGraphicsExtractor.class)
public class DrawContextMixin {
    @ModifyVariable(
            method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component coinsToUsd(Component text) {
        return SkyblockUsdMod.replaceCoinsComponent(text);
    }

    @ModifyVariable(
            method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component coinsToUsdShadow(Component text) {
        return SkyblockUsdMod.replaceCoinsComponent(text);
    }

    @ModifyVariable(
            method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String coinsToUsdString(String text) {
        return SkyblockUsdMod.replaceCoinsString(text);
    }

    @ModifyVariable(
            method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String coinsToUsdStringShadow(String text) {
        return SkyblockUsdMod.replaceCoinsString(text);
    }

    @ModifyVariable(
            method = "centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component coinsToUsdCentered(Component text) {
        return SkyblockUsdMod.replaceCoinsComponent(text);
    }

    @ModifyVariable(
            method = "centeredText(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String coinsToUsdCenteredString(String text) {
        return SkyblockUsdMod.replaceCoinsString(text);
    }
}

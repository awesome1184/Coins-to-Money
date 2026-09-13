package com.example.skyblockusd.mixin;

import com.example.skyblockusd.SkyblockUsdMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
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
        if (text == null || !text.getString().contains("Coins")) {
            return text;
        }
        return rebuildTextTree(text);
    }

    private Text rebuildTextTree(Text original) {
        MutableText rebuilt;
        
        if (original.getContent() instanceof PlainTextContent plain) {
            String raw = plain.string();
            String converted = SkyblockUsdMod.replaceCoinsString(raw);
            rebuilt = Text.literal(converted);
        } else {
            rebuilt = MutableText.of(original.getContent());
        }

        rebuilt.setStyle(original.getStyle());

        for (Text sibling : original.getSiblings()) {
            rebuilt.append(rebuildTextTree(sibling));
        }

        return rebuilt;
    }
}

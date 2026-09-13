package com.example.skyblockusd.mixin;

import com.example.skyblockusd.SkyblockUsdMod;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerScoreEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces the split scoreboard entry rendering used by Hypixel. */
@Mixin(Gui.class)
public class GuiScoreboardMixin {
    @Redirect(
            method = "displayScoreboardSidebar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/scores/PlayerScoreEntry;ownerName()Lnet/minecraft/network/chat/Component;"
            )
    )
    private Component coinsToUsdScoreboardOwner(PlayerScoreEntry entry) {
        return SkyblockUsdMod.replaceScoreboardEntry(entry.ownerName(), entry.value());
    }
}

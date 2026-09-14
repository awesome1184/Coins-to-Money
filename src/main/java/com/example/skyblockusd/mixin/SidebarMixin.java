package com.example.skyblockusd.mixin;

import com.example.skyblockusd.CookiePriceFetcher;
import com.example.skyblockusd.ModConfig;
import com.example.skyblockusd.ScoreboardCoinHelper;
import com.example.skyblockusd.SkyblockUsdModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** MC 26.1.2: convert after name AND formatted value assembly, before sidebar width/layout. */
@Mixin(Gui.class)
public abstract class SidebarMixin {
    @ModifyArgs(method = "lambda$displayScoreboardSidebar$1", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui$1DisplayEntry;<init>(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;I)V"), require = 1)
    private void ctm$replaceVisibleRow(Args args, Scoreboard board, NumberFormat fallback, PlayerScoreEntry entry) {
        if (!SkyblockUsdModClient.inSkyblock() || !ModConfig.INSTANCE.enabled || !ModConfig.INSTANCE.enablePurse) return;
        var row = ScoreboardCoinHelper.replace(args.get(0), args.get(1), entry, fallback,
                CookiePriceFetcher.state(), System.currentTimeMillis());
        args.set(0, row.name());
        args.set(1, row.score());
        args.set(2, Minecraft.getInstance().font.width(row.score()));
    }
}

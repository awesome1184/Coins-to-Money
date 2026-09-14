package com.example.skyblockusd.mixin;

import com.example.skyblockusd.ScoreboardCoinHelper;
import com.example.skyblockusd.SidebarDiagnostics;
import com.example.skyblockusd.SkyblockUsdModClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Convert the complete vanilla row before sidebar width/layout calculations. */
@Mixin(Gui.class)
public abstract class SidebarMixin {
    @Shadow public abstract Font getFont();

    @ModifyArgs(method = "lambda$displayScoreboardSidebar$1", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui$1DisplayEntry;<init>(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;I)V"))
    private void coinsToMoney$completeRow(Args args, Scoreboard scoreboard, NumberFormat defaultFormat, PlayerScoreEntry entry) {
        SidebarDiagnostics.seen();
        if (!SkyblockUsdModClient.inSkyblock()) return;
        var row = ScoreboardCoinHelper.convertRow(args.get(0), args.get(1), entry, defaultFormat);
        args.set(0, row.name());
        args.set(1, row.value());
        args.set(2, getFont().width(row.value()));
    }
}

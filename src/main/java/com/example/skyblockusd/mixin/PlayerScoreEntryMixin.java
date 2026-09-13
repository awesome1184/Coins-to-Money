package com.example.skyblockusd.mixin;

import com.example.skyblockusd.SkyblockUsdMod;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.PlayerScoreEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.NumberFormat;

/** Prevents the raw scoreboard value from being rendered a second time after combining it with the owner text. */
@Mixin(PlayerScoreEntry.class)
public class PlayerScoreEntryMixin {
    @Inject(method = "formatValue", at = @At("HEAD"), cancellable = true)
    private void coinsToUsdSuppressSplitValue(NumberFormat defaultFormat, CallbackInfoReturnable<MutableComponent> cir) {
        PlayerScoreEntry entry = (PlayerScoreEntry) (Object) this;
        Component ownerName = entry.ownerName();
        if (SkyblockUsdMod.isScoreboardCoinEntry(ownerName)) {
            cir.setReturnValue(Component.empty());
        }
    }
}

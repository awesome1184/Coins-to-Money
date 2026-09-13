package com.example.skyblockusd.mixin;

import com.example.skyblockusd.SkyblockUsdMod;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.scores.PlayerScoreEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hypixel uses the scoreboard's owner text for all but the final digits of some coin values,
 * then stores the final digits in PlayerScoreEntry.value(). Vanilla renders those separately.
 * Handle both halves here, before GUI rendering can split them apart.
 */
@Mixin(PlayerScoreEntry.class)
public class PlayerScoreEntryMixin {
    @Inject(method = "ownerName", at = @At("RETURN"), cancellable = true)
    private void coinsToUsdOwnerName(CallbackInfoReturnable<Component> cir) {
        Component original = cir.getReturnValue();
        if (SkyblockUsdMod.isSplitScoreboardCoinOwner(original)) {
            cir.setReturnValue(SkyblockUsdMod.stripSplitScoreboardNumber(original));
        }
    }

    @Inject(method = "formatValue", at = @At("RETURN"), cancellable = true)
    private void coinsToUsdValue(NumberFormat format, CallbackInfoReturnable<MutableComponent> cir) {
        PlayerScoreEntry entry = (PlayerScoreEntry) (Object) this;
        String owner = entry.owner();
        if (!SkyblockUsdMod.isSplitScoreboardCoinOwner(owner)) {
            return;
        }

        MutableComponent vanillaScore = cir.getReturnValue();
        MutableComponent converted = SkyblockUsdMod.formatSplitScoreboardValue(owner, entry.value(), vanillaScore);
        if (converted != null) {
            cir.setReturnValue(converted);
        }
    }
}

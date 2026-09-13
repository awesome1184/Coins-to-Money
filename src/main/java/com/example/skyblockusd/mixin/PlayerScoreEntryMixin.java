package com.example.skyblockusd.mixin;

import com.example.skyblockusd.ScoreboardCoinHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.scores.PlayerScoreEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Handles Hypixel's split scoreboard coin values before GUI rendering.
 * The visible owner text contains most of the number while value() contains the final digits.
 */
@Mixin(PlayerScoreEntry.class)
public class PlayerScoreEntryMixin {
    @Inject(method = "ownerName", at = @At("RETURN"), cancellable = true)
    private void coinsToUsdOwnerName(CallbackInfoReturnable<Component> cir) {
        Component original = cir.getReturnValue();
        if (ScoreboardCoinHelper.isSplitScoreboardCoinOwner(original)) {
            cir.setReturnValue(ScoreboardCoinHelper.stripSplitScoreboardNumber(original));
        }
    }

    @Inject(method = "formatValue", at = @At("RETURN"), cancellable = true)
    private void coinsToUsdValue(NumberFormat format, CallbackInfoReturnable<MutableComponent> cir) {
        PlayerScoreEntry entry = (PlayerScoreEntry) (Object) this;
        String owner = entry.owner();
        if (!ScoreboardCoinHelper.isSplitScoreboardCoinOwner(owner)) {
            return;
        }

        MutableComponent converted = ScoreboardCoinHelper.formatSplitScoreboardValue(
                owner,
                entry.value(),
                cir.getReturnValue()
        );
        if (converted != null) {
            cir.setReturnValue(converted);
        }
    }
}

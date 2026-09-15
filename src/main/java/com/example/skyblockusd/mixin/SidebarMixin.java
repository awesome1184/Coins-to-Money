package com.example.skyblockusd.mixin;

import com.example.skyblockusd.CoinParser;
import com.example.skyblockusd.CoinText;
import com.example.skyblockusd.SidebarDiagnostics;
import com.example.skyblockusd.SkyblockUsdModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.IntFunction;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Convert the complete vanilla row before sidebar width/layout calculations. */
@Mixin(Gui.class)
public abstract class SidebarMixin {
    private static final Pattern INCOMPLETE_GROUP = Pattern.compile(
            "(?i)^.*\\b(?:Purse|Piggy(?: Bank)?|Bank|Balance|Coins):\\h*[+-]?[0-9]{1,3}(?:,[0-9]{3})*,[0-9]{0,2}$");

    private static void coinsToMoney$convert(Args args, NumberFormat defaultFormat, PlayerScoreEntry entry) {
        SidebarDiagnostics.seen();
        if (!SkyblockUsdModClient.inSkyblock()) return;
        var row = com.example.skyblockusd.ScoreboardCoinHelper.convertRow(args.get(0), args.get(1), entry, defaultFormat);
        args.set(0, row.name());
        args.set(1, row.value());
        args.set(2, Minecraft.getInstance().font.width(row.value()));
    }

    // Retain the working 26.1.2 hook when its compiler-generated method exists.
    // The optional injection prevents a startup crash when 26.2 gives the lambda a different name.
    @ModifyArgs(method = "lambda$displayScoreboardSidebar$1", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui$1DisplayEntry;<init>(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;I)V"), require = 0)
    private void coinsToMoney$completeRowLegacy(Args args, Scoreboard scoreboard, NumberFormat defaultFormat, PlayerScoreEntry entry) {
        coinsToMoney$convert(args, defaultFormat, entry);
    }

    /**
     * The lambda name changed between supported Minecraft builds. The stream terminal operation did not.
     * Rebuild the private DisplayEntry records after the mapped components exist, without depending on
     * the compiler's synthetic lambda numbering.
     */
    @Redirect(method = "displayScoreboardSidebar", at = @At(value = "INVOKE",
            target = "Ljava/util/stream/Stream;toArray(Ljava/util/function/IntFunction;)[Ljava/lang/Object;"))
    private Object[] coinsToMoney$rebuildRows(Stream<?> stream, IntFunction<Object[]> generator) {
        Object[] rows = stream.toArray(generator);
        if (!SkyblockUsdModClient.inSkyblock()) return rows;

        long now = System.currentTimeMillis();
        for (int i = 0; i < rows.length; i++) {
            Object row = rows[i];
            try {
                Class<?> type = row.getClass();
                Method nameMethod = type.getDeclaredMethod("name");
                Method scoreMethod = type.getDeclaredMethod("score");
                nameMethod.setAccessible(true);
                scoreMethod.setAccessible(true);
                Component name = (Component) nameMethod.invoke(row);
                Component score = (Component) scoreMethod.invoke(row);
                Component convertedName = name;
                Component convertedScore = score;

                String plainName = name.getString();
                String plainScore = score.getString();
                SidebarDiagnostics.seen();

                if (INCOMPLETE_GROUP.matcher(plainName).matches() && !plainScore.isEmpty()) {
                    Component combined = Component.empty().append(name).append(score);
                    String visible = combined.getString();
                    if (CoinParser.isBalance(visible)) {
                        convertedName = CoinText.convert(combined, true, false);
                        convertedScore = Component.empty();
                    }
                } else {
                    if (CoinParser.isBalance(plainName)) {
                        convertedName = CoinText.convert(name, true, false);
                    }
                    if (CoinParser.isBalance(plainScore)) {
                        convertedScore = CoinText.convert(score, true, false);
                    }
                }

                if (convertedName != name || convertedScore != score) {
                    Constructor<?> ctor = type.getDeclaredConstructor(Component.class, Component.class, int.class);
                    ctor.setAccessible(true);
                    rows[i] = ctor.newInstance(convertedName, convertedScore,
                            convertedScore.getString().isEmpty() ? 0 : Minecraft.getInstance().font.width(convertedScore));
                }
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Unable to rebuild vanilla scoreboard row", ex);
            }
        }
        return rows;
    }
}

package com.example.skyblockusd;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SkyblockUsdModClient implements ClientModInitializer {
    private static boolean skyblock;
    private static double purse = Double.NaN;
    public static boolean inSkyblock() { return skyblock; }

    @Override public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("coins-to-money", "general"));
        KeyMapping configKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.coins-to-money.config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
        KeyMapping toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.coins-to-money.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, category));
        CookiePriceFetcher.start();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            updateContext(client);
            while (configKey.consumeClick()) client.setScreen(new CoinsToMoneyConfigScreen(client.screen));
            while (toggleKey.consumeClick()) { ModConfig.INSTANCE.enabled = !ModConfig.INSTANCE.enabled; ModConfig.save(); }
            CookiePriceFetcher.setActive(skyblock && ModConfig.INSTANCE.enabled);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> CookiePriceFetcher.stop());
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            PlayerTeam.formatNameForTeam(null, Component.literal("Coins to Money"));
            SkyblockUsdMod.LOGGER.info("CTM_CLIENT_READY: client callbacks and team-name mixin loaded");
        });
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!skyblock || !ModConfig.INSTANCE.enabled || !ModConfig.INSTANCE.enableTooltips) return;
            for (int i = 0; i < lines.size(); i++) lines.set(i, CoinText.annotate(lines.get(i), true, true));
        });
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            if (!skyblock || !ModConfig.INSTANCE.enabled || !ModConfig.INSTANCE.enableChat) return message;
            return CoinText.annotate(message, true, false);
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("coins-to-money", "conversion"), (graphics, delta) -> {
                    Minecraft client = Minecraft.getInstance();
                    if (!skyblock || !ModConfig.INSTANCE.enabled || !ModConfig.INSTANCE.showGui || client.options.hideGui) return;
                    List<String> lines = hudLines(System.currentTimeMillis());
                    int width = lines.stream().mapToInt(client.font::width).max().orElse(0);
                    int x = Math.max(2, Math.min(ModConfig.INSTANCE.guiX, client.getWindow().getGuiScaledWidth() - width - 6));
                    int y = Math.max(2, Math.min(ModConfig.INSTANCE.guiY, client.getWindow().getGuiScaledHeight() - lines.size() * 11 - 6));
                    graphics.fill(x - 2, y - 2, x + width + 3, y + lines.size() * 11 + 2, 0x99000000);
                    for (int i = 0; i < lines.size(); i++) graphics.text(client.font, Component.literal(lines.get(i)), x, y + i * 11, 0xFFFFFFFF, true);
                });
    }
    private static void updateContext(Minecraft client) {
        skyblock = false;
        purse = Double.NaN;
        if (client.level == null || client.player == null || client.getCurrentServer() == null) return;
        String address = client.getCurrentServer().ip.toLowerCase(Locale.ROOT).split(":", 2)[0];
        if (!(address.equals("hypixel.net") || address.endsWith(".hypixel.net"))) return;
        Objective sidebar = client.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null || !CoinParser.plain(sidebar.getDisplayName().getString()).toUpperCase(Locale.ROOT).contains("SKYBLOCK")) return;
        skyblock = true;
        for (PlayerScoreEntry entry : sidebar.getScoreboard().listPlayerScores(sidebar)) {
            if (entry.isHidden()) continue;
            var amount = CoinParser.purse(ScoreboardCoinHelper.rawName(sidebar.getScoreboard(), entry).getString());
            if (amount.isPresent()) { purse = amount.getAsDouble(); break; }
        }
    }
    private static List<String> hudLines(long now) {
        CookiePriceFetcher.State state = CookiePriceFetcher.state();
        if (!state.available(now)) return List.of("Cookie/USD rate: " + (state.error() == null ? "loading..." : "unavailable"));
        List<String> lines = new ArrayList<>();
        if (Double.isFinite(purse)) lines.add("Purse: " + CoinConversion.of(purse, state.quote().instantBuyPrice()).display());
        lines.add(String.format(Locale.US, "1 cookie: %,.1f coins (instant buy)", state.quote().instantBuyPrice()));
        lines.add(String.format(Locale.US, "1 cookie: $%.4f USD ($100 / 11,000 gems)", CoinConversion.USD_PER_COOKIE));
        if (state.stale(now)) lines.add("STALE quote - last update " + Math.max(0, (now - state.quote().updatedAt()) / 60_000) + " min ago");
        return lines;
    }
}

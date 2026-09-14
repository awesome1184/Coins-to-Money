package com.example.skyblockusd;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class SkyblockUsdModClient implements ClientModInitializer {
    private static boolean skyblock;
    public static boolean inSkyblock() { return skyblock; }

    @Override public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("coins-to-money", "general"));
        KeyMapping configKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.coins-to-money.config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
        KeyMapping toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.coins-to-money.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, category));
        CookiePriceFetcher.start();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            updateContext(client);
            while (configKey.consumeClick()) {
                if (!(client.screen instanceof CoinsToMoneyConfigScreen) && !(client.screen instanceof CurrencyConfigScreen)) client.setScreen(new CoinsToMoneyConfigScreen(client.screen));
            }
            while (toggleKey.consumeClick()) { ModConfig.INSTANCE.enabled = !ModConfig.INSTANCE.enabled; ModConfig.save(); }
            CookiePriceFetcher.setActive(skyblock && ModConfig.INSTANCE.enabled);
            SkyHanniCompat.tick();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> CookiePriceFetcher.stop());
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!skyblock || !ModConfig.INSTANCE.enabled || !ModConfig.INSTANCE.enableTooltips) return;
            for (int i = 0; i < lines.size(); i++) lines.set(i, CoinText.convert(lines.get(i), true, true));
        });
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            if (!skyblock || !ModConfig.INSTANCE.enabled || !ModConfig.INSTANCE.enableChat) return message;
            return CoinText.convert(message, true, false);
        });
    }
    private static void updateContext(Minecraft client) {
        skyblock = NativeModApis.customSkyblock() || NativeModApis.hanniSkyblock()
                || (SkyblockContext.hypixel(client) && SkyblockContext.skyblock(
                SkyblockContext.sidebar(client.level.getScoreboard(), client.player.getScoreboardName())));
    }
}

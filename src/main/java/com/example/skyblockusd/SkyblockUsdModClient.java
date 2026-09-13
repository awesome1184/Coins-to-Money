package com.example.skyblockusd;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public class SkyblockUsdModClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("coins-to-money", "general")
    );

    private static final KeyMapping TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.coins-to-money.toggle",
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_O,
                    CATEGORY
            )
    );

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_KEY.consumeClick()) {
                SkyblockUsdMod.enabled = !SkyblockUsdMod.enabled;
                if (client.player != null) {
                    String state = SkyblockUsdMod.enabled ? "enabled" : "disabled";
                    client.player.sendOverlayMessage(Component.literal("Coins to Money: " + state));
                }
            }
        });
    }
}

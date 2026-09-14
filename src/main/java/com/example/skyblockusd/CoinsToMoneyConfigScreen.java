package com.example.skyblockusd;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.BooleanSupplier;

public final class CoinsToMoneyConfigScreen extends Screen {
    private final Screen parent;
    public CoinsToMoneyConfigScreen(Screen parent) { super(Component.literal("Coins to Money")); this.parent = parent; }
    @Override protected void init() {
        int x = width / 2 - 110, y = Math.max(35, height / 2 - 90);
        toggle("Enabled", () -> ModConfig.INSTANCE.enabled, () -> ModConfig.INSTANCE.enabled = !ModConfig.INSTANCE.enabled, x, y);
        toggle("Sidebar conversion", () -> ModConfig.INSTANCE.enablePurse, () -> ModConfig.INSTANCE.enablePurse = !ModConfig.INSTANCE.enablePurse, x, y + 24);
        toggle("Item / Bazaar / AH tooltips", () -> ModConfig.INSTANCE.enableTooltips, () -> ModConfig.INSTANCE.enableTooltips = !ModConfig.INSTANCE.enableTooltips, x, y + 48);
        toggle("Server chat / action bar", () -> ModConfig.INSTANCE.enableChat, () -> ModConfig.INSTANCE.enableChat = !ModConfig.INSTANCE.enableChat, x, y + 72);
        toggle("Show cookie count", () -> ModConfig.INSTANCE.showCookies, () -> ModConfig.INSTANCE.showCookies = !ModConfig.INSTANCE.showCookies, x, y + 96);
        addRenderableWidget(Button.builder(Component.literal("USD decimals: " + ModConfig.INSTANCE.decimalPlaces), button -> {
            ModConfig.INSTANCE.decimalPlaces = ModConfig.INSTANCE.decimalPlaces >= 8 ? 2 : ModConfig.INSTANCE.decimalPlaces + 1;
            ModConfig.save();
            button.setMessage(Component.literal("USD decimals: " + ModConfig.INSTANCE.decimalPlaces));
        }).bounds(x, y + 120, 220, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Refresh price"), button -> CookiePriceFetcher.requestRefresh()).bounds(x, y + 148, 107, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose()).bounds(x + 113, y + 148, 107, 20).build());
    }
    private void toggle(String label, BooleanSupplier value, Runnable action, int x, int y) {
        addRenderableWidget(Button.builder(label(label, value.getAsBoolean()), button -> {
            action.run(); ModConfig.save(); button.setMessage(label(label, value.getAsBoolean()));
        }).bounds(x, y, 220, 20).build());
    }
    private static Component label(String text, boolean enabled) { return Component.literal(text + ": " + (enabled ? "ON" : "OFF")); }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Screen.extractRenderStateWithTooltipAndSubtitles already extracts the background once.
        graphics.centeredText(font, title, width / 2, 15, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
    @Override public void onClose() { ModConfig.save(); if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}

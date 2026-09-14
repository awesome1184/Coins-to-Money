package com.example.skyblockusd;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;
import java.util.function.BooleanSupplier;

public class CoinsToMoneyConfigScreen extends Screen {
    private final Screen parent;
    private int top;
    public CoinsToMoneyConfigScreen(Screen parent) { super(Component.literal("Coins to Money")); this.parent = parent; }

    @Override protected void init() {
        int w = Math.min(200, (width - 24) / 2);
        int left = width / 2 - w - 3, right = width / 2 + 3;
        top = Math.max(32, (height - 240) / 2 + 36);
        toggle("Enabled", () -> ModConfig.INSTANCE.enabled, () -> ModConfig.INSTANCE.enabled = !ModConfig.INSTANCE.enabled, left, top, w);
        toggle("Sidebar", () -> ModConfig.INSTANCE.enablePurse, () -> ModConfig.INSTANCE.enablePurse = !ModConfig.INSTANCE.enablePurse, right, top, w);
        toggle("Bazaar / AH / tooltips", () -> ModConfig.INSTANCE.enableTooltips, () -> ModConfig.INSTANCE.enableTooltips = !ModConfig.INSTANCE.enableTooltips, left, top + 24, w);
        toggle("Chat / action bar", () -> ModConfig.INSTANCE.enableChat, () -> ModConfig.INSTANCE.enableChat = !ModConfig.INSTANCE.enableChat, right, top + 24, w);
        toggle("Show dollars", () -> ModConfig.INSTANCE.showUsd, () -> ModConfig.INSTANCE.showUsd = !ModConfig.INSTANCE.showUsd, left, top + 48, w);
        toggle("Show cookies", () -> ModConfig.INSTANCE.showCookies, () -> ModConfig.INSTANCE.showCookies = !ModConfig.INSTANCE.showCookies, right, top + 48, w);
        toggle("Keep coin amounts", () -> ModConfig.INSTANCE.keepCoins, () -> ModConfig.INSTANCE.keepCoins = !ModConfig.INSTANCE.keepCoins, left, top + 72, w);
        addRenderableWidget(Button.builder(Component.literal("USD decimals: " + ModConfig.INSTANCE.decimalPlaces), button -> {
            ModConfig.INSTANCE.decimalPlaces = ModConfig.INSTANCE.decimalPlaces >= 8 ? 2 : ModConfig.INSTANCE.decimalPlaces + 1;
            ModConfig.save(); rebuildWidgets();
        }).bounds(right, top + 72, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cookie decimals: " + ModConfig.INSTANCE.cookieDecimalPlaces), button -> {
            ModConfig.INSTANCE.cookieDecimalPlaces = ModConfig.INSTANCE.cookieDecimalPlaces >= 6 ? 1 : ModConfig.INSTANCE.cookieDecimalPlaces + 1;
            ModConfig.save(); rebuildWidgets();
        }).bounds(left, top + 96, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Refresh cookie price"), button -> CookiePriceFetcher.requestRefresh())
                .bounds(right, top + 96, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose()).bounds(width / 2 - 100, top + 176, 200, 20).build());
    }
    private void toggle(String label, BooleanSupplier value, Runnable action, int x, int y, int w) {
        addRenderableWidget(Button.builder(Component.literal(label + ": " + (value.getAsBoolean() ? "ON" : "OFF")), button -> {
            action.run(); ModConfig.save(); rebuildWidgets();
        }).bounds(x, y, w, 20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Screen.extractRenderStateWithTooltipAndSubtitles already renders the background.
        // Calling extractBackground here blurs TWICE and crashes Minecraft 26.1.2.
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, top - 22, 0xFFFFFFFF);
        var state = CookiePriceFetcher.state();
        long now = System.currentTimeMillis();
        String quote = state.available(now)
                ? String.format(Locale.US, "Cookie instant buy: %,.1f coins%s", state.quote().instantBuyPrice(), state.stale(now) ? " (stale)" : "")
                : "Cookie price unavailable - originals shown until a quote arrives";
        graphics.centeredText(font, Component.literal(quote), width / 2, top + 128, 0xFFCCCCCC);
        graphics.centeredText(font, Component.literal("USD basis: $100 / 11,000 gems; 325 gems / cookie"), width / 2, top + 140, 0xFFCCCCCC);
        graphics.centeredText(font, Component.literal("At least one value stays visible. O toggles the mod; K opens settings."), width / 2, top + 152, 0xFFAAAAAA);
    }
    @Override public void onClose() { ModConfig.save(); if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}

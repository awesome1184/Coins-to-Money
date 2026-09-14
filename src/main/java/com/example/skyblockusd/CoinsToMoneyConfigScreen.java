package com.example.skyblockusd;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.time.Duration;
import java.util.function.BooleanSupplier;

public class CoinsToMoneyConfigScreen extends Screen {
    private final Screen parent;
    private int top;
    public CoinsToMoneyConfigScreen(Screen parent) { super(Component.literal("Coins to Money")); this.parent = parent; }
    @Override protected void init() {
        int w = Math.min(188, (width - 24) / 2), left = width / 2 - w - 3, right = width / 2 + 3;
        top = Math.max(30, (height - 240) / 2 + 30);
        var c = ModConfig.INSTANCE;
        toggle("Enabled", "Enable conversions everywhere. O is the default hotkey. Disabling restores original coin text; no server data is changed.", () -> c.enabled, () -> c.enabled = !c.enabled, left, top, w);
        toggle("Sidebar", "Convert purse, Piggy Bank and bank balances in the vanilla sidebar and supported CustomScoreboard purse layouts. Both displayed columns are considered; ordinary ordering scores are not treated as coin digits.", () -> c.enablePurse, () -> c.enablePurse = !c.enablePurse, right, top, w);
        toggle("Item tooltips", "Convert recognized coin prices in Bazaar, Auction House and NPC item tooltips. Item counts, stats, Bits and Gems are left alone.", () -> c.enableTooltips, () -> c.enableTooltips = !c.enableTooltips, left, top + 22, w);
        toggle("Chat / action bar", "Convert coin amounts in new server game messages and the action bar, not player chat. Existing chat messages are not rewritten when settings change.", () -> c.enableChat, () -> c.enableChat = !c.enableChat, right, top + 22, w);
        toggle("Show money", "Show the real-money equivalent in green. Uses 325 gems per cookie and the requested $100 / 11,000-gem basis, then your currency multiplier. This is not a cash-out value.", () -> c.showUsd, () -> c.showUsd = !c.showUsd, left, top + 44, w);
        toggle("Show cookies", "Show fractional Booster Cookie equivalents in green: coins divided by the current one-cookie Bazaar instant-buy price. This is not rounded to whole purchasable cookies.", () -> c.showCookies, () -> c.showCookies = !c.showCookies, right, top + 44, w);
        toggle("Keep coins", "Keep the original coin number and its original colour. Order and layout determine where it appears alongside money/cookies. At least one value must remain enabled.", () -> c.keepCoins, () -> c.keepCoins = !c.keepCoins, left, top + 66, w);
        button("Currency: " + c.currencyCode, "Choose a real-world currency and enter target-currency units per 1 USD. This is a saved manual rate, not a live FX quote. Cookie prices still update automatically.", () -> minecraft.setScreen(new CurrencyConfigScreen(this)), right, top + 66, w);
        button("Order: " + c.displayOrder.label, "Cycle all six orders of coins, money and cookies. Disabled values are skipped. For money then coins, enable Keep coins and select Money > Coins > Cookies.", () -> { c.displayOrder = c.displayOrder.next(); changed(); }, left, top + 88, w * 2 + 6);
        button("Layout: " + c.displayLayout.label, "Cycle brackets, parentheses, inline bars and equals signs. Examples: 100 coins [$1.00], $1.00 (100 coins), or 100 coins = $1.00. No separators appear when only one value is shown.", () -> { c.displayLayout = c.displayLayout.next(); changed(); }, left, top + 110, w);
        button("Money decimals: " + c.decimalPlaces, "Cycle 2 to 8 decimal places for money. Conversion is rounded only for display. Amounts below the smallest displayed unit use < instead of incorrectly showing zero.", () -> { c.decimalPlaces = c.decimalPlaces >= 8 ? 2 : c.decimalPlaces + 1; changed(); }, right, top + 110, w);
        button("Cookie decimals: " + c.cookieDecimalPlaces, "Cycle 1 to 6 decimal places for fractional cookie equivalents. This does not change the conversion rate or money precision.", () -> { c.cookieDecimalPlaces = c.cookieDecimalPlaces >= 6 ? 1 : c.cookieDecimalPlaces + 1; changed(); }, left, top + 132, w);
        button("Copy purse diagnostics", "Copy the current sidebar's currency rows, formatting types, hook status and settings to your clipboard for debugging. Contains your displayed balances; no passwords, chat or uploads. Share only when needed.", () -> minecraft.keyboardHandler.setClipboard(SidebarDiagnostics.report(minecraft)), right, top + 132, w);
        button("Refresh cookie price", "Request an asynchronous refresh while enabled on Hypixel SkyBlock. Requests are limited to one per 30 seconds. Failed quotes are marked stale and expire after 30 minutes; no price is invented.", CookiePriceFetcher::requestRefresh, left, top + 154, w);
        toggle("SkyHanni profits", "Convert SkyHanni coin-valued profit displays, money/hour and chest/item price formatting. Item counts, XP, mana, Soulflow and calculations remain unchanged. Requires SkyHanni; no dependency is bundled.", () -> c.enableSkyHanni, () -> c.enableSkyHanni = !c.enableSkyHanni, right, top + 154, w);
        button("Done", "Return to the previous screen. These settings are saved as you change them. The standalone rate HUD remains removed.", this::onClose, left, top + 176, w * 2 + 6);
    }
    static <T extends AbstractWidget> T help(T widget, String description) {
        widget.setTooltip(Tooltip.create(Component.literal(description)));
        widget.setTooltipDelay(Duration.ofMillis(350));
        return widget;
    }
    private void button(String text, String tip, Runnable action, int x, int y, int w) {
        addRenderableWidget(help(Button.builder(Component.literal(text), b -> action.run()).bounds(x, y, w, 20).build(), tip));
    }
    private void toggle(String text, String tip, BooleanSupplier value, Runnable action, int x, int y, int w) {
        button(text + ": " + (value.getAsBoolean() ? "ON" : "OFF"), tip, () -> { action.run(); changed(); }, x, y, w);
    }
    private void changed() { ModConfig.save(); rebuildWidgets(); }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Vanilla's wrapper already extracts the background. Never blur twice.
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, top - 28, 0xFFFFFFFF);
        long now = System.currentTimeMillis();
        var previewRate = new CookiePriceFetcher.State(new BazaarQuote(1_000, now), null);
        Component preview = CoinText.convert(Component.literal("1,000 coins").withStyle(ChatFormatting.GOLD), false, false, previewRate, now, ModConfig.INSTANCE);
        Component sample = Component.literal("Preview: ").withStyle(ChatFormatting.GRAY).append(preview);
        var lines = font.split(sample, width - 20);
        if (!lines.isEmpty()) graphics.text(font, lines.getFirst(), Math.max(10, (width - font.width(lines.getFirst())) / 2), top - 14, 0xFFFFFFFF, true);
    }
    @Override public void onClose() { ModConfig.save(); if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}

package com.example.skyblockusd;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;

/** Apply both code and rate together, never commit a half-edited or invalid conversion. */
public class CurrencyConfigScreen extends Screen {
    private final Screen parent;
    private EditBox code, rate;
    private Button apply;
    private String draftCode, draftRate, error = "";
    private int top;
    public CurrencyConfigScreen(Screen parent) {
        super(Component.literal("Money currency"));
        this.parent = parent;
        draftCode = ModConfig.INSTANCE.currencyCode;
        draftRate = java.math.BigDecimal.valueOf(ModConfig.INSTANCE.currencyPerUsd).stripTrailingZeros().toPlainString();
    }
    @Override protected void init() {
        int x = width / 2 - 110;
        top = Math.max(42, height / 2 - 72);
        code = addRenderableWidget(CoinsToMoneyConfigScreen.help(new EditBox(font, x, top + 16, 220, 20, Component.literal("Currency code")),
                "Three-letter real-world currency code, such as USD, EUR, GBP, JPY, CAD or RSD. The displayed symbol follows this code; it does not fetch or guess an exchange rate."));
        code.setMaxLength(3); code.setValue(draftCode);
        rate = addRenderableWidget(CoinsToMoneyConfigScreen.help(new EditBox(font, x, top + 58, 220, 20, Component.literal("Target currency per 1 USD")),
                "Target-currency units for ONE US dollar. Example only: a rate of 0.92 with EUR means $10 becomes EUR 9.20. Use your chosen current rate with a decimal dot. This is manual and never auto-updated. USD must stay 1."));
        rate.setMaxLength(30); rate.setValue(draftRate);
        apply = addRenderableWidget(CoinsToMoneyConfigScreen.help(Button.builder(Component.literal("Apply"), b -> apply()).bounds(x, top + 128, 107, 20).build(),
                "Save the currency and multiplier together. Invalid or non-positive rates cannot be saved. All newly rendered money values use this rate; cookies stay unchanged."));
        addRenderableWidget(CoinsToMoneyConfigScreen.help(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(x + 113, top + 128, 107, 20).build(),
                "Discard edits and keep the previously saved currency and rate."));
        addRenderableWidget(CoinsToMoneyConfigScreen.help(Button.builder(Component.literal("Reset to USD"), b -> { code.setValue("USD"); rate.setValue("1"); }).bounds(x, top + 101, 220, 20).build(),
                "Fill USD and a rate of 1. Nothing is saved until Apply is pressed."));
        code.setResponder(value -> { draftCode = value; validate(); });
        rate.setResponder(value -> { draftRate = value; validate(); });
        validate();
        setInitialFocus(code);
    }
    private void validate() {
        try {
            String currency = MoneyCurrency.code(code.getValue());
            MoneyCurrency.validate(currency, MoneyCurrency.rate(rate.getValue()));
            error = ""; apply.active = true;
        } catch (IllegalArgumentException ex) { error = ex.getMessage(); apply.active = false; }
    }
    private void apply() {
        validate();
        if (!apply.active) return;
        ModConfig.INSTANCE.currencyCode = MoneyCurrency.code(code.getValue());
        ModConfig.INSTANCE.currencyPerUsd = MoneyCurrency.rate(rate.getValue());
        ModConfig.save();
        onClose();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, top - 30, 0xFFFFFFFF);
        graphics.centeredText(font, Component.literal("Manual FX rate - no currency API"), width / 2, top - 16, 0xFFAAAAAA);
        graphics.text(font, Component.literal("Currency code"), width / 2 - 110, top + 3, 0xFFFFFFFF, false);
        graphics.text(font, Component.literal("1 USD = how many target units?"), width / 2 - 110, top + 44, 0xFFFFFFFF, false);
        String message = error.isEmpty() ? "1 USD = " + draftRate + " " + draftCode.toUpperCase(Locale.ROOT) : error;
        var lines = font.split(Component.literal(message), width - 24);
        for (int i = 0; i < Math.min(2, lines.size()); i++) graphics.text(font, lines.get(i), 12, top + 83 + i * 9, error.isEmpty() ? 0xFF55FF55 : 0xFFFF5555, false);
    }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}

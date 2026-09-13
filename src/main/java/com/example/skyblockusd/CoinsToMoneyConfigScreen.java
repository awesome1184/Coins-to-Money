package com.example.skyblockusd;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Simple Mod Menu configuration screen for display precision. */
public class CoinsToMoneyConfigScreen extends Screen {
    private final Screen parent;
    private Button precisionButton;

    public CoinsToMoneyConfigScreen(Screen parent) {
        super(Component.literal("Coins to Money Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        precisionButton = Button.builder(
                precisionText(),
                button -> {
                    ModConfig.decimalPlaces = ModConfig.nextDecimalPlaces();
                    ModConfig.save();
                    button.setMessage(precisionText());
                }
        ).bounds(this.width / 2 - 100, this.height / 2 - 10, 200, 20).build();
        this.addRenderableWidget(precisionButton);

        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                button -> this.onClose()
        ).bounds(this.width / 2 - 100, this.height / 2 + 20, 200, 20).build());
    }

    private Component precisionText() {
        return Component.literal("Decimal places: " + ModConfig.decimalPlaces);
    }

    @Override
    public void onClose() {
        ModConfig.save();
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(
                this.font,
                Component.literal("More decimal places = more precise USD values"),
                this.width / 2 - 100,
                this.height / 2 - 45,
                0xFFFFFFFF,
                true
        );
    }
}

package com.example.skyblockusd;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import java.lang.reflect.*;
import java.util.*;

/** Exercises the published CustomScoreboard JAR, never mock classes with its names. */
public final class CustomScoreboardSmokeTest {
    private static final String CORE = "me.owdding.customscoreboard.core.";
    private static final List<AbstractWidget> widgets = new ArrayList<>();
    private CustomScoreboardSmokeTest() { }
    public static void run(Minecraft client) throws Exception {
        if (!FabricLoader.getInstance().isModLoaded("customscoreboard")) {
            if (CustomScoreboardCompat.numberCalls() != 0) throw new AssertionError("Optional hooks loaded without mod");
            SkyblockUsdMod.LOGGER.info("CTM_COMPAT_ABSENT_PASS: no CustomScoreboard or Kotlin runtime requirement");
            return;
        }
        eq("1.12.14-2", FabricLoader.getInstance().getModContainer("customscoreboard").orElseThrow().getMetadata().getVersion().getFriendlyString());
        Class<?> profile = Class.forName("tech.thatgravyboat.skyblockapi.api.profile.profile.ProfileAPI");
        Field profileName = profile.getDeclaredField("profileName"); profileName.setAccessible(true);
        Object previousProfile = profileName.get(null); profileName.set(null, "ctm-synthetic-test");
        Object currency = instance("tech.thatgravyboat.skyblockapi.api.data.stored.CurrencyStorage");
        currency.getClass().getMethod("setPurse", double.class).invoke(currency, 7_416_701d);
        eq(7_416_701d, currency.getClass().getMethod("getPurse").invoke(currency));
        Object purse = instance("me.owdding.customscoreboard.elements.PurseElement");
        Method getDisplay = Arrays.stream(purse.getClass().getDeclaredMethods())
                .filter(m -> m.getName().equals("getDisplay") && m.getReturnType() == Component.class).findFirst().orElseThrow();
        getDisplay.setAccessible(true);
        purse.getClass().getMethod("setPreviousAmount", long.class).invoke(purse, 7_416_701L);
        purse.getClass().getMethod("setTemporaryChangeDisplay", Component.class).invoke(purse,
                Component.literal(" (+5)").withStyle(ChatFormatting.YELLOW));
        Class<?> settings = Class.forName("me.owdding.customscoreboard.config.category.LinesConfig");
        Field delegateField = settings.getDeclaredField("numberDisplayFormat$delegate"); delegateField.setAccessible(true);
        Object delegate = delegateField.get(null);
        Method set = delegate.getClass().getMethod("set", Object.class);
        Object previousFormat = delegate.getClass().getMethod("get").invoke(delegate);
        Class<?> formats = Class.forName(CORE + "CustomScoreboardRenderer$NumberDisplayFormat");
        Method widgetFactory = Class.forName(CORE + "ScoreboardLineKt").getDeclaredMethod("asTextWidget", Component.class);
        widgetFactory.setAccessible(true);
        ModConfig originalConfig = ModConfig.INSTANCE;
        try {
            for (Object format : formats.getEnumConstants()) {
                set.invoke(delegate, format);
                ModConfig.INSTANCE = new ModConfig();
                Component plainMoney = (Component) getDisplay.invoke(purse);
                String text = VisibleText.plain(plainMoney);
                expect(text.contains("$1.78") && text.contains("Purse") && text.contains("(+5)"), text);
                expect(!text.contains("7,416") && !text.contains("7.42"), text);
                eq(0x55FF55, colorAt(plainMoney, text.indexOf('$')));
                ModConfig.INSTANCE.keepCoins = true; ModConfig.INSTANCE.showCookies = true;
                ModConfig.INSTANCE.displayOrder = DisplayOrder.MONEY_COINS_COOKIES;
                Component combined = (Component) getDisplay.invoke(purse);
                text = VisibleText.plain(combined);
                expect(text.contains("$1.78") && text.contains("0.603 cookies") && text.contains("(+5)"), text);
                eq(0x55FF55, colorAt(combined, text.indexOf('$')));
                eq(0x55FF55, colorAt(combined, text.indexOf("0.603")));
                AbstractWidget widget = (AbstractWidget) widgetFactory.invoke(null, combined);
                eq(client.font.width(combined), widget.getWidth()); widgets.add(widget);
                eq(7_416_701d, currency.getClass().getMethod("getPurse").invoke(currency));
            }
            ModConfig.INSTANCE = new ModConfig();
            Class<?> chunks = Class.forName(CORE + "ChunkedStat");
            Method getSupplier = chunks.getMethod("getDisplay");
            Method invoke = Class.forName("kotlin.jvm.functions.Function0").getMethod("invoke");
            for (Object kind : chunks.getEnumConstants()) {
                ModConfig.INSTANCE.enabled = false;
                Object untouchedSupplier = getSupplier.invoke(kind);
                ModConfig.INSTANCE.enabled = true;
                Object supplier = getSupplier.invoke(kind);
                if (((Enum<?>)kind).name().equals("PURSE")) {
                    eq("$1.78", CoinParser.plain((String) invoke.invoke(supplier)));
                    ModConfig.INSTANCE.enabled = false;
                    String vanilla = (String) invoke.invoke(supplier);
                    expect(!vanilla.contains("$"), vanilla); ModConfig.INSTANCE.enabled = true;
                } else eq(untouchedSupplier, supplier);
            }
            Object renderer = instance(CORE + "CustomScoreboardRenderer");
            Method format = renderer.getClass().getMethod("formatNumberDisplayDisplay", Component.class, Component.class, int.class);
            for (String label : List.of("Bits", "Motes", "Gems", "Mana", "Soulflow", "Heat", "Copper")) {
                Component result = (Component)format.invoke(renderer, Component.literal(label), Component.literal("50"), 0x55FFFF);
                expect(!result.getString().contains("$"), result.getString());
            }
            Component originalRow = Component.literal("Purse: §67,416,7§p§601 §e(+5)");
            AbstractWidget row = (AbstractWidget) widgetFactory.invoke(null, originalRow);
            Field rowText = row.getClass().getDeclaredField("text"); rowText.setAccessible(true);
            eq("Purse: $1.78 (+5)", ((Component)rowText.get(row)).getString());
            eq(client.font.width((Component)rowText.get(row)), row.getWidth()); widgets.add(row);
            ModConfig.INSTANCE.enablePurse = false;
            eq(originalRow, rowText.get(widgetFactory.invoke(null, originalRow)));
            ModConfig.INSTANCE.enablePurse = true;
            expect(CustomScoreboardCompat.numberCalls() > 0 && CustomScoreboardCompat.widgetCalls() > 0 && CustomScoreboardCompat.chunkCalls() > 0,
                    "All three actual optional hooks must execute");
            SkyblockUsdMod.LOGGER.info("CTM_CUSTOMSCOREBOARD_PASS: published 1.12.14-2, four number formats, green values, exact purse, chunks, raw data, toggles, vanilla-lines and measured widgets");
        } finally {
            set.invoke(delegate, previousFormat); profileName.set(null, previousProfile); ModConfig.INSTANCE = originalConfig;
        }
    }
    public static void render(GuiGraphicsExtractor graphics) {
        int y = 25;
        for (AbstractWidget widget : widgets) {
            widget.setPosition(5, y); widget.extractRenderState(graphics, 0, 0, 0f); y += 12;
        }
    }
    private static Object instance(String name) throws Exception { return Class.forName(name).getField("INSTANCE").get(null); }
    private static int colorAt(Component value, int offset) {
        int[] position = {0}, color = {-1};
        VisibleText.visit(value, (index, style, cp) -> {
            if (position[0] == offset && style.getColor() != null) color[0] = style.getColor().getValue();
            position[0] += Character.charCount(cp); return true;
        });
        return color[0];
    }
    private static void expect(boolean ok, String reason) { if (!ok) throw new AssertionError(reason); }
    private static void eq(Object expected, Object actual) { if (!Objects.equals(expected, actual)) throw new AssertionError("Expected " + expected + ", got " + actual); }
}

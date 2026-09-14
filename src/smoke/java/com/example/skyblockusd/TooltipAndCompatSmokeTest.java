package com.example.skyblockusd;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Runs with and without the unmodified, published CustomScoreboard 1.12.11 binary. */
final class TooltipAndCompatSmokeTest {
    private static final String CORE = "me.owdding.customscoreboard.core.";
    private static final List<AbstractWidget> widgets = new ArrayList<>();
    private static void check(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
    static void run(Minecraft client) throws Exception {
        for (String text : List.of("Mana Cost: 50✎", "Soulflow Cost: 1", "Health Cost: 100", "Cost: 100❤", "Cost: 50 Mana")) {
            Component c = Component.literal(text).withStyle(ChatFormatting.AQUA);
            if (CoinText.convert(c, true, true) != c) throw new AssertionError("Resource cost changed: " + text);
        }
        check("Buy price: $2.95", CoinText.convert(Component.literal("Buy price: 12,295,597.2 coins"), true, true).getString());
        SkyblockUsdMod.LOGGER.info("CTM_RESOURCE_COSTS_PASS: mana/soulflow/health preserved, real coin prices converted");
        if (!FabricLoader.getInstance().isModLoaded("customscoreboard")) {
            check(false, FabricLoader.getInstance().isModLoaded("fabric-language-kotlin"));
            SkyblockUsdMod.LOGGER.info("CTM_OPTIONAL_COMPAT_ABSENT_PASS: client runs without CustomScoreboard or Kotlin");
            return;
        }
        check("1.12.11", FabricLoader.getInstance().getModContainer("customscoreboard").orElseThrow().getMetadata().getVersion().getFriendlyString());
        Class<?> renderer = Class.forName(CORE + "CustomScoreboardRenderer");
        Object instance = renderer.getField("INSTANCE").get(null);
        Method format = renderer.getMethod("formatNumberDisplayDisplay", Component.class, Component.class, int.class);
        Class<?> formats = Class.forName(CORE + "CustomScoreboardRenderer$NumberDisplayFormat");
        Component number = Component.literal("12,295,597.2").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" (+5)").withStyle(ChatFormatting.YELLOW));
        for (Object order : formats.getEnumConstants()) {
            setting("numberDisplayFormat", order);
            ModConfig.INSTANCE.enabled = false;
            Component vanilla = (Component) format.invoke(instance, Component.literal("Purse"), number, ChatFormatting.GOLD.getColor());
            ModConfig.INSTANCE.enabled = true;
            Component adapted = (Component) format.invoke(instance, Component.literal("Purse"), number, ChatFormatting.GOLD.getColor());
            check(vanilla.getString().replace("12,295,597.2", "$2.95"), adapted.getString());
            assertGreen(adapted);
            widgets.add(widget(client, adapted));
            ModConfig.INSTANCE.showCookies = true; ModConfig.INSTANCE.keepCoins = true;
            Component both = (Component) format.invoke(instance, Component.literal("Piggy"), number, ChatFormatting.GOLD.getColor());
            if (!both.getString().contains("12,295,597.2 [$2.95 | 1.000 cookies] (+5)")) throw new AssertionError(both.getString());
            assertGreen(both); widgets.add(widget(client, both));
            ModConfig.INSTANCE.showCookies = false; ModConfig.INSTANCE.keepCoins = false;
            for (String label : List.of("Bits", "Motes", "Gems", "Soulflow", "Mana")) {
                Component nonCoin = (Component) format.invoke(instance, Component.literal(label), number, ChatFormatting.AQUA.getColor());
                if (nonCoin.getString().contains("$")) throw new AssertionError(label + " converted");
            }
        }
        Component fallback = Component.literal("Purse: §67,416,7§p§601 §e(+5)");
        AbstractWidget fallbackWidget = widget(client, fallback);
        check("Purse: $1.78 (+5)", text(fallbackWidget).getString()); widgets.add(fallbackWidget);
        // Exercise actual PurseElement -> CurrencyAPI -> renderer and the label-free chunk supplier.
        Class<?> api = Class.forName("tech.thatgravyboat.skyblockapi.api.profile.currency.CurrencyAPI");
        Object currency = api.getField("INSTANCE").get(null);
        Method set = api.getDeclaredMethod("setPurse", double.class); set.setAccessible(true); set.invoke(currency, 12_295_597.2);
        Class<?> purseElement = Class.forName("me.owdding.customscoreboard.elements.PurseElement");
        Method display = purseElement.getDeclaredMethod("getDisplay"); display.setAccessible(true);
        Component actualPurse = (Component) display.invoke(purseElement.getField("INSTANCE").get(null));
        if (!actualPurse.getString().contains("$")) throw new AssertionError("Actual PurseElement not converted");
        widgets.add(widget(client, actualPurse));
        check(12_295_597.2, api.getMethod("getPurse").invoke(currency));
        Class<?> chunk = Class.forName(CORE + "ChunkedStat");
        Class<?> function = Class.forName("kotlin.jvm.functions.Function0");
        for (Object stat : chunk.getEnumConstants()) {
            ModConfig.INSTANCE.enabled = false;
            Object supplier = chunk.getMethod("getDisplay").invoke(stat);
            String raw = (String) function.getMethod("invoke").invoke(supplier);
            ModConfig.INSTANCE.enabled = true;
            String converted = (String) function.getMethod("invoke").invoke(supplier);
            if (((Enum<?>)stat).name().equals("PURSE")) {
                if (!converted.contains("$")) throw new AssertionError("Chunked purse unchanged");
                assertGreen(Component.literal(converted)); widgets.add(widget(client, Component.literal(converted)));
            } else check(raw, converted);
        }
        // No stale replacement survives disabling or unavailable quotes.
        ModConfig.INSTANCE.enablePurse = false;
        check(fallback.getString(), text(widget(client, fallback)).getString());
        ModConfig.INSTANCE.enablePurse = true;
        SkyblockUsdMod.LOGGER.info("CTM_CUSTOMSCOREBOARD_PASS: published 1.12.11, four formats, Piggy, actual PurseElement, chunks, Hypixel lines, colours, widths and raw balance intact");
    }
    private static void setting(String name, Object value) throws Exception {
        Class<?> cls = Class.forName("me.owdding.customscoreboard.config.category.LinesConfig");
        Field field = cls.getDeclaredField(name + "$delegate"); field.setAccessible(true);
        Object delegate = field.get(null); delegate.getClass().getMethod("set", Object.class).invoke(delegate, value);
    }
    private static AbstractWidget widget(Minecraft client, Component input) throws Exception {
        Class<?> cls = Class.forName(CORE + "ScoreboardLineKt");
        Method method = cls.getDeclaredMethod("asTextWidget", Component.class); method.setAccessible(true);
        AbstractWidget widget = (AbstractWidget) method.invoke(null, input);
        check(client.font.width(text(widget)), widget.getWidth());
        return widget;
    }
    private static Component text(AbstractWidget widget) throws Exception {
        Field field = widget.getClass().getDeclaredField("text"); field.setAccessible(true); return (Component) field.get(widget);
    }
    private static void assertGreen(Component component) {
        StringBuilder plain = new StringBuilder(); List<Integer> colours = new ArrayList<>();
        VisibleText.visit(component, (index, style, cp) -> {
            plain.appendCodePoint(cp); colours.add(style.getColor() == null ? -1 : style.getColor().getValue()); return true;
        });
        int money = plain.indexOf("$");
        if (money < 0) throw new AssertionError("Missing money");
        check(ChatFormatting.GREEN.getColor(), colours.get(money));
        int cookies = plain.indexOf("cookies"); if (cookies >= 0) check(ChatFormatting.GREEN.getColor(), colours.get(cookies));
    }
    static void render(GuiGraphicsExtractor graphics, int frame) {
        if (widgets.isEmpty()) return;
        AbstractWidget widget = widgets.get(frame % widgets.size());
        widget.setPosition(4, 4); widget.extractRenderState(graphics, 0, 0, 0f);
    }
}

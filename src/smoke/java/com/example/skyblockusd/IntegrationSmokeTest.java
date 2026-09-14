package com.example.skyblockusd;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import java.awt.Color;
import java.lang.reflect.*;
import java.util.*;

/** Calls actual published mod methods and renders their actual widgets, never mock targets. */
final class IntegrationSmokeTest {
    private static final String CUSTOM = "me.owdding.customscoreboard.";
    private static final String SH = "at.hannibal2.skyhanni.";
    private static final List<AbstractWidget> customWidgets = new ArrayList<>();
    private static final List<Object> hanniWidgets = new ArrayList<>();
    private static final double COINS = 7_416_701;
    private static Object drawContext;
    static void run(Minecraft mc) throws Exception {
        for (String s : List.of("Mana Cost: 50✎", "Soulflow Cost: 1", "Health Cost: 100", "Cost: 50 Mana")) {
            Component c = Component.literal(s);
            if (CoinText.convert(c, true, true) != c) throw new AssertionError("Resource cost changed: " + s);
        }
        SkyblockUsdMod.LOGGER.info("CTM_RESOURCE_COSTS_PASS: resource costs retain original components");
        boolean custom = FabricLoader.getInstance().isModLoaded("customscoreboard");
        boolean hanni = FabricLoader.getInstance().isModLoaded("skyhanni");
        if (custom) custom(mc);
        if (hanni) hanni(mc);
        if (!custom && !hanni) {
            expect(!FabricLoader.getInstance().isModLoaded("fabric-language-kotlin"), "Kotlin should be absent");
            SkyblockUsdMod.LOGGER.info("CTM_OPTIONAL_COMPAT_ABSENT_PASS: no optional mods or Kotlin needed");
        }
        ModConfig.INSTANCE = new ModConfig(); SkyHanniCompat.tick();
    }
    private static void custom(Minecraft mc) throws Exception {
        eq("1.12.14-2", FabricLoader.getInstance().getModContainer("customscoreboard").orElseThrow().getMetadata().getVersion().getFriendlyString());
        Class<?> profiles = Class.forName("tech.thatgravyboat.skyblockapi.api.profile.profile.ProfileAPI");
        Field profile = field(profiles, "profileName"); Object priorProfile = profile.get(null); profile.set(null, "ctm123-fixture");
        Object currency = instance("tech.thatgravyboat.skyblockapi.api.data.stored.CurrencyStorage");
        method(currency.getClass(), "setPurse", double.class).invoke(currency, COINS);
        Class<?> location = Class.forName("tech.thatgravyboat.skyblockapi.api.location.LocationAPI");
        Field context = field(location, "isOnSkyBlock"); Object oldContext = context.get(null); context.set(null, true);
        Field ctmContext = field(SkyblockUsdModClient.class, "skyblock"); ctmContext.set(null, false);
        expect(NativeModApis.customSkyblock(), "Native SkyBlock context must work without a vanilla objective");
        method(SkyblockUsdModClient.class, "updateContext", Minecraft.class).invoke(null, mc);
        expect(SkyblockUsdModClient.inSkyblock(), "Client must keep quotes active from native context");
        Object purse = instance(CUSTOM + "elements.PurseElement");
        method(purse.getClass(), "setPreviousAmount", long.class).invoke(purse, (long)COINS);
        method(purse.getClass(), "setTemporaryChangeDisplay", Component.class).invoke(purse, Component.literal(" (+5)").withStyle(ChatFormatting.YELLOW));
        Method getDisplay = Arrays.stream(purse.getClass().getDeclaredMethods()).filter(m -> m.getName().equals("getDisplay") && m.getReturnType() == Component.class).findFirst().orElseThrow(); getDisplay.setAccessible(true);
        Object renderer = instance(CUSTOM + "core.CustomScoreboardRenderer");
        Field elements = field(renderer.getClass(), "currentIslandElements"); Object priorElements = elements.get(null); elements.set(null, List.of(purse));
        Locale oldLocale = Locale.getDefault();
        try {
            setting(CUSTOM + "config.category.LinesConfig", "forcedLocale", false);
            Object[] orders = Class.forName(CUSTOM + "core.CustomScoreboardRenderer$NumberDisplayFormat").getEnumConstants();
            Object[] formats = Class.forName(CUSTOM + "utils.NumberFormatType").getEnumConstants();
            for (Locale locale : List.of(Locale.US, Locale.GERMANY, Locale.FRANCE)) for (Object format : formats) for (Object order : orders) {
                Locale.setDefault(locale);
                setting(CUSTOM + "config.category.LinesConfig", "numberFormat", format);
                setting(CUSTOM + "config.category.LinesConfig", "numberDisplayFormat", order);
                ModConfig.INSTANCE = new ModConfig(); ModConfig.INSTANCE.enabled = false;
                String nativeText = VisibleText.plain((Component)getDisplay.invoke(purse));
                ModConfig.INSTANCE.enabled = true; ModConfig.INSTANCE.decimalPlaces = 8;
                Component converted = (Component)getDisplay.invoke(purse);
                String expected = CoinConversion.of(COINS, CookiePriceFetcher.state().quote().instantBuyPrice()).moneyText(ModConfig.INSTANCE);
                expect(converted.getString().contains(expected), "Localized/compact purse unchanged: " + locale + " " + format + " " + converted.getString());
                expect(converted.getString().contains("(+5)"), "Gain lost"); green(converted);
                ModConfig.INSTANCE.keepCoins = true; ModConfig.INSTANCE.showCookies = true;
                Component combined = (Component)getDisplay.invoke(purse);
                expect(combined.getString().contains(expected) && combined.getString().contains("0.603 cookies"), "Typed precision lost");
                // Construct and arrange the complete native scoreboard, not just a formatter.
                method(renderer.getClass(), "updateDisplay").invoke(renderer);
                List<?> lines = (List<?>)method(renderer.getClass(), "getLines").invoke(renderer);
                expect(!lines.isEmpty(), "Native scoreboard produced no lines");
                Object line = lines.getFirst();
                Component lineText = (Component)method(line.getClass(), "getComponent").invoke(line);
                expect(lineText.getString().contains(expected), "Conversion missing from actual native line");
                AbstractWidget widget = (AbstractWidget)method(line.getClass(), "getWidget").invoke(line);
                expect(widget.getWidth() >= mc.font.width(lineText), "Native button too narrow");
                if (locale.equals(Locale.US) && ((Enum<?>)format).name().equals("LONG")) customWidgets.add(widget);
                ModConfig.INSTANCE.enablePurse = false;
                eq(nativeText, VisibleText.plain((Component)getDisplay.invoke(purse)));
                eq(COINS, method(currency.getClass(), "getPurse").invoke(currency));
            }
            Locale.setDefault(Locale.GERMANY); ModConfig.INSTANCE = new ModConfig();
            Class<?> chunks = Class.forName(CUSTOM + "core.ChunkedStat");
            Method invoke = Class.forName("kotlin.jvm.functions.Function0").getMethod("invoke");
            for (Object kind : chunks.getEnumConstants()) {
                Object supplier = method(chunks, "getDisplay").invoke(kind);
                if (((Enum<?>)kind).name().equals("PURSE")) eq("$1.78", CoinParser.plain((String)invoke.invoke(supplier)));
                else {
                    String enabled = (String)invoke.invoke(supplier); ModConfig.INSTANCE.enabled = false;
                    eq(enabled, (String)invoke.invoke(supplier)); ModConfig.INSTANCE.enabled = true;
                }
            }
            Method fmt = method(renderer.getClass(), "formatNumberDisplayDisplay", Component.class, Component.class, int.class);
            for (String label : List.of("Mana", "Soulflow", "Bits", "Motes", "Gems", "Copper")) {
                Component c = (Component)fmt.invoke(renderer, Component.literal(label), Component.literal("50"), 0x55FFFF);
                expect(!c.getString().contains("$"), label + " was converted");
            }
            String stringPurse = (String)method(renderer.getClass(), "formatNumberDisplayDisplay", String.class, String.class, String.class).invoke(renderer, "Purse", "7.416.701", "§6");
            expect(CoinParser.plain(stringPurse).contains("$1.78"), "Legacy string overload missing");
            var state = field(CookiePriceFetcher.class, "state"); Object saved = state.get(null);
            state.set(null, new CookiePriceFetcher.State(null, "offline"));
            Component unavailable = (Component)getDisplay.invoke(purse);
            expect(!unavailable.getString().contains("$"), "Unavailable quote fabricated a purse");
            state.set(null, saved);
            expect(CustomScoreboardCompat.numberChanges() > 0 && CustomScoreboardCompat.chunkCalls() > 0 && CustomScoreboardCompat.lineCalls() > 0, "Missing native adapter");
            SkyblockUsdMod.LOGGER.info("CTM_CUSTOMSCOREBOARD_PASS: published 1.12.14-2, native scoreboard build, 3 locales x 2 number formats x 4 layouts, exact coins, gains, chunks, string overload, gates and widths");
        } finally {
            Locale.setDefault(oldLocale); profile.set(null, priorProfile); context.set(null, oldContext); elements.set(null, priorElements); ctmContext.set(null, true); ModConfig.INSTANCE = new ModConfig();
        }
    }
    private static void hanni(Minecraft mc) throws Exception {
        eq("7.56.0", FabricLoader.getInstance().getModContainer("skyhanni").orElseThrow().getMetadata().getVersion().getFriendlyString());
        SkyHanniCompat.tick();
        Object prices = instance(SH + "utils.ItemPriceUtils"); Method coin = method(prices.getClass(), "formatCoin", Number.class, boolean.class);
        eq("$2.95", CoinParser.plain((String)coin.invoke(prices, 12_295_597.2, false)));
        eq("-$2.95", CoinParser.plain((String)coin.invoke(prices, -12_295_597.2, false)));
        ModConfig.INSTANCE.showCookies = true;
        eq("$2.95 [1.000 cookies]", CoinParser.plain((String)coin.invoke(prices, 12_295_597.2, false)));
        ModConfig.INSTANCE = new ModConfig();
        Class<?> crop = Class.forName(SH + "features.garden.farming.CropMoneyDisplay");
        Method lambda = method(crop, "buildCropMoneyLine_Qx8j4vQ$lambda$0$0", double.class, String.class, double.class);
        CharSequence cropText = (CharSequence)lambda.invoke(null, 295_597.2, "§6", 12_000_000d);
        eq("$2.95", CoinParser.plain(cropText.toString()));
        Object line = hanniText("§eSession Profit: §67,416,701 coins");
        checkHanni(mc, line, "Session Profit: $1.78"); hanniWidgets.add(line);
        Object hourly = hanniText("§eProfit per hour: §612.5m");
        checkHanni(mc, hourly, "Profit per hour: $3.00"); hanniWidgets.add(hourly);
        for (String s : List.of("Mana Cost: 50✎", "Soulflow Cost: 1", "Crops per hour: 120k", "Skill XP: 2M", "Uptime: 01:02:03")) {
            Object stat = hanniText(s); checkHanni(mc, stat, s); hanniWidgets.add(stat);
        }
        ModConfig.INSTANCE.enabled = false; SkyHanniCompat.tick();
        checkHanni(mc, line, "Session Profit: 7,416,701 coins");
        ModConfig.INSTANCE.enabled = true; ModConfig.INSTANCE.currencyCode = "EUR"; ModConfig.INSTANCE.currencyPerUsd = .92; SkyHanniCompat.tick();
        checkHanni(mc, line, "Session Profit: €1.64");
        ModConfig.INSTANCE.enableSkyHanni = false; SkyHanniCompat.tick();
        checkHanni(mc, line, "Session Profit: 7,416,701 coins");
        ModConfig.INSTANCE = new ModConfig(); SkyHanniCompat.tick();
        Object chest = instance(SH + "features.combat.InstanceChestProfit");
        var moneyMap = new LinkedHashMap<String, Double>(); moneyMap.put("Fixture Sword", 12_295_597.2); moneyMap.put("Chest Cost", -1_000_000d);
        Object rows = method(chest.getClass(), "buildDisplay", String.class, Map.class).invoke(chest, "Gold Chest", moneyMap);
        List<Object> texts = renderedTexts(rows);
        expect(!texts.isEmpty(), "No native chest renderables");
        expect(texts.stream().anyMatch(t -> textOf(t).contains("$2.95")), "Chest price not converted: " + texts.stream().map(IntegrationSmokeTest::textOf).toList());
        eq(12_295_597.2, moneyMap.get("Fixture Sword")); eq(-1_000_000d, moneyMap.get("Chest Cost"));
        hanniWidgets.addAll(texts);
        Object fishing = instance(SH + "features.fishing.tracker.FishingProfitTracker");
        Object tracker = field(fishing.getClass(), "tracker").get(null);
        Class<?> units = Class.forName("kotlin.time.DurationUnit");
        Object hour = Arrays.stream(units.getEnumConstants()).filter(v -> ((Enum<?>)v).name().equals("HOURS")).findFirst().orElseThrow();
        long duration = (Long)method(Class.forName("kotlin.time.DurationKt"), "toDuration", long.class, units).invoke(null, 1L, hour);
        Object total = method(tracker.getClass(), "addTotalProfit-gwCluXo", double.class, long.class, String.class, long.class, String.class, boolean.class).invoke(tracker, 12_295_597.2, 20L, "catch", duration, "Catches", true);
        List<Object> totalTexts = renderedTexts(total);
        expect(totalTexts.stream().anyMatch(t -> textOf(t).contains("$2.95")), "Actual tracker total missing: " + totalTexts.stream().map(IntegrationSmokeTest::textOf).toList());
        hanniWidgets.addAll(totalTexts);
        Object component = hanniComponent(Component.literal("Profit per hour: 12,295,597.2 coins"));
        checkHanni(mc, component, "Profit per hour: $2.95"); hanniWidgets.add(component);
        expect(SkyHanniCompat.scalarCalls() > 0 && SkyHanniCompat.cropCalls() > 0 && SkyHanniCompat.textChanges() > 0, "Missing SkyHanni hook");
        drawContext = instance(SH + "utils.compat.DrawContextUtils");
        SkyblockUsdMod.LOGGER.info("CTM_SKYHANNI_PASS: published 7.56.0, actual chest builder, shared tracker total, crop-price lambda, signed profits, dynamic widths, currency/toggle changes, unchanged stats and source balances");
    }
    private static Object hanniText(String s) throws Exception { return hanni(s, String.class, "String"); }
    private static Object hanniComponent(Component s) throws Exception { return hanni(s, Component.class, "Text"); }
    private static Object hanni(Object s, Class<?> type, String kind) throws Exception {
        Class<?> h = Class.forName(SH + "utils.RenderUtils$HorizontalAlignment"), v = Class.forName(SH + "utils.RenderUtils$VerticalAlignment");
        return Class.forName(SH + "utils.renderables.primitives." + kind + "Renderable").getConstructor(type, double.class, Color.class, h, v).newInstance(s, 1d, Color.WHITE, h.getEnumConstants()[0], v.getEnumConstants()[1]);
    }
    private static void checkHanni(Minecraft mc, Object line, String expected) throws Exception {
        eq(expected, textOf(line));
        Object text = method(line.getClass(), "getText").invoke(line);
        int width = text instanceof Component c ? mc.font.width(c) : mc.font.width((String)text);
        eq(width + 1, method(line.getClass(), "getWidth").invoke(line));
        if (expected.contains("$") || expected.contains("€")) green(text instanceof Component c ? c : Component.literal((String)text));
    }
    private static String textOf(Object line) {
        try { Object t = method(line.getClass(), "getText").invoke(line); return t instanceof Component c ? VisibleText.plain(c) : CoinParser.plain((String)t); }
        catch (Exception ex) { throw new AssertionError(ex); }
    }
    private static List<Object> renderedTexts(Object root) throws Exception {
        List<Object> result = new ArrayList<>(); collect(root, result, Collections.newSetFromMap(new IdentityHashMap<>()), 0); return result;
    }
    private static void collect(Object o, List<Object> result, Set<Object> seen, int depth) throws Exception {
        if (o == null || depth > 12 || !seen.add(o)) return;
        if (o instanceof Iterable<?> list) { for (Object item : list) collect(item, result, seen, depth + 1); return; }
        String name = o.getClass().getName();
        if (name.endsWith(".StringRenderable") || name.endsWith(".TextRenderable")) { result.add(o); return; }
        if (!name.startsWith(SH + "utils.renderables.")) return;
        for (Class<?> c = o.getClass(); c != Object.class; c = c.getSuperclass()) for (Field f : c.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue; f.setAccessible(true); collect(f.get(o), result, seen, depth + 1);
        }
    }
    static void render(GuiGraphicsExtractor graphics, int frame) {
        try {
            if (!customWidgets.isEmpty()) { var w = customWidgets.get(frame % customWidgets.size()); w.setPosition(4, 4); w.extractRenderState(graphics, 0, 0, 0f); }
            if (!hanniWidgets.isEmpty()) {
                method(drawContext.getClass(), "setContext", GuiGraphicsExtractor.class).invoke(drawContext, graphics);
                try { method(hanniWidgets.get(frame % hanniWidgets.size()).getClass(), "render", int.class, int.class).invoke(hanniWidgets.get(frame % hanniWidgets.size()), 0, 0); }
                finally { method(drawContext.getClass(), "clearContext").invoke(drawContext); }
            }
        } catch (Exception ex) { throw new AssertionError("Native widget render failed", ex); }
    }
    private static void green(Component c) {
        StringBuilder text = new StringBuilder(); List<Integer> colours = new ArrayList<>();
        VisibleText.visit(c, (i,s,cp) -> { text.appendCodePoint(cp); colours.add(s.getColor() == null ? -1 : s.getColor().getValue()); return true; });
        int at = text.indexOf("$"); if (at < 0) at = text.indexOf("€");
        if (at >= 0) eq(0x55FF55, colours.get(at));
    }
    private static void setting(String cls, String key, Object value) throws Exception {
        Object delegate = field(Class.forName(cls), key + "$delegate").get(null);
        method(delegate.getClass(), "set", Object.class).invoke(delegate, value);
    }
    private static Object instance(String cls) throws Exception { return Class.forName(cls).getField("INSTANCE").get(null); }
    private static Field field(Class<?> c, String n) throws Exception { Field f = c.getDeclaredField(n); f.setAccessible(true); return f; }
    private static Method method(Class<?> c, String n, Class<?>... types) throws Exception {
        for (Class<?> cls = c; cls != null; cls = cls.getSuperclass()) try { Method m = cls.getDeclaredMethod(n, types); m.setAccessible(true); return m; } catch (NoSuchMethodException ignored) { }
        throw new NoSuchMethodException(c.getName() + "." + n);
    }
    private static void expect(boolean ok, String reason) { if (!ok) throw new AssertionError(reason); }
    private static void eq(Object expected, Object actual) { if (!Objects.equals(expected, actual)) throw new AssertionError("Expected " + expected + ", got " + actual); }
}

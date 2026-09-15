package com.example.skyblockusd;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import java.time.Duration;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Loads only in runSmokeClient. Real transformed Gui + real Screen rendering, no Hypixel login. */
public final class ClientRenderSmokeTest implements ClientModInitializer {
    private static final String LIVE_PURSE = "Purse: §67,416,7§p§601 §e(+5)";
    private static Method rowFactory;
    private static Method sidebarRenderer;
    private static boolean started;
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (started || client.getOverlay() != null || client.screen == null) return;
            started = true;
            try {
                rowFactory = Gui.class.getDeclaredMethod("lambda$displayScoreboardSidebar$1", Scoreboard.class, NumberFormat.class, PlayerScoreEntry.class);
                rowFactory.setAccessible(true);
                sidebarRenderer = Gui.class.getDeclaredMethod("displayScoreboardSidebar", GuiGraphicsExtractor.class, Objective.class);
                sidebarRenderer.setAccessible(true);
                seed();
                rows(client);
                IntegrationSmokeTest.run(client);
                var screen = new RenderScreen();
                client.setScreen(screen);
                click(screen, "Show cookies:"); check(true, ModConfig.INSTANCE.showCookies);
                click(screen, "Show money:"); check(false, ModConfig.INSTANCE.showUsd);
                click(screen, "Keep coins:"); check(true, ModConfig.INSTANCE.keepCoins);
                click(screen, "Money decimals:"); check(3, ModConfig.INSTANCE.decimalPlaces);
                click(screen, "Order:"); check(DisplayOrder.MONEY_COINS_COOKIES, ModConfig.INSTANCE.displayOrder);
                click(screen, "Layout:"); check(DisplayLayout.PARENTHESES, ModConfig.INSTANCE.displayLayout);
                assertTooltips(screen);
                screen.resize(320, 240); assertBounds(screen);
                click(screen, "Currency:");
                Screen currency = client.screen;
                assertTooltips(currency); currency.resize(320, 240); assertBounds(currency);
                edit(currency, "Currency code", "EUR"); edit(currency, "Target currency per 1 USD", "0");
                check(false, button(currency, "Apply").active);
                edit(currency, "Target currency per 1 USD", "0.92"); check(true, button(currency, "Apply").active);
                click(currency, "Apply");
                check("EUR", ModConfig.INSTANCE.currencyCode); check(.92d, ModConfig.INSTANCE.currencyPerUsd);
                ModConfig.load(); check("EUR", ModConfig.INSTANCE.currencyCode); check(.92d, ModConfig.INSTANCE.currencyPerUsd);
                seed(); ModConfig.save();
                client.setScreen(new RenderScreen());
                SkyblockUsdMod.LOGGER.info("CTM_ROW_TESTS_PASS: real vanilla row constructor, fixed formats, widths, toggles and saved settings");
            } catch (Throwable ex) { throw new AssertionError("CTM_RENDER_TEST_FAILURE", ex); }
        });
    }
    private static void seed() throws Exception {
        setStatic(SkyblockUsdModClient.class, "skyblock", true);
        long now = System.currentTimeMillis();
        setStatic(CookiePriceFetcher.class, "state", new CookiePriceFetcher.State(new BazaarQuote(12_295_597.2, now), null));
        ModConfig.INSTANCE = new ModConfig();
    }
    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(null, value);
    }
    private static void check(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
    private static FixedFormat fixed(String value) { return new FixedFormat(Component.literal(value)); }
    private static PlayerScoreEntry entry(String name, int sort, NumberFormat format) {
        return new PlayerScoreEntry("fixture", sort, Component.literal(name), format);
    }
    private static void assertRow(Minecraft client, Scoreboard board, PlayerScoreEntry entry, NumberFormat fallback,
                                  String expectedName, String expectedValue) throws Exception {
        int originalScore = entry.value();
        Object row = rowFactory.invoke(client.gui, board, fallback, entry);
        Method name = row.getClass().getDeclaredMethod("name"), value = row.getClass().getDeclaredMethod("score"), width = row.getClass().getDeclaredMethod("scoreWidth");
        name.setAccessible(true); value.setAccessible(true); width.setAccessible(true);
        check(expectedName, ((Component) name.invoke(row)).getString());
        check(expectedValue, ((Component) value.invoke(row)).getString());
        check(client.font.width((Component) value.invoke(row)), width.invoke(row));
        check(originalScore, entry.value());
    }
    private static void rows(Minecraft client) throws Exception {
        var board = new Scoreboard();
        var fallback = StyledFormat.SIDEBAR_DEFAULT;
        assertRow(client, board, entry("Purse: 7,416,6", 11, null), fallback, "Purse: $1.78", "");
        assertRow(client, board, entry("Purse: 7,014,5", 56, fallback), fallback, "Purse: $1.69", "");
        assertRow(client, board, entry("Purse: 7,416,6", 8, fixed("11")), fallback, "Purse: $1.78", "");
        assertRow(client, board, entry("Purse: 7,014,5", 8, fixed("56")), fallback, "Purse: $1.69", "");
        assertRow(client, board, entry("Purse: ", 8, fixed("7,416,611")), fallback, "Purse: $1.78", "");
        assertRow(client, board, entry("", 8, fixed("Purse: 7,416,611")), fallback, "Purse: $1.78", "");
        assertRow(client, board, entry("Purse: 7,416,6", 987, null), fixed("11"), "Purse: $1.78", "");
        assertRow(client, board, entry("Purse: 7,416,611", 8, null), fallback, "Purse: $1.78", "8");
        assertRow(client, board, entry("Purse: 7,416,611", 8, BlankFormat.INSTANCE), fallback, "Purse: $1.78", "");
        assertRow(client, board, entry("Bits: ", 8, fixed("7,120")), fallback, "Bits: ", "7,120");
        assertRow(client, board, entry(LIVE_PURSE, 5, BlankFormat.INSTANCE), fallback, "Purse: $1.78 (+5)", "");
        assertRow(client, board, entry(LIVE_PURSE, 5, null), BlankFormat.INSTANCE, "Purse: $1.78 (+5)", "");
        ModConfig.INSTANCE.showCookies = true; ModConfig.INSTANCE.keepCoins = true;
        assertRow(client, board, entry(LIVE_PURSE, 5, BlankFormat.INSTANCE), fallback,
                "Purse: 7,416,701 [$1.78 | 0.603 cookies] (+5)", "");
        ModConfig.INSTANCE.showCookies = false; ModConfig.INSTANCE.keepCoins = false;
        var liveTeam = board.addPlayerTeam("live-purse");
        liveTeam.setPlayerPrefix(Component.literal("Purse: §67,416,7"));
        liveTeam.setPlayerSuffix(Component.literal("§601 §e(+5)"));
        board.addPlayerToTeam("§p", liveTeam);
        assertRow(client, board, new PlayerScoreEntry("§p", 5, null, BlankFormat.INSTANCE), fallback, "Purse: $1.78 (+5)", "");
        board.removePlayerTeam(liveTeam);
        check("1.2.6", com.google.gson.JsonParser.parseString(SidebarDiagnostics.report(client)).getAsJsonObject().get("version").getAsString());
        SkyblockUsdMod.LOGGER.info("CTM_PURSE_DIAGNOSTIC_PASS: exact section-p row, all 7,416,701 coins, BlankFormat, rawScore=5 unchanged, gain suffix preserved");
        var team = board.addPlayerTeam("team"); team.setPlayerPrefix(Component.literal("Purse: "));
        board.addPlayerToTeam("fixture", team);
        assertRow(client, board, entry("7,416,6", 8, fixed("11")), fallback, "Purse: $1.78", "");
        board.removePlayerTeam(team);
        ModConfig.INSTANCE.enabled = false;
        assertRow(client, board, entry("Purse: 7,416,6", 8, fixed("11")), fallback, "Purse: 7,416,6", "11");
        ModConfig.INSTANCE.enabled = true;
        setStatic(SkyblockUsdModClient.class, "skyblock", false);
        assertRow(client, board, entry("Purse: 7,416,6", 8, fixed("11")), fallback, "Purse: 7,416,6", "11");
        setStatic(SkyblockUsdModClient.class, "skyblock", true);
        check("Worth $3.27", CoinText.convert(Component.literal("Worth 13.6M coins"), true, true).getString());
        check("Price per unit: $0.01", CoinText.convert(Component.literal("Price per unit: 57,716.6 coins"), true, true).getString());
    }
    private static void click(Screen screen, String prefix) throws Exception {
        for (var child : List.copyOf(screen.children())) {
            if (child instanceof Button button && button.getMessage().getString().startsWith(prefix)) {
                for (Method method : button.getClass().getMethods()) {
                    if (method.getName().equals("onPress") && method.getParameterCount() <= 1) {
                        method.invoke(button, new Object[method.getParameterCount()]); return;
                    }
                }
            }
        }
        throw new AssertionError("Cannot click " + prefix);
    }
    private static Button button(Screen screen, String prefix) {
        return screen.children().stream().filter(c -> c instanceof Button b && b.getMessage().getString().startsWith(prefix))
                .map(c -> (Button)c).findFirst().orElseThrow();
    }
    private static void edit(Screen screen, String label, String value) {
        screen.children().stream().filter(c -> c instanceof EditBox box && box.getMessage().getString().equals(label))
                .map(c -> (EditBox)c).findFirst().orElseThrow().setValue(value);
    }
    private static void assertTooltips(Screen screen) throws Exception {
        Field field = AbstractWidget.class.getDeclaredField("tooltip"); field.setAccessible(true);
        for (var child : screen.children()) if (child instanceof AbstractWidget widget) {
            Object holder = field.get(widget); boolean found = false;
            for (Field f : holder.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                if (f.get(holder) instanceof Tooltip tooltip && !tooltip.toCharSequence(Minecraft.getInstance()).isEmpty()) found = true;
            }
            check(true, found);
        }
    }
    private static void assertBounds(Screen screen) {
        for (var child : screen.children()) if (child instanceof AbstractWidget widget) {
            if (widget.getX() < 0 || widget.getY() < 0 || widget.getX() + widget.getWidth() > screen.width || widget.getY() + widget.getHeight() > screen.height)
                throw new AssertionError("Widget outside minimum GUI: " + widget.getMessage().getString());
        }
    }
    private static final class FixtureBoard extends Scoreboard {
        @Override public Collection<PlayerScoreEntry> listPlayerScores(Objective objective) {
            return List.of(entry(LIVE_PURSE, 5, BlankFormat.INSTANCE),
                    new PlayerScoreEntry("split-fixture", 11, Component.literal("Purse: 7,416,6"), StyledFormat.SIDEBAR_DEFAULT),
                    new PlayerScoreEntry("bits", 7, Component.literal("Bits: "), fixed("7,120")));
        }
    }
    private static final class RenderScreen extends CoinsToMoneyConfigScreen {
        private int backgrounds;
        private int frames;
        private final Objective objective;
        RenderScreen() {
            super(null);
            var board = new FixtureBoard();
            objective = board.addObjective("fixture", ObjectiveCriteria.DUMMY, Component.literal("SKYBLOCK"), ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
        }
        @Override public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float delta) {
            backgrounds++;
            graphics.blurBeforeThisStratum();
            graphics.fill(0, 0, width, height, 0x99000000);
        }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {
            AbstractWidget hovered = (AbstractWidget) children().get(frames % children().size());
            hovered.setTooltipDelay(Duration.ZERO);
            super.extractRenderState(graphics, hovered.getX() + 2, hovered.getY() + 2, delta);
            check(frames + 1, backgrounds);
            try {
                setStatic(SkyblockUsdModClient.class, "skyblock", true);
                sidebarRenderer.invoke(minecraft.gui, graphics, objective);
            } catch (Exception ex) { throw new AssertionError("Actual sidebar render failed", ex); }
            IntegrationSmokeTest.render(graphics, frames);
            if (++frames == 60) {
                SkyblockUsdMod.LOGGER.info("CTM_SETTINGS_FRAMES_PASS: 60 settings/tooltip/blur/live section-p and StyledFormat sidebar frames");
                minecraft.setScreen(new CurrencyRenderScreen());
            }
        }
    }
    private static final class CurrencyRenderScreen extends CurrencyConfigScreen {
        private int frames;
        CurrencyRenderScreen() { super(null); }
        @Override public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float delta) {
            graphics.blurBeforeThisStratum(); graphics.fill(0, 0, width, height, 0x99000000);
        }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {
            AbstractWidget hovered = (AbstractWidget) children().get(frames % children().size());
            hovered.setTooltipDelay(Duration.ZERO);
            super.extractRenderState(graphics, hovered.getX() + 2, hovered.getY() + 2, delta);
            IntegrationSmokeTest.render(graphics, frames);
            if (++frames == 60) {
                SkyblockUsdMod.LOGGER.info("CTM_RENDER_TESTS_PASS: 120 real frames, both screens, hover tooltips, currency validation and sidebar");
                minecraft.stop();
            }
        }
    }
}

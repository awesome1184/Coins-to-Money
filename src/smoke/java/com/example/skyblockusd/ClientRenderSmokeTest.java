package com.example.skyblockusd;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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
                var screen = new RenderScreen();
                client.setScreen(screen);
                click(screen, "Show cookies:"); check(true, ModConfig.INSTANCE.showCookies);
                click(screen, "Show dollars:"); check(false, ModConfig.INSTANCE.showUsd);
                click(screen, "Keep coin amounts:"); check(true, ModConfig.INSTANCE.keepCoins);
                click(screen, "USD decimals:"); check(3, ModConfig.INSTANCE.decimalPlaces);
                ModConfig.load(); check(true, ModConfig.INSTANCE.showCookies); check(3, ModConfig.INSTANCE.decimalPlaces);
                screen.onClose();
                seed();
                ModConfig.save();
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
        assertRow(client, board, entry("Purse: 7,416,6", 8, fixed("11")), fallback, "Purse: $1.78", "");
        assertRow(client, board, entry("Purse: 7,014,5", 8, fixed("56")), fallback, "Purse: $1.69", "");
        assertRow(client, board, entry("Purse: ", 8, fixed("7,416,611")), fallback, "Purse: $1.78", "");
        assertRow(client, board, entry("", 8, fixed("Purse: 7,416,611")), fallback, "Purse: $1.78", "");
        assertRow(client, board, entry("Purse: 7,416,6", 987, null), fixed("11"), "Purse: $1.78", "");
        assertRow(client, board, entry("Purse: 7,416,611", 8, null), fallback, "Purse: $1.78", "8");
        assertRow(client, board, entry("Purse: 7,416,611", 8, BlankFormat.INSTANCE), fallback, "Purse: $1.78", "");
        assertRow(client, board, entry("Bits: ", 8, fixed("7,120")), fallback, "Bits: ", "7,120");
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
    private static void click(RenderScreen screen, String prefix) throws Exception {
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
    private static final class FixtureBoard extends Scoreboard {
        @Override public Collection<PlayerScoreEntry> listPlayerScores(Objective objective) {
            return List.of(entry("Purse: 7,416,6", 8, fixed("11")), new PlayerScoreEntry("bits", 7, Component.literal("Bits: "), fixed("7,120")));
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
            // Emulate the in-world blur even in the CI client's title-screen environment.
            // The real GuiRenderState rejects a second blur, reproducing the reported crash.
            graphics.blurBeforeThisStratum();
            graphics.fill(0, 0, width, height, 0x99000000);
        }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {
            super.extractRenderState(graphics, x, y, delta);
            check(frames + 1, backgrounds);
            try {
                setStatic(SkyblockUsdModClient.class, "skyblock", true);
                sidebarRenderer.invoke(minecraft.gui, graphics, objective);
            } catch (Exception ex) { throw new AssertionError("Actual sidebar render failed", ex); }
            if (++frames == 60) {
                SkyblockUsdMod.LOGGER.info("CTM_RENDER_TESTS_PASS: 60 settings/blur/sidebar frames without crash");
                minecraft.stop();
            }
        }
    }
}

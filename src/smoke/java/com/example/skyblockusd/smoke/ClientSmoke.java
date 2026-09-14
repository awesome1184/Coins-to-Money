package com.example.skyblockusd.smoke;

import com.example.skyblockusd.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Lives in a separate test mod/source set; never included in the distributed JAR. */
public final class ClientSmoke implements ClientModInitializer {
    private static int ticks, phase, sidebarFrames, configFrames, transitionAt;
    private static FixtureScreen fixture;
    private static Field contextField, quoteField;
    private static Method rowMapper, sidebarRenderer;

    @Override public void onInitializeClient() {
        try {
            contextField = SkyblockUsdModClient.class.getDeclaredField("skyblock"); contextField.setAccessible(true);
            quoteField = CookiePriceFetcher.class.getDeclaredField("state"); quoteField.setAccessible(true);
            rowMapper = Gui.class.getDeclaredMethod("lambda$displayScoreboardSidebar$1", Scoreboard.class, NumberFormat.class, PlayerScoreEntry.class);
            rowMapper.setAccessible(true);
            sidebarRenderer = Gui.class.getDeclaredMethod("displayScoreboardSidebar", GuiGraphicsExtractor.class, Objective.class);
            sidebarRenderer.setAccessible(true);
        } catch (ReflectiveOperationException ex) { throw new AssertionError("Rendering contracts changed", ex); }
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ticks++;
            if (phase == 0 && ticks > 80 && client.screen != null) {
                ModConfig.INSTANCE = new ModConfig();
                withQuote(() -> assertions(client));
                fixture = new FixtureScreen();
                client.setScreen(fixture);
                phase = 1;
            } else if (phase == 1 && sidebarFrames >= 40) {
                System.out.println("CTM_SIDEBAR_RENDERED: enabled mixin, widths, tooltip callback and 40 real frames passed");
                transitionAt = ticks + 100; phase = 2;
            } else if (phase == 2 && ticks >= transitionAt) {
                check(client.options.getMenuBackgroundBlurriness() >= 1, "Blur must be enabled to test the reported crash");
                client.setScreen(new CoinsToMoneyConfigScreen(fixture)); phase = 3;
            } else if (phase == 3 && configFrames >= 40) {
                System.out.println("CTM_CONFIG_RENDERED: 40 real vanilla background/screen/tooltip frames passed");
                transitionAt = ticks + 100; phase = 4;
            } else if (phase == 4 && ticks >= transitionAt) {
                client.screen.onClose();
                check(client.screen == fixture, "Config did not return to its parent");
                System.out.println("CTM_RENDER_TESTS_PASSED");
                phase = 5;
                client.stop();
            }
        });
    }
    public static void frame(Screen screen) {
        if (screen instanceof FixtureScreen) sidebarFrames++;
        if (screen instanceof CoinsToMoneyConfigScreen) configFrames++;
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void withQuote(Runnable action) {
        try {
            boolean previousContext = contextField.getBoolean(null);
            Object previousQuote = quoteField.get(null);
            contextField.setBoolean(null, true);
            quoteField.set(null, new CookiePriceFetcher.State(new BazaarQuote(10_000_000, System.currentTimeMillis()), null));
            try { action.run(); }
            finally { contextField.setBoolean(null, previousContext); quoteField.set(null, previousQuote); }
        } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }
    private static Object map(Minecraft client, String left, String right, NumberFormat format) {
        try {
            var entry = new PlayerScoreEntry("row", 8, Component.literal(left), format == null ? new FixedFormat(Component.literal(right)) : format);
            Object result = rowMapper.invoke(client.gui, new Scoreboard(), StyledFormat.SIDEBAR_DEFAULT, entry);
            check(entry.value() == 8, "Raw ordering score was mutated");
            return result;
        } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }
    private static Object field(Object row, String field) {
        try { Method accessor = row.getClass().getDeclaredMethod(field); accessor.setAccessible(true); return accessor.invoke(row); }
        catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }
    private static void checkRow(Minecraft client, String left, String right, String expected) {
        Object row = map(client, left, right, null);
        Component name = (Component) field(row, "name"), score = (Component) field(row, "score");
        check((name.getString() + score.getString()).equals(expected), "Unexpected actual rendered row: " + name.getString() + " / " + score.getString());
        check((int) field(row, "scoreWidth") == client.font.width(score), "Converted score width is stale");
    }
    private static void assertions(Minecraft client) {
        checkRow(client, "Purse: ", "7,416,611", "Purse: $2.19");
        checkRow(client, "Purse: 7,014,5", "56", "Purse: $2.07");
        checkRow(client, "Purse: 7,014,", "056", "Purse: $2.07");
        checkRow(client, "", "Purse: 10,000,000,000", "Purse: $2,954.55");
        checkRow(client, "Bits: ", "7,120", "Bits: 7,120");
        ModConfig.INSTANCE.enabled = false;
        checkRow(client, "Purse: ", "7,416,611", "Purse: 7,416,611");
        ModConfig.INSTANCE.enabled = true;
        ModConfig.INSTANCE.enablePurse = false;
        checkRow(client, "Purse: ", "7,416,611", "Purse: 7,416,611");
        ModConfig.INSTANCE.enablePurse = true;
        Object ordinary = map(client, "Purse: 7,014,556", "", StyledFormat.SIDEBAR_DEFAULT);
        check(((Component) field(ordinary, "score")).getString().equals("8"), "Ordinary ordering score was suppressed");
        check(((Component) field(ordinary, "name")).getString().equals("Purse: $2.07"), "Ordering score was appended to currency");
        List<Component> lines = tooltip();
        check(lines.get(1).getString().equals("Worth $4.02"), "Real tooltip callback did not replace the total");
        check(lines.get(3).getString().equals("Price per unit: $0.02"), "Real tooltip callback did not replace per-unit price");
        check(lines.get(2).getString().equals("Offer amount: 246x"), "Tooltip callback changed a quantity");
        check(lines.size() == 4, "Tooltip callback inserted unwanted annotation lines");
        ModConfig.INSTANCE.enableTooltips = false;
        check(tooltip().get(1).getString().equals("Worth 13.6M coins"), "Disabled tooltip callback changed text");
        ModConfig.INSTANCE.enableTooltips = true;
    }
    private static List<Component> tooltip() {
        List<Component> lines = new ArrayList<>(List.of(
                Component.literal("SELL Example Shard").withStyle(ChatFormatting.GREEN),
                Component.literal("Worth 13.6M coins").withStyle(ChatFormatting.GRAY),
                Component.literal("Offer amount: 246x").withStyle(ChatFormatting.GRAY),
                Component.literal("Price per unit: ").withStyle(ChatFormatting.GRAY).append(Component.literal("57,716.6 coins").withStyle(ChatFormatting.GOLD))));
        ItemTooltipCallback.EVENT.invoker().getTooltip(new ItemStack(Items.STONE), Item.TooltipContext.EMPTY, TooltipFlag.NORMAL, lines);
        return lines;
    }
    private static final class FixtureScreen extends Screen {
        private final Scoreboard board = new Scoreboard();
        private final Objective objective = board.addObjective("test", ObjectiveCriteria.DUMMY, Component.literal("SKYBLOCK").withStyle(ChatFormatting.YELLOW), ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
        FixtureScreen() {
            super(Component.literal("Coins to Money rendering regression"));
            add("purse", 3, "Purse: ", "7,416,611", ChatFormatting.GOLD);
            add("piggy", 2, "Piggy: 7,014,5", "56", ChatFormatting.GOLD);
            add("bits", 1, "Bits: ", "7,120", ChatFormatting.AQUA);
        }
        private void add(String id, int order, String name, String value, ChatFormatting color) {
            ScoreAccess score = board.getOrCreatePlayerScore(ScoreHolder.forNameOnly(id), objective);
            score.set(order);
            score.display(Component.literal(name));
            score.numberFormatOverride(new FixedFormat(Component.literal(value).withStyle(color)));
        }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {
            super.extractRenderState(graphics, x, y, delta);
            withQuote(() -> {
                try { sidebarRenderer.invoke(minecraft.gui, graphics, objective); }
                catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
                graphics.setComponentTooltipForNextFrame(font, tooltip(), 16, 60);
            });
        }
    }
}

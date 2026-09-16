package com.example.skyblockusd;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import java.lang.reflect.Field;

/** Additional real-mod lifecycle/unit checks, isolated from the distributable. */
public final class FinalIntegrationSmokeTest implements ClientModInitializer {
    private boolean done;
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (done || !FabricLoader.getInstance().isModLoaded("skyhanni")
                    || !(client.gui.screen() instanceof CoinsToMoneyConfigScreen)) return;
            done = true;
            ModConfig saved = ModConfig.INSTANCE;
            try {
                ModConfig.INSTANCE = new ModConfig(); SkyHanniCompat.tick();
                Class<?> type = Class.forName("at.hannibal2.skyhanni.utils.ItemPriceUtils");
                Object instance = type.getField("INSTANCE").get(null);
                var name = type.getMethod("getPriceName-0mM9I0c", String.class, Number.class, double.class);
                String result = CoinParser.plain((String)name.invoke(instance, "SKYBLOCK_COIN", 1, 12_295_597.2));
                check(result.equals("$2.95"), "Typed coin name retained a false unit: " + result);
                ModConfig.INSTANCE.showCookies = true;
                result = CoinParser.plain((String)name.invoke(instance, "SKYBLOCK_COIN", 1, 12_295_597.2));
                check(result.equals("$2.95 [1.000 cookies]"), "Combined typed coin name: " + result);
                ModConfig.INSTANCE.enabled = false;
                result = CoinParser.plain((String)name.invoke(instance, "SKYBLOCK_COIN", 1, 12_295_597.2));
                check(result.endsWith(" coins") && !result.contains("$"), "Disabled formatter changed");
                // Request the native self-clearing one-frame update, not permanent dirty=true.
                Class<?> manager = Class.forName("at.hannibal2.skyhanni.data.TrackerManager");
                Field changed = manager.getDeclaredField("hasChanged"); changed.setAccessible(true); changed.setBoolean(null, false);
                Object tracker = manager.getField("INSTANCE").get(null);
                manager.getMethod("setDirty", boolean.class).invoke(tracker, false);
                ModConfig.INSTANCE.enabled = true; ModConfig.INSTANCE.decimalPlaces = 5; SkyHanniCompat.tick();
                check(changed.getBoolean(null), "Missing one-frame invalidation");
                check(Boolean.FALSE.equals(manager.getMethod("getDirty").invoke(tracker)), "Permanent dirty flag would rebuild every frame");
                SkyblockUsdMod.LOGGER.info("CTM_FINAL_INTEGRATION_CHECKS_PASS: native typed coin units and self-clearing tracker refresh");
            } catch (Exception ex) { throw new AssertionError("Extra integration failed", ex); }
            finally { ModConfig.INSTANCE = saved; SkyHanniCompat.tick(); }
        });
    }
    private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
}

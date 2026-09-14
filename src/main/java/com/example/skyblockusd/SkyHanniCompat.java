package com.example.skyblockusd;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import java.util.Objects;

/** Scoped display adapters: never intercept generic numbers, prices or tracker arithmetic. */
public final class SkyHanniCompat {
    private static long scalarCalls, cropCalls, textChanges, revision = 1;
    private static Object lastView;
    private static boolean triedTracker;
    private static Object tracker;
    private static java.lang.reflect.Method dirty;
    private SkyHanniCompat() { }
    public static boolean active() {
        return ModConfig.INSTANCE.enabled && ModConfig.INSTANCE.enableSkyHanni
                && (SkyblockUsdModClient.inSkyblock() || NativeModApis.hanniSkyblock());
    }
    public static long revision() { return revision; }
    /** Rebuild native tracker containers when display settings/quotes change, not balances. */
    public static void tick() {
        var c = ModConfig.INSTANCE; var state = CookiePriceFetcher.state(); long now = System.currentTimeMillis();
        var view = java.util.Arrays.asList(active(), state.quote(), state.available(now), state.stale(now), c.showUsd, c.showCookies,
                c.keepCoins, c.decimalPlaces, c.cookieDecimalPlaces, c.currencyCode, c.currencyPerUsd, c.displayOrder, c.displayLayout);
        if (Objects.equals(lastView, view)) return;
        lastView = view; revision++;
        if (!FabricLoader.getInstance().isModLoaded("skyhanni")) return;
        if (!triedTracker) {
            triedTracker = true;
            try {
                Class<?> cls = Class.forName("at.hannibal2.skyhanni.data.TrackerManager");
                tracker = cls.getField("INSTANCE").get(null); dirty = cls.getMethod("setDirty", boolean.class);
            } catch (ReflectiveOperationException | LinkageError ex) {
                SkyblockUsdMod.LOGGER.warn("SkyHanni tracker invalidation unavailable; displays update on their normal refresh");
            }
        }
        if (dirty != null) try { dirty.invoke(tracker, true); }
        catch (ReflectiveOperationException ex) { dirty = null; }
    }
    public static String scalar(String original, double coins) {
        scalarCalls++;
        if (!active()) return original;
        Component source = Component.literal(original);
        Component converted = CoinText.convertKnownCoinValue(source, coins);
        return converted == source ? original : CustomScoreboardCompat.legacy(converted) + CustomScoreboardCompat.terminalStyle(source);
    }
    public static CharSequence crop(CharSequence original, double extraPerHour, double cropPerHour) {
        cropCalls++;
        return scalar(original.toString(), extraPerHour + cropPerHour);
    }
    public static String text(String original) {
        Component source = Component.literal(original);
        Component changed = component(source);
        return changed == source ? original : CustomScoreboardCompat.legacy(changed) + CustomScoreboardCompat.terminalStyle(source);
    }
    public static Component component(Component original) {
        if (!active()) return original;
        Component changed = CoinText.convertProfitText(original);
        if (changed != original) textChanges++;
        return changed;
    }
    public static long scalarCalls() { return scalarCalls; }
    public static long cropCalls() { return cropCalls; }
    public static long textChanges() { return textChanges; }
}

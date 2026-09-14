package com.example.skyblockusd;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import java.util.Objects;

/** Scoped display adapters: never intercept generic numbers, prices or tracker arithmetic. */
public final class SkyHanniCompat {
    private static long scalarCalls, cropCalls, textChanges, revision = 1;
    private static final java.util.regex.Pattern MONEY_MARKER = java.util.regex.Pattern.compile("\\p{Sc}|\\bcookies\\b|\\b[A-Z]{3} [+-]?<?[0-9]");
    private static Object lastView;
    private static boolean triedTracker;
    private static java.lang.reflect.Field trackerChanged;
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
                // Use the native one-frame invalidation flag. Setting dirty directly
                // would leave it true indefinitely and rebuild every tracker each frame.
                trackerChanged = cls.getDeclaredField("hasChanged"); trackerChanged.setAccessible(true);
            } catch (ReflectiveOperationException | LinkageError ex) {
                SkyblockUsdMod.LOGGER.warn("SkyHanni tracker invalidation unavailable; displays update on their normal refresh");
            }
        }
        if (trackerChanged != null) try { trackerChanged.setBoolean(null, true); }
        catch (ReflectiveOperationException ex) { trackerChanged = null; }
    }
    public static String scalar(String original, double coins) {
        scalarCalls++;
        if (!active()) return original;
        Component source = Component.literal(original);
        Component converted = CoinText.convertKnownCoinValue(source, coins);
        return converted == source ? original : CustomScoreboardCompat.legacy(converted) + CustomScoreboardCompat.terminalStyle(source);
    }
    /** This native helper appends " coins" after formatCoin. Remove that unit only
     * for its typed SKYBLOCK_COIN branch when the result contains our green equivalent. */
    public static String coinName(String internalName, String formatted) {
        if (!active() || ModConfig.INSTANCE.keepCoins || !internalName.equals("SKYBLOCK_COIN")
                || !formatted.endsWith(" coins") || !formatted.contains("§a")) return formatted;
        String visible = CoinParser.plain(formatted);
        if (!MONEY_MARKER.matcher(visible).find()) return formatted;
        return formatted.substring(0, formatted.length() - " coins".length());
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

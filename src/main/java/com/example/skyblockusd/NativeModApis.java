package com.example.skyblockusd;

import net.fabricmc.loader.api.FabricLoader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Optional read-only APIs, resolved lazily; no Kotlin dependency without the other mods. */
public final class NativeModApis {
    private NativeModApis() { }
    private record Getter(Object instance, Method method) {
        Object read() {
            try { return method.invoke(instance); }
            catch (ReflectiveOperationException | RuntimeException ex) { return null; }
        }
    }
    private static Getter getter(String cls, String method) {
        try {
            Class<?> type = Class.forName(cls);
            Method m = type.getMethod(method);
            return new Getter(Modifier.isStatic(m.getModifiers()) ? null : type.getField("INSTANCE").get(null), m);
        } catch (ReflectiveOperationException | LinkageError ex) {
            SkyblockUsdMod.LOGGER.warn("Optional integration API unavailable: {}.{}; retaining original display", cls, method);
            return null;
        }
    }
    private static final class Custom {
        static final Getter CONTEXT = getter("tech.thatgravyboat.skyblockapi.api.location.LocationAPI", "isOnSkyBlock");
        static final Getter PURSE = getter("tech.thatgravyboat.skyblockapi.api.profile.currency.CurrencyAPI", "getPurse");
    }
    private static final class Hanni {
        static final Getter CONTEXT = getter("at.hannibal2.skyhanni.utils.SkyBlockUtils", "getInSkyBlock");
    }
    public static boolean customSkyblock() {
        return FabricLoader.getInstance().isModLoaded("customscoreboard") && Custom.CONTEXT != null && Boolean.TRUE.equals(Custom.CONTEXT.read());
    }
    public static boolean hanniSkyblock() {
        return FabricLoader.getInstance().isModLoaded("skyhanni") && Hanni.CONTEXT != null && Boolean.TRUE.equals(Hanni.CONTEXT.read());
    }
    public static double purse() {
        if (!FabricLoader.getInstance().isModLoaded("customscoreboard") || Custom.PURSE == null) return Double.NaN;
        Object value = Custom.PURSE.read();
        return value instanceof Number n && n.doubleValue() >= 0 && Double.isFinite(n.doubleValue()) ? n.doubleValue() : Double.NaN;
    }
}

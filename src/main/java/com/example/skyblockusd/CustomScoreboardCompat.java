package com.example.skyblockusd;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.util.Locale;

/** Display-only integration. Never changes SkyBlock API balances or server scoreboard data. */
public final class CustomScoreboardCompat {
    private static long numberCalls, chunkCalls, widgetCalls;
    private CustomScoreboardCompat() { }
    private static boolean active() {
        return SkyblockUsdModClient.inSkyblock() && ModConfig.INSTANCE.enabled && ModConfig.INSTANCE.enablePurse;
    }
    static boolean coinLabel(String label) {
        return switch (CoinParser.plain(label).strip().toUpperCase(Locale.ROOT)) {
            case "PURSE", "PIGGY", "PIGGY BANK" -> true;
            default -> false;
        };
    }
    public static Component number(Component label, Component value) {
        numberCalls++;
        if (!active() || !coinLabel(VisibleText.plain(label))) return value;
        return CoinText.convertPurseValue(value, purse(), CookiePriceFetcher.state(), System.currentTimeMillis(), ModConfig.INSTANCE);
    }
    public static Component widget(Component value) {
        widgetCalls++;
        return active() && CoinParser.purse(VisibleText.plain(value)).isPresent() ? CoinText.convert(value, true, false) : value;
    }
    public static String chunk(String kind, String original) {
        chunkCalls++;
        if (!active() || !kind.equals("PURSE")) return original;
        Component input = Component.literal(original);
        Component converted = CoinText.convertPurseValue(input, purse(), CookiePriceFetcher.state(), System.currentTimeMillis(), ModConfig.INSTANCE);
        return converted == input ? original : legacy(converted, input);
    }
    // Resolve the optional dependency's read-only getter once. Never modify its profile data.
    private record PurseReader(Object instance, java.lang.reflect.Method getter) { }
    private static final class Access {
        private static final PurseReader READER = resolve();
        private static PurseReader resolve() {
            try {
                Class<?> type = Class.forName("tech.thatgravyboat.skyblockapi.api.profile.currency.CurrencyAPI");
                return new PurseReader(type.getField("INSTANCE").get(null), type.getMethod("getPurse"));
            } catch (ReflectiveOperationException | LinkageError ex) {
                SkyblockUsdMod.LOGGER.warn("CustomScoreboard purse API unavailable; keeping its original display", ex);
                return null;
            }
        }
    }
    private static double purse() {
        try {
            PurseReader reader = Access.READER;
            return reader == null ? Double.NaN : ((Number) reader.getter().invoke(reader.instance())).doubleValue();
        } catch (ReflectiveOperationException | RuntimeException ex) { return Double.NaN; }
    }
    // ChunkedStat's API returns legacy strings. Component paths keep all interaction styles.
    static String legacy(Component component, Component original) {
        StringBuilder out = new StringBuilder();
        Style[] previous = {null};
        VisibleText.visit(component, (index, style, cp) -> {
            if (!style.equals(previous[0])) { out.append(codes(style)); previous[0] = style; }
            out.appendCodePoint(cp); return true;
        });
        Style[] terminal = {Style.EMPTY};
        VisibleText.visit(original, (index, style, cp) -> { terminal[0] = style; return true; });
        out.append(codes(terminal[0]));
        return out.toString();
    }
    private static String codes(Style style) {
        StringBuilder out = new StringBuilder("§r");
        if (style.getColor() != null) {
            for (ChatFormatting format : ChatFormatting.values()) {
                if (format.getColor() != null && format.getColor() == style.getColor().getValue()) { out.append(format); break; }
            }
        }
        if (style.isBold()) out.append(ChatFormatting.BOLD);
        if (style.isItalic()) out.append(ChatFormatting.ITALIC);
        if (style.isUnderlined()) out.append(ChatFormatting.UNDERLINE);
        if (style.isStrikethrough()) out.append(ChatFormatting.STRIKETHROUGH);
        if (style.isObfuscated()) out.append(ChatFormatting.OBFUSCATED);
        return out.toString();
    }
    public static long numberCalls() { return numberCalls; }
    public static long chunkCalls() { return chunkCalls; }
    public static long widgetCalls() { return widgetCalls; }
}

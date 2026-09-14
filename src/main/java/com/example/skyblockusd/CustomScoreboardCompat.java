package com.example.skyblockusd;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** Optional display-only adapters. Never write to CurrencyAPI or server scoreboard data. */
public final class CustomScoreboardCompat {
    private static long numberCalls, numberChanges, lineCalls, chunkCalls;
    private CustomScoreboardCompat() { }
    public static boolean active() {
        return (SkyblockUsdModClient.inSkyblock() || NativeModApis.customSkyblock()) && ModConfig.INSTANCE.enabled && ModConfig.INSTANCE.enablePurse;
    }
    public static Component number(Component label, Component number) {
        numberCalls++;
        String name = VisibleText.plain(label).strip();
        if (!active() || !(name.equalsIgnoreCase("Purse") || name.equalsIgnoreCase("Piggy") || name.equalsIgnoreCase("Piggy Bank"))) return number;
        double coins = NativeModApis.purse();
        Component converted = Double.isFinite(coins) ? CoinText.convertKnownCoinValue(number, coins) : CoinText.convertKnownCoinValue(number);
        if (converted != number) numberChanges++;
        return converted;
    }
    public static Component vanillaLine(Component line) {
        lineCalls++;
        return active() ? CoinText.convert(line, true, false) : line;
    }
    public static String chunkedPurse(String original) {
        chunkCalls++;
        if (!active()) return original;
        Component source = Component.literal(original);
        double coins = NativeModApis.purse();
        Component converted = Double.isFinite(coins) ? CoinText.convertKnownCoinValue(source, coins) : CoinText.convertKnownCoinValue(source);
        return converted == source ? original : legacy(converted) + terminalStyle(source);
    }
    public static long numberCalls() { return numberCalls; }
    public static long numberChanges() { return numberChanges; }
    public static long lineCalls() { return lineCalls; }
    public static long chunkCalls() { return chunkCalls; }
    /** Restore the final original colour before a caller appends a unit or chunk icon. */
    public static String terminalStyle(Component original) {
        Style[] last = {Style.EMPTY};
        VisibleText.visit(original, (i, style, cp) -> { last[0] = style; return true; });
        String coded = legacy(Component.literal(" ").withStyle(last[0]));
        return coded.substring(0, coded.length() - 1);
    }
    public static String stringNumber(String label, String value) {
        Component original = Component.literal(value);
        Component changed = number(Component.literal(label), original);
        return changed == original ? value : legacy(changed) + terminalStyle(original);
    }
    public static String legacy(Component component) {
        StringBuilder result = new StringBuilder();
        Style[] previous = {null};
        VisibleText.visit(component, (index, style, cp) -> {
            if (!style.equals(previous[0])) {
                result.append(ChatFormatting.RESET);
                if (style.getColor() != null) for (ChatFormatting colour : ChatFormatting.values()) {
                    if (colour.isColor() && colour.getColor().intValue() == style.getColor().getValue()) {
                        result.append(colour); break;
                    }
                }
                if (style.isBold()) result.append(ChatFormatting.BOLD);
                if (style.isItalic()) result.append(ChatFormatting.ITALIC);
                if (style.isUnderlined()) result.append(ChatFormatting.UNDERLINE);
                if (style.isStrikethrough()) result.append(ChatFormatting.STRIKETHROUGH);
                if (style.isObfuscated()) result.append(ChatFormatting.OBFUSCATED);
                previous[0] = style;
            }
            result.appendCodePoint(cp);
            return true;
        });
        return result.toString();
    }
}

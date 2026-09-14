package com.example.skyblockusd;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** Optional display-only adapters. Never write to CurrencyAPI or server scoreboard data. */
public final class CustomScoreboardCompat {
    private CustomScoreboardCompat() { }
    public static boolean active() {
        return SkyblockUsdModClient.inSkyblock() && ModConfig.INSTANCE.enabled && ModConfig.INSTANCE.enablePurse;
    }
    public static Component number(Component label, Component number) {
        String name = VisibleText.plain(label).strip();
        if (!active() || !(name.equalsIgnoreCase("Purse") || name.equalsIgnoreCase("Piggy") || name.equalsIgnoreCase("Piggy Bank"))) return number;
        return CoinText.convertKnownCoinValue(number);
    }
    public static Component vanillaLine(Component line) {
        // CustomScoreboard's uncustomized "Hypixel Lines" path bypasses Gui completely.
        return active() ? CoinText.convert(line, true, false) : line;
    }
    public static String chunkedPurse(String original) {
        if (!active()) return original;
        Component source = Component.literal(original);
        Component converted = CoinText.convertKnownCoinValue(source);
        return converted == source ? original : legacy(converted);
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

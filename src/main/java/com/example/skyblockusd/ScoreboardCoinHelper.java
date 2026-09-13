package com.example.skyblockusd;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.scores.PlayerTeam;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Handles Hypixel's split scoreboard coin format. */
public final class ScoreboardCoinHelper {
    private static final Pattern SPLIT_OWNER_PATTERN = Pattern.compile(
            "(?i)^(.*\\b(?:purse|piggy|balance|coins)\\s*[:=]\\s*)([\\d][\\d,]*)\\s*$"
    );

    private ScoreboardCoinHelper() {}

    /** Builds the name after the scoreboard team prefix/suffix have been applied. */
    public static Component buildTeamFormattedName(PlayerTeam team, Component ownerName) {
        MutableComponent result = Component.empty();
        if (team != null) result.append(team.getPlayerPrefix());
        result.append(ownerName.copy());
        if (team != null) result.append(team.getPlayerSuffix());
        return result;
    }

    public static boolean isSplitScoreboardCoinOwner(Component formattedName) {
        return formattedName != null && isSplitScoreboardCoinOwner(formattedName.getString());
    }

    public static boolean isSplitScoreboardCoinOwner(String formattedName) {
        return formattedName != null && SPLIT_OWNER_PATTERN.matcher(formattedName).matches();
    }

    /** Removes only the trailing numeric portion, keeping all preceding component styles intact. */
    public static Component stripSplitScoreboardNumber(Component formattedName) {
        if (formattedName == null) return null;
        Matcher matcher = SPLIT_OWNER_PATTERN.matcher(formattedName.getString());
        if (!matcher.matches()) return formattedName;

        List<StyledPart> parts = flatten(formattedName);
        MutableComponent rebuilt = Component.empty().withStyle(formattedName.getStyle());
        appendStyledRange(rebuilt, parts, 0, matcher.start(2));
        return rebuilt;
    }

    /** Reconstructs the full value as partial * 100 + the separate scoreboard value. */
    public static MutableComponent formatSplitScoreboardValue(
            Component formattedName,
            int score,
            MutableComponent vanillaScore
    ) {
        if (!SkyblockUsdMod.enabled || formattedName == null || vanillaScore == null) return null;

        Matcher matcher = SPLIT_OWNER_PATTERN.matcher(formattedName.getString());
        if (!matcher.matches()) return null;

        String partial = matcher.group(2).replace(",", "");
        try {
            long fullCoins = Math.addExact(Math.multiplyExact(Long.parseLong(partial), 100L), score);
            if (fullCoins < 0) return null;

            NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
            currency.setMinimumFractionDigits(ModConfig.decimalPlaces);
            currency.setMaximumFractionDigits(ModConfig.decimalPlaces);
            return Component.literal(currency.format(fullCoins / CookiePriceFetcher.coinsPerUsd))
                    .withStyle(vanillaScore.getStyle());
        } catch (ArithmeticException | NumberFormatException ignored) {
            return null;
        }
    }

    private static List<StyledPart> flatten(Component component) {
        List<StyledPart> parts = new ArrayList<>();
        component.visit((style, text) -> {
            if (!text.isEmpty()) parts.add(new StyledPart(text, style));
            return java.util.Optional.empty();
        }, component.getStyle());
        return parts;
    }

    private static void appendStyledRange(MutableComponent destination, List<StyledPart> parts, int start, int end) {
        if (start >= end) return;
        int position = 0;
        for (StyledPart part : parts) {
            int partStart = position;
            int partEnd = position + part.text().length();
            int from = Math.max(start, partStart);
            int to = Math.min(end, partEnd);
            if (from < to) {
                destination.append(Component.literal(part.text().substring(from - partStart, to - partStart)).withStyle(part.style()));
            }
            position = partEnd;
            if (position >= end) break;
        }
    }

    private record StyledPart(String text, Style style) {}
}

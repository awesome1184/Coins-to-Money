package com.example.skyblockusd;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Handles Hypixel's scoreboard format where the final digits live in the numeric score field. */
public final class ScoreboardCoinHelper {
    private static final String FORMAT_CODES = "(?:§[0-9a-fk-or])*";

    /**
     * Matches a scoreboard owner whose visible text is a coin label followed by a partial
     * numeric value, e.g. "Purse: 7,014,5". Hypixel then supplies the final digits separately
     * through PlayerScoreEntry.value().
     */
    private static final Pattern SPLIT_OWNER_PATTERN = Pattern.compile(
            "(?i)^(.*\\b(?:purse|piggy|balance|coins)\\s*[:=]\\s*)" +
            FORMAT_CODES + "([\\d,.]+)\\s*$"
    );

    private ScoreboardCoinHelper() {}

    public static boolean isSplitScoreboardCoinOwner(Component ownerName) {
        return ownerName != null && isSplitScoreboardCoinOwner(ownerName.getString());
    }

    public static boolean isSplitScoreboardCoinOwner(String owner) {
        return owner != null && SPLIT_OWNER_PATTERN.matcher(stripFormatting(owner)).matches();
    }

    /** Removes the partial numeric suffix so vanilla will not draw it on the left side. */
    public static Component stripSplitScoreboardNumber(Component ownerName) {
        if (ownerName == null) return null;
        String visible = ownerName.getString();
        Matcher matcher = SPLIT_OWNER_PATTERN.matcher(stripFormatting(visible));
        if (!matcher.matches()) return ownerName;

        String label = matcher.group(1);
        return Component.literal(label).withStyle(ownerName.getStyle());
    }

    /**
     * Reconstructs the full coin amount from the raw scoreboard owner plus score value and
     * returns the USD component with the score's existing formatting preserved.
     */
    public static MutableComponent formatSplitScoreboardValue(
            String owner,
            int score,
            MutableComponent vanillaScore
    ) {
        if (owner == null || vanillaScore == null) return null;

        String cleanOwner = stripFormatting(owner);
        Matcher matcher = SPLIT_OWNER_PATTERN.matcher(cleanOwner);
        if (!matcher.matches()) return null;

        String partialNumber = matcher.group(2).replace(",", "").replace(".", "");
        if (partialNumber.isEmpty()) return null;

        try {
            // Hypixel deliberately moves the final digits into PlayerScoreEntry.value().
            double coins = Double.parseDouble(partialNumber + Math.abs(score));
            if (!Double.isFinite(coins) || coins < 0 || !Double.isFinite(CookiePriceFetcher.coinsPerUsd)
                    || CookiePriceFetcher.coinsPerUsd <= 0) {
                return null;
            }

            NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
            currency.setMinimumFractionDigits(ModConfig.decimalPlaces);
            currency.setMaximumFractionDigits(ModConfig.decimalPlaces);

            return Component.literal(currency.format(coins / CookiePriceFetcher.coinsPerUsd))
                    .withStyle(vanillaScore.getStyle());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String stripFormatting(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "");
    }
}

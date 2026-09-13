package com.example.skyblockusd;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Handles Hypixel's scoreboard coin format where the final two digits are stored separately. */
public final class ScoreboardCoinHelper {
    private static final String FORMAT_CODES = "(?:§[0-9a-fk-or])*";
    private static final int MISSING_SCORE_DIGITS = 2;

    /** Matches labels such as "Purse: 7,014,5" or "Balance: 12,345". */
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

    /** Removes the partial number from the owner component so it cannot be drawn twice. */
    public static Component stripSplitScoreboardNumber(Component ownerName) {
        if (ownerName == null) return null;
        Matcher matcher = SPLIT_OWNER_PATTERN.matcher(stripFormatting(ownerName.getString()));
        if (!matcher.matches()) return ownerName;

        return Component.literal(matcher.group(1)).withStyle(ownerName.getStyle());
    }

    /**
     * Hypixel's sidebar can split the final two digits of a coin amount into PlayerScoreEntry.value().
     * We cannot reliably recover those digits at this rendering boundary, so the display-side
     * fallback deliberately treats the visible partial amount as missing exactly two digits:
     *     visibleAmount × 100
     * The separate score is replaced with the resulting USD value, so the raw digits disappear.
     */
    public static MutableComponent formatSplitScoreboardValue(
            String owner,
            int score,
            MutableComponent vanillaScore
    ) {
        if (!SkyblockUsdMod.enabled || owner == null || vanillaScore == null) return null;

        String cleanOwner = stripFormatting(owner);
        Matcher matcher = SPLIT_OWNER_PATTERN.matcher(cleanOwner);
        if (!matcher.matches()) return null;

        String partialNumber = matcher.group(2).replace(",", "").replace(".", "");
        if (partialNumber.isEmpty()) return null;

        try {
            double visibleAmount = Double.parseDouble(partialNumber);
            double coins = visibleAmount * Math.pow(10, MISSING_SCORE_DIGITS);

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

package com.example.skyblockusd;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import java.util.regex.Pattern;

/** Works on both DISPLAY columns, never guesses digits from the sorting score. */
public final class ScoreboardCoinHelper {
    private static final Pattern LABEL_ONLY = Pattern.compile("(?i)^\\h*(?:Purse|Piggy(?: Bank)?|Bank|Balance|Coins):\\h*$");
    private static final Pattern INCOMPLETE_GROUP = Pattern.compile("(?i)^.*\\b(?:Purse|Piggy(?: Bank)?|Bank|Balance|Coins):\\h*[+-]?[0-9]{1,3}(?:,[0-9]{3})*,[0-9]{0,2}$");
    public record Row(Component name, Component value) { }
    private ScoreboardCoinHelper() { }

    public static Component rawName(Scoreboard board, PlayerScoreEntry entry) {
        PlayerTeam team = board.getPlayersTeam(entry.owner());
        return PlayerTeam.formatNameForTeam(team, entry.ownerName());
    }
    public static Row convertRow(Component name, Component value, PlayerScoreEntry entry, NumberFormat fallback) {
        NumberFormat format = entry.numberFormatOverride() == null ? fallback : entry.numberFormatOverride();
        return convertRow(name, value, format instanceof FixedFormat, CookiePriceFetcher.state(),
                System.currentTimeMillis(), ModConfig.INSTANCE);
    }
    static Row convertRow(Component name, Component value, boolean fixedValue,
                          CookiePriceFetcher.State state, long now, ModConfig config) {
        if (!config.enabled || !config.enablePurse || !state.available(now)) return new Row(name, value);
        String left = VisibleText.plain(name);
        // FixedFormat contains server-provided text, not PlayerScoreEntry.value().
        // Hypixel can put some or ALL of the purse in that separate right column.
        // Some servers encode a numeric tail using StyledFormat (or another NumberFormat),
        // not FixedFormat. Only join that ambiguous numeric column when it completes an
        // INCOMPLETE comma group. A complete left amount must never absorb a sorting score.
        boolean incompleteGroup = INCOMPLETE_GROUP.matcher(left).matches();
        if (fixedValue || LABEL_ONLY.matcher(left).matches() || incompleteGroup) {
            Component combined = Component.empty().append(name).append(value);
            String visible = VisibleText.plain(combined);
            if (CoinParser.isBalance(visible) && CoinParser.find(visible, true, false).stream().anyMatch(a -> a.end() > left.length())) {
                Component converted = CoinText.convert(combined, true, false, state, now, config);
                if (converted != combined) return new Row(converted, Component.empty());
            }
        }
        Component convertedName = CoinParser.isBalance(left) ? CoinText.convert(name, true, false, state, now, config) : name;
        // Leave ordinary numeric ordering scores and all non-currency rows alone.
        Component convertedValue = fixedValue && CoinParser.isBalance(VisibleText.plain(value))
                ? CoinText.convert(value, true, false, state, now, config) : value;
        return new Row(convertedName, convertedValue);
    }
}

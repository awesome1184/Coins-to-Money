package com.example.skyblockusd;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import java.util.regex.Pattern;

/** Work on BOTH visible sidebar columns, never on the integer used to order the rows. */
public final class ScoreboardCoinHelper {
    private static final Pattern LABEL = Pattern.compile("(?i)\\b(?:Purse|Piggy(?: Bank)?|Bank|Balance|Coins):\\h*");
    public record Row(Component name, Component score) { }
    private ScoreboardCoinHelper() { }

    public static Component rawName(Scoreboard board, PlayerScoreEntry entry) {
        PlayerTeam team = board.getPlayersTeam(entry.owner());
        return PlayerTeam.formatNameForTeam(team, entry.ownerName());
    }

    public static Row replace(Component name, Component score, PlayerScoreEntry entry, NumberFormat fallback,
                              CookiePriceFetcher.State state, long now) {
        if (!state.available(now)) return new Row(name, score);
        String left = CoinParser.plain(name.getString());
        var label = LABEL.matcher(left);
        NumberFormat format = entry.numberFormatOverride() == null ? fallback : entry.numberFormatOverride();
        // FixedFormat stores arbitrary server-supplied text. Its digits need not match entry.value().
        // An ordinary numeric score is only currency when the name contains a bare currency label.
        if (label.find() && !score.getString().isEmpty()
                && (format instanceof FixedFormat || label.end() == left.length())
                && left.indexOf('$') < 0) {
            Component joined = Component.empty().append(name).append(score);
            Component converted = CoinText.replace(joined, true, false, state, now);
            if (converted != joined) return new Row(converted, Component.empty());
            return new Row(name, score); // malformed/incomplete joined amounts must remain untouched
        }
        return new Row(CoinText.replace(name, true, false, state, now),
                CoinText.replace(score, true, false, state, now));
    }
}

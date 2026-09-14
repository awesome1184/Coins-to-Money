package com.example.skyblockusd;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/** Additional MONEY labels scoped exclusively to SkyHanni's presentation objects. */
public final class ProfitText {
    private static final String N = "[+-]?(?:[0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.[0-9]+)?[kKmMbBtTqQ]?";
    private static final Pattern LABEL = Pattern.compile("(?im)(?:^|[|;])\\h*(?:(?:(?:Session|Total|This [a-z]+)\\h+)?Profit(?:\\h+per\\h+[a-z ]+|/h(?:r|our)?)?|Coins(?:/h(?:r|our)?|\\h+made)|Money/h(?:r|our)?|Purse|Piggy(?: Bank)?):\\h*(" + N + ")(?![\\w.,])");
    private static final Pattern SALE = Pattern.compile("(?im)^Selling [^\\r\\n]{1,120}? for\\h+(" + N + ")(?![\\w.,])\\h+each\\h*$");
    private static final Pattern TAIL = Pattern.compile("(?i)^\\h*(?:$|coins?\\b|[|;\\r\\n]|\\([+-][0-9,.]+\\)\\h*$)");
    private ProfitText() { }
    public static List<CoinParser.Amount> find(String text) {
        if (text.length() > 16_384 || CoinParser.isAnnotated(text)) return List.of();
        List<CoinParser.Amount> result = new ArrayList<>(CoinParser.find(text, false, false));
        for (Pattern pattern : List.of(LABEL, SALE)) {
            var m = pattern.matcher(text);
            while (m.find()) {
                if (pattern == LABEL && !TAIL.matcher(text.substring(m.end(1))).find()) continue;
                int start = m.start(1), end = m.end(1);
                double coins = CoinParser.parseNumber(m.group(1));
                if (!Double.isFinite(coins) || result.stream().anyMatch(a -> start < a.end() && end > a.start())) continue;
                result.add(new CoinParser.Amount(start, end, coins, m.group(1)));
            }
        }
        result.sort(Comparator.comparingInt(CoinParser.Amount::start));
        return List.copyOf(result);
    }
}

package com.example.skyblockusd;

import net.fabricmc.api.ModInitializer;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkyblockUsdMod implements ModInitializer {
    private static final Pattern COIN_PATTERN = Pattern.compile("([\\d,.]+)([kmbKMB]?)\\s*Coins", Pattern.CASE_INSENSITIVE);

    @Override
    public void onInitialize() {
        CookiePriceFetcher.startPeriodicUpdates(5);
    }

    public static String replaceCoinsString(String text) {
        if (text == null || !text.contains("Coins")) {
            return text;
        }

        Matcher matcher = COIN_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            try {
                String numberStr = matcher.group(1).replace(",", "");
                String suffix = matcher.group(2) != null ? matcher.group(2).toLowerCase() : "";
                double coins = Double.parseDouble(numberStr);

                switch (suffix) {
                    case "k": coins *= 1_000; break;
                    case "m": coins *= 1_000_000; break;
                    case "b": coins *= 1_000_000_000; break;
                }

                double usdValue = coins / CookiePriceFetcher.coinsPerUsd;
                NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
                matcher.appendReplacement(sb, formatter.format(usdValue));
                
            } catch (Exception e) {
                matcher.appendReplacement(sb, matcher.group(0)); 
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}

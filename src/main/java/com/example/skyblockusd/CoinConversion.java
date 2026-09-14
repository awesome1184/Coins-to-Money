package com.example.skyblockusd;

import java.util.Locale;

/** USD replacement-cost equivalent, not a cash-out value. No intermediate rounding. */
public record CoinConversion(double cookies, double usd) {
    public static final double GEMS_PER_COOKIE = 325d;
    public static final double BUNDLE_GEMS = 11_000d;
    public static final double BUNDLE_USD = 100d;
    public static final double USD_PER_COOKIE = GEMS_PER_COOKIE * BUNDLE_USD / BUNDLE_GEMS;

    public static CoinConversion of(double coins, double instantBuyPrice) {
        if (!Double.isFinite(coins) || !Double.isFinite(instantBuyPrice) || instantBuyPrice <= 0) {
            throw new IllegalArgumentException("Finite coins and a positive instant-buy price are required");
        }
        double cookies = coins / instantBuyPrice;
        double usd = cookies * USD_PER_COOKIE;
        if (!Double.isFinite(cookies) || !Double.isFinite(usd)) throw new IllegalArgumentException("Conversion overflow");
        return new CoinConversion(cookies, usd);
    }

    public String display() { return cookieText(3) + " | " + usdText(2); }
    public String display(ModConfig config) {
        if (config.showUsd && config.showCookies) return usdText(config.decimalPlaces) + " | " + cookieText(config.cookieDecimalPlaces);
        if (config.showCookies) return cookieText(config.cookieDecimalPlaces);
        return usdText(config.decimalPlaces);
    }
    public String moneyText(ModConfig config) {
        return MoneyCurrency.format(usd, config.currencyCode, config.currencyPerUsd, config.decimalPlaces);
    }
    public String cookieText(int precision) {
        return number(cookies, Math.clamp(precision, 1, 6), "") + " cookies";
    }
    public String usdText(int precision) {
        return number(usd, Math.clamp(precision, 2, 8), "$");
    }
    static String number(double value, int precision, String unit) {
        String sign = value < 0 ? "-" : "";
        double minimum = Math.pow(10, -precision);
        if (value != 0 && Math.abs(value) < minimum) {
            return sign + "<" + unit + String.format(Locale.US, "%." + precision + "f", minimum).trim();
        }
        return sign + unit + String.format(Locale.US, "%,." + precision + "f", Math.abs(value));
    }
}

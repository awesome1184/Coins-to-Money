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

    public String display() { return cookieText(cookies) + " cookies | $" + moneyText(usd) + " USD"; }
    private static String cookieText(double value) {
        if (value != 0 && Math.abs(value) < .001d) return value < 0 ? "-<0.001" : "<0.001";
        return String.format(Locale.US, "%,.3f", value == 0 ? 0d : value);
    }
    private static String moneyText(double value) {
        if (value != 0 && Math.abs(value) < .01d) return value < 0 ? "-<0.01" : "<0.01";
        return String.format(Locale.US, "%,.2f", value == 0 ? 0d : value);
    }
}

package com.example.skyblockusd;

import java.util.Currency;
import java.util.Locale;

/** Manual exchange rate: target currency units per ONE USD. No FX network requests. */
public final class MoneyCurrency {
    private MoneyCurrency() { }
    public static String code(String input) {
        if (input == null) throw new IllegalArgumentException("Enter a currency code such as USD, EUR or JPY.");
        String code = input.strip().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z]{3}")) throw new IllegalArgumentException("Use a three-letter currency code, such as EUR.");
        try {
            if (Currency.getInstance(code).getDefaultFractionDigits() < 0) throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Unknown money currency: " + code); }
        return code;
    }
    public static double rate(String input) {
        if (input == null || !input.strip().matches("[0-9]+(?:\\.[0-9]+)?"))
            throw new IllegalArgumentException("Use a positive decimal with a dot, for example 0.92.");
        double value = Double.parseDouble(input.strip());
        if (!Double.isFinite(value) || value <= 0 || value > 1e12)
            throw new IllegalArgumentException("Rate must be greater than zero and at most 1,000,000,000,000.");
        return value;
    }
    public static void validate(String code, double rate) {
        code(code);
        if (!Double.isFinite(rate) || rate <= 0 || rate > 1e12) throw new IllegalArgumentException("Invalid currency multiplier.");
        if (code.equals("USD") && rate != 1d) throw new IllegalArgumentException("USD must use a rate of 1. Change the currency code first.");
    }
    public static String prefix(String code) {
        String symbol = Currency.getInstance(code).getSymbol(Locale.US);
        return symbol.codePoints().allMatch(Character::isLetter) ? symbol + " " : symbol;
    }
    public static String format(double usd, String code, double rate, int precision) {
        validate(code, rate);
        double converted = usd * rate;
        if (!Double.isFinite(converted)) throw new IllegalArgumentException("Currency conversion overflow");
        return CoinConversion.number(converted, Math.clamp(precision, 2, 8), prefix(code));
    }
}

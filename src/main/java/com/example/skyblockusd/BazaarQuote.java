package com.example.skyblockusd;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Immutable quote timestamped by Hypixel, not by our last HTTP request. */
public record BazaarQuote(double instantBuyPrice, long updatedAt) {
    public static final long FRESH_MILLIS = 3 * 60_000L;
    public static final long MAX_AGE_MILLIS = 30 * 60_000L;
    private static final long MAX_RESPONSE_AGE = 10 * 60_000L;
    private static final long CLOCK_SKEW = 2 * 60_000L;

    public BazaarQuote {
        if (!Double.isFinite(instantBuyPrice) || instantBuyPrice <= 0 || updatedAt <= 0) throw new IllegalArgumentException("Invalid Bazaar quote");
    }
    public boolean usable(long now) { return updatedAt <= now + CLOCK_SKEW && now - updatedAt <= MAX_AGE_MILLIS; }
    public boolean stale(long now) { return now - updatedAt > FRESH_MILLIS; }

    public static BazaarQuote parse(String json, long now) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("success") || !root.get("success").getAsBoolean()) throw new IllegalArgumentException("Bazaar API reported failure");
            long updated = root.get("lastUpdated").getAsLong();
            if (updated <= 0 || updated > now + CLOCK_SKEW || now - updated > MAX_RESPONSE_AGE) throw new IllegalArgumentException("Old or invalid Bazaar timestamp");
            JsonObject cookie = root.getAsJsonObject("products").getAsJsonObject("BOOSTER_COOKIE");
            double best = Double.POSITIVE_INFINITY;
            // Hypixel's buy_summary is the instant BUY side (sell offers).
            // quick_status.buyPrice is a volume-weighted average, not this quote.
            for (JsonElement element : cookie.getAsJsonArray("buy_summary")) {
                JsonObject offer = element.getAsJsonObject();
                double price = offer.get("pricePerUnit").getAsDouble();
                double amount = offer.get("amount").getAsDouble();
                if (Double.isFinite(price) && price > 0 && Double.isFinite(amount) && amount >= 1) best = Math.min(best, price);
            }
            if (!Double.isFinite(best)) throw new IllegalArgumentException("No instant-buy cookie offers");
            return new BazaarQuote(best, updated);
        } catch (RuntimeException ex) { throw new IllegalArgumentException("Invalid cookie Bazaar response", ex); }
    }
}

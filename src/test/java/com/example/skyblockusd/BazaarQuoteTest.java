package com.example.skyblockusd;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BazaarQuoteTest {
    private static final long NOW = 1_800_000_000_000L;
    private static String payload(long time, String offers) {
        return "{\"success\":true,\"lastUpdated\":" + time + ",\"products\":{\"BOOSTER_COOKIE\":{"
                + "\"quick_status\":{\"buyPrice\":99999999,\"sellPrice\":1},"
                + "\"sell_summary\":[{\"pricePerUnit\":2,\"amount\":100}],\"buy_summary\":" + offers + "}}}";
    }
    @Test void selectsBestInstantBuyOfferNotWeightedAverageOrSellSide() {
        var quote = BazaarQuote.parse(payload(NOW, "[{\"pricePerUnit\":11000000,\"amount\":4},{\"pricePerUnit\":10000000,\"amount\":2}]"), NOW);
        assertEquals(10_000_000, quote.instantBuyPrice());
    }
    @Test void ignoresEmptyAndInvalidPriceLevels() {
        var quote = BazaarQuote.parse(payload(NOW, "[{\"pricePerUnit\":1,\"amount\":0},{\"pricePerUnit\":-2,\"amount\":4},{\"pricePerUnit\":9000000,\"amount\":1}]"), NOW);
        assertEquals(9_000_000, quote.instantBuyPrice());
    }
    @Test void failsClosedOnBadPayloads() {
        for (String json : new String[]{"null", "{}", "not json", "{\"success\":false}", payload(NOW, "[]"), payload(NOW, "null")}) assertThrows(IllegalArgumentException.class, () -> BazaarQuote.parse(json, NOW));
    }
    @Test void rejectsOldAndFutureServerTimestamps() {
        String offers = "[{\"pricePerUnit\":100,\"amount\":1}]";
        assertThrows(IllegalArgumentException.class, () -> BazaarQuote.parse(payload(NOW - 11 * 60_000, offers), NOW));
        assertThrows(IllegalArgumentException.class, () -> BazaarQuote.parse(payload(NOW + 3 * 60_000, offers), NOW));
    }
    @Test void staleQuotesHaveAFiniteLifetime() {
        var quote = new BazaarQuote(100, NOW);
        assertFalse(quote.stale(NOW)); assertTrue(quote.stale(NOW + 4 * 60_000));
        assertTrue(quote.usable(NOW + 4 * 60_000)); assertFalse(quote.usable(NOW + 31 * 60_000));
        assertFalse(new CookiePriceFetcher.State(null, "offline").available(NOW));
        assertTrue(new CookiePriceFetcher.State(quote, "offline").stale(NOW));
    }
}

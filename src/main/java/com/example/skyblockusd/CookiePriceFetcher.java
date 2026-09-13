package com.example.skyblockusd;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CookiePriceFetcher {
    public static volatile double coinsPerUsd = 5_200_000.0;

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "SkyblockUSD-API-Timer");
        thread.setDaemon(true);
        return thread;
    });

    public static void startPeriodicUpdates(long intervalMinutes) {
        SCHEDULER.scheduleAtFixedRate(CookiePriceFetcher::fetchPriceSync, 0, intervalMinutes, TimeUnit.MINUTES);
    }

    private static void fetchPriceSync() {
        HttpURLConnection conn = null;
        try {
            URL url = URI.create("https://api.hypixel.net/v2/skyblock/bazaar").toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8_000);
            conn.setReadTimeout(8_000);
            conn.setRequestProperty("Accept", "application/json");

            try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject cookie = response.getAsJsonObject("products")
                        .getAsJsonObject("BOOSTER_COOKIE")
                        .getAsJsonObject("quick_status");

                double cookiePrice = cookie.get("buyPrice").getAsDouble();
                if (cookiePrice > 0) {
                    coinsPerUsd = cookiePrice / 2.40;
                }
            }
        } catch (Exception ignored) {
            // Keep the last known rate if Hypixel is unreachable or rate-limits the request.
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}

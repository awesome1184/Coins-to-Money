package com.example.skyblockusd;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CookiePriceFetcher {
    public static volatile double coinsPerUsd = 5200000.0; 
    
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("SkyblockUSD-API-Timer");
        return thread;
    });

    public static void startPeriodicUpdates(long intervalMinutes) {
        scheduler.scheduleAtFixedRate(CookiePriceFetcher::fetchPriceSync, 0, intervalMinutes, TimeUnit.MINUTES);
    }

    private static void fetchPriceSync() {
        try {
            URL url = URI.create("https://api.hypixel.net/v2/skyblock/bazaar").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            InputStreamReader reader = new InputStreamReader(conn.getInputStream());
            JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
            
            double cookiePrice = response.getAsJsonObject("products")
                                         .getAsJsonObject("BOOSTER_COOKIE")
                                         .getAsJsonObject("quick_status")
                                         .get("sellPrice").getAsDouble();
            
            coinsPerUsd = cookiePrice / 2.40;
        } catch (Exception e) {
            // Falls back to the default rate if the API fails or rate-limits
        }
    }
}src/main/java/com/example/skyblockusd/

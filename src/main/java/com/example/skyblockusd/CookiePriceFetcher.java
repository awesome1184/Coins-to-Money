package com.example.skyblockusd;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Network IO is confined to one daemon worker. Rendering reads immutable snapshots. */
public final class CookiePriceFetcher {
    private static final String ENDPOINT = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final int MAX_BODY_BYTES = 8 * 1024 * 1024;
    private static final ScheduledExecutorService WORKER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "coins-to-money-bazaar");
        thread.setDaemon(true);
        return thread;
    });
    public record State(BazaarQuote quote, String error) {
        public boolean available(long now) { return quote != null && quote.usable(now); }
        public boolean stale(long now) { return error != null || (quote != null && quote.stale(now)); }
    }
    private static volatile State state = new State(null, null);
    private static volatile boolean active;
    private static boolean started;
    private static long lastAttempt;
    private CookiePriceFetcher() { }
    public static State state() { return state; }

    public static synchronized void start() {
        if (started) return;
        started = true;
        WORKER.scheduleWithFixedDelay(CookiePriceFetcher::refresh, 0, 60, TimeUnit.SECONDS);
    }
    public static void setActive(boolean value) {
        boolean entering = value && !active;
        active = value;
        if (entering) requestRefresh();
    }
    public static void requestRefresh() {
        if (started && active && !WORKER.isShutdown()) WORKER.execute(CookiePriceFetcher::refresh);
    }
    private static void refresh() {
        long now = System.currentTimeMillis();
        if (!active || now - lastAttempt < 30_000L) return;
        lastAttempt = now;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(ENDPOINT).toURL().openConnection();
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(8_000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "Coins-to-Money/1.1.0");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IOException("Bazaar HTTP " + connection.getResponseCode());
            try (InputStream input = connection.getInputStream()) {
                byte[] body = input.readNBytes(MAX_BODY_BYTES + 1);
                if (body.length > MAX_BODY_BYTES) throw new IOException("Bazaar response exceeds size limit");
                BazaarQuote quote = BazaarQuote.parse(new String(body, StandardCharsets.UTF_8), System.currentTimeMillis());
                State previous = state;
                if (previous.quote() != null && quote.updatedAt() < previous.quote().updatedAt()) throw new IOException("Bazaar timestamp moved backwards");
                state = new State(quote, null);
            }
        } catch (IOException | RuntimeException ex) {
            State previous = state;
            state = new State(previous.quote(), ex.getMessage());
            if (previous.error() == null) SkyblockUsdMod.LOGGER.warn("Cookie quote unavailable; retaining last quote with a stale label: {}", ex.toString());
        } finally { if (connection != null) connection.disconnect(); }
    }
    public static void stop() { active = false; WORKER.shutdownNow(); }
}

package com.example.skyblockusd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configFile() { return FabricLoader.getInstance().getConfigDir().resolve("coins-to-money.json"); }
    public static ModConfig INSTANCE = new ModConfig();
    public int schemaVersion = 3;
    public boolean enabled = true;
    public boolean enablePurse = true;
    public boolean enableTooltips = true;
    public boolean enableChat = true;
    public boolean showUsd = true;
    public boolean showCookies = false;
    public boolean keepCoins = false;
    public int decimalPlaces = 2;
    public int cookieDecimalPlaces = 3;

    public void normalize() {
        schemaVersion = 3;
        decimalPlaces = Math.clamp(decimalPlaces, 2, 8);
        cookieDecimalPlaces = Math.clamp(cookieDecimalPlaces, 1, 6);
        if (!showUsd && !showCookies && !keepCoins) showUsd = true;
    }

    public static void load() {
        Path FILE = configFile();
        if (!Files.isRegularFile(FILE)) return;
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
            if (loaded != null) INSTANCE = loaded;
            INSTANCE.normalize();
            // Legacy exchange-rate fields cannot masquerade as a current Bazaar quote.
        } catch (IOException | RuntimeException ex) {
            INSTANCE = new ModConfig();
            SkyblockUsdMod.LOGGER.warn("Cannot load Coins to Money config; using defaults", ex);
        }
    }
    public static void save() {
        Path FILE = configFile();
        INSTANCE.normalize();
        Path temporary = null;
        try {
            Files.createDirectories(FILE.getParent());
            temporary = Files.createTempFile(FILE.getParent(), "coins-to-money-", ".tmp");
            Files.writeString(temporary, GSON.toJson(INSTANCE), StandardCharsets.UTF_8);
            try { Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException | RuntimeException ex) { SkyblockUsdMod.LOGGER.warn("Cannot save Coins to Money config", ex); }
        finally { if (temporary != null) { try { Files.deleteIfExists(temporary); } catch (IOException ignored) { } } }
    }
}

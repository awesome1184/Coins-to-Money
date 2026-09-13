package com.example.skyblockusd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MIN_DECIMAL_PLACES = 2;
    private static final int MAX_DECIMAL_PLACES = 8;
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("coins-to-money.json");

    public static int decimalPlaces = 2;

    private ModConfig() {}

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }

        try {
            String json = Files.readString(PATH, StandardCharsets.UTF_8);
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            if (object.has("decimal_places")) {
                decimalPlaces = clamp(object.get("decimal_places").getAsInt());
            }
        } catch (Exception ignored) {
            decimalPlaces = 2;
            save();
        }
    }

    public static void save() {
        decimalPlaces = clamp(decimalPlaces);

        JsonObject object = new JsonObject();
        object.addProperty("decimal_places", decimalPlaces);

        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(object), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Configuration is optional; keep the in-memory value if saving fails.
        }
    }

    public static int clamp(int value) {
        return Math.max(MIN_DECIMAL_PLACES, Math.min(MAX_DECIMAL_PLACES, value));
    }

    public static int nextDecimalPlaces() {
        return decimalPlaces >= MAX_DECIMAL_PLACES ? MIN_DECIMAL_PLACES : decimalPlaces + 1;
    }
}

package com.example.skyblockusd;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.numbers.StyledFormat;
import java.util.Locale;

/** On-demand, clipboard-only diagnostics. No telemetry and no automatic player/chat logging. */
public final class SidebarDiagnostics {
    private static long hookCalls;
    private static long lastHookTime;
    private SidebarDiagnostics() { }
    public static void seen() { hookCalls++; lastHookTime = System.currentTimeMillis(); }
    public static String report(Minecraft client) {
        long now = System.currentTimeMillis();
        JsonObject report = new JsonObject();
        report.addProperty("version", "1.2.0");
        report.addProperty("inSkyblock", SkyblockUsdModClient.inSkyblock());
        report.addProperty("sidebarHookCalls", hookCalls);
        report.addProperty("lastHookMillisAgo", lastHookTime == 0 ? -1 : now - lastHookTime);
        report.addProperty("quoteAvailable", CookiePriceFetcher.state().available(now));
        report.addProperty("enabled", ModConfig.INSTANCE.enabled);
        report.addProperty("sidebarEnabled", ModConfig.INSTANCE.enablePurse);
        report.addProperty("showMoney", ModConfig.INSTANCE.showUsd);
        report.addProperty("showCookies", ModConfig.INSTANCE.showCookies);
        report.addProperty("keepCoins", ModConfig.INSTANCE.keepCoins);
        if (client.level == null || client.player == null) return report.toString();
        var board = client.level.getScoreboard();
        var objective = SkyblockContext.sidebar(board, client.player.getScoreboardName());
        if (objective == null) { report.addProperty("objective", "none"); return report.toString(); }
        report.addProperty("objective", objective.getDisplayName().getString());
        var fallback = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        JsonArray rows = new JsonArray();
        for (var entry : board.listPlayerScores(objective)) {
            if (entry.isHidden()) continue;
            var name = ScoreboardCoinHelper.rawName(board, entry);
            var value = entry.formatValue(fallback);
            String visible = CoinParser.plain(name.getString() + value.getString()).toLowerCase(Locale.ROOT);
            if (!(visible.contains("purse") || visible.contains("piggy") || visible.contains("bank") || visible.contains("balance") || visible.contains("coins"))) continue;
            JsonObject row = new JsonObject();
            row.addProperty("name", name.getString());
            row.addProperty("value", value.getString());
            row.addProperty("nameCodepoints", codepoints(name.getString()));
            row.addProperty("valueCodepoints", codepoints(value.getString()));
            row.addProperty("rawScore", entry.value());
            var format = entry.numberFormatOverride() == null ? fallback : entry.numberFormatOverride();
            row.addProperty("format", format.getClass().getSimpleName());
            var converted = ScoreboardCoinHelper.convertRow(name, value, entry, fallback);
            row.addProperty("convertedName", converted.name().getString());
            row.addProperty("convertedValue", converted.value().getString());
            rows.add(row);
            if (rows.size() == 15) break;
        }
        report.add("currencyRows", rows);
        return report.toString();
    }
    private static String codepoints(String text) {
        StringBuilder out = new StringBuilder();
        text.codePoints().limit(256).forEach(cp -> out.append(String.format(Locale.ROOT, "U+%04X ", cp)));
        return out.toString().stripTrailing();
    }
}

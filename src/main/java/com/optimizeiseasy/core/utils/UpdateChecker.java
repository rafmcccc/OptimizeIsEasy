package com.optimizeiseasy.core.utils;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class UpdateChecker {
    private final OptimizeIsEasyPlugin plugin;
    private String latest = null;
    private final HttpClient client = HttpClient.newHttpClient();

    public UpdateChecker(OptimizeIsEasyPlugin plugin) { this.plugin = plugin; }

    public void checkAsync() {
        if (!plugin.getConfig().getBoolean("main.updater", false)) return;
        String current = plugin.getDescription().getVersion();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/rafmcccc/OptimizeIsEasy/releases/latest"))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/vnd.github.v3+json")
                .GET().build();
        client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(res -> {
            try {
                if (res.statusCode() != 200) return;
                String body = res.body();
                // Gson ships with the Paper server (libraries/com/google/code/gson), so no
                // declared dependency: parsing beats indexOf on a field order we do not own.
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (!json.has("tag_name") || json.get("tag_name").isJsonNull()) return;
                latest = json.get("tag_name").getAsString().replaceFirst("^v", "");
                if (!latest.equals(current) && isNewer(latest, current)) {
                    String msg = "Update available " + current + " -> " + latest + " at https://github.com/rafmcccc/OptimizeIsEasy/releases";
                    plugin.getLogger().warning(msg);
                    for (Player p : Bukkit.getOnlinePlayers()) if (p.isOp()) p.sendMessage("§e[OptimizeIsEasy] §a" + msg);
                } else if (plugin.isDebug()) plugin.getLogger().fine("Up to date: " + current);
            } catch (Throwable t) { if (plugin.isDebug()) plugin.getLogger().fine("Update check failed: " + t.getMessage()); }
        }).exceptionally(ex -> { if (plugin.isDebug()) plugin.getLogger().fine("Update check error: " + ex.getMessage()); return null; });
    }

    public String getLatest() { return latest; }

    public static boolean isNewer(String latest, String current) {
        try {
            String[] la = latest.split("\\.");
            String[] ca = current.split("\\.");
            for (int i = 0; i < Math.max(la.length, ca.length); i++) {
                int lv = i < la.length ? Integer.parseInt(la[i].replaceAll("[^0-9]", "")) : 0;
                int cv = i < ca.length ? Integer.parseInt(ca[i].replaceAll("[^0-9]", "")) : 0;
                if (lv > cv) return true;
                if (lv < cv) return false;
            }
        } catch (Exception ignored) {}
        return false;
    }
}

package com.optimizeiseasy.core.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    public static void loadConfig(Plugin plugin, FileConfiguration cfg, String path) throws Exception {
        File file = new File(plugin.getDataFolder(), path);
        // Ensure parent dirs
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        // Migration and backup: if file exists and config-version mismatched, backup
        if (file.exists()) {
            YamlConfiguration cur = YamlConfiguration.loadConfiguration(file);
            int curVer = cur.getInt("config-version", 1);
            int defVer = 1;
            try (InputStream din = plugin.getResource(path)) {
                if (din != null) {
                    YamlConfiguration def = YamlConfiguration.loadConfiguration(new InputStreamReader(din, StandardCharsets.UTF_8));
                    defVer = def.getInt("config-version", 1);
                }
            } catch (Exception ignored) {}
            if (curVer < defVer) {
                new BackupManager(plugin).backup(file);
                plugin.getLogger().warning("Migrated " + path + " from v" + curVer + " to v" + defVer);
            }
        }

        // If file doesn't exist, copy from resource
        if (!file.exists()) {
            try (InputStream in = plugin.getResource(path)) {
                if (in != null) {
                    YamlConfiguration def = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
                    def.save(file);
                }
            }
        }

        // Load file into cfg
        if (file.exists()) {
            cfg.load(file);
            // Merge defaults without overwriting user values if config_formatter true
            boolean formatter = plugin.getConfig().getBoolean("main.config_formatter", true);
            if (formatter) {
                try (InputStream defStream = plugin.getResource(path)) {
                    if (defStream != null) {
                        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
                        boolean changed = false;
                        for (String key : defConfig.getKeys(true)) {
                            if (!cfg.contains(key)) {
                                cfg.set(key, defConfig.get(key));
                                changed = true;
                            }
                        }
                        if (changed) cfg.save(file);
                    }
                }
            }
        }
    }
}

package com.optimizeiseasy.core.utils;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BadPluginDetector {
    private static final Set<String> KNOWN_BAD = Set.of(
            "ClearLag", "EntityClearer", "StackMob", "PluginManager+", "PluginManager",
            "AutoPluginLoader", "PlugMan", "PlugManX", "WildStacker", "FarmLimiter",
            "ChunkSpawnerLimiter", "AntiLagX", "WildTools", "AutoClear"
    );

    private final OptimizeIsEasyPlugin plugin;

    public BadPluginDetector(OptimizeIsEasyPlugin plugin) {
        this.plugin = plugin;
    }

    public List<String> findBadPlugins() {
        List<String> found = new ArrayList<>();
        try {
            for (Plugin p : plugin.getServer().getPluginManager().getPlugins()) {
                for (String bad : KNOWN_BAD) {
                    if (p.getName().equalsIgnoreCase(bad)) {
                        found.add(p.getName());
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return found;
    }

    public void warnIfBad() {
        for (String bad : findBadPlugins()) {
            plugin.getLogger().warning("Using known lag-causing/conflicting plugin: " + bad
                    + " - it may cause more lag than it resolves or conflict with OptimizeIsEasy. Consider removing it.");
        }
    }
}

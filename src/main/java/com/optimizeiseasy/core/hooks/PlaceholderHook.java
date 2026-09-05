package com.optimizeiseasy.core.hooks;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.modules.HibernateModule;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderHook extends PlaceholderExpansion {
    private final OptimizeIsEasyPlugin plugin;

    public PlaceholderHook(OptimizeIsEasyPlugin plugin) { this.plugin = plugin; }

    @Override public @NotNull String getIdentifier() { return "optimizeiseasy"; }
    @Override public @NotNull String getAuthor() { return "rafmcccc"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        String p = params.toLowerCase();
        try {
            switch (p) {
                case "tps": return String.format("%.2f", Bukkit.getTPS()[0]);
                case "mspt": {
                    try { return String.format("%.2f", Bukkit.getAverageTickTime()); } catch (Throwable t) { return "0"; }
                }
                case "frozen": {
                    HibernateModule hm = plugin.getModuleManager().get(HibernateModule.class);
                    return hm != null && hm.isFrozen() ? "true" : "false";
                }
                case "entities": {
                    long c = 0; for (World w : Bukkit.getWorlds()) c += w.getEntities().size();
                    return String.valueOf(c);
                }
                case "modules_enabled": {
                    long c = plugin.getModuleManager().getModules().stream().filter(m -> m.isLoaded()).count();
                    return String.valueOf(c);
                }
                case "modules_total": return String.valueOf(plugin.getModuleManager().getModules().size());
                default: return null;
            }
        } catch (Throwable t) { return null; }
    }
}

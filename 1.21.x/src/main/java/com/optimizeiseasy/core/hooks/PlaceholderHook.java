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
                case "tps_1m": return String.format("%.2f", Bukkit.getTPS()[0]);
                case "tps_5m": return String.format("%.2f", Bukkit.getTPS()[1]);
                case "tps_15m": return String.format("%.2f", Bukkit.getTPS()[2]);
                case "mspt": {
                    try { return String.format("%.2f", Bukkit.getAverageTickTime()); } catch (Throwable t) { return "0"; }
                }
                case "frozen": {
                    HibernateModule hm = plugin.getModuleManager().get(HibernateModule.class);
                    return hm != null && hm.isFrozen() ? "true" : "false";
                }
                case "frozen_status": {
                    HibernateModule hm = plugin.getModuleManager().get(HibernateModule.class);
                    return hm != null && hm.isFrozen() ? "frozen" : "running";
                }
                case "entities": {
                    long c = 0; for (World w : Bukkit.getWorlds()) c += w.getEntities().size();
                    return String.valueOf(c);
                }
                case "chunks_loaded": {
                    int c = 0; for (World w : Bukkit.getWorlds()) c += w.getLoadedChunks().length;
                    return String.valueOf(c);
                }
                case "worlds_total": return String.valueOf(Bukkit.getWorlds().size());
                case "modules_enabled": {
                    long c = plugin.getModuleManager().getModules().stream().filter(m -> m.isLoaded()).count();
                    return String.valueOf(c);
                }
                case "modules_total": return String.valueOf(plugin.getModuleManager().getModules().size());
                case "modules_disabled": {
                    long e = plugin.getModuleManager().getModules().stream().filter(m -> m.isLoaded()).count();
                    return String.valueOf(plugin.getModuleManager().getModules().size() - e);
                }
                case "version": return plugin.getDescription().getVersion();
                case "is_folia": {
                    try { Class.forName("io.papermc.paper.threadedregions.RegionizedServer"); return "true"; } catch (Throwable t) { return "false"; }
                }
                case "server_fork": {
                    try { Class.forName("org.purpurmc.purpur.PurpurConfig"); return "Purpur"; } catch (Throwable t) {}
                    try { Class.forName("io.papermc.paper.threadedregions.RegionizedServer"); return "Folia"; } catch (Throwable t) {}
                    return Bukkit.getName().contains("Paper") ? "Paper" : Bukkit.getName();
                }
                case "debug": return String.valueOf(plugin.isDebug());
                default: {
                    if (p.startsWith("module_") && p.endsWith("_enabled")) {
                        String name = p.substring(7, p.length() - 8);
                        var m = plugin.getModuleManager().get(name);
                        return m != null ? String.valueOf(m.isLoaded()) : "false";
                    }
                    return null;
                }
            }
        } catch (Throwable t) { return null; }
    }
}

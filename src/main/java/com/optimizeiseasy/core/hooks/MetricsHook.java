package com.optimizeiseasy.core.hooks;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

public class MetricsHook {
    private final Metrics metrics;

    public MetricsHook(OptimizeIsEasyPlugin plugin) {
        Metrics m = null;
        try {
            m = new Metrics(plugin, 27015);
            m.addCustomChart(new SimplePie("hibernation_enabled", () -> plugin.getConfig().getBoolean("hibernate.gc-on-freeze") ? "enabled" : "disabled"));
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                m.addCustomChart(new SimplePie("folia", () -> "true"));
            } catch (Throwable t) {
                m.addCustomChart(new SimplePie("folia", () -> "false"));
            }
            // Modules breakdown
            m.addCustomChart(new AdvancedPie("modules", () -> {
                Map<String, Integer> map = new HashMap<>();
                plugin.getModuleManager().getModules().forEach(mod -> map.put(mod.getName(), mod.isLoaded() ? 1 : 0));
                return map;
            }));
            m.addCustomChart(new SimplePie("server_fork", () -> {
                try { Class.forName("org.purpurmc.purpur.PurpurConfig"); return "Purpur"; } catch (Throwable t) {}
                try { Class.forName("io.papermc.paper.threadedregions.RegionizedServer"); return "Folia"; } catch (Throwable t) {}
                return Bukkit.getName().contains("Paper") ? "Paper" : Bukkit.getName();
            }));
            m.addCustomChart(new SimplePie("java_version", () -> System.getProperty("java.version").split("\\.")[0]));
            m.addCustomChart(new SingleLineChart("entities", () -> {
                int c = 0; try { for (org.bukkit.World w : Bukkit.getWorlds()) c += w.getEntities().size(); } catch (Throwable ignored) {}
                return c;
            }));
            m.addCustomChart(new SingleLineChart("players", () -> Bukkit.getOnlinePlayers().size()));
        } catch (Throwable t) {
            plugin.getLogger().fine("bStats not available: " + t.getMessage());
        }
        this.metrics = m;
    }

    public void shutdown() {
        if (metrics != null) try { metrics.shutdown(); } catch (Throwable ignored) {}
    }
}

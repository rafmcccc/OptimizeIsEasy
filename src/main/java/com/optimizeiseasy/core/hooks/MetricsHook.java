package com.optimizeiseasy.core.hooks;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;

public class MetricsHook {
    private final Metrics metrics;

    public MetricsHook(OptimizeIsEasyPlugin plugin) {
        Metrics m = null;
        try {
            m = new Metrics(plugin, 27015);
            // Example chart: hibernation enabled
            m.addCustomChart(new SimplePie("hibernation_enabled", () -> plugin.getConfig().getBoolean("hibernate.gc-on-freeze") ? "enabled" : "disabled"));
            // Folia detection chart
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                m.addCustomChart(new SimplePie("folia", () -> "true"));
            } catch (Throwable t) {
                m.addCustomChart(new SimplePie("folia", () -> "false"));
            }
        } catch (Throwable t) {
            plugin.getLogger().fine("bStats not available: " + t.getMessage());
        }
        this.metrics = m;
    }

    public void shutdown() {
        if (metrics != null) try { metrics.shutdown(); } catch (Throwable ignored) {}
    }
}

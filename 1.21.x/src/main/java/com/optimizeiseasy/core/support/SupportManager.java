package com.optimizeiseasy.core.support;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import org.bukkit.Bukkit;

import java.util.Comparator;
import java.util.stream.Stream;

public class SupportManager {
    private static SupportManager instance;
    private AbstractFork fork;

    public SupportManager(OptimizeIsEasyPlugin plugin) {
        instance = this;
        fork = Stream.of(new SpigotSupport(plugin), new PaperSupport(plugin))
                .filter(AbstractFork::isSupported)
                .max(Comparator.comparingInt(AbstractFork::getPriority))
                .orElse(new SpigotSupport(plugin));
        if (plugin.isDebug()) plugin.getLogger().fine("Support fork: " + fork.getClass().getSimpleName());
    }

    public static SupportManager getInstance() { return instance; }
    public AbstractFork getFork() { return fork; }

    public double getMspt() { return fork.getMspt(); }
    public boolean isSupportMspt() { return fork.isSupportMspt(); }
    public double getTps() { return fork.getTps(); }
    public boolean isTpsReliable() { return fork.isTpsReliable(); }
    public boolean isFolia() { return fork instanceof PaperSupport && fork.isSupported() && isFoliaDetected(); }

    /**
     * TPS without throwing, even before enable or without a fork.
     * Falls back to Bukkit.getTPS() and finally to 20.0.
     */
    public static double currentTps() {
        try {
            SupportManager sm = getInstance();
            if (sm != null && sm.getFork() != null) return sm.getTps();
        } catch (Throwable ignored) {
            // Fall through to Bukkit below.
        }
        try {
            return Bukkit.getTPS()[0];
        } catch (Throwable ignored) {
            return 20.0;
        }
    }

    /** 1m/5m/15m TPS without throwing. Falls back to {20, 20, 20}. */
    public static double[] currentTpsArray() {
        try {
            SupportManager sm = getInstance();
            if (sm != null && sm.getFork() != null && sm.isTpsReliable()) {
                double tps = sm.getTps();
                return new double[]{tps, tps, tps};
            }
        } catch (Throwable ignored) {
            // Fall through to Bukkit below.
        }
        try {
            return Bukkit.getTPS();
        } catch (Throwable ignored) {
            return new double[]{20.0, 20.0, 20.0};
        }
    }

    /** False when throttling decisions would be based on a fake 20.0 reading. */
    public static boolean isTpsReliableNow() {
        try {
            SupportManager sm = getInstance();
            if (sm != null && sm.getFork() != null) return sm.isTpsReliable();
        } catch (Throwable ignored) {
            // Fall through to Bukkit below.
        }
        try {
            Bukkit.getTPS();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isFoliaDetected() {
        try { Class.forName("io.papermc.paper.threadedregions.RegionizedServer"); return true; } catch (Throwable t) { return false; }
    }
}

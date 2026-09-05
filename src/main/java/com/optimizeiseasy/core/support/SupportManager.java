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
    public boolean isFolia() { return fork instanceof PaperSupport && fork.isSupported() && isFoliaDetected(); }

    private boolean isFoliaDetected() {
        try { Class.forName("io.papermc.paper.threadedregions.RegionizedServer"); return true; } catch (Throwable t) { return false; }
    }
}

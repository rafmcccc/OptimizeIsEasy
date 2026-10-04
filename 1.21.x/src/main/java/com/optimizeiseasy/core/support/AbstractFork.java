package com.optimizeiseasy.core.support;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

public abstract class AbstractFork {
    protected final Plugin plugin;

    public AbstractFork(Plugin plugin) { this.plugin = plugin; }

    public boolean checkTask(long delay) { return !plugin.isEnabled() || delay <= 0L; }
    public boolean checkTask(long initial, long delay) { return !plugin.isEnabled() || initial <= 0L || delay <= 0L; }

    public abstract boolean isSupported();
    public abstract int getPriority();
    public abstract double getMspt();
    public abstract boolean isSupportMspt();

    public abstract BukkitTask runNow(boolean async, Location loc, Runnable runnable);
    public abstract BukkitTask runLater(boolean async, Runnable runnable, long delay, TimeUnit unit);
    public abstract BukkitTask runTimer(boolean async, Runnable runnable, long initialDelay, long delay, TimeUnit unit);

    /**
     * Current 1-minute TPS. Falls back to an MSPT-derived estimate and
     * finally to 20.0 when the server exposes neither.
     */
    public abstract double getTps();

    /** False when no usable TPS source exists (e.g. Folia without tick times). */
    public abstract boolean isTpsReliable();

    /** Clamp an MSPT-derived TPS estimate into (0, 20]. Returns 20.0 when mspt is unusable. */
    protected static double tpsFromMspt(double mspt) {
        if (mspt <= 0 || Double.isNaN(mspt) || Double.isInfinite(mspt)) return 20.0;
        return Math.min(20.0, 1000.0 / mspt);
    }
}

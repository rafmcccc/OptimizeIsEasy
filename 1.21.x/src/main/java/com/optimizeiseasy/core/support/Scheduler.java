package com.optimizeiseasy.core.support;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Single place for "run via the fork when present, Bukkit scheduler otherwise".
 * Replaces the copy-pasted {@code if (sm != null) ... else Bukkit.getScheduler()}
 * block in every module and GUI, and keeps Folia off the legacy scheduler.
 */
public final class Scheduler {
    private Scheduler() {}

    public static BukkitTask runNow(Plugin plugin, boolean async, @Nullable Location loc, Runnable runnable) {
        SupportManager sm = SupportManager.getInstance();
        if (sm != null && sm.getFork() != null) {
            try {
                return sm.getFork().runNow(async, loc, runnable);
            } catch (Throwable t) {
                plugin.getLogger().fine("Scheduler.runNow via fork failed, falling back: " + t.getMessage());
            }
        }
        try {
            if (async) return Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            return Bukkit.getScheduler().runTask(plugin, runnable);
        } catch (Throwable t) {
            plugin.getLogger().fine("Scheduler.runNow fallback failed: " + t.getMessage());
            return null;
        }
    }

    public static BukkitTask runLater(Plugin plugin, boolean async, Runnable runnable, long delay, TimeUnit unit) {
        SupportManager sm = SupportManager.getInstance();
        if (sm != null && sm.getFork() != null) {
            try {
                return sm.getFork().runLater(async, runnable, delay, unit);
            } catch (Throwable t) {
                plugin.getLogger().fine("Scheduler.runLater via fork failed, falling back: " + t.getMessage());
            }
        }
        try {
            long ticks = unit.toMillis(delay) / 50;
            if (async) return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, ticks);
            return Bukkit.getScheduler().runTaskLater(plugin, runnable, ticks);
        } catch (Throwable t) {
            plugin.getLogger().fine("Scheduler.runLater fallback failed: " + t.getMessage());
            return null;
        }
    }

    public static BukkitTask runTimer(Plugin plugin, boolean async, Runnable runnable, long initialDelay, long delay, TimeUnit unit) {
        SupportManager sm = SupportManager.getInstance();
        if (sm != null && sm.getFork() != null) {
            try {
                return sm.getFork().runTimer(async, runnable, initialDelay, delay, unit);
            } catch (Throwable t) {
                plugin.getLogger().fine("Scheduler.runTimer via fork failed, falling back: " + t.getMessage());
            }
        }
        try {
            long iTicks = unit.toMillis(initialDelay) / 50;
            long dTicks = unit.toMillis(delay) / 50;
            if (async) return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, iTicks, dTicks);
            return Bukkit.getScheduler().runTaskTimer(plugin, runnable, iTicks, dTicks);
        } catch (Throwable t) {
            plugin.getLogger().fine("Scheduler.runTimer fallback failed: " + t.getMessage());
            return null;
        }
    }

    /** Sync delayed task measured in ticks (e.g. GUI refreshes). */
    public static BukkitTask runLaterTicks(Plugin plugin, Runnable runnable, long ticks) {
        return runLater(plugin, false, runnable, ticks * 50, TimeUnit.MILLISECONDS);
    }

    /** Sync repeating task measured in ticks. */
    public static BukkitTask runTimerTicks(Plugin plugin, Runnable runnable, long initialTicks, long periodTicks) {
        return runTimer(plugin, false, runnable, initialTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS);
    }
}

package com.optimizeiseasy.core.support;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

public class SpigotSupport extends AbstractFork {
    public SpigotSupport(Plugin plugin) { super(plugin); }
    @Override public boolean isSupported() { return true; }
    @Override public int getPriority() { return -1; }
    @Override public double getMspt() { return 0; }
    @Override public boolean isSupportMspt() { return false; }

    @Override
    public BukkitTask runNow(boolean async, @Nullable Location loc, Runnable run) {
        if (!plugin.isEnabled()) return null;
        return async ? Bukkit.getScheduler().runTaskAsynchronously(plugin, run) : Bukkit.getScheduler().runTask(plugin, run);
    }

    @Override
    public BukkitTask runLater(boolean async, Runnable run, long delay, TimeUnit unit) {
        if (checkTask(delay)) return null;
        long ticks = unit.toMillis(delay) / 50;
        return async ? Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, run, ticks) : Bukkit.getScheduler().runTaskLater(plugin, run, ticks);
    }

    @Override
    public BukkitTask runTimer(boolean async, Runnable run, long initialDelay, long delay, TimeUnit unit) {
        if (checkTask(initialDelay, delay)) return null;
        long iTicks = unit.toMillis(initialDelay) / 50;
        long dTicks = unit.toMillis(delay) / 50;
        return async ? Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, run, iTicks, dTicks) : Bukkit.getScheduler().runTaskTimer(plugin, run, iTicks, dTicks);
    }
}

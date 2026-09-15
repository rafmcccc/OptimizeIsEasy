package com.optimizeiseasy.core.support;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

public class PaperSupport extends AbstractFork {
    public PaperSupport(Plugin plugin) { super(plugin); }

    @Override
    public boolean isSupported() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            Bukkit.getAsyncScheduler();
            Bukkit.getGlobalRegionScheduler();
            return true;
        } catch (Throwable ex) { return false; }
    }

    @Override public int getPriority() { return 1; }
    @Override public double getMspt() { try { return Bukkit.getAverageTickTime(); } catch (Throwable t) { return 0; } }
    @Override public boolean isSupportMspt() { return true; }

    @Override
    public BukkitTask runNow(boolean async, @Nullable Location loc, Runnable run) {
        if (!plugin.isEnabled()) return new FoliaTask(plugin, null);
        ScheduledTask task;
        if (async) task = Bukkit.getAsyncScheduler().runNow(plugin, s -> run.run());
        else {
            if (loc == null) task = Bukkit.getGlobalRegionScheduler().run(plugin, s -> run.run());
            else task = Bukkit.getRegionScheduler().run(plugin, loc, s -> run.run());
        }
        return new FoliaTask(plugin, task);
    }

    @Override
    public BukkitTask runLater(boolean async, Runnable run, long delay, TimeUnit unit) {
        if (checkTask(delay)) return new FoliaTask(plugin, null);
        return new FoliaTask(plugin, async ? Bukkit.getAsyncScheduler().runDelayed(plugin, s -> run.run(), delay, unit) : Bukkit.getGlobalRegionScheduler().runDelayed(plugin, s -> run.run(), unit.toMillis(delay) / 50));
    }

    @Override
    public BukkitTask runTimer(boolean async, Runnable run, long initialDelay, long delay, TimeUnit unit) {
        if (checkTask(initialDelay, delay)) return new FoliaTask(plugin, null);
        return new FoliaTask(plugin, async ? Bukkit.getAsyncScheduler().runAtFixedRate(plugin, s -> run.run(), initialDelay, delay, unit) : Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, s -> run.run(), unit.toMillis(initialDelay) / 50, unit.toMillis(delay) / 50));
    }

    public static class FoliaTask implements BukkitTask {
        private final @Nullable ScheduledTask task;
        private final Plugin plugin;
        public FoliaTask(Plugin plugin, @Nullable ScheduledTask task) { this.plugin = plugin; this.task = task; }
        @Override public int getTaskId() { return task == null ? -1 : task.hashCode(); }
        @Override public @NotNull Plugin getOwner() { return plugin; }
        @Override public boolean isSync() { return false; }
        @Override public boolean isCancelled() { return task == null || task.isCancelled(); }
        @Override public void cancel() { if (task != null && !task.isCancelled()) task.cancel(); }
    }
}

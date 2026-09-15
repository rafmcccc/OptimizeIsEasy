package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.SupportManager;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.ServerTickManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

/**
 * Handles ServerTickManager freezing when no players online.
 * Ported from original tick-freeze logic.
 */
public class HibernateModule extends AbstractModule implements Listener {
    private ServerTickManager tickManager;
    private Server server;
    private boolean gcOnFreeze;
    private int checkIntervalSeconds;
    private String frozenOnEmptyMessage;
    private BukkitTask checkTask;

    public HibernateModule(OptimizeIsEasyPlugin plugin) {
        super(plugin, "Hibernate");
    }

    @Override
    public void loadConfigSection() {
        // Hibernate uses main config.yml under hibernate.* not a module file
        // Override to avoid requiring modules/Hibernate.yml
        worlds.clear();
        // canContinue defaults to always true
    }

    @Override
    public boolean loadConfig() {
        // Read from main config.yml under hibernate.*
        gcOnFreeze = plugin.getConfig().getBoolean("hibernate.gc-on-freeze", true);
        checkIntervalSeconds = Math.max(0, plugin.getConfig().getInt("hibernate.check-interval-seconds", 60));
        frozenOnEmptyMessage = plugin.getConfig().getString("hibernate.messages.frozen-on-empty", "Last player disconnected. Server is now frozen.");
        // Also allow modules/Hibernate.yml if present (for compatibility)
        if (getConfig().contains("Hibernate.enabled")) {
            // no-op
        }
        return true;
    }

    @Override
    public void load() throws Exception {
        server = Bukkit.getServer();
        try {
            tickManager = server.getServerTickManager();
        } catch (Throwable t) {
            plugin.getLogger().warning("ServerTickManager not available (requires Paper 1.20.6+): " + t.getMessage() + " - Hibernate disabled");
            tickManager = null;
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Periodic re-freeze check - use fork for Folia GlobalRegionScheduler support
        if (checkIntervalSeconds > 0) {
            SupportManager sm = SupportManager.getInstance();
            if (sm != null) {
                checkTask = sm.getFork().runTimer(false, () -> {
                    if (server.getOnlinePlayers().isEmpty() && tickManager != null && !tickManager.isFrozen()) {
                        freeze("No players online and server was not frozen. Freezing now.");
                    }
                }, checkIntervalSeconds, checkIntervalSeconds, TimeUnit.SECONDS);
            } else {
                long ticks = checkIntervalSeconds * 20L;
                checkTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    if (server.getOnlinePlayers().isEmpty() && tickManager != null && !tickManager.isFrozen()) {
                        freeze("No players online and server was not frozen. Freezing now.");
                    }
                }, ticks, ticks);
            }
        }

        // Initial freeze if empty - use GlobalRegionScheduler on Folia
        if (server.getOnlinePlayers().isEmpty() && tickManager != null && !tickManager.isFrozen()) {
            SupportManager sm = SupportManager.getInstance();
            Runnable r = () -> {
                if (server.getOnlinePlayers().isEmpty() && tickManager != null && !tickManager.isFrozen()) {
                    freeze("Server is frozen until a player joins.");
                }
            };
            if (sm != null && sm.getFork() != null) {
                sm.getFork().runNow(false, null, r);
            } else {
                Bukkit.getScheduler().runTask(plugin, r);
            }
        }

        if (plugin.isDebug()) {
            plugin.getLogger().fine("Hibernate module loaded, frozen=" + (tickManager != null && tickManager.isFrozen()));
        }
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }

    public void freeze(String message) {
        if (tickManager == null) return;
        try {
            tickManager.setFrozen(true);
        } catch (Throwable t) {
            plugin.getLogger().fine("Freeze failed: " + t.getMessage());
            return;
        }
        if (plugin.isDebug()) {
            plugin.getLogger().fine(message);
        } else {
            // Spec: keep warnings/errors visible, but move per-event to fine.
            // Freeze is infrequent - log at fine when debug false to cut spam.
            // However we keep one fine, not info.
            plugin.getLogger().fine(message);
        }
        if (gcOnFreeze) {
            try {
                System.gc();
            } catch (Throwable ignored) {}
        }
    }

    public void unfreeze(String message) {
        if (tickManager == null) return;
        try {
            tickManager.setFrozen(false);
        } catch (Throwable t) {
            plugin.getLogger().fine("Unfreeze failed: " + t.getMessage());
            return;
        }
        if (plugin.isDebug()) plugin.getLogger().fine(message);
        else plugin.getLogger().fine(message);
    }

    public boolean isFrozen() {
        return tickManager != null && tickManager.isFrozen();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // After this quit, count will be 0 if size ==1 before quit
        if (server.getOnlinePlayers().size() == 1) {
            freeze(frozenOnEmptyMessage);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (tickManager != null && tickManager.isFrozen()) {
            unfreeze("A player joined. Server is now unfrozen.");
        }
    }

    @EventHandler
    public void onStartComplete(ServerLoadEvent event) {
        if (event.getType() == ServerLoadEvent.LoadType.RELOAD) return;
        if (server.getOnlinePlayers().isEmpty() && tickManager != null && !tickManager.isFrozen()) {
            freeze("Server is frozen until a player joins.");
        }
    }
}

package com.optimizeiseasy.core.listeners;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class RestartAlertListener implements Listener {
    private final OptimizeIsEasyPlugin plugin;

    public RestartAlertListener(OptimizeIsEasyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!e.getPlayer().isOp()) return;
        try {
            if (plugin.isRestartRequired()) {
                e.getPlayer().sendMessage("§e[OptimizeIsEasy] §cRestart required - server-file patches (KOS/EDB) will not apply until restart.");
            }
        } catch (Throwable ignored) {}
    }
}

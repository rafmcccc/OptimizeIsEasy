package com.optimizeiseasy.core;

import com.optimizeiseasy.core.commands.ExploitFixCommand;
import com.optimizeiseasy.core.commands.OptimizeCommand;
import com.optimizeiseasy.core.gui.OptimizeGui;
import com.optimizeiseasy.core.hooks.MetricsHook;
import com.optimizeiseasy.core.hooks.PlaceholderHook;
import com.optimizeiseasy.core.managers.ModuleManager;
import com.optimizeiseasy.core.support.SupportManager;
import com.optimizeiseasy.core.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class OptimizeIsEasyPlugin extends JavaPlugin {
    private static OptimizeIsEasyPlugin instance;
    private ModuleManager moduleManager;
    private MetricsHook metricsHook;
    private ExploitFixCommand exploitFixCommand;
    private OptimizeGui gui;
    private UpdateChecker updateChecker;
    private boolean debug;

    public static OptimizeIsEasyPlugin getInstance() { return instance; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public OptimizeGui getGui() { return gui; }
    public UpdateChecker getUpdateChecker() { return updateChecker; }
    public boolean isDebug() { return debug; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        debug = getConfig().getBoolean("debug", false);
        if (debug) {
            getLogger().setLevel(Level.FINE);
            getLogger().fine("Debug enabled");
        } else {
            getLogger().setLevel(Level.INFO);
        }
        // Fork detection for Paper/Purpur/Folia support (1.21+)
        new SupportManager(this);

        moduleManager = new ModuleManager(this);
        moduleManager.loadAll();

        // Register commands - only two allowed
        if (getCommand("optimize") != null) {
            OptimizeCommand cmd = new OptimizeCommand(this);
            getCommand("optimize").setExecutor(cmd);
            getCommand("optimize").setTabCompleter(cmd);
        }
        if (getCommand("exploitfix") != null) {
            exploitFixCommand = new ExploitFixCommand(this);
            getCommand("exploitfix").setExecutor(exploitFixCommand);
            getCommand("exploitfix").setTabCompleter(exploitFixCommand);
        }

        // Soft hooks - no hard dep, safe if missing
        if (getConfig().getBoolean("main.bStats", true)) {
            try { metricsHook = new MetricsHook(this); } catch (Throwable t) { getLogger().fine("Metrics init failed: " + t.getMessage()); }
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try { new PlaceholderHook(this).register(); getLogger().fine("PlaceholderAPI hook registered"); } catch (Throwable t) { getLogger().fine("PAPI hook failed: " + t.getMessage()); }
        }
        // GUI and update checker
        try { gui = new OptimizeGui(this); } catch (Throwable t) { getLogger().fine("GUI init failed: " + t.getMessage()); }
        try {
            updateChecker = new UpdateChecker(this);
            updateChecker.checkAsync();
            // Notify ops on join if update available
            Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
                @org.bukkit.event.EventHandler
                public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
                    if (e.getPlayer().isOp() && updateChecker.getLatest() != null) {
                        String cur = getDescription().getVersion();
                        String lat = updateChecker.getLatest();
                        if (!cur.equals(lat)) e.getPlayer().sendMessage("§e[OptimizeIsEasy] §aUpdate available " + cur + " -> " + lat);
                    }
                }
            }, this);
        } catch (Throwable t) { getLogger().fine("Update checker init failed: " + t.getMessage()); }

        getLogger().info("OptimizeIsEasy enabled");
    }

    @Override
    public void onDisable() {
        if (exploitFixCommand != null) try { exploitFixCommand.saveFrozen(); } catch (Throwable ignored) {}
        if (moduleManager != null) moduleManager.disableAll();
        if (metricsHook != null) try { metricsHook.shutdown(); } catch (Throwable ignored) {}
        getLogger().info("OptimizeIsEasy disabled");
    }

    public void reload() {
        // Validate main config before applying
        java.util.List<String> mainErrs = com.optimizeiseasy.core.managers.ConfigValidator.validateMainConfig(getConfig());
        for (String e : mainErrs) getLogger().warning("Config validator main: " + e);
        reloadConfig();
        debug = getConfig().getBoolean("debug", false);
        getLogger().setLevel(debug ? Level.FINE : Level.INFO);
        if (moduleManager != null) {
            // Save frozen before reload
            if (exploitFixCommand != null) try { exploitFixCommand.saveFrozen(); } catch (Throwable ignored) {}
            moduleManager.disableAll();
            moduleManager.loadAll();
            if (exploitFixCommand != null) try { exploitFixCommand.loadFrozen(); } catch (Throwable ignored) {}
        }
    }
}

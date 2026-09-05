package com.optimizeiseasy.core;

import com.optimizeiseasy.core.commands.ExploitFixCommand;
import com.optimizeiseasy.core.commands.OptimizeCommand;
import com.optimizeiseasy.core.managers.ModuleManager;
import com.optimizeiseasy.core.support.SupportManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class OptimizeIsEasyPlugin extends JavaPlugin {
    private static OptimizeIsEasyPlugin instance;
    private ModuleManager moduleManager;
    private boolean debug;

    public static OptimizeIsEasyPlugin getInstance() { return instance; }
    public ModuleManager getModuleManager() { return moduleManager; }
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
            ExploitFixCommand cmd = new ExploitFixCommand(this);
            getCommand("exploitfix").setExecutor(cmd);
            getCommand("exploitfix").setTabCompleter(cmd);
        }

        getLogger().info("OptimizeIsEasy enabled");
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) moduleManager.disableAll();
        getLogger().info("OptimizeIsEasy disabled");
    }

    public void reload() {
        reloadConfig();
        debug = getConfig().getBoolean("debug", false);
        getLogger().setLevel(debug ? Level.FINE : Level.INFO);
        if (moduleManager != null) {
            moduleManager.disableAll();
            moduleManager.loadAll();
        }
    }
}

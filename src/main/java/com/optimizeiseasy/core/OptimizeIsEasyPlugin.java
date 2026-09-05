package com.optimizeiseasy.core;

import com.optimizeiseasy.api.OptimizeIsEasyAPI;
import com.optimizeiseasy.api.event.HibernateFreezeEvent;
import com.optimizeiseasy.api.event.HibernateUnfreezeEvent;
import com.optimizeiseasy.core.commands.ExploitFixCommand;
import com.optimizeiseasy.core.commands.OptimizeCommand;
import com.optimizeiseasy.core.gui.OptimizeGui;
import com.optimizeiseasy.core.hooks.PlaceholderHook;
import com.optimizeiseasy.core.managers.ModuleManager;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.SupportManager;
import com.optimizeiseasy.core.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.logging.Level;

public final class OptimizeIsEasyPlugin extends JavaPlugin implements OptimizeIsEasyAPI {
    private static OptimizeIsEasyPlugin instance;
    private ModuleManager moduleManager;
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
        // Register API
        try { Bukkit.getServicesManager().register(OptimizeIsEasyAPI.class, this, this, ServicePriority.Normal); } catch (Throwable t) { getLogger().fine("API register failed: " + t.getMessage()); }

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

    // API implementation
    @Override public Collection<AbstractModule> getModules() { return moduleManager != null ? moduleManager.getModules() : java.util.Collections.emptyList(); }
    @Override public AbstractModule getModule(String name) { return moduleManager != null ? moduleManager.get(name) : null; }
    @Override public boolean isModuleEnabled(String name) { var m = getModule(name); return m != null && m.isLoaded(); }
    @Override public boolean toggleModule(String name) {
        var m = getModule(name);
        if (m == null) return false;
        try {
            if (m.isLoaded()) { m.disable(); m.setLoaded(false); m.getConfig().set(m.getName() + ".enabled", false); m.getConfig().save(new java.io.File(getDataFolder(), "modules/" + m.getName() + ".yml")); }
            else { m.loadConfigSection(); if (m.loadConfig()) { m.load(); m.setLoaded(true); m.getConfig().set(m.getName() + ".enabled", true); m.getConfig().save(new java.io.File(getDataFolder(), "modules/" + m.getName() + ".yml")); } }
            Bukkit.getPluginManager().callEvent(new com.optimizeiseasy.api.event.ModuleEnableEvent(m));
            return true;
        } catch (Exception e) { getLogger().warning("API toggle failed for " + name + ": " + e.getMessage()); return false; }
    }
    @Override public boolean isFrozen() { var hm = moduleManager != null ? moduleManager.get(com.optimizeiseasy.core.modules.HibernateModule.class) : null; return hm != null && hm.isFrozen(); }
    @Override public void setFrozen(boolean frozen) {
        var hm = moduleManager != null ? moduleManager.get(com.optimizeiseasy.core.modules.HibernateModule.class) : null;
        if (hm == null) return;
        if (frozen) {
            var ev = new HibernateFreezeEvent("API");
            Bukkit.getPluginManager().callEvent(ev);
            if (!ev.isCancelled()) hm.freeze("API freeze");
        } else {
            var ev = new HibernateUnfreezeEvent("API");
            Bukkit.getPluginManager().callEvent(ev);
            if (!ev.isCancelled()) hm.unfreeze("API unfreeze");
        }
    }
    @Override public boolean isFoliaSupported() { var sm = SupportManager.getInstance(); return sm != null && sm.isFolia(); }
    @Override public double getMspt() { var sm = SupportManager.getInstance(); return sm != null ? sm.getMspt() : 0; }
    @Override public boolean canOptimizeWorld(World world) { var m = getModule("WorldCleaner"); return m != null && m.canContinue(world); }
    @Override public void freezePlayer(Player player) { if (exploitFixCommand != null) { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "exploitfix freeze " + player.getName()); } }
    @Override public void unfreezePlayer(Player player) { if (exploitFixCommand != null) { if (isPlayerFrozen(player)) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "exploitfix freeze " + player.getName()); } }
    @Override public boolean isPlayerFrozen(Player player) { return exploitFixCommand != null && player != null && exploitFixCommand.isFrozen(player.getUniqueId()); }
}

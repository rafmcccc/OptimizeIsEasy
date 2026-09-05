package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;

public class TrashDisposalModule extends AbstractModule implements Listener {
    private boolean forcedTrash, global, storeItems;

    public TrashDisposalModule(OptimizeIsEasyPlugin plugin) { super(plugin, "TrashDisposal"); }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (!forcedTrash) return;
        if (!canContinue(e.getPlayer().getWorld())) return;
        e.setCancelled(true);
        // Forced trash: open trash inventory instead (simplified: just cancel)
        if (plugin.isDebug()) plugin.getLogger().fine("TrashDisposal forced trash for " + e.getPlayer().getName());
    }

    @Override
    public void load() { Bukkit.getPluginManager().registerEvents(this, plugin); }

    @Override
    public boolean loadConfig() {
        global = getSection().getBoolean("global", false);
        storeItems = getSection().getBoolean("store_items", true);
        forcedTrash = getSection().getBoolean("forced_trash", false);
        return true;
    }

    @Override
    public void disable() { HandlerList.unregisterAll(this); }
}

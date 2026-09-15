package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.LeavesDecayEvent;

public class InstantLeafDecayModule extends AbstractModule implements Listener {
    private boolean dropItems;
    private int treeDistance;
    private boolean leavesDecay;

    public InstantLeafDecayModule(OptimizeIsEasyPlugin plugin) { super(plugin, "InstantLeafDecay"); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent e) {
        if (!leavesDecay || !canContinue(e.getBlock().getWorld())) return;
        // Instant decay already default; we just ensure drops handled
        if (!dropItems) {
            e.getBlock().setType(Material.AIR);
            e.setCancelled(true);
        }
    }

    @Override
    public void load() { Bukkit.getPluginManager().registerEvents(this, plugin); }

    @Override
    public boolean loadConfig() {
        dropItems = getSection().getBoolean("drop_items", true);
        treeDistance = getSection().getInt("tree_distance", 7);
        leavesDecay = getSection().getBoolean("leaves_decay", true);
        return true;
    }

    @Override
    public void disable() { HandlerList.unregisterAll(this); }
}

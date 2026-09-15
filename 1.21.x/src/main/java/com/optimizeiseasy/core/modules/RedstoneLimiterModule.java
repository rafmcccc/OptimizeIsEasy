package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.SupportManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RedstoneLimiterModule extends AbstractModule implements Listener {
    private int redstoneLimit, pistonLimit;
    private boolean breakRedstone, breakPiston;
    private int clickCooldown;
    private final Map<Chunk, Integer> redstoneTicks = new HashMap<>();
    private final Map<Chunk, Long> cooldowns = new HashMap<>();

    public RedstoneLimiterModule(OptimizeIsEasyPlugin plugin) { super(plugin, "RedstoneLimiter"); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRedstone(BlockRedstoneEvent e) {
        if (!canContinue(e.getBlock().getWorld())) return;
        Chunk chunk = e.getBlock().getChunk();
        int count = redstoneTicks.getOrDefault(chunk, 0) + 1;
        redstoneTicks.put(chunk, count);
        // Reset count next second via fork for Folia support
        SupportManager sm = SupportManager.getInstance();
        if (sm != null) sm.getFork().runLater(false, () -> redstoneTicks.remove(chunk), 1, TimeUnit.SECONDS);
        else Bukkit.getScheduler().runTaskLater(plugin, () -> redstoneTicks.remove(chunk), 20L);
        if (count > redstoneLimit) {
            e.setNewCurrent(0);
            if (breakRedstone) e.getBlock().setType(Material.AIR);
            if (plugin.isDebug()) plugin.getLogger().fine("RedstoneLimiter blocked at " + e.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent e) {
        if (!canContinue(e.getBlock().getWorld())) return;
        if (e.getBlocks().size() > pistonLimit) {
            e.setCancelled(true);
            if (breakPiston) e.getBlock().setType(Material.AIR);
        }
    }

    @Override
    public void load() { Bukkit.getPluginManager().registerEvents(this, plugin); }

    @Override
    public boolean loadConfig() {
        redstoneLimit = getSection().getInt("ticks_limit.redstone", 1100);
        pistonLimit = getSection().getInt("ticks_limit.piston", 50);
        breakRedstone = getSection().getBoolean("break_block.redstone", false);
        breakPiston = getSection().getBoolean("break_block.piston", false);
        clickCooldown = getSection().getInt("click_cooldown", 1500);
        return true;
    }

    @Override
    public void disable() { HandlerList.unregisterAll(this); redstoneTicks.clear(); }
}

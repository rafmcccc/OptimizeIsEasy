package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RedstoneLimiterModule extends AbstractModule implements Listener {
    private int redstoneLimit, pistonLimit;
    private boolean breakRedstone, breakPiston;
    /**
     * Per-chunk event counts, reset by a single periodic task. Keyed by world
     * UUID plus chunk coordinates (not {@link Chunk}) so entries never pin
     * chunk objects and stay safe on Folia region threads.
     */
    private final Map<ChunkKey, AtomicInteger> redstoneTicks = new ConcurrentHashMap<>();
    private BukkitTask resetTask;

    public RedstoneLimiterModule(OptimizeIsEasyPlugin plugin) { super(plugin, "RedstoneLimiter"); }

    private record ChunkKey(UUID world, int x, int z) {
        static ChunkKey of(Chunk chunk) {
            return new ChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRedstone(BlockRedstoneEvent e) {
        if (!canContinue(e.getBlock().getWorld())) return;
        int count = redstoneTicks.computeIfAbsent(ChunkKey.of(e.getBlock().getChunk()), k -> new AtomicInteger())
                .incrementAndGet();
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
    public void load() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        resetTask = Scheduler.runTimer(plugin, false, redstoneTicks::clear, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public boolean loadConfig() {
        redstoneLimit = getSection().getInt("ticks_limit.redstone", 1100);
        pistonLimit = getSection().getInt("ticks_limit.piston", 50);
        breakRedstone = getSection().getBoolean("break_block.redstone", false);
        breakPiston = getSection().getBoolean("break_block.piston", false);
        return true;
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (resetTask != null) {
            try {
                resetTask.cancel();
            } catch (Exception ex) {
                if (plugin.isDebug()) plugin.getLogger().fine("RedstoneLimiter reset cancel failed: " + ex.getMessage());
            }
            resetTask = null;
        }
        redstoneTicks.clear();
    }
}

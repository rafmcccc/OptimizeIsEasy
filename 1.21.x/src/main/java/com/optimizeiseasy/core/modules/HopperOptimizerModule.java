package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.SupportManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class HopperOptimizerModule extends AbstractModule implements Listener {
    private final Map<String, LongAdder> chunkCount = new ConcurrentHashMap<>();
    private final Map<String, Long> lastActivity = new ConcurrentHashMap<>();
    private final Map<String, Long> lastFull = new ConcurrentHashMap<>();
    private BukkitTask validateTask, cleanupTask;
    private boolean chunkLimitEnabled, emptyOptimization, fullOptimization;
    private int maxPerChunk, checkInterval, emptyCheckDelay, fullCheckDelay;
    private long inactiveMs;

    public HopperOptimizerModule(OptimizeIsEasyPlugin plugin) { super(plugin, "HopperOptimizer"); }

    private static String locKey(Location l) {
        return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }

    private static String chunkKey(Location l) {
        return l.getWorld().getName() + ":" + (l.getBlockX() >> 4) + ":" + (l.getBlockZ() >> 4);
    }

    private static Location holderLoc(InventoryHolder h) {
        if (h instanceof Hopper hopper) {
            try {
                Location l = hopper.getLocation();
                if (l != null && l.getWorld() != null) return l;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent e) {
        InventoryHolder src = e.getSource().getHolder();
        InventoryHolder dst = e.getDestination().getHolder();
        long now = System.currentTimeMillis();

        if (src instanceof Hopper) {
            Location l = holderLoc(src);
            if (l != null && canContinue(l.getWorld())) {
                if (chunkLimitEnabled && overCap(l)) { e.setCancelled(true); return; }
                lastActivity.put(locKey(l), now);
            }
        }
        if (dst instanceof Hopper) {
            Location l = holderLoc(dst);
            if (l == null || !canContinue(l.getWorld())) return;
            if (chunkLimitEnabled && overCap(l)) { e.setCancelled(true); return; }
            String key = locKey(l);
            if (fullOptimization && isFullFor(e.getDestination(), e.getItem())) {
                Long lf = lastFull.get(key);
                if (lf != null && now - lf < fullCheckDelay) { e.setCancelled(true); return; }
                lastFull.put(key, now);
            }
            if (emptyOptimization && isEmpty(e.getDestination())) {
                Long la = lastActivity.get(key);
                if (la != null && now - la < emptyCheckDelay) { e.setCancelled(true); return; }
            }
            lastActivity.put(key, now);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (e.getBlock().getType() == Material.HOPPER) addHopper(e.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() == Material.HOPPER) removeHopper(e.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent e) {
        if (!canContinue(e.getWorld())) return;
        scanChunk(e.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent e) {
        String prefix = e.getWorld().getName() + ":" + e.getChunk().getX() + ":" + e.getChunk().getZ() + ":";
        chunkCount.remove(e.getWorld().getName() + ":" + e.getChunk().getX() + ":" + e.getChunk().getZ());
        lastActivity.keySet().removeIf(k -> k.startsWith(prefix));
        lastFull.keySet().removeIf(k -> k.startsWith(prefix));
    }

    private boolean overCap(Location l) {
        LongAdder c = chunkCount.get(chunkKey(l));
        return c != null && c.intValue() >= maxPerChunk;
    }

    private boolean isEmpty(Inventory inv) {
        for (ItemStack it : inv.getContents()) if (it != null && it.getType() != Material.AIR) return false;
        return true;
    }

    private boolean isFullFor(Inventory inv, ItemStack moving) {
        try {
            if (inv.firstEmpty() != -1) return false;
            if (moving == null) return true;
            for (ItemStack it : inv.getContents()) {
                if (it != null && it.isSimilar(moving) && it.getAmount() < it.getMaxStackSize()) return false;
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private void addHopper(Location l) {
        if (l == null || l.getWorld() == null) return;
        chunkCount.computeIfAbsent(chunkKey(l), k -> new LongAdder()).increment();
        lastActivity.put(locKey(l), System.currentTimeMillis());
    }

    private void removeHopper(Location l) {
        if (l == null || l.getWorld() == null) return;
        String ck = chunkKey(l);
        LongAdder c = chunkCount.get(ck);
        if (c != null) {
            c.decrement();
            if (c.intValue() <= 0) chunkCount.remove(ck);
        }
        String key = locKey(l);
        lastActivity.remove(key);
        lastFull.remove(key);
    }

    private void scanChunk(Chunk chunk) {
        try {
            for (BlockState state : chunk.getTileEntities()) {
                if (state instanceof Hopper hopper) {
                    try {
                        Location l = hopper.getLocation();
                        if (l == null || l.getWorld() == null || !canContinue(l.getWorld())) continue;
                        String key = locKey(l);
                        if (!lastActivity.containsKey(key)) {
                            chunkCount.computeIfAbsent(chunkKey(l), k -> new LongAdder()).increment();
                            lastActivity.put(key, System.currentTimeMillis());
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (World w : getAllowedWorlds()) {
            try {
                for (Chunk chunk : w.getLoadedChunks()) scanChunk(chunk);
            } catch (Throwable ignored) {}
        }
        Runnable validate = () -> {
            for (String key : new java.util.HashSet<>(lastActivity.keySet())) {
                try {
                    String[] p = key.split(":");
                    World w = Bukkit.getWorld(p[0]);
                    if (w == null) { lastActivity.remove(key); lastFull.remove(key); continue; }
                    Chunk chunk = w.getChunkAt(Integer.parseInt(p[1]) >> 4, Integer.parseInt(p[2]) >> 4);
                    if (!chunk.isLoaded()) removeHopper(new Location(w, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])));
                    else {
                        BlockState state = chunk.getWorld().getBlockAt(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])).getState();
                        if (!(state instanceof Hopper)) removeHopper(state.getLocation());
                    }
                } catch (Throwable t) {
                    lastActivity.remove(key);
                    lastFull.remove(key);
                }
            }
        };
        Runnable cleanup = () -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, Long> en : new java.util.HashSet<>(lastActivity.entrySet())) {
                if (now - en.getValue() > inactiveMs) {
                    lastActivity.remove(en.getKey());
                    lastFull.remove(en.getKey());
                }
            }
        };
        SupportManager sm = SupportManager.getInstance();
        long validateMs = Math.max(1000, checkInterval);
        if (sm != null) {
            validateTask = sm.getFork().runTimer(false, validate, validateMs, validateMs, TimeUnit.MILLISECONDS);
            cleanupTask = sm.getFork().runTimer(false, cleanup, 60000, 60000, TimeUnit.MILLISECONDS);
        } else {
            validateTask = Bukkit.getScheduler().runTaskTimer(plugin, validate, validateMs / 50L, validateMs / 50L);
            cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, cleanup, 1200L, 1200L);
        }
    }

    @Override
    public boolean loadConfig() {
        chunkLimitEnabled = getSection().getBoolean("chunk_limit.enabled", false);
        maxPerChunk = Math.max(1, getSection().getInt("chunk_limit.limit", 24));
        emptyOptimization = getSection().getBoolean("empty_hopper_optimization.enabled", true);
        emptyCheckDelay = Math.max(0, getSection().getInt("empty_hopper_optimization.empty_check_delay", 300));
        fullOptimization = getSection().getBoolean("full_hopper_optimization.enabled", true);
        fullCheckDelay = Math.max(0, getSection().getInt("full_hopper_optimization.full_check_delay", 80));
        checkInterval = Math.max(1000, getSection().getInt("hopper_check_interval", 2000));
        inactiveMs = Math.max(5000L, getSection().getLong("inactive_hopper_delay", 30000L));
        return true;
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (validateTask != null) validateTask.cancel();
        if (cleanupTask != null) cleanupTask.cancel();
        chunkCount.clear();
        lastActivity.clear();
        lastFull.clear();
    }
}

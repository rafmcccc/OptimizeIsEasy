package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.SupportManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class HopperOptimizerModule extends AbstractModule implements Listener {
    private final Set<Hopper> tracked = ConcurrentHashMap.newKeySet();
    private final Map<String, LongAdder> chunkCount = new ConcurrentHashMap<>();
    private final Map<Hopper, Long> lastActivity = new ConcurrentHashMap<>();
    private BukkitTask optTask, cleanupTask;
    private boolean smartThrottling, chunkLimitEnabled, emptyOptimization;
    private int maxPerChunk, checkInterval, emptyCheckDelay;
    private long inactiveMs;

    public HopperOptimizerModule(OptimizeIsEasyPlugin plugin) { super(plugin, "HopperOptimizer"); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(InventoryMoveItemEvent e) {
        if (e.getSource().getType()==InventoryType.HOPPER) {
            InventoryHolder h = e.getSource().getHolder();
            if (h instanceof Hopper hopper) {
                if (shouldBlock(hopper)) { e.setCancelled(true); return; }
                track(hopper);
            }
        }
        if (e.getDestination().getType()==InventoryType.HOPPER) {
            InventoryHolder h = e.getDestination().getHolder();
            if (h instanceof Hopper ho) track(ho);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlace(BlockPlaceEvent e) {
        if (!e.isCancelled() && e.getBlock().getType()==Material.HOPPER) {
            BlockState s = e.getBlock().getState();
            if (s instanceof Hopper h) trackHopper(h);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent e) {
        if (!e.isCancelled() && e.getBlock().getType()==Material.HOPPER) {
            BlockState s = e.getBlock().getState();
            if (s instanceof Hopper h) removeHopper(h);
        }
    }

    private boolean shouldBlock(Hopper hopper, InventoryMoveItemEvent e) { return shouldBlock(hopper); }
    private boolean shouldBlock(Hopper hopper) {
        if (chunkLimitEnabled) {
            String key = getChunkKey(hopper);
            LongAdder c = chunkCount.get(key);
            if (c != null && c.intValue() >= maxPerChunk && !tracked.contains(hopper)) return true;
        }
        if (smartThrottling) {
            boolean empty = isEmpty(hopper.getInventory());
            Long last = lastActivity.get(hopper);
            if (empty && emptyOptimization && last != null && System.currentTimeMillis()-last < emptyCheckDelay) return true;
        }
        return false;
    }
    private boolean isEmpty(Inventory inv) { for (ItemStack it: inv.getContents()) if (it!=null && it.getType()!=Material.AIR) return false; return true; }
    private void track(Hopper h) { if (h==null||!h.isPlaced()) return; lastActivity.put(h, System.currentTimeMillis()); }
    private void trackHopper(Hopper h) { if (h==null||!h.isPlaced()) return; tracked.add(h); lastActivity.put(h, System.currentTimeMillis()); if (chunkLimitEnabled) chunkCount.computeIfAbsent(getChunkKey(h), k->new LongAdder()).increment(); }
    private void removeHopper(Hopper h) { tracked.remove(h); lastActivity.remove(h); if (chunkLimitEnabled) { LongAdder c=chunkCount.get(getChunkKey(h)); if (c!=null){c.decrement(); if(c.intValue()<=0) chunkCount.remove(getChunkKey(h));} } }
    private String getChunkKey(Hopper h) { Chunk ch=h.getChunk(); return ch.getWorld().getName()+":"+ch.getX()+":"+ch.getZ(); }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        SupportManager sm = SupportManager.getInstance();
        if (sm != null) {
            optTask = sm.getFork().runTimer(false, () -> {
                for (Hopper h: new HashSet<>(tracked)) {
                    try {
                        if (!h.isPlaced() || !h.getChunk().isLoaded()) { removeHopper(h); continue; }
                    } catch (Throwable ex) { removeHopper(h); continue; }
                }
            }, 40, checkInterval, TimeUnit.MILLISECONDS);
            cleanupTask = sm.getFork().runTimer(false, () -> {
                long now=System.currentTimeMillis();
                tracked.removeIf(h->{
                    try {
                        if (h==null||!h.isPlaced()||!h.getChunk().isLoaded()) return true;
                    } catch (Throwable ex) { return true; }
                    Long last=lastActivity.get(h);
                    return last!=null && now-last>300000;
                });
            }, 200, 200, TimeUnit.MILLISECONDS);
        } else {
            optTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                for (Hopper h: new HashSet<>(tracked)) {
                    if (!h.isPlaced() || !h.getChunk().isLoaded()) { removeHopper(h); continue; }
                }
            }, 40L, checkInterval/50L);
            cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                long now=System.currentTimeMillis();
                tracked.removeIf(h->{
                    if (h==null||!h.isPlaced()||!h.getChunk().isLoaded()) return true;
                    Long last=lastActivity.get(h);
                    return last!=null && now-last>300000;
                });
            }, 200L, 200L);
        }
    }

    @Override
    public boolean loadConfig() {
        smartThrottling = getSection().getBoolean("smart_throttling", true);
        chunkLimitEnabled = getSection().getBoolean("chunk_limit.enabled", false);
        maxPerChunk = getSection().getInt("chunk_limit.limit", 16);
        emptyOptimization = getSection().getBoolean("empty_hopper_optimization.enabled", true);
        emptyCheckDelay = getSection().getInt("empty_hopper_optimization.empty_check_delay", 100);
        checkInterval = getSection().getInt("hopper_check_interval", 2000);
        inactiveMs = getSection().getLong("inactive_hopper_delay", 30000L);
        // compatibility with flat keys
        if (!getSection().contains("smart_throttling")) smartThrottling = getConfig().getBoolean(getName()+".values.smart_throttling", true);
        return true;
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (optTask!=null) optTask.cancel();
        if (cleanupTask!=null) cleanupTask.cancel();
    }
}

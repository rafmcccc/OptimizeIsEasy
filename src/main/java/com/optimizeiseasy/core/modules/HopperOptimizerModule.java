package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
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
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class HopperOptimizerModule extends AbstractModule implements Listener {
    private final Map<ChunkKey, LongAdder> chunkCount = new ConcurrentHashMap<>();
    private final Map<HopperKey, Long> lastActivity = new ConcurrentHashMap<>();
    private final Map<HopperKey, Long> lastFull = new ConcurrentHashMap<>();
    private BukkitTask validateTask, cleanupTask;
    private boolean chunkLimitEnabled, emptyOptimization, fullOptimization;
    private int maxPerChunk, checkInterval, emptyCheckDelay, fullCheckDelay;
    private long inactiveMs;

    public HopperOptimizerModule(OptimizeIsEasyPlugin plugin) { super(plugin, "HopperOptimizer"); }

    static final class ChunkKey {
        final UUID world;
        final int x, z;
        ChunkKey(UUID world, int x, int z) { this.world = world; this.x = x; this.z = z; }
        static ChunkKey of(Location l) { return new ChunkKey(l.getWorld().getUID(), l.getBlockX() >> 4, l.getBlockZ() >> 4); }
        static ChunkKey of(World w, int x, int z) { return new ChunkKey(w.getUID(), x, z); }
        @Override public boolean equals(Object o) {
            if (!(o instanceof ChunkKey k)) return false;
            return x == k.x && z == k.z && world.equals(k.world);
        }
        @Override public int hashCode() { return world.hashCode() * 31 + x * 31 + z; }
    }

    static final class HopperKey {
        final UUID world;
        final long packed;
        HopperKey(UUID world, long packed) { this.world = world; this.packed = packed; }
        static long pack(int x, int y, int z) {
            return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
        }
        static HopperKey of(Location l) { return new HopperKey(l.getWorld().getUID(), pack(l.getBlockX(), l.getBlockY(), l.getBlockZ())); }
        int x() { return (int) (packed >> 38); }
        int y() {
            int y = (int) (packed & 0xFFF);
            return y >= 2048 ? y - 4096 : y;
        }
        int z() {
            int z = (int) ((packed >> 12) & 0x3FFFFFF);
            return z >= (1 << 25) ? z - (1 << 26) : z;
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof HopperKey k)) return false;
            return packed == k.packed && world.equals(k.world);
        }
        @Override public int hashCode() { return world.hashCode() * 31 + Long.hashCode(packed); }
    }

    private static InventoryHolder holderNoSnapshot(Inventory inv) {
        try {
            return inv.getHolder(false);
        } catch (Throwable t) {
            return inv.getHolder();
        }
    }

    private static BlockState[] tileEntitiesNoSnapshot(Chunk chunk) {
        try {
            return chunk.getTileEntities(false);
        } catch (Throwable t) {
            return chunk.getTileEntities();
        }
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
        InventoryHolder src = holderNoSnapshot(e.getSource());
        InventoryHolder dst = holderNoSnapshot(e.getDestination());
        long now = System.currentTimeMillis();

        if (src instanceof Hopper) {
            Location l = holderLoc(src);
            if (l != null && canContinue(l.getWorld())) {
                if (chunkLimitEnabled && overCapForMove(l)) { e.setCancelled(true); return; }
                lastActivity.put(HopperKey.of(l), now);
            }
        }
        if (dst instanceof Hopper) {
            Location l = holderLoc(dst);
            if (l == null || !canContinue(l.getWorld())) return;
            if (chunkLimitEnabled && overCapForMove(l)) { e.setCancelled(true); return; }
            HopperKey key = HopperKey.of(l);
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlaceCap(BlockPlaceEvent e) {
        if (e.getBlock().getType() != Material.HOPPER) return;
        if (!chunkLimitEnabled || !canContinue(e.getBlock().getWorld())) return;
        if (isFullForPlace(e.getBlock().getLocation())) {
            if (recountChunk(e.getBlock().getChunk()) >= maxPerChunk) {
                e.setCancelled(true);
                e.getPlayer().sendMessage("§cHopper limit reached in this chunk (" + maxPerChunk + ").");
                return;
            }
        }
        addHopper(e.getBlock().getLocation());
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
        ChunkKey ck = ChunkKey.of(e.getWorld(), e.getChunk().getX(), e.getChunk().getZ());
        chunkCount.remove(ck);
        UUID uid = e.getWorld().getUID();
        int cx = e.getChunk().getX(), cz = e.getChunk().getZ();
        lastActivity.keySet().removeIf(k -> k.world.equals(uid) && (k.x() >> 4) == cx && (k.z() >> 4) == cz);
        lastFull.keySet().removeIf(k -> k.world.equals(uid) && (k.x() >> 4) == cx && (k.z() >> 4) == cz);
    }

    private boolean overCap(Location l) {
        return isFullForPlace(l);
    }

    static boolean isFullForPlace(int count, int max) { return count >= max; }
    static boolean isOverForMove(int count, int max) { return count > max; }

    private boolean isFullForPlace(Location l) {
        LongAdder c = chunkCount.get(ChunkKey.of(l));
        return c != null && isFullForPlace(c.intValue(), maxPerChunk);
    }

    private boolean overCapForMove(Location l) {
        LongAdder c = chunkCount.get(ChunkKey.of(l));
        if (c == null || !isOverForMove(c.intValue(), maxPerChunk)) return false;
        try {
            World w = l.getWorld();
            int cx = l.getBlockX() >> 4, cz = l.getBlockZ() >> 4;
            if (w != null && w.isChunkLoaded(cx, cz)) {
                return isOverForMove(recountChunk(w.getChunkAt(cx, cz, false)), maxPerChunk);
            }
        } catch (Throwable ignored) {}
        return true;
    }

    private int recountChunk(Chunk chunk) {
        int n = 0;
        try {
            for (BlockState state : tileEntitiesNoSnapshot(chunk)) {
                if (state instanceof Hopper) n++;
            }
        } catch (Throwable ignored) {}
        try {
            chunkCount.put(ChunkKey.of(chunk.getWorld(), chunk.getX(), chunk.getZ()), new LongAdder());
            chunkCount.get(ChunkKey.of(chunk.getWorld(), chunk.getX(), chunk.getZ())).add(n);
        } catch (Throwable ignored) {}
        return n;
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
        chunkCount.computeIfAbsent(ChunkKey.of(l), k -> new LongAdder()).increment();
        lastActivity.put(HopperKey.of(l), System.currentTimeMillis());
    }

    private void removeHopper(Location l) {
        if (l == null || l.getWorld() == null) return;
        ChunkKey ck = ChunkKey.of(l);
        LongAdder c = chunkCount.get(ck);
        if (c != null) {
            c.decrement();
            if (c.intValue() <= 0) chunkCount.remove(ck);
        }
        HopperKey key = HopperKey.of(l);
        lastActivity.remove(key);
        lastFull.remove(key);
    }

    private void scanChunk(Chunk chunk) {
        try {
            for (BlockState state : tileEntitiesNoSnapshot(chunk)) {
                if (state instanceof Hopper hopper) {
                    try {
                        Location l = hopper.getLocation();
                        if (l == null || l.getWorld() == null || !canContinue(l.getWorld())) continue;
                        HopperKey key = HopperKey.of(l);
                        if (!lastActivity.containsKey(key)) {
                            chunkCount.computeIfAbsent(ChunkKey.of(l), k -> new LongAdder()).increment();
                            lastActivity.put(key, System.currentTimeMillis());
                        }
                    } catch (Throwable t) {
                        if (plugin.isDebug()) plugin.getLogger().fine("[HopperOptimizer] skipping unreadable hopper: " + t.getMessage());
                    }
                }
            }
        } catch (Throwable t) {
            if (plugin.isDebug()) plugin.getLogger().fine("[HopperOptimizer] chunk tile-entity scan failed: " + t.getMessage());
        }
    }

    @Override
    public void load() {
        boolean moveEventDisabled = false;
        try {
            org.bukkit.configuration.file.YamlConfiguration paperGlobal =
                    com.optimizeiseasy.core.utils.ServerFileUtil.loadYaml("config/paper-global.yml");
            moveEventDisabled = paperGlobal.getBoolean("hopper.disable-move-event", false);
        } catch (Throwable ignored) {}
        if (moveEventDisabled) {
            plugin.getLogger().warning("[HopperOptimizer] paper hopper.disable-move-event=true: InventoryMoveItemEvent never fires, transfer throttling disabled (place cap + scan still active).");
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (World w : getAllowedWorlds()) {
            try {
                for (Chunk chunk : w.getLoadedChunks()) scanChunk(chunk);
            } catch (Throwable t) {
                if (plugin.isDebug()) plugin.getLogger().fine("[HopperOptimizer] loaded-chunk scan failed for a world: " + t.getMessage());
            }
        }
        Runnable validate = () -> {
            for (HopperKey key : new java.util.HashSet<>(lastActivity.keySet())) {
                try {
                    World w = Bukkit.getWorld(key.world);
                    if (w == null) { lastActivity.remove(key); lastFull.remove(key); continue; }
                    int bx = key.x(), by = key.y(), bz = key.z();
                    if (!w.isChunkLoaded(bx >> 4, bz >> 4)) {
                        lastActivity.remove(key);
                        lastFull.remove(key);
                        continue;
                    }
                    Block b;
                    try {
                        b = w.getBlockAt(bx, by, bz);
                    } catch (Throwable t) {
                        lastActivity.remove(key);
                        lastFull.remove(key);
                        continue;
                    }
                    Material type;
                    try {
                        type = b.getType();
                    } catch (Throwable t) {
                        lastActivity.remove(key);
                        lastFull.remove(key);
                        continue;
                    }
                    if (type != Material.HOPPER) {
                        lastActivity.remove(key);
                        lastFull.remove(key);
                        ChunkKey ck = ChunkKey.of(w, bx >> 4, bz >> 4);
                        LongAdder c = chunkCount.get(ck);
                        if (c != null) {
                            c.decrement();
                            if (c.intValue() <= 0) chunkCount.remove(ck);
                        }
                    }
                } catch (Throwable t) {
                    lastActivity.remove(key);
                    lastFull.remove(key);
                }
            }
        };
        Runnable cleanup = () -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<HopperKey, Long> en : new java.util.HashSet<>(lastActivity.entrySet())) {
                if (now - en.getValue() > inactiveMs) {
                    lastActivity.remove(en.getKey());
                    lastFull.remove(en.getKey());
                }
            }
        };
        long validateMs = Math.max(1000, checkInterval);
        validateTask = Scheduler.runTimer(plugin, false, validate, validateMs, validateMs, TimeUnit.MILLISECONDS);
        cleanupTask = Scheduler.runTimer(plugin, false, cleanup, 60000, 60000, TimeUnit.MILLISECONDS);
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

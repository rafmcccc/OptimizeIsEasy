package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.Scheduler;
import com.optimizeiseasy.core.support.SupportManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class EntityLimiterModule extends AbstractModule implements Listener {
    private final EnumSet<CreatureSpawnEvent.SpawnReason> reasons = EnumSet.noneOf(CreatureSpawnEvent.SpawnReason.class);
    private final EnumSet<EntityType> whitelist = EnumSet.noneOf(EntityType.class);
    private final Map<ChunkKey, int[]> counts = new ConcurrentHashMap<>();
    private BukkitTask overflowTask, recountTask;
    private int creatures, items, vehicles, projectiles;
    private boolean overflowEnabled;
    private int overflowInterval;
    private double overflowMultiplier;
    private boolean overflowCreatures, overflowItems, overflowVehicles, overflowProjectiles;
    private boolean foliaOverflowWarned = false;

    public EntityLimiterModule(OptimizeIsEasyPlugin plugin) {
        super(plugin, "EntityLimiter");
    }

    record ChunkKey(UUID world, int x, int z) {
        static ChunkKey of(Location l) { return new ChunkKey(l.getWorld().getUID(), l.getBlockX() >> 4, l.getBlockZ() >> 4); }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (handleEvent(e.getLocation(), e.getSpawnReason(), e.getEntityType(), creatures, ent -> ent instanceof Mob, 0)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpawner(SpawnerSpawnEvent e) {
        if (handleEvent(e.getLocation(), CreatureSpawnEvent.SpawnReason.SPAWNER, e.getEntityType(), creatures, ent -> ent instanceof Mob, 0)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (handleEvent(e.getItemDrop().getLocation(), null, e.getItemDrop().getType(), items, ent -> ent instanceof Item, 1)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent e) {
        if (handleEvent(e.getLocation(), null, e.getEntityType(), items, ent -> ent instanceof Item, 1)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVehicle(VehicleCreateEvent e) {
        if (handleEvent(e.getVehicle().getLocation(), null, e.getVehicle().getType(), vehicles, ent -> ent instanceof Vehicle, 2)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        if (handleEvent(e.getEntity().getLocation(), null, e.getEntity().getType(), projectiles, ent -> ent instanceof Projectile, 3)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        try {
            Location l = e.getEntity().getLocation();
            if (l == null || l.getWorld() == null) return;
            ChunkKey ck = ChunkKey.of(l);
            int[] c = counts.get(ck);
            if (c == null) return;
            Entity ent = e.getEntity();
            if (ent instanceof Mob) c[0] = Math.max(0, c[0] - 1);
            else if (ent instanceof Item) c[1] = Math.max(0, c[1] - 1);
            else if (ent instanceof Vehicle) c[2] = Math.max(0, c[2] - 1);
            else if (ent instanceof Projectile) c[3] = Math.max(0, c[3] - 1);
        } catch (Throwable ignored) {}
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent e) {
        counts.remove(new ChunkKey(e.getWorld().getUID(), e.getChunk().getX(), e.getChunk().getZ()));
    }

    private boolean handleEvent(Location loc, CreatureSpawnEvent.SpawnReason reason, EntityType type, int limit, Predicate<Entity> filter, int slot) {
        if (limit < 1 || loc == null || loc.getWorld() == null) return false;
        if (!canContinue(loc.getWorld()) || (reason != null && !reasons.contains(reason)) || whitelist.contains(type)) return false;
        World w = loc.getWorld();
        int cx = loc.getBlockX() >> 4, cz = loc.getBlockZ() >> 4;
        if (!w.isChunkLoaded(cx, cz)) return false;
        ChunkKey ck = new ChunkKey(w.getUID(), cx, cz);
        int[] cached = counts.get(ck);
        if (cached != null && cached[slot] >= limit) return true;
        int count = 0;
        Entity[] entities;
        try {
            entities = w.getChunkAt(cx, cz, false).getEntities();
        } catch (Throwable t) {
            return false;
        }
        for (Entity entity : entities) {
            if (filter.test(entity) && !whitelist.contains(entity.getType()) && ++count >= limit) {
                counts.computeIfAbsent(ck, k -> new int[4])[slot] = count;
                return true;
            }
        }
        counts.computeIfAbsent(ck, k -> new int[4])[slot] = count;
        if (cached == null) recountChunk(w, cx, cz);
        return false;
    }

    private void recountChunk(World w, int cx, int cz) {
        try {
            if (!w.isChunkLoaded(cx, cz)) {
                counts.remove(new ChunkKey(w.getUID(), cx, cz));
                return;
            }
            Entity[] entities = w.getChunkAt(cx, cz, false).getEntities();
            int[] c = new int[4];
            for (Entity entity : entities) {
                if (whitelist.contains(entity.getType())) continue;
                if (entity instanceof Mob) c[0]++;
                else if (entity instanceof Item) c[1]++;
                else if (entity instanceof Vehicle) c[2]++;
                else if (entity instanceof Projectile) c[3]++;
            }
            counts.put(new ChunkKey(w.getUID(), cx, cz), c);
        } catch (Throwable ignored) {}
    }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        SupportManager sm = SupportManager.getInstance();
        boolean folia = sm != null && sm.isFolia();
        if (overflowEnabled) {
            if (folia) {
                if (!foliaOverflowWarned) {
                    foliaOverflowWarned = true;
                    plugin.getLogger().warning("[EntityLimiter] overflow purge disabled on Folia: global getLoadedChunks()/chunk.getEntities() is not region-safe. Per-spawn limits still apply.");
                }
            } else {
                Runnable purge = () -> {
                    int limitCreatures = (int) (creatures * overflowMultiplier);
                    int limitItems = (int) (items * overflowMultiplier);
                    int limitVehicles = (int) (vehicles * overflowMultiplier);
                    int limitProjectiles = (int) (projectiles * overflowMultiplier);
                    for (org.bukkit.World w : getAllowedWorlds()) {
                        Chunk[] loaded;
                        try {
                            loaded = w.getLoadedChunks();
                        } catch (Throwable t) {
                            plugin.getLogger().warning("[EntityLimiter] could not list chunks in " + w.getName() + ": " + t.getMessage());
                            continue;
                        }
                        for (Chunk chunk : loaded) {
                            Entity[] entities;
                            try {
                                entities = chunk.getEntities();
                            } catch (Throwable t) {
                                plugin.getLogger().warning("[EntityLimiter] could not read chunk entities: " + t.getMessage());
                                continue;
                            }
                            if (entities.length == 0) continue;
                            int c = 0, i = 0, v = 0, p = 0;
                            for (Entity entity : entities) {
                                if (whitelist.contains(entity.getType()) || (entity.getCustomName() != null && !getSection().getBoolean("overflow_purge.types.named", false))) continue;
                                boolean removed = false;
                                if (entity instanceof Mob) { if (c < limitCreatures) c++; else if (overflowCreatures) removed = true; }
                                else if (entity instanceof Item) { if (i < limitItems) i++; else if (overflowItems) removed = true; }
                                else if (entity instanceof Vehicle) { if (v < limitVehicles) v++; else if (overflowVehicles) removed = true; }
                                else if (entity instanceof Projectile) { if (p < limitProjectiles) p++; else if (overflowProjectiles) removed = true; }
                                if (removed) entity.remove();
                            }
                        }
                    }
                };
                overflowTask = Scheduler.runTimer(plugin, false, purge, overflowInterval, overflowInterval, TimeUnit.SECONDS);
            }
        }
        recountTask = Scheduler.runTimer(plugin, true, () -> {
            for (org.bukkit.World w : getAllowedWorlds()) {
                Chunk[] loaded;
                try {
                    loaded = w.getLoadedChunks();
                } catch (Throwable ignored) {
                    continue;
                }
                for (Chunk chunk : loaded) recountChunk(w, chunk.getX(), chunk.getZ());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public boolean loadConfig() {
        creatures = getSection().getInt("creatures", 15);
        items = getSection().getInt("items", -1);
        vehicles = getSection().getInt("vehicles", 3);
        projectiles = getSection().getInt("projectiles", 5);
        reasons.clear();
        for (String s : getSection().getStringList("reasons")) {
            try {
                reasons.add(CreatureSpawnEvent.SpawnReason.valueOf(s));
            } catch (Exception e) {
                plugin.getLogger().warning("[EntityLimiter] ignoring unknown spawn reason '" + s + "'");
            }
        }
        whitelist.clear();
        for (String s : getSection().getStringList("whitelist")) {
            try {
                whitelist.add(EntityType.valueOf(s));
            } catch (Exception e) {
                plugin.getLogger().warning("[EntityLimiter] ignoring unknown entity type '" + s + "' in whitelist");
            }
        }
        overflowInterval = Math.max(1, getSection().getInt("overflow_purge.interval", 30));
        overflowEnabled = getSection().getBoolean("overflow_purge.enabled", false);
        if (overflowEnabled) {
            overflowMultiplier = getSection().getDouble("overflow_purge.limit_multiplier", 1.5);
            overflowCreatures = creatures > 0 && getSection().getBoolean("overflow_purge.types.creatures", true);
            overflowItems = items > 0 && getSection().getBoolean("overflow_purge.types.items", false);
            overflowVehicles = vehicles > 0 && getSection().getBoolean("overflow_purge.types.vehicles", true);
            overflowProjectiles = projectiles > 0 && getSection().getBoolean("overflow_purge.types.projectiles", true);
        }
        return true;
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (overflowTask != null) overflowTask.cancel();
        if (recountTask != null) recountTask.cancel();
        counts.clear();
    }
}

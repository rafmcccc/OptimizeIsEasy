package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.SupportManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class EntityLimiterModule extends AbstractModule implements Listener {
    private final EnumSet<CreatureSpawnEvent.SpawnReason> reasons = EnumSet.noneOf(CreatureSpawnEvent.SpawnReason.class);
    private final EnumSet<EntityType> whitelist = EnumSet.noneOf(EntityType.class);
    private BukkitTask overflowTask;
    private int creatures, items, vehicles, projectiles;
    private boolean overflowEnabled;
    private int overflowInterval;
    private double overflowMultiplier;
    private boolean overflowCreatures, overflowItems, overflowVehicles, overflowProjectiles;
    private boolean ignoreModels;

    public EntityLimiterModule(OptimizeIsEasyPlugin plugin) {
        super(plugin, "EntityLimiter");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (handleEvent(e.getLocation(), e.getSpawnReason(), e.getEntityType(), creatures, ent -> ent instanceof Mob)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpawner(SpawnerSpawnEvent e) {
        if (handleEvent(e.getLocation(), CreatureSpawnEvent.SpawnReason.SPAWNER, e.getEntityType(), creatures, ent -> ent instanceof Mob)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (handleEvent(e.getItemDrop().getLocation(), null, e.getItemDrop().getType(), items, ent -> ent instanceof Item)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVehicle(VehicleCreateEvent e) {
        if (handleEvent(e.getVehicle().getLocation(), null, e.getVehicle().getType(), vehicles, ent -> ent instanceof Vehicle)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        if (handleEvent(e.getEntity().getLocation(), null, e.getEntity().getType(), projectiles, ent -> ent instanceof Projectile)) e.setCancelled(true);
    }

    private boolean handleEvent(Location loc, CreatureSpawnEvent.SpawnReason reason, EntityType type, int limit, Predicate<Entity> filter) {
        if (limit < 1 || !canContinue(loc.getWorld()) || (reason != null && !reasons.contains(reason)) || whitelist.contains(type) || !loc.getChunk().isLoaded()) return false;
        int count = 0;
        for (Entity entity : loc.getChunk().getEntities()) {
            if (filter.test(entity) && !whitelist.contains(entity.getType()) && ++count >= limit) return true;
        }
        return false;
    }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (overflowEnabled) {
            SupportManager sm = SupportManager.getInstance();
            Runnable purge = () -> {
                int limitCreatures = (int)(creatures * overflowMultiplier);
                int limitItems = (int)(items * overflowMultiplier);
                int limitVehicles = (int)(vehicles * overflowMultiplier);
                int limitProjectiles = (int)(projectiles * overflowMultiplier);
                boolean folia = sm != null && sm.isFolia();
                for (org.bukkit.World w : getAllowedWorlds()) {
                    for (Chunk chunk : w.getLoadedChunks()) {
                        Entity[] entities = chunk.getEntities();
                        if (entities.length == 0) continue;
                        int c=0,i=0,v=0,p=0;
                        for (Entity entity : entities) {
                            if (whitelist.contains(entity.getType()) || (entity.getCustomName()!=null && !getSection().getBoolean("overflow_purge.types.named", false))) continue;
                            boolean removed = false;
                            if (entity instanceof Mob) { if (c < limitCreatures) c++; else if (overflowCreatures) removed=true; }
                            else if (entity instanceof Item) { if (i < limitItems) i++; else if (overflowItems) removed=true; }
                            else if (entity instanceof Vehicle) { if (v < limitVehicles) v++; else if (overflowVehicles) removed=true; }
                            else if (entity instanceof Projectile) { if (p < limitProjectiles) p++; else if (overflowProjectiles) removed=true; }
                            if (removed) {
                                if (folia) {
                                    try { Bukkit.getRegionScheduler().run(plugin, entity.getLocation(), t -> entity.remove()); } catch (Throwable ex) { entity.remove(); }
                                } else entity.remove();
                            }
                        }
                    }
                }
            };
            if (sm != null) overflowTask = sm.getFork().runTimer(false, purge, overflowInterval, overflowInterval, TimeUnit.SECONDS);
            else overflowTask = Bukkit.getScheduler().runTaskTimer(plugin, purge, overflowInterval*20L, overflowInterval*20L);
        }
    }

    @Override
    public boolean loadConfig() {
        ignoreModels = getSection().getBoolean("ignore_models", true);
        creatures = getSection().getInt("creatures", 15);
        items = getSection().getInt("items", -1);
        vehicles = getSection().getInt("vehicles", 3);
        projectiles = getSection().getInt("projectiles", 5);
        reasons.clear();
        for (String s : getSection().getStringList("reasons")) try { reasons.add(CreatureSpawnEvent.SpawnReason.valueOf(s)); } catch (Exception ignored) {}
        whitelist.clear();
        for (String s : getSection().getStringList("whitelist")) try { whitelist.add(EntityType.valueOf(s)); } catch (Exception ignored) {}
        overflowInterval = Math.max(1, getSection().getInt("overflow_purge.interval", 30));
        overflowEnabled = getSection().getBoolean("overflow_purge.enabled", false);
        if (overflowEnabled) {
            overflowMultiplier = getSection().getDouble("overflow_purge.limit_multiplier", 1.5);
            overflowCreatures = creatures>0 && getSection().getBoolean("overflow_purge.types.creatures", true);
            overflowItems = items>0 && getSection().getBoolean("overflow_purge.types.items", false);
            overflowVehicles = vehicles>0 && getSection().getBoolean("overflow_purge.types.vehicles", true);
            overflowProjectiles = projectiles>0 && getSection().getBoolean("overflow_purge.types.projectiles", true);
        }
        return true;
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (overflowTask != null) overflowTask.cancel();
    }
}

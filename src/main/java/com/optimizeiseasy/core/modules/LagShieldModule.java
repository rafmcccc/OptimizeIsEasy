package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.Scheduler;
import com.optimizeiseasy.core.support.SupportManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.FireworkExplodeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class LagShieldModule extends AbstractModule implements Listener, Runnable {
    private final TreeMap<Double, Integer> viewMap = new TreeMap<>();
    private final TreeMap<Double, Integer> simMap = new TreeMap<>();
    private final TreeMap<Double, Integer> tickMap = new TreeMap<>();
    /** Per-world originals snapshotted before the first dynamic change, restored on disable. */
    private final Map<UUID, Integer> origView = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> origSim = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> origTick = new ConcurrentHashMap<>();
    private BukkitTask task;
    private double entitySpawnTps, hopperTps, redstoneTps, projectilesTps, leavesTps, liquidTps, explosionsTps, fireworksTps;
    private boolean dv, ds, dt;

    public LagShieldModule(OptimizeIsEasyPlugin plugin) { super(plugin, "LagShield"); }

    @Override
    public void run() {
        if (!SupportManager.isTpsReliableNow()) {
            if (plugin.isDebug()) plugin.getLogger().fine("LagShield check skipped: no reliable TPS source on this server software.");
            return;
        }
        double tps = getTps();
        boolean lowSpawn = tps < entitySpawnTps && entitySpawnTps!=-1;
        boolean lowHopper = tps < hopperTps && hopperTps!=-1;
        boolean lowRedstone = tps < redstoneTps && redstoneTps!=-1;
        // Dynamic distances
        if (dv) {
            Integer v = getThreshold(viewMap, tps);
            if (v!=null) for (World w: getAllowedWorlds()) {
                try {
                    origView.computeIfAbsent(w.getUID(), k -> w.getViewDistance());
                    w.setViewDistance(v);
                } catch (Exception ex) {
                    if (plugin.isDebug()) plugin.getLogger().fine("LagShield view-distance set failed: " + ex.getMessage());
                }
            }
        }
        if (ds) {
            Integer s = getThreshold(simMap, tps);
            if (s!=null) for (World w: getAllowedWorlds()) {
                try {
                    origSim.computeIfAbsent(w.getUID(), k -> w.getSimulationDistance());
                    w.setSimulationDistance(s);
                } catch (Exception ex) {
                    if (plugin.isDebug()) plugin.getLogger().fine("LagShield simulation-distance set failed: " + ex.getMessage());
                }
            }
        }
        if (dt) {
            Integer t = getThreshold(tickMap, tps);
            if (t!=null) for (World w: getAllowedWorlds()) {
                try {
                    origTick.computeIfAbsent(w.getUID(), k -> w.getGameRuleValue(GameRule.RANDOM_TICK_SPEED));
                    w.setGameRule(GameRule.RANDOM_TICK_SPEED, t);
                } catch (Exception ex) {
                    if (plugin.isDebug()) plugin.getLogger().fine("LagShield tick-speed set failed: " + ex.getMessage());
                }
            }
        }
        if (plugin.isDebug()) plugin.getLogger().fine(String.format("LagShield check tps=%.2f lowSpawn=%b lowHopper=%b", tps, lowSpawn, lowHopper));
    }

    private double getTps() {
        return SupportManager.currentTps();
    }
    private Integer getThreshold(TreeMap<Double,Integer> map, double tps) {
        if (map.isEmpty()) return null;
        Map.Entry<Double,Integer> e = map.ceilingEntry(tps);
        if (e!=null) return e.getValue();
        e = map.lastEntry();
        return e!=null?e.getValue():null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRedstone(BlockRedstoneEvent e) {
        if (e.getNewCurrent() != 0 && redstoneTps != -1 && canContinue(e.getBlock().getWorld())
                && SupportManager.isTpsReliableNow() && getTps() < redstoneTps) e.setNewCurrent(0);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawn(VehicleCreateEvent e) { if (entitySpawnTps!=-1 && canContinue(e.getVehicle().getWorld()) && SupportManager.isTpsReliableNow() && getTps() < entitySpawnTps) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) { if (entitySpawnTps!=-1 && canContinue(e.getEntity().getWorld()) && SupportManager.isTpsReliableNow() && getTps() < entitySpawnTps) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) { if (projectilesTps!=-1 && canContinue(e.getEntity().getWorld()) && SupportManager.isTpsReliableNow() && getTps() < projectilesTps) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent e) { if (e.getSource().getType()==InventoryType.HOPPER && hopperTps!=-1 && SupportManager.isTpsReliableNow() && getTps() < hopperTps) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent e) { if (leavesTps!=-1 && canContinue(e.getBlock().getWorld()) && SupportManager.isTpsReliableNow() && getTps() < leavesTps) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLiquid(BlockFromToEvent e) { if (e.getBlock().isLiquid() && liquidTps!=-1 && canContinue(e.getBlock().getWorld()) && SupportManager.isTpsReliableNow() && getTps() < liquidTps) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(BlockExplodeEvent e) { if (explosionsTps!=-1 && canContinue(e.getBlock().getWorld()) && SupportManager.isTpsReliableNow() && getTps() < explosionsTps) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFirework(FireworkExplodeEvent e) { if (fireworksTps!=-1 && canContinue(e.getEntity().getWorld()) && SupportManager.isTpsReliableNow() && getTps() < fireworksTps) e.setCancelled(true); }

    private void loadThreshold(Map<Double,Integer> map, String key) {
        map.clear();
        for (String s: getSection().getStringList(key)) {
            try {
                String[] p = s.split(":");
                map.put(Double.parseDouble(p[0]), Integer.parseInt(p[1]));
            } catch (Exception ex) {
                if (plugin.isDebug()) plugin.getLogger().fine("LagShield ignoring bad threshold '" + s + "': " + ex.getMessage());
            }
        }
    }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (!SupportManager.isTpsReliableNow()) {
            plugin.getLogger().warning("LagShield: no reliable TPS source on this server software - throttling and dynamic distances are disabled until one is available.");
        }
        task = Scheduler.runTimer(plugin, false, this, 20, 20, TimeUnit.SECONDS);
    }

    @Override
    public boolean loadConfig() {
        entitySpawnTps = getSection().getDouble("tps_threshold.entity_spawn", 19);
        hopperTps = getSection().getDouble("tps_threshold.tick_hopper", 18);
        redstoneTps = getSection().getDouble("tps_threshold.redstone", 18);
        projectilesTps = getSection().getDouble("tps_threshold.projectiles", 15);
        leavesTps = getSection().getDouble("tps_threshold.leaves_decay", 19);
        liquidTps = getSection().getDouble("tps_threshold.liquid_flow", 18);
        explosionsTps = getSection().getDouble("tps_threshold.explosions", 18.5);
        fireworksTps = getSection().getDouble("tps_threshold.fireworks", 19);
        dv = getSection().getBoolean("dynamic_view_distance.enabled", false);
        if (dv) loadThreshold(viewMap, "dynamic_view_distance.tps_thresholds");
        ds = getSection().getBoolean("dynamic_simulation_distance.enabled", true);
        if (ds) loadThreshold(simMap, "dynamic_simulation_distance.tps_thresholds");
        dt = getSection().getBoolean("dynamic_tick_speed.enabled", true);
        if (dt) loadThreshold(tickMap, "dynamic_tick_speed.tps_thresholds");
        return true;
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ex) {
                if (plugin.isDebug()) plugin.getLogger().fine("LagShield task cancel failed: " + ex.getMessage());
            }
            task = null;
        }
        restoreOriginals();
    }

    /** Restore view distance, simulation distance and random tick speed tweaked while running. */
    private void restoreOriginals() {
        if (origView.isEmpty() && origSim.isEmpty() && origTick.isEmpty()) return;
        for (World w : Bukkit.getWorlds()) {
            Integer v = origView.remove(w.getUID());
            if (v != null) {
                try {
                    w.setViewDistance(v);
                } catch (Exception ex) {
                    if (plugin.isDebug()) plugin.getLogger().fine("LagShield view-distance restore failed: " + ex.getMessage());
                }
            }
            Integer s = origSim.remove(w.getUID());
            if (s != null) {
                try {
                    w.setSimulationDistance(s);
                } catch (Exception ex) {
                    if (plugin.isDebug()) plugin.getLogger().fine("LagShield simulation-distance restore failed: " + ex.getMessage());
                }
            }
            Integer t = origTick.remove(w.getUID());
            if (t != null) {
                try {
                    w.setGameRule(GameRule.RANDOM_TICK_SPEED, t);
                } catch (Exception ex) {
                    if (plugin.isDebug()) plugin.getLogger().fine("LagShield tick-speed restore failed: " + ex.getMessage());
                }
            }
        }
        origView.clear();
        origSim.clear();
        origTick.clear();
    }
}

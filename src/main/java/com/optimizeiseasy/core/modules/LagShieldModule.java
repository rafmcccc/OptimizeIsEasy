package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.Scheduler;
import com.optimizeiseasy.core.support.SupportManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
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

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class LagShieldModule extends AbstractModule implements Listener, Runnable {
    private final TreeMap<Double, Integer> viewMap = new TreeMap<>();
    private final TreeMap<Double, Integer> simMap = new TreeMap<>();
    private final TreeMap<Double, Integer> tickMap = new TreeMap<>();
    private final Map<UUID, Integer> origView = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> origSim = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> origTick = new ConcurrentHashMap<>();
    private BukkitTask task;
    private double entitySpawnTps, hopperTps, redstoneTps, projectilesTps, leavesTps, liquidTps, explosionsTps, fireworksTps;
    private boolean dv, ds, dt;
    private boolean preferMspt = true;
    private long minHoldMs = 60000;
    private volatile double cachedTps = 20.0;
    private volatile double cachedMspt = 0;
    private volatile long cachedAt = 0;
    private volatile boolean tpsReliable = true;
    private Integer lastView, lastSim, lastTick;
    private long lastViewChange = 0, lastSimChange = 0, lastTickChange = 0;
    private boolean foliaWarned = false;

    public LagShieldModule(OptimizeIsEasyPlugin plugin) { super(plugin, "LagShield"); }

    private File originalsFile() {
        return new File(plugin.getDataFolder(), "lagshield-originals.yml");
    }

    private void persistOriginals() {
        try {
            YamlConfiguration cfg = new YamlConfiguration();
            for (Map.Entry<UUID, Integer> e : origView.entrySet()) cfg.set(e.getKey() + ".view", e.getValue());
            for (Map.Entry<UUID, Integer> e : origSim.entrySet()) cfg.set(e.getKey() + ".sim", e.getValue());
            for (Map.Entry<UUID, Integer> e : origTick.entrySet()) cfg.set(e.getKey() + ".tick", e.getValue());
            cfg.save(originalsFile());
        } catch (Throwable t) {
            plugin.getLogger().warning("LagShield could not persist original distances: " + t.getMessage());
        }
    }

    private void loadPersistedOriginals() {
        File f = originalsFile();
        if (!f.isFile()) return;
        try {
            YamlConfiguration cfg = new YamlConfiguration();
            cfg.load(f);
            for (String key : cfg.getKeys(false)) {
                try {
                    UUID uid = UUID.fromString(key);
                    if (cfg.contains(key + ".view")) origView.put(uid, cfg.getInt(key + ".view"));
                    if (cfg.contains(key + ".sim")) origSim.put(uid, cfg.getInt(key + ".sim"));
                    if (cfg.contains(key + ".tick")) origTick.put(uid, cfg.getInt(key + ".tick"));
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("LagShield could not load persisted originals: " + t.getMessage());
        }
    }

    private double readMetric() {
        try {
            SupportManager sm = SupportManager.getInstance();
            if (sm != null && sm.getFork() != null) {
                if (preferMspt && sm.isSupportMspt()) {
                    double mspt = sm.getMspt();
                    cachedMspt = mspt;
                    if (mspt > 0 && !Double.isNaN(mspt) && !Double.isInfinite(mspt)) {
                        cachedTps = Math.min(20.0, 1000.0 / mspt);
                        cachedAt = System.currentTimeMillis();
                        return cachedTps;
                    }
                }
                double tps = sm.getTps();
                cachedTps = tps;
                cachedAt = System.currentTimeMillis();
                return tps;
            }
        } catch (Throwable ignored) {}
        try {
            double tps = Bukkit.getTPS()[0];
            cachedTps = tps;
            cachedAt = System.currentTimeMillis();
            return tps;
        } catch (Throwable ignored) {
            return cachedTps;
        }
    }

    @Override
    public void run() {
        boolean reliableNow = SupportManager.isTpsReliableNow();
        tpsReliable = reliableNow;
        if (!reliableNow) {
            if (plugin.isDebug()) plugin.getLogger().fine("LagShield check skipped: no reliable TPS source on this server software.");
            return;
        }
        double tps = readMetric();
        long now = System.currentTimeMillis();
        boolean lowSpawn = tps < entitySpawnTps && entitySpawnTps != -1;
        boolean lowHopper = tps < hopperTps && hopperTps != -1;
        if (dv) {
            Integer v = getThreshold(viewMap, tps);
            if (v != null && !v.equals(lastView) && now - lastViewChange >= minHoldMs) {
                lastView = v;
                lastViewChange = now;
                for (World w : getAllowedWorlds()) {
                    try {
                        UUID uid = w.getUID();
                        boolean isNew = !origView.containsKey(uid);
                        origView.computeIfAbsent(uid, k -> w.getViewDistance());
                        if (isNew) persistOriginals();
                        w.setViewDistance(v);
                    } catch (Exception ex) {
                        plugin.getLogger().warning("LagShield view-distance set failed for " + w.getName() + ": " + ex.getMessage());
                    }
                }
            }
        }
        if (ds) {
            Integer s = getThreshold(simMap, tps);
            if (s != null && !s.equals(lastSim) && now - lastSimChange >= minHoldMs) {
                lastSim = s;
                lastSimChange = now;
                for (World w : getAllowedWorlds()) {
                    try {
                        UUID uid = w.getUID();
                        boolean isNew = !origSim.containsKey(uid);
                        origSim.computeIfAbsent(uid, k -> w.getSimulationDistance());
                        if (isNew) persistOriginals();
                        w.setSimulationDistance(s);
                    } catch (Exception ex) {
                        plugin.getLogger().warning("LagShield simulation-distance set failed for " + w.getName() + ": " + ex.getMessage());
                    }
                }
            }
        }
        if (dt) {
            Integer t = getThreshold(tickMap, tps);
            if (t != null && !t.equals(lastTick) && now - lastTickChange >= minHoldMs) {
                lastTick = t;
                lastTickChange = now;
                for (World w : getAllowedWorlds()) {
                    try {
                        UUID uid = w.getUID();
                        boolean isNew = !origTick.containsKey(uid);
                        origTick.computeIfAbsent(uid, k -> w.getGameRuleValue(GameRule.RANDOM_TICK_SPEED));
                        if (isNew) persistOriginals();
                        w.setGameRule(GameRule.RANDOM_TICK_SPEED, t);
                    } catch (Exception ex) {
                        plugin.getLogger().warning("LagShield tick-speed set failed for " + w.getName() + ": " + ex.getMessage());
                    }
                }
            }
        }
        if (plugin.isDebug()) plugin.getLogger().fine(String.format("LagShield check tps=%.2f mspt=%.1f lowSpawn=%b lowHopper=%b", tps, cachedMspt, lowSpawn, lowHopper));
    }

    private double getTps() {
        if (System.currentTimeMillis() - cachedAt < 1000) return cachedTps;
        return readMetric();
    }

    private Integer getThreshold(TreeMap<Double, Integer> map, double tps) {
        if (map.isEmpty()) return null;
        Map.Entry<Double, Integer> e = map.ceilingEntry(tps);
        if (e != null) return e.getValue();
        e = map.lastEntry();
        return e != null ? e.getValue() : null;
    }

    private boolean low(double threshold) {
        if (threshold == -1) return false;
        return cachedTps < threshold;
    }

    private boolean reliable() {
        return tpsReliable;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRedstone(BlockRedstoneEvent e) {
        if (e.getNewCurrent() != 0 && redstoneTps != -1 && canContinue(e.getBlock().getWorld())
                && reliable() && low(redstoneTps)) e.setNewCurrent(0);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawn(VehicleCreateEvent e) { if (entitySpawnTps != -1 && canContinue(e.getVehicle().getWorld()) && reliable() && low(entitySpawnTps)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) { if (entitySpawnTps != -1 && canContinue(e.getEntity().getWorld()) && reliable() && low(entitySpawnTps)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) { if (projectilesTps != -1 && canContinue(e.getEntity().getWorld()) && reliable() && low(projectilesTps)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent e) {
        if (hopperTps == -1) return;
        try {
            if (e.getSource().getType() != InventoryType.HOPPER) return;
            org.bukkit.Location l = null;
            try {
                org.bukkit.inventory.InventoryHolder h = e.getSource().getHolder(false);
                if (h instanceof org.bukkit.block.Hopper hop) l = hop.getLocation();
            } catch (Throwable ignored) {}
            if (l != null && l.getWorld() != null && !canContinue(l.getWorld())) return;
            if (reliable() && low(hopperTps)) e.setCancelled(true);
        } catch (Throwable ignored) {
            if (reliable() && low(hopperTps)) e.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent e) { if (leavesTps != -1 && canContinue(e.getBlock().getWorld()) && reliable() && low(leavesTps)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLiquid(BlockFromToEvent e) { if (e.getBlock().isLiquid() && liquidTps != -1 && canContinue(e.getBlock().getWorld()) && reliable() && low(liquidTps)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(BlockExplodeEvent e) { if (explosionsTps != -1 && canContinue(e.getBlock().getWorld()) && reliable() && low(explosionsTps)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFirework(FireworkExplodeEvent e) { if (fireworksTps != -1 && canContinue(e.getEntity().getWorld()) && reliable() && low(fireworksTps)) e.setCancelled(true); }

    private void loadThreshold(Map<Double, Integer> map, String key) {
        map.clear();
        for (String s : getSection().getStringList(key)) {
            try {
                String[] p = s.split(":");
                map.put(Double.parseDouble(p[0]), Integer.parseInt(p[1]));
            } catch (Exception ex) {
                plugin.getLogger().warning("LagShield ignoring bad threshold '" + s + "' in " + key);
            }
        }
    }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadPersistedOriginals();
        if (!SupportManager.isTpsReliableNow()) {
            plugin.getLogger().warning("LagShield: no reliable TPS source on this server software - throttling and dynamic distances are disabled until one is available.");
        }
        SupportManager sm = SupportManager.getInstance();
        if (sm != null && sm.isFolia() && !foliaWarned) {
            foliaWarned = true;
            plugin.getLogger().warning("LagShield: Folia detected - dynamic view/simulation/tick changes are skipped (global scheduler cannot touch region data). Event throttles still apply.");
        }
        readMetric();
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
        preferMspt = getSection().getBoolean("prefer_mspt", true);
        minHoldMs = Math.max(5000L, getSection().getLong("hysteresis.min_hold_seconds", 60) * 1000L);
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
                plugin.getLogger().warning("LagShield task cancel failed: " + ex.getMessage());
            }
            task = null;
        }
        restoreOriginals();
    }

    private void restoreOriginals() {
        if (origView.isEmpty() && origSim.isEmpty() && origTick.isEmpty()) return;
        for (World w : Bukkit.getWorlds()) {
            Integer v = origView.remove(w.getUID());
            if (v != null) {
                try {
                    w.setViewDistance(v);
                } catch (Exception ex) {
                    plugin.getLogger().warning("LagShield view-distance restore failed for " + w.getName() + ": " + ex.getMessage());
                }
            }
            Integer s = origSim.remove(w.getUID());
            if (s != null) {
                try {
                    w.setSimulationDistance(s);
                } catch (Exception ex) {
                    plugin.getLogger().warning("LagShield simulation-distance restore failed for " + w.getName() + ": " + ex.getMessage());
                }
            }
            Integer t = origTick.remove(w.getUID());
            if (t != null) {
                try {
                    w.setGameRule(GameRule.RANDOM_TICK_SPEED, t);
                } catch (Exception ex) {
                    plugin.getLogger().warning("LagShield tick-speed restore failed for " + w.getName() + ": " + ex.getMessage());
                }
            }
        }
        origView.clear();
        origSim.clear();
        origTick.clear();
        try {
            File f = originalsFile();
            if (f.isFile() && !f.delete()) {
                plugin.getLogger().warning("LagShield could not delete persisted originals file.");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("LagShield originals cleanup failed: " + t.getMessage());
        }
        lastView = null;
        lastSim = null;
        lastTick = null;
    }

    double getCachedTps() { return cachedTps; }
    double getCachedMspt() { return cachedMspt; }
}

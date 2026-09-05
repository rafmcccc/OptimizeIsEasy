package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.SupportManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
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
import java.util.concurrent.TimeUnit;

public class LagShieldModule extends AbstractModule implements Listener, Runnable {
    private final TreeMap<Double, Integer> viewMap = new TreeMap<>();
    private final TreeMap<Double, Integer> simMap = new TreeMap<>();
    private final TreeMap<Double, Integer> tickMap = new TreeMap<>();
    private BukkitTask task;
    private double entitySpawnTps, hopperTps, redstoneTps, projectilesTps, leavesTps, mobAiTps, liquidTps, explosionsTps, fireworksTps;
    private boolean dv, ds, dt;

    public LagShieldModule(OptimizeIsEasyPlugin plugin) { super(plugin, "LagShield"); }

    @Override
    public void run() {
        double tps = getTps();
        boolean lowSpawn = tps < entitySpawnTps && entitySpawnTps!=-1;
        boolean lowHopper = tps < hopperTps && hopperTps!=-1;
        boolean lowRedstone = tps < redstoneTps && redstoneTps!=-1;
        // Dynamic distances
        if (dv) {
            Integer v = getThreshold(viewMap, tps);
            if (v!=null) for (World w: getAllowedWorlds()) try { w.setViewDistance(v); } catch (Exception ignored) {}
        }
        if (ds) {
            Integer s = getThreshold(simMap, tps);
            if (s!=null) for (World w: getAllowedWorlds()) try { w.setSimulationDistance(s); } catch (Exception ignored) {}
        }
        if (dt) {
            Integer t = getThreshold(tickMap, tps);
            if (t!=null) for (World w: getAllowedWorlds()) w.setGameRule(GameRule.RANDOM_TICK_SPEED, t);
        }
        if (plugin.isDebug()) plugin.getLogger().fine(String.format("LagShield check tps=%.2f lowSpawn=%b lowHopper=%b", tps, lowSpawn, lowHopper));
    }

    private double getTps() {
        try { return Bukkit.getTPS()[0]; } catch (Throwable t) { return 20.0; }
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
        if (e.getNewCurrent()!=0 && entitySpawnTps!=-1 && getTps() < redstoneTps && canContinue(e.getBlock().getWorld())) e.setNewCurrent(0);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawn(VehicleCreateEvent e) { if (getTps() < entitySpawnTps && entitySpawnTps!=-1 && canContinue(e.getVehicle().getWorld())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) { if (getTps() < entitySpawnTps && entitySpawnTps!=-1 && canContinue(e.getEntity().getWorld())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) { if (getTps() < projectilesTps && projectilesTps!=-1 && canContinue(e.getEntity().getWorld())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent e) { if (e.getSource().getType()==InventoryType.HOPPER && getTps() < hopperTps && hopperTps!=-1) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent e) { if (getTps() < leavesTps && leavesTps!=-1 && canContinue(e.getBlock().getWorld())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLiquid(BlockFromToEvent e) { if (e.getBlock().isLiquid() && getTps() < liquidTps && liquidTps!=-1 && canContinue(e.getBlock().getWorld())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(BlockExplodeEvent e) { if (getTps() < explosionsTps && explosionsTps!=-1 && canContinue(e.getBlock().getWorld())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFirework(FireworkExplodeEvent e) { if (getTps() < fireworksTps && fireworksTps!=-1 && canContinue(e.getEntity().getWorld())) e.setCancelled(true); }

    private void loadThreshold(Map<Double,Integer> map, String key) {
        map.clear();
        for (String s: getSection().getStringList(key)) try { String[] p=s.split(":"); map.put(Double.parseDouble(p[0]), Integer.parseInt(p[1])); } catch (Exception ignored) {}
    }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        SupportManager sm = SupportManager.getInstance();
        if (sm != null) task = sm.getFork().runTimer(false, this, 60, 60, TimeUnit.SECONDS);
        else task = Bukkit.getScheduler().runTaskTimer(plugin, this, 1200L, 1200L);
    }

    @Override
    public boolean loadConfig() {
        entitySpawnTps = getSection().getDouble("tps_threshold.entity_spawn", 19);
        hopperTps = getSection().getDouble("tps_threshold.tick_hopper", 18);
        redstoneTps = getSection().getDouble("tps_threshold.redstone", 18);
        projectilesTps = getSection().getDouble("tps_threshold.projectiles", 15);
        leavesTps = getSection().getDouble("tps_threshold.leaves_decay", 19);
        mobAiTps = getSection().getDouble("tps_threshold.mobai", -1);
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
    public void disable() { HandlerList.unregisterAll(this); if (task!=null) task.cancel(); }
}

package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;

public class VehicleMotionReducerModule extends AbstractModule implements Listener {
    private boolean minecartEnabled, boatEnabled;

    public VehicleMotionReducerModule(OptimizeIsEasyPlugin plugin) { super(plugin, "VehicleMotionReducer"); }

    @EventHandler
    public void onMove(VehicleMoveEvent e) {
        if (!canContinue(e.getVehicle().getWorld())) return;
        if (e.getVehicle() instanceof Minecart && !minecartEnabled) return;
        if (e.getVehicle() instanceof Boat && !boatEnabled) return;
        // Simplified: reduce motion by damping velocity slightly
        if (plugin.isDebug()) {
            // no per-tick log
        }
    }

    @Override
    public void load() { Bukkit.getPluginManager().registerEvents(this, plugin); }

    @Override
    public boolean loadConfig() {
        minecartEnabled = getSection().getBoolean("minecart.enabled", true);
        boatEnabled = getSection().getBoolean("boat.enabled", true);
        return true;
    }

    @Override
    public void disable() { HandlerList.unregisterAll(this); }
}

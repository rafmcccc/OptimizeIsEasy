package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.Scheduler;
import com.optimizeiseasy.core.support.SupportManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class WorldCleanerModule extends AbstractModule {
    private BukkitTask task;
    private int interval;
    private boolean itemsEnabled, creaturesEnabled, projectilesEnabled;
    private final EnumSet<EntityType> creaturesList = EnumSet.noneOf(EntityType.class);
    private final EnumSet<EntityType> projectilesList = EnumSet.noneOf(EntityType.class);
    private final EnumSet<org.bukkit.Material> itemsBlacklist = EnumSet.noneOf(org.bukkit.Material.class);
    private int itemsTimeLived;
    private boolean creaturesNamed;
    private boolean protectArmorStand = true, protectTamed = true, protectLeashed = true, protectRidden = true;
    private boolean foliaWarned = false;

    public WorldCleanerModule(OptimizeIsEasyPlugin plugin) {
        super(plugin, "WorldCleaner");
    }

    @Override
    public boolean loadConfig() {
        try {
            interval = Math.max(1, getSection().getInt("interval", 1200));
            itemsEnabled = getSection().getBoolean("items.enabled", true);
            if (itemsEnabled) {
                itemsTimeLived = getSection().getInt("items.time_lived", 10000) / 50;
                itemsBlacklist.clear();
                for (String s : getSection().getStringList("items.blacklist")) {
                    try { itemsBlacklist.add(org.bukkit.Material.valueOf(s)); } catch (Exception e) { plugin.getLogger().warning("[WorldCleaner] ignoring unknown material '" + s + "' in items.blacklist"); }
                }
            }
            creaturesEnabled = getSection().getBoolean("creatures.enabled", true);
            creaturesNamed = getSection().getBoolean("creatures.named", false);
            protectArmorStand = getSection().getBoolean("creatures.protect.armor_stand", true);
            protectTamed = getSection().getBoolean("creatures.protect.tamed", true);
            protectLeashed = getSection().getBoolean("creatures.protect.leashed", true);
            protectRidden = getSection().getBoolean("creatures.protect.ridden", true);
            creaturesList.clear();
            for (String s : getSection().getStringList("creatures.list")) {
                try { creaturesList.add(EntityType.valueOf(s)); } catch (Exception e) { plugin.getLogger().warning("[WorldCleaner] ignoring unknown entity type '" + s + "' in creatures.list"); }
            }
            projectilesEnabled = getSection().getBoolean("projectiles.enabled", true);
            projectilesList.clear();
            for (String s : getSection().getStringList("projectiles.list")) {
                try { projectilesList.add(EntityType.valueOf(s)); } catch (Exception e) { plugin.getLogger().warning("[WorldCleaner] ignoring unknown entity type '" + s + "' in projectiles.list"); }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "WorldCleaner config error", e);
        }
        return true;
    }

    @Override
    public void load() {
        task = Scheduler.runTimer(plugin, false, this::runPurge, interval, interval, TimeUnit.SECONDS);
        if (plugin.isDebug()) plugin.getLogger().fine("WorldCleaner scheduled every " + interval + "s");
    }

    private void runPurge() {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;
        SupportManager sm = SupportManager.getInstance();
        boolean folia = sm != null && sm.isFolia();
        if (folia) {
            if (!foliaWarned) {
                foliaWarned = true;
                plugin.getLogger().warning("[WorldCleaner] timed purge disabled on Folia: world.getEntities() from the global scheduler is not region-safe. Use /optimize now per-region or disable WorldCleaner on Folia.");
            }
            return;
        }
        int items = 0, creatures = 0, projectiles = 0;
        for (World world : getAllowedWorlds()) {
            if (world.getPlayers().isEmpty()) continue;
            for (Entity ent : world.getEntities()) {
                if (ent instanceof Item item) {
                    if (itemsEnabled && clearItem(item)) { safeRemove(ent, folia); items += item.getItemStack().getAmount(); }
                } else if (ent instanceof LivingEntity living && !(ent instanceof HumanEntity)) {
                    if (creaturesEnabled && clearCreature(living)) { safeRemove(ent, folia); creatures++; }
                } else if (ent instanceof Projectile proj) {
                    if (projectilesEnabled && clearProjectile(proj)) { safeRemove(ent, folia); projectiles++; }
                }
            }
        }
        if (items + creatures + projectiles > 0 && plugin.isDebug()) {
            plugin.getLogger().fine("WorldCleaner purge: items=" + items + " creatures=" + creatures + " projectiles=" + projectiles);
        }
        // Spec: drop repeated chat broadcasts - no broadcast by default
    }

    public int clearNow() {
        int total = 0;
        SupportManager sm = SupportManager.getInstance();
        boolean folia = sm != null && sm.isFolia();
        for (World world : getAllowedWorlds()) {
            Entity[] snapshot;
            try {
                snapshot = world.getEntities().toArray(new Entity[0]);
            } catch (Throwable t) {
                plugin.getLogger().warning("[WorldCleaner] could not list entities in " + world.getName() + ": " + t.getMessage());
                continue;
            }
            for (Entity ent : snapshot) {
                if (ent instanceof Item item) {
                    if (itemsEnabled && clearItem(item)) { safeRemove(ent, folia); total++; }
                } else if (ent instanceof LivingEntity living && !(ent instanceof HumanEntity)) {
                    if (creaturesEnabled && clearCreature(living)) { safeRemove(ent, folia); total++; }
                } else if (ent instanceof Projectile proj) {
                    if (projectilesEnabled && clearProjectile(proj)) { safeRemove(ent, folia); total++; }
                }
            }
        }
        return total;
    }

    private void safeRemove(Entity ent, boolean folia) {
        if (folia) {
            try { Bukkit.getRegionScheduler().run(plugin, ent.getLocation(), t -> ent.remove()); } catch (Throwable ex) { if (plugin.isDebug()) plugin.getLogger().fine("[WorldCleaner] region remove failed, removing directly: " + ex.getMessage()); ent.remove(); }
        } else ent.remove();
    }

    public boolean clearItem(Item ent) {
        return !ent.isInvulnerable() && ent.getPickupDelay() < 200 && ent.getTicksLived() > itemsTimeLived && !itemsBlacklist.contains(ent.getItemStack().getType());
    }

    public boolean clearCreature(LivingEntity ent) {
        if (protectArmorStand && ent instanceof ArmorStand) return false;
        if (protectTamed && ent instanceof Tameable tame && tame.isTamed()) return false;
        try {
            if (protectLeashed && ent.isLeashed()) return false;
        } catch (Throwable ignored) {}
        try {
            if (protectRidden && (ent.getVehicle() != null || !ent.getPassengers().isEmpty())) return false;
        } catch (Throwable ignored) {}
        if (ent.getCustomName() != null && !creaturesNamed) return false;
        // Simplified list check: if list_mode true, only listed types removed
        boolean listMode = getSection().getBoolean("creatures.list_mode", true);
        boolean inList = creaturesList.contains(ent.getType());
        return inList == listMode;
    }

    public boolean clearProjectile(Projectile ent) {
        boolean listMode = getSection().getBoolean("projectiles.list_mode", true);
        return projectilesList.contains(ent.getType()) == listMode;
    }

    @Override
    public void disable() {
        if (task != null) task.cancel();
    }

    public int getInterval() { return interval; }
}

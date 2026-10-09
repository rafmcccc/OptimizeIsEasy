package com.optimizeiseasy.core.modules;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;

public final class EntityProtection {
    private EntityProtection() {}

    public static boolean isProtected(LivingEntity ent, boolean armorStand, boolean tamed, boolean leashed, boolean ridden) {
        if (armorStand && ent instanceof ArmorStand) return true;
        if (tamed && ent instanceof Tameable tame) {
            try {
                if (tame.isTamed()) return true;
            } catch (Throwable ignored) {}
        }
        if (leashed) {
            try {
                if (ent.isLeashed()) return true;
            } catch (Throwable ignored) {}
        }
        if (ridden) {
            try {
                if (ent.getVehicle() != null || !ent.getPassengers().isEmpty()) return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }
}

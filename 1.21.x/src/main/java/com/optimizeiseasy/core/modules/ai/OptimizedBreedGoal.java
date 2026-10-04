package com.optimizeiseasy.core.modules.ai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Clean-room replacement for the vanilla breed goal.
 *
 * <p>Ideas adapted from lagfixer's OptimizedBreedGoal (GPL-3.0, by lajczik) but
 * reimplemented against the public Bukkit/Paper API: instead of the vanilla
 * goal's constant partner scan, this goal only runs while the mob is in love
 * mode and mates the nearest in-love adult of the same type, spawning the baby
 * through the Bukkit API so {@code CreatureSpawnEvent} still fires for
 * protection plugins. No NMS is used.</p>
 */
public class OptimizedBreedGoal implements Goal<Animals> {

    /** Squared distance at which mating completes. */
    static final double MATE_DIST_SQ = 9.0d;

    /**
     * Egg-laying species are excluded: their vanilla goal produces eggs/frogspawn,
     * which a generic baby-spawn cannot replicate. Their vanilla breed goal is kept.
     */
    static final EnumSet<EntityType> EGG_LAYERS =
            EnumSet.of(EntityType.TURTLE, EntityType.FROG, EntityType.SNIFFER);

    private static final GoalKey<Animals> KEY =
            GoalKey.of(Animals.class, new NamespacedKey("optimizeiseasy", "optimized_breed"));

    private final Animals mob;
    private final double range;
    private final double speed;
    private final boolean fireEvent;
    private final boolean teleport;
    private final Consumer<Animals> onBaby;

    private Animals partner;

    public OptimizedBreedGoal(Animals mob, double range, double speed,
                              boolean fireEvent, boolean teleport, Consumer<Animals> onBaby) {
        this.mob = mob;
        this.range = range;
        this.speed = speed;
        this.fireEvent = fireEvent;
        this.teleport = teleport;
        this.onBaby = onBaby;
    }

    /** Whether this mob type can use the optimized breed goal at all. */
    public static boolean supports(EntityType type) {
        return !EGG_LAYERS.contains(type);
    }

    @Override
    public boolean shouldActivate() {
        if (!mob.isLoveMode()) return false;
        partner = findPartner();
        if (partner == null) return false;
        if (fireEvent) {
            EntityTargetLivingEntityEvent event = new EntityTargetLivingEntityEvent(
                    mob, partner, EntityTargetEvent.TargetReason.CUSTOM);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                partner = null;
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean shouldStayActive() {
        return partner != null && partner.isValid() && mob.isLoveMode() && partner.isLoveMode()
                && partner.getWorld().equals(mob.getWorld())
                && partner.getLocation().distanceSquared(mob.getLocation()) < range * range * 4;
    }

    @Override
    public void tick() {
        if (partner == null) return;
        if (teleport) {
            mob.teleport(partner.getLocation());
        } else {
            mob.getPathfinder().moveTo(partner, speed);
        }
        if (mob.getLocation().distanceSquared(partner.getLocation()) < MATE_DIST_SQ) {
            completeBreeding();
        }
    }

    @Override
    public void stop() {
        partner = null;
    }

    private Animals findPartner() {
        Animals nearest = null;
        double nearestSq = range * range;
        for (Entity entity : mob.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof Animals other) || other == mob) continue;
            if (other.getType() != mob.getType()) continue;
            if (!other.isAdult() || !other.isLoveMode()) continue;
            double distSq = entity.getLocation().distanceSquared(mob.getLocation());
            if (distSq < nearestSq) {
                nearestSq = distSq;
                nearest = other;
            }
        }
        return nearest;
    }

    private void completeBreeding() {
        Animals father = partner;
        mob.setLoveModeTicks(0);
        father.setLoveModeTicks(0);
        partner = null;
        mob.getPathfinder().stopPathfinding();
        try {
            Animals baby = (Animals) mob.getWorld().spawnEntity(mob.getLocation(), mob.getType());
            baby.setBaby();
            baby.setBreedCause(father.getUniqueId());
            mob.getWorld().spawn(mob.getLocation(), ExperienceOrb.class,
                    orb -> orb.setExperience(1 + ThreadLocalRandom.current().nextInt(7)));
            if (onBaby != null) onBaby.accept(baby);
        } catch (Throwable t) {
            // If the world rejects the spawn, love mode is already consumed; nothing else to do.
            Bukkit.getLogger().warning("[MobAiReducer] breeding spawn failed for " + mob.getType() + ": " + t.getMessage());
        }
    }

    @Override
    public GoalKey<Animals> getKey() {
        return KEY;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }
}

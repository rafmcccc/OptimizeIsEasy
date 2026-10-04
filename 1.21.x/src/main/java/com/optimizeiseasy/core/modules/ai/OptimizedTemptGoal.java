package com.optimizeiseasy.core.modules.ai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;

/**
 * Clean-room replacement for the vanilla tempt goal.
 *
 * <p>Ideas adapted from lagfixer's OptimizedTemptGoal (GPL-3.0, by lajczik) but
 * reimplemented against the public Paper Goal API: instead of scanning with
 * line-of-sight targeting conditions every tick, the goal sleeps on a
 * configurable cooldown and only wakes up to look for a nearby player holding
 * a breeding item. No NMS is used.</p>
 */
public class OptimizedTemptGoal implements Goal<Animals> {

    /** Squared distance at which the mob stops instead of pathing closer. */
    static final double ARRIVE_DIST_SQ = 6.25d;

    private static final GoalKey<Animals> KEY =
            GoalKey.of(Animals.class, new NamespacedKey("optimizeiseasy", "optimized_tempt"));

    private final Animals mob;
    private final double range;
    private final double speed;
    private final int cooldownTicks;
    private final boolean bothHands;
    private final boolean fireEvent;
    private final boolean teleport;

    private int cooldown;
    private Player target;

    public OptimizedTemptGoal(Animals mob, double range, double speed, int cooldownTicks,
                              boolean bothHands, boolean fireEvent, boolean teleport) {
        this.mob = mob;
        this.range = range;
        this.speed = speed;
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.bothHands = bothHands;
        this.fireEvent = fireEvent;
        this.teleport = teleport;
    }

    @Override
    public boolean shouldActivate() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        target = findTemptingPlayer();
        if (target == null) return false;
        if (fireEvent) {
            EntityTargetLivingEntityEvent event = new EntityTargetLivingEntityEvent(
                    mob, target, EntityTargetEvent.TargetReason.TEMPT);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                target = null;
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean shouldStayActive() {
        return target != null && target.isValid()
                && target.getWorld().equals(mob.getWorld())
                && target.getLocation().distanceSquared(mob.getLocation()) < range * range * 4
                && isHoldingFood(target);
    }

    @Override
    public void tick() {
        if (target == null) return;
        if (teleport) {
            mob.teleport(target.getLocation());
        } else if (mob.getLocation().distanceSquared(target.getLocation()) >= ARRIVE_DIST_SQ) {
            mob.getPathfinder().moveTo(target, speed);
        } else {
            mob.getPathfinder().stopPathfinding();
        }
        cooldown = cooldownTicks;
    }

    @Override
    public void stop() {
        target = null;
    }

    private Player findTemptingPlayer() {
        Player nearest = null;
        double nearestSq = range * range;
        for (Entity entity : mob.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof Player player)) continue;
            if (!isHoldingFood(player)) continue;
            double distSq = entity.getLocation().distanceSquared(mob.getLocation());
            if (distSq < nearestSq) {
                nearestSq = distSq;
                nearest = player;
            }
        }
        return nearest;
    }

    private boolean isHoldingFood(Player player) {
        if (isFood(player.getInventory().getItemInMainHand())) return true;
        return bothHands && isFood(player.getInventory().getItemInOffHand());
    }

    private boolean isFood(ItemStack item) {
        return item != null && !item.getType().isAir() && mob.isBreedItem(item);
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

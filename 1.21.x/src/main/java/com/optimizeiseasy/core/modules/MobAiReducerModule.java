package com.optimizeiseasy.core.modules;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.MobGoals;
import com.destroystokyo.paper.entity.ai.VanillaGoal;
import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.modules.ai.OptimizedBreedGoal;
import com.optimizeiseasy.core.modules.ai.OptimizedTemptGoal;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Flying;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Replaces expensive vanilla mob goals with cheaper equivalents.
 *
 * <p>Behavior adapted from lagfixer's MobAiReducer (GPL-3.0, by lajczik),
 * reimplemented clean-room against the public Paper Goal API so no NMS
 * toolchain is needed and Folia region threading stays correct. Per entity
 * the module applies the {@code collides}/{@code silent} flags, strips goals
 * matched by the {@code pathfinder.list} phrases (keeping non-vanilla goals
 * when {@code keep_dedicated} is set), and swaps vanilla tempt/breed goals
 * for cooldown-gated {@link OptimizedTemptGoal}/{@link OptimizedBreedGoal}
 * versions.</p>
 */
public class MobAiReducerModule extends AbstractModule implements Listener {

    /** Namespace used for goals added by this module. Never stripped as vanilla. */
    static final String OWN_NAMESPACE = "optimizeiseasy";

    private final EnumSet<CreatureSpawnEvent.SpawnReason> reasons = EnumSet.noneOf(CreatureSpawnEvent.SpawnReason.class);
    private final EnumSet<EntityType> list = EnumSet.noneOf(EntityType.class);
    private final Set<String> aiList = new HashSet<>();
    private final Set<Mob> optimized =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private BukkitTask purgeTask;
    private boolean async;
    private boolean ignoreModels;
    private boolean modelEnginePresent;
    private boolean mythicMobsPresent;
    private boolean animals;
    private boolean monsters;
    private boolean villagers;
    private boolean tameable;
    private boolean birds;
    private boolean others;
    private boolean listMode;
    private boolean forceLoad;
    private boolean clickEvent;
    private boolean collides = true;
    private boolean silent;
    private boolean keepDedicated = true;
    private boolean aiListMode;
    private int purgeInterval = 30;

    private boolean temptEnabled = true;
    private double temptRange = 6.25d;
    private double temptSpeed = 1.25d;
    private int temptCooldown = 30;
    private boolean temptBothHands = true;
    private boolean temptEvent;
    private boolean temptTeleport;

    private boolean breedEnabled = true;
    private double breedRange = 5.0d;
    private double breedSpeed = 1.0d;
    private boolean breedEvent;
    private boolean breedTeleport;

    public MobAiReducerModule(OptimizeIsEasyPlugin plugin) { super(plugin, "MobAiReducer"); }

    /**
     * Phrase-list matching, mirroring upstream semantics: when {@code listMode} is
     * false every goal containing a listed phrase is removed, when true only listed
     * goals are removed. Both sides must already be lower-cased.
     */
    static boolean matchesAiList(String haystackLower, Set<String> phrasesLower, boolean listMode) {
        boolean hit = false;
        for (String phrase : phrasesLower) {
            if (!phrase.isEmpty() && haystackLower.contains(phrase)) {
                hit = true;
                break;
            }
        }
        // listMode=false removes every goal NOT on the list; listMode=true removes
        // only goals on the list. Either way a listed goal is removed iff listMode.
        return hit == listMode;
    }

    /** Whether this entity looks like a ModelEngine/MythicMobs custom entity. */
    static boolean looksCustom(Entity entity) {
        try {
            if (!entity.getPersistentDataContainer().isEmpty()) return true;
        } catch (Throwable ignored) {
            // PDC access can fail on exotic entity wrappers; fall through to tags.
        }
        try {
            return !entity.getScoreboardTags().isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean isEnabled(Entity entity) {
        if (entity == null) return false;
        if (ignoreModels && (modelEnginePresent || mythicMobsPresent) && looksCustom(entity)) return false;
        if (list.contains(entity.getType()) != listMode) return false;
        if (entity instanceof Villager) return villagers;
        if (entity instanceof Tameable) return tameable;
        if (entity instanceof Flying) return birds;
        if (entity instanceof Animals) return animals;
        if (entity instanceof Monster) return monsters;
        return others;
    }

    /** Whether this mob was already optimized by this module. */
    public boolean isOptimized(Mob mob) {
        return optimized.contains(mob);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSpawn(CreatureSpawnEvent e) {
        if (!reasons.contains(e.getSpawnReason())) return;
        if (!isEnabled(e.getEntity())) return;
        if (!canContinue(e.getEntity().getWorld())) return;
        if (async) {
            runOnEntityThread(e.getEntity(), () -> optimize(e.getEntity(), false));
        } else {
            optimize(e.getEntity(), false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChunkLoad(EntitiesLoadEvent e) {
        if (!canContinue(e.getWorld())) return;
        optimizeEntities(e.getEntities());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!clickEvent) return;
        Entity clicked = e.getRightClicked();
        if (!(clicked instanceof Mob mob) || !optimized.contains(mob)) return;
        if (!canContinue(clicked.getWorld())) return;
        Player player = e.getPlayer();
        int goals = countGoals(mob);
        player.sendMessage("§7[§a✓§8] §f" + mob.getType()
                + " §7has optimized AI §8(§7" + goals + " goals active§8)");
        if (plugin.isDebug()) {
            plugin.getLogger().fine("MobAiReducer click info for " + mob.getType() + " (" + goals + " goals)");
        }
    }

    /**
     * Optimizes one entity: applies the collides/silent flags and performs goal
     * surgery. Safe to call repeatedly; already-optimized mobs are skipped.
     * Must run on the entity's region thread (callers schedule it there).
     */
    public void optimize(Entity entity, boolean init) {
        if (!(entity instanceof Mob mob)) return;
        if (!optimized.add(mob)) return;
        try {
            mob.setCollidable(collides);
        } catch (Throwable ignored) {
            // setCollidable is best-effort on exotic forks.
        }
        try {
            mob.setSilent(silent);
        } catch (Throwable ignored) {
            // Same: never let a cosmetic flag break optimization.
        }
        stripGoals(mob);
    }

    public void optimizeEntities(List<Entity> entities) {
        for (Entity entity : entities) {
            if (!isEnabled(entity)) continue;
            if (!canContinue(entity.getWorld())) continue;
            if (async) {
                runOnEntityThread(entity, () -> optimize(entity, false));
            } else {
                optimize(entity, false);
            }
        }
    }

    /** Drops dead entries from the optimized cache. Never removes world entities. */
    public void purge() {
        try {
            synchronized (optimized) {
                optimized.removeIf(mob -> !mob.isValid());
            }
        } catch (Throwable ignored) {
            // Purge is best-effort; the WeakHashMap still self-cleans.
        }
    }

    private void stripGoals(Mob mob) {
        MobGoals api;
        List<Goal<Mob>> snapshot;
        try {
            api = Bukkit.getMobGoals();
            snapshot = new ArrayList<>(api.getAllGoals(mob));
        } catch (Throwable t) {
            return;
        }
        for (Goal<Mob> goal : snapshot) {
            NamespacedKey key;
            try {
                key = goal.getKey().getNamespacedKey();
            } catch (Throwable t) {
                continue;
            }
            if (key == null) continue;
            if (OWN_NAMESPACE.equalsIgnoreCase(key.getNamespace())) continue;

            boolean vanilla = "minecraft".equalsIgnoreCase(key.getNamespace());
            if (vanilla && breedEnabled && mob instanceof Animals animal
                    && OptimizedBreedGoal.supports(animal.getType())
                    && key.equals(VanillaGoal.BREED.getNamespacedKey())) {
                replaceGoal(api, mob, goal, 2, new OptimizedBreedGoal(animal, breedRange, breedSpeed,
                        breedEvent, breedTeleport, baby -> optimize(baby, false)));
                continue;
            }
            if (vanilla && temptEnabled && mob instanceof Animals animal
                    && key.getKey().contains("tempt")) {
                replaceGoal(api, mob, goal, 3, new OptimizedTemptGoal(animal, temptRange, temptSpeed,
                        temptCooldown, temptBothHands, temptEvent, temptTeleport));
                continue;
            }
            // keep_dedicated: custom (non-vanilla) goals belong to other plugins, keep them.
            if (keepDedicated && !vanilla) continue;

            String haystack = (key + " " + goal.getClass().getSimpleName()).toLowerCase(Locale.ROOT);
            if (matchesAiList(haystack, aiList, aiListMode)) {
                try {
                    goal.stop();
                } catch (Throwable ignored) {
                    // A stuck goal must not block the rest of the surgery.
                }
                try {
                    api.removeGoal(mob, goal);
                } catch (Throwable ignored) {
                    // Already gone or concurrently modified; move on.
                }
            }
        }
    }

    private void replaceGoal(MobGoals api, Mob mob, Goal<Mob> oldGoal, int priority, Goal<Animals> replacement) {
        try {
            oldGoal.stop();
        } catch (Throwable ignored) {
            // See stripGoals: never fail the whole pass on one goal.
        }
        try {
            api.removeGoal(mob, oldGoal);
        } catch (Throwable ignored) {
            // Already gone; still add the replacement below.
        }
        try {
            if (mob instanceof Animals animal) {
                api.addGoal(animal, priority, replacement);
            }
        } catch (Throwable t) {
            if (plugin.isDebug()) {
                plugin.getLogger().fine("MobAiReducer could not add replacement goal: " + t.getMessage());
            }
        }
    }

    private int countGoals(Mob mob) {
        try {
            return Bukkit.getMobGoals().getAllGoals(mob).size();
        } catch (Throwable t) {
            return -1;
        }
    }

    private void runOnEntityThread(Entity entity, Runnable task) {
        Location loc;
        try {
            loc = entity.getLocation();
        } catch (Throwable t) {
            if (plugin.isDebug()) plugin.getLogger().fine("MobAiReducer location lookup failed: " + t.getMessage());
            loc = null;
        }
        // NB: sync (not raw-async) so entity mutation stays on the region thread
        // on Folia/Paper and on the main thread on Spigot. The `async` flag only
        // controls whether the work is deferred through the scheduler at all.
        BukkitTask scheduled = Scheduler.runNow(plugin, false, loc, task);
        if (scheduled != null) return;
        // Scheduler unavailable (e.g. disabling); run inline as last resort.
        try {
            task.run();
        } catch (Throwable t) {
            if (plugin.isDebug()) plugin.getLogger().fine("MobAiReducer inline task failed: " + t.getMessage());
        }
    }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (forceLoad) {
            for (World world : getAllowedWorlds()) {
                Location at;
                try {
                    at = world.getSpawnLocation();
                } catch (Throwable t) {
                    if (plugin.isDebug()) plugin.getLogger().fine("MobAiReducer spawn lookup failed: " + t.getMessage());
                    continue;
                }
                Location anchor = at;
                BukkitTask scheduled = Scheduler.runNow(plugin, false, anchor, () -> {
                    for (LivingEntity entity : world.getLivingEntities()) {
                        if (isEnabled(entity)) optimize(entity, true);
                    }
                });
                if (scheduled == null && plugin.isDebug()) {
                    plugin.getLogger().fine("MobAiReducer force-load scan skipped for " + world.getName());
                }
            }
        }
        try {
            purgeTask = Scheduler.runTimer(plugin, true, this::purge,
                    60, Math.max(1, purgeInterval), TimeUnit.SECONDS);
        } catch (Throwable t) {
            if (plugin.isDebug()) plugin.getLogger().fine("MobAiReducer purge task failed: " + t.getMessage());
            purgeTask = null;
        }
    }

    @Override
    public boolean loadConfig() {
        if (getSection() == null) return false;
        reasons.clear();
        for (String s : getSection().getStringList("spawn_reasons")) {
            try {
                reasons.add(CreatureSpawnEvent.SpawnReason.valueOf(s));
            } catch (Exception ignored) {
                // Unknown reason names are skipped, never fatal.
            }
        }
        async = getSection().getBoolean("async", true);
        ignoreModels = getSection().getBoolean("ignore_models", true);
        animals = getSection().getBoolean("entities.animals", true);
        monsters = getSection().getBoolean("entities.monsters", true);
        villagers = getSection().getBoolean("entities.villagers", false);
        tameable = getSection().getBoolean("entities.tameable", false);
        birds = getSection().getBoolean("entities.birds", false);
        others = getSection().getBoolean("entities.others", true);
        forceLoad = getSection().getBoolean("force_load", true);
        clickEvent = getSection().getBoolean("click_event", true);
        purgeInterval = Math.max(1, getSection().getInt("purge_interval", 30));
        listMode = getSection().getBoolean("list_mode", false);
        list.clear();
        for (String s : getSection().getStringList("list")) {
            try {
                list.add(EntityType.valueOf(s));
            } catch (Exception ignored) {
                // Unknown entity names are skipped, never fatal.
            }
        }
        collides = getSection().getBoolean("collides", true);
        silent = getSection().getBoolean("silent", false);

        keepDedicated = getSection().getBoolean("pathfinder.keep_dedicated", true);
        aiListMode = getSection().getBoolean("pathfinder.list_mode", false);
        aiList.clear();
        for (String s : getSection().getStringList("pathfinder.list")) {
            if (s != null && !s.isBlank()) aiList.add(s.toLowerCase(Locale.ROOT));
        }

        temptEnabled = getSection().getBoolean("animals.tempt.enabled", true);
        temptRange = getSection().getDouble("animals.tempt.range", 6.25d);
        temptSpeed = getSection().getDouble("animals.tempt.speed", 1.25d);
        temptCooldown = Math.max(0, getSection().getInt("animals.tempt.cooldown", 30));
        temptBothHands = getSection().getBoolean("animals.tempt.trigger_both_hands", true);
        temptEvent = getSection().getBoolean("animals.tempt.event", false);
        temptTeleport = getSection().getBoolean("animals.tempt.teleport", false);

        breedEnabled = getSection().getBoolean("animals.breed.enabled", true);
        breedRange = getSection().getDouble("animals.breed.range", 5.0d);
        breedSpeed = getSection().getDouble("animals.breed.speed", 1.0d);
        breedEvent = getSection().getBoolean("animals.breed.event", false);
        breedTeleport = getSection().getBoolean("animals.breed.teleport", false);

        try {
            modelEnginePresent = Bukkit.getPluginManager().getPlugin("ModelEngine") != null;
            mythicMobsPresent = Bukkit.getPluginManager().getPlugin("MythicMobs") != null;
        } catch (Throwable t) {
            // No server running (unit tests): assume no model plugins.
            modelEnginePresent = false;
            mythicMobsPresent = false;
        }
        return true;
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (purgeTask != null) {
            try {
                purgeTask.cancel();
            } catch (Throwable ignored) {
                // Already cancelled or scheduler gone.
            }
            purgeTask = null;
        }
        synchronized (optimized) {
            optimized.clear();
        }
    }

    // Getters for tests and diagnostics.
    public Set<CreatureSpawnEvent.SpawnReason> getReasons() { return reasons; }
    public Set<EntityType> getList() { return list; }
    public Set<String> getAiList() { return aiList; }
    public boolean isAsync() { return async; }
    public boolean isIgnoreModels() { return ignoreModels; }
    public boolean isAnimals() { return animals; }
    public boolean isMonsters() { return monsters; }
    public boolean isVillagers() { return villagers; }
    public boolean isTameable() { return tameable; }
    public boolean isBirds() { return birds; }
    public boolean isOthers() { return others; }
    public boolean isListMode() { return listMode; }
    public boolean isForceLoad() { return forceLoad; }
    public boolean isClickEvent() { return clickEvent; }
    public boolean isCollides() { return collides; }
    public boolean isSilent() { return silent; }
    public boolean isKeepDedicated() { return keepDedicated; }
    public boolean isAiListMode() { return aiListMode; }
    public int getPurgeInterval() { return purgeInterval; }
    public boolean isTemptEnabled() { return temptEnabled; }
    public double getTemptRange() { return temptRange; }
    public double getTemptSpeed() { return temptSpeed; }
    public int getTemptCooldown() { return temptCooldown; }
    public boolean isTemptBothHands() { return temptBothHands; }
    public boolean isBreedEnabled() { return breedEnabled; }
    public double getBreedRange() { return breedRange; }
    public double getBreedSpeed() { return breedSpeed; }
}

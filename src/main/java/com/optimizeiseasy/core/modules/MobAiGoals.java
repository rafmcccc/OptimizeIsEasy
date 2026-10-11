package com.optimizeiseasy.core.modules;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.MobGoals;
import com.destroystokyo.paper.entity.ai.VanillaGoal;
import com.optimizeiseasy.core.modules.ai.OptimizedBreedGoal;
import com.optimizeiseasy.core.modules.ai.OptimizedTemptGoal;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Goal surgery for MobAiReducer. Extracted from MobAiReducerModule so the
 * module stays focused on config, events and scheduling while this file owns
 * the Paper Goal API calls.
 */
public final class MobAiGoals {

    private MobAiGoals() {}

    /** Namespace used for goals added by this module. Never stripped as vanilla. */
    public static final String OWN_NAMESPACE = "optimizeiseasy";

    /** Everything stripGoals needs from the module config, plus the newborn callback. */
    public record AiOptions(boolean breedEnabled, double breedRange, double breedSpeed,
                            boolean breedEvent, boolean breedTeleport,
                            boolean temptEnabled, double temptRange, double temptSpeed,
                            int temptCooldown, boolean temptBothHands,
                            boolean temptEvent, boolean temptTeleport,
                            boolean keepDedicated, Set<String> aiList, boolean aiListMode,
                            Consumer<Animals> onBaby) {}

    /**
     * Phrase-list matching, mirroring upstream semantics: when {@code listMode} is
     * false every goal containing a listed phrase is removed, when true only listed
     * goals are removed. Both sides must already be lower-cased.
     */
    public static boolean matchesAiList(String haystackLower, Set<String> phrasesLower, boolean listMode) {
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
    public static boolean looksCustom(Entity entity) {
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

    public static void stripGoals(Mob mob, MobGoals api, AiOptions opts, Logger log, boolean debug) {
        List<Goal<Mob>> snapshot;
        try {
            snapshot = new ArrayList<>(api.getAllGoals(mob));
        } catch (Throwable t) {
            log.warning("[MobAiReducer] AI snapshot failed for " + mob.getType() + ", skipping: " + t.getMessage());
            return;
        }
        for (Goal<Mob> goal : snapshot) {
            NamespacedKey key;
            try {
                key = goal.getKey().getNamespacedKey();
            } catch (Throwable t) {
                log.warning("[MobAiReducer] unreadable goal on " + mob.getType() + ", skipping it: " + t.getMessage());
                continue;
            }
            if (key == null) continue;
            if (OWN_NAMESPACE.equalsIgnoreCase(key.getNamespace())) continue;

            boolean vanilla = "minecraft".equalsIgnoreCase(key.getNamespace());
            if (vanilla && opts.breedEnabled() && mob instanceof Animals animal
                    && OptimizedBreedGoal.supports(animal.getType())
                    && key.equals(VanillaGoal.BREED.getNamespacedKey())) {
                replaceGoal(api, mob, goal, 2, new OptimizedBreedGoal(animal, opts.breedRange(), opts.breedSpeed(),
                        opts.breedEvent(), opts.breedTeleport(), opts.onBaby()), log, debug);
                continue;
            }
            if (vanilla && opts.temptEnabled() && mob instanceof Animals animal
                    && key.getKey().contains("tempt")) {
                replaceGoal(api, mob, goal, 3, new OptimizedTemptGoal(animal, opts.temptRange(), opts.temptSpeed(),
                        opts.temptCooldown(), opts.temptBothHands(), opts.temptEvent(), opts.temptTeleport()), log, debug);
                continue;
            }
            // keep_dedicated: custom (non-vanilla) goals belong to other plugins, keep them.
            if (opts.keepDedicated() && !vanilla) continue;

            String haystack = (key + " " + goal.getClass().getSimpleName()).toLowerCase(Locale.ROOT);
            if (matchesAiList(haystack, opts.aiList(), opts.aiListMode())) {
                try {
                    goal.stop();
                } catch (Throwable ignored) {
                    // A stuck goal must not block the rest of the surgery.
                }
                try {
                    api.removeGoal(mob, goal);
                } catch (Throwable t) {
                    // Already gone or concurrently modified; move on.
                    log.warning("[MobAiReducer] could not remove goal from " + mob.getType() + ": " + t.getMessage());
                }
            }
        }
    }

    private static void replaceGoal(MobGoals api, Mob mob, Goal<Mob> oldGoal, int priority,
                                    Goal<Animals> replacement, Logger log, boolean debug) {
        try {
            oldGoal.stop();
        } catch (Throwable ignored) {
            // See stripGoals: never fail the whole pass on one goal.
        }
        try {
            api.removeGoal(mob, oldGoal);
        } catch (Throwable t) {
            // Already gone; still add the replacement below.
            log.warning("[MobAiReducer] could not remove old goal from " + mob.getType() + ": " + t.getMessage());
        }
        try {
            if (mob instanceof Animals animal) {
                api.addGoal(animal, priority, replacement);
            }
        } catch (Throwable t) {
            if (debug) {
                log.fine("MobAiReducer could not add replacement goal: " + t.getMessage());
            }
        }
    }

    public static int countGoals(Mob mob, Logger log, boolean debug) {
        try {
            return Bukkit.getMobGoals().getAllGoals(mob).size();
        } catch (Throwable t) {
            if (debug) log.fine("[MobAiReducer] goal count failed: " + t.getMessage());
            return -1;
        }
    }

    public static MobGoals goalsApi() {
        return Bukkit.getMobGoals();
    }
}

package com.optimizeiseasy.core.utils;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class BadPluginDetector {

    /** A known conflicting plugin: why it hurts performance and what to use instead. */
    public record Conflict(String name, String why, String instead) {}

    private static final List<Conflict> KNOWN_BAD = List.of(
            // Mass cleaners: overlap WorldCleaner and their purge ticks spike MSPT.
            new Conflict("ClearLag", "scheduled mass purges overlap WorldCleaner and spike MSPT",
                    "WorldCleaner (/optimize now)"),
            new Conflict("EntityClearer", "scheduled mass purges overlap WorldCleaner and spike MSPT",
                    "WorldCleaner (/optimize now)"),
            new Conflict("AutoClear", "scheduled mass purges overlap WorldCleaner and spike MSPT",
                    "WorldCleaner (/optimize now)"),
            new Conflict("AntiLagX", "scheduled mass purges overlap WorldCleaner and spike MSPT",
                    "WorldCleaner (/optimize now)"),
            // Stackers: custom stacked entities fight EntityLimiter caps and MobAiReducer goal surgery.
            new Conflict("StackMob", "stacked entities fight EntityLimiter caps and MobAiReducer goal surgery",
                    "EntityLimiter + MobAiReducer"),
            new Conflict("WildStacker", "stacked entities fight EntityLimiter caps and MobAiReducer goal surgery",
                    "EntityLimiter + MobAiReducer"),
            new Conflict("RoseStacker", "stacked entities fight EntityLimiter caps and MobAiReducer goal surgery",
                    "EntityLimiter + MobAiReducer"),
            new Conflict("UltimateStacker", "stacked entities fight EntityLimiter caps and MobAiReducer goal surgery",
                    "EntityLimiter + MobAiReducer"),
            // Spawn cappers: double-capping alongside EntityLimiter causes spawn thrash.
            new Conflict("FarmLimiter", "double-capping spawns with EntityLimiter causes spawn thrash",
                    "EntityLimiter"),
            new Conflict("ChunkSpawnerLimiter", "double-capping spawns with EntityLimiter causes spawn thrash",
                    "EntityLimiter"),
            // Runtime reloaders: leak classes and silently un-do EDB patches and optimized AI.
            new Conflict("PlugMan", "runtime reload leaks and un-does EDB patches and optimized AI",
                    "a full restart instead"),
            new Conflict("PlugManX", "runtime reload leaks and un-does EDB patches and optimized AI",
                    "a full restart instead"),
            new Conflict("PluginManager", "runtime reload leaks and un-does EDB patches and optimized AI",
                    "a full restart instead"),
            new Conflict("PluginManager+", "runtime reload leaks and un-does EDB patches and optimized AI",
                    "a full restart instead"),
            new Conflict("AutoPluginLoader", "runtime reload leaks and un-does EDB patches and optimized AI",
                    "a full restart instead"),
            // Heavy per-block tooling that scales with farm size.
            new Conflict("WildTools", "per-block listeners and scheduled tasks scale with farm size",
                    "vanilla mechanics + a KOS profile")
    );

    private final OptimizeIsEasyPlugin plugin;

    public BadPluginDetector(OptimizeIsEasyPlugin plugin) {
        this.plugin = plugin;
    }

    /** Every known conflict, for tests and documentation. */
    public static List<Conflict> knownConflicts() {
        return KNOWN_BAD;
    }

    /** Installed plugins matching the known-conflict list (exact name, case-insensitive). */
    public List<Conflict> findConflicts() {
        List<Conflict> found = new ArrayList<>();
        try {
            for (Plugin p : plugin.getServer().getPluginManager().getPlugins()) {
                for (Conflict bad : KNOWN_BAD) {
                    if (p.getName().equalsIgnoreCase(bad.name())) {
                        found.add(new Conflict(p.getName(), bad.why(), bad.instead()));
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return found;
    }

    public List<String> findBadPlugins() {
        List<String> found = new ArrayList<>();
        for (Conflict conflict : findConflicts()) found.add(conflict.name());
        return found;
    }

    public void warnIfBad() {
        for (Conflict bad : findConflicts()) {
            plugin.getLogger().warning("Known lag-causing/conflicting plugin: " + bad.name()
                    + " - " + bad.why() + ". Use " + bad.instead() + " instead.");
        }
    }
}

package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.EnumSet;

public class MobAiReducerModule extends AbstractModule implements Listener {
    private final EnumSet<CreatureSpawnEvent.SpawnReason> reasons = EnumSet.noneOf(CreatureSpawnEvent.SpawnReason.class);
    private final EnumSet<EntityType> list = EnumSet.noneOf(EntityType.class);
    private boolean listMode, animals, monsters, villagers, tameable, birds, others;

    public MobAiReducerModule(OptimizeIsEasyPlugin plugin) { super(plugin, "MobAiReducer"); }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent e) {
        // Simplified: just count, do not replace pathfinder (requires NMS)
        if (!canContinue(e.getLocation().getWorld())) return;
        if (!reasons.contains(e.getSpawnReason())) return;
        // In real OptimizeIsEasy this would replace AI; here we just log fine
        if (plugin.isDebug()) plugin.getLogger().fine("MobAiReducer would optimize " + e.getEntityType());
    }

    @Override
    public void load() { Bukkit.getPluginManager().registerEvents(this, plugin); }

    @Override
    public boolean loadConfig() {
        for (String s: getSection().getStringList("spawn_reasons")) try { reasons.add(CreatureSpawnEvent.SpawnReason.valueOf(s)); } catch (Exception ignored) {}
        animals = getSection().getBoolean("entities.animals", true);
        monsters = getSection().getBoolean("entities.monsters", true);
        villagers = getSection().getBoolean("entities.villagers", false);
        listMode = getSection().getBoolean("list_mode", false);
        list.clear();
        for (String s: getSection().getStringList("list")) try { list.add(EntityType.valueOf(s)); } catch (Exception ignored) {}
        return true;
    }

    @Override
    public void disable() { HandlerList.unregisterAll(this); }
}

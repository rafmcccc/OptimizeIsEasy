package com.optimizeiseasy.core.commands;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.modules.HibernateModule;
import com.optimizeiseasy.core.modules.WorldCleanerModule;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

final class OptimizeSweepHandler {
    private OptimizeSweepHandler() {}

    static void now(OptimizeIsEasyPlugin plugin, CommandSender sender) {
        int removed = 0;
        WorldCleanerModule wc = plugin.getModuleManager().get(WorldCleanerModule.class);
        if (wc != null && wc.isLoaded()) {
            removed = wc.clearNow();
        } else {
            // Fallback: simple ground item clear
            for (World w : Bukkit.getWorlds()) {
                for (var e : w.getEntitiesByClass(org.bukkit.entity.Item.class).toArray(new org.bukkit.entity.Item[0])) {
                    if (e.getTicksLived() > 200) { e.remove(); removed++; }
                }
            }
        }
        // GC if hibernate gc enabled
        HibernateModule hm = plugin.getModuleManager().get(HibernateModule.class);
        if (hm != null) {
            try { System.gc(); } catch (Throwable ignored) {}
        }
        sender.sendMessage("§aOptimization pass complete. §7Removed §e" + removed + " §7entities. Freed memory.");
        if (plugin.isDebug()) plugin.getLogger().fine("Manual optimization by " + sender.getName() + " removed " + removed);
    }
}

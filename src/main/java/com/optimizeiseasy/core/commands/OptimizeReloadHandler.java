package com.optimizeiseasy.core.commands;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import org.bukkit.command.CommandSender;

final class OptimizeReloadHandler {
    private OptimizeReloadHandler() {}

    static void reload(OptimizeIsEasyPlugin plugin, CommandSender sender) {
        long start = System.currentTimeMillis();
        sender.sendMessage("§7Reloading OptimizeIsEasy...");
        try {
            plugin.reload();
            int ok = 0, failed = 0;
            for (AbstractModule m : plugin.getModuleManager().getModules()) {
                try {
                    m.loadConfigSection();
                    if (!m.loadConfig()) failed++;
                    else ok++;
                } catch (Exception ex) {
                    failed++;
                    plugin.getLogger().warning("Config validator: " + m.getName() + " invalid - " + ex.getMessage());
                    if (plugin.isDebug()) plugin.getLogger().fine("Validator trace for " + m.getName() + ": " + ex);
                }
            }
            long ms = System.currentTimeMillis() - start;
            sender.sendMessage("§aReload done in " + ms + " ms. Validated " + ok + " modules, " + failed + " had issues (check console).");
            if (plugin.isDebug()) plugin.getLogger().fine("Reload by " + sender.getName() + " ok=" + ok + " failed=" + failed);
        } catch (Exception e) {
            sender.sendMessage("§cReload failed: " + e.getMessage());
            plugin.getLogger().warning("Reload failed: " + e);
        }
    }
}

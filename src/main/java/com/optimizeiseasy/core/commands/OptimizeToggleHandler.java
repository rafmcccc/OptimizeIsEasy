package com.optimizeiseasy.core.commands;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.modules.HibernateModule;
import com.optimizeiseasy.core.objects.AbstractModule;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class OptimizeToggleHandler {
    private OptimizeToggleHandler() {}

    static void toggle(OptimizeIsEasyPlugin plugin, CommandSender sender, String moduleName) {
        HibernateModule hm = plugin.getModuleManager().get(HibernateModule.class);
        if (moduleName == null) {
            // Toggle hibernation/auto-optimize globally
            if (hm == null) { sender.sendMessage("§cHibernate module not found."); return; }
            if (hm.isFrozen()) {
                hm.unfreeze("Manual unfreeze via /optimize toggle by " + sender.getName());
                sender.sendMessage("§aServer unfrozen.");
            } else {
                if (!Bukkit.getOnlinePlayers().isEmpty()) {
                    sender.sendMessage("§cCannot freeze while players are online.");
                    return;
                }
                hm.freeze("Manual freeze via /optimize toggle by " + sender.getName());
                sender.sendMessage("§aServer frozen.");
            }
            return;
        }
        // Toggle specific module
        if (moduleName.equalsIgnoreCase("hibernation") || moduleName.equalsIgnoreCase("hibernate")) {
            toggle(plugin, sender, null);
            return;
        }
        AbstractModule mod = plugin.getModuleManager().get(moduleName);
        if (mod == null) {
            sender.sendMessage("§cUnknown module: §f" + moduleName);
            sender.sendMessage("§7Available: " + String.join(", ", plugin.getModuleManager().getModules().stream().map(AbstractModule::getName).toList()));
            return;
        }
        try {
            if (mod.isLoaded()) {
                mod.disable();
                mod.setLoaded(false);
                mod.getConfig().set(mod.getName() + ".enabled", false);
                mod.getConfig().save(new java.io.File(plugin.getDataFolder(), "modules/" + mod.getName() + ".yml"));
                sender.sendMessage("§cDisabled module §f" + mod.getName());
            } else {
                mod.loadConfigSection();
                boolean ok = mod.loadConfig();
                if (ok) {
                    mod.load();
                    mod.setLoaded(true);
                    mod.getConfig().set(mod.getName() + ".enabled", true);
                    mod.getConfig().save(new java.io.File(plugin.getDataFolder(), "modules/" + mod.getName() + ".yml"));
                    sender.sendMessage("§aEnabled module §f" + mod.getName());
                } else {
                    sender.sendMessage("§cFailed to enable " + mod.getName());
                }
            }
        } catch (Exception e) {
            sender.sendMessage("§cError toggling " + mod.getName() + ": " + e.getMessage());
            plugin.getLogger().warning("Toggle error for " + mod.getName() + ": " + e);
        }
    }

    static boolean requirePlayer(CommandSender sender) {
        if (sender instanceof Player) return true;
        sender.sendMessage("§7The GUI is in-game only. Console: §f/optimize status§7, §f/optimize edb check§7, §f/optimize kos <profile> <true|false>§7.");
        return false;
    }
}

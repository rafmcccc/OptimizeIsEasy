package com.optimizeiseasy.core.commands;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.modules.BorderControlModule;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class OptimizeGuiHandler {
    private OptimizeGuiHandler() {}

    static void gui(OptimizeIsEasyPlugin plugin, CommandSender sender) {
        if (!OptimizeToggleHandler.requirePlayer(sender)) return;
        if (plugin.getGui() == null) {
            sender.sendMessage("§cGUI not available.");
            return;
        }
        plugin.getGui().open((Player) sender);
    }

    static void krypton(OptimizeIsEasyPlugin plugin, CommandSender sender) {
        if (!OptimizeToggleHandler.requirePlayer(sender)) return;
        if (plugin.getKryptonGui() == null) {
            sender.sendMessage("§cKrypton GUI not available.");
            return;
        }
        plugin.getKryptonGui().open((Player) sender);
    }

    static void edbGui(OptimizeIsEasyPlugin plugin, CommandSender sender) {
        if (!OptimizeToggleHandler.requirePlayer(sender)) return;
        if (plugin.getEdbGui() == null) {
            sender.sendMessage("§cExploitDB GUI not available.");
            return;
        }
        plugin.getEdbGui().open((Player) sender);
    }

    static void border(OptimizeIsEasyPlugin plugin, CommandSender sender) {
        BorderControlModule mod = plugin.getModuleManager().get(BorderControlModule.class);
        if (mod == null || !mod.isLoaded()) {
            sender.sendMessage("§cBorderControl module is disabled. Enable it with /optimize toggle BorderControl.");
            return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§7Border is per-world. In-game use §f/border §7or §f/optimize border§7. Worlds: "
                    + Bukkit.getWorlds().stream().map(World::getName).reduce((a, b) -> a + ", " + b).orElse("none"));
            return;
        }
        mod.openMainGUI(p);
    }
}

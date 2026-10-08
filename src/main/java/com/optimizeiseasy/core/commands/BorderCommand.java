package com.optimizeiseasy.core.commands;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.modules.BorderControlModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

public class BorderCommand implements TabExecutor {
    private final OptimizeIsEasyPlugin plugin;

    public BorderCommand(OptimizeIsEasyPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasPerm(CommandSender s) {
        return s.hasPermission("optimizeiseasy.border") || s.hasPermission("border.admin")
                || s.hasPermission("optimizeiseasy.optimize") || s.isOp();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPerm(sender)) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be executed by a player. Use /optimize border status instead.");
            return true;
        }
        BorderControlModule mod = plugin.getModuleManager().get(BorderControlModule.class);
        if (mod == null || !mod.isLoaded()) {
            player.sendMessage("§cBorderControl module is disabled. Enable it with /optimize toggle BorderControl.");
            return true;
        }
        mod.openMainGUI(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}

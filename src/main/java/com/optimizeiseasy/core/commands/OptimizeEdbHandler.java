package com.optimizeiseasy.core.commands;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.modules.ExploitDBModule;
import org.bukkit.command.CommandSender;

final class OptimizeEdbHandler {
    private OptimizeEdbHandler() {}

    static void edb(OptimizeIsEasyPlugin plugin, CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("gui")) {
            OptimizeGuiHandler.edbGui(plugin, sender);
            return;
        }
        ExploitDBModule edb = plugin.getModuleManager().get(ExploitDBModule.class);
        if (edb == null || !edb.isLoaded()) {
            sender.sendMessage("§cExploitDB module is disabled. Enable it with /optimize toggle ExploitDB.");
            return;
        }
        if (args.length == 1 || (args.length >= 2 && args[1].equalsIgnoreCase("check"))) {
            var res = edb.checkDetailed();
            sender.sendMessage("§8§m    §r §c§lExploitDB §7Check §8§m    ");
            for (var e : res.entrySet()) {
                if (!edb.isEnabled(e.getKey())) {
                    sender.sendMessage(" §8✖ §8" + e.getKey() + " " + edb.describe(e.getKey()) + " §8- disabled in config");
                    continue;
                }
                sender.sendMessage(switch (e.getValue()) {
                    case SAFE -> " §a✔ ";
                    case VULNERABLE -> " §c✘ ";
                    case UNSUPPORTED -> " §8– ";
                } + "§f" + e.getKey() + " §8- §7" + edb.describe(e.getKey()));
            }
            sender.sendMessage("§7Fix with §f/optimize edb patch <id|all>");
            if (edb.isDryRun()) sender.sendMessage("§6Dry run is on in modules/ExploitDB.yml - patching writes nothing.");
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("patch")) {
            if (args.length < 3) {
                sender.sendMessage("§7Usage: §f/optimize edb patch <EDB-1..EDB-" + edb.allIds().size() + "|all>");
                return;
            }
            String id = args[2];
            if (id.equalsIgnoreCase("all")) {
                edb.patchAll(sender);
            } else {
                edb.patch(id, sender);
            }
            return;
        }
        sender.sendMessage("§7Usage: §f/optimize edb <check|patch <id|all>>");
    }
}

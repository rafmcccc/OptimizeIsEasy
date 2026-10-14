package com.optimizeiseasy.core.commands;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.modules.ServerTunerModule;
import com.optimizeiseasy.core.modules.ExploitDBModule;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.utils.ServerReport;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OptimizeCommand implements TabExecutor {
    private final OptimizeIsEasyPlugin plugin;

    public OptimizeCommand(OptimizeIsEasyPlugin plugin) { this.plugin = plugin; }

    private boolean hasPerm(CommandSender s) {
        return s.hasPermission("optimizeiseasy.optimize") || s.hasPermission("rafmc.optimize") || s.isOp();
    }

    private boolean autoOpen() {
        return plugin.getConfig().getBoolean("gui.auto-open", true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPerm(sender)) {
            sender.sendMessage("§7Missing permission: §eoptimizeiseasy.optimize");
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player p && autoOpen()) {
                OptimizeGuiHandler.gui(plugin, sender);
                return true;
            }
            sender.sendMessage("§7Usage: §f/optimize <status|now|toggle [module]|reload|gui|krypton|version|kos|edb|border|report>");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "status" -> OptimizeStatusHandler.status(plugin, sender);
            case "now" -> OptimizeSweepHandler.now(plugin, sender);
            case "toggle" -> OptimizeToggleHandler.toggle(plugin, sender, args.length > 1 ? args[1] : null);
            case "reload" -> OptimizeReloadHandler.reload(plugin, sender);
            case "gui", "menu" -> OptimizeGuiHandler.gui(plugin, sender);
            case "krypton" -> OptimizeGuiHandler.krypton(plugin, sender);
            case "version", "ver", "v" -> OptimizeStatusHandler.version(plugin, sender);
            case "kos" -> OptimizeKosHandler.kos(plugin, sender, args);
            case "edb" -> OptimizeEdbHandler.edb(plugin, sender, args);
            case "border" -> OptimizeGuiHandler.border(plugin, sender);
            case "report" -> new ServerReport(plugin).run(sender);
            default -> sender.sendMessage("§7Unknown subcommand. §f/optimize <status|now|toggle|reload|gui|krypton|version|kos|edb|border|report>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!hasPerm(sender)) return List.of();
        if (args.length == 1) {
            return filter(Arrays.asList("status", "now", "toggle", "reload", "gui", "krypton", "version", "kos", "edb", "border", "report"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            List<String> mods = new ArrayList<>();
            mods.add("hibernation");
            for (AbstractModule m : plugin.getModuleManager().getModules()) mods.add(m.getName());
            return filter(mods, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("kos")) {
            List<String> profiles = new ArrayList<>();
            profiles.add("gui");
            ServerTunerModule tuner = plugin.getModuleManager().get(ServerTunerModule.class);
            if (tuner != null) profiles.addAll(tuner.listProfiles());
            else profiles.addAll(Arrays.asList("YouHaveTrouble.kos", "FarmFriendly.kos", "Balanced.kos", "LowEnd.kos",
                    "HighEnd.kos", "VanillaPlus.kos", "LobbyGames.kos", "Anarchy.kos"));
            return filter(profiles, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("kos")) {
            return filter(Arrays.asList("true", "false"), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("edb")) {
            return filter(Arrays.asList("check", "patch", "gui"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("edb") && args[1].equalsIgnoreCase("patch")) {
            List<String> ids = new ArrayList<>();
            ids.add("all");
            ExploitDBModule edb = plugin.getModuleManager().get(ExploitDBModule.class);
            if (edb != null) ids.addAll(edb.allIds());
            return filter(ids, args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> opts, String prefix) {
        String p = prefix.toLowerCase();
        return opts.stream().filter(s -> s.toLowerCase().startsWith(p)).sorted().toList();
    }
}

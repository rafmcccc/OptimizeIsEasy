package com.optimizeiseasy.core.commands;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.modules.ServerTunerModule;
import org.bukkit.command.CommandSender;

final class OptimizeKosHandler {
    private OptimizeKosHandler() {}

    static void kos(OptimizeIsEasyPlugin plugin, CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("gui")) {
            OptimizeGuiHandler.krypton(plugin, sender);
            return;
        }
        ServerTunerModule tuner = plugin.getModuleManager().get(ServerTunerModule.class);
        if (tuner == null || !tuner.isLoaded()) {
            sender.sendMessage("§cServerTuner module is disabled. Enable it with /optimize toggle ServerTuner.");
            return;
        }
        if (args.length == 1) {
            String def = tuner.defaultProfile();
            sender.sendMessage("§7Available profiles: §f" + String.join(", ", tuner.listProfiles()));
            sender.sendMessage("§7Default: §f" + def);
            if (!tuner.deniedProfiles().isEmpty()) {
                sender.sendMessage("§7Hidden by denied-profiles: §8" + String.join(", ", tuner.deniedProfiles()));
            }
            Boolean known = tuner.storedPregen();
            if (known != null) {
                tuner.runProfile(def, known, sender);
            } else {
                sender.sendMessage("§7Usage: §f/optimize kos <profile> <true|false-pregenerated>");
            }
            return;
        }
        String profile = args[1];
        Boolean known = tuner.storedPregen();
        if (args.length == 2) {
            if (known != null) {
                tuner.runProfile(profile, known, sender);
            } else {
                sender.sendMessage("§7Is your world pre-generated? §f/optimize kos " + profile + " <true|false>");
            }
            return;
        }
        String flag = args[2].toLowerCase();
        boolean pregen;
        if (flag.equals("true") || flag.equals("yes") || flag.equals("y")) pregen = true;
        else if (flag.equals("false") || flag.equals("no") || flag.equals("n")) pregen = false;
        else {
            sender.sendMessage("§cUse true/false for pregenerated. Example: /optimize kos " + profile + " true");
            return;
        }
        tuner.runProfile(profile, pregen, sender);
    }
}

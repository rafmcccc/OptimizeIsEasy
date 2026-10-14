package com.optimizeiseasy.core.commands;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.modules.ExploitDBModule;
import com.optimizeiseasy.core.modules.HibernateModule;
import com.optimizeiseasy.core.modules.WorldCleanerModule;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.SupportManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

// ponytail: one class per /optimize branch; add a handler, not a switch arm, when a subcommand lands.
final class OptimizeStatusHandler {
    private OptimizeStatusHandler() {}

    static void status(OptimizeIsEasyPlugin plugin, CommandSender sender) {
        double tps = SupportManager.currentTps();
        double mspt = 0;
        SupportManager sm = SupportManager.getInstance();
        if (sm != null && sm.isSupportMspt()) {
            try { mspt = sm.getMspt(); } catch (Exception e) { if (plugin.isDebug()) plugin.getLogger().fine("[OptimizeCommand] MSPT read failed: " + e.getMessage()); }
        } else {
            try { mspt = Bukkit.getAverageTickTime(); } catch (Exception e) { if (plugin.isDebug()) plugin.getLogger().fine("[OptimizeCommand] MSPT read failed: " + e.getMessage()); }
        }
        long entities = 0;
        for (World w : Bukkit.getWorlds()) try { entities += w.getEntities().size(); } catch (Exception e) { if (plugin.isDebug()) plugin.getLogger().fine("[OptimizeCommand] entity count failed: " + e.getMessage()); }
        HibernateModule hm = plugin.getModuleManager().get(HibernateModule.class);
        boolean frozen = hm != null && hm.isFrozen();
        // Count enabled modules
        long enabled = plugin.getModuleManager().getModules().stream().filter(AbstractModule::isLoaded).count();
        sender.sendMessage("§8§m    §r §a§lOptimizeIsEasy §7Status §8§m    ");
        sender.sendMessage(" §8• §fTPS: §e" + String.format("%.2f", Math.min(tps, 20.0)));
        sender.sendMessage(" §8• §fMSPT: §e" + String.format("%.2f", mspt));
        sender.sendMessage(" §8• §fEntities: §e" + entities);
        sender.sendMessage(" §8• §fHibernation: " + (frozen ? "§cFrozen (0 TPS)" : "§aRunning"));
        sender.sendMessage(" §8• §fModules enabled: §e" + enabled + "§8/§e" + plugin.getModuleManager().getModules().size());
        if (hm != null) {
            WorldCleanerModule wc = plugin.getModuleManager().get(WorldCleanerModule.class);
            if (wc != null && wc.isLoaded()) sender.sendMessage(" §8• §fNext purge: §e" + wc.getInterval() + "s");
        }
        if (plugin.isRestartRequired()) sender.sendMessage(" §8• §cRestart required §7- KOS/EDB patches pending");
        if (plugin.getSoftwareDetector() != null) sender.sendMessage(" §8• §fConfigs: §e" + plugin.getSoftwareDetector().describe());
        ExploitDBModule edb = plugin.getModuleManager().get(ExploitDBModule.class);
        if (edb != null && edb.isLoaded()) {
            long failed = edb.countEnabled(ExploitDBModule.CheckResult.VULNERABLE);
            if (failed > 0) sender.sendMessage(" §8• §fExploitDB: §c" + failed + " checks failing §8(§7/optimize edb check§8)");
        }
    }

    static void version(OptimizeIsEasyPlugin plugin, CommandSender sender) {
        String ver = plugin.getDescription().getVersion();
        String paper = Bukkit.getVersion();
        String fork = "Paper";
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            fork = "Folia";
        } catch (Throwable t) {
            try { Class.forName("org.purpurmc.purpur.PurpurConfig"); fork = "Purpur"; } catch (Throwable ignored) {}
        }
        sender.sendMessage("§aOptimizeIsEasy §7" + ver + " §8- §fPaper " + paper.split("-")[0] + " §8(" + fork + ")");
        String latest = plugin.getUpdateChecker() != null ? plugin.getUpdateChecker().getLatest() : null;
        if (latest != null && !latest.equals(ver)) sender.sendMessage("§eUpdate available: §f" + ver + " -> " + latest);
        else if (latest != null) sender.sendMessage("§7You are up to date.");
        else sender.sendMessage("§7Use §f/optimize reload §7to reload configs.");
    }
}

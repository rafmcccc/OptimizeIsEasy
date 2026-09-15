package com.optimizeiseasy.core.utils;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.modules.BorderControlModule;
import com.optimizeiseasy.core.modules.ExploitDBModule;
import com.optimizeiseasy.core.modules.HibernateModule;
import com.optimizeiseasy.core.modules.ServerTunerModule;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.SupportManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ServerReport {
    private final OptimizeIsEasyPlugin plugin;

    public ServerReport(OptimizeIsEasyPlugin plugin) {
        this.plugin = plugin;
    }

    public void run(CommandSender sender) {
        double tps = 20.0;
        try { tps = Bukkit.getTPS()[0]; } catch (Throwable ignored) {}
        double mspt = 0;
        try {
            SupportManager sm = SupportManager.getInstance();
            if (sm != null && sm.isSupportMspt()) mspt = sm.getMspt();
            else mspt = Bukkit.getAverageTickTime();
        } catch (Throwable ignored) {}

        long entities = 0;
        for (World w : Bukkit.getWorlds()) {
            try { entities += w.getEntities().size(); } catch (Throwable ignored) {}
        }

        SoftwareDetector sd = plugin.getSoftwareDetector();
        String software = sd != null ? sd.describe() : "unknown";

        long enabled = plugin.getModuleManager().getModules().stream().filter(AbstractModule::isLoaded).count();
        int total = plugin.getModuleManager().getModules().size();

        HibernateModule hm = plugin.getModuleManager().get(HibernateModule.class);
        boolean frozen = hm != null && hm.isFrozen();

        sender.sendMessage("§8§m    §r §a§lOptimizeIsEasy §7Report §8§m    ");
        sender.sendMessage(" §8• §fVersion: §e" + plugin.getDescription().getVersion());
        sender.sendMessage(" §8• §fTPS: §e" + String.format("%.2f", Math.min(tps, 20.0)) + " §8| §fMSPT: §e" + String.format("%.2f", mspt));
        sender.sendMessage(" §8• §fEntities: §e" + entities + " §8| §fHibernation: " + (frozen ? "§cFrozen" : "§aRunning"));
        sender.sendMessage(" §8• §fModules: §e" + enabled + "§8/§e" + total);
        sender.sendMessage(" §8• §fSoftware configs: §e" + software);
        sender.sendMessage(" §8• §fRestart required: " + (plugin.isRestartRequired() ? "§cYES - restart to apply server-file patches" : "§aNo"));

        ExploitDBModule edb = plugin.getModuleManager().get(ExploitDBModule.class);
        if (edb != null && edb.isLoaded()) {
            var results = edb.checkAll();
            long failed = results.values().stream().filter(b -> !b).count();
            sender.sendMessage(" §8• §fExploitDB: §e" + (results.size() - failed) + "§a passed§8, §e" + failed + "§c failed §8(§7/exploitfix list§8)");
            if (failed > 0 && plugin.isDebug()) {
                for (var e : results.entrySet()) {
                    if (!e.getValue()) sender.sendMessage(" §8- §c" + e.getKey() + " failed");
                }
            }
        }

        ServerTunerModule tuner = plugin.getModuleManager().get(ServerTunerModule.class);
        if (tuner != null && tuner.isLoaded()) {
            sender.sendMessage(" §8• §fKOS profiles: §e" + String.join(", ", tuner.listProfiles()) + " §8(§7/optimize kos§8)");
        }

        BorderControlModule border = plugin.getModuleManager().get(BorderControlModule.class);
        if (border != null && border.isLoaded()) {
            sender.sendMessage(" §8• §fBorder: §e/optimize border §7or §e/border");
        }

        List<String> bad = new BadPluginDetector(plugin).findBadPlugins();
        if (!bad.isEmpty()) sender.sendMessage(" §8• §cConflicting plugins: §f" + String.join(", ", bad));
    }
}

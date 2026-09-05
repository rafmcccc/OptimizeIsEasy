package com.optimizeiseasy.core.commands;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.modules.HibernateModule;
import com.optimizeiseasy.core.modules.WorldCleanerModule;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.SupportManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPerm(sender)) {
            sender.sendMessage("§7Missing permission: §eoptimizeiseasy.optimize");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§7Usage: §f/optimize <status|now|toggle [module]>");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "status" -> handleStatus(sender);
            case "now" -> handleNow(sender);
            case "toggle" -> handleToggle(sender, args.length > 1 ? args[1] : null);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage("§7Unknown subcommand. §f/optimize <status|now|toggle|reload>");
        }
        return true;
    }

    private void handleStatus(CommandSender sender) {
        double tps = 20.0;
        try { tps = Bukkit.getTPS()[0]; } catch (Throwable ignored) {}
        double mspt = 0;
        SupportManager sm = SupportManager.getInstance();
        if (sm != null && sm.isSupportMspt()) {
            try { mspt = sm.getMspt(); } catch (Throwable ignored) {}
        } else {
            try { mspt = Bukkit.getAverageTickTime(); } catch (Throwable ignored) {}
        }
        long entities = 0;
        for (World w : Bukkit.getWorlds()) try { entities += w.getEntities().size(); } catch (Throwable ignored) {}
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
    }

    private void handleNow(CommandSender sender) {
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

    private void handleReload(CommandSender sender) {
        long start = System.currentTimeMillis();
        sender.sendMessage("§7Reloading OptimizeIsEasy...");
        try {
            // Validate main config before reload
            plugin.reload();
            // Validate each module config
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

    private void handleToggle(CommandSender sender, String moduleName) {
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
            handleToggle(sender, null);
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!hasPerm(sender)) return List.of();
        if (args.length == 1) {
            return filter(Arrays.asList("status", "now", "toggle", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            List<String> mods = new ArrayList<>();
            mods.add("hibernation");
            for (AbstractModule m : plugin.getModuleManager().getModules()) mods.add(m.getName());
            return filter(mods, args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> opts, String prefix) {
        String p = prefix.toLowerCase();
        return opts.stream().filter(s -> s.toLowerCase().startsWith(p)).sorted().toList();
    }
}

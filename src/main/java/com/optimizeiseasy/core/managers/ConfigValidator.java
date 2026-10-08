package com.optimizeiseasy.core.managers;

import com.optimizeiseasy.core.objects.AbstractModule;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ConfigValidator {
    public static List<String> validateMainConfig(FileConfiguration cfg) {
        List<String> errs = new ArrayList<>();
        if (!(cfg.get("debug") instanceof Boolean) && cfg.contains("debug")) errs.add("debug must be boolean");
        if (cfg.contains("hibernate.gc-on-freeze") && !(cfg.get("hibernate.gc-on-freeze") instanceof Boolean)) errs.add("hibernate.gc-on-freeze must be boolean");
        int ci = cfg.getInt("hibernate.check-interval-seconds", 60);
        if (ci < 0 || ci > 86400) errs.add("hibernate.check-interval-seconds out of range 0..86400");
        if (cfg.contains("main.updater") && !(cfg.get("main.updater") instanceof Boolean)) errs.add("main.updater must be boolean");
        ConfigurationSection mon = cfg.getConfigurationSection("main.monitor.resource");
        if (mon != null) {
            int iv = mon.getInt("interval", 5);
            if (iv < 1 || iv > 36000) errs.add("main.monitor.resource.interval out of range");
        }
        return errs;
    }

    public static List<String> validateModule(AbstractModule module) {
        List<String> errs = new ArrayList<>();
        FileConfiguration cfg = module.getConfig();
        String name = module.getName();
        if (cfg.contains(name + ".enabled") && !(cfg.get(name + ".enabled") instanceof Boolean)) errs.add(name + ".enabled must be boolean");
        List<String> worlds = cfg.getStringList(name + ".worlds");
        for (String w : worlds) {
            if (w.equals("*")) continue;
            if (w == null || w.isBlank()) errs.add(name + ".worlds contains blank");
        }
        ConfigurationSection sec = module.getSection();
        if (sec == null && cfg.contains(name + ".enabled") && cfg.getBoolean(name + ".enabled")) {
            errs.add(name + ".values section missing");
        }
        // Range checks for common keys
        if (sec != null) {
            if (sec.contains("interval")) {
                int v = sec.getInt("interval", -1);
                if (v != -1 && (v < 1 || v > 86400)) errs.add(name + ".values.interval out of range");
            }
            if (sec.contains("ticks_limit.redstone")) {
                int v = sec.getInt("ticks_limit.redstone", -1);
                if (v != -1 && (v < 1 || v > 5000)) errs.add(name + ".values.ticks_limit.redstone out of range");
            }
            errs.addAll(validateModuleSection(name, sec));
        }
        return errs;
    }

    public static List<String> validateModuleSection(String name, ConfigurationSection sec) {
        List<String> errs = new ArrayList<>();
        if (sec == null) return errs;
        errs.addAll(validateExploitDb(name, sec));
        errs.addAll(validateServerTuner(name, sec));
        return errs;
    }

    private static List<String> validateExploitDb(String name, ConfigurationSection sec) {
        List<String> errs = new ArrayList<>();
        if (!name.equals("ExploitDB")) return errs;
        for (String b : new String[]{"auto-check-on-start", "auto-patch-on-start", "dry-run", "backup-before-patch"}) {
            if (sec.contains(b) && !(sec.get(b) instanceof Boolean)) errs.add("ExploitDB.values." + b + " must be boolean");
        }
        if (sec.contains("rate-limit-packets")) {
            if (!(sec.get("rate-limit-packets") instanceof Number)) {
                errs.add("ExploitDB.values.rate-limit-packets must be a number");
            } else {
                int v = sec.getInt("rate-limit-packets", 400);
                if (v < 0 || v > 100000) errs.add("ExploitDB.values.rate-limit-packets out of range 0..100000");
            }
        }
        if (sec.contains("patch-cooldown-seconds")) {
            if (!(sec.get("patch-cooldown-seconds") instanceof Number)) {
                errs.add("ExploitDB.values.patch-cooldown-seconds must be a number");
            } else {
                int v = sec.getInt("patch-cooldown-seconds", 3);
                if (v < 0 || v > 3600) errs.add("ExploitDB.values.patch-cooldown-seconds out of range 0..3600");
            }
        }
        for (String list : new String[]{"enabled-exploits", "patch-allowlist", "patch-denylist"}) {
            if (!sec.contains(list)) continue;
            for (String id : sec.getStringList(list)) {
                if (id == null || id.isBlank() || !id.equals("*") && !id.matches("EDB-\\d{1,3}")) {
                    errs.add("ExploitDB.values." + list + " has an invalid entry: '" + id + "' (use '*' or EDB-N)");
                }
            }
        }
        ConfigurationSection limits = sec.getConfigurationSection("packet-limits");
        if (limits != null) {
            for (String id : limits.getKeys(false)) {
                if (!id.matches("EDB-\\d{1,3}")) {
                    errs.add("ExploitDB.values.packet-limits has an unknown key: '" + id + "'");
                    continue;
                }
                ConfigurationSection one = limits.getConfigurationSection(id);
                if (one == null) {
                    errs.add("ExploitDB.values.packet-limits." + id + " must be a section");
                    continue;
                }
                String action = one.getString("action");
                if (action != null && !action.equalsIgnoreCase("DROP") && !action.equalsIgnoreCase("FILTER")
                        && !action.equalsIgnoreCase("DISABLED")) {
                    errs.add("ExploitDB.values.packet-limits." + id + ".action must be DROP, FILTER or DISABLED");
                }
                for (String k : new String[]{"interval", "max-packet-rate"}) {
                    if (one.contains(k) && one.getDouble(k) < 0) {
                        errs.add("ExploitDB.values.packet-limits." + id + "." + k + " must be >= 0");
                    }
                }
            }
        }
        return errs;
    }

    private static List<String> validateServerTuner(String name, ConfigurationSection sec) {
        List<String> errs = new ArrayList<>();
        if (!name.equals("ServerTuner")) return errs;
        for (String b : new String[]{"dry-run", "backup-before-apply", "allow-override-pregenerated"}) {
            if (sec.contains(b) && !(sec.get(b) instanceof Boolean)) errs.add("ServerTuner.values." + b + " must be boolean");
        }
        if (sec.contains("world-is-pregenerated")) {
            if (!(sec.get("world-is-pregenerated") instanceof Number)) {
                errs.add("ServerTuner.values.world-is-pregenerated must be a number (0, 1 or 2)");
            } else {
                int v = sec.getInt("world-is-pregenerated", 0);
                if (v < 0 || v > 2) errs.add("ServerTuner.values.world-is-pregenerated must be 0, 1 or 2");
            }
        }
        if (sec.contains("denied-profiles")) {
            for (String p : sec.getStringList("denied-profiles")) {
                if (p == null || p.isBlank()) errs.add("ServerTuner.values.denied-profiles contains a blank entry");
                else if (p.contains("..") || p.contains("/") || p.contains("\\")) {
                    errs.add("ServerTuner.values.denied-profiles has an invalid entry: '" + p + "'");
                }
            }
        }
        return errs;
    }

    public static boolean isValidWorld(String name) {
        if (name.equals("*")) return true;
        for (World w : Bukkit.getWorlds()) if (w.getName().equals(name)) return true;
        return false;
    }
}

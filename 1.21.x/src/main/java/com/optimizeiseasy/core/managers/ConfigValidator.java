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
        }
        return errs;
    }

    public static boolean isValidWorld(String name) {
        if (name.equals("*")) return true;
        for (World w : Bukkit.getWorlds()) if (w.getName().equals(name)) return true;
        return false;
    }
}

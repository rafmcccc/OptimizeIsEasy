package com.optimizeiseasy.core.utils;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SoftwareDetector {
    public enum ConfigType {
        MINECRAFT, BUKKIT, SPIGOT, PAPER_WORLD, PAPER_GLOBAL, PURPUR, PUFFERFISH, LEAF
    }

    private final OptimizeIsEasyPlugin plugin;
    private final List<ConfigType> supported = new ArrayList<>();

    public SoftwareDetector(OptimizeIsEasyPlugin plugin) {
        this.plugin = plugin;
        detect();
    }

    private void detect() {
        supported.clear();
        if (new File("server.properties").exists()) supported.add(ConfigType.MINECRAFT);
        if (new File("bukkit.yml").exists()) supported.add(ConfigType.BUKKIT);
        if (new File("spigot.yml").exists()) supported.add(ConfigType.SPIGOT);
        if (new File("config/paper-world-defaults.yml").exists()) supported.add(ConfigType.PAPER_WORLD);
        if (new File("config/paper-global.yml").exists()) supported.add(ConfigType.PAPER_GLOBAL);
        if (new File("purpur.yml").exists()) supported.add(ConfigType.PURPUR);
        if (new File("pufferfish.yml").exists()) supported.add(ConfigType.PUFFERFISH);
        if (new File("config/leaf-global.yml").exists()) supported.add(ConfigType.LEAF);
        if (plugin.isDebug()) plugin.getLogger().fine("Supported configs: " + supported);
        else plugin.getLogger().info("Supported configs: " + supported);
    }

    public List<ConfigType> getSupported() { return new ArrayList<>(supported); }

    public boolean supportsMinecraft() { return supported.contains(ConfigType.MINECRAFT); }
    public boolean supportsBukkit() { return supported.contains(ConfigType.BUKKIT); }
    public boolean supportsSpigot() { return supported.contains(ConfigType.SPIGOT); }
    public boolean supportsPaperWorld() { return supported.contains(ConfigType.PAPER_WORLD); }
    public boolean supportsPaperGlobal() { return supported.contains(ConfigType.PAPER_GLOBAL); }
    public boolean supportsPurpur() { return supported.contains(ConfigType.PURPUR); }
    public boolean supportsPufferfish() { return supported.contains(ConfigType.PUFFERFISH); }
    public boolean supportsLeaf() { return supported.contains(ConfigType.LEAF); }

    public String describe() {
        if (supported.isEmpty()) return "none";
        StringBuilder sb = new StringBuilder();
        for (ConfigType t : supported) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(t.name());
        }
        return sb.toString();
    }
}

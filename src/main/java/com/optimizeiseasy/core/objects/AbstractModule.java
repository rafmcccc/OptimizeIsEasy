package com.optimizeiseasy.core.objects;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.managers.ConfigManager;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractModule {

    protected final OptimizeIsEasyPlugin plugin;
    protected final String name;
    protected final YamlConfiguration config = new YamlConfiguration();
    protected ConfigurationSection section;
    protected boolean loaded = false;

    protected final HashSet<String> worlds = new HashSet<>();
    protected int canContinue; // -1 empty, 1 = *, 0 = list

    public AbstractModule(OptimizeIsEasyPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        try {
            loadConfigSection();
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to load config for " + name + ": " + ex.getMessage());
        }
    }

    public void loadConfigSection() throws Exception {
        File file = new File(plugin.getDataFolder(), "modules/" + name + ".yml");
        // ensure default file exists
        if (!file.exists()) {
            plugin.saveResource("modules/" + name + ".yml", false);
        }
        ConfigManager.loadConfig(plugin, config, "modules/" + name + ".yml");
        section = config.getConfigurationSection(name + ".values");
        if (section == null) section = config.getConfigurationSection(name);
        worlds.clear();
        List<String> list = config.getStringList(name + ".worlds");
        if (list.isEmpty()) {
            canContinue = -1;
        } else if (list.contains("*")) {
            canContinue = 1;
        } else {
            canContinue = 0;
            worlds.addAll(list);
        }
    }

    public boolean canContinue(World w) {
        if (canContinue == -1) return true;
        if (canContinue == 1) return true;
        return worlds.contains(w.getName());
    }

    public Set<World> getAllowedWorlds() {
        HashSet<World> set = new HashSet<>();
        for (World world : Bukkit.getWorlds()) {
            if (canContinue(world)) set.add(world);
        }
        return Collections.unmodifiableSet(set);
    }

    public String getName() { return name; }
    public boolean isLoaded() { return loaded; }
    public void setLoaded(boolean loaded) { this.loaded = loaded; }
    public YamlConfiguration getConfig() { return config; }
    public ConfigurationSection getSection() { return section; }

    public boolean isEnabledInConfig() {
        return config.getBoolean(name + ".enabled", false);
    }

    public abstract void load() throws Exception;
    public abstract boolean loadConfig() throws Exception;
    public abstract void disable() throws Exception;
}

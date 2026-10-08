package com.optimizeiseasy.core.managers;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.modules.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ModuleManager {
    private final OptimizeIsEasyPlugin plugin;
    private final Map<String, AbstractModule> modules = new HashMap<>();

    public ModuleManager(OptimizeIsEasyPlugin plugin) {
        this.plugin = plugin;
        add(new HibernateModule(plugin));
        add(new WorldCleanerModule(plugin));
        add(new EntityLimiterModule(plugin));
        add(new HopperOptimizerModule(plugin));
        add(new ExplosionOptimizerModule(plugin));
        add(new LagShieldModule(plugin));
        add(new MobAiReducerModule(plugin));
        add(new RedstoneLimiterModule(plugin));
        add(new ConsoleFilterModule(plugin));
        add(new InstantLeafDecayModule(plugin));
        add(new TrashDisposalModule(plugin));
        add(new VehicleMotionReducerModule(plugin));
        add(new AbilityLimiterModule(plugin));
        add(new AFKOptimizerModule(plugin));
        add(new ServerTunerModule(plugin));
        add(new ExploitDBModule(plugin));
        add(new BorderControlModule(plugin));
    }

    private void add(AbstractModule m) {
        modules.put(m.getName().toLowerCase(), m);
    }

    public Collection<AbstractModule> getModules() {
        return modules.values();
    }

    public AbstractModule get(String name) {
        if (name == null) return null;
        return modules.get(name.toLowerCase());
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractModule> T get(Class<T> clazz) {
        for (AbstractModule m : modules.values()) {
            if (clazz.isInstance(m)) return (T) m;
        }
        return null;
    }

    public void loadAll() {
        for (AbstractModule module : modules.values()) {
            try {
                module.loadConfigSection();
                List<String> errs = ConfigValidator.validateModule(module);
                if (!errs.isEmpty()) {
                    for (String e : errs) plugin.getLogger().warning("Config validator: " + e);
                    // do not load if validation fails, keep disabled
                    module.setLoaded(false);
                    continue;
                }
                boolean enabled = module.isEnabledInConfig();
                boolean success = true;
                if (enabled) {
                    success = module.loadConfig();
                }
                if (enabled && success) {
                    module.load();
                    module.setLoaded(true);
                    if (plugin.isDebug()) {
                        plugin.getLogger().fine("Loaded module " + module.getName());
                    }
                } else {
                    module.setLoaded(false);
                    if (plugin.isDebug()) {
                        plugin.getLogger().fine("Skipped module " + module.getName() + " enabled=" + enabled + " success=" + success);
                    }
                }
            } catch (Exception ex) {
                module.setLoaded(false);
                plugin.getLogger().log(Level.WARNING, "Failed to load module " + module.getName() + ": " + ex.getMessage(), ex);
            }
        }
    }

    public void disableAll() {
        for (AbstractModule module : modules.values()) {
            if (!module.isLoaded()) continue;
            try {
                module.disable();
                module.setLoaded(false);
                if (plugin.isDebug()) plugin.getLogger().fine("Disabled module " + module.getName());
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Error disabling module " + module.getName(), ex);
            }
        }
    }
}

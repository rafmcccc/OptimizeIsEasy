package com.optimizeiseasy.api;

import com.optimizeiseasy.core.objects.AbstractModule;
import org.bukkit.World;

import java.util.Collection;

public interface OptimizeIsEasyAPI {
    Collection<AbstractModule> getModules();
    AbstractModule getModule(String name);
    boolean isModuleEnabled(String name);
    boolean toggleModule(String name);
    boolean isFrozen();
    void setFrozen(boolean frozen);
    boolean isFoliaSupported();
    double getMspt();
    boolean canOptimizeWorld(World world);
}

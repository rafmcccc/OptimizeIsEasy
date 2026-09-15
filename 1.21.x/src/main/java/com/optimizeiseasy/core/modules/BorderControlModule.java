package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.modules.border.BorderListener;
import com.optimizeiseasy.core.objects.AbstractModule;
import org.bukkit.entity.Player;

public class BorderControlModule extends AbstractModule {
    private BorderListener listener;

    public BorderControlModule(OptimizeIsEasyPlugin plugin) {
        super(plugin, "BorderControl");
    }

    @Override
    public boolean loadConfig() {
        return true;
    }

    @Override
    public void load() {
        listener = new BorderListener(plugin);
        listener.register();
        if (plugin.isDebug()) plugin.getLogger().fine("BorderControl loaded");
    }

    @Override
    public void disable() {
        if (listener != null) {
            try { listener.unregister(); } catch (Throwable ignored) {}
            listener = null;
        }
    }

    public void openMainGUI(Player player) {
        if (listener != null) listener.openMainGUI(player);
    }
}

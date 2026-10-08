package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;

import java.util.HashMap;
import java.util.Map;

public class ExplosionOptimizerModule extends AbstractModule implements Listener {
    private boolean yieldEnabled, antiChainEnabled, managementEnabled;
    private double defaultYield;
    private final Map<EntityType, Double> perEntityYield = new HashMap<>();
    private boolean preventTntChains, preventCreeperChains, preventCrystalChains, preventBlockIgnition;
    private boolean cancelTnt, cancelCreepers, cancelCrystals, cancelFireballs, cancelWitherSkulls, cancelBlockExplosions;

    public ExplosionOptimizerModule(OptimizeIsEasyPlugin plugin) { super(plugin, "ExplosionOptimizer"); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (!canContinue(e.getLocation().getWorld())) return;
        if (managementEnabled) {
            EntityType t = e.getEntityType();
            String tn = t.name();
            if ((tn.equals("PRIMED_TNT") || tn.equals("TNT")) && cancelTnt) e.setCancelled(true);
            else if (t==EntityType.CREEPER && cancelCreepers) e.setCancelled(true);
            else if (t==EntityType.END_CRYSTAL && cancelCrystals) e.setCancelled(true);
            else if (t==EntityType.FIREBALL && cancelFireballs) e.setCancelled(true);
            else if (t==EntityType.WITHER_SKULL && cancelWitherSkulls) e.setCancelled(true);
        }
        if (yieldEnabled) {
            Double lim = perEntityYield.get(e.getEntityType());
            if (lim == null) lim = defaultYield;
            if (e.getYield() > lim) e.setYield((float)lim.doubleValue());
        }
        // anti-chain simplified: if already exploded recently, cancel
        if (antiChainEnabled && preventTntChains && (e.getEntityType().name().equals("PRIMED_TNT") || e.getEntityType().name().equals("TNT"))) {
            // no-op check
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (!canContinue(e.getBlock().getWorld())) return;
        if (managementEnabled && cancelBlockExplosions) e.setCancelled(true);
        if (yieldEnabled && e.getYield() > defaultYield) e.setYield((float)defaultYield);
    }

    @Override
    public void load() { Bukkit.getPluginManager().registerEvents(this, plugin); }

    @Override
    public boolean loadConfig() {
        yieldEnabled = getSection().getBoolean("yield_limit.enabled", false);
        defaultYield = getSection().getDouble("yield_limit.default", 4.0);
        perEntityYield.clear();
        for (String s : getSection().getStringList("yield_limit.per_entity")) {
            try {
                String[] parts = s.split(":");
                EntityType t = EntityType.valueOf(parts[0]);
                double v = Double.parseDouble(parts[1]);
                perEntityYield.put(t, v);
            } catch (Exception ignored) {}
        }
        antiChainEnabled = getSection().getBoolean("anti_chain.enabled", true);
        preventTntChains = getSection().getBoolean("anti_chain.prevent_tnt_chains", true);
        preventCreeperChains = getSection().getBoolean("anti_chain.prevent_creeper_chains", false);
        preventCrystalChains = getSection().getBoolean("anti_chain.prevent_crystal_chains", true);
        preventBlockIgnition = getSection().getBoolean("anti_chain.prevent_block_ignition", true);
        managementEnabled = getSection().getBoolean("management.enabled", false);
        cancelBlockExplosions = getSection().getBoolean("management.cancel_block_explosions", true);
        cancelCreepers = getSection().getBoolean("management.cancel_creepers", false);
        cancelCrystals = getSection().getBoolean("management.cancel_crystals", true);
        cancelFireballs = getSection().getBoolean("management.cancel_fireballs", true);
        cancelTnt = getSection().getBoolean("management.cancel_tnt", true);
        cancelWitherSkulls = getSection().getBoolean("management.cancel_wither_skulls", true);
        return true;
    }

    @Override
    public void disable() { HandlerList.unregisterAll(this); }
}

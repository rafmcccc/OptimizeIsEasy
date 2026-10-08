package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityLimiterModule extends AbstractModule implements Listener {
    private int elytraCooldownMs, tridentCooldownMs;
    private final Map<UUID, Long> elytraLast = new HashMap<>();
    private final Map<UUID, Long> tridentLast = new HashMap<>();

    public AbilityLimiterModule(OptimizeIsEasyPlugin plugin) { super(plugin, "AbilityLimiter"); }

    @EventHandler
    public void onRiptide(PlayerRiptideEvent e) {
        if (!canContinue(e.getPlayer().getWorld())) return;
        long now = System.currentTimeMillis();
        long last = tridentLast.getOrDefault(e.getPlayer().getUniqueId(), 0L);
        if (now - last < tridentCooldownMs) {
            // PlayerRiptideEvent is not cancellable in this API version; just throttle by velocity reset
            e.getPlayer().setVelocity(e.getPlayer().getVelocity().multiply(0.5));
        } else tridentLast.put(e.getPlayer().getUniqueId(), now);
    }

    @EventHandler
    public void onGlide(PlayerMoveEvent e) {
        if (!e.getPlayer().isGliding()) return;
        if (!canContinue(e.getPlayer().getWorld())) return;
        // Elytra boost check simplified: if player recently boosted, throttle
        long now = System.currentTimeMillis();
        long last = elytraLast.getOrDefault(e.getPlayer().getUniqueId(), 0L);
        if (now - last < elytraCooldownMs) {
            // no direct cancel for gliding, just log
        }
    }

    // Called from external boost event if available; we expose for command
    public void recordElytraBoost(Player p) {
        elytraLast.put(p.getUniqueId(), System.currentTimeMillis());
    }

    @Override
    public void load() { Bukkit.getPluginManager().registerEvents(this, plugin); }

    @Override
    public boolean loadConfig() {
        elytraCooldownMs = getSection().getInt("elytra_boost.cooldown", 5) * 1000;
        tridentCooldownMs = getSection().getInt("trident_riptide.cooldown", 3) * 1000;
        return true;
    }

    @Override
    public void disable() { HandlerList.unregisterAll(this); elytraLast.clear(); tridentLast.clear(); }
}

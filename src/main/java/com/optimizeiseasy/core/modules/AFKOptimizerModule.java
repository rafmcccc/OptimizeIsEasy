package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.support.SupportManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AFKOptimizerModule extends AbstractModule implements Listener, Runnable {
    private final Map<UUID, AFKData> afkMap = new ConcurrentHashMap<>();
    private BukkitTask task;
    private int checkInterval;
    private int checkMode;
    private boolean kickEnabled, hideEntities, teleportEnabled;
    private int kickTime, hideTime, teleportTime;
    private Location teleportLoc;

    public AFKOptimizerModule(OptimizeIsEasyPlugin plugin) { super(plugin, "AFKOptimizer"); }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) { afkMap.put(e.getPlayer().getUniqueId(), new AFKData(e.getPlayer().getLocation())); }
    @EventHandler
    public void onQuit(PlayerQuitEvent e) { afkMap.remove(e.getPlayer().getUniqueId()); }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!canContinue(p.getWorld())) continue;
            AFKData data = afkMap.computeIfAbsent(p.getUniqueId(), k-> new AFKData(p.getLocation()));
            Location cur = p.getLocation();
            boolean posAfk = data.lastLoc.getBlockX()==cur.getBlockX() && data.lastLoc.getBlockY()==cur.getBlockY() && data.lastLoc.getBlockZ()==cur.getBlockZ();
            boolean rotAfk = data.lastLoc.getYaw()==cur.getYaw() && data.lastLoc.getPitch()==cur.getPitch();
            boolean isAfk = switch (checkMode) {
                case 0 -> posAfk || rotAfk;
                case 1 -> posAfk && rotAfk;
                case 2 -> posAfk;
                case 3 -> rotAfk;
                default -> false;
            };
            if (isAfk) {
                data.afkTicks += checkInterval;
                if (kickEnabled && data.afkTicks > kickTime) {
                    String msg = "You have been kicked for being AFK";
                    try { msg = getConfig().getString("AFKOptimizer.values.kick_message", msg); } catch (Exception ignored) {}
                    p.kickPlayer(msg);
                }
            } else {
                data.afkTicks = 0;
            }
            data.lastLoc = cur;
        }
    }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (Player p: Bukkit.getOnlinePlayers()) afkMap.putIfAbsent(p.getUniqueId(), new AFKData(p.getLocation()));
        SupportManager sm = SupportManager.getInstance();
        if (sm != null) task = sm.getFork().runTimer(false, this, checkInterval, checkInterval, TimeUnit.MILLISECONDS);
        else task = Bukkit.getScheduler().runTaskTimer(plugin, this, checkInterval/50L, checkInterval/50L);
    }

    @Override
    public boolean loadConfig() {
        checkInterval = Math.max(500, getSection().getInt("afk_check.interval", 1000));
        checkMode = getSection().getInt("afk_check.mode", 0);
        kickEnabled = getSection().getBoolean("kick_afk_players.enabled", false);
        if (kickEnabled) kickTime = getSection().getInt("kick_afk_players.afk_time", 300)*1000;
        hideEntities = getSection().getBoolean("hide_entities.enabled", true);
        if (hideEntities) hideTime = getSection().getInt("hide_entities.afk_time", 180)*1000;
        teleportEnabled = getSection().getBoolean("teleport_afk_players.enabled", false);
        if (teleportEnabled) teleportTime = getSection().getInt("teleport_afk_players.afk_time", 600)*1000;
        String wName = getSection().getString("teleport_afk_players.location.world", "afk_world");
        if (teleportEnabled && wName != null && Bukkit.getWorld(wName)!=null) {
            teleportLoc = new Location(Bukkit.getWorld(wName), getSection().getDouble("teleport_afk_players.location.x"), getSection().getDouble("teleport_afk_players.location.y"), getSection().getDouble("teleport_afk_players.location.z"));
        }
        return true;
    }

    @Override
    public void disable() { HandlerList.unregisterAll(this); if (task!=null) task.cancel(); afkMap.clear(); }

    private static class AFKData {
        Location lastLoc;
        long afkTicks=0;
        AFKData(Location loc){ this.lastLoc=loc; }
    }
}

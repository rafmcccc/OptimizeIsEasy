package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.utils.ServerFileUtil;
import com.optimizeiseasy.core.utils.SoftwareDetector;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ServerTunerModule extends AbstractModule {

    public ServerTunerModule(OptimizeIsEasyPlugin plugin) {
        super(plugin, "ServerTuner");
    }

    @Override
    public boolean loadConfig() {
        try {
            File dir = new File(plugin.getDataFolder(), "profiles");
            dir.mkdirs();
            for (String p : List.of("YouHaveTrouble.kos", "FarmFriendly.kos")) {
                File out = new File(dir, p);
                if (!out.exists()) {
                    try { plugin.saveResource("profiles/" + p, false); } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().fine("ServerTuner profile init: " + t.getMessage());
        }
        return true;
    }

    @Override
    public void load() {
    }

    @Override
    public void disable() {
    }

    public List<String> listProfiles() {
        List<String> out = new ArrayList<>();
        File dir = new File(plugin.getDataFolder(), "profiles");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".kos"));
        if (files != null) {
            for (File f : files) out.add(f.getName());
        }
        if (out.isEmpty()) {
            out.add("YouHaveTrouble.kos");
            out.add("FarmFriendly.kos");
        }
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    public boolean runProfile(String profileName, boolean pregenerated, CommandSender sender) {
        if (!profileName.endsWith(".kos")) profileName += ".kos";
        File f = new File(plugin.getDataFolder(), "profiles/" + profileName);
        if (!f.exists()) {
            sender.sendMessage("§cProfile not found: §f" + profileName + " §7in §fprofiles/");
            return false;
        }
        YamlConfiguration kos = YamlConfiguration.loadConfiguration(f);
        SoftwareDetector sd = plugin.getSoftwareDetector();

        sender.sendMessage("§aRunning ServerTuner (KOS) with §f" + profileName + " §7pregenerated=" + pregenerated);
        int applied = 0;

        if (sd == null || sd.supportsMinecraft()) {
            applied += applyProp("server.properties", "network-compression-threshold", kos.get("server.network-compression-threshold"));
            applied += applyProp("server.properties", "view-distance", kos.get("server.distance.view"));
            applied += applyProp("server.properties", "simulation-distance", kos.get("server.distance.simulation"));
            applied += applyProp("server.properties", "sync-chunk-writes", kos.get("server.sync-chunk-writes"));
        }
        if (sd == null || sd.supportsBukkit()) {
            applied += applyYaml("bukkit.yml", "spawn-limits.monsters", kos.get("craftbukkit.spawn-limits.monsters"));
            applied += applyYaml("bukkit.yml", "spawn-limits.animals", kos.get("craftbukkit.spawn-limits.animals"));
            applied += applyYaml("bukkit.yml", "spawn-limits.ambient", kos.get("craftbukkit.spawn-limits.ambient"));
            applied += applyYaml("bukkit.yml", "ticks-per.monster-spawns", kos.get("craftbukkit.ticks-per.monsters"));
            applied += applyYaml("bukkit.yml", "ticks-per.animal-spawns", kos.get("craftbukkit.ticks-per.animals"));
            applied += applyYaml("bukkit.yml", "chunk-gc.period-in-ticks", kos.get("craftbukkit.chunk-gc-period-in-ticks"));
        }
        if (sd == null || sd.supportsSpigot()) {
            applied += applyYaml("spigot.yml", "world-settings.default.mob-spawn-range", kos.get("spigot.mob-spawn-range"));
            applied += applyYaml("spigot.yml", "world-settings.default.entity-activation-range.animals", kos.get("spigot.entities.activation-range.animals"));
            applied += applyYaml("spigot.yml", "world-settings.default.entity-activation-range.monsters", kos.get("spigot.entities.activation-range.monsters"));
            applied += applyYaml("spigot.yml", "world-settings.default.entity-activation-range.misc", kos.get("spigot.entities.activation-range.misc"));
            applied += applyYaml("spigot.yml", "world-settings.default.ticks-per.hopper-transfer", kos.get("spigot.hopper.transfer"));
            applied += applyYaml("spigot.yml", "world-settings.default.ticks-per.hopper-check", kos.get("spigot.hopper.check"));
            applied += applyYaml("spigot.yml", "world-settings.default.nerf-spawner-mobs", kos.get("spigot.entities.spawner-mobs-nerfed"));
        }
        if (sd == null || sd.supportsPaperWorld()) {
            String pw = "config/paper-world-defaults.yml";
            applied += applyYaml(pw, "entities.armor-stands.tick", kos.get("paper.armor-stands.tick"));
            applied += applyYaml(pw, "entities.armor-stands.do-collision-entity-lookups", kos.get("paper.armor-stands.do-collision-entity-lookups"));
            applied += applyYaml(pw, "collisions.max-entity-collisions", kos.get("paper.max-entity-collisions"));
            applied += applyYaml(pw, "collisions.fix-climbing-bypassing-cramming-rule", kos.get("paper.fix-climbing-bypass-cramming-rule"));
            applied += applyYaml(pw, "chunks.delay-chunk-unloads-by", kos.get("paper.chunks.delay-unloads"));
            applied += applyYaml(pw, "chunks.max-auto-save-chunks-per-tick", kos.get("paper.chunks.max-autosave-per-tick"));
            applied += applyYaml(pw, "chunks.prevent-moving-into-unloaded-chunks", kos.get("paper.chunks.prevent-moving-into-unloaded"));
            applied += applyYaml(pw, "entities.spawning.per-player-mob-spawns", kos.get("paper.per-player-mob-spawns"));
            applied += applyYaml(pw, "misc.redstone-implementation", kos.get("paper.redstone-implementation"));
            applied += applyYaml(pw, "hopper.disable-move-event", kos.get("paper.hoppers.disable-move-event"));
            applied += applyYaml(pw, "hopper.ignore-occluding-blocks", kos.get("paper.hoppers.ignore-occluding-blocks"));
            applied += applyYaml(pw, "tick-rates.mob-spawner", kos.get("paper.tick-rates.mob-spawner"));
            applied += applyYaml(pw, "tick-rates.grass-spread", kos.get("paper.tick-rates.grass-spread"));
            applied += applyYaml(pw, "tick-rates.container-update", kos.get("paper.tick-rates.container-update"));
            applied += applyYaml(pw, "explode-optimizations.enabled", kos.get("paper.optimise-explosions"));
            applied += applyYaml(pw, "entities.spawning.despawn-ranges.monster.hard", kos.get("paper.despawn-ranges.monster.hard"));
            applied += applyYaml(pw, "entities.spawning.despawn-ranges.monster.soft", kos.get("paper.despawn-ranges.monster.soft"));
            applied += applyYaml(pw, "entities.spawning.despawn-ranges.creature.hard", kos.get("paper.despawn-ranges.creature.hard"));
            applied += applyYaml(pw, "entities.spawning.despawn-ranges.creature.soft", kos.get("paper.despawn-ranges.creature.soft"));
            boolean overridePregen = plugin.getConfig().getBoolean("kos.override-pregenerated-world-protections", false);
            if (pregenerated || overridePregen) {
                applied += applyYaml(pw, "treasure-maps.enabled", true);
            } else {
                applied += applyYaml(pw, "treasure-maps.enabled", false);
                sender.sendMessage("§eTreasure maps disabled (world not pregenerated). Pre-generate to re-enable.");
            }
            applied += applyYaml(pw, "alt-item-despawn-rate.enabled", kos.get("paper.optimised-despawn.enabled"));
            applied += applyYaml(pw, "alt-item-despawn-rate.items.cobblestone", kos.get("paper.optimised-despawn.cobblestone"));
            applied += applyYaml(pw, "alt-item-despawn-rate.items.netherrack", kos.get("paper.optimised-despawn.netherrack"));
            applied += applyYaml(pw, "alt-item-despawn-rate.items.sand", kos.get("paper.optimised-despawn.sand"));
        }
        if (sd == null || sd.supportsPaperGlobal()) {
            String pg = "config/paper-global.yml";
            applied += applyYaml(pg, "item-validation.book-size.page-max", 1024);
            applied += applyYaml(pg, "misc.max-joins-per-tick", 3);
        }
        if (sd != null && sd.supportsPurpur()) {
            boolean tcpShield = plugin.getConfig().getBoolean("kos.using-tcpshield", false);
            if (!tcpShield) applied += applyYaml("purpur.yml", "settings.use-alternate-keepalive", kos.get("purpur.use-alternative-keepalive"));
            applied += applyYaml("purpur.yml", "world-settings.default.entities.can-use-portals", kos.get("purpur.entities.all.can-use-portals"));
            boolean overridePregen = plugin.getConfig().getBoolean("kos.override-pregenerated-world-protections", false);
            if (pregenerated || overridePregen) applied += applyYaml("purpur.yml", "world-settings.default.entities.dolphin.disable-treasure-searching", false);
            else applied += applyYaml("purpur.yml", "world-settings.default.entities.dolphin.disable-treasure-searching", true);
        }
        if (sd != null && sd.supportsPufferfish()) {
            applied += applyYaml("pufferfish.yml", "dab.enabled", kos.get("pufferfish.entities.dynamic-activation-of-brain.enabled"));
            applied += applyYaml("pufferfish.yml", "dab.max-tick-freq", kos.get("pufferfish.entities.dynamic-activation-of-brain.max-tick-freq"));
            applied += applyYaml("pufferfish.yml", "enable-async-mob-spawning", kos.get("pufferfish.entities.async-mob-spawning"));
            applied += applyYaml("pufferfish.yml", "enable-suffocation-optimization", kos.get("pufferfish.entities.suffocation-optimisation"));
        }

        plugin.setRestartRequired(true);
        sender.sendMessage("§aDone! Applied §e" + applied + " §asettings. §cRestart required.");
        if (sd != null && !sd.supportsPaperWorld()) {
            sender.sendMessage("§cYou are not running Paper - over 50 optimisations were skipped. Consider switching to Paper/Purpur.");
        }
        plugin.getLogger().info("ServerTuner applied " + applied + " settings from " + profileName);
        return true;
    }

    private int applyYaml(String file, String key, Object value) {
        if (value == null) return 0;
        try {
            if (value instanceof String s && s.equalsIgnoreCase("default")) return 0;
            boolean ok = ServerFileUtil.setYaml(file, key, value);
            return ok ? 1 : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    private int applyProp(String file, String key, Object value) {
        if (value == null) return 0;
        try {
            boolean ok = ServerFileUtil.setProperty(file, key, String.valueOf(value));
            return ok ? 1 : 0;
        } catch (Throwable t) {
            return 0;
        }
    }
}

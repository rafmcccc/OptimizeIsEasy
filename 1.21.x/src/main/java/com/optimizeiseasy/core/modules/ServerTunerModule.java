package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.managers.BackupManager;
import com.optimizeiseasy.core.objects.AbstractModule;
import com.optimizeiseasy.core.utils.ServerFileUtil;
import com.optimizeiseasy.core.utils.SoftwareDetector;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServerTunerModule extends AbstractModule {

    private enum VType { INT, NUM, BOOL, STR, DUR, PROP }

    private record TunKey(String kos, String file, String path, VType type) {}

    public ServerTunerModule(OptimizeIsEasyPlugin plugin) {
        super(plugin, "ServerTuner");
    }

    @Override
    public boolean loadConfig() {
        try {
            File dir = new File(plugin.getDataFolder(), "profiles");
            dir.mkdirs();
            for (String p : List.of("YouHaveTrouble.kos", "FarmFriendly.kos", "Balanced.kos", "LowEnd.kos")) {
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
            out.add("Balanced.kos");
            out.add("LowEnd.kos");
        }
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    public boolean runProfile(String profileName, boolean pregenerated, CommandSender sender) {
        if (profileName == null) return false;
        if (!profileName.endsWith(".kos")) profileName += ".kos";
        if (profileName.contains("..") || profileName.contains("/") || profileName.contains("\\")) {
            sender.sendMessage("§cInvalid profile name.");
            return false;
        }
        try {
            File dir = new File(plugin.getDataFolder(), "profiles").getCanonicalFile();
            File f = new File(dir, profileName).getCanonicalFile();
            if (!f.getPath().startsWith(dir.getPath() + File.separator)) {
                sender.sendMessage("§cInvalid profile name.");
                return false;
            }
            if (!f.isFile()) {
                sender.sendMessage("§cProfile not found: §f" + profileName + " §7in §fprofiles/");
                return false;
            }
            return applyProfile(f, pregenerated, sender);
        } catch (Exception e) {
            sender.sendMessage("§cCould not load profile: §f" + profileName);
            return false;
        }
    }

    private boolean applyProfile(File f, boolean pregenerated, CommandSender sender) {
        YamlConfiguration kos = YamlConfiguration.loadConfiguration(f);
        SoftwareDetector sd = plugin.getSoftwareDetector();
        String profileName = f.getName();

        sender.sendMessage("§aRunning ServerTuner (KOS) with §f" + profileName + " §7pregenerated=" + pregenerated);
        int applied = 0, skipped = 0, failed = 0;
        Set<String> backedUp = new HashSet<>();

        for (TunKey k : keys()) {
            Object raw = kos.get(k.kos());
            if (raw == null) { skipped++; continue; }
            if (raw instanceof String s && (s.equalsIgnoreCase("default"))) { skipped++; continue; }
            if (!supports(sd, k.file())) { skipped++; continue; }
            if (!new File(k.file()).isFile()) { skipped++; continue; }
            Object value = coerce(raw, k.type());
            if (value == null) { skipped++; continue; }
            if (!backedUp.contains(k.file())) {
                try { new BackupManager(plugin).backup(new File(k.file())); } catch (Throwable ignored) {}
                backedUp.add(k.file());
            }
            boolean ok = k.type() == VType.PROP
                    ? ServerFileUtil.setProperty(k.file(), k.path(), String.valueOf(value))
                    : ServerFileUtil.setYaml(k.file(), k.path(), value);
            if (ok) applied++; else failed++;
        }

        boolean overridePregen = plugin.getConfig().getBoolean("kos.override-pregenerated-world-protections", false);
        if (sd == null || sd.supportsPaperWorld()) {
            String pw = "config/paper-world-defaults.yml";
            if (new File(pw).isFile()) {
                if (!backedUp.contains(pw)) {
                    try { new BackupManager(plugin).backup(new File(pw)); } catch (Throwable ignored) {}
                    backedUp.add(pw);
                }
                if (ServerFileUtil.setYaml(pw, "environment.treasure-maps.enabled", pregenerated || overridePregen)) applied++;
                else failed++;
                if (!pregenerated && !overridePregen) sender.sendMessage("§eTreasure maps disabled (world not pregenerated). Pre-generate to re-enable.");
            } else skipped++;
        }
        if (sd != null && sd.supportsPurpur()) {
            String pf = "purpur.yml";
            if (new File(pf).isFile()) {
                if (!backedUp.contains(pf)) {
                    try { new BackupManager(plugin).backup(new File(pf)); } catch (Throwable ignored) {}
                    backedUp.add(pf);
                }
                if (ServerFileUtil.setYaml(pf, "world-settings.default.mobs.dolphin.disable-treasure-searching", !(pregenerated || overridePregen))) applied++;
                else failed++;
            } else skipped++;
        }
        if (sd == null || sd.supportsPaperGlobal()) {
            String pg = "config/paper-global.yml";
            if (new File(pg).isFile()) {
                if (!backedUp.contains(pg)) {
                    try { new BackupManager(plugin).backup(new File(pg)); } catch (Throwable ignored) {}
                    backedUp.add(pg);
                }
                if (ServerFileUtil.setYaml(pg, "item-validation.book-size.page-max", 1024)) applied++; else failed++;
                if (ServerFileUtil.setYaml(pg, "misc.max-joins-per-tick", 3)) applied++; else failed++;
            } else skipped += 2;
        }

        plugin.setRestartRequired(true);
        sender.sendMessage("§aDone! Applied §e" + applied + " §asettings§7, skipped §e" + skipped + "§7, failed §e" + failed + "§7. §cRestart required.");
        sender.sendMessage("§7Originals backed up to §fplugins/OptimizeIsEasy/backups/");
        if (sd != null && !sd.supportsPaperWorld()) {
            sender.sendMessage("§cYou are not running Paper - over 50 optimisations were skipped. Consider switching to Paper/Purpur.");
        }
        plugin.getLogger().info("ServerTuner applied " + applied + "/" + (applied + skipped + failed) + " settings from " + profileName);
        return failed == 0;
    }

    private boolean supports(SoftwareDetector sd, String file) {
        if (sd == null) return true;
        return switch (file) {
            case "server.properties" -> sd.supportsMinecraft();
            case "bukkit.yml" -> sd.supportsBukkit();
            case "spigot.yml" -> sd.supportsSpigot();
            case "config/paper-world-defaults.yml" -> sd.supportsPaperWorld();
            case "config/paper-global.yml" -> sd.supportsPaperGlobal();
            case "purpur.yml" -> sd.supportsPurpur();
            case "pufferfish.yml" -> sd.supportsPufferfish();
            default -> false;
        };
    }

    private Object coerce(Object raw, VType type) {
        try {
            return switch (type) {
                case INT -> {
                    if (raw instanceof Number n) yield n.intValue();
                    yield Integer.parseInt(String.valueOf(raw).trim());
                }
                case NUM -> {
                    if (raw instanceof Number n) yield n;
                    String s = String.valueOf(raw).trim();
                    yield s.contains(".") ? Double.parseDouble(s) : Integer.parseInt(s);
                }
                case BOOL -> {
                    if (raw instanceof Boolean b) yield b;
                    if (raw instanceof Number n) yield n.intValue() != 0;
                    yield Boolean.parseBoolean(String.valueOf(raw).trim());
                }
                case STR -> String.valueOf(raw);
                case DUR -> {
                    if (raw instanceof Number n) yield n.intValue() + "s";
                    String s = String.valueOf(raw).trim();
                    yield s.matches("\\d+") ? s + "s" : s;
                }
                case PROP -> {
                    if (raw instanceof Boolean b) yield b.toString();
                    if (raw instanceof Number n) {
                        if (n.doubleValue() == n.intValue()) yield String.valueOf(n.intValue());
                        yield String.valueOf(n);
                    }
                    yield String.valueOf(raw);
                }
            };
        } catch (Exception e) {
            return null;
        }
    }

    private static List<TunKey> keys() {
        List<TunKey> k = new ArrayList<>();
        // server.properties
        k.add(new TunKey("server.network-compression-threshold", "server.properties", "network-compression-threshold", VType.PROP));
        k.add(new TunKey("server.distance.view", "server.properties", "view-distance", VType.PROP));
        k.add(new TunKey("server.distance.simulation", "server.properties", "simulation-distance", VType.PROP));
        k.add(new TunKey("server.sync-chunk-writes", "server.properties", "sync-chunk-writes", VType.PROP));
        k.add(new TunKey("server.allow-flight", "server.properties", "allow-flight", VType.PROP));
        // bukkit.yml
        k.add(new TunKey("craftbukkit.spawn-limits.monsters", "bukkit.yml", "spawn-limits.monsters", VType.INT));
        k.add(new TunKey("craftbukkit.spawn-limits.animals", "bukkit.yml", "spawn-limits.animals", VType.INT));
        k.add(new TunKey("craftbukkit.spawn-limits.axolotls", "bukkit.yml", "spawn-limits.axolotls", VType.INT));
        k.add(new TunKey("craftbukkit.spawn-limits.ambient", "bukkit.yml", "spawn-limits.ambient", VType.INT));
        k.add(new TunKey("craftbukkit.spawn-limits.water.animals", "bukkit.yml", "spawn-limits.water-animals", VType.INT));
        k.add(new TunKey("craftbukkit.spawn-limits.water.ambient", "bukkit.yml", "spawn-limits.water-ambient", VType.INT));
        k.add(new TunKey("craftbukkit.spawn-limits.water.underground-creature", "bukkit.yml", "spawn-limits.water-underground-creature", VType.INT));
        k.add(new TunKey("craftbukkit.spawn-limits.water.underground-water-creature", "bukkit.yml", "spawn-limits.water-underground-creature", VType.INT));
        k.add(new TunKey("craftbukkit.ticks-per.monsters", "bukkit.yml", "ticks-per.monster-spawns", VType.INT));
        k.add(new TunKey("craftbukkit.ticks-per.animals", "bukkit.yml", "ticks-per.animal-spawns", VType.INT));
        k.add(new TunKey("craftbukkit.ticks-per.axolotls", "bukkit.yml", "ticks-per.axolotl-spawns", VType.INT));
        k.add(new TunKey("craftbukkit.ticks-per.ambient", "bukkit.yml", "ticks-per.ambient-spawns", VType.INT));
        k.add(new TunKey("craftbukkit.ticks-per.water.animals", "bukkit.yml", "ticks-per.water-spawns", VType.INT));
        k.add(new TunKey("craftbukkit.ticks-per.water.ambient", "bukkit.yml", "ticks-per.water-ambient-spawns", VType.INT));
        k.add(new TunKey("craftbukkit.ticks-per.water.underground-creature", "bukkit.yml", "ticks-per.water-underground-creature-spawns", VType.INT));
        k.add(new TunKey("craftbukkit.ticks-per.water.underground-water-creature", "bukkit.yml", "ticks-per.water-underground-creature-spawns", VType.INT));
        k.add(new TunKey("craftbukkit.chunk-gc-period-in-ticks", "bukkit.yml", "chunk-gc.period-in-ticks", VType.INT));
        // spigot.yml
        k.add(new TunKey("spigot.view-distance", "spigot.yml", "world-settings.default.view-distance", VType.STR));
        k.add(new TunKey("spigot.mob-spawn-range", "spigot.yml", "world-settings.default.mob-spawn-range", VType.INT));
        k.add(new TunKey("spigot.entities.activation-range.animals", "spigot.yml", "world-settings.default.entity-activation-range.animals", VType.INT));
        k.add(new TunKey("spigot.entities.activation-range.monsters", "spigot.yml", "world-settings.default.entity-activation-range.monsters", VType.INT));
        k.add(new TunKey("spigot.entities.activation-range.raiders", "spigot.yml", "world-settings.default.entity-activation-range.raiders", VType.INT));
        k.add(new TunKey("spigot.entities.activation-range.misc", "spigot.yml", "world-settings.default.entity-activation-range.misc", VType.INT));
        k.add(new TunKey("spigot.entities.activation-range.water", "spigot.yml", "world-settings.default.entity-activation-range.water", VType.INT));
        k.add(new TunKey("spigot.entities.activation-range.villagers", "spigot.yml", "world-settings.default.entity-activation-range.villagers", VType.INT));
        k.add(new TunKey("spigot.entities.activation-range.flying", "spigot.yml", "world-settings.default.entity-activation-range.flying-monsters", VType.INT));
        k.add(new TunKey("spigot.entities.tracking-range.players", "spigot.yml", "world-settings.default.entity-tracking-range.players", VType.INT));
        k.add(new TunKey("spigot.entities.tracking-range.animals", "spigot.yml", "world-settings.default.entity-tracking-range.animals", VType.INT));
        k.add(new TunKey("spigot.entities.tracking-range.monsters", "spigot.yml", "world-settings.default.entity-tracking-range.monsters", VType.INT));
        k.add(new TunKey("spigot.entities.tracking-range.misc", "spigot.yml", "world-settings.default.entity-tracking-range.misc", VType.INT));
        k.add(new TunKey("spigot.entities.tracking-range.other", "spigot.yml", "world-settings.default.entity-tracking-range.other", VType.INT));
        k.add(new TunKey("spigot.entities.tick-inactive-villagers", "spigot.yml", "world-settings.default.entity-activation-range.tick-inactive-villagers", VType.BOOL));
        k.add(new TunKey("spigot.entities.spawner-mobs-nerfed", "spigot.yml", "world-settings.default.nerf-spawner-mobs", VType.BOOL));
        k.add(new TunKey("spigot.hopper.transfer", "spigot.yml", "world-settings.default.ticks-per.hopper-transfer", VType.INT));
        k.add(new TunKey("spigot.hopper.check", "spigot.yml", "world-settings.default.ticks-per.hopper-check", VType.INT));
        // paper-world-defaults.yml
        String pw = "config/paper-world-defaults.yml";
        k.add(new TunKey("paper.chunks.delay-unloads", pw, "chunks.delay-chunk-unloads-by", VType.DUR));
        k.add(new TunKey("paper.chunks.max-autosave-per-tick", pw, "chunks.max-auto-save-chunks-per-tick", VType.INT));
        k.add(new TunKey("paper.chunks.prevent-moving-into-unloaded", pw, "chunks.prevent-moving-into-unloaded-chunks", VType.BOOL));
        String[] saveTypes = {"area-effect-cloud:area_effect_cloud", "arrow:arrow", "dragon-fireball:dragon_fireball",
                "egg:egg", "ender-pearl:ender_pearl", "experience-bottle:experience_bottle", "experience-orb:experience_orb",
                "eye-of-ender:eye_of_ender", "fireball:fireball", "llama-spit:llama_spit", "potion:splash_potion",
                "shulker-bullet:shulker_bullet", "small-fireball:small_fireball", "snowball:snowball",
                "spectral-arrow:spectral_arrow", "trident:trident", "wither-skull:wither_skull"};
        for (String pair : saveTypes) {
            String[] kv = pair.split(":");
            k.add(new TunKey("paper.chunks.entity-save-limit." + kv[0], pw, "chunks.entity-per-chunk-save-limit." + kv[1], VType.INT));
        }
        String[] dr = {"ambient:ambient", "axolotl:axolotls", "creature:creature", "misc:misc", "monster:monster"};
        for (String pair : dr) {
            String[] kv = pair.split(":");
            k.add(new TunKey("paper.despawn-ranges." + kv[0] + ".hard", pw, "entities.spawning.despawn-ranges." + kv[1] + ".hard", VType.INT));
            k.add(new TunKey("paper.despawn-ranges." + kv[0] + ".soft", pw, "entities.spawning.despawn-ranges." + kv[1] + ".soft", VType.INT));
        }
        k.add(new TunKey("paper.despawn-ranges.water.underground-creature.hard", pw, "entities.spawning.despawn-ranges.underground_water_creature.hard", VType.INT));
        k.add(new TunKey("paper.despawn-ranges.water.underground-creature.soft", pw, "entities.spawning.despawn-ranges.underground_water_creature.soft", VType.INT));
        k.add(new TunKey("paper.despawn-ranges.water.ambient.hard", pw, "entities.spawning.despawn-ranges.water_ambient.hard", VType.INT));
        k.add(new TunKey("paper.despawn-ranges.water.ambient.soft", pw, "entities.spawning.despawn-ranges.water_ambient.soft", VType.INT));
        k.add(new TunKey("paper.despawn-ranges.water.creature.hard", pw, "entities.spawning.despawn-ranges.water_creature.hard", VType.INT));
        k.add(new TunKey("paper.despawn-ranges.water.creature.soft", pw, "entities.spawning.despawn-ranges.water_creature.soft", VType.INT));
        String[] dt = {"llama-spit:llama_spit", "snowball:snowball", "fireball:fireball", "dragon-fireball:dragon_fireball",
                "small-fireball:small_fireball", "arrow:arrow", "shulker-bullet:shulker_bullet", "wither-skull:wither_skull", "trident:trident"};
        for (String pair : dt) {
            String[] kv = pair.split(":");
            k.add(new TunKey("paper.despawn-time." + kv[0], pw, "entities.spawning.despawn-time." + kv[1], VType.INT));
        }
        k.add(new TunKey("paper.per-player-mob-spawns", pw, "entities.spawning.per-player-mob-spawns", VType.BOOL));
        k.add(new TunKey("paper.max-entity-collisions", pw, "collisions.max-entity-collisions", VType.INT));
        k.add(new TunKey("paper.update-pathfinding-on-block-update", pw, "misc.update-pathfinding-on-block-update", VType.BOOL));
        k.add(new TunKey("paper.fix-climbing-bypass-cramming-rule", pw, "collisions.fix-climbing-bypassing-cramming-rule", VType.BOOL));
        k.add(new TunKey("paper.armor-stands.tick", pw, "entities.armor-stands.tick", VType.BOOL));
        k.add(new TunKey("paper.armor-stands.do-collision-entity-lookups", pw, "entities.armor-stands.do-collision-entity-lookups", VType.BOOL));
        k.add(new TunKey("paper.nerfed-spawner-mobs-can-jump", pw, "spawner-nerfed-mobs-should-jump", VType.BOOL));
        k.add(new TunKey("paper.tick-rates.villager.behaviour.nearby-poi", pw, "tick-rates.behavior.villager.validatenearbypoi", VType.INT));
        k.add(new TunKey("paper.tick-rates.villager.behaviour.acquire-poi", pw, "tick-rates.behavior.villager.acquirepoi", VType.INT));
        k.add(new TunKey("paper.tick-rates.villager.sensor.secondary-poi", pw, "tick-rates.sensor.villager.secondarypoisensor", VType.INT));
        k.add(new TunKey("paper.tick-rates.villager.sensor.nearest-bed", pw, "tick-rates.sensor.villager.nearestbedsensor", VType.INT));
        k.add(new TunKey("paper.tick-rates.villager.sensor.villager-babies", pw, "tick-rates.sensor.villager.villagerbabiessensor", VType.INT));
        k.add(new TunKey("paper.tick-rates.villager.sensor.player", pw, "tick-rates.sensor.villager.playersensor", VType.INT));
        k.add(new TunKey("paper.tick-rates.villager.sensor.nearest-living-entity", pw, "tick-rates.sensor.villager.nearestlivingentitysensor", VType.INT));
        k.add(new TunKey("paper.tick-rates.mob-spawner", pw, "tick-rates.mob-spawner", VType.INT));
        k.add(new TunKey("paper.tick-rates.grass-spread", pw, "tick-rates.grass-spread", VType.INT));
        k.add(new TunKey("paper.tick-rates.container-update", pw, "tick-rates.container-update", VType.INT));
        k.add(new TunKey("paper.optimised-despawn.enabled", pw, "entities.spawning.alt-item-despawn-rate.enabled", VType.BOOL));
        String[] items = {"cobblestone", "netherrack", "sand", "red-sand:red_sand", "gravel", "dirt", "short-grass:short_grass",
                "pumpkin", "melon-slice:melon_slice", "kelp", "bamboo", "sugar-cane:sugar_cane", "twisting-vines:twisting_vines",
                "weeping-vines:weeping_vines", "oak-leaves:oak_leaves", "spruce-leaves:spruce_leaves", "birch-leaves:birch_leaves",
                "jungle-leaves:jungle_leaves", "acacia-leaves:acacia_leaves", "dark-oak-leaves:dark_oak_leaves",
                "mangrove-leaves:mangrove_leaves", "cactus", "diorite", "granite", "andesite", "scaffolding", "egg:egg"};
        for (String item : items) {
            String[] kv = item.contains(":") ? item.split(":") : new String[]{item, item};
            k.add(new TunKey("paper.optimised-despawn." + kv[0], pw, "entities.spawning.alt-item-despawn-rate.items." + kv[1], VType.INT));
        }
        k.add(new TunKey("paper.optimised-despawn.arrow.non-player", pw, "entities.spawning.non-player-arrow-despawn-rate", VType.INT));
        k.add(new TunKey("paper.optimised-despawn.arrow.creative", pw, "entities.spawning.creative-arrow-despawn-rate", VType.INT));
        k.add(new TunKey("paper.redstone-implementation", pw, "misc.redstone-implementation", VType.STR));
        k.add(new TunKey("paper.hoppers.ignore-occluding-blocks", pw, "hopper.ignore-occluding-blocks", VType.BOOL));
        k.add(new TunKey("paper.optimise-explosions", pw, "environment.optimize-explosions", VType.BOOL));
        k.add(new TunKey("paper.find-already-discovered-loot-tables", pw, "environment.treasure-maps.find-already-discovered.loot-tables", VType.BOOL));
        k.add(new TunKey("paper.find-already-discovered-villager-trade", pw, "environment.treasure-maps.find-already-discovered.villager-trade", VType.BOOL));
        k.add(new TunKey("paper.xp-orb-groups-per-area", pw, "misc.xp-orb-groups-per-area", VType.INT));
        // purpur.yml
        k.add(new TunKey("purpur.use-alternative-keepalive", "purpur.yml", "settings.use-alternate-keepalive", VType.BOOL));
        k.add(new TunKey("purpur.entities.zombie.aggressive-towards-villager-when-lagging", "purpur.yml", "world-settings.default.mobs.zombie.aggressive-towards-villager-when-lagging", VType.BOOL));
        k.add(new TunKey("purpur.entities.all.can-use-portals", "purpur.yml", "world-settings.default.gameplay-mechanics.entities-can-use-portals", VType.BOOL));
        k.add(new TunKey("purpur.entities.villager.lobotomized", "purpur.yml", "world-settings.default.mobs.villager.lobotomize.enabled", VType.BOOL));
        k.add(new TunKey("purpur.entities.villager.search-radius.acquire-poi", "purpur.yml", "world-settings.default.mobs.villager.search-radius.acquire-poi", VType.INT));
        k.add(new TunKey("purpur.entities.villager.search-radius.nearest-bed-sensor", "purpur.yml", "world-settings.default.mobs.villager.search-radius.nearest-bed-sensor", VType.INT));
        k.add(new TunKey("purpur.teleport-if-outside-worldborder", "purpur.yml", "world-settings.default.gameplay-mechanics.player.teleport-if-outside-border", VType.BOOL));
        k.add(new TunKey("purpur.lagging-tps-threshold", "purpur.yml", "settings.lagging-threshold", VType.NUM));
        // pufferfish.yml
        k.add(new TunKey("pufferfish.max-loads-per-projectile", "pufferfish.yml", "projectile.max-loads-per-projectile", VType.INT));
        k.add(new TunKey("pufferfish.entities.dynamic-activation-of-brain.enabled", "pufferfish.yml", "dab.enabled", VType.BOOL));
        k.add(new TunKey("pufferfish.entities.dynamic-activation-of-brain.max-tick-freq", "pufferfish.yml", "dab.max-tick-freq", VType.INT));
        k.add(new TunKey("pufferfish.entities.dynamic-activation-of-brain.activation-distance-modifier", "pufferfish.yml", "dab.activation-dist-mod", VType.INT));
        k.add(new TunKey("pufferfish.entities.async-mob-spawning", "pufferfish.yml", "enable-async-mob-spawning", VType.BOOL));
        k.add(new TunKey("pufferfish.entities.suffocation-optimisation", "pufferfish.yml", "enable-suffocation-optimization", VType.BOOL));
        k.add(new TunKey("pufferfish.entities.inactive-goal-selector-throttle", "pufferfish.yml", "inactive-goal-selector-throttle", VType.BOOL));
        k.add(new TunKey("pufferfish.disable-method-profiler", "pufferfish.yml", "misc.disable-method-profiler", VType.BOOL));
        return k;
    }
}

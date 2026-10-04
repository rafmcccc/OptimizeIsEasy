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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServerTunerModule extends AbstractModule {

    private enum VType { INT, NUM, BOOL, STR, DUR, PROP }

    /**
     * @param strict when true the key is only written if it already exists in the target
     *               file. Forks rename and drop options between Minecraft versions, so a
     *               blind write would invent keys the running server knows nothing about.
     */
    private record TunKey(String kos, String file, String path, VType type, boolean strict) {}

    private static TunKey key(String kos, String file, String path, VType type) {
        return new TunKey(kos, file, path, type, false);
    }

    private static TunKey strictKey(String kos, String file, String path, VType type) {
        return new TunKey(kos, file, path, type, true);
    }

    public ServerTunerModule(OptimizeIsEasyPlugin plugin) {
        super(plugin, "ServerTuner");
    }

    @Override
    public boolean loadConfig() {
        try {
            File dir = new File(plugin.getDataFolder(), "profiles");
            dir.mkdirs();
            for (String p : List.of("YouHaveTrouble.kos", "FarmFriendly.kos", "Balanced.kos", "LowEnd.kos",
                    "HighEnd.kos", "VanillaPlus.kos", "LobbyGames.kos", "Anarchy.kos")) {
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

    public boolean isDryRun() {
        return getSection() != null && getSection().getBoolean("dry-run", false);
    }

    public boolean isBackupBeforeApply() {
        return getSection() == null || getSection().getBoolean("backup-before-apply", true);
    }

    public String defaultProfile() {
        if (getSection() != null) {
            String v = getSection().getString("default-profile");
            if (v != null && !v.isBlank()) return v;
        }
        return plugin.getConfig().getString("kos.default-profile", "YouHaveTrouble.kos");
    }

    public Boolean storedPregen() {
        int v = -1;
        if (getSection() != null && getSection().contains("world-is-pregenerated")) {
            v = getSection().getInt("world-is-pregenerated", 0);
        }
        if (v < 0) v = plugin.getConfig().getInt("kos.world-is-pregenerated", 0);
        if (v == 1) return true;
        if (v == 2) return false;
        return null;
    }

    public boolean isDeniedProfile(String name) {
        if (getSection() == null) return false;
        for (String s : getSection().getStringList("denied-profiles")) {
            if (s.equalsIgnoreCase(name) || s.equalsIgnoreCase(stripExtension(name))) return true;
        }
        return false;
    }

    private static String stripExtension(String name) {
        return name.endsWith(".kos") ? name.substring(0, name.length() - 4) : name;
    }

    public List<String> listProfiles() {
        List<String> allowed = new ArrayList<>();
        List<String> denied = new ArrayList<>();
        File dir = new File(plugin.getDataFolder(), "profiles");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".kos"));
        if (files != null) {
            for (File f : files) {
                if (isDeniedProfile(f.getName())) denied.add(f.getName());
                else allowed.add(f.getName());
            }
        }
        if (allowed.isEmpty()) {
            for (String p : List.of("YouHaveTrouble.kos", "FarmFriendly.kos", "Balanced.kos", "LowEnd.kos",
                    "HighEnd.kos", "VanillaPlus.kos", "LobbyGames.kos", "Anarchy.kos")) {
                if (!isDeniedProfile(p) && !allowed.contains(p)) allowed.add(p);
            }
        }
        if (denied.isEmpty() && allowed.isEmpty()) allowed.add("YouHaveTrouble.kos");
        allowed.sort(String::compareToIgnoreCase);
        return allowed;
    }

    public List<String> deniedProfiles() {
        List<String> denied = new ArrayList<>();
        File dir = new File(plugin.getDataFolder(), "profiles");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".kos"));
        if (files != null) {
            for (File f : files) if (isDeniedProfile(f.getName())) denied.add(f.getName());
        }
        if (getSection() != null) {
            for (String s : getSection().getStringList("denied-profiles")) {
                String name = s.endsWith(".kos") ? s : s + ".kos";
                if (!denied.contains(name)) denied.add(name);
            }
        }
        denied.sort(String::compareToIgnoreCase);
        return denied;
    }


    public boolean runProfile(String profileName, boolean pregenerated, CommandSender sender) {
        if (profileName == null) return false;
        if (!profileName.endsWith(".kos")) profileName += ".kos";
        if (profileName.contains("..") || profileName.contains("/") || profileName.contains("\\")) {
            sender.sendMessage("§cInvalid profile name.");
            return false;
        }
        if (isDeniedProfile(profileName)) {
            sender.sendMessage("§cProfile §f" + profileName + " §cis on the denied-profiles list in modules/ServerTuner.yml.");
            return false;
        }
        if (isDryRun()) {
            sender.sendMessage("§eDry run. §7modules/ServerTuner.yml has §edry-run: true§7, so nothing will be written.");
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
        int applied = 0, skipped = 0, failed = 0, absentKeys = 0;
        Set<String> backedUp = new HashSet<>();
        boolean dry = isDryRun();

        for (TunKey k : keys()) {
            Object raw = kos.get(k.kos());
            if (raw == null) { skipped++; continue; }
            if (raw instanceof String s && (s.equalsIgnoreCase("default"))) { skipped++; continue; }
            if (!supports(sd, k.file())) { skipped++; continue; }
            if (!new File(k.file()).isFile()) { skipped++; continue; }
            Object value = coerce(raw, k.type());
            if (value == null) { skipped++; continue; }
            if (dry) {
                // A strict key that the server version does not have would be a
                // no-op for real, so a dry run must not count it as applied.
                if (k.strict() && !ServerFileUtil.hasKey(k.file(), k.path())) { skipped++; absentKeys++; continue; }
                applied++;
                continue;
            }
            backupOnce(k.file(), backedUp);
            switch (write(k, value)) {
                case WROTE -> applied++;
                case MISSING_KEY -> {
                    skipped++;
                    absentKeys++;
                    plugin.getLogger().fine(profileName + ": " + k.file() + " has no '" + k.path()
                            + "' - absent in this server version, skipped.");
                }
                default -> failed++;
            }
        }
        if (absentKeys > 0) {
            sender.sendMessage("§7" + absentKeys + " §7keys are not in this server version and were §8skipped§7, nothing was invented."
                    + (dry ? "" : " Enable debug for the list."));
        }

        boolean overridePregen = (getSection() != null && getSection().getBoolean("allow-override-pregenerated", false))
                || plugin.getConfig().getBoolean("kos.override-pregenerated-world-protections", false);
        if (sd == null || sd.supportsPaperWorld()) {
            String pw = "config/paper-world-defaults.yml";
            if (new File(pw).isFile()) {
                if (!dry) backupOnce(pw, backedUp);
                if (dry || ServerFileUtil.setYaml(pw, "environment.treasure-maps.enabled", pregenerated || overridePregen)) applied++;
                else failed++;
                if (!pregenerated && !overridePregen) sender.sendMessage("§eTreasure maps disabled (world not pregenerated). Pre-generate to re-enable.");
            } else skipped++;
        }
        if (sd != null && sd.supportsPurpur()) {
            String pf = "purpur.yml";
            if (new File(pf).isFile()) {
                if (!dry) backupOnce(pf, backedUp);
                if (dry || ServerFileUtil.setYaml(pf, "world-settings.default.mobs.dolphin.disable-treasure-searching", !(pregenerated || overridePregen))) applied++;
                else failed++;
            } else skipped++;
        }
        if (sd == null || sd.supportsPaperGlobal()) {
            String pg = "config/paper-global.yml";
            if (new File(pg).isFile()) {
                if (!dry) backupOnce(pg, backedUp);
                if (dry || ServerFileUtil.setYaml(pg, "item-validation.book-size.page-max", 1024)) applied++; else failed++;
                if (dry || ServerFileUtil.setYaml(pg, "misc.max-joins-per-tick", 3)) applied++; else failed++;
            } else skipped += 2;
        }

        if (!dry) plugin.setRestartRequired(true);
        sender.sendMessage(dry
                ? "§eDry run done. §7Would apply §e" + applied + " §7settings. Nothing was written."
                : "§aDone! Applied §e" + applied + " §asettings§7, skipped §e" + skipped + "§7, failed §e" + failed + "§7. §cRestart required.");
        if (!dry) sender.sendMessage("§7Originals backed up to §fplugins/OptimizeIsEasy/backups/");
        if (sd != null && !sd.supportsPaperWorld()) {
            sender.sendMessage("§cYou are not running Paper - over 50 optimisations were skipped. Consider switching to Paper/Purpur.");
        }
        List<String> denied = deniedProfiles();
        if (!denied.isEmpty()) {
            sender.sendMessage("§7Hidden by denied-profiles: §8" + String.join(", ", denied));
        }
        plugin.getLogger().info("ServerTuner " + (dry ? "dry-run " : "") + applied + "/" + (applied + skipped + failed)
                + " settings from " + profileName);
        return failed == 0;
    }

    private void backupOnce(String file, Set<String> backedUp) {
        if (!isBackupBeforeApply() || !backedUp.add(file)) return;
        try { new BackupManager(plugin).backup(new File(file)); } catch (Throwable ignored) {}
    }

    private ServerFileUtil.WriteResult write(TunKey k, Object value) {
        if (k.type() == VType.PROP) {
            return ServerFileUtil.setProperty(k.file(), k.path(), String.valueOf(value))
                    ? ServerFileUtil.WriteResult.WROTE : ServerFileUtil.WriteResult.FAILED;
        }
        if (k.strict()) return ServerFileUtil.setYamlStrict(k.file(), k.path(), value);
        return ServerFileUtil.setYaml(k.file(), k.path(), value)
                ? ServerFileUtil.WriteResult.WROTE : ServerFileUtil.WriteResult.FAILED;
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
            case "config/gale-global.yml" -> sd.supportsGale();
            case "config/leaf-global.yml" -> sd.supportsLeaf();
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
        k.add(key("server.network-compression-threshold", "server.properties", "network-compression-threshold", VType.PROP));
        k.add(key("server.distance.view", "server.properties", "view-distance", VType.PROP));
        k.add(key("server.distance.simulation", "server.properties", "simulation-distance", VType.PROP));
        k.add(key("server.sync-chunk-writes", "server.properties", "sync-chunk-writes", VType.PROP));
        k.add(key("server.allow-flight", "server.properties", "allow-flight", VType.PROP));
        // bukkit.yml
        k.add(key("craftbukkit.spawn-limits.monsters", "bukkit.yml", "spawn-limits.monsters", VType.INT));
        k.add(key("craftbukkit.spawn-limits.animals", "bukkit.yml", "spawn-limits.animals", VType.INT));
        k.add(key("craftbukkit.spawn-limits.axolotls", "bukkit.yml", "spawn-limits.axolotls", VType.INT));
        k.add(key("craftbukkit.spawn-limits.ambient", "bukkit.yml", "spawn-limits.ambient", VType.INT));
        k.add(key("craftbukkit.spawn-limits.water.animals", "bukkit.yml", "spawn-limits.water-animals", VType.INT));
        k.add(key("craftbukkit.spawn-limits.water.ambient", "bukkit.yml", "spawn-limits.water-ambient", VType.INT));
        k.add(key("craftbukkit.spawn-limits.water.underground-creature", "bukkit.yml", "spawn-limits.water-underground-creature", VType.INT));
        k.add(key("craftbukkit.spawn-limits.water.underground-water-creature", "bukkit.yml", "spawn-limits.water-underground-creature", VType.INT));
        k.add(key("craftbukkit.ticks-per.monsters", "bukkit.yml", "ticks-per.monster-spawns", VType.INT));
        k.add(key("craftbukkit.ticks-per.animals", "bukkit.yml", "ticks-per.animal-spawns", VType.INT));
        k.add(key("craftbukkit.ticks-per.axolotls", "bukkit.yml", "ticks-per.axolotl-spawns", VType.INT));
        k.add(key("craftbukkit.ticks-per.ambient", "bukkit.yml", "ticks-per.ambient-spawns", VType.INT));
        k.add(key("craftbukkit.ticks-per.water.animals", "bukkit.yml", "ticks-per.water-spawns", VType.INT));
        k.add(key("craftbukkit.ticks-per.water.ambient", "bukkit.yml", "ticks-per.water-ambient-spawns", VType.INT));
        k.add(key("craftbukkit.ticks-per.water.underground-creature", "bukkit.yml", "ticks-per.water-underground-creature-spawns", VType.INT));
        k.add(key("craftbukkit.ticks-per.water.underground-water-creature", "bukkit.yml", "ticks-per.water-underground-creature-spawns", VType.INT));
        k.add(key("craftbukkit.chunk-gc-period-in-ticks", "bukkit.yml", "chunk-gc.period-in-ticks", VType.INT));
        k.add(strictKey("craftbukkit.ticks-per.autosave", "bukkit.yml", "ticks-per.autosave", VType.INT));
        k.add(strictKey("craftbukkit.warn-on-overload", "bukkit.yml", "settings.warn-on-overload", VType.BOOL));
        k.add(strictKey("craftbukkit.connection-throttle", "bukkit.yml", "settings.connection-throttle", VType.INT));
        k.add(strictKey("craftbukkit.query-plugins", "bukkit.yml", "settings.query-plugins", VType.BOOL));
        k.add(strictKey("craftbukkit.deprecated-verbose", "bukkit.yml", "settings.deprecated-verbose", VType.STR));
        // spigot.yml
        k.add(key("spigot.view-distance", "spigot.yml", "world-settings.default.view-distance", VType.STR));
        k.add(key("spigot.mob-spawn-range", "spigot.yml", "world-settings.default.mob-spawn-range", VType.INT));
        k.add(key("spigot.entities.activation-range.animals", "spigot.yml", "world-settings.default.entity-activation-range.animals", VType.INT));
        k.add(key("spigot.entities.activation-range.monsters", "spigot.yml", "world-settings.default.entity-activation-range.monsters", VType.INT));
        k.add(key("spigot.entities.activation-range.raiders", "spigot.yml", "world-settings.default.entity-activation-range.raiders", VType.INT));
        k.add(key("spigot.entities.activation-range.misc", "spigot.yml", "world-settings.default.entity-activation-range.misc", VType.INT));
        k.add(key("spigot.entities.activation-range.water", "spigot.yml", "world-settings.default.entity-activation-range.water", VType.INT));
        k.add(key("spigot.entities.activation-range.villagers", "spigot.yml", "world-settings.default.entity-activation-range.villagers", VType.INT));
        k.add(key("spigot.entities.activation-range.flying", "spigot.yml", "world-settings.default.entity-activation-range.flying-monsters", VType.INT));
        k.add(key("spigot.entities.tracking-range.players", "spigot.yml", "world-settings.default.entity-tracking-range.players", VType.INT));
        k.add(key("spigot.entities.tracking-range.animals", "spigot.yml", "world-settings.default.entity-tracking-range.animals", VType.INT));
        k.add(key("spigot.entities.tracking-range.monsters", "spigot.yml", "world-settings.default.entity-tracking-range.monsters", VType.INT));
        k.add(key("spigot.entities.tracking-range.misc", "spigot.yml", "world-settings.default.entity-tracking-range.misc", VType.INT));
        k.add(key("spigot.entities.tracking-range.other", "spigot.yml", "world-settings.default.entity-tracking-range.other", VType.INT));
        k.add(key("spigot.entities.tick-inactive-villagers", "spigot.yml", "world-settings.default.entity-activation-range.tick-inactive-villagers", VType.BOOL));
        k.add(key("spigot.entities.spawner-mobs-nerfed", "spigot.yml", "world-settings.default.nerf-spawner-mobs", VType.BOOL));
        k.add(key("spigot.hopper.transfer", "spigot.yml", "world-settings.default.ticks-per.hopper-transfer", VType.INT));
        k.add(key("spigot.hopper.check", "spigot.yml", "world-settings.default.ticks-per.hopper-check", VType.INT));
        k.add(strictKey("spigot.hopper.amount", "spigot.yml", "world-settings.default.hopper-amount", VType.INT));
        k.add(strictKey("spigot.hopper.can-load-chunks", "spigot.yml", "world-settings.default.hopper-can-load-chunks", VType.BOOL));
        k.add(strictKey("spigot.simulation-distance", "spigot.yml", "world-settings.default.simulation-distance", VType.STR));
        k.add(strictKey("spigot.max-tnt-per-tick", "spigot.yml", "world-settings.default.max-tnt-per-tick", VType.INT));
        k.add(strictKey("spigot.item-despawn-rate", "spigot.yml", "world-settings.default.item-despawn-rate", VType.INT));
        k.add(strictKey("spigot.arrow-despawn-rate", "spigot.yml", "world-settings.default.arrow-despawn-rate", VType.INT));
        k.add(strictKey("spigot.trident-despawn-rate", "spigot.yml", "world-settings.default.trident-despawn-rate", VType.INT));
        k.add(strictKey("spigot.thunder-chance", "spigot.yml", "world-settings.default.thunder-chance", VType.INT));
        k.add(strictKey("spigot.zombie-aggressive-towards-villager", "spigot.yml", "world-settings.default.zombie-aggressive-towards-villager", VType.BOOL));
        k.add(strictKey("spigot.merge-radius.item", "spigot.yml", "world-settings.default.merge-radius.item", VType.NUM));
        k.add(strictKey("spigot.merge-radius.exp", "spigot.yml", "world-settings.default.merge-radius.exp", VType.NUM));
        // how often sleeping entities outside their activation range are woken back up
        for (String cat : List.of("animals", "monsters", "villagers", "flying-monsters")) {
            k.add(strictKey("spigot.entities.wake-up-inactive." + cat + ".every",
                    "spigot.yml", "world-settings.default.entity-activation-range.wake-up-inactive." + cat + "-every", VType.INT));
            k.add(strictKey("spigot.entities.wake-up-inactive." + cat + ".max-per-tick",
                    "spigot.yml", "world-settings.default.entity-activation-range.wake-up-inactive." + cat + "-max-per-tick", VType.INT));
        }
        // random growth speed, 100 is vanilla. Higher means faster farms, lower means cheaper ticks
        String[] growth = {"cane", "wheat", "carrot", "potato", "beetroot", "melon", "pumpkin", "bamboo", "kelp"};
        for (String plant : growth) {
            k.add(strictKey("spigot.growth." + plant, "spigot.yml", "world-settings.default.growth." + plant + "-modifier", VType.INT));
        }
        // paper-world-defaults.yml
        String pw = "config/paper-world-defaults.yml";
        k.add(key("paper.chunks.delay-unloads", pw, "chunks.delay-chunk-unloads-by", VType.DUR));
        k.add(key("paper.chunks.max-autosave-per-tick", pw, "chunks.max-auto-save-chunks-per-tick", VType.INT));
        k.add(key("paper.chunks.prevent-moving-into-unloaded", pw, "chunks.prevent-moving-into-unloaded-chunks", VType.BOOL));
        String[] saveTypes = {"area-effect-cloud:area_effect_cloud", "arrow:arrow", "dragon-fireball:dragon_fireball",
                "egg:egg", "ender-pearl:ender_pearl", "experience-bottle:experience_bottle", "experience-orb:experience_orb",
                "eye-of-ender:eye_of_ender", "fireball:fireball", "llama-spit:llama_spit", "potion:splash_potion",
                "shulker-bullet:shulker_bullet", "small-fireball:small_fireball", "snowball:snowball",
                "spectral-arrow:spectral_arrow", "trident:trident", "wither-skull:wither_skull"};
        for (String pair : saveTypes) {
            String[] kv = pair.split(":");
            k.add(key("paper.chunks.entity-save-limit." + kv[0], pw, "chunks.entity-per-chunk-save-limit." + kv[1], VType.INT));
        }
        String[] dr = {"ambient:ambient", "axolotl:axolotls", "creature:creature", "misc:misc", "monster:monster"};
        for (String pair : dr) {
            String[] kv = pair.split(":");
            k.add(key("paper.despawn-ranges." + kv[0] + ".hard", pw, "entities.spawning.despawn-ranges." + kv[1] + ".hard", VType.INT));
            k.add(key("paper.despawn-ranges." + kv[0] + ".soft", pw, "entities.spawning.despawn-ranges." + kv[1] + ".soft", VType.INT));
        }
        k.add(key("paper.despawn-ranges.water.underground-creature.hard", pw, "entities.spawning.despawn-ranges.underground_water_creature.hard", VType.INT));
        k.add(key("paper.despawn-ranges.water.underground-creature.soft", pw, "entities.spawning.despawn-ranges.underground_water_creature.soft", VType.INT));
        k.add(key("paper.despawn-ranges.water.ambient.hard", pw, "entities.spawning.despawn-ranges.water_ambient.hard", VType.INT));
        k.add(key("paper.despawn-ranges.water.ambient.soft", pw, "entities.spawning.despawn-ranges.water_ambient.soft", VType.INT));
        k.add(key("paper.despawn-ranges.water.creature.hard", pw, "entities.spawning.despawn-ranges.water_creature.hard", VType.INT));
        k.add(key("paper.despawn-ranges.water.creature.soft", pw, "entities.spawning.despawn-ranges.water_creature.soft", VType.INT));
        String[] dt = {"llama-spit:llama_spit", "snowball:snowball", "fireball:fireball", "dragon-fireball:dragon_fireball",
                "small-fireball:small_fireball", "arrow:arrow", "shulker-bullet:shulker_bullet", "wither-skull:wither_skull", "trident:trident"};
        for (String pair : dt) {
            String[] kv = pair.split(":");
            k.add(key("paper.despawn-time." + kv[0], pw, "entities.spawning.despawn-time." + kv[1], VType.INT));
        }
        k.add(key("paper.per-player-mob-spawns", pw, "entities.spawning.per-player-mob-spawns", VType.BOOL));
        k.add(key("paper.max-entity-collisions", pw, "collisions.max-entity-collisions", VType.INT));
        k.add(key("paper.update-pathfinding-on-block-update", pw, "misc.update-pathfinding-on-block-update", VType.BOOL));
        k.add(key("paper.fix-climbing-bypass-cramming-rule", pw, "collisions.fix-climbing-bypassing-cramming-rule", VType.BOOL));
        k.add(key("paper.armor-stands.tick", pw, "entities.armor-stands.tick", VType.BOOL));
        k.add(key("paper.armor-stands.do-collision-entity-lookups", pw, "entities.armor-stands.do-collision-entity-lookups", VType.BOOL));
        k.add(key("paper.nerfed-spawner-mobs-can-jump", pw, "spawner-nerfed-mobs-should-jump", VType.BOOL));
        k.add(key("paper.tick-rates.villager.behaviour.nearby-poi", pw, "tick-rates.behavior.villager.validatenearbypoi", VType.INT));
        k.add(key("paper.tick-rates.villager.behaviour.acquire-poi", pw, "tick-rates.behavior.villager.acquirepoi", VType.INT));
        k.add(key("paper.tick-rates.villager.sensor.secondary-poi", pw, "tick-rates.sensor.villager.secondarypoisensor", VType.INT));
        k.add(key("paper.tick-rates.villager.sensor.nearest-bed", pw, "tick-rates.sensor.villager.nearestbedsensor", VType.INT));
        k.add(key("paper.tick-rates.villager.sensor.villager-babies", pw, "tick-rates.sensor.villager.villagerbabiessensor", VType.INT));
        k.add(key("paper.tick-rates.villager.sensor.player", pw, "tick-rates.sensor.villager.playersensor", VType.INT));
        k.add(key("paper.tick-rates.villager.sensor.nearest-living-entity", pw, "tick-rates.sensor.villager.nearestlivingentitysensor", VType.INT));
        k.add(key("paper.tick-rates.mob-spawner", pw, "tick-rates.mob-spawner", VType.INT));
        k.add(key("paper.tick-rates.grass-spread", pw, "tick-rates.grass-spread", VType.INT));
        k.add(key("paper.tick-rates.container-update", pw, "tick-rates.container-update", VType.INT));
        k.add(key("paper.optimised-despawn.enabled", pw, "entities.spawning.alt-item-despawn-rate.enabled", VType.BOOL));
        String[] items = {"cobblestone", "netherrack", "sand", "red-sand:red_sand", "gravel", "dirt", "short-grass:short_grass",
                "pumpkin", "melon-slice:melon_slice", "kelp", "bamboo", "sugar-cane:sugar_cane", "twisting-vines:twisting_vines",
                "weeping-vines:weeping_vines", "oak-leaves:oak_leaves", "spruce-leaves:spruce_leaves", "birch-leaves:birch_leaves",
                "jungle-leaves:jungle_leaves", "acacia-leaves:acacia_leaves", "dark-oak-leaves:dark_oak_leaves",
                "mangrove-leaves:mangrove_leaves", "cactus", "diorite", "granite", "andesite", "scaffolding", "egg:egg"};
        for (String item : items) {
            String[] kv = item.contains(":") ? item.split(":") : new String[]{item, item};
            k.add(key("paper.optimised-despawn." + kv[0], pw, "entities.spawning.alt-item-despawn-rate.items." + kv[1], VType.INT));
        }
        k.add(key("paper.optimised-despawn.arrow.non-player", pw, "entities.spawning.non-player-arrow-despawn-rate", VType.INT));
        k.add(key("paper.optimised-despawn.arrow.creative", pw, "entities.spawning.creative-arrow-despawn-rate", VType.INT));
        k.add(key("paper.redstone-implementation", pw, "misc.redstone-implementation", VType.STR));
        k.add(key("paper.hoppers.ignore-occluding-blocks", pw, "hopper.ignore-occluding-blocks", VType.BOOL));
        k.add(key("paper.optimise-explosions", pw, "environment.optimize-explosions", VType.BOOL));
        k.add(key("paper.find-already-discovered-loot-tables", pw, "environment.treasure-maps.find-already-discovered.loot-tables", VType.BOOL));
        k.add(key("paper.find-already-discovered-villager-trade", pw, "environment.treasure-maps.find-already-discovered.villager-trade", VType.BOOL));
        k.add(key("paper.xp-orb-groups-per-area", pw, "misc.xp-orb-groups-per-area", VType.INT));
        // environment tweaks - cheap wins that vanilla servers can take without gameplay cost
        k.add(key("paper.environment.disable-thunder", pw, "environment.disable-thunder", VType.BOOL));
        k.add(key("paper.environment.disable-ice-and-snow", pw, "environment.disable-ice-and-snow", VType.BOOL));
        k.add(key("paper.environment.fire-tick-delay", pw, "environment.fire-tick-delay", VType.INT));
        k.add(key("paper.environment.portal-search-radius", pw, "environment.portal-search-radius", VType.INT));
        k.add(key("paper.environment.max-block-ticks", pw, "environment.max-block-ticks", VType.INT));
        k.add(key("paper.environment.max-fluid-ticks", pw, "environment.max-fluid-ticks", VType.INT));
        k.add(key("paper.collisions.only-players-collide", pw, "collisions.only-players-collide", VType.BOOL));
        k.add(key("paper.maps.item-frame-cursor-limit", pw, "maps.item-frame-cursor-limit", VType.INT));
        k.add(key("paper.entities.scan-for-legacy-ender-dragon", pw, "entities.spawning.scan-for-legacy-ender-dragon", VType.BOOL));
        k.add(key("paper.anticheat.enabled", pw, "anticheat.anti-xray.enabled", VType.BOOL));
        k.add(key("paper.anticheat.engine-mode", pw, "anticheat.anti-xray.engine-mode", VType.INT));
        k.add(key("paper.misc.light-queue-size", pw, "misc.light-queue-size", VType.INT));
        // paper-global.yml
        String pg = "config/paper-global.yml";
        k.add(strictKey("paper-global.chunk-system.io-threads", pg, "chunk-system.io-threads", VType.INT));
        k.add(strictKey("paper-global.chunk-system.worker-threads", pg, "chunk-system.worker-threads", VType.INT));
        k.add(strictKey("paper-global.chunk-loading.player-max-concurrent-chunk-generates", pg, "chunk-loading-advanced.player-max-concurrent-chunk-generates", VType.INT));
        k.add(strictKey("paper-global.chunk-loading.player-max-concurrent-chunk-loads", pg, "chunk-loading-advanced.player-max-concurrent-chunk-loads", VType.INT));
        k.add(strictKey("paper-global.chunk-loading.player-max-chunk-generate-rate", pg, "chunk-loading-basic.player-max-chunk-generate-rate", VType.NUM));
        k.add(strictKey("paper-global.chunk-loading.player-max-chunk-load-rate", pg, "chunk-loading-basic.player-max-chunk-load-rate", VType.NUM));
        k.add(strictKey("paper-global.chunk-loading.player-max-chunk-send-rate", pg, "chunk-loading-basic.player-max-chunk-send-rate", VType.NUM));
        k.add(strictKey("paper-global.player-auto-save.max-per-tick", pg, "player-auto-save.max-per-tick", VType.INT));
        k.add(strictKey("paper-global.player-auto-save.rate", pg, "player-auto-save.rate", VType.INT));
        k.add(strictKey("paper-global.misc.region-file-cache-size", pg, "misc.region-file-cache-size", VType.INT));
        k.add(strictKey("paper-global.misc.max-tracking-combat-entries", pg, "misc.max-tracking-combat-entries", VType.INT));
        k.add(strictKey("paper-global.scoreboards.save-empty-scoreboard-teams", pg, "scoreboards.save-empty-scoreboard-teams", VType.BOOL));
        k.add(strictKey("paper-global.spam-limiter.incoming-packet-threshold", pg, "spam-limiter.incoming-packet-threshold", VType.INT));
        k.add(strictKey("paper-global.spam-limiter.recipe-spam-limit", pg, "spam-limiter.recipe-spam-limit", VType.INT));
        k.add(strictKey("paper-global.spam-limiter.tab-spam-limit", pg, "spam-limiter.tab-spam-limit", VType.INT));
        k.add(strictKey("paper-global.unsupported-settings.allow-headless-pistons", pg, "unsupported-settings.allow-headless-pistons", VType.BOOL));
        k.add(strictKey("paper-global.unsupported-settings.allow-permanent-block-break-exploits", pg, "unsupported-settings.allow-permanent-block-break-exploits", VType.BOOL));
        k.add(strictKey("paper-global.unsupported-settings.allow-piston-duplication", pg, "unsupported-settings.allow-piston-duplication", VType.BOOL));
        k.add(strictKey("paper-global.unsupported-settings.allow-unsafe-end-portal-teleportation", pg, "unsupported-settings.allow-unsafe-end-portal-teleportation", VType.BOOL));
        // global perf and anti-spam switches. All strict: forks rename these between versions.
        k.add(strictKey("paper-global.collisions.enable-player-collisions", pg, "collisions.enable-player-collisions", VType.BOOL));
        k.add(strictKey("paper-global.timings.enabled", pg, "timings.enabled", VType.BOOL));
        k.add(strictKey("paper-global.spam-limiter.recipe-spam-increment", pg, "spam-limiter.recipe-spam-increment", VType.INT));
        k.add(strictKey("paper-global.spam-limiter.tab-spam-increment", pg, "spam-limiter.tab-spam-increment", VType.INT));
        k.add(strictKey("paper-global.item-validation.book-page-max", pg, "item-validation.book-size.page-max", VType.INT));
        k.add(strictKey("paper-global.item-validation.display-name", pg, "item-validation.display-name", VType.INT));
        k.add(strictKey("paper-global.item-validation.lore-line", pg, "item-validation.lore-line", VType.INT));
        k.add(strictKey("paper-global.packet-limiter.max-packet-rate", pg, "packet-limiter.all-packets.max-packet-rate", VType.NUM));
        k.add(strictKey("paper-global.packet-limiter.interval", pg, "packet-limiter.all-packets.interval", VType.NUM));
        k.add(strictKey("paper-global.watchdog.early-warning-delay", pg, "watchdog.early-warning-delay", VType.INT));
        k.add(strictKey("paper-global.watchdog.early-warning-every", pg, "watchdog.early-warning-every", VType.INT));
        k.add(strictKey("paper-global.misc.fix-entity-position-desync", pg, "misc.fix-entity-position-desync", VType.BOOL));
        k.add(strictKey("paper-global.console.enable-brigadier-highlighting", pg, "console.enable-brigadier-highlighting", VType.BOOL));
        // purpur.yml
        k.add(key("purpur.use-alternative-keepalive", "purpur.yml", "settings.use-alternate-keepalive", VType.BOOL));
        k.add(key("purpur.entities.zombie.aggressive-towards-villager-when-lagging", "purpur.yml", "world-settings.default.mobs.zombie.aggressive-towards-villager-when-lagging", VType.BOOL));
        k.add(key("purpur.entities.all.can-use-portals", "purpur.yml", "world-settings.default.gameplay-mechanics.entities-can-use-portals", VType.BOOL));
        k.add(key("purpur.entities.villager.lobotomized", "purpur.yml", "world-settings.default.mobs.villager.lobotomize.enabled", VType.BOOL));
        k.add(key("purpur.entities.villager.search-radius.acquire-poi", "purpur.yml", "world-settings.default.mobs.villager.search-radius.acquire-poi", VType.INT));
        k.add(key("purpur.entities.villager.search-radius.nearest-bed-sensor", "purpur.yml", "world-settings.default.mobs.villager.search-radius.nearest-bed-sensor", VType.INT));
        k.add(key("purpur.teleport-if-outside-worldborder", "purpur.yml", "world-settings.default.gameplay-mechanics.player.teleport-if-outside-border", VType.BOOL));
        k.add(key("purpur.lagging-tps-threshold", "purpur.yml", "settings.lagging-threshold", VType.NUM));
        k.add(strictKey("purpur.gameplay-mechanics.elytra.kinetic-damage", "purpur.yml", "world-settings.default.gameplay-mechanics.elytra.kinetic-damage", VType.BOOL));
        k.add(strictKey("purpur.mobs.phantom.spawn.per-attempt.max", "purpur.yml", "world-settings.default.mobs.phantom.spawn.per-attempt.max", VType.INT));
        // Uncertain paths stay strict so a wrong path is skipped, never invented.
        k.add(strictKey("purpur.tps-catchup", "purpur.yml", "settings.tps-catchup", VType.BOOL));
        k.add(strictKey("purpur.entities.shared-random", "purpur.yml", "world-settings.default.settings.entity.shared-random", VType.BOOL));
        // pufferfish.yml
        k.add(key("pufferfish.max-loads-per-projectile", "pufferfish.yml", "projectile.max-loads-per-projectile", VType.INT));
        k.add(key("pufferfish.entities.dynamic-activation-of-brain.enabled", "pufferfish.yml", "dab.enabled", VType.BOOL));
        k.add(key("pufferfish.entities.dynamic-activation-of-brain.max-tick-freq", "pufferfish.yml", "dab.max-tick-freq", VType.INT));
        k.add(key("pufferfish.entities.dynamic-activation-of-brain.activation-distance-modifier", "pufferfish.yml", "dab.activation-dist-mod", VType.INT));
        k.add(key("pufferfish.entities.async-mob-spawning", "pufferfish.yml", "enable-async-mob-spawning", VType.BOOL));
        k.add(key("pufferfish.entities.suffocation-optimisation", "pufferfish.yml", "enable-suffocation-optimization", VType.BOOL));
        k.add(key("pufferfish.entities.inactive-goal-selector-throttle", "pufferfish.yml", "inactive-goal-selector-throttle", VType.BOOL));
        k.add(key("pufferfish.disable-method-profiler", "pufferfish.yml", "misc.disable-method-profiler", VType.BOOL));
        k.add(strictKey("pufferfish.max-loads-per-tick", "pufferfish.yml", "projectile.max-loads-per-tick", VType.INT));
        // config/gale-global.yml - Leaf ships this too, Leaf is built on Gale
        String gale = "config/gale-global.yml";
        k.add(strictKey("gale.log-to-console.chat.empty-message-warning", gale, "log-to-console.chat.empty-message-warning", VType.BOOL));
        k.add(strictKey("gale.log-to-console.chat.expired-message-warning", gale, "log-to-console.chat.expired-message-warning", VType.BOOL));
        k.add(strictKey("gale.log-to-console.ignored-advancements", gale, "log-to-console.ignored-advancements", VType.BOOL));
        k.add(strictKey("gale.log-to-console.invalid-pool-element-error-log-level", gale, "log-to-console.invalid-pool-element-error-log-level", VType.STR));
        k.add(strictKey("gale.log-to-console.invalid-statistics", gale, "log-to-console.invalid-statistics", VType.BOOL));
        k.add(strictKey("gale.log-to-console.legacy-material-initialization", gale, "log-to-console.legacy-material-initialization", VType.BOOL));
        k.add(strictKey("gale.log-to-console.set-block-in-far-chunk", gale, "log-to-console.set-block-in-far-chunk", VType.BOOL));
        k.add(strictKey("gale.log-to-console.unrecognized-recipes", gale, "log-to-console.unrecognized-recipes", VType.BOOL));
        k.add(strictKey("gale.misc.ignore-null-legacy-structure-data", gale, "misc.ignore-null-legacy-structure-data", VType.BOOL));
        k.add(strictKey("gale.misc.keepalive.send-multiple", gale, "misc.keepalive.send-multiple", VType.BOOL));
        k.add(strictKey("gale.small-optimizations.reduced-intervals.increase-time-statistics", gale, "small-optimizations.reduced-intervals.increase-time-statistics", VType.INT));
        k.add(strictKey("gale.small-optimizations.reduced-intervals.update-entity-line-of-sight", gale, "small-optimizations.reduced-intervals.update-entity-line-of-sight", VType.INT));
        k.add(strictKey("gale.small-optimizations.optimized-tracker.enabled", gale, "small-optimizations.optimized-tracker.enabled", VType.BOOL));
        // config/leaf-global.yml - thread counts stay 0, Leaf sizes them from CPU cores
        String leaf = "config/leaf-global.yml";
        k.add(strictKey("leaf.async.async-entity-tracker.enabled", leaf, "async.async-entity-tracker.enabled", VType.BOOL));
        k.add(strictKey("leaf.async.async-entity-tracker.compat-mode", leaf, "async.async-entity-tracker.compat-mode", VType.BOOL));
        k.add(strictKey("leaf.async.async-entity-tracker.max-threads", leaf, "async.async-entity-tracker.max-threads", VType.INT));
        k.add(strictKey("leaf.async.async-entity-tracker.threads", leaf, "async.async-entity-tracker.threads", VType.INT));
        k.add(strictKey("leaf.async.async-entity-tracker.keepalive", leaf, "async.async-entity-tracker.keepalive", VType.INT));
        k.add(strictKey("leaf.async.async-pathfinding.enabled", leaf, "async.async-pathfinding.enabled", VType.BOOL));
        k.add(strictKey("leaf.async.async-pathfinding.max-threads", leaf, "async.async-pathfinding.max-threads", VType.INT));
        k.add(strictKey("leaf.async.async-pathfinding.keepalive", leaf, "async.async-pathfinding.keepalive", VType.INT));
        k.add(strictKey("leaf.async.async-pathfinding.queue-size", leaf, "async.async-pathfinding.queue-size", VType.INT));
        k.add(strictKey("leaf.async.async-pathfinding.reject-policy", leaf, "async.async-pathfinding.reject-policy", VType.STR));
        k.add(strictKey("leaf.async.async-target-finding.enabled", leaf, "async.async-target-finding.enabled", VType.BOOL));
        k.add(strictKey("leaf.async.async-mob-spawning.enabled", leaf, "async.async-mob-spawning.enabled", VType.BOOL));
        k.add(strictKey("leaf.async.async-locator.enabled", leaf, "async.async-locator.enabled", VType.BOOL));
        k.add(strictKey("leaf.async.async-locator.threads", leaf, "async.async-locator.threads", VType.INT));
        k.add(strictKey("leaf.async.async-locator.keepalive", leaf, "async.async-locator.keepalive", VType.INT));
        k.add(strictKey("leaf.async.async-chunk-send.enabled", leaf, "async.async-chunk-send.enabled", VType.BOOL));
        k.add(strictKey("leaf.async.async-chunk-send.threads", leaf, "async.async-chunk-send.threads", VType.INT));
        k.add(strictKey("leaf.async.async-chunk-send.keepalive", leaf, "async.async-chunk-send.keepalive", VType.INT));
        k.add(strictKey("leaf.performance.use-virtual-thread-for-async-chat-executor", leaf, "performance.use-virtual-thread-for-async-chat-executor", VType.BOOL));
        k.add(strictKey("leaf.performance.use-virtual-thread-for-async-scheduler", leaf, "performance.use-virtual-thread-for-async-scheduler", VType.BOOL));
        k.add(strictKey("leaf.performance.use-virtual-thread-for-user-authenticator", leaf, "performance.use-virtual-thread-for-user-authenticator", VType.BOOL));
        k.add(strictKey("leaf.performance.inactive-goal-selector-throttle", leaf, "performance.inactive-goal-selector-throttle", VType.BOOL));
        k.add(strictKey("leaf.performance.skip-ai-for-non-aware-mob", leaf, "performance.skip-ai-for-non-aware-mob", VType.BOOL));
        k.add(strictKey("leaf.performance.optimized-powered-rails", leaf, "performance.optimized-powered-rails", VType.BOOL));
        k.add(strictKey("leaf.performance.throttle-hopper-when-full.enabled", leaf, "performance.throttle-hopper-when-full.enabled", VType.BOOL));
        k.add(strictKey("leaf.performance.throttle-hopper-when-full.skip-ticks", leaf, "performance.throttle-hopper-when-full.skip-ticks", VType.INT));
        k.add(strictKey("leaf.performance.optimize-minecart.enabled", leaf, "performance.optimize-minecart.enabled", VType.BOOL));
        k.add(strictKey("leaf.performance.optimize-minecart.skip-tick-count", leaf, "performance.optimize-minecart.skip-tick-count", VType.INT));
        k.add(strictKey("leaf.performance.dab.enabled", leaf, "performance.dab.enabled", VType.BOOL));
        k.add(strictKey("leaf.performance.dab.max-tick-freq", leaf, "performance.dab.max-tick-freq", VType.INT));
        k.add(strictKey("leaf.performance.dab.start-distance", leaf, "performance.dab.start-distance", VType.INT));
        k.add(strictKey("leaf.performance.dab.activation-dist-mod", leaf, "performance.dab.activation-dist-mod", VType.INT));
        k.add(strictKey("leaf.misc.secure-seed.enabled", leaf, "misc.secure-seed.enabled", VType.BOOL));
        return k;
    }

    /** Every .kos path the tuner knows how to write. Tests use this to catch dead profile keys. */
    public static Set<String> mappedKosPaths() {
        Set<String> out = new HashSet<>();
        for (TunKey k : keys()) out.add(k.kos());
        return out;
    }

    /** .kos path to the server file it is written to. */
    public static Map<String, String> mappedKosFiles() {
        Map<String, String> out = new LinkedHashMap<>();
        for (TunKey k : keys()) out.put(k.kos(), k.file());
        return out;
    }

    /** .kos paths that are only written when that key already exists in the target file. */
    public static Set<String> strictKosPaths() {
        Set<String> out = new HashSet<>();
        for (TunKey k : keys()) if (k.strict()) out.add(k.kos());
        return out;
    }
}

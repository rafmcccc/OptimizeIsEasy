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
import java.util.Map;
import java.util.Set;

public class ServerTunerModule extends AbstractModule {

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
                    try { plugin.saveResource("profiles/" + p, false); } catch (Throwable t) { plugin.getLogger().warning("[ServerTuner] could not seed default profile " + p + ": " + t.getMessage()); }
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

        for (ServerTunerKeys.TunKey k : ServerTunerKeys.keys()) {
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
            if (!backupOnce(k.file(), backedUp, sender)) {
                failed++;
                continue;
            }
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
                if (dry) applied++;
                else if (!backupOnce(pw, backedUp, sender)) failed++;
                else if (ServerFileUtil.setYaml(pw, "environment.treasure-maps.enabled", pregenerated || overridePregen)) applied++;
                else failed++;
                if (!pregenerated && !overridePregen) sender.sendMessage("§eTreasure maps disabled (world not pregenerated). Pre-generate to re-enable.");
            } else skipped++;
        }
        if (sd != null && sd.supportsPurpur()) {
            String pf = "purpur.yml";
            if (new File(pf).isFile()) {
                if (dry) applied++;
                else if (!backupOnce(pf, backedUp, sender)) failed++;
                else if (ServerFileUtil.setYaml(pf, "world-settings.default.mobs.dolphin.disable-treasure-searching", !(pregenerated || overridePregen))) applied++;
                else failed++;
            } else skipped++;
        }
        if (sd == null || sd.supportsPaperGlobal()) {
            String pg = "config/paper-global.yml";
            if (new File(pg).isFile()) {
                if (dry) {
                    applied += 2;
                } else if (!backupOnce(pg, backedUp, sender)) {
                    failed += 2;
                } else {
                    if (ServerFileUtil.setYaml(pg, "item-validation.book-size.page-max", 1024)) applied++; else failed++;
                    if (ServerFileUtil.setYaml(pg, "misc.max-joins-per-tick", 3)) applied++; else failed++;
                }
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

    /**
     * Backs up {@code file} once per run. Returns false when backups are
     * enabled and the backup failed, in which case the caller must not write
     * that file.
     */
    boolean backupOnce(String file, Set<String> backedUp, CommandSender sender) {
        if (!isBackupBeforeApply() || backedUp.contains(file)) return true;
        boolean ok;
        try {
            ok = new BackupManager(plugin).backup(new File(file));
        } catch (Throwable t) {
            plugin.getLogger().warning("ServerTuner: backup of " + file + " failed: " + t.getMessage());
            ok = false;
        }
        if (!ok) {
            plugin.getLogger().warning("ServerTuner: backup of " + file + " failed, skipped patching it.");
            if (sender != null) sender.sendMessage("§cBackup of §f" + file + "§c failed, skipped patching it.");
            return false;
        }
        backedUp.add(file);
        return true;
    }

    private ServerFileUtil.WriteResult write(ServerTunerKeys.TunKey k, Object value) {
        if (k.type() == ServerTunerKeys.VType.PROP) {
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

    private Object coerce(Object raw, ServerTunerKeys.VType type) {
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

    /** Every .kos path the tuner knows how to write. Tests use this to catch dead profile keys. */
    public static Set<String> mappedKosPaths() {
        return ServerTunerKeys.mappedKosPaths();
    }

    /** .kos path to the server file it is written to. */
    public static Map<String, String> mappedKosFiles() {
        return ServerTunerKeys.mappedKosFiles();
    }

    /** .kos paths that are only written when that key already exists in the target file. */
    public static Set<String> strictKosPaths() {
        return ServerTunerKeys.strictKosPaths();
    }
}

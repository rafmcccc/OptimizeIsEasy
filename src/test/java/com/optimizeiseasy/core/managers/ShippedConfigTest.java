package com.optimizeiseasy.core.managers;

import com.optimizeiseasy.core.modules.ServerTunerModule;
import org.junit.jupiter.api.Test;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ShippedConfigTest {

    private static final List<String> PROFILES =
            List.of("YouHaveTrouble.kos", "FarmFriendly.kos", "Balanced.kos", "LowEnd.kos",
                    "HighEnd.kos", "VanillaPlus.kos", "LobbyGames.kos", "Anarchy.kos");

    private YamlConfiguration load(String path) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as(path + " must exist in the jar").isNotNull();
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    private YamlConfiguration profile(String name) throws Exception {
        return load("profiles/" + name);
    }

    /** Every dotted path in the document that holds a value, so sections themselves are skipped. */
    private static List<String> valuePaths(ConfigurationSection section, String prefix) {
        List<String> out = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            String child = prefix.isEmpty() ? key : prefix + "." + key;
            ConfigurationSection nested = section.getConfigurationSection(key);
            if (nested != null) out.addAll(valuePaths(nested, child));
            else out.add(child);
        }
        return out;
    }

    @Test
    void exploitDbConfigIsValid() throws Exception {
        YamlConfiguration cfg = load("modules/ExploitDB.yml");
        assertThat(ConfigValidator.validateModuleSection("ExploitDB", cfg.getConfigurationSection("ExploitDB.values"))).isEmpty();
        assertThat(cfg.getBoolean("ExploitDB.values.auto-check-on-start")).isTrue();
        assertThat(cfg.getStringList("ExploitDB.values.enabled-exploits")).containsExactly("*");
        assertThat(cfg.getStringList("ExploitDB.values.patch-denylist"))
                .contains("EDB-12", "EDB-18");
        assertThat(cfg.getBoolean("ExploitDB.values.backup-before-patch", false)).isTrue();
    }

    @Test
    void serverTunerConfigIsValid() throws Exception {
        YamlConfiguration cfg = load("modules/ServerTuner.yml");
        assertThat(ConfigValidator.validateModuleSection("ServerTuner", cfg.getConfigurationSection("ServerTuner.values"))).isEmpty();
        assertThat(cfg.getString("ServerTuner.values.default-profile")).isEqualTo("YouHaveTrouble.kos");
        assertThat(cfg.getStringList("ServerTuner.values.denied-profiles")).isEmpty();
    }

    @Test
    void validatorRejectsBadEntries() {
        YamlConfiguration bad = new YamlConfiguration();
        bad.set("ExploitDB.values.rate-limit-packets", "not a number");
        bad.set("ExploitDB.values.patch-denylist", java.util.List.of("../../etc/passwd"));
        bad.set("ExploitDB.values.packet-limits.EDB-13.action", "EXPLODE");
        assertThat(ConfigValidator.validateModuleSection("ExploitDB", bad.getConfigurationSection("ExploitDB.values")))
                .anyMatch(s -> s.contains("rate-limit-packets"))
                .anyMatch(s -> s.contains("invalid entry"))
                .anyMatch(s -> s.contains("action must be"));
    }

    @Test
    void shippedProfilesParseAndDescribeThemselves() throws Exception {
        for (String name : PROFILES) {
            YamlConfiguration cfg = profile(name);
            assertThat(cfg.getString("meta.description")).as(name + " meta.description").isNotBlank();
            assertThat(cfg.getString("meta.author")).as(name + " meta.author").isNotBlank();
            assertThat(cfg.getConfigurationSection("server")).as(name + " server section").isNotNull();
        }
    }

    @Test
    void everyProfileKeyIsMappedByTheTuner() throws Exception {
        Set<String> mapped = ServerTunerModule.mappedKosPaths();
        for (String name : PROFILES) {
            YamlConfiguration cfg = profile(name);
            for (String path : valuePaths(cfg, "")) {
                if (path.startsWith("meta.")) continue;
                assertThat(mapped).as(name + " has a key the tuner cannot write: " + path).contains(path);
            }
        }
    }

    @Test
    void everyProfileCarriesTheForkConfig() throws Exception {
        for (String name : PROFILES) {
            YamlConfiguration cfg = profile(name);
            assertThat(cfg.getBoolean("paper-global.unsupported-settings.allow-piston-duplication", true))
                    .as(name + " piston duplication").isFalse();
            assertThat(cfg.getBoolean("gale.log-to-console.unrecognized-recipes", true))
                    .as(name + " gale log noise").isFalse();
            assertThat(cfg.getBoolean("leaf.async.async-pathfinding.enabled", false))
                    .as(name + " leaf async pathfinding").isTrue();
            assertThat(cfg.getBoolean("leaf.async.async-entity-tracker.enabled", false))
                    .as(name + " leaf async entity tracker").isTrue();
            // 0 means "Leaf sizes this pool from the CPU cores", which keeps the
            // profile portable instead of guessing thread counts.
            assertThat(cfg.getInt("leaf.async.async-pathfinding.max-threads")).as(name + " pathfinding threads").isZero();
            assertThat(cfg.getInt("leaf.async.async-entity-tracker.max-threads")).as(name + " tracker threads").isZero();
            // Leaf 26.x renamed the three virtual-thread keys into this single one.
            assertThat(cfg.getBoolean("leaf.performance.use-virtual-thread", false))
                    .as(name + " leaf virtual threads").isTrue();
        }
    }

    @Test
    void forkKeysAreStrictAndTargetTheirOwnFile() {
        Map<String, String> files = ServerTunerModule.mappedKosFiles();
        Set<String> strict = ServerTunerModule.strictKosPaths();
        for (String kos : ServerTunerModule.mappedKosPaths()) {
            if (kos.startsWith("gale.")) {
                assertThat(files.get(kos)).as(kos).isEqualTo("config/gale-global.yml");
                assertThat(strict).as(kos + " must not be invented on an older fork").contains(kos);
            } else if (kos.startsWith("leaf.")) {
                assertThat(files.get(kos)).as(kos).isEqualTo("config/leaf-global.yml");
                assertThat(strict).as(kos + " must not be invented on an older fork").contains(kos);
            }
        }
    }
}
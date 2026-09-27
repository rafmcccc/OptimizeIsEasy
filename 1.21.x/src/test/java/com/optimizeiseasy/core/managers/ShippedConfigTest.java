package com.optimizeiseasy.core.managers;

import org.junit.jupiter.api.Test;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ShippedConfigTest {

    private YamlConfiguration load(String path) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as(path + " must exist in the jar").isNotNull();
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
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
}

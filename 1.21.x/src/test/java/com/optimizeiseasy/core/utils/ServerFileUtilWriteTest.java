package com.optimizeiseasy.core.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ServerFileUtilWriteTest {

    @TempDir
    Path tmp;

    @Test
    void yamlDoublesRoundTripAsTheStringTheEdbTableExpects() throws Exception {
        File f = tmp.resolve("paper-global.yml").toFile();
        String key = "packet-limiter.overrides.ServerboundCommandSuggestionPacket.interval";
        YamlConfiguration seed = new YamlConfiguration();
        seed.set(key, 0.0);
        seed.save(f);

        // The EDB table pairs expected="1.0" with fix=1.0 (a Double). If Bukkit
        // serialised or parsed that as "1", the check could never pass.
        assertThat(ServerFileUtil.setYamlStrict(f.getPath(), key, 1.0))
                .isEqualTo(ServerFileUtil.WriteResult.WROTE);
        assertThat(ServerFileUtil.getYamlString(f.getPath(), key)).isEqualTo("1.0");

        ServerFileUtil.setYamlStrict(f.getPath(), key, 15.0);
        assertThat(ServerFileUtil.getYamlString(f.getPath(), key)).isEqualTo("15.0");

        ServerFileUtil.setYamlStrict(f.getPath(), key, 0.5);
        assertThat(ServerFileUtil.getYamlString(f.getPath(), key)).isEqualTo("0.5");
    }

    @Test
    void missingKeyIsNeverCreated() throws Exception {
        File f = tmp.resolve("paper-world-defaults.yml").toFile();
        YamlConfiguration seed = new YamlConfiguration();
        seed.set("entities.armor-stands.tick", true);
        seed.save(f);

        assertThat(ServerFileUtil.setYamlStrict(f.getPath(), "anticheat.anti-xray.exempt-blocks", new java.util.ArrayList<>()))
                .isEqualTo(ServerFileUtil.WriteResult.MISSING_KEY);

        YamlConfiguration reloaded = YamlConfiguration.loadConfiguration(f);
        assertThat(reloaded.contains("anticheat.anti-xray.exempt-blocks"))
                .as("a renamed key must not be invented, or check() would report a false pass")
                .isFalse();
        assertThat(ServerFileUtil.getYamlString(f.getPath(), "anticheat.anti-xray.exempt-blocks")).isNull();
    }

    @Test
    void existingKeyIsWritten() throws Exception {
        File f = tmp.resolve("paper-world-defaults.yml").toFile();
        YamlConfiguration seed = new YamlConfiguration();
        seed.set("anticheat.anti-xray.enabled", false);
        seed.save(f);

        assertThat(ServerFileUtil.setYamlStrict(f.getPath(), "anticheat.anti-xray.enabled", true))
                .isEqualTo(ServerFileUtil.WriteResult.WROTE);
        assertThat(ServerFileUtil.getYamlString(f.getPath(), "anticheat.anti-xray.enabled")).isEqualTo("true");
    }

    @Test
    void emptyListExpectationMatchesWhatYamlReports() throws Exception {
        File f = tmp.resolve("spigot.yml").toFile();
        YamlConfiguration seed = new YamlConfiguration();
        seed.set("commands.spam-exclusions", new java.util.ArrayList<>());
        seed.save(f);
        // EDB-5 pairs expected="[]" with fix=new ArrayList<>().
        assertThat(ServerFileUtil.getYamlString(f.getPath(), "commands.spam-exclusions")).isEqualTo("[]");
    }

    @Test
    void missingFileIsReported() throws Exception {
        assertThat(ServerFileUtil.setYamlStrict(tmp.resolve("nope.yml").toString(), "a.b", 1))
                .isEqualTo(ServerFileUtil.WriteResult.NO_FILE);
        assertThat(ServerFileUtil.setPropertyStrict(tmp.resolve("nope.properties").toString(), "a", "1"))
                .isEqualTo(ServerFileUtil.WriteResult.NO_FILE);
    }

    @Test
    void propertiesStrictRefusesToInventKeys() throws Exception {
        File f = tmp.resolve("server.properties").toFile();
        try (var w = new java.io.PrintWriter(f)) {
            w.println("online-mode=false");
        }
        assertThat(ServerFileUtil.setPropertyStrict(f.getPath(), "prevent-proxy-connections", "true"))
                .isEqualTo(ServerFileUtil.WriteResult.MISSING_KEY);
        assertThat(ServerFileUtil.getProperty(f.getPath(), "prevent-proxy-connections")).isNull();

        assertThat(ServerFileUtil.setPropertyStrict(f.getPath(), "online-mode", "true"))
                .isEqualTo(ServerFileUtil.WriteResult.WROTE);
        assertThat(ServerFileUtil.getProperty(f.getPath(), "online-mode")).isEqualTo("true");
    }
}

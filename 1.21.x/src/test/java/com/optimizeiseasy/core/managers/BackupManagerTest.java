package com.optimizeiseasy.core.managers;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackupManagerTest {

    @TempDir
    Path tmp;

    private Plugin pluginWithDataFolder(File dataFolder) {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("BackupManagerTest"));
        return plugin;
    }

    @Test
    void missingFileReturnsTrueAndBacksUpNothing() {
        Plugin plugin = pluginWithDataFolder(tmp.toFile());
        assertThat(new BackupManager(plugin).backup(new File(tmp.toFile(), "nope.yml"))).isTrue();
        assertThat(new File(tmp.toFile(), "backups")).doesNotExist();
    }

    @Test
    void existingFileIsCopied() throws Exception {
        File data = tmp.resolve("data").toFile();
        File source = tmp.resolve("spigot.yml").toFile();
        Files.writeString(source.toPath(), "foo: bar\n");
        assertThat(new BackupManager(pluginWithDataFolder(data)).backup(source)).isTrue();
        File[] kept = new File(data, "backups").listFiles((d, n) -> n.startsWith("spigot.yml"));
        assertThat(kept).hasSize(1);
        assertThat(Files.readString(kept[0].toPath())).isEqualTo("foo: bar\n");
    }

    @Test
    void failureReturnsFalse() throws Exception {
        // A regular file where the backups directory should be makes every
        // backup fail, which must surface as false (fail closed).
        File blocker = tmp.resolve("blocker").toFile();
        Files.writeString(blocker.toPath(), "in the way\n");
        File source = tmp.resolve("spigot.yml").toFile();
        Files.writeString(source.toPath(), "foo: bar\n");
        assertThat(new BackupManager(pluginWithDataFolder(blocker)).backup(source)).isFalse();
    }
}

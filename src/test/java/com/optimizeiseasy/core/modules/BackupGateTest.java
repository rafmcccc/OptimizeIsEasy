package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.utils.SoftwareDetector;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackupGateTest {

    @TempDir
    Path tmp;

    private OptimizeIsEasyPlugin pluginWithDataFolder(File dataFolder) {
        OptimizeIsEasyPlugin plugin = mock(OptimizeIsEasyPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("BackupGateTest"));
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        return plugin;
    }

    private File blockerDataFolder() throws Exception {
        File blocker = tmp.resolve("blocker").toFile();
        Files.writeString(blocker.toPath(), "in the way\n");
        return blocker;
    }

    @Test
    void tunerBackupFailureReturnsFalseAndNotifies() throws Exception {
        // Break backups: a regular file where the data folder should be, plus
        // a real target file so there is actually something to back up.
        File target = tmp.resolve("target.yml").toFile();
        Files.writeString(target.toPath(), "a: 1\n");
        CommandSender sender = mock(CommandSender.class);
        ServerTunerModule module = new ServerTunerModule(pluginWithDataFolder(blockerDataFolder()));
        assertThat(module.backupOnce(target.getAbsolutePath(), new HashSet<>(), sender)).isFalse();
        ArgumentCaptor<String> msgs = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).sendMessage(msgs.capture());
        assertThat(msgs.getAllValues()).anyMatch(m -> m.contains("Backup of") && m.contains("target.yml"));
    }

    @Test
    void tunerBackupDisabledReturnsTrueWithoutTouchingDisk() throws Exception {
        File data = tmp.resolve("data").toFile();
        File modFile = new File(new File(data, "modules"), "ServerTuner.yml");
        modFile.getParentFile().mkdirs();
        Files.writeString(modFile.toPath(), "ServerTuner:\n  values:\n    backup-before-apply: false\n");
        OptimizeIsEasyPlugin plugin = pluginWithDataFolder(data);
        ServerTunerModule module = new ServerTunerModule(plugin);
        module.loadConfigSection();
        CommandSender sender = mock(CommandSender.class);
        assertThat(module.backupOnce("spigot.yml", new HashSet<>(), sender)).isTrue();
        assertThat(new File(data, "backups")).doesNotExist();
    }

    @Test
    void tunerBackupSuccessBacksUpRealFile() throws Exception {
        File data = tmp.resolve("data").toFile();
        File target = tmp.resolve("target.yml").toFile();
        Files.writeString(target.toPath(), "a: 1\n");
        ServerTunerModule module = new ServerTunerModule(pluginWithDataFolder(data));
        assertThat(module.backupOnce(target.getAbsolutePath(), new HashSet<>(), mock(CommandSender.class))).isTrue();
        File[] kept = new File(data, "backups").listFiles((d, n) -> n.startsWith("target.yml"));
        assertThat(kept).hasSize(1);
    }

    @Test
    void edbBackupFailureReturnsFalseAndNotifies() throws Exception {
        File target = tmp.resolve("server.properties").toFile();
        Files.writeString(target.toPath(), "online-mode=false\n");
        CommandSender sender = mock(CommandSender.class);
        ExploitDBModule module = new ExploitDBModule(pluginWithDataFolder(blockerDataFolder()));
        assertThat(module.backupOnce(target.getAbsolutePath(), new HashSet<>(), sender)).isFalse();
        ArgumentCaptor<String> msgs = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).sendMessage(msgs.capture());
        assertThat(msgs.getAllValues()).anyMatch(m -> m.contains("Backup of") && m.contains("server.properties"));
    }

    /**
     * End-to-end through {@link ExploitDBModule#patch}: EDB targets resolve
     * against the working directory, so the target file is created there and
     * removed in a finally block.
     */
    @Test
    void edbPatchSkipsWriteWhenBackupFails() throws Exception {
        File cfgDir = new File("config");
        assertThat(cfgDir.mkdirs() || cfgDir.isDirectory()).isTrue();
        File target = new File(cfgDir, "paper-global.yml");
        String original = "packet-limiter:\n  overrides:\n    ServerboundCommandSuggestionPacket:\n"
                + "      action: FILTER\n      interval: 9.0\n      max-packet-rate: 99.0\n";
        Files.writeString(target.toPath(), original);
        try {
            OptimizeIsEasyPlugin plugin = pluginWithDataFolder(blockerDataFolder());
            SoftwareDetector sd = mock(SoftwareDetector.class);
            when(sd.supportsPaperGlobal()).thenReturn(true);
            when(plugin.getSoftwareDetector()).thenReturn(sd);
            ExploitDBModule module = new ExploitDBModule(plugin);
            CommandSender sender = mock(CommandSender.class);
            assertThat(module.patch("EDB-4", sender)).isFalse();
            assertThat(Files.readString(target.toPath()))
                    .as("failed backup must leave the target byte-identical")
                    .isEqualTo(original);
            ArgumentCaptor<String> msgs = ArgumentCaptor.forClass(String.class);
            verify(sender, atLeastOnce()).sendMessage(msgs.capture());
            assertThat(msgs.getAllValues()).anyMatch(m -> m.contains("Backup of") && m.contains("config/paper-global.yml"));
        } finally {
            Files.deleteIfExists(target.toPath());
            cfgDir.delete();
        }
    }

    @Test
    void edbPatchProceedsWhenBackupSucceeds() throws Exception {
        File cfgDir = new File("config");
        assertThat(cfgDir.mkdirs() || cfgDir.isDirectory()).isTrue();
        File target = new File(cfgDir, "paper-global.yml");
        Files.writeString(target.toPath(), "packet-limiter:\n  overrides:\n    ServerboundCommandSuggestionPacket:\n"
                + "      action: FILTER\n      interval: 9.0\n      max-packet-rate: 99.0\n");
        try {
            File data = tmp.resolve("data").toFile();
            OptimizeIsEasyPlugin plugin = pluginWithDataFolder(data);
            SoftwareDetector sd = mock(SoftwareDetector.class);
            when(sd.supportsPaperGlobal()).thenReturn(true);
            when(plugin.getSoftwareDetector()).thenReturn(sd);
            ExploitDBModule module = new ExploitDBModule(plugin);
            CommandSender sender = mock(CommandSender.class);
            assertThat(module.patch("EDB-4", sender)).isTrue();
            String after = Files.readString(target.toPath());
            assertThat(after).contains("action: DROP").contains("interval: 1.0").contains("max-packet-rate: 15.0");
            File[] kept = new File(data, "backups").listFiles((d, n) -> n.startsWith("paper-global.yml"));
            assertThat(kept).hasSize(1);
            ArgumentCaptor<String> msgs = ArgumentCaptor.forClass(String.class);
            verify(sender, atLeastOnce()).sendMessage(msgs.capture());
            assertThat(msgs.getAllValues()).noneMatch(m -> m.contains("Backup of"));
        } finally {
            Files.deleteIfExists(target.toPath());
            cfgDir.delete();
        }
    }
}

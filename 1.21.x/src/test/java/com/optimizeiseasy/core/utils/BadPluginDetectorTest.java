package com.optimizeiseasy.core.utils;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BadPluginDetectorTest {

    private OptimizeIsEasyPlugin pluginWith(String... names) {
        OptimizeIsEasyPlugin plugin = mock(OptimizeIsEasyPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("BadPluginDetectorTest"));
        Server server = mock(Server.class);
        PluginManager manager = mock(PluginManager.class);
        Plugin[] installed = new Plugin[names.length];
        for (int i = 0; i < names.length; i++) {
            Plugin p = mock(Plugin.class);
            when(p.getName()).thenReturn(names[i]);
            installed[i] = p;
        }
        when(manager.getPlugins()).thenReturn(installed);
        when(server.getPluginManager()).thenReturn(manager);
        when(plugin.getServer()).thenReturn(server);
        return plugin;
    }

    @Test
    void everyKnownConflictHasReasonAndReplacement() {
        assertThat(BadPluginDetector.knownConflicts()).isNotEmpty();
        for (BadPluginDetector.Conflict conflict : BadPluginDetector.knownConflicts()) {
            assertThat(conflict.name()).as("name").isNotBlank();
            assertThat(conflict.why()).as(conflict.name() + " reason").isNotBlank();
            assertThat(conflict.instead()).as(conflict.name() + " replacement").isNotBlank();
        }
    }

    @Test
    void matchingIsExactNameCaseInsensitive() {
        BadPluginDetector detector = new BadPluginDetector(pluginWith("ClearLag", "spark", "ClearLagger"));
        List<BadPluginDetector.Conflict> found = detector.findConflicts();
        assertThat(found).hasSize(1);
        assertThat(found.get(0).name()).isEqualTo("ClearLag");
        assertThat(found.get(0).why()).isNotBlank();
        assertThat(found.get(0).instead()).isNotBlank();

        BadPluginDetector lower = new BadPluginDetector(pluginWith("clearlag"));
        assertThat(lower.findBadPlugins()).containsExactly("clearlag");

        BadPluginDetector clean = new BadPluginDetector(pluginWith("spark", "Chunky"));
        assertThat(clean.findConflicts()).isEmpty();
        assertThat(clean.findBadPlugins()).isEmpty();
    }

    @Test
    void stackersAndReloadersAreCovered() {
        BadPluginDetector detector = new BadPluginDetector(
                pluginWith("RoseStacker", "UltimateStacker", "PlugManX", "WildStacker"));
        assertThat(detector.findBadPlugins())
                .containsExactlyInAnyOrder("RoseStacker", "UltimateStacker", "PlugManX", "WildStacker");
    }

    @Test
    void warnIfBadDoesNotThrow() {
        new BadPluginDetector(pluginWith("ClearLag")).warnIfBad();
        new BadPluginDetector(pluginWith()).warnIfBad();
    }
}

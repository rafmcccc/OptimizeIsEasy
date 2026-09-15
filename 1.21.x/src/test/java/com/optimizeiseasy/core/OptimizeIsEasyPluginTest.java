package com.optimizeiseasy.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

public class OptimizeIsEasyPluginTest {
    @BeforeEach
    void setUp() {
        // MockBukkit would be used here if available
        // For now, basic sanity test that main class exists and has correct methods
    }

    @Test
    void pluginMainExists() throws Exception {
        Class<?> clazz = Class.forName("com.optimizeiseasy.core.OptimizeIsEasyPlugin");
        assertThat(clazz).isNotNull();
        assertThat(clazz.getSuperclass().getName()).contains("JavaPlugin");
    }

    @Test
    void commandsExist() throws Exception {
        // Verify command handlers exist
        assertThat(Class.forName("com.optimizeiseasy.core.commands.OptimizeCommand")).isNotNull();
        assertThat(Class.forName("com.optimizeiseasy.core.commands.ExploitFixCommand")).isNotNull();
    }

    @Test
    void modulesExist() throws Exception {
        assertThat(Class.forName("com.optimizeiseasy.core.modules.HibernateModule")).isNotNull();
        assertThat(Class.forName("com.optimizeiseasy.core.modules.WorldCleanerModule")).isNotNull();
        assertThat(Class.forName("com.optimizeiseasy.core.support.SupportManager")).isNotNull();
    }

    @AfterEach
    void tearDown() {}
}

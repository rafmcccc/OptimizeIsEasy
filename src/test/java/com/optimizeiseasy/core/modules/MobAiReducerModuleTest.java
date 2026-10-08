package com.optimizeiseasy.core.modules;

import com.destroystokyo.paper.entity.ai.GoalType;
import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.modules.ai.OptimizedBreedGoal;
import com.optimizeiseasy.core.modules.ai.OptimizedTemptGoal;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MobAiReducerModuleTest {

    private MobAiReducerModule loadModule(OptimizeIsEasyPlugin plugin) throws Exception {
        MobAiReducerModule module = new MobAiReducerModule(plugin);
        assertThat(module.loadConfig()).isTrue();
        return module;
    }

    private OptimizeIsEasyPlugin mockPlugin(@TempDir Path tempDir) throws Exception {
        Path modulesDir = tempDir.resolve("modules");
        Files.createDirectories(modulesDir);
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("modules/MobAiReducer.yml")) {
            assertThat(in).as("shipped MobAiReducer.yml must be on the classpath").isNotNull();
            Files.copy(in, modulesDir.resolve("MobAiReducer.yml"), StandardCopyOption.REPLACE_EXISTING);
        }
        OptimizeIsEasyPlugin plugin = mock(OptimizeIsEasyPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("MobAiReducerTest"));
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getResource("modules/MobAiReducer.yml")).thenAnswer(inv ->
                getClass().getClassLoader().getResourceAsStream(inv.getArgument(0)));
        return plugin;
    }

    @Test
    void defaultConfigIsFullyConsumed(@TempDir Path tempDir) throws Exception {
        MobAiReducerModule module = loadModule(mockPlugin(tempDir));

        assertThat(module.isAnimals()).isTrue();
        assertThat(module.isMonsters()).isTrue();
        assertThat(module.isVillagers()).isFalse();
        assertThat(module.isTameable()).isFalse();
        assertThat(module.isBirds()).isFalse();
        assertThat(module.isOthers()).isTrue();
        assertThat(module.isListMode()).isFalse();
        assertThat(module.getList()).contains(EntityType.WOLF, EntityType.ENDER_DRAGON, EntityType.WARDEN);

        assertThat(module.isCollides()).isTrue();
        assertThat(module.isSilent()).isFalse();
        assertThat(module.isAsync()).isTrue();
        assertThat(module.isForceLoad()).isTrue();
        assertThat(module.isClickEvent()).isTrue();
        assertThat(module.getPurgeInterval()).isEqualTo(30);
        assertThat(module.isIgnoreModels()).isTrue();

        assertThat(module.isKeepDedicated()).isTrue();
        assertThat(module.isAiListMode()).isFalse();
        assertThat(module.getAiList()).contains("attack", "panic", "swell");

        assertThat(module.getReasons()).contains(
                CreatureSpawnEvent.SpawnReason.NATURAL,
                CreatureSpawnEvent.SpawnReason.BREEDING,
                CreatureSpawnEvent.SpawnReason.SPAWNER);

        assertThat(module.isTemptEnabled()).isTrue();
        assertThat(module.getTemptRange()).isEqualTo(6.25d);
        assertThat(module.getTemptSpeed()).isEqualTo(1.25d);
        assertThat(module.getTemptCooldown()).isEqualTo(30);
        assertThat(module.isTemptBothHands()).isTrue();

        assertThat(module.isBreedEnabled()).isTrue();
        assertThat(module.getBreedRange()).isEqualTo(5.0d);
        assertThat(module.getBreedSpeed()).isEqualTo(1.0d);
    }

    @Test
    void categoryGatingMatchesDefaults(@TempDir Path tempDir) throws Exception {
        MobAiReducerModule module = loadModule(mockPlugin(tempDir));

        Cow cow = mock(Cow.class);
        when(cow.getType()).thenReturn(EntityType.COW);
        assertThat(module.isEnabled(cow)).as("farm animal").isTrue();

        Zombie zombie = mock(Zombie.class);
        when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        assertThat(module.isEnabled(zombie)).as("monster").isTrue();

        IronGolem golem = mock(IronGolem.class);
        when(golem.getType()).thenReturn(EntityType.IRON_GOLEM);
        assertThat(module.isEnabled(golem)).as("listed golem is excluded by list").isFalse();

        Villager villager = mock(Villager.class);
        when(villager.getType()).thenReturn(EntityType.VILLAGER);
        assertThat(module.isEnabled(villager)).as("villagers off by default").isFalse();

        Wolf wolf = mock(Wolf.class);
        when(wolf.getType()).thenReturn(EntityType.WOLF);
        assertThat(module.isEnabled(wolf)).as("listed wolf is excluded by list").isFalse();

        Phantom phantom = mock(Phantom.class);
        when(phantom.getType()).thenReturn(EntityType.PHANTOM);
        assertThat(module.isEnabled(phantom)).as("flying category off by default").isFalse();

        EnderDragon dragon = mock(EnderDragon.class);
        when(dragon.getType()).thenReturn(EntityType.ENDER_DRAGON);
        assertThat(module.isEnabled(dragon)).as("listed dragon is excluded by list").isFalse();

        assertThat(module.isEnabled(null)).isFalse();
    }

    @Test
    void phraseMatchingFollowsListMode() {
        Set<String> phrases = new HashSet<>(Set.of("attack", "panic", "swell"));
        // list_mode=false: everything NOT listed is removed.
        assertThat(MobAiReducerModule.matchesAiList("minecraft:random_stroll PaperRandomStroll".toLowerCase(), phrases, false)).isTrue();
        assertThat(MobAiReducerModule.matchesAiList("minecraft:melee_attack PaperMeleeAttack".toLowerCase(), phrases, false)).isFalse();
        // list_mode=true: only listed goals are removed.
        assertThat(MobAiReducerModule.matchesAiList("minecraft:melee_attack PaperMeleeAttack".toLowerCase(), phrases, true)).isTrue();
        assertThat(MobAiReducerModule.matchesAiList("minecraft:random_stroll PaperRandomStroll".toLowerCase(), phrases, true)).isFalse();
        // Empty phrases never match.
        assertThat(MobAiReducerModule.matchesAiList("minecraft:panic PaperPanic".toLowerCase(), Set.of(), false)).isTrue();
        assertThat(MobAiReducerModule.matchesAiList("minecraft:panic PaperPanic".toLowerCase(), Set.of(), true)).isFalse();
    }

    @Test
    void breedGoalSupportsFarmAnimalsButNotEggLayers() {
        assertThat(OptimizedBreedGoal.supports(EntityType.COW)).isTrue();
        assertThat(OptimizedBreedGoal.supports(EntityType.SHEEP)).isTrue();
        assertThat(OptimizedBreedGoal.supports(EntityType.TURTLE)).isFalse();
        assertThat(OptimizedBreedGoal.supports(EntityType.FROG)).isFalse();
        assertThat(OptimizedBreedGoal.supports(EntityType.SNIFFER)).isFalse();
    }

    @Test
    void customGoalsExposeStableKeys() {
        Cow cow = mock(Cow.class);
        OptimizedTemptGoal tempt = new OptimizedTemptGoal(cow, 6.25d, 1.25d, 30, true, false, false);
        assertThat(tempt.getKey().getNamespacedKey().getNamespace()).isEqualTo("optimizeiseasy");
        assertThat(tempt.getTypes()).contains(GoalType.MOVE);

        OptimizedBreedGoal breed = new OptimizedBreedGoal(cow, 5.0d, 1.0d, false, false, baby -> {
        });
        assertThat(breed.getKey().getNamespacedKey().getNamespace()).isEqualTo("optimizeiseasy");
        assertThat(breed.getTypes()).contains(GoalType.MOVE);
        assertThat(breed.getKey()).isNotEqualTo(tempt.getKey());
    }
}

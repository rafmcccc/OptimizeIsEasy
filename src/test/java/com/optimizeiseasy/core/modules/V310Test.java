package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.utils.UpdateChecker;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

class V310Test {

    @Test
    void hopperKeyRoundtripsNegativeCoords() throws Exception {
        Class<?> keyClass = Class.forName("com.optimizeiseasy.core.modules.HopperOptimizerModule$HopperKey");
        var pack = keyClass.getDeclaredMethod("pack", int.class, int.class, int.class);
        pack.setAccessible(true);
        Constructor<?> ctor = keyClass.getDeclaredConstructor(java.util.UUID.class, long.class);
        ctor.setAccessible(true);
        int[][] coords = {{0, 64, 0}, {-1, -64, -1}, {-30000000, -2032, 30000000}, {123, 320, -456}, {Integer.MAX_VALUE >> 6, 2047, Integer.MIN_VALUE >> 6}};
        for (int[] c : coords) {
            long packed = (long) pack.invoke(null, c[0], c[1], c[2]);
            Object key = ctor.newInstance(java.util.UUID.randomUUID(), packed);
            var x = keyClass.getDeclaredMethod("x"); x.setAccessible(true);
            var y = keyClass.getDeclaredMethod("y"); y.setAccessible(true);
            var z = keyClass.getDeclaredMethod("z"); z.setAccessible(true);
            assertThat((int) x.invoke(key)).as("x %d", c[0]).isEqualTo(c[0]);
            assertThat((int) y.invoke(key)).as("y %d", c[1]).isEqualTo(c[1]);
            assertThat((int) z.invoke(key)).as("z %d", c[2]).isEqualTo(c[2]);
        }
    }

    @Test
    void hopperKeyPackIsStable() throws Exception {
        Class<?> keyClass = Class.forName("com.optimizeiseasy.core.modules.HopperOptimizerModule$HopperKey");
        var pack = keyClass.getDeclaredMethod("pack", int.class, int.class, int.class);
        pack.setAccessible(true);
        assertThat((long) pack.invoke(null, 1, 2, 3)).isEqualTo((long) pack.invoke(null, 1, 2, 3));
        assertThat((long) pack.invoke(null, 1, 2, 3)).isNotEqualTo((long) pack.invoke(null, 1, 2, 4));
    }

    @Test
    void isNewerComparesNumerically() {
        assertThat(UpdateChecker.isNewer("3.1.0", "3.0.0")).isTrue();
        assertThat(UpdateChecker.isNewer("3.0.0", "3.1.0")).isFalse();
        assertThat(UpdateChecker.isNewer("3.0.0", "3.0.0")).isFalse();
        assertThat(UpdateChecker.isNewer("3.10.0", "3.9.0")).isTrue();
    }

    @Test
    void leadingVStrippedOnce() {
        assertThat("v1.2.3avel".replaceFirst("^v", "")).isEqualTo("1.2.3avel");
        assertThat("vv1.0".replaceFirst("^v", "")).isEqualTo("v1.0");
    }
}

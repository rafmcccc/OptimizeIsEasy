package com.optimizeiseasy.core.modules;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Equal-to-limit froze legal setups twice: every cap below pins its edge here.
class BoundaryTest {

    @Test
    void entityLimiterDeniesAtLimit() {
        assertThat(EntityLimiterModule.isDeniedAt(20, 20)).as("at limit denies").isTrue();
        assertThat(EntityLimiterModule.isDeniedAt(19, 20)).as("under limit allows").isFalse();
        assertThat(EntityLimiterModule.isDeniedAt(21, 20)).as("over limit denies").isTrue();
    }

    @Test
    void hopperPlaceDeniesMoveAllowsAtLimit() {
        assertThat(HopperOptimizerModule.isFullForPlace(24, 24)).as("place at limit denies").isTrue();
        assertThat(HopperOptimizerModule.isFullForPlace(23, 24)).as("place under allows").isFalse();
        assertThat(HopperOptimizerModule.isOverForMove(24, 24)).as("move at limit still works").isFalse();
        assertThat(HopperOptimizerModule.isOverForMove(25, 24)).as("move over cancels").isTrue();
    }

    @Test
    void redstoneKeepsAtLimitLegal() {
        assertThat(RedstoneLimiterModule.isRedstoneOver(1100, 1100)).as("ticks at limit legal").isFalse();
        assertThat(RedstoneLimiterModule.isRedstoneOver(1101, 1100)).as("ticks over blocked").isTrue();
        assertThat(RedstoneLimiterModule.isPistonOver(50, 50)).as("piston at limit legal").isFalse();
        assertThat(RedstoneLimiterModule.isPistonOver(51, 50)).as("piston over blocked").isTrue();
    }

    @Test
    void explosionCapsOnlyOverLimit() {
        assertThat(ExplosionOptimizerModule.cappedYield(4.0f, 4.0)).as("yield at limit untouched").isEqualTo(4.0f);
        assertThat(ExplosionOptimizerModule.cappedYield(3.0f, 4.0)).as("yield under untouched").isEqualTo(3.0f);
        assertThat(ExplosionOptimizerModule.cappedYield(6.0f, 4.0)).as("yield over capped").isEqualTo(4.0f);
    }

    @Test
    void worldCleanerKeepsAtAgeItems() {
        assertThat(WorldCleanerModule.isOldEnough(200, 200)).as("at age kept").isFalse();
        assertThat(WorldCleanerModule.isOldEnough(201, 200)).as("over age cleared").isTrue();
        assertThat(WorldCleanerModule.isPickupEligible(199)).as("delay 199 eligible").isTrue();
        assertThat(WorldCleanerModule.isPickupEligible(200)).as("delay 200 held").isFalse();
    }
}

package com.zacatac.clashboard.data.model

import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildingUpgradeBoostTest {

    // Export instant at 0s; the upgrade finishes at 3600s
    private val start = Instant.fromEpochSeconds(0)
    private val end = Instant.fromEpochSeconds(3600)
    private val upgrade = BuildingUpgrade(
        name = "Cannon",
        targetLevel = 8,
        endTime = end,
        category = UpgradeCategory.BUILDER_VILLAGE,
        startTime = start
    )

    private fun boost(type: BoostType, fromSec: Long, toSec: Long) = ActiveBoost(
        type = type.id,
        startTime = Instant.fromEpochSeconds(fromSec),
        endTime = Instant.fromEpochSeconds(toSec)
    )

    @Test
    fun withoutBoostsRemainingIsEndMinusNow() {
        assertEquals(3000.0, upgrade.remainingSeconds(emptyList(), Instant.fromEpochSeconds(600)), 0.001)
    }

    @Test
    fun boostBeforeExportIsNotCountedTwice() {
        // Started before the export instant: already reflected in the export timer
        val before = boost(BoostType.BUILDER_BITE, -500, 400)
        assertEquals(3000.0, upgrade.remainingSeconds(listOf(before), Instant.fromEpochSeconds(600)), 0.001)
    }

    @Test
    fun boostAfterExportReducesRemainingByItsBoostedTime() {
        // Builder Bite is 2x: 300s of boost gives 300s of extra work
        val after = boost(BoostType.BUILDER_BITE, 100, 400)
        assertEquals(2700.0, upgrade.remainingSeconds(listOf(after), Instant.fromEpochSeconds(600)), 0.001)
    }

    @Test
    fun boostForAnotherCategoryIsIgnored() {
        val lab = boost(BoostType.RESEARCH_POTION, 100, 400)
        assertEquals(3000.0, upgrade.remainingSeconds(listOf(lab), Instant.fromEpochSeconds(600)), 0.001)
    }

    @Test
    fun timeRemainingTextIncludesSeconds() {
        assertEquals("50m 0s", formatDurationWithSeconds(3000.0))
        assertEquals("1h 0m 0s", formatDurationWithSeconds(3600.0))
    }
}

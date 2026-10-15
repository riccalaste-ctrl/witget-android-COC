package com.zacatac.clashboard.widget.main

import com.zacatac.clashboard.data.model.BuildingUpgrade
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.data.model.UpgradeCategory
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainWidgetSummaryTest {

    private val now = Instant.fromEpochSeconds(1_000_000)

    private fun upgrade(
        category: UpgradeCategory,
        endInSeconds: Long,
        name: String = "Item",
        sourceTimer: Int? = null,
        totalDuration: Double = 0.0
    ) = BuildingUpgrade(
        name = name,
        targetLevel = 5,
        endTime = now.plusSeconds(endInSeconds),
        category = category,
        startTime = now,
        totalDuration = totalDuration,
        sourceExportTimer = sourceTimer
    )

    private fun Instant.plusSeconds(s: Long) = Instant.fromEpochSeconds(epochSeconds + s)

    private fun profile(builders: Int, vararg upgrades: BuildingUpgrade) =
        PlayerAccount(builderCount = builders, activeUpgrades = upgrades.toList())

    @Test
    fun emptyVillageHasAllBuildersFree() {
        val summary = buildMainWidgetSummary(profile(5), now)
        assertEquals(0, summary.villageBusy)
        assertEquals(5, summary.villageFree)
        assertTrue(summary.rows.isEmpty())
    }

    @Test
    fun countsOnlyVillageUpgradesAsBusyBuilders() {
        val summary = buildMainWidgetSummary(
            profile(
                5,
                upgrade(UpgradeCategory.BUILDER_VILLAGE, 100),
                upgrade(UpgradeCategory.BUILDER_VILLAGE, 200),
                upgrade(UpgradeCategory.LAB, 300),
                upgrade(UpgradeCategory.PETS, 400)
            ),
            now
        )
        assertEquals(2, summary.villageBusy)
        assertEquals(3, summary.villageFree)
        assertTrue(summary.labBusy)
        assertTrue(summary.petBusy)
        assertEquals(4, summary.rows.size)
    }

    @Test
    fun busyBuildersAreCappedAtTheTotal() {
        val upgrades = (1..7).map { upgrade(UpgradeCategory.BUILDER_VILLAGE, it * 100L) }
        val summary = buildMainWidgetSummary(profile(5, *upgrades.toTypedArray()), now)
        assertEquals(5, summary.villageBusy)
        assertEquals(0, summary.villageFree)
    }

    @Test
    fun finishedUpgradesAreExcludedAndRowsAreSortedByEnd() {
        val summary = buildMainWidgetSummary(
            profile(
                5,
                upgrade(UpgradeCategory.LAB, 300, name = "late"),
                upgrade(UpgradeCategory.LAB, -10, name = "done"),
                upgrade(UpgradeCategory.LAB, 100, name = "early")
            ),
            now
        )
        assertEquals(listOf("early", "late"), summary.rows.map { it.name })
    }

    @Test
    fun progressBarOnlyWhenBaseDurationIsKnown() {
        // Base duration known: total (3600) is larger than the remaining export timer (600)
        assertTrue(hasReliableProgress(upgrade(UpgradeCategory.BUILDER_VILLAGE, 600, sourceTimer = 600, totalDuration = 3600.0)))
        // No base duration: total equals the import timer, so the bar would always start at 0%
        assertFalse(hasReliableProgress(upgrade(UpgradeCategory.LAB, 600, sourceTimer = 600, totalDuration = 600.0)))
    }
}

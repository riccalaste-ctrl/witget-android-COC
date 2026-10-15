package com.zacatac.clashboard.widget.closest

import com.zacatac.clashboard.data.model.BuildingUpgrade
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.data.model.UpgradeCategory
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClosestWidgetStateTest {

    private val now = Instant.fromEpochSeconds(1_000_000)

    private fun upgrade(category: UpgradeCategory, endInSeconds: Long, name: String = "Item") = BuildingUpgrade(
        name = name,
        targetLevel = 5,
        endTime = Instant.fromEpochSeconds(now.epochSeconds + endInSeconds),
        category = category,
        startTime = now
    )

    @Test
    fun villageWithFreeBuilderShowsFreeNow() {
        val profile = PlayerAccount(builderCount = 5, activeUpgrades = listOf(upgrade(UpgradeCategory.BUILDER_VILLAGE, 600)))
        val state = closestWidgetState(profile, UpgradeCategory.BUILDER_VILLAGE, now) as ClosestState.BuilderSlots
        assertEquals(1, state.busy)
        assertEquals(5, state.total)
        assertEquals(0.0, state.nextFreeInSeconds, 0.0)
    }

    @Test
    fun villageAllBusyShowsEarliestFreeTime() {
        val profile = PlayerAccount(
            builderCount = 2,
            activeUpgrades = listOf(
                upgrade(UpgradeCategory.BUILDER_VILLAGE, 900),
                upgrade(UpgradeCategory.BUILDER_VILLAGE, 300)
            )
        )
        val state = closestWidgetState(profile, UpgradeCategory.BUILDER_VILLAGE, now) as ClosestState.BuilderSlots
        assertEquals(2, state.busy)
        assertEquals(300.0, state.nextFreeInSeconds, 0.5)
    }

    @Test
    fun otherCategoryShowsNextCompletion() {
        val profile = PlayerAccount(
            activeUpgrades = listOf(
                upgrade(UpgradeCategory.LAB, 900, name = "late"),
                upgrade(UpgradeCategory.LAB, 120, name = "next")
            )
        )
        val state = closestWidgetState(profile, UpgradeCategory.LAB, now) as ClosestState.NextUpgrade
        assertEquals("next", state.upgrade.name)
        assertEquals(120.0, state.remainingSeconds, 0.5)
    }

    @Test
    fun emptyCategoryIsEmptyState() {
        val profile = PlayerAccount(activeUpgrades = listOf(upgrade(UpgradeCategory.PETS, 100)))
        assertTrue(closestWidgetState(profile, UpgradeCategory.LAB, now) is ClosestState.Empty)
        assertNull((closestWidgetState(profile, UpgradeCategory.LAB, now) as? ClosestState.NextUpgrade))
    }
}

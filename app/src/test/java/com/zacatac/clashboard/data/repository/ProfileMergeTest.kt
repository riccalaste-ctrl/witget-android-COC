package com.zacatac.clashboard.data.repository

import com.zacatac.clashboard.data.model.ActiveBoost
import com.zacatac.clashboard.data.model.BuildingUpgrade
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.data.model.UpgradeCategory
import com.zacatac.clashboard.data.parser.CoCExportParser
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileMergeTest {

    private val now = Instant.fromEpochSeconds(1_000_000)

    private fun parsed(tag: String, vararg upgrades: BuildingUpgrade, apprentice: Int? = null) = CoCExportParser.ParseResult(
        upgrades = upgrades.toList(),
        activeBoosts = emptyList(),
        playerTag = tag,
        timestamp = now,
        helperCooldowns = emptyMap(),
        helperLevels = apprentice?.let { mapOf(ProfileMerge.HELPER_BUILDER_APPRENTICE to it) } ?: emptyMap()
    )

    private fun upgrade(name: String) = BuildingUpgrade(
        name = name,
        targetLevel = 3,
        endTime = now.plusSeconds(600),
        category = UpgradeCategory.LAB,
        startTime = now
    )

    private fun Instant.plusSeconds(s: Long) = Instant.fromEpochSeconds(epochSeconds + s)

    @Test
    fun findExistingMatchesByTagAndIgnoresBlankTags() {
        val stored = PlayerAccount(tag = "ABC123")
        assertEquals(stored, ProfileMerge.findExisting(listOf(stored), "ABC123"))
        assertNull(ProfileMerge.findExisting(listOf(stored), "ZZZ"))
        assertNull(ProfileMerge.findExisting(listOf(PlayerAccount(tag = "")), ""))
    }

    @Test
    fun newVillageGetsUpgradesAndHelperLevels() {
        val result = ProfileMerge.merge(null, parsed("ABC123", upgrade("Archer"), apprentice = 12), "{}", now)
        assertEquals("Imported ABC123", result.displayName)
        assertEquals(listOf("Archer"), result.activeUpgrades.map { it.name })
        assertEquals(12, result.builderApprenticeLevel)
    }

    @Test
    fun reimportKeepsIdAndSettingsAndReplacesUpgrades() {
        val existing = PlayerAccount(
            displayName = "Mio villaggio",
            tag = "ABC123",
            builderCount = 6,
            goldPassBoost = 15,
            activeUpgrades = listOf(upgrade("Old")),
            activeBoosts = listOf(
                ActiveBoost(type = "builder_bite", startTime = now, endTime = now.plusSeconds(3600)),
                ActiveBoost(type = "builder_bite", startTime = now, endTime = now.plusSeconds(-10))
            )
        )
        val merged = ProfileMerge.merge(existing, parsed("ABC123", upgrade("New")), "{}", now)

        assertEquals(existing.id, merged.id)
        assertEquals("Mio villaggio", merged.displayName)
        assertEquals(6, merged.builderCount)
        assertEquals(15, merged.goldPassBoost)
        assertEquals(listOf("New"), merged.activeUpgrades.map { it.name })
        // Only the still-running manual boost is kept
        assertEquals(1, merged.activeBoosts.size)
        assertTrue(merged.activeBoosts.all { it.endTime > now })
        assertNotEquals("Old", merged.activeUpgrades.first().name)
    }
}

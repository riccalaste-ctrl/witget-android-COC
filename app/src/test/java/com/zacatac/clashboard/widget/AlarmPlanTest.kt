package com.zacatac.clashboard.widget

import com.zacatac.clashboard.data.model.ActiveBoost
import com.zacatac.clashboard.data.model.BuildingUpgrade
import com.zacatac.clashboard.data.model.NotificationSettings
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.data.model.UpgradeCategory
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmPlanTest {

    private val now = Instant.fromEpochSeconds(1_000_000)

    private fun at(seconds: Long) = Instant.fromEpochSeconds(now.epochSeconds + seconds)

    private fun upgrade(category: UpgradeCategory, endInSeconds: Long, name: String = "Item") = BuildingUpgrade(
        name = name,
        targetLevel = 4,
        endTime = at(endInSeconds),
        category = category,
        startTime = now
    )

    private fun settings(
        enabled: Boolean = true,
        lab: Boolean = true,
        offsetMinutes: Int = 0
    ) = NotificationSettings.DEFAULT.copy(
        notificationsEnabled = enabled,
        labNotificationsEnabled = lab,
        notificationOffsetMinutes = offsetMinutes
    )

    @Test
    fun masterSwitchOffPlansNothing() {
        val profile = PlayerAccount(
            notificationSettings = settings(enabled = false),
            activeUpgrades = listOf(upgrade(UpgradeCategory.BUILDER_VILLAGE, 600))
        )
        assertTrue(planProfileAlarms(profile, now).isEmpty())
    }

    @Test
    fun disabledCategoryIsSkipped() {
        val profile = PlayerAccount(
            notificationSettings = settings(lab = false),
            activeUpgrades = listOf(
                upgrade(UpgradeCategory.LAB, 600, name = "lab"),
                upgrade(UpgradeCategory.BUILDER_VILLAGE, 600, name = "village")
            )
        )
        assertEquals(listOf("village"), planProfileAlarms(profile, now).map { it.name })
    }

    @Test
    fun finishedUpgradesAreSkipped() {
        val profile = PlayerAccount(activeUpgrades = listOf(upgrade(UpgradeCategory.BUILDER_VILLAGE, -5)))
        assertTrue(planProfileAlarms(profile, now).isEmpty())
    }

    @Test
    fun offsetFiresEarlierThanTheEndTime() {
        val profile = PlayerAccount(
            notificationSettings = settings(offsetMinutes = 5),
            activeUpgrades = listOf(upgrade(UpgradeCategory.BUILDER_VILLAGE, 3600))
        )
        val plan = planProfileAlarms(profile, now).single()
        assertEquals(at(3600 - 300).toEpochMilliseconds(), plan.triggerAtMs)
    }

    @Test
    fun offsetThatIsAlreadyPastFiresAtTheEndTime() {
        val profile = PlayerAccount(
            notificationSettings = settings(offsetMinutes = 10),
            activeUpgrades = listOf(upgrade(UpgradeCategory.BUILDER_VILLAGE, 300))
        )
        val plan = planProfileAlarms(profile, now).single()
        assertEquals(at(300).toEpochMilliseconds(), plan.triggerAtMs)
    }

    @Test
    fun runningBoostGetsAnExpiryAlarm() {
        val profile = PlayerAccount(
            activeBoosts = listOf(
                ActiveBoost(type = "research_potion", startTime = now, endTime = at(1800))
            )
        )
        val plan = planProfileAlarms(profile, now).single()
        assertEquals(ACTION_BOOST_EXPIRE, plan.action)
        assertEquals(UpgradeAlarmReceiver.KIND_BOOST, plan.kind)
        assertEquals(at(1800).toEpochMilliseconds(), plan.triggerAtMs)
    }
}

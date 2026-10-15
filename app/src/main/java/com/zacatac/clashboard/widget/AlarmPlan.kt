package com.zacatac.clashboard.widget

import com.zacatac.clashboard.data.model.PlayerAccount
import kotlinx.datetime.Instant

/** One alarm to register with AlarmManager. */
data class AlarmPlan(
    val requestCode: Int,
    val action: String,
    val triggerAtMs: Long,
    val name: String,
    val level: Int,
    val kind: String
)

/**
 * Plans the alarms for one profile. Respects the master switch, the per-category switches and the
 * advance offset. An offset that would already be past fires at the real end time instead.
 */
fun planProfileAlarms(profile: PlayerAccount, now: Instant): List<AlarmPlan> {
    val settings = profile.notificationSettings
    if (!settings.notificationsEnabled) return emptyList()

    val offsetMs = settings.notificationOffsetMinutes.coerceAtLeast(0) * 60_000L
    val plans = mutableListOf<AlarmPlan>()

    for (upgrade in profile.activeUpgrades) {
        if (upgrade.endTime <= now) continue
        if (!settings.allows(upgrade.category)) continue
        val endMs = upgrade.endTime.toEpochMilliseconds()
        val earlyMs = endMs - offsetMs
        val triggerMs = if (earlyMs > now.toEpochMilliseconds()) earlyMs else endMs
        plans += AlarmPlan(
            requestCode = "${profile.id}|upgrade|${upgrade.id}".hashCode(),
            action = ACTION_UPGRADE_COMPLETE,
            triggerAtMs = triggerMs,
            name = upgrade.name,
            level = upgrade.targetLevel,
            kind = UpgradeAlarmReceiver.KIND_UPGRADE
        )
    }

    for (boost in profile.activeBoosts) {
        if (boost.endTime <= now) continue
        val type = boost.boostType ?: continue
        if (type.affectedCategories.none { settings.allows(it) }) continue
        plans += AlarmPlan(
            requestCode = "${profile.id}|boost|${boost.id}".hashCode(),
            action = ACTION_BOOST_EXPIRE,
            triggerAtMs = boost.endTime.toEpochMilliseconds(),
            name = type.displayName,
            level = 0,
            kind = UpgradeAlarmReceiver.KIND_BOOST
        )
    }
    return plans
}

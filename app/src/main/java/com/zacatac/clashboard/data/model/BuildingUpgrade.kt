package com.zacatac.clashboard.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.zacatac.clashboard.data.database.Converters
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
@Entity(tableName = "building_upgrades")
@TypeConverters(Converters::class)
data class BuildingUpgrade(
    @PrimaryKey
    @Serializable(with = UuidSerializer::class)
    val id: UUID = UUID.randomUUID(),
    val dataId: Int? = null,
    val name: String,
    val targetLevel: Int,
    val superchargeLevel: Int? = null,
    val superchargeTargetLevel: Int? = null,
    var endTime: Instant,
    val category: UpgradeCategory,
    val startTime: Instant = Clock.System.now(),
    val totalDuration: Double = 0.0,
    val isSeasonalDefense: Boolean = false,
    val usesGoblin: Boolean = false,
    val appliedRemoteEventIDs: Set<String> = emptySet(),
    val goldPassFactorAtImport: Double? = null,
    val remoteEventTimeMultiplier: Double? = null,
    val sourceExportTimer: Int? = null
) {
    @Ignore
    fun durationBeforeRemoteEvent(goldPassBoost: Int): Double {
        val currentGold = 1.0 - max(0, min(100, goldPassBoost)) / 100.0
        val gold = remoteEventTimeMultiplier?.let { currentGold } ?: goldPassFactorAtImport ?: currentGold
        return max(totalDuration * gold, 1.0)
    }

    @Ignore
    fun effectiveTotalDuration(goldPassBoost: Int): Double {
        return max(durationBeforeRemoteEvent(goldPassBoost) * (remoteEventTimeMultiplier ?: 1.0), 1.0)
    }

    val levelDisplayText: String
        get() = if (superchargeTargetLevel != null) {
            val current = superchargeLevel ?: max(targetLevel - 1, 0)
            "SC $current → $superchargeTargetLevel"
        } else {
            "Lv ${targetLevel - 1} → $targetLevel"
        }

    val showsSuperchargeIcon: Boolean
        get() = superchargeTargetLevel != null

    @Ignore
    fun timeRemaining(referenceDate: Instant = Clock.System.now()): String {
        val remaining = endTime - referenceDate
        if (remaining <= Duration.ZERO) return "Completato"

        val days = remaining.inWholeDays
        val hours = (remaining.inWholeHours % 24).toInt()
        val minutes = (remaining.inWholeMinutes % 60).toInt()
        val seconds = (remaining.inWholeSeconds % 60).toInt()

        return when {
            days > 0 -> "${days}g ${hours}h ${minutes}m ${seconds}s"
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /** Boost-aware countdown text, used by the app so it matches the widgets. */
    @Ignore
    fun timeRemainingText(activeBoosts: List<ActiveBoost>, referenceDate: Instant = Clock.System.now()): String =
        formatDurationWithSeconds(remainingSeconds(activeBoosts, referenceDate))

    /**
     * Seconds of work left, accounting for boosts. The export timer already includes boosts that
     * were active at the export instant ([startTime]), so only boosts starting at or after
     * [startTime] are applied on top of it.
     */
    @Ignore
    fun remainingSeconds(activeBoosts: List<ActiveBoost>, referenceDate: Instant): Double {
        val baseRemaining = max(0.0, (endTime - referenceDate).inWholeMilliseconds / 1000.0)
        val now = referenceDate
        if (now <= startTime) return baseRemaining

        val relevant = relevantBoosts(activeBoosts, now)
        if (relevant.isEmpty()) return baseRemaining

        // Split [startTime, now] at every boost boundary, then sum the boosted extra time.
        val points = mutableListOf(startTime, now)
        for (boost in relevant) {
            val s = boost.startTime
            val e = minOf(now, boost.endTime)
            if (s < e) {
                points.add(s)
                points.add(e)
            }
        }
        val sorted = points.filter { it >= startTime && it <= now }.distinct().sorted()

        var extraSeconds = 0.0
        for (idx in 0 until sorted.size - 1) {
            val segmentStart = sorted[idx]
            val segmentEnd = sorted[idx + 1]
            if (segmentEnd <= segmentStart) continue
            val segmentSeconds = (segmentEnd - segmentStart).inWholeMilliseconds / 1000.0
            extraSeconds += segmentSeconds * speedBoostAt(relevant, segmentStart)
        }
        return max(0.0, baseRemaining - extraSeconds)
    }

    /** Projected completion instant, using the same boost rules as [remainingSeconds]. */
    @Ignore
    fun projectedCompletionDate(activeBoosts: List<ActiveBoost>, referenceDate: Instant = Clock.System.now()): Instant {
        val now = referenceDate
        var remainingWork = remainingSeconds(activeBoosts, now)
        if (remainingWork <= 0.0) return now

        val relevant = relevantBoosts(activeBoosts, now).filter { it.endTime > now }
        if (relevant.isEmpty()) return now.plus(remainingWork.seconds)

        val transitions = mutableListOf(now)
        for (boost in relevant) {
            if (boost.startTime > now) transitions.add(boost.startTime)
            transitions.add(boost.endTime)
        }
        val sortedTransitions = transitions.distinct().sorted()

        for (idx in 0 until sortedTransitions.size - 1) {
            val segmentStart = sortedTransitions[idx]
            val segmentEnd = sortedTransitions[idx + 1]
            val speed = 1.0 + speedBoostAt(relevant, segmentStart)
            val realSeconds = (segmentEnd - segmentStart).inWholeMilliseconds / 1000.0
            val workDone = realSeconds * speed
            if (workDone >= remainingWork) {
                return segmentStart.plus((remainingWork / speed).seconds)
            }
            remainingWork -= workDone
        }
        return sortedTransitions.last().plus(remainingWork.seconds)
    }

    /** Boosts that affect this upgrade, were not already in the export, and are not for another upgrade. */
    private fun relevantBoosts(activeBoosts: List<ActiveBoost>, now: Instant): List<ActiveBoost> {
        return activeBoosts.filter { boost ->
            val type = boost.boostType ?: return@filter false
            if (category !in type.affectedCategories) return@filter false
            if (boost.startTime < startTime) return@filter false
            if (type == BoostType.BUILDER_APPRENTICE || type == BoostType.LAB_ASSISTANT) {
                if (boost.targetUpgradeId != null && boost.targetUpgradeId != id) return@filter false
            }
            true
        }
    }

    /** Sum of speed bonuses active at [at]. Clock Tower boosts do not stack with each other. */
    private fun speedBoostAt(boosts: List<ActiveBoost>, at: Instant): Double {
        var total = 0.0
        var clockTowerApplied = false
        for (boost in boosts) {
            val type = boost.boostType ?: continue
            if (!(boost.startTime <= at && boost.endTime > at)) continue
            val bonus = type.speedMultiplier(boost.helperLevel ?: 0)
            if (type.isClockTowerBoost) {
                if (!clockTowerApplied) {
                    total += bonus
                    clockTowerApplied = true
                }
            } else {
                total += bonus
            }
        }
        return total
    }
}

/** Formats seconds as "Xg Xh Xm Xs" (same style as the app timers). */
fun formatDurationWithSeconds(remainingSeconds: Double): String {
    val total = max(0L, remainingSeconds.toLong())
    if (total <= 0L) return "Completato"
    val days = total / 86_400
    val hours = (total % 86_400) / 3_600
    val minutes = (total % 3_600) / 60
    val seconds = total % 60
    return when {
        days > 0 -> "${days}g ${hours}h ${minutes}m ${seconds}s"
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

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
        if (remaining <= Duration.ZERO) return "Complete"

        val days = remaining.inWholeDays
        val hours = remaining.inWholeHours % 24
        val minutes = remaining.inWholeMinutes % 60

        return when {
            days > 0 -> "$days d $hours h"
            hours > 0 -> "$hours h $minutes m"
            minutes > 0 -> "$minutes m ${remaining.inWholeSeconds % 60} s"
            else -> "${remaining.inWholeSeconds} s"
        }
    }

    @Ignore
    fun remainingSeconds(activeBoosts: List<ActiveBoost>, referenceDate: Instant): Double {
        val baseRemaining = max(0.0, (endTime - referenceDate).inWholeSeconds.toDouble())

        val start = startTime
        val now = referenceDate
        if (now <= start) return baseRemaining

        // Filter boosts that affect this upgrade's category
        val relevantBoosts = activeBoosts.mapNotNull { boost ->
            val boostType = boost.boostType
            if (boostType == null || category !in boostType.affectedCategories) return@mapNotNull null
            if (boostType == BoostType.BUILDER_APPRENTICE || boostType == BoostType.LAB_ASSISTANT) {
                if (boost.targetUpgradeId != null && boost.targetUpgradeId != id) return@mapNotNull null
            }
            boost
        }

        if (relevantBoosts.isEmpty()) return baseRemaining

        // Build timeline of boost periods
        val timePoints = mutableListOf<Instant>(start, now)
        for (boost in relevantBoosts) {
            val s = maxOf(start, boost.startTime)
            val e = minOf(now, boost.endTime)
            if (s < e) {
                timePoints.add(s)
                timePoints.add(e)
            }
        }

        val sortedPoints = timePoints.distinct().sorted()
        if (sortedPoints.size <= 1) return baseRemaining

        // Calculate extra elapsed time from boosts
        var extraElapsed = 0.0
        for (idx in 0 until sortedPoints.size - 1) {
            val segmentStart = sortedPoints[idx]
            val segmentEnd = sortedPoints[idx + 1]
            if (segmentEnd <= segmentStart) continue

            var totalExtra = 0.0
            var clockTowerApplied = false
            for (boost in relevantBoosts) {
                val boostType = boost.boostType ?: continue
                val s = maxOf(start, boost.startTime)
                val e = minOf(now, boost.endTime)
                if (segmentStart < s || segmentStart >= e) continue

                val level = boost.helperLevel ?: 0
                if (boostType.isClockTowerBoost) {
                    if (!clockTowerApplied) {
                        totalExtra += boostType.speedMultiplier(level)
                        clockTowerApplied = true
                    }
                } else {
                    totalExtra += boostType.speedMultiplier(level)
                }
            }
            extraElapsed += (segmentEnd - segmentStart).inWholeSeconds * totalExtra
        }

        val adjustedRemaining = baseRemaining - extraElapsed
        return max(0.0, adjustedRemaining)
    }

    @Ignore
    fun projectedCompletionDate(activeBoosts: List<ActiveBoost>, referenceDate: Instant = Clock.System.now()): Instant {
        val now = referenceDate
        var remainingWork = remainingSeconds(activeBoosts, now)

        if (activeBoosts.isEmpty() || remainingWork <= 0) {
            return now.plus(remainingWork.toLong().seconds)
        }

        // Filter boosts that affect this upgrade's category and are still active
        val relevantBoosts = activeBoosts.mapNotNull { boost ->
            val boostType = boost.boostType
            if (boostType == null || category !in boostType.affectedCategories) return@mapNotNull null
            if (boost.endTime <= now) return@mapNotNull null
            if (boostType == BoostType.BUILDER_APPRENTICE || boostType == BoostType.LAB_ASSISTANT) {
                if (boost.targetUpgradeId != null && boost.targetUpgradeId != id) return@mapNotNull null
            }
            boost
        }.sortedBy { it.endTime }

        if (relevantBoosts.isEmpty()) {
            return now.plus(remainingWork.toLong().seconds)
        }

        // Simulate time passing and calculate when work completes
        var currentTime = now

        // Build timeline of future boost transitions
        val transitions = mutableListOf<Instant>(now)
        for (boost in relevantBoosts) {
            if (boost.startTime > now) transitions.add(boost.startTime)
            transitions.add(boost.endTime)
        }
        val sortedTransitions = transitions.distinct().sorted()

        // Process each time segment
        for (idx in 0 until sortedTransitions.size - 1) {
            val segmentStart = sortedTransitions[idx]
            val segmentEnd = sortedTransitions[idx + 1]

            // Calculate effective speed multiplier for this segment
            var speedMultiplier = 1.0
            var clockTowerApplied = false

            for (boost in relevantBoosts) {
                val boostType = boost.boostType ?: continue
                // Check if this boost is active during this segment
                if (boost.startTime <= segmentStart && boost.endTime > segmentStart) {
                    val level = boost.helperLevel ?: 0
                    if (boostType.isClockTowerBoost) {
                        if (!clockTowerApplied) {
                            speedMultiplier += boostType.speedMultiplier(level)
                            clockTowerApplied = true
                        }
                    } else {
                        speedMultiplier += boostType.speedMultiplier(level)
                    }
                }
            }

            // How much real time passes in this segment
            val realTime = (segmentEnd - segmentStart).inWholeSeconds
            // How much work gets done (boosted time)
            val workDone = realTime * speedMultiplier

            if (workDone >= remainingWork) {
                // Upgrade completes during this segment
                val timeNeeded = remainingWork / speedMultiplier
                return segmentStart.plus(timeNeeded.toLong().seconds)
            }

            remainingWork -= workDone
            currentTime = segmentEnd
        }

        // If we still have work after all boosts expire, add unboosted time
        return currentTime.plus(remainingWork.toLong().seconds)
    }
}
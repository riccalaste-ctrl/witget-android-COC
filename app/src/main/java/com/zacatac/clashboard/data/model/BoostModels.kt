package com.zacatac.clashboard.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.zacatac.clashboard.data.database.Converters
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*
import kotlin.math.max

@Serializable
enum class UpgradeCategory(val value: String) {
    BUILDER_VILLAGE("builderVillage"),
    LAB("lab"),
    STAR_LAB("starLab"),
    PETS("pets"),
    BUILDER_BASE("builderBase");

    val displayName: String
        get() = when (this) {
            BUILDER_VILLAGE -> "Builders"
            LAB -> "Lab"
            STAR_LAB -> "Star Lab"
            PETS -> "Pet House"
            BUILDER_BASE -> "Builder Base"
        }
}

@Serializable
enum class BoostType(val id: String) {
    BUILDER_POTION("builder_potion"),
    RESEARCH_POTION("research_potion"),
    PET_POTION("pet_potion"),
    LAB_ASSISTANT("lab_assistant"),
    BUILDER_APPRENTICE("builder_apprentice"),
    BUILDER_BITE("builder_bite"),
    STUDY_SOUP("study_soup"),
    CLOCK_TOWER_POTION("clock_tower_potion"),
    CLOCK_TOWER("clock_tower");

    val displayName: String
        get() = when (this) {
            BUILDER_POTION -> "Builder Potion"
            RESEARCH_POTION -> "Research Potion"
            PET_POTION -> "Pet Potion"
            LAB_ASSISTANT -> "Lab Assistant"
            BUILDER_APPRENTICE -> "Builder's Apprentice"
            BUILDER_BITE -> "Builder Bite"
            STUDY_SOUP -> "Study Soup"
            CLOCK_TOWER_POTION -> "Clock Tower Potion"
            CLOCK_TOWER -> "Clock Tower"
        }

    val assetPath: String
        get() = when (this) {
            BUILDER_POTION -> "extras/builder_potion"
            RESEARCH_POTION -> "extras/research_potion"
            PET_POTION -> "extras/pet_potion"
            LAB_ASSISTANT -> "profile/lab_assistant"
            BUILDER_APPRENTICE -> "profile/apprentice_builder"
            BUILDER_BITE -> "extras/builder_bite"
            STUDY_SOUP -> "extras/study_soup"
            CLOCK_TOWER_POTION -> "extras/clock_tower_potion"
            CLOCK_TOWER -> "builder_base/clock_tower"
        }

    // Boost multipliers as percentages (10x = +900%, 2x = +100%)
    // For level-based boosts, returns 0 and actual multiplier calculated from level
    fun speedMultiplier(level: Int = 0): Double = when (this) {
        BUILDER_POTION -> 9.0      // +900% = 10x
        RESEARCH_POTION -> 23.0    // +2300% = 24x
        PET_POTION -> 23.0         // +2300% = 24x
        BUILDER_BITE -> 1.0        // +100% = 2x
        STUDY_SOUP -> 3.0          // +300% = 4x
        LAB_ASSISTANT, BUILDER_APPRENTICE -> level.toDouble() // Level-based: level 8 = +800%
        CLOCK_TOWER_POTION, CLOCK_TOWER -> 9.0  // +900% = 10x speed
    }

    fun durationSeconds(clockTowerLevel: Int = 0): Long = when (this) {
        BUILDER_POTION, RESEARCH_POTION, PET_POTION, BUILDER_BITE, STUDY_SOUP,
        LAB_ASSISTANT, BUILDER_APPRENTICE -> 3600L // 1 hour
        CLOCK_TOWER_POTION -> 1800L // 30 minutes
        CLOCK_TOWER -> {
            // 14 minutes at level 1, +2 minutes per level (level 1 = 14, level 10 = 32)
            val minutes = max(1, clockTowerLevel)
            ((14 + (minutes - 1) * 2) * 60).toLong()
        }
    }

    // Which upgrade categories this boost affects
    val affectedCategories: Set<UpgradeCategory>
        get() = when (this) {
            BUILDER_POTION, BUILDER_BITE, BUILDER_APPRENTICE -> setOf(UpgradeCategory.BUILDER_VILLAGE)
            RESEARCH_POTION, LAB_ASSISTANT -> setOf(UpgradeCategory.LAB)
            PET_POTION -> setOf(UpgradeCategory.PETS)
            STUDY_SOUP -> setOf(UpgradeCategory.LAB, UpgradeCategory.PETS)
            CLOCK_TOWER_POTION, CLOCK_TOWER -> setOf(UpgradeCategory.BUILDER_BASE)
        }

    // Clock tower boosts don't stack with each other, they extend duration instead
    val isClockTowerBoost: Boolean
        get() = this == CLOCK_TOWER || this == CLOCK_TOWER_POTION

    val requiresTargetSelection: Boolean
        get() = this == BUILDER_APPRENTICE

    val requiresClockTowerLevel: Boolean
        get() = this == CLOCK_TOWER

    companion object {
        fun fromString(type: String): BoostType? = values().firstOrNull { it.id == type }
    }
}

@Serializable
data class ActiveBoost(
    @Serializable(with = UuidSerializer::class)
    val id: UUID = UUID.randomUUID(),
    val type: String,
    val startTime: Instant,
    val endTime: Instant,
    @Serializable(with = UuidSerializer::class)
    val targetUpgradeId: UUID? = null,
    val helperLevel: Int? = null
) {
    val boostType: BoostType?
        get() = BoostType.fromString(type)
}

object UuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

@Serializable
data class InstantBoostEndTimeSnapshot(
    @Serializable(with = UuidSerializer::class)
    val upgradeID: UUID,
    val previousEndTime: Instant
)

@Serializable
data class InstantBoostUndoAction(
    @Serializable(with = UuidSerializer::class)
    val id: UUID = UUID.randomUUID(),
    val type: String,
    val appliedAt: Instant,
    val expiresAt: Instant,
    val endTimeSnapshots: List<InstantBoostEndTimeSnapshot>,
    val removedUpgrades: List<BuildingUpgrade>
) {
    val boostType: BoostType?
        get() = BoostType.fromString(type)
}

@Serializable
enum class AppearancePreference(val value: String) {
    DEVICE("device"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        val STORAGE_KEY = "appearance_preference"
    }
}

@Serializable
data class NotificationSettings(
    val notificationsEnabled: Boolean = false,
    val builderNotificationsEnabled: Boolean = true,
    val labNotificationsEnabled: Boolean = true,
    val petNotificationsEnabled: Boolean = true,
    val builderBaseNotificationsEnabled: Boolean = true,
    val helperNotificationsEnabled: Boolean = true,
    val clanWarNotificationsEnabled: Boolean = false,
    val autoOpenClashOfClansEnabled: Boolean = false,
    val notificationOffsetMinutes: Int = 0
) {
    companion object {
        val DEFAULT = NotificationSettings(
            notificationsEnabled = true,
            builderNotificationsEnabled = true,
            labNotificationsEnabled = true,
            petNotificationsEnabled = true,
            builderBaseNotificationsEnabled = true,
            helperNotificationsEnabled = true,
            clanWarNotificationsEnabled = false
        )
    }

    fun allows(category: UpgradeCategory): Boolean = when (category) {
        UpgradeCategory.BUILDER_VILLAGE -> builderNotificationsEnabled
        UpgradeCategory.LAB -> labNotificationsEnabled
        UpgradeCategory.STAR_LAB -> builderBaseNotificationsEnabled
        UpgradeCategory.PETS -> petNotificationsEnabled
        UpgradeCategory.BUILDER_BASE -> builderBaseNotificationsEnabled
    }
}
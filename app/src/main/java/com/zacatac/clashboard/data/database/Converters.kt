package com.zacatac.clashboard.data.database

import androidx.room.TypeConverter
import com.zacatac.clashboard.data.model.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.util.*

class Converters {
    @TypeConverter
    fun fromInstant(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun toInstant(value: Instant?): Long? = value?.toEpochMilliseconds()

    @TypeConverter
    fun fromUUID(value: String?): UUID? = value?.let { UUID.fromString(it) }

    @TypeConverter
    fun toUUID(value: UUID?): String? = value?.toString()

    @TypeConverter
    fun fromUpgradeCategory(value: String?): UpgradeCategory? = value?.let { v -> UpgradeCategory.entries.first { it.value == v } }

    @TypeConverter
    fun toUpgradeCategory(value: UpgradeCategory?): String? = value?.value

    @TypeConverter
    fun fromBoostType(value: String?): BoostType? = value?.let { BoostType.fromString(it) }

    @TypeConverter
    fun toBoostType(value: BoostType?): String? = value?.id

    @TypeConverter
    fun fromAppearancePreference(value: String?): AppearancePreference? = value?.let { AppearancePreference.valueOf(it.uppercase()) }

    @TypeConverter
    fun toAppearancePreference(value: AppearancePreference?): String? = value?.value

    @TypeConverter
    fun fromNotificationSettings(value: String?): NotificationSettings? = value?.let {
        kotlinx.serialization.json.Json.decodeFromString<NotificationSettings>(it)
    }

    @TypeConverter
    fun toNotificationSettings(value: NotificationSettings?): String? = value?.let {
        kotlinx.serialization.json.Json.encodeToString(it)
    }

    @TypeConverter
    fun fromActiveBoostList(value: String?): List<ActiveBoost>? = value?.let {
        kotlinx.serialization.json.Json.decodeFromString<List<ActiveBoost>>(it)
    }

    @TypeConverter
    fun toActiveBoostList(value: List<ActiveBoost>?): String? = value?.let {
        kotlinx.serialization.json.Json.encodeToString(it)
    }

    @TypeConverter
    fun fromBuildingUpgradeList(value: String?): List<BuildingUpgrade>? = value?.let {
        kotlinx.serialization.json.Json.decodeFromString<List<BuildingUpgrade>>(it)
    }

    @TypeConverter
    fun toBuildingUpgradeList(value: List<BuildingUpgrade>?): String? = value?.let {
        kotlinx.serialization.json.Json.encodeToString(it)
    }

    @TypeConverter
    fun fromInstantBoostUndoActionList(value: String?): List<InstantBoostUndoAction>? = value?.let {
        kotlinx.serialization.json.Json.decodeFromString<List<InstantBoostUndoAction>>(it)
    }

    @TypeConverter
    fun toInstantBoostUndoActionList(value: List<InstantBoostUndoAction>?): String? = value?.let {
        kotlinx.serialization.json.Json.encodeToString(it)
    }

    @TypeConverter
    fun fromPlayerProfile(value: String?): PlayerProfile? = value?.let {
        kotlinx.serialization.json.Json.decodeFromString<PlayerProfile>(it)
    }

    @TypeConverter
    fun toPlayerProfile(value: PlayerProfile?): String? = value?.let {
        kotlinx.serialization.json.Json.encodeToString(it)
    }

    @TypeConverter
    fun fromWarDetails(value: String?): WarDetails? = value?.let {
        kotlinx.serialization.json.Json.decodeFromString<WarDetails>(it)
    }

    @TypeConverter
    fun toWarDetails(value: WarDetails?): String? = value?.let {
        kotlinx.serialization.json.Json.encodeToString(it)
    }

    @TypeConverter
    fun fromSetString(value: String?): Set<String>? = value?.let {
        kotlinx.serialization.json.Json.decodeFromString<Set<String>>(it)
    }

    @TypeConverter
    fun toSetString(value: Set<String>?): String? = value?.let {
        kotlinx.serialization.json.Json.encodeToString(it)
    }

    @TypeConverter
    fun fromMapIntInstant(value: String?): Map<Int, Instant>? = value?.let {
        val map = kotlinx.serialization.json.Json.decodeFromString<Map<String, String>>(it)
        map.mapKeys { it.key.toInt() }.mapValues { (_, v) -> Instant.parse(v) }
    }

    @TypeConverter
    fun toMapIntInstant(value: Map<Int, Instant>?): String? = value?.let {
        val map = it.mapKeys { it.key.toString() }.mapValues { (_, v) -> v.toString() }
        kotlinx.serialization.json.Json.encodeToString(map)
    }
}
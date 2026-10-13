package com.zacatac.clashboard.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.zacatac.clashboard.data.database.Converters
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
@Entity(tableName = "player_accounts")
@TypeConverters(Converters::class)
data class PlayerAccount(
    @PrimaryKey
    @Serializable(with = UuidSerializer::class)
    val id: UUID = UUID.randomUUID(),
    var displayName: String = "New Profile",
    var tag: String = "",
    var rawJSON: String = "",
    var lastImportDate: Instant? = null,
    var activeUpgrades: List<BuildingUpgrade> = emptyList(),
    var cachedProfile: PlayerProfile? = null,
    var apiProfileJSON: String = "",
    var lastAPIFetchDate: Instant? = null,
    var notificationSettings: NotificationSettings = NotificationSettings.DEFAULT,
    var builderCount: Int = 5,
    var builderApprenticeLevel: Int = 0,
    var labAssistantLevel: Int = 0,
    var alchemistLevel: Int = 0,
    var clockTowerLevel: Int = 0,
    var goldPassBoost: Int = 0,
    var goldPassReminderEnabled: Boolean = false,
    var hiddenEquipmentNames: Set<String> = emptySet(),
    var activeBoosts: List<ActiveBoost> = emptyList(),
    var instantBoostUndoActions: List<InstantBoostUndoAction> = emptyList(),
    var helperCooldownOverrides: Map<Int, Instant> = emptyMap()
) {
    val activeUpgradesSorted: List<BuildingUpgrade>
        get() = activeUpgrades.sortedBy { it.endTime }

    @Ignore
    fun builderUpgrades(): List<BuildingUpgrade> = activeUpgrades.filter { it.category == UpgradeCategory.BUILDER_VILLAGE }

    @Ignore
    fun labUpgrades(): List<BuildingUpgrade> = activeUpgrades.filter { it.category == UpgradeCategory.LAB }

    @Ignore
    fun starLabUpgrades(): List<BuildingUpgrade> = activeUpgrades.filter { it.category == UpgradeCategory.STAR_LAB }

    @Ignore
    fun petUpgrades(): List<BuildingUpgrade> = activeUpgrades.filter { it.category == UpgradeCategory.PETS }

    @Ignore
    fun builderBaseUpgrades(): List<BuildingUpgrade> = activeUpgrades.filter { it.category == UpgradeCategory.BUILDER_BASE }

    val widgetDisplayName: String
        get() = if (displayName.isNotBlank()) displayName
        else if (tag.isNotBlank()) "#$tag"
        else "Clashboard"

    val townHallLevel: Int
        get() = cachedProfile?.townHallLevel ?: 0
}

@Serializable
data class PlayerProfile(
    val tag: String,
    val name: String,
    val townHallLevel: Int,
    val townHallWeaponLevel: Int? = null,
    val expLevel: Int,
    val trophies: Int,
    val bestTrophies: Int,
    val warStars: Int? = null,
    val attackWins: Int? = null,
    val defenseWins: Int? = null,
    val builderHallLevel: Int? = null,
    val builderBaseTrophies: Int? = null,
    val bestBuilderBaseTrophies: Int? = null,
    val donations: Int? = null,
    val donationsReceived: Int? = null,
    val clanCapitalContributions: Int? = null,
    val clan: Clan? = null,
    val leagueTier: LeagueTier? = null,
    val builderBaseLeague: LeagueTier? = null,
    val achievements: List<PlayerAchievement>? = null,
    val labels: List<PlayerLabel>? = null,
    val heroes: List<HeroProfile>? = null,
    val troops: List<TroopProfile>? = null,
    val spells: List<SpellProfile>? = null,
    val heroEquipment: List<HeroEquipment>? = null,
    val playerHouse: PlayerHouse? = null
)

@Serializable
data class Clan(
    val tag: String,
    val name: String,
    val clanLevel: Int,
    val badgeUrls: BadgeUrls
)

@Serializable
data class LeagueTier(
    val id: Int,
    val name: String,
    val iconUrls: IconUrls? = null
)

@Serializable
data class IconUrls(
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null
)

@Serializable
data class BadgeUrls(
    val small: String,
    val medium: String,
    val large: String
)

@Serializable
data class HeroProfile(
    val name: String,
    val level: Int,
    val maxLevel: Int,
    val village: String,
    val equipment: List<HeroEquipment>? = null
)

@Serializable
data class TroopProfile(
    val name: String,
    val level: Int,
    val maxLevel: Int,
    val village: String
)

@Serializable
data class SpellProfile(
    val name: String,
    val level: Int,
    val maxLevel: Int,
    val village: String
)

@Serializable
data class HeroEquipment(
    val name: String,
    val level: Int,
    val maxLevel: Int,
    val village: String
)

@Serializable
data class PlayerLabel(
    val id: Int,
    val name: String,
    val iconUrls: IconUrls? = null
)

@Serializable
data class PlayerAchievement(
    val name: String,
    val stars: Int,
    val value: Int,
    val target: Int,
    val info: String,
    val completionInfo: String? = null,
    val village: String
) {
    val id: String get() = name
}

@Serializable
data class PlayerHouse(
    val elements: List<HouseElement>? = null
)

@Serializable
data class HouseElement(
    val type: String,
    val elementID: Int
) {
    val id: Int get() = elementID
}
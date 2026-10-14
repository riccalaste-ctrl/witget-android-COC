package com.zacatac.clashboard.data.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max

// Clan War Models
@Serializable
data class WarResponse(
    val state: String? = null,
    val clan: ClanWarMember? = null,
    val opponent: ClanWarMember? = null,
    val preparationStartTime: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val teamSize: Int? = null,
    val attacksPerMember: Int? = null,
    val battleModifier: String? = null
)

@Serializable
data class ClanWarMember(
    val tag: String? = null,
    val name: String? = null,
    val badgeUrls: BadgeUrls? = null,
    val clanLevel: Int? = null,
    val attacks: Int? = null,
    val stars: Int? = null,
    val destructionPercentage: Double? = null,
    val members: List<ClanMember>? = null
)

@Serializable
data class ClanMember(
    val tag: String? = null,
    val name: String? = null,
    val townhallLevel: Int? = null,
    val mapPosition: Int? = null,
    val attacks: List<ClanAttack>? = null,
    val opponentAttacks: Int? = null,
    val bestOpponentAttack: ClanAttack? = null
)

@Serializable
data class ClanAttack(
    val attackerTag: String? = null,
    val defenderTag: String? = null,
    val stars: Int? = null,
    val destructionPercentage: Double? = null,
    val order: Int? = null,
    val duration: Int? = null
)

@Serializable
data class WarDetails(
    val state: String? = null,
    val clan: ClanWarMember? = null,
    val opponent: ClanWarMember? = null,
    val preparationStartTime: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val teamSize: Int? = null,
    val attacksPerMember: Int? = null,
    val battleModifier: String? = null
)

@Serializable
data class PlayerClan(
    val tag: String? = null,
    val name: String? = null,
    val clanLevel: Int? = null,
    val badgeUrls: BadgeUrls? = null
)

@Serializable
data class PlayerLeague(
    val id: Int? = null,
    val name: String? = null,
    val iconUrls: BadgeUrls? = null
)

@Serializable
data class ClanCapital(
    val capitalHallLevel: Int? = null
)

@Serializable
data class WarLeagueGroup(
    val state: String? = null,
    val season: String? = null,
    val rounds: List<WarLeagueRound>? = null
)

@Serializable
data class WarLeagueRound(
    val warTags: List<String>? = null
)

// Deep JSON Export Models with Supercell exact keys
@Serializable
data class CoCExport(
    val tag: String? = null,
    val timestamp: Long? = null,
    val helpers: List<ExportHelper>? = null,
    val guardians: List<ExportGuardian>? = null,
    val buildings: List<Building>? = null,
    val buildings2: List<Building>? = null,
    val traps: List<Trap>? = null,
    val traps2: List<Trap>? = null,
    val heroes: List<ExportHero>? = null,
    val heroes2: List<ExportHero>? = null,
    val pets: List<ExportPet>? = null,
    @SerialName("siege_machines")
    val siegeMachines: List<ExportSiegeMachine>? = null,
    val units: List<ExportUnit>? = null,
    val units2: List<ExportUnit>? = null,
    val spells: List<ExportSpell>? = null,
    val equipment: List<ExportUnit>? = null
)

@Serializable
data class ExportGuardian(
    val data: Int,
    val lvl: Int? = null,
    val timer: Int? = null,
    val extra: Boolean? = null
)

@Serializable
data class Building(
    val data: Int,
    val lvl: Int? = null,
    val weapon: Int? = null,
    val timer: Int? = null,
    val cnt: Int? = null,
    val supercharge: Int? = null,
    val extra: Boolean? = null,
    val types: List<BuildingType>? = null
)

@Serializable
data class BuildingType(
    val data: Int,
    val modules: List<BuildingModule>? = null
)

@Serializable
data class BuildingModule(
    val data: Int,
    val lvl: Int? = null,
    val timer: Int? = null,
    val extra: Boolean? = null
)

@Serializable
data class Trap(
    val data: Int,
    val lvl: Int,
    val timer: Int? = null,
    val cnt: Int? = null,
    val extra: Boolean? = null
)

@Serializable
data class ExportHero(
    val data: Int,
    val lvl: Int,
    val timer: Int? = null,
    val extra: Boolean? = null
)

@Serializable
data class ExportPet(
    val data: Int,
    val lvl: Int,
    val timer: Int? = null,
    val extra: Boolean? = null
)

@Serializable
data class ExportSiegeMachine(
    override val data: Int,
    override val lvl: Int,
    override val timer: Int? = null,
    override val extra: Boolean? = null
) : ExportLabItem

@Serializable
data class ExportUnit(
    override val data: Int,
    override val lvl: Int,
    override val timer: Int? = null,
    override val extra: Boolean? = null
) : ExportLabItem

@Serializable
data class ExportSpell(
    override val data: Int,
    override val lvl: Int,
    override val timer: Int? = null,
    override val extra: Boolean? = null
) : ExportLabItem

interface ExportLabItem {
    val data: Int
    val lvl: Int
    val timer: Int?
    val extra: Boolean?
}

@Serializable
data class ExportHelper(
    val data: Int,
    val lvl: Int,
    @SerialName("helper_cooldown")
    val helperCooldown: Int? = null
)

@Serializable
data class HelperCooldownEntry(
    val id: Int,
    val level: Int,
    val cooldownSeconds: Int,
    val expiresAt: kotlinx.datetime.Instant? = null
) {
    fun remainingSeconds(referenceDate: kotlinx.datetime.Instant = Clock.System.now()): Int {
        return if (expiresAt != null) {
            max(0, (expiresAt!! - referenceDate).inWholeSeconds.toInt())
        } else {
            max(0, cooldownSeconds)
        }
    }
}

// Parsed building data from JSON
@Serializable
data class ParsedBuildingLevel(
    val level: Int,
    val buildTimeSeconds: Int? = null,
    val buildResource: String? = null,
    val buildCost: Int? = null,
    val townHallLevel: Int? = null
)

@Serializable
data class ParsedBuilding(
    val id: Int,
    val internalName: String,
    val levels: List<ParsedBuildingLevel>
)

@Serializable
data class ParsedMiniLevel(
    val internalName: String,
    val levels: List<ParsedMiniLevelLevel>
)

@Serializable
data class ParsedMiniLevelLevel(
    val level: Int,
    val buildTimeSeconds: Int? = null
)

data class RemainingBuildingUpgrade(
    val id: Int,
    val name: String,
    val fromLevel: Int,
    val toLevel: Int,
    val buildTimeSeconds: Int,
    val isSupercharge: Boolean = false
)

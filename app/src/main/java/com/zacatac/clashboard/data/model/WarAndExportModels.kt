package com.zacatac.clashboard.data.model

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

@Serializable
data class WarDetails(
    val state: String,
    val teamSize: Int,
    val attacksPerMember: Int? = null,
    val battleModifier: String? = null,
    val preparationStartTime: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val clan: WarClan? = null,
    val opponent: WarClan? = null
)

@Serializable
data class WarClan(
    val tag: String,
    val name: String,
    val badgeUrls: BadgeUrls,
    val clanLevel: Int,
    val attacks: Int? = null,
    val stars: Int? = null,
    val destructionPercentage: Double? = null,
    val members: List<WarMember>? = null
)

@Serializable
data class WarMember(
    val tag: String,
    val name: String,
    val townhallLevel: Int? = null,
    val mapPosition: Int? = null,
    val attacks: List<WarAttack>? = null,
    val opponentAttacks: Int? = null,
    val bestOpponentAttack: WarAttack? = null
)

@Serializable
data class WarAttack(
    val attackerTag: String? = null,
    val defenderTag: String? = null,
    val stars: Int? = null,
    val destructionPercentage: Int? = null,
    val order: Int? = null,
    val duration: Int? = null
)

@Serializable
data class ClanStats(
    val tag: String,
    val name: String,
    val type: String? = null,
    val description: String? = null,
    val location: ClanLocation? = null,
    val isFamilyFriendly: Boolean? = null,
    val badgeUrls: BadgeUrls,
    val clanLevel: Int,
    val clanPoints: Int? = null,
    val clanBuilderBasePoints: Int? = null,
    val clanCapitalPoints: Int? = null,
    val clanCapital: ClanCapital? = null,
    val capitalLeague: ClanLeague? = null,
    val requiredTrophies: Int? = null,
    val warFrequency: String? = null,
    val warWinStreak: Int? = null,
    val warWins: Int? = null,
    val warTies: Int? = null,
    val warLosses: Int? = null,
    val warLeague: ClanLeague? = null,
    val members: Int? = null
)

@Serializable
data class ClanLocation(
    val id: Int? = null,
    val name: String? = null,
    val isCountry: Boolean? = null,
    val countryCode: String? = null
)

@Serializable
data class ClanLeague(
    val id: Int? = null,
    val name: String? = null
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

// Deep JSON Export Models
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
    val currentLevel: Int,
    val targetLevel: Int,
    val buildTimeSeconds: Int,
    val buildResource: String,
    val buildCost: Int
)

@Serializable
data class ResourceTotals(
    var gold: Int = 0,
    var elixir: Int = 0,
    var darkElixir: Int = 0
) {
    fun add(resource: String, amount: Int) {
        val key = resource.lowercase()
        when {
            key.contains("dark") -> darkElixir += amount
            key.contains("elixir") -> elixir += amount
            key.contains("gold") -> gold += amount
        }
    }

    operator fun plus(other: ResourceTotals): ResourceTotals {
        return ResourceTotals(
            gold = gold + other.gold,
            elixir = elixir + other.elixir,
            darkElixir = darkElixir + other.darkElixir
        )
    }

    val totalValue: Int
        get() = gold + elixir + darkElixir
}

data class CategoryProgress(
    val id: String,
    val title: String,
    val remainingTime: Double,
    val totalTime: Double,
    val remainingCost: ResourceTotals,
    val totalCost: ResourceTotals
) {
    val completion: Double
        get() {
            if (id == "walls") {
                val total = max(totalCost.totalValue, 1).toDouble()
                return max(0.0, min(1.0, 1.0 - (remainingCost.totalValue.toDouble() / total)))
            }
            if (totalTime <= 0) return 1.0
            return max(0.0, min(1.0, 1.0 - (remainingTime / totalTime)))
        }
}

data class TownHallProgress(
    val id: Int,
    val level: Int,
    val categories: List<CategoryProgress>
) {
    val overallCompletion: Double
        get() {
            if (categories.isEmpty()) return 0.0
            var weightedSum = 0.0
            var weightTotal = 0.0
            for (category in categories) {
                val weight = max(category.totalCost.totalValue, 1).toDouble()
                weightedSum += category.completion * weight
                weightTotal += weight
            }
            if (weightTotal <= 0) return 0.0
            return weightedSum / weightTotal
        }

    val remainingCosts: ResourceTotals
        get() = categories.fold(ResourceTotals()) { acc, cat -> acc + cat.remainingCost }

    val remainingTime: Double
        get() = categories.sumOf { it.remainingTime }
}

@Serializable
data class TownHallLevelCounts(
    val level: Int,
    val counts: Map<String, Int>
)

data class ParsedUnitLevelData(
    val level: Int,
    val upgradeTimeSeconds: Int,
    val upgradeCost: Int,
    val upgradeResource: String,
    val laboratoryLevel: Int
)

data class ParsedUnitData(
    val id: Int,
    val name: String,
    val levels: List<ParsedUnitLevelData>
) {
    val levelsByLevel: Map<Int, ParsedUnitLevelData>
        get() = levels.associateBy { it.level }

    fun maxLevelForLabLevel(labLevel: Int): Int {
        return levels
            .filter { it.laboratoryLevel <= labLevel && it.level > 0 }
            .maxByOrNull { it.level }?.level ?: 0
    }
}
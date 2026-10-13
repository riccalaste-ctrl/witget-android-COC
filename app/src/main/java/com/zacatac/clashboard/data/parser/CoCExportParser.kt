package com.zacatac.clashboard.data.parser

import android.content.Context
import android.content.res.AssetManager
import com.zacatac.clashboard.data.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStreamReader
import java.util.*
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

class CoCExportParser(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val assetManager: AssetManager = context.assets

    // Mapping from dataId to building name
    private val nameMapping: MutableMap<Int, String> = mutableMapOf()
    // Building JSON map for metadata
    private val buildingJsonMap: MutableMap<String, BuildingJsonMapEntry> = mutableMapOf()
    // Build times lookup: dataId -> level -> buildTimeSeconds
    private val buildTimes: MutableMap<Int, MutableMap<Int, Int>> = mutableMapOf()
    // Lab times lookup: dataId -> level -> labTimeSeconds
    private val labTimes: MutableMap<Int, MutableMap<Int, Int>> = mutableMapOf()

    init {
        loadMappings()
    }

    private fun loadMappings() {
        // Load name mapping
        loadJsonMap("json/mapping.json")?.let { map ->
            map.forEach { (key, value) ->
                key.toIntOrNull()?.let { nameMapping[it] = value.jsonPrimitive.content }
            }
        }

        // Load building JSON map
        loadJsonMap("json/json_maps/buildings_json_map.json")?.let { map ->
            map.forEach { (_, value) ->
                val entry = json.decodeFromJsonElement<BuildingJsonMapEntry>(value)
                buildingJsonMap[entry.internalName] = entry
                buildingJsonMap[entry.id.toString()] = entry
            }
        }

        // Load parsed building times from parsed_json_files/buildings.json
        loadParsedBuildTimes("json/parsed_json_files/buildings.json")
        loadParsedBuildTimes("json/parsed_json_files/mini_levels.json")
    }

    private fun loadJsonMap(path: String): Map<String, JsonElement>? {
        return try {
            assetManager.open(path).use { inputStream ->
                val reader = InputStreamReader(inputStream)
                json.decodeFromString<Map<String, JsonElement>>(reader.readText())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadParsedBuildTimes(path: String) {
        try {
            assetManager.open(path).use { inputStream ->
                val reader = InputStreamReader(inputStream)
                val parsed = json.decodeFromString<List<ParsedBuilding>>(reader.readText())
                for (building in parsed) {
                    val timesMap = mutableMapOf<Int, Int>()
                    for (level in building.levels) {
                        level.buildTimeSeconds?.let { timesMap[level.level] = it }
                    }
                    if (timesMap.isNotEmpty()) {
                        buildTimes[building.id] = timesMap
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun parseExport(jsonString: String, profile: PlayerAccount): ParseResult {
        val export = json.decodeFromString<CoCExport>(jsonString)
        val timestamp = export.timestamp?.let { Instant.fromEpochSeconds(it) } ?: Clock.System.now()
        val playerTag = export.tag?.uppercase() ?: ""

        val upgrades = mutableListOf<BuildingUpgrade>()
        val activeBoosts = mutableListOf<ActiveBoost>()

        // Parse buildings (Home Village)
        export.buildings?.forEach { building ->
            parseBuilding(building, UpgradeCategory.BUILDER_VILLAGE, timestamp, upgrades, activeBoosts, profile)
        }

        // Parse buildings2 (Builder Base)
        export.buildings2?.forEach { building ->
            parseBuilding(building, UpgradeCategory.BUILDER_BASE, timestamp, upgrades, activeBoosts, profile)
        }

        // Parse traps
        export.traps?.forEach { trap ->
            parseTrap(trap, UpgradeCategory.BUILDER_VILLAGE, timestamp, upgrades, activeBoosts)
        }

        export.traps2?.forEach { trap ->
            parseTrap(trap, UpgradeCategory.BUILDER_BASE, timestamp, upgrades, activeBoosts)
        }

        // Parse heroes
        export.heroes?.forEach { hero ->
            parseHero(hero, UpgradeCategory.BUILDER_VILLAGE, timestamp, upgrades, activeBoosts)
        }

        export.heroes2?.forEach { hero ->
            parseHero(hero, UpgradeCategory.BUILDER_BASE, timestamp, upgrades, activeBoosts)
        }

        // Parse pets
        export.pets?.forEach { pet ->
            parsePet(pet, timestamp, upgrades, activeBoosts)
        }

        // Parse lab upgrades (units, spells, siege machines)
        export.units?.forEach { unit ->
            parseLabUpgrade(unit, UpgradeCategory.LAB, timestamp, upgrades, activeBoosts)
        }

        export.spells?.forEach { spell ->
            parseLabUpgrade(spell, UpgradeCategory.LAB, timestamp, upgrades, activeBoosts)
        }

        export.siegeMachines?.forEach { siege ->
            parseLabUpgrade(siege, UpgradeCategory.LAB, timestamp, upgrades, activeBoosts)
        }

        // Parse builder base lab (units2 = star lab)
        export.units2?.forEach { unit ->
            parseLabUpgrade(unit, UpgradeCategory.STAR_LAB, timestamp, upgrades, activeBoosts)
        }

        // Parse equipment
        export.equipment?.forEach { equip ->
            parseLabUpgrade(equip, UpgradeCategory.LAB, timestamp, upgrades, activeBoosts)
        }

        // Parse helpers for boost info
        export.helpers?.forEach { helper ->
            parseHelper(helper, activeBoosts, profile)
        }

        return ParseResult(
            upgrades = upgrades,
            activeBoosts = activeBoosts,
            playerTag = playerTag,
            timestamp = timestamp
        )
    }

    private fun parseBuilding(
        building: Building,
        category: UpgradeCategory,
        timestamp: Instant,
        upgrades: MutableList<BuildingUpgrade>,
        activeBoosts: MutableList<ActiveBoost>,
        profile: PlayerAccount
    ) {
        val name = nameMapping[building.data] ?: buildingJsonMap[building.data.toString()]?.displayName ?: "Unknown (${building.data})"
        val currentLevel = building.lvl ?: 0
        val isExtra = building.extra == true
        val usesGoblin = category == UpgradeCategory.BUILDER_VILLAGE && isExtra

        // Check for active upgrade (timer present)
        building.timer?.let { timerSeconds ->
            val targetLevel = currentLevel + 1
            val totalDuration = getBuildTime(building.data, targetLevel, category)?.toDouble() ?: (timerSeconds.toDouble())
            val endTime = timestamp.plus(timerSeconds.toLong().seconds)

            val upgrade = BuildingUpgrade(
                dataId = building.data,
                name = name,
                targetLevel = targetLevel,
                endTime = endTime,
                category = category,
                startTime = timestamp.minus(totalDuration.toLong().seconds),
                totalDuration = totalDuration,
                usesGoblin = usesGoblin,
                sourceExportTimer = timerSeconds
            )
            upgrades.add(upgrade)
        }

        // Check for supercharge
        building.supercharge?.let { superchargeLevel ->
            val targetLevel = superchargeLevel
            val currentLevel = building.lvl ?: (targetLevel - 1)
            building.timer?.let { timerSeconds ->
                val totalDuration = getBuildTime(building.data, targetLevel, category)?.toDouble() ?: (timerSeconds.toDouble())
                val endTime = timestamp.plus(timerSeconds.toLong().seconds)

                val upgrade = BuildingUpgrade(
                    dataId = building.data,
                    name = name,
                    targetLevel = targetLevel,
                    superchargeLevel = currentLevel,
                    superchargeTargetLevel = targetLevel,
                    endTime = endTime,
                    category = category,
                    startTime = timestamp.minus(totalDuration.toLong().seconds),
                    totalDuration = totalDuration,
                    usesGoblin = usesGoblin,
                    sourceExportTimer = timerSeconds
                )
                upgrades.add(upgrade)
            }
        }

        // Handle seasonal defenses (types with modules)
        building.types?.forEach { type ->
            type.modules?.forEach { module ->
                module.timer?.let { timerSeconds ->
                    val moduleName = nameMapping[module.data] ?: buildingJsonMap[module.data.toString()]?.displayName ?: "Unknown (${module.data})"
                    val targetLevel = (module.lvl ?: 0) + 1
                    val totalDuration = getBuildTime(module.data, targetLevel, category)?.toDouble() ?: (timerSeconds.toDouble())
                    val endTime = timestamp.plus(timerSeconds.toLong().seconds)

                    val upgrade = BuildingUpgrade(
                        dataId = module.data,
                        name = moduleName,
                        targetLevel = targetLevel,
                        endTime = endTime,
                        category = category,
                        startTime = timestamp.minus(totalDuration.toLong().seconds),
                        totalDuration = totalDuration,
                        isSeasonalDefense = true,
                        usesGoblin = usesGoblin,
                        sourceExportTimer = timerSeconds
                    )
                    upgrades.add(upgrade)
                }
            }
        }
    }

    private fun parseTrap(
        trap: Trap,
        category: UpgradeCategory,
        timestamp: Instant,
        upgrades: MutableList<BuildingUpgrade>,
        activeBoosts: MutableList<ActiveBoost>
    ) {
        val name = nameMapping[trap.data] ?: "Unknown Trap (${trap.data})"
        val currentLevel = trap.lvl
        trap.timer?.let { timerSeconds ->
            val targetLevel = currentLevel + 1
            val totalDuration = getBuildTime(trap.data, targetLevel, category)?.toDouble() ?: (timerSeconds.toDouble())
            val endTime = timestamp.plus(timerSeconds.toLong().seconds)

            val upgrade = BuildingUpgrade(
                dataId = trap.data,
                name = name,
                targetLevel = targetLevel,
                endTime = endTime,
                category = category,
                startTime = timestamp.minus(totalDuration.toLong().seconds),
                totalDuration = totalDuration,
                sourceExportTimer = timerSeconds
            )
            upgrades.add(upgrade)
        }
    }

    private fun parseHero(
        hero: ExportHero,
        category: UpgradeCategory,
        timestamp: Instant,
        upgrades: MutableList<BuildingUpgrade>,
        activeBoosts: MutableList<ActiveBoost>
    ) {
        val name = nameMapping[hero.data] ?: "Unknown Hero (${hero.data})"
        val currentLevel = hero.lvl
        hero.timer?.let { timerSeconds ->
            val targetLevel = currentLevel + 1
            val totalDuration = getBuildTime(hero.data, targetLevel, category)?.toDouble() ?: (timerSeconds.toDouble())
            val endTime = timestamp.plus(timerSeconds.toLong().seconds)

            val upgrade = BuildingUpgrade(
                dataId = hero.data,
                name = name,
                targetLevel = targetLevel,
                endTime = endTime,
                category = category,
                startTime = timestamp.minus(totalDuration.toLong().seconds),
                totalDuration = totalDuration,
                sourceExportTimer = timerSeconds
            )
            upgrades.add(upgrade)
        }
    }

    private fun parsePet(
        pet: ExportPet,
        timestamp: Instant,
        upgrades: MutableList<BuildingUpgrade>,
        activeBoosts: MutableList<ActiveBoost>
    ) {
        val name = nameMapping[pet.data] ?: "Unknown Pet (${pet.data})"
        val currentLevel = pet.lvl
        pet.timer?.let { timerSeconds ->
            val targetLevel = currentLevel + 1
            val totalDuration = getBuildTime(pet.data, targetLevel, UpgradeCategory.PETS)?.toDouble() ?: (timerSeconds.toDouble())
            val endTime = timestamp.plus(timerSeconds.toLong().seconds)

            val upgrade = BuildingUpgrade(
                dataId = pet.data,
                name = name,
                targetLevel = targetLevel,
                endTime = endTime,
                category = UpgradeCategory.PETS,
                startTime = timestamp.minus(totalDuration.toLong().seconds),
                totalDuration = totalDuration,
                sourceExportTimer = timerSeconds
            )
            upgrades.add(upgrade)
        }
    }

    private fun parseLabUpgrade(
        item: ExportLabItem,
        category: UpgradeCategory,
        timestamp: Instant,
        upgrades: MutableList<BuildingUpgrade>,
        activeBoosts: MutableList<ActiveBoost>
    ) {
        val name = nameMapping[item.data] ?: "Unknown (${item.data})"
        val currentLevel = item.lvl
        item.timer?.let { timerSeconds ->
            val targetLevel = currentLevel + 1
            val totalDuration = getLabTime(item.data, targetLevel, category)?.toDouble() ?: (timerSeconds.toDouble())
            val endTime = timestamp.plus(timerSeconds.toLong().seconds)

            val upgrade = BuildingUpgrade(
                dataId = item.data,
                name = name,
                targetLevel = targetLevel,
                endTime = endTime,
                category = category,
                startTime = timestamp.minus(totalDuration.toLong().seconds),
                totalDuration = totalDuration,
                sourceExportTimer = timerSeconds
            )
            upgrades.add(upgrade)
        }
    }

    private fun parseHelper(
        helper: ExportHelper,
        activeBoosts: MutableList<ActiveBoost>,
        profile: PlayerAccount
    ) {
        // Helper cooldown tracking
        helper.helperCooldown?.let { cooldownSeconds ->
            val expiresAt = Clock.System.now().plus(cooldownSeconds.toLong().seconds)
            profile.helperCooldownOverrides = profile.helperCooldownOverrides + (helper.data to expiresAt)
        }
    }

    private fun getBuildTime(dataId: Int, targetLevel: Int, category: UpgradeCategory): Int? {
        // First try exact level match
        val times = buildTimes[dataId]
        return times?.getOrElse(targetLevel) { 
            // Find closest level if exact not found
            times.entries.minByOrNull { abs(it.key - targetLevel) }?.value 
        }
    }

    private fun getLabTime(dataId: Int, targetLevel: Int, category: UpgradeCategory): Int? {
        val times = labTimes[dataId]
        return times?.getOrElse(targetLevel) { 
            times.entries.minByOrNull { abs(it.key - targetLevel) }?.value 
        }
    }

    data class ParseResult(
        val upgrades: List<BuildingUpgrade>,
        val activeBoosts: List<ActiveBoost>,
        val playerTag: String,
        val timestamp: Instant
    )
}

@kotlinx.serialization.Serializable
data class BuildingJsonMapEntry(
    val internalName: String,
    val id: Int,
    val displayName: String
)
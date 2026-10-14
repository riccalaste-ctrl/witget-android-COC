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
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private val assetManager: AssetManager = context.assets

    // Mapping from dataId to building/unit name
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
        // Load name mapping (227 canonical game IDs)
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

        // Load parsed building times
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
        // Timers in the export are remaining seconds from NOW (when the user exported)
        val referenceDate = Clock.System.now()
        val playerTag = export.tag?.uppercase()?.replace("#", "") ?: ""

        val upgrades = mutableListOf<BuildingUpgrade>()
        val activeBoosts = mutableListOf<ActiveBoost>()

        // Parse buildings (Home Village)
        export.buildings?.forEach { building ->
            parseBuilding(building, UpgradeCategory.BUILDER_VILLAGE, referenceDate, upgrades, profile)
        }

        // Parse buildings2 (Builder Base)
        export.buildings2?.forEach { building ->
            parseBuilding(building, UpgradeCategory.BUILDER_BASE, referenceDate, upgrades, profile)
        }

        // Parse traps (Home Village)
        export.traps?.forEach { trap ->
            parseTrap(trap, UpgradeCategory.BUILDER_VILLAGE, referenceDate, upgrades)
        }

        // Parse traps2 (Builder Base)
        export.traps2?.forEach { trap ->
            parseTrap(trap, UpgradeCategory.BUILDER_BASE, referenceDate, upgrades)
        }

        // Parse heroes (Home Village)
        export.heroes?.forEach { hero ->
            parseHero(hero, UpgradeCategory.BUILDER_VILLAGE, referenceDate, upgrades)
        }

        // Parse heroes2 (Builder Base)
        export.heroes2?.forEach { hero ->
            parseHero(hero, UpgradeCategory.BUILDER_BASE, referenceDate, upgrades)
        }

        // Parse pets (Pet House)
        export.pets?.forEach { pet ->
            parsePet(pet, referenceDate, upgrades)
        }

        // Parse lab upgrades (units, spells, siege machines, equipment)
        export.units?.forEach { unit ->
            parseLabUpgrade(unit, UpgradeCategory.LAB, referenceDate, upgrades)
        }

        export.spells?.forEach { spell ->
            parseLabUpgrade(spell, UpgradeCategory.LAB, referenceDate, upgrades)
        }

        export.siegeMachines?.forEach { siege ->
            parseLabUpgrade(siege, UpgradeCategory.LAB, referenceDate, upgrades)
        }

        // Equipment in the laboratory
        export.equipment?.forEach { equip ->
            parseLabUpgrade(equip, UpgradeCategory.LAB, referenceDate, upgrades)
        }

        // Parse builder base lab (units2 = star lab)
        export.units2?.forEach { unit ->
            parseLabUpgrade(unit, UpgradeCategory.STAR_LAB, referenceDate, upgrades)
        }

        // Guardians (Home Village)
        export.guardians?.forEach { guardian ->
            parseGuardian(guardian, referenceDate, upgrades)
        }

        // Parse helpers for cooldown tracking
        export.helpers?.forEach { helper ->
            parseHelper(helper, profile)
        }

        return ParseResult(
            upgrades = upgrades.sortedBy { it.endTime },
            activeBoosts = activeBoosts,
            playerTag = playerTag,
            timestamp = referenceDate
        )
    }

    private fun parseBuilding(
        building: Building,
        category: UpgradeCategory,
        now: Instant,
        upgrades: MutableList<BuildingUpgrade>,
        profile: PlayerAccount
    ) {
        val name = resolveName(building.data, "Edificio")
        val currentLevel = building.lvl ?: 0
        val isExtra = building.extra == true
        val usesGoblin = category == UpgradeCategory.BUILDER_VILLAGE && isExtra

        // Town Hall Weapon upgrade check
        if (building.data == 1000001 && building.weapon != null && building.timer != null && building.timer > 0) {
            val timerSeconds = building.timer
            val endTime = now.plus(timerSeconds.toLong().seconds)
            val upgrade = BuildingUpgrade(
                dataId = building.data,
                name = "Arma Municipio",
                targetLevel = (building.weapon ?: 0) + 1,
                endTime = endTime,
                category = category,
                startTime = now,
                totalDuration = timerSeconds.toDouble(),
                usesGoblin = usesGoblin,
                sourceExportTimer = timerSeconds
            )
            upgrades.add(upgrade)
            return
        }

        // Regular building upgrade
        if (building.timer != null && building.timer > 0) {
            val timerSeconds = building.timer
            val targetLevel = currentLevel + 1
            val totalDuration = getBuildTime(building.data, targetLevel, category)?.toDouble() ?: timerSeconds.toDouble()
            val endTime = now.plus(timerSeconds.toLong().seconds)

            val superchargeLevel = building.supercharge
            val superchargeTarget = superchargeLevel?.let { it + 1 }

            val upgrade = BuildingUpgrade(
                dataId = building.data,
                name = name,
                targetLevel = targetLevel,
                superchargeLevel = superchargeLevel,
                superchargeTargetLevel = superchargeTarget,
                endTime = endTime,
                category = category,
                startTime = now,
                totalDuration = maxOf(totalDuration, timerSeconds.toDouble()),
                usesGoblin = usesGoblin,
                sourceExportTimer = timerSeconds
            )
            upgrades.add(upgrade)
        }

        // Seasonal defenses (types and modules)
        building.types?.forEach { type ->
            type.modules?.forEach { module ->
                val timerSeconds = module.timer
                if (timerSeconds != null && timerSeconds > 0) {
                    val moduleName = resolveName(type.data, "Difesa Modulare")
                    val targetLevel = (module.lvl ?: 0) + 1
                    val endTime = now.plus(timerSeconds.toLong().seconds)

                    val upgrade = BuildingUpgrade(
                        dataId = type.data,
                        name = moduleName,
                        targetLevel = targetLevel,
                        endTime = endTime,
                        category = category,
                        startTime = now,
                        totalDuration = timerSeconds.toDouble(),
                        isSeasonalDefense = true,
                        usesGoblin = module.extra == true,
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
        now: Instant,
        upgrades: MutableList<BuildingUpgrade>
    ) {
        val timer = trap.timer
        if (timer != null && timer > 0) {
            val name = resolveName(trap.data, "Trappola")
            val targetLevel = trap.lvl + 1
            val endTime = now.plus(timer.toLong().seconds)
            upgrades.add(
                BuildingUpgrade(
                    dataId = trap.data,
                    name = name,
                    targetLevel = targetLevel,
                    endTime = endTime,
                    category = category,
                    startTime = now,
                    totalDuration = timer.toDouble(),
                    sourceExportTimer = timer
                )
            )
        }
    }

    private fun parseHero(
        hero: ExportHero,
        category: UpgradeCategory,
        now: Instant,
        upgrades: MutableList<BuildingUpgrade>
    ) {
        val timer = hero.timer
        if (timer != null && timer > 0) {
            val name = resolveName(hero.data, "Eroe")
            val targetLevel = hero.lvl + 1
            val endTime = now.plus(timer.toLong().seconds)
            upgrades.add(
                BuildingUpgrade(
                    dataId = hero.data,
                    name = name,
                    targetLevel = targetLevel,
                    endTime = endTime,
                    category = category,
                    startTime = now,
                    totalDuration = timer.toDouble(),
                    sourceExportTimer = timer
                )
            )
        }
    }

    private fun parsePet(
        pet: ExportPet,
        now: Instant,
        upgrades: MutableList<BuildingUpgrade>
    ) {
        val timer = pet.timer
        if (timer != null && timer > 0) {
            val name = resolveName(pet.data, "Pet")
            val targetLevel = pet.lvl + 1
            val endTime = now.plus(timer.toLong().seconds)
            upgrades.add(
                BuildingUpgrade(
                    dataId = pet.data,
                    name = name,
                    targetLevel = targetLevel,
                    endTime = endTime,
                    category = UpgradeCategory.PETS,
                    startTime = now,
                    totalDuration = timer.toDouble(),
                    sourceExportTimer = timer
                )
            )
        }
    }

    private fun parseLabUpgrade(
        item: ExportLabItem,
        category: UpgradeCategory,
        now: Instant,
        upgrades: MutableList<BuildingUpgrade>
    ) {
        val timer = item.timer
        if (timer != null && timer > 0) {
            val name = resolveName(item.data, "Laboratorio")
            val targetLevel = item.lvl + 1
            val endTime = now.plus(timer.toLong().seconds)
            upgrades.add(
                BuildingUpgrade(
                    dataId = item.data,
                    name = name,
                    targetLevel = targetLevel,
                    endTime = endTime,
                    category = category,
                    startTime = now,
                    totalDuration = timer.toDouble(),
                    sourceExportTimer = timer
                )
            )
        }
    }

    private fun parseGuardian(
        guardian: ExportGuardian,
        now: Instant,
        upgrades: MutableList<BuildingUpgrade>
    ) {
        val timer = guardian.timer
        if (timer != null && timer > 0) {
            val name = resolveName(guardian.data, "Guardiano")
            val targetLevel = (guardian.lvl ?: 0) + 1
            val endTime = now.plus(timer.toLong().seconds)
            upgrades.add(
                BuildingUpgrade(
                    dataId = guardian.data,
                    name = name,
                    targetLevel = targetLevel,
                    endTime = endTime,
                    category = UpgradeCategory.BUILDER_VILLAGE,
                    startTime = now,
                    totalDuration = timer.toDouble(),
                    usesGoblin = guardian.extra == true,
                    sourceExportTimer = timer
                )
            )
        }
    }

    private fun parseHelper(
        helper: ExportHelper,
        profile: PlayerAccount
    ) {
        helper.helperCooldown?.let { cooldownSeconds ->
            val expiresAt = Clock.System.now().plus(cooldownSeconds.toLong().seconds)
            profile.helperCooldownOverrides = profile.helperCooldownOverrides + (helper.data to expiresAt)
        }
    }

    private fun resolveName(dataId: Int, fallbackPrefix: String): String {
        return nameMapping[dataId]
            ?: buildingJsonMap[dataId.toString()]?.displayName
            ?: "$fallbackPrefix ($dataId)"
    }

    private fun getBuildTime(dataId: Int, targetLevel: Int, category: UpgradeCategory): Int? {
        val times = buildTimes[dataId]
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

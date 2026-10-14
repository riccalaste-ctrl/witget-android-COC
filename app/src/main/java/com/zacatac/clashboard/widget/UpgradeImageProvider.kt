package com.zacatac.clashboard.widget

import android.content.Context
import androidx.glance.ImageProvider
import com.zacatac.clashboard.R
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

/**
 * Resolves upgrade icons to Glance [ImageProvider]s backed by drawable resources.
 * Looks for exact drawables named coc_<slug>, <folder>_<slug>, or <slug>.
 */
class UpgradeImageProvider(private val context: Context) {

    private val assetMap: Map<String, String> by lazy { loadAssetMap() }
    private val resIdCache: MutableMap<String, Int> = mutableMapOf()

    /** Real icon if a drawable exists for the upgrade; null otherwise (callers can use an emoji fallback). */
    fun providerForOrNull(upgradeName: String): ImageProvider? {
        val resId = drawableResFor(upgradeName)
        return if (resId != 0) ImageProvider(resId) else null
    }

    fun providerFor(upgradeName: String): ImageProvider {
        val resId = drawableResFor(upgradeName)
        return ImageProvider(if (resId != 0) resId else R.drawable.ic_import)
    }

    private fun drawableResFor(upgradeName: String): Int {
        resIdCache[upgradeName]?.let { return it }

        val assetName = assetMap[upgradeName] ?: sanitizeName(upgradeName)
        val sanitized = sanitizeName(upgradeName)

        // Candidates in order of priority:
        // 1. coc_<assetName>
        // 2. coc_<sanitized>
        // 3. <folder>_<assetName>
        // 4. <assetName>
        val candidates = listOf(
            "coc_$assetName",
            "coc_$sanitized",
            getDrawableFolderForAsset(assetName) + "_" + assetName,
            assetName
        )

        var foundId = 0
        for (candidate in candidates) {
            val id = context.resources.getIdentifier(candidate, "drawable", context.packageName)
            if (id != 0) {
                foundId = id
                break
            }
        }

        resIdCache[upgradeName] = foundId
        return foundId
    }

    private fun loadAssetMap(): Map<String, String> {
        return try {
            context.assets.open("json/asset_map.json").use { inputStream ->
                val reader = InputStreamReader(inputStream)
                Json { ignoreUnknownKeys = true }.decodeFromString<Map<String, String>>(reader.readText())
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun getDrawableFolderForAsset(assetName: String): String {
        return when {
            assetName in setOf("hero_bell", "bomb_hive", "light_beam", "hero_hunter", "cake_a_pult", "hot_candle") -> "crafted"
            assetName in setOf("builder_potion", "research_potion", "pet_potion", "builder_bite", "study_soup", "clock_tower_potion", "supercharge", "hero_potion") -> "extras"
            assetName in setOf("lab_assistant", "apprentice_builder", "alchemist", "gold_pass", "free_pass", "gem", "cwl", "goblin_builder", "home_builder") -> "profile"
            assetName in setOf("archer", "barbarian", "dragon", "electro", "golem", "legend_league", "p_e_k_k_a", "skeleton", "titan", "unranked", "valkyrie", "witch", "wizard") -> "leagues"
            assetName in setOf("wall_1", "wall_2", "wall_3", "wall_4", "wall_5", "wall_6", "wall_7", "wall_8", "wall_9", "wall_10", "wall_11", "wall_12", "wall_13", "wall_14", "wall_15", "wall_16", "wall_17", "wall_18", "wall_19") -> "resources"
            assetName in setOf("builder", "builder_elixir", "builder_gold", "clock", "dark_elixir", "elixir", "gold", "lab", "master_builder", "pet_house") -> "resources"
            assetName in setOf("th1", "th2", "th3", "th4", "th5", "th6", "th7", "th8", "th9", "th10", "th11", "th12", "th13", "th14", "th15", "th16", "th17", "th18") -> "townhall"
            assetName in setOf("angry_jelly", "diggy", "electro_owl", "frosty", "greedy_raven", "l_a_s_s_i", "mighty_yak", "phoenix", "poison_lizard", "sneezy", "spirit_fox", "unicorn") -> "pets"
            assetName in setOf("air_bombs", "archer_tower", "army_camp", "baby_dragon", "battle_copter", "battle_machine", "beta_minion", "bomber", "boxer_giant", "builder_barracks", "builder_hall", "b_o_b_control", "cannon", "cannon_cart", "clock_tower", "crusher", "double_cannon", "drop_ship", "electrofire_wizard", "elixir_storage", "firecrackers", "gem_mine", "giant_cannon", "gold_mine", "gold_storage", "guard_post", "healing_hut", "hidden_tesla", "hog_glider", "lava_launcher", "mega_mine", "mega_tesla", "mine", "multi_mortar", "night_witch", "o_t_t_o_s_outpost", "power_p_e_k_k_a", "push_trap", "raged_barbarian", "reinforcement_camp", "roaster", "sneaky_archer", "spring_trap", "star_laboratory", "x_bow") -> "builder_base"
            else -> "lab"
        }
    }

    private fun sanitizeName(name: String): String {
        return name.lowercase()
            .replace(" ", "_")
            .replace("-", "_")
            .replace("'", "")
            .replace(".", "_")
            .replace("(", "")
            .replace(")", "")
            .replace("/", "_")
            .replace(":", "")
            .replace("%", "")
    }
}

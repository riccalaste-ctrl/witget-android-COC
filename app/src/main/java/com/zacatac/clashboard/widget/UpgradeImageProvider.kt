package com.zacatac.clashboard.widget

import android.content.Context
import androidx.glance.ImageProvider
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

/**
 * Resolves upgrade icons to Glance [ImageProvider]s backed by drawable resources.
 * Drawables are named `coc_<slug>` (see res/drawable-nodpi). Returns null when no icon exists
 * so callers can use an emoji fallback.
 */
class UpgradeImageProvider(private val context: Context) {

    private val assetMap: Map<String, String> by lazy { loadAssetMap() }
    private val resIdCache: MutableMap<String, Int> = mutableMapOf()

    /** Real icon if a drawable exists for the upgrade; null otherwise. */
    fun providerForOrNull(upgradeName: String): ImageProvider? {
        val resId = drawableResFor(upgradeName)
        return if (resId != 0) ImageProvider(resId) else null
    }

    /** Drawable resource id for the upgrade icon, or 0 when none exists. */
    fun resIdFor(upgradeName: String): Int = drawableResFor(upgradeName)

    private fun drawableResFor(upgradeName: String): Int {
        resIdCache[upgradeName]?.let { return it }
        val candidates = listOfNotNull(
            assetMap[upgradeName]?.let { "coc_$it" },
            "coc_${sanitizeName(upgradeName)}"
        )
        val resId = candidates
            .map { context.resources.getIdentifier(it, "drawable", context.packageName) }
            .firstOrNull { it != 0 } ?: 0
        resIdCache[upgradeName] = resId
        return resId
    }

    private fun loadAssetMap(): Map<String, String> {
        return try {
            context.assets.open("json/asset_map.json").use { inputStream ->
                val reader = InputStreamReader(inputStream)
                Json { ignoreUnknownKeys = true }.decodeFromString<Map<String, String>>(reader.readText())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    private fun sanitizeName(name: String): String {
        return name.lowercase()
            .replace(" ", "_")
            .replace("-", "_")
            .replace(".", "_")
            .replace("'", "")
            .replace("’", "")
            .replace("(", "")
            .replace(")", "")
            .replace("/", "_")
            .replace(":", "")
            .replace("%", "")
    }
}

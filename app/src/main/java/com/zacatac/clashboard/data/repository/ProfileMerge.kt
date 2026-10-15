package com.zacatac.clashboard.data.repository

import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.data.parser.CoCExportParser
import kotlinx.datetime.Instant

object ProfileMerge {
    // Helper data ids from the export (see WIDGET_ANDROID_SPEC.md); verify against a real export.
    const val HELPER_BUILDER_APPRENTICE = 93000000
    const val HELPER_LAB_ASSISTANT = 93000001
    const val HELPER_ALCHEMIST = 93000002

    /** The stored village for [tag], or null when the tag is blank or unknown. */
    fun findExisting(profiles: List<PlayerAccount>, tag: String): PlayerAccount? {
        if (tag.isBlank()) return null
        return profiles.firstOrNull { it.tag == tag }
    }

    /**
     * Builds the profile to store after an import. An existing village keeps its id, name,
     * builder count, Gold Pass, notification settings and boosts still running; upgrades, timers,
     * cooldowns and helper levels come from the new export.
     */
    fun merge(
        existing: PlayerAccount?,
        result: CoCExportParser.ParseResult,
        rawJson: String,
        now: Instant
    ): PlayerAccount {
        val levels = result.helperLevels
        if (existing == null) {
            return PlayerAccount(
                displayName = "Imported ${result.playerTag}".takeIf { result.playerTag.isNotBlank() } ?: "Imported Village",
                tag = result.playerTag,
                rawJSON = rawJson,
                lastImportDate = result.timestamp,
                activeUpgrades = result.upgrades,
                helperCooldownOverrides = result.helperCooldowns,
                builderApprenticeLevel = levels[HELPER_BUILDER_APPRENTICE] ?: 0,
                labAssistantLevel = levels[HELPER_LAB_ASSISTANT] ?: 0,
                alchemistLevel = levels[HELPER_ALCHEMIST] ?: 0
            )
        }
        return existing.copy(
            tag = result.playerTag.ifBlank { existing.tag },
            rawJSON = rawJson,
            lastImportDate = result.timestamp,
            activeUpgrades = result.upgrades,
            helperCooldownOverrides = result.helperCooldowns,
            builderApprenticeLevel = levels[HELPER_BUILDER_APPRENTICE] ?: existing.builderApprenticeLevel,
            labAssistantLevel = levels[HELPER_LAB_ASSISTANT] ?: existing.labAssistantLevel,
            alchemistLevel = levels[HELPER_ALCHEMIST] ?: existing.alchemistLevel,
            activeBoosts = existing.activeBoosts.filter { it.endTime > now }
        )
    }
}

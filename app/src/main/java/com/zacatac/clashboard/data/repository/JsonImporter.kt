package com.zacatac.clashboard.data.repository

import android.content.Context
import com.zacatac.clashboard.data.database.AppDatabase
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.data.parser.CoCExportParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Single entry point for importing a CoC deep JSON export into Room.
 * Used by the Import screen and can be reused by other import paths.
 */
object JsonImporter {

    suspend fun importFromJson(context: Context, json: String): PlayerAccount = withContext(Dispatchers.IO) {
        val parser = CoCExportParser(context)
        val result = parser.parseExport(json, PlayerAccount())

        val displayName = if (result.playerTag.isBlank()) "Imported Village" else "Imported ${result.playerTag}"
        val profile = PlayerAccount(
            displayName = displayName,
            tag = result.playerTag,
            rawJSON = json,
            lastImportDate = result.timestamp,
            activeUpgrades = result.upgrades,
            activeBoosts = result.activeBoosts
        )

        ProfileRepository(AppDatabase.getDatabase(context)).insertProfile(profile)
        profile
    }
}

package com.zacatac.clashboard.data.repository

import android.content.Context
import com.zacatac.clashboard.data.database.AppDatabase
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.data.parser.CoCExportParser
import com.zacatac.clashboard.util.ProfileSelection
import com.zacatac.clashboard.widget.UpgradeAlarmScheduler
import com.zacatac.clashboard.widget.WidgetTickScheduler
import com.zacatac.clashboard.widget.WidgetUpdateWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * Single entry point for importing a CoC deep JSON export into Room.
 * A re-import of the same player tag updates that village (same id, same settings).
 */
object JsonImporter {

    suspend fun importFromJson(context: Context, json: String): PlayerAccount = withContext(Dispatchers.IO) {
        val result = CoCExportParser(context).parseExport(json)
        val repository = ProfileRepository(AppDatabase.getDatabase(context))

        val existing = ProfileMerge.findExisting(repository.getAllProfiles().first(), result.playerTag)
        val profile = ProfileMerge.merge(existing, result, json, Clock.System.now())

        repository.insertProfile(profile)
        ProfileSelection.select(context, profile.id)

        // Refresh widgets right now, keep the countdown tick running, and re-plan this village's alarms
        WidgetUpdateWorker.scheduleImmediateUpdate(context)
        WidgetTickScheduler.start(context)
        UpgradeAlarmScheduler.scheduleForProfile(context, profile)

        profile
    }
}

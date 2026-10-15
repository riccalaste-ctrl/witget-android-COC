package com.zacatac.clashboard.util

import android.content.Context
import com.zacatac.clashboard.data.model.PlayerAccount
import kotlinx.datetime.Instant
import java.util.UUID

/** Stores which village the app screens (dashboard, notifications, boosts) show. */
object ProfileSelection {
    private const val PREFS = "clashboard_prefs"
    private const val KEY = "selected_profile_id"

    fun storedId(context: Context): UUID? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return raw?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    }

    fun select(context: Context, id: UUID) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, id.toString()).apply()
    }
}

/** Pure: the stored village if it still exists, otherwise the most recently imported one. */
fun resolveSelectedProfile(profiles: List<PlayerAccount>, storedId: UUID?): PlayerAccount? {
    return profiles.firstOrNull { it.id == storedId }
        ?: profiles.maxByOrNull { it.lastImportDate ?: Instant.fromEpochMilliseconds(0) }
}

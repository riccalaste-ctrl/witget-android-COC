package com.zacatac.clashboard.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.zacatac.clashboard.data.database.AppDatabase
import com.zacatac.clashboard.data.model.ActiveBoost
import com.zacatac.clashboard.data.model.BuildingUpgrade
import com.zacatac.clashboard.data.model.PlayerAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.roundToInt

val WidgetAccentGreen = Color(0xFF4CAF50)
val WidgetAccentOrange = Color(0xFFFF9800)
val WidgetMutedGray = Color(0xFF9E9E9E)

fun widgetColor(color: Color): ColorProvider = ColorProvider(color)

/**
 * Picks the profile a widget should display. When [autoProfile] is set, or no profile was
 * explicitly chosen, the first profile is used.
 */
suspend fun selectWidgetProfile(
    context: Context,
    autoProfile: Boolean,
    storedProfileId: String?
): PlayerAccount? = withContext(Dispatchers.IO) {
    val profiles = AppDatabase.getDatabase(context).playerAccountDao().getAllProfiles().first()
    if (profiles.isEmpty()) return@withContext null
    if (autoProfile) return@withContext profiles.first()
    profiles.firstOrNull { it.id.toString() == storedProfileId } ?: profiles.first()
}

fun upgradeProgress(upgrade: BuildingUpgrade, profile: PlayerAccount, now: Instant): Float {
    val total = upgrade.effectiveTotalDuration(profile.goldPassBoost)
    val remaining = upgrade.remainingSeconds(profile.activeBoosts, now)
    return ((total - remaining) / total).coerceIn(0.0, 1.0).toFloat()
}

fun upgradeProgressFor(upgrade: BuildingUpgrade, activeBoosts: List<ActiveBoost>, now: Instant = Clock.System.now()): Float {
    val total = upgrade.effectiveTotalDuration(0)
    val remaining = upgrade.remainingSeconds(activeBoosts, now)
    return ((total - remaining) / total).coerceIn(0.0, 1.0).toFloat()
}

fun formatRemainingSeconds(remainingSeconds: Double): String {
    val total = remainingSeconds.toLong().coerceAtLeast(0L)
    if (total <= 0L) return "Complete"
    val days = total / 86_400
    val hours = (total % 86_400) / 3_600
    val minutes = (total % 3_600) / 60
    val seconds = total % 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

@Composable
fun SegmentedProgressBar(progress: Float, modifier: GlanceModifier = GlanceModifier, segments: Int = 10) {
    val filled = (progress * segments).roundToInt().coerceIn(0, segments)
    Row(modifier = modifier.fillMaxWidth()) {
        for (i in 0 until segments) {
            val color = if (i < filled) WidgetAccentGreen else WidgetMutedGray
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(4.dp)
                    .background(widgetColor(color))
            ) {}
        }
    }
}

@Composable
fun WidgetMessage(message: String) {
    Box(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
        Text(
            text = message,
            style = TextStyle(fontSize = 11.sp, color = widgetColor(WidgetMutedGray))
        )
    }
}

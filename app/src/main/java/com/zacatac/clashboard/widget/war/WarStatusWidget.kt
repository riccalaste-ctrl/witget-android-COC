package com.zacatac.clashboard.widget.war

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.fillMaxSize
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.zacatac.clashboard.data.database.AppDatabase
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.data.model.WarDetails
import com.zacatac.clashboard.util.WidgetConfigStore
import com.zacatac.clashboard.widget.WidgetAccentGreen
import com.zacatac.clashboard.widget.WidgetMutedGray
import com.zacatac.clashboard.widget.widgetColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.text.SimpleDateFormat
import java.util.Locale

class WarStatusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, glanceId: GlanceId) {
        val war = try {
            getWarForWidget(context)
        } catch (e: Throwable) {
            null
        }
        provideContent { WarStatusContent(war) }
    }

    private suspend fun getWarForWidget(context: Context): WarDetails? = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(context)
            val profiles = database.playerAccountDao().getAllProfiles().first()
            if (profiles.isEmpty()) return@withContext null
            null
        } catch (e: Throwable) {
            null
        }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            try {
                WarStatusWidget().updateAll(context)
            } catch (e: Throwable) {
                // Ignore widget update errors
            }
        }
    }
}

@Composable
fun WarStatusContent(war: WarDetails?) {
    val clan = war?.clan
    val opponent = war?.opponent

    if (war != null && clan != null && opponent != null) {
        val phase = warPhase(war)
        Column(
            modifier = GlanceModifier.fillMaxSize().background(com.zacatac.clashboard.widget.WidgetWhite).padding(12.dp),
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                Text(
                    text = clan.name ?: "Clan",
                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "${clan.stars ?: 0}-${opponent.stars ?: 0}",
                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            LinearProgressIndicator(
                progress = phase.progress.toFloat(),
                modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                color = ColorProvider(WidgetAccentGreen)
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = phase.label,
                style = TextStyle(fontSize = 9.sp, color = ColorProvider(WidgetMutedGray))
            )
        }
    } else {
        Box(
            modifier = GlanceModifier.fillMaxSize().background(com.zacatac.clashboard.widget.WidgetWhite).padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nessun dato di guerra",
                style = TextStyle(fontSize = 10.sp, color = ColorProvider(WidgetMutedGray))
            )
        }
    }
}

private data class WarPhaseInfo(val label: String, val progress: Double)

private fun warPhase(war: WarDetails): WarPhaseInfo {
    val now = Clock.System.now()
    val prepStart = parseWarDate(war.preparationStartTime)
    val start = parseWarDate(war.startTime)
    val end = parseWarDate(war.endTime)

    if (prepStart != null && start != null && now < start) {
        val total = (start - prepStart).inWholeSeconds.toDouble().coerceAtLeast(0.0)
        val progress = if (total > 0) (now - prepStart).inWholeSeconds.toDouble().coerceIn(0.0, total) / total else 0.0
        return WarPhaseInfo("Preparazione", progress)
    }
    if (start != null && end != null && now >= start && now < end) {
        val total = (end - start).inWholeSeconds.toDouble().coerceAtLeast(0.0)
        val progress = if (total > 0) (now - start).inWholeSeconds.toDouble().coerceIn(0.0, total) / total else 0.0
        return WarPhaseInfo("Battaglia", progress)
    }
    if (end != null && now >= end) {
        return WarPhaseInfo("Terminata", 1.0)
    }
    return WarPhaseInfo("Stato Guerra", 0.0)
}

private fun parseWarDate(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return try {
        val formatter = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
        formatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
        formatter.parse(value)?.toInstant()?.let { Instant.fromEpochMilliseconds(it.toEpochMilli()) }
    } catch (e: Exception) {
        null
    }
}

class WarStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WarStatusWidget()
}

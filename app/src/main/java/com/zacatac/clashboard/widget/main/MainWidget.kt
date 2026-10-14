package com.zacatac.clashboard.widget.main

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.zacatac.clashboard.data.model.ActiveBoost
import com.zacatac.clashboard.data.model.BuildingUpgrade
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.util.WidgetConfigStore
import com.zacatac.clashboard.widget.UpgradeImageProvider
import com.zacatac.clashboard.widget.WidgetAccentGreen
import com.zacatac.clashboard.widget.WidgetAccentOrange
import com.zacatac.clashboard.widget.WidgetCardBg
import com.zacatac.clashboard.widget.WidgetDarkText
import com.zacatac.clashboard.widget.WidgetSubText
import com.zacatac.clashboard.widget.WidgetWhite
import com.zacatac.clashboard.widget.emojiForCategory
import com.zacatac.clashboard.widget.selectWidgetProfile
import com.zacatac.clashboard.widget.upgradeProgressFor
import kotlinx.datetime.Clock

class MainWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, glanceId: GlanceId) {
        val profile = try {
            selectWidgetProfile(
                context = context,
                autoProfile = WidgetConfigStore.getMainWidgetAutoProfile(context),
                storedProfileId = WidgetConfigStore.getMainWidgetProfileId(context)
            )
        } catch (e: Throwable) {
            null
        }

        provideContent {
            MainWidgetContent(profile, UpgradeImageProvider(context))
        }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            try {
                MainWidget().updateAll(context)
            } catch (e: Throwable) {
                // Ignore widget update errors if provider is not present
            }
        }
    }
}

@Composable
fun MainWidgetContent(profile: PlayerAccount?, imageProvider: UpgradeImageProvider) {
    Column(
        modifier = GlanceModifier.fillMaxSize().background(WidgetWhite).padding(8.dp),
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        if (profile == null) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = "⚔️ Clashboard",
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WidgetDarkText)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Apri l'app e importa il JSON del tuo villaggio",
                    style = TextStyle(fontSize = 10.sp, color = WidgetSubText)
                )
            }
            return@Column
        }

        val now = Clock.System.now()
        val active = profile.activeUpgrades.filter { it.endTime > now }.sortedBy { it.endTime }

        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                text = profile.widgetDisplayName,
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WidgetDarkText),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "${active.size} attivi",
                style = TextStyle(fontSize = 10.sp, color = WidgetSubText)
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))

        if (active.isEmpty()) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = "Nessun upgrade in corso",
                    style = TextStyle(fontSize = 11.sp, color = WidgetSubText)
                )
            }
        } else {
            // Mostra fino a 5 upgrade per evitare il limite di RemoteViews in Glance
            val displayed = active.take(5)
            for (upgrade in displayed) {
                UpgradeCountdownRow(upgrade, profile.activeBoosts, imageProvider)
            }
            if (active.size > 5) {
                Text(
                    text = "+ altri ${active.size - 5} upgrade nell'app",
                    style = TextStyle(fontSize = 8.sp, color = WidgetSubText),
                    modifier = GlanceModifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun UpgradeCountdownRow(
    upgrade: BuildingUpgrade,
    activeBoosts: List<ActiveBoost>,
    imageProvider: UpgradeImageProvider
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(WidgetCardBg)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            val icon = imageProvider.providerForOrNull(upgrade.name)
            if (icon != null) {
                Image(
                    provider = icon,
                    contentDescription = upgrade.name,
                    modifier = GlanceModifier.size(20.dp)
                )
            } else {
                Text(text = emojiForCategory(upgrade.category), style = TextStyle(fontSize = 12.sp))
            }
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                text = upgrade.name,
                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = WidgetDarkText),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            val remaining = upgrade.remainingSeconds(activeBoosts, Clock.System.now())
            Text(
                text = formatCountdown(remaining),
                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ColorProvider(WidgetAccentOrange))
            )
        }
        LinearProgressIndicator(
            progress = upgradeProgressFor(upgrade, activeBoosts),
            modifier = GlanceModifier.fillMaxWidth().height(2.dp).padding(top = 2.dp),
            color = ColorProvider(WidgetAccentGreen)
        )
    }
}

fun formatCountdown(remainingSeconds: Double): String {
    val total = remainingSeconds.toLong().coerceAtLeast(0L)
    if (total <= 0L) return "Pronto"
    val days = total / 86_400
    val hours = (total % 86_400) / 3_600
    val minutes = (total % 3_600) / 60
    return when {
        days > 0 -> "${days}g ${hours}h ${minutes}m"
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}

class MainWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MainWidget()
}

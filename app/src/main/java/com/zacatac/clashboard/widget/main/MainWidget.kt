package com.zacatac.clashboard.widget.main

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.zacatac.clashboard.data.model.ActiveBoost
import com.zacatac.clashboard.data.model.BuildingUpgrade
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.data.model.UpgradeCategory
import com.zacatac.clashboard.util.WidgetConfigStore
import com.zacatac.clashboard.widget.WidgetAccentGreen
import com.zacatac.clashboard.widget.WidgetAccentOrange
import com.zacatac.clashboard.widget.WidgetMutedGray
import com.zacatac.clashboard.widget.UpgradeImageProvider
import com.zacatac.clashboard.widget.formatRemainingSeconds
import com.zacatac.clashboard.widget.selectWidgetProfile
import com.zacatac.clashboard.widget.upgradeProgressFor
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.max
import kotlin.math.min

class MainWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, glanceId: GlanceId) {
        val profile = selectWidgetProfile(
            context = context,
            autoProfile = WidgetConfigStore.getMainWidgetAutoProfile(context),
            storedProfileId = WidgetConfigStore.getMainWidgetProfileId(context)
        ) ?: return

        provideContent {
            MainWidgetContent(profile, UpgradeImageProvider(context))
        }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            MainWidget().updateAll(context)
        }
    }
}

@Composable
fun MainWidgetContent(profile: PlayerAccount, imageProvider: UpgradeImageProvider) {
    val builderUpgrades = profile.activeUpgrades
        .filter { it.category == UpgradeCategory.BUILDER_VILLAGE }
        .sortedBy { it.endTime }

    val builderCount = max(profile.builderCount, 1)
    val goblinActive = profile.activeUpgrades.any { it.category == UpgradeCategory.BUILDER_VILLAGE && it.usesGoblin }
    val displayCount = min(builderCount + if (goblinActive) 1 else 0, 6)

    val totalBuilderUpgrades = builderUpgrades.size
    val freeBuilders = max(displayCount - totalBuilderUpgrades, 0)
    val columnCount = if (displayCount == 4) 2 else 3

    val allBusy = totalBuilderUpgrades >= displayCount
    val statusText = if (allBusy) {
        "All Builders Busy"
    } else {
        val noun = if (freeBuilders == 1) "builder" else "builders"
        "$freeBuilders $noun free"
    }
    val profileName = profile.widgetDisplayName
    val fullText = if (profileName != "Clashboard") "$statusText • $profileName" else statusText

    Column(
        modifier = GlanceModifier.fillMaxWidth().padding(12.dp),
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        // Builder grid
        val cells = builderUpgrades.take(displayCount)
        for (row in 0 until ((cells.size + columnCount - 1) / columnCount)) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                for (col in 0 until columnCount) {
                    val index = row * columnCount + col
                    if (index < displayCount) {
                        Box(modifier = GlanceModifier.defaultWeight().padding(4.dp)) {
                            if (index < cells.size) {
                                builderCell(cells[index], profile.activeBoosts, imageProvider)
                            } else {
                                emptyBuilderCell(index + 1)
                            }
                        }
                    } else {
                        Box(modifier = GlanceModifier.defaultWeight()) {}
                    }
                }
            }
        }

        // Status line
        Box(
            modifier = GlanceModifier
                .padding(top = 2.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fullText,
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(if (allBusy) WidgetAccentGreen else WidgetAccentOrange)
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
fun builderCell(upgrade: BuildingUpgrade, activeBoosts: List<ActiveBoost>, imageProvider: UpgradeImageProvider) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        // Name + Supercharge icon
        Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Horizontal.Start) {
            Text(
                text = upgrade.name,
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            if (upgrade.showsSuperchargeIcon) {
                Text(text = "⚡", style = TextStyle(fontSize = 8.sp))
            }
        }

        // Icon + Level + Time
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Image(
                provider = imageProvider.providerFor(upgrade.name),
                contentDescription = upgrade.name,
                modifier = GlanceModifier.size(28.dp),
                contentScale = ContentScale.Fit
            )

            Column {
                Text(
                    text = upgrade.levelDisplayText,
                    style = TextStyle(fontSize = 8.sp, color = ColorProvider(WidgetMutedGray))
                )
                val remaining = upgrade.remainingSeconds(activeBoosts, Clock.System.now())
                Text(
                    text = formatRemainingSeconds(remaining),
                    style = TextStyle(fontSize = 9.sp, color = ColorProvider(WidgetAccentOrange))
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())
        }

        // Progress bar
        LinearProgressIndicator(
            progress = upgradeProgressFor(upgrade, activeBoosts),
            modifier = GlanceModifier.fillMaxWidth().height(4.dp),
            color = ColorProvider(WidgetAccentGreen)
        )
    }
}

@Composable
fun emptyBuilderCell(builderNumber: Int) {
    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        Text(
            text = "✓",
            style = TextStyle(fontSize = 20.sp, color = ColorProvider(WidgetAccentGreen))
        )
        Column {
            Text(
                text = "Builder $builderNumber",
                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ColorProvider(WidgetMutedGray))
            )
            Text(
                text = "Available",
                style = TextStyle(fontSize = 8.sp, color = ColorProvider(WidgetAccentGreen))
            )
        }
    }
}

class MainWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MainWidget()
}

package com.zacatac.clashboard.widget.closest

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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
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

class ClosestUpgradeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, glanceId: GlanceId) {
        val profile = selectWidgetProfile(
            context = context,
            autoProfile = WidgetConfigStore.getClosestWidgetAutoProfile(context),
            storedProfileId = WidgetConfigStore.getClosestWidgetProfileId(context)
        ) ?: return

        val upgradeType = WidgetConfigStore.getClosestWidgetUpgradeType(context)
        val upgrade = findClosestUpgrade(profile, upgradeType)

        provideContent {
            ClosestUpgradeContent(upgradeType, upgrade, UpgradeImageProvider(context))
        }
    }

    private fun findClosestUpgrade(profile: PlayerAccount, type: UpgradeCategory): BuildingUpgrade? {
        val upgrades = when (type) {
            UpgradeCategory.BUILDER_VILLAGE -> profile.builderUpgrades()
            UpgradeCategory.LAB -> profile.labUpgrades()
            UpgradeCategory.STAR_LAB -> profile.starLabUpgrades()
            UpgradeCategory.PETS -> profile.petUpgrades()
            UpgradeCategory.BUILDER_BASE -> profile.builderBaseUpgrades()
        }
        return upgrades.sortedBy { it.endTime }.firstOrNull()
    }

    companion object {
        suspend fun updateAll(context: Context) {
            ClosestUpgradeWidget().updateAll(context)
        }
    }
}

@Composable
fun ClosestUpgradeContent(
    upgradeType: UpgradeCategory,
    upgrade: BuildingUpgrade?,
    imageProvider: UpgradeImageProvider
) {
    if (upgrade != null) {
        Row(modifier = GlanceModifier.fillMaxWidth().padding(12.dp)) {
            Image(
                provider = imageProvider.providerFor(upgrade.name),
                contentDescription = upgrade.name,
                modifier = GlanceModifier.size(36.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            Column {
                Text(
                    text = upgradeType.displayName,
                    style = TextStyle(fontSize = 10.sp, color = ColorProvider(WidgetMutedGray))
                )
                Text(
                    text = upgrade.name,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                val remaining = upgrade.remainingSeconds(emptyList(), Clock.System.now())
                Text(
                    text = formatRemainingSeconds(remaining),
                    style = TextStyle(fontSize = 11.sp, color = ColorProvider(WidgetAccentOrange))
                )
                LinearProgressIndicator(
                    progress = upgradeProgressFor(upgrade, emptyList()),
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp).padding(top = 4.dp),
                    color = ColorProvider(WidgetAccentGreen)
                )
            }
        }
    } else {
        Column(
            modifier = GlanceModifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            Text(
                text = upgradeType.displayName,
                style = TextStyle(fontSize = 10.sp, color = ColorProvider(WidgetMutedGray))
            )
            Text(
                text = "No active upgrades",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

class ClosestUpgradeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClosestUpgradeWidget()
}

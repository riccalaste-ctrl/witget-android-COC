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
import androidx.glance.background
import androidx.glance.layout.fillMaxSize
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
        val profile = try {
            selectWidgetProfile(
                context = context,
                autoProfile = WidgetConfigStore.getClosestWidgetAutoProfile(context),
                storedProfileId = WidgetConfigStore.getClosestWidgetProfileId(context)
            )
        } catch (e: Throwable) {
            null
        }

        val upgradeType = try {
            WidgetConfigStore.getClosestWidgetUpgradeType(context)
        } catch (e: Throwable) {
            UpgradeCategory.BUILDER_VILLAGE
        }

        val upgrade = if (profile != null) findClosestUpgrade(profile, upgradeType) else null

        provideContent {
            ClosestUpgradeContent(upgradeType, upgrade, UpgradeImageProvider(context))
        }
    }

    private fun findClosestUpgrade(profile: PlayerAccount, type: UpgradeCategory): BuildingUpgrade? {
        val now = Clock.System.now()
        val upgrades = when (type) {
            UpgradeCategory.BUILDER_VILLAGE -> profile.builderUpgrades()
            UpgradeCategory.LAB -> profile.labUpgrades()
            UpgradeCategory.STAR_LAB -> profile.starLabUpgrades()
            UpgradeCategory.PETS -> profile.petUpgrades()
            UpgradeCategory.BUILDER_BASE -> profile.builderBaseUpgrades()
        }
        return upgrades.filter { it.endTime > now }.minByOrNull { it.endTime }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            try {
                ClosestUpgradeWidget().updateAll(context)
            } catch (e: Throwable) {
                // Ignore widget update errors
            }
        }
    }
}

@Composable
fun ClosestUpgradeContent(
    category: UpgradeCategory,
    upgrade: BuildingUpgrade?,
    imageProvider: UpgradeImageProvider
) {
    if (upgrade != null) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(com.zacatac.clashboard.widget.WidgetWhite)
                .padding(10.dp)
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                val icon = imageProvider.providerForOrNull(upgrade.name)
                if (icon != null) {
                    Image(
                        provider = icon,
                        contentDescription = upgrade.name,
                        modifier = GlanceModifier.size(28.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = com.zacatac.clashboard.widget.emojiForCategory(upgrade.category),
                        style = TextStyle(fontSize = 18.sp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = upgrade.name,
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = com.zacatac.clashboard.widget.WidgetDarkText),
                        maxLines = 1
                    )
                    Text(
                        text = upgrade.levelDisplayText,
                        style = TextStyle(fontSize = 9.sp, color = com.zacatac.clashboard.widget.WidgetSubText)
                    )
                }

                val remaining = upgrade.remainingSeconds(emptyList(), Clock.System.now())
                Text(
                    text = com.zacatac.clashboard.widget.main.formatCountdown(remaining),
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(WidgetAccentOrange)
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            LinearProgressIndicator(
                progress = upgradeProgressFor(upgrade, emptyList()),
                modifier = GlanceModifier.fillMaxWidth().height(3.dp),
                color = ColorProvider(WidgetAccentGreen)
            )
        }
    } else {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(com.zacatac.clashboard.widget.WidgetWhite)
                .padding(10.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = category.displayName,
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = com.zacatac.clashboard.widget.WidgetDarkText)
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "Nessun upgrade attivo",
                style = TextStyle(fontSize = 10.sp, color = com.zacatac.clashboard.widget.WidgetSubText)
            )
        }
    }
}

class ClosestUpgradeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClosestUpgradeWidget()
}

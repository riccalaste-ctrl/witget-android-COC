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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
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
import com.zacatac.clashboard.data.model.BuildingUpgrade
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.data.model.UpgradeCategory
import com.zacatac.clashboard.util.WidgetConfigStore
import com.zacatac.clashboard.widget.UpgradeImageProvider
import com.zacatac.clashboard.widget.WidgetAccentGreen
import com.zacatac.clashboard.widget.WidgetAccentOrange
import com.zacatac.clashboard.widget.WidgetDarkText
import com.zacatac.clashboard.widget.WidgetSubText
import com.zacatac.clashboard.widget.WidgetWhite
import com.zacatac.clashboard.widget.emojiForCategory
import com.zacatac.clashboard.widget.main.formatCountdown
import com.zacatac.clashboard.widget.main.hasReliableProgress
import com.zacatac.clashboard.widget.selectWidgetProfile
import com.zacatac.clashboard.widget.upgradeProgressFor
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/** What the closest widget shows for the selected category. */
sealed interface ClosestState {
    /** Village category: builder slots. [nextFreeInSeconds] is 0 when a builder is free now. */
    data class BuilderSlots(val busy: Int, val total: Int, val nextFreeInSeconds: Double) : ClosestState

    /** Other categories: the next upgrade to finish. [progress] is null when no reliable base duration is known. */
    data class NextUpgrade(val upgrade: BuildingUpgrade, val remainingSeconds: Double, val progress: Float?) : ClosestState

    object Empty : ClosestState
}

/** Pure: computes the widget state from the profile, the category and the instant. */
fun closestWidgetState(profile: PlayerAccount, category: UpgradeCategory, now: Instant): ClosestState {
    if (category == UpgradeCategory.BUILDER_VILLAGE) {
        val busy = profile.builderUpgrades().filter { it.endTime > now }
        val total = profile.builderCount.coerceAtLeast(1)
        val nextFree = if (busy.size < total) {
            0.0
        } else {
            busy.minOf { it.remainingSeconds(profile.activeBoosts, now) }
        }
        return ClosestState.BuilderSlots(minOf(busy.size, total), total, nextFree)
    }

    val upgrades = when (category) {
        UpgradeCategory.BUILDER_VILLAGE -> emptyList()
        UpgradeCategory.LAB -> profile.labUpgrades()
        UpgradeCategory.STAR_LAB -> profile.starLabUpgrades()
        UpgradeCategory.PETS -> profile.petUpgrades()
        UpgradeCategory.BUILDER_BASE -> profile.builderBaseUpgrades()
    }
    val next = upgrades.filter { it.endTime > now }.minByOrNull { it.endTime } ?: return ClosestState.Empty
    val remaining = next.remainingSeconds(profile.activeBoosts, now)
    val progress = if (hasReliableProgress(next)) upgradeProgressFor(next, profile.activeBoosts, now) else null
    return ClosestState.NextUpgrade(next, remaining, progress)
}

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

        val category = try {
            WidgetConfigStore.getClosestWidgetUpgradeType(context)
        } catch (e: Throwable) {
            UpgradeCategory.BUILDER_VILLAGE
        }

        val state = if (profile != null) {
            closestWidgetState(profile, category, Clock.System.now())
        } else {
            ClosestState.Empty
        }

        provideContent {
            ClosestUpgradeContent(category, state, UpgradeImageProvider(context))
        }
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
    state: ClosestState,
    imageProvider: UpgradeImageProvider
) {
    when (state) {
        is ClosestState.BuilderSlots -> Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetWhite)
                .cornerRadius(20.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "Costruttori",
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WidgetDarkText)
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "${state.busy}/${state.total} occupati",
                style = TextStyle(fontSize = 10.sp, color = WidgetSubText)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            if (state.nextFreeInSeconds <= 0.0) {
                Text(
                    text = "${state.total - state.busy} liberi ora",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorProvider(WidgetAccentGreen))
                )
            } else {
                Text(
                    text = "Prossimo libero: ${formatCountdown(state.nextFreeInSeconds)}",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorProvider(WidgetAccentOrange))
                )
            }
        }

        is ClosestState.NextUpgrade -> Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetWhite)
                .cornerRadius(20.dp)
                .padding(12.dp)
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                val icon = imageProvider.providerForOrNull(state.upgrade.name)
                if (icon != null) {
                    Image(
                        provider = icon,
                        contentDescription = state.upgrade.name,
                        modifier = GlanceModifier.size(28.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = emojiForCategory(state.upgrade.category),
                        style = TextStyle(fontSize = 18.sp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = state.upgrade.name,
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WidgetDarkText),
                        maxLines = 1
                    )
                    Text(
                        text = state.upgrade.levelDisplayText,
                        style = TextStyle(fontSize = 9.sp, color = WidgetSubText)
                    )
                }

                Text(
                    text = formatCountdown(state.remainingSeconds),
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(WidgetAccentOrange)
                    )
                )
            }

            val progress = state.progress
            if (progress != null) {
                Spacer(modifier = GlanceModifier.height(6.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                    color = ColorProvider(WidgetAccentGreen)
                )
            }
        }

        is ClosestState.Empty -> Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetWhite)
                .cornerRadius(20.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = category.displayName,
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WidgetDarkText)
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "Nessun upgrade attivo",
                style = TextStyle(fontSize = 10.sp, color = WidgetSubText)
            )
        }
    }
}

class ClosestUpgradeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClosestUpgradeWidget()
}

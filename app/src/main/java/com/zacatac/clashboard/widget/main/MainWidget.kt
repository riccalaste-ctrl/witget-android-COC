package com.zacatac.clashboard.widget.main

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
import com.zacatac.clashboard.data.model.UpgradeCategory
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
import kotlinx.datetime.Instant

/** Height below which the widget switches to the compact layout. */
private val COMPACT_HEIGHT_THRESHOLD = 220.dp

/**
 * Builders panel summary, computed from the stored profile.
 * Busy counts are capped to the builder total; free = total - busy.
 */
data class MainWidgetSummary(
    val villageBusy: Int,
    val villageTotal: Int,
    val baseBusy: Int,
    val starLabBusy: Boolean,
    val labBusy: Boolean,
    val petBusy: Boolean,
    val rows: List<BuildingUpgrade>
) {
    val villageFree: Int get() = (villageTotal - villageBusy).coerceAtLeast(0)
}

/** Pure function (no Android types) so it can be unit-tested. */
fun buildMainWidgetSummary(profile: PlayerAccount, now: Instant): MainWidgetSummary {
    val active = profile.activeUpgrades.filter { it.endTime > now }.sortedBy { it.endTime }
    val counts = active.groupingBy { it.category }.eachCount()
    val villageTotal = profile.builderCount.coerceAtLeast(1)
    return MainWidgetSummary(
        villageBusy = minOf(counts[UpgradeCategory.BUILDER_VILLAGE] ?: 0, villageTotal),
        villageTotal = villageTotal,
        baseBusy = counts[UpgradeCategory.BUILDER_BASE] ?: 0,
        starLabBusy = (counts[UpgradeCategory.STAR_LAB] ?: 0) > 0,
        labBusy = (counts[UpgradeCategory.LAB] ?: 0) > 0,
        petBusy = (counts[UpgradeCategory.PETS] ?: 0) > 0,
        rows = active
    )
}

/**
 * A progress bar is only meaningful when the base duration is known. Items without a base
 * duration store totalDuration == sourceExportTimer (remaining time at import), so their bar
 * would always start from 0%.
 */
fun hasReliableProgress(upgrade: BuildingUpgrade): Boolean =
    upgrade.totalDuration > (upgrade.sourceExportTimer ?: 0).toDouble()

class MainWidget : GlanceAppWidget() {

    // Exact sizing: LocalSize gives the real widget size, so the layout can adapt to it.
    override val sizeMode: SizeMode = SizeMode.Exact

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
    val compact = LocalSize.current.height < COMPACT_HEIGHT_THRESHOLD

    Column(
        modifier = GlanceModifier.fillMaxSize().background(WidgetWhite).cornerRadius(20.dp).padding(10.dp),
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
        val summary = buildMainWidgetSummary(profile, now)

        Text(
            text = profile.widgetDisplayName,
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WidgetDarkText),
            maxLines = 1
        )
        BuildersSummary(summary, compact)
        Spacer(modifier = GlanceModifier.height(4.dp))

        if (summary.rows.isEmpty()) {
            Text(
                text = "Nessun upgrade in corso",
                style = TextStyle(fontSize = 11.sp, color = WidgetSubText)
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                items(summary.rows, itemId = { it.id.hashCode().toLong() }) { upgrade ->
                    UpgradeCountdownRow(upgrade, profile.activeBoosts, imageProvider, now, compact)
                }
            }
        }
    }
}

@Composable
fun BuildersSummary(summary: MainWidgetSummary, compact: Boolean) {
    val free = summary.villageFree
    Row(
        modifier = GlanceModifier
            .padding(bottom = 2.dp)
            .background(WidgetCardBg)
            .cornerRadius(10.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = "Villaggio ${summary.villageBusy}/${summary.villageTotal}",
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WidgetDarkText)
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = if (free > 0) "$free liberi" else "tutti occupati",
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(if (free > 0) WidgetAccentOrange else WidgetAccentGreen)
            )
        )
    }
    if (!compact) {
        Text(
            text = "Base: ${summary.baseBusy} in corso · Star Lab: ${busyLabel(summary.starLabBusy)} · " +
                "Lab: ${busyLabel(summary.labBusy)} · Pet: ${busyLabel(summary.petBusy)}",
            style = TextStyle(fontSize = 8.sp, color = WidgetSubText),
            maxLines = 1
        )
    }
}

private fun busyLabel(busy: Boolean): String = if (busy) "in corso" else "libero"

private fun categoryShortLabel(category: UpgradeCategory): String = when (category) {
    UpgradeCategory.BUILDER_VILLAGE -> "Villaggio"
    UpgradeCategory.BUILDER_BASE -> "Base"
    UpgradeCategory.STAR_LAB -> "Star Lab"
    UpgradeCategory.LAB -> "Lab"
    UpgradeCategory.PETS -> "Pet"
}

@Composable
fun UpgradeCountdownRow(
    upgrade: BuildingUpgrade,
    activeBoosts: List<ActiveBoost>,
    imageProvider: UpgradeImageProvider,
    now: Instant,
    compact: Boolean
) {
    val remaining = upgrade.remainingSeconds(activeBoosts, now)
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(WidgetCardBg)
            .cornerRadius(12.dp)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            val icon = imageProvider.providerForOrNull(upgrade.name)
            if (icon != null) {
                Image(
                    provider = icon,
                    contentDescription = upgrade.name,
                    modifier = GlanceModifier.size(22.dp)
                )
            } else {
                Text(text = emojiForCategory(upgrade.category), style = TextStyle(fontSize = 12.sp))
            }
            Spacer(modifier = GlanceModifier.width(4.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = upgrade.name,
                    style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = WidgetDarkText),
                    maxLines = 1
                )
                if (!compact) {
                    Text(
                        text = "${categoryShortLabel(upgrade.category)} · ${upgrade.levelDisplayText}",
                        style = TextStyle(fontSize = 8.sp, color = WidgetSubText),
                        maxLines = 1
                    )
                }
            }
            Text(
                text = formatCountdown(remaining),
                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ColorProvider(WidgetAccentOrange))
            )
        }
        if (!compact && hasReliableProgress(upgrade)) {
            LinearProgressIndicator(
                progress = upgradeProgressFor(upgrade, activeBoosts, now),
                modifier = GlanceModifier.fillMaxWidth().height(3.dp).padding(top = 4.dp),
                color = ColorProvider(WidgetAccentGreen)
            )
        }
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

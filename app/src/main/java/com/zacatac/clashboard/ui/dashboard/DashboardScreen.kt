package com.zacatac.clashboard.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zacatac.clashboard.data.model.ActiveBoost
import com.zacatac.clashboard.data.model.BuildingUpgrade
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.data.model.UpgradeCategory
import com.zacatac.clashboard.ui.MainViewModel
import com.zacatac.clashboard.util.ProfileSelection
import com.zacatac.clashboard.util.resolveSelectedProfile
import com.zacatac.clashboard.widget.UpgradeImageProvider
import com.zacatac.clashboard.widget.emojiForCategory
import com.zacatac.clashboard.widget.main.hasReliableProgress
import com.zacatac.clashboard.widget.upgradeProgressFor
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours

/** Import age after which the dashboard asks the user to re-import the village. */
private val STALE_IMPORT_AGE = 12.hours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onProfileClick: (String) -> Unit,
    onImportClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val profiles by viewModel.profiles.collectAsState()
    val selectedProfile = resolveSelectedProfile(profiles, ProfileSelection.storedId(context))

    // Ticking clock: refreshes the countdowns every second
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            tick++
        }
    }
    @Suppress("UNUSED_VARIABLE")
    val currentTick = tick
    val now: Instant = Clock.System.now()
    val imageProvider = remember(context) { UpgradeImageProvider(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopAppBar(
            title = {
                Text(
                    selectedProfile?.widgetDisplayName ?: "Clashboard",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            },
            actions = {
                IconButton(onClick = onImportClick) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Importa JSON",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Impostazioni",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        if (selectedProfile == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Nessun profilo importato",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Tocca il pulsante qui sotto per incollare il file JSON del tuo villaggio",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onImportClick,
                    modifier = Modifier.padding(top = 20.dp)
                ) {
                    Text("Importa Dati Villaggio")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ImportAgeNotice(selectedProfile, now)
                }
                items(UpgradeCategory.entries) { category ->
                    // Completed upgrades are hidden: only running ones are shown
                    val upgrades = selectedProfile.activeUpgrades
                        .filter { it.category == category && it.endTime > now }
                        .sortedBy { it.endTime }

                    CategorySection(
                        category = category,
                        upgrades = upgrades,
                        activeBoosts = selectedProfile.activeBoosts,
                        now = now,
                        imageProvider = imageProvider
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun ImportAgeNotice(profile: PlayerAccount, now: Instant) {
    val lastImport = profile.lastImportDate ?: return
    val stale = (now - lastImport) >= STALE_IMPORT_AGE
    val local = lastImport.toLocalDateTime(TimeZone.currentSystemDefault())
    val stamp = "%02d/%02d %02d:%02d".format(
        local.dayOfMonth, local.monthNumber, local.hour, local.minute
    )
    Text(
        text = if (stale) {
            "Dati importati il $stamp: sono vecchi, reimporta il JSON per aggiornare i timer."
        } else {
            "Dati importati il $stamp"
        },
        fontSize = 12.sp,
        color = if (stale) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
fun CategorySection(
    category: UpgradeCategory,
    upgrades: List<BuildingUpgrade>,
    activeBoosts: List<ActiveBoost>,
    now: Instant,
    imageProvider: UpgradeImageProvider,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        emojiForCategory(category),
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        category.displayName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (upgrades.isNotEmpty()) {
                    Text(
                        "${upgrades.size} in corso",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (upgrades.isEmpty()) {
                Text(
                    "Nessun upgrade in corso",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    upgrades.forEach { upgrade ->
                        UpgradeRow(upgrade = upgrade, activeBoosts = activeBoosts, now = now, imageProvider = imageProvider)
                    }
                }
            }
        }
    }
}

@Composable
fun UpgradeRow(
    upgrade: BuildingUpgrade,
    activeBoosts: List<ActiveBoost>,
    now: Instant,
    imageProvider: UpgradeImageProvider
) {
    val drawableResId = imageProvider.resIdFor(upgrade.name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (drawableResId != 0) {
                Image(
                    painter = painterResource(id = drawableResId),
                    contentDescription = upgrade.name,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emojiForCategory(upgrade.category), fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    upgrade.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    upgrade.levelDisplayText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(130.dp)
        ) {
            Text(
                upgrade.timeRemainingText(activeBoosts, now),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            // Bar only when the base duration is known, same rule as the widgets
            if (hasReliableProgress(upgrade)) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { upgradeProgressFor(upgrade, activeBoosts, now) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

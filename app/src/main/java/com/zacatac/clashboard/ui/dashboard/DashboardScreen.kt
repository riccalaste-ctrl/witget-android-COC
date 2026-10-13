package com.zacatac.clashboard.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zacatac.clashboard.data.model.BuildingUpgrade
import com.zacatac.clashboard.data.model.UpgradeCategory
import com.zacatac.clashboard.ui.MainViewModel
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onProfileClick: (String) -> Unit,
    onImportClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsState()
    val selectedProfile = profiles.firstOrNull()

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopAppBar(
            title = { Text("Clashboard", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = onImportClick) {
                    Icon(Icons.Default.Add, contentDescription = "Import")
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        if (selectedProfile == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No profiles yet", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Tap + to import your village data", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onImportClick, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Import Data")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(UpgradeCategory.entries) { category ->
                    val upgrades = selectedProfile.activeUpgrades
                        .filter { it.category == category }
                        .sortedBy { it.endTime }

                    CategorySection(category = category, upgrades = upgrades)
                }
            }
        }
    }
}

@Composable
fun CategorySection(
    category: UpgradeCategory,
    upgrades: List<BuildingUpgrade>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(category.displayName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (upgrades.isNotEmpty()) {
                    Text(
                        "${upgrades.size} active",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (upgrades.isEmpty()) {
                Text("No active upgrades", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    upgrades.forEach { upgrade ->
                        UpgradeRow(upgrade = upgrade)
                    }
                }
            }
        }
    }
}

@Composable
fun UpgradeRow(upgrade: BuildingUpgrade) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(upgrade.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                upgrade.levelDisplayText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                upgrade.timeRemaining(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            LinearProgressIndicator(
                progress = { calculateProgress(upgrade) },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun calculateProgress(upgrade: BuildingUpgrade): Float {
    val now = Clock.System.now()
    val total = upgrade.effectiveTotalDuration(0)
    if (total <= 0.0) return 1f
    val remaining = upgrade.remainingSeconds(emptyList(), now)
    return ((total - remaining) / total).coerceIn(0.0, 1.0).toFloat()
}

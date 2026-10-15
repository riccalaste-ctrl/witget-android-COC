@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zacatac.clashboard.ui.notifications

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zacatac.clashboard.data.database.AppDatabase
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.ui.MainViewModel
import com.zacatac.clashboard.util.ProfileSelection
import com.zacatac.clashboard.util.resolveSelectedProfile
import com.zacatac.clashboard.widget.UpgradeAlarmScheduler
import kotlinx.coroutines.launch

private val OFFSET_CHOICES = listOf(0, 1, 5, 10)

@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val profiles by viewModel.profiles.collectAsState()
    val profile = resolveSelectedProfile(profiles, ProfileSelection.storedId(context))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    "Notifiche",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Indietro",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        if (profile == null) {
            Text(
                "Importa un villaggio per configurare le notifiche.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            // Re-create the form state when the selected village changes
            key(profile.id) {
                NotificationsForm(profile)
            }
        }
    }
}

@Composable
private fun NotificationsForm(profile: PlayerAccount) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initial = profile.notificationSettings

    var notificationsEnabled by remember { mutableStateOf(initial.notificationsEnabled) }
    var builderNotifications by remember { mutableStateOf(initial.builderNotificationsEnabled) }
    var labNotifications by remember { mutableStateOf(initial.labNotificationsEnabled) }
    var petNotifications by remember { mutableStateOf(initial.petNotificationsEnabled) }
    var builderBaseNotifications by remember { mutableStateOf(initial.builderBaseNotificationsEnabled) }
    var offsetMinutes by remember { mutableIntStateOf(initial.notificationOffsetMinutes) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Villaggio: ${profile.widgetDisplayName}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Notifiche Globali", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Avviso alla fine di upgrade e boost",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
            }
        }

        Text(
            "Categorie notificate",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NotificationToggle("Edifici Villaggio Principale", builderNotifications) { builderNotifications = it }
                NotificationToggle("Laboratorio & Incantesimi", labNotifications) { labNotifications = it }
                NotificationToggle("Casa dei Pets", petNotifications) { petNotifications = it }
                NotificationToggle("Base del Costruttore & Star Lab", builderBaseNotifications) { builderBaseNotifications = it }
            }
        }

        Text(
            "Anticipo della notifica",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OFFSET_CHOICES.forEach { minutes ->
                val label = if (minutes == 0) "Alla scadenza" else "$minutes min prima"
                if (minutes == offsetMinutes) {
                    Button(onClick = { offsetMinutes = minutes }) { Text(label, fontSize = 12.sp) }
                } else {
                    OutlinedButton(onClick = { offsetMinutes = minutes }) { Text(label, fontSize = 12.sp) }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = {
                scope.launch {
                    // Keep the settings the form does not edit (helper and war switches)
                    val updated = profile.copy(
                        notificationSettings = initial.copy(
                            notificationsEnabled = notificationsEnabled,
                            builderNotificationsEnabled = builderNotifications,
                            labNotificationsEnabled = labNotifications,
                            petNotificationsEnabled = petNotifications,
                            builderBaseNotificationsEnabled = builderBaseNotifications,
                            notificationOffsetMinutes = offsetMinutes
                        )
                    )
                    AppDatabase.getDatabase(context).playerAccountDao().updateProfile(updated)
                    UpgradeAlarmScheduler.scheduleForProfile(context, updated)
                    Toast.makeText(context, "Preferenze salvate e allarmi aggiornati", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salva Preferenze Notifiche")
        }
    }
}

@Composable
fun NotificationToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

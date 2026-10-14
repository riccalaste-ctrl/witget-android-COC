@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zacatac.clashboard.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zacatac.clashboard.data.database.AppDatabase
import com.zacatac.clashboard.widget.UpgradeAlarmScheduler
import com.zacatac.clashboard.widget.WidgetTickReceiver
import com.zacatac.clashboard.widget.WidgetTickScheduler
import com.zacatac.clashboard.widget.WidgetUpdateWorker
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    onNavigateToImport: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfiles: () -> Unit = {},
    onNavigateToBoosts: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    "Impostazioni",
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sezione Dati Villaggio
            SettingsGroup(title = "Dati del Villaggio") {
                SettingsActionRow(
                    icon = Icons.Default.Upload,
                    title = "Importa JSON Villaggio",
                    subtitle = "Incolla il testo esportato da Clash of Clans",
                    onClick = onNavigateToImport
                )
                SettingsActionRow(
                    icon = Icons.Default.Person,
                    title = "Gestione Profili",
                    subtitle = "Visualizza o elimina i profili salvati",
                    onClick = onNavigateToProfiles
                )
            }

            // Sezione Widget & Notifiche
            SettingsGroup(title = "Widget & Notifiche") {
                SettingsActionRow(
                    icon = Icons.Default.Refresh,
                    title = "Forza Aggiornamento Widget",
                    subtitle = "Ricalcola i timer e aggiorna la schermata home adesso",
                    onClick = {
                        scope.launch {
                            WidgetTickReceiver.updateAllWidgets(context)
                            WidgetTickScheduler.start(context)
                            WidgetUpdateWorker.scheduleImmediateUpdate(context)
                            Toast.makeText(context, "Widget aggiornati!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                SettingsActionRow(
                    icon = Icons.Default.Notifications,
                    title = "Impostazioni Notifiche",
                    subtitle = "Scegli quali upgrade notificano alla scadenza",
                    onClick = onNavigateToNotifications
                )
                SettingsActionRow(
                    icon = Icons.Default.Warning,
                    title = "Permessi Sveglie & Allarmi",
                    subtitle = "Apri impostazioni di sistema per allarmi precisi al minuto",
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }

            // Sezione Manutenzione & Reset
            SettingsGroup(title = "Manutenzione") {
                SettingsActionRow(
                    icon = Icons.Default.Refresh,
                    title = "Ripristina Allarmi",
                    subtitle = "Riprogramma le notifiche di fine upgrade",
                    onClick = {
                        scope.launch {
                            UpgradeAlarmScheduler.scheduleForAllProfiles(context)
                            Toast.makeText(context, "Allarmi riprogrammati!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                SettingsActionRow(
                    icon = Icons.Default.Warning,
                    title = "Cancella Tutti i Dati",
                    subtitle = "Rimuove profili e upgrade locali dal database",
                    isDestructive = true,
                    onClick = {
                        scope.launch {
                            AppDatabase.getDatabase(context).clearAllTables()
                            WidgetTickReceiver.updateAllWidgets(context)
                            Toast.makeText(context, "Dati rimossi. Riapri l'app e importa il JSON.", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }

            // Info versione
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text("Clashboard Android v1.1.0", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Replica nativa con widget live e countdown preciso", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        val titleColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.padding(end = 14.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = titleColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

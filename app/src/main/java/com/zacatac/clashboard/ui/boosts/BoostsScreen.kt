@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zacatac.clashboard.ui.boosts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zacatac.clashboard.data.database.AppDatabase
import com.zacatac.clashboard.data.model.ActiveBoost
import com.zacatac.clashboard.data.model.BoostType
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.ui.MainViewModel
import com.zacatac.clashboard.util.ProfileSelection
import com.zacatac.clashboard.util.resolveSelectedProfile
import com.zacatac.clashboard.widget.UpgradeAlarmScheduler
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

@Composable
fun BoostsScreen(
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
                    "Boost & Configurazione",
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
                "Importa un villaggio per gestire boost e configurazione.",
                fontSize = 14.sp,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            key(profile.id) {
                BoostsContent(profile)
            }
        }
    }
}

@Composable
private fun BoostsContent(profile: PlayerAccount) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val now = Clock.System.now()
    val running = profile.activeBoosts.filter { it.endTime > now }.sortedBy { it.endTime }

    fun save(updated: PlayerAccount) {
        scope.launch {
            AppDatabase.getDatabase(context).playerAccountDao().updateProfile(updated)
            UpgradeAlarmScheduler.scheduleForProfile(context, updated)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Boost attivi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Il JSON di Clash of Clans non contiene le pozioni attive: registrale qui quando le attivi nel gioco. " +
                        "Vengono conteggiate solo per gli upgrade avviati dopo l'export.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (running.isEmpty()) {
                    Text("Nessun boost registrato", fontSize = 13.sp)
                }
                running.forEach { boost ->
                    val type = boost.boostType
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${type?.displayName ?: boost.type} · fino alle ${boost.endTime.toString().take(16).replace('T', ' ')} UTC",
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = {
                            save(profile.copy(activeBoosts = profile.activeBoosts.filter { it.id != boost.id }))
                        }) {
                            Text("Rimuovi", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Avvia un boost adesso", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Premi quando attivi la pozione nel gioco. La durata è quella standard del boost.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BoostType.entries.forEach { type ->
                    val level = levelFor(type, profile)
                    val usable = level == null || level > 0
                    OutlinedButton(
                        enabled = usable,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val started = Clock.System.now()
                            val boost = ActiveBoost(
                                type = type.id,
                                startTime = started,
                                endTime = started + type.durationSeconds(profile.clockTowerLevel).seconds,
                                helperLevel = level
                            )
                            save(profile.copy(activeBoosts = profile.activeBoosts.filter { it.endTime > started } + boost))
                        }
                    ) {
                        val suffix = if (usable) "" else " (livello 0)"
                        Text("${type.displayName}$suffix", fontSize = 13.sp)
                    }
                }
            }
        }

        SettingsCard(profile = profile, onSave = { save(it) })
    }
}

/** Level used by level-based boosts; null for boosts that do not depend on a helper level. */
private fun levelFor(type: BoostType, profile: PlayerAccount): Int? = when (type) {
    BoostType.BUILDER_APPRENTICE -> profile.builderApprenticeLevel
    BoostType.LAB_ASSISTANT -> profile.labAssistantLevel
    BoostType.CLOCK_TOWER, BoostType.CLOCK_TOWER_POTION -> profile.clockTowerLevel
    else -> null
}

@Composable
private fun SettingsCard(profile: PlayerAccount, onSave: (PlayerAccount) -> Unit) {
    var builderCount by remember { mutableStateOf(profile.builderCount.toString()) }
    var goldPass by remember { mutableStateOf(profile.goldPassBoost.toString()) }
    var apprentice by remember { mutableStateOf(profile.builderApprenticeLevel.toString()) }
    var labAssistant by remember { mutableStateOf(profile.labAssistantLevel.toString()) }
    var alchemist by remember { mutableStateOf(profile.alchemistLevel.toString()) }
    var clockTower by remember { mutableStateOf(profile.clockTowerLevel.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Configurazione villaggio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Valori non presenti nel JSON: inseriscili a mano.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NumberField("Costruttori del villaggio", builderCount) { builderCount = it }
            NumberField("Gold Pass: % di riduzione (0-100)", goldPass) { goldPass = it }
            NumberField("Livello Apprendista Costruttore", apprentice) { apprentice = it }
            NumberField("Livello Assistente Laboratorio", labAssistant) { labAssistant = it }
            NumberField("Livello Alchimista", alchemist) { alchemist = it }
            NumberField("Livello Torre dell'Orologio", clockTower) { clockTower = it }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onSave(
                        profile.copy(
                            builderCount = builderCount.toIntOrNull()?.coerceIn(1, 10) ?: profile.builderCount,
                            goldPassBoost = goldPass.toIntOrNull()?.coerceIn(0, 100) ?: profile.goldPassBoost,
                            builderApprenticeLevel = apprentice.toIntOrNull()?.coerceAtLeast(0) ?: profile.builderApprenticeLevel,
                            labAssistantLevel = labAssistant.toIntOrNull()?.coerceAtLeast(0) ?: profile.labAssistantLevel,
                            alchemistLevel = alchemist.toIntOrNull()?.coerceAtLeast(0) ?: profile.alchemistLevel,
                            clockTowerLevel = clockTower.toIntOrNull()?.coerceAtLeast(0) ?: profile.clockTowerLevel
                        )
                    )
                }
            ) {
                Text("Salva configurazione")
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { ch -> ch.isDigit() }) },
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

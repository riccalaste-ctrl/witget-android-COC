@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zacatac.clashboard.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotificationsScreen(modifier: androidx.compose.ui.Modifier = Modifier) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var builderNotifications by remember { mutableStateOf(true) }
    var labNotifications by remember { mutableStateOf(true) }
    var petNotifications by remember { mutableStateOf(true) }
    var builderBaseNotifications by remember { mutableStateOf(true) }
    var helperNotifications by remember { mutableStateOf(true) }
    var warNotifications by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopAppBar(
            title = { Text("Notifications", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer)
        )

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable Notifications", fontSize = 16.sp)
                Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
            }

            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
                NotificationToggle("Builder Upgrades", builderNotifications) { builderNotifications = it }
                NotificationToggle("Lab Upgrades", labNotifications) { labNotifications = it }
                NotificationToggle("Pet Upgrades", petNotifications) { petNotifications = it }
                NotificationToggle("Builder Base Upgrades", builderBaseNotifications) { builderBaseNotifications = it }
                NotificationToggle("Helper Ready", helperNotifications) { helperNotifications = it }
                NotificationToggle("Clan War Alerts", warNotifications) { warNotifications = it }
            }
        }
    }
}

@Composable
fun NotificationToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 16.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
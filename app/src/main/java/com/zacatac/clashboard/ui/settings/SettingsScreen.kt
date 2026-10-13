@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zacatac.clashboard.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(modifier: androidx.compose.ui.Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopAppBar(
            title = { Text("Settings", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer)
        )

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Settings screen - TODO: Implement", fontSize = 16.sp)
            
            Button(onClick = { /* Backup */ }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text("Backup Data")
            }
            
            Button(onClick = { /* Restore */ }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Restore Data")
            }
        }
    }
}
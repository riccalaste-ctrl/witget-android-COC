@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zacatac.clashboard.ui.boosts

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
fun BoostsScreen(modifier: androidx.compose.ui.Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopAppBar(
            title = { Text("Active Boosts", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer)
        )

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Boosts screen - TODO: Implement boost management", fontSize = 16.sp)
            
            Button(onClick = { /* Add boost */ }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text("Add Boost")
            }
        }
    }
}
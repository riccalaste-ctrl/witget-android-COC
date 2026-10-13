@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zacatac.clashboard.ui.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import com.zacatac.clashboard.data.model.PlayerAccount

@Composable
fun ProfileDetailScreen(
    profileId: String,
    modifier: androidx.compose.ui.Modifier = Modifier
) {
    // TODO: Load profile from ViewModel
    val profile = remember { mutableStateOf<PlayerAccount?>(null) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopAppBar(
            title = { Text("Profile Details", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer)
        )

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Profile ID: $profileId", fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            
            // Profile form fields
            ProfileField("Profile Name", "My Village")
            ProfileField("Player Tag", "#ABC123DEF")
            ProfileField("Builder Count", "5", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            ProfileField("Builder Apprentice Level", "0", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            ProfileField("Lab Assistant Level", "0", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            ProfileField("Alchemist Level", "0", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            ProfileField("Clock Tower Level", "0", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            ProfileField("Gold Pass Boost %", "0", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)

            Button(onClick = { /* Save */ }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text("Save Changes")
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String, keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        TextField(
            value = value,
            onValueChange = { },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType)
        )
    }
}
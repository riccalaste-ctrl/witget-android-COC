@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zacatac.clashboard.ui.importdata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zacatac.clashboard.data.repository.JsonImporter
import com.zacatac.clashboard.widget.WidgetUpdateWorker
import kotlinx.coroutines.launch

@Composable
fun ImportScreen(
    onImportComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var jsonText by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopAppBar(
            title = { Text("Import Village Data", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Paste your Clash of Clans JSON export below:", fontSize = 14.sp)

            OutlinedTextField(
                value = jsonText,
                onValueChange = { jsonText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = false,
                placeholder = { Text("Paste JSON here...") }
            )

            OutlinedButton(
                onClick = {
                    val text = clipboard.getText()?.text
                    if (text.isNullOrBlank()) {
                        errorMessage = "Clipboard is empty. Copy the JSON export first."
                    } else {
                        jsonText = text.trim()
                        errorMessage = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isImporting
            ) {
                Text("Paste from Clipboard")
            }

            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (isImporting) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            Button(
                onClick = {
                    isImporting = true
                    errorMessage = null
                    scope.launch {
                        try {
                            JsonImporter.importFromJson(context, jsonText.trim())
                            // Refresh widgets right away with the new data
                            WidgetUpdateWorker.scheduleImmediateUpdate(context)
                            onImportComplete()
                        } catch (e: Exception) {
                            errorMessage = "Import failed: ${e.message ?: e.javaClass.simpleName}"
                        } finally {
                            isImporting = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = !isImporting && jsonText.isNotBlank()
            ) {
                Text("Import")
            }
        }
    }
}

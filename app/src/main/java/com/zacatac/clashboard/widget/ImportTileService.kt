package com.zacatac.clashboard.widget

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.service.quicksettings.TileService
import android.widget.Toast
import com.zacatac.clashboard.data.database.AppDatabase
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.data.parser.CoCExportParser
import com.zacatac.clashboard.data.repository.ProfileRepository
import com.zacatac.clashboard.widget.closest.ClosestUpgradeWidget
import com.zacatac.clashboard.widget.main.MainWidget
import com.zacatac.clashboard.widget.war.WarStatusWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImportTileService : TileService() {
    private lateinit var profileRepository: ProfileRepository
    private lateinit var exportParser: CoCExportParser
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        profileRepository = ProfileRepository(database)
        exportParser = CoCExportParser(this)
    }

    override fun onClick() {
        super.onClick()

        // Get clipboard content
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip

        if (clipData == null || clipData.itemCount == 0) {
            showToast("Clipboard is empty")
            return
        }

        val text = clipData.getItemAt(0).text?.toString()?.trim()

        if (text.isNullOrBlank()) {
            showToast("No text in clipboard")
            return
        }

        // Try to parse as JSON
        if (!text.startsWith("{")) {
            showToast("Clipboard doesn't contain valid JSON")
            return
        }

        serviceScope.launch {
            try {
                val importedText = text
                val result = withContext(Dispatchers.IO) {
                    exportParser.parseExport(importedText, createTempProfile())
                }

                // Create new profile with imported data
                val newProfile = PlayerAccount(
                    displayName = "Imported ${result.playerTag}",
                    tag = result.playerTag,
                    rawJSON = importedText,
                    lastImportDate = result.timestamp,
                    activeUpgrades = result.upgrades,
                    activeBoosts = result.activeBoosts
                )

                withContext(Dispatchers.IO) {
                    profileRepository.insertProfile(newProfile)
                }

                // Update widgets
                MainWidget.updateAll(applicationContext)
                ClosestUpgradeWidget.updateAll(applicationContext)
                WarStatusWidget.updateAll(applicationContext)

                showToast("Import successful! ${result.upgrades.size} upgrades loaded")
            } catch (e: Exception) {
                showToast("Import failed: ${e.message}")
            }
        }
    }

    private fun createTempProfile(): PlayerAccount {
        return PlayerAccount()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

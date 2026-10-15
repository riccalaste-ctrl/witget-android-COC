package com.zacatac.clashboard.widget

import android.content.ClipboardManager
import android.content.Context
import android.service.quicksettings.TileService
import android.widget.Toast
import com.zacatac.clashboard.data.repository.JsonImporter
import com.zacatac.clashboard.widget.main.MainWidget
import com.zacatac.clashboard.widget.closest.ClosestUpgradeWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ImportTileService : TileService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onClick() {
        super.onClick()

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
        if (!text.startsWith("{")) {
            showToast("Clipboard doesn't contain valid JSON")
            return
        }

        serviceScope.launch {
            try {
                val profile = JsonImporter.importFromJson(applicationContext, text)
                MainWidget.updateAll(applicationContext)
                ClosestUpgradeWidget.updateAll(applicationContext)
                showToast("Import successful! ${profile.activeUpgrades.size} upgrades loaded")
            } catch (e: Exception) {
                showToast("Import failed: ${e.message}")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

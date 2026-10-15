package com.zacatac.clashboard.widget.config

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zacatac.clashboard.R
import com.zacatac.clashboard.data.database.AppDatabase
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.data.model.UpgradeCategory
import com.zacatac.clashboard.util.WidgetConfigStore
import com.zacatac.clashboard.widget.closest.ClosestUpgradeWidget
import com.zacatac.clashboard.widget.main.MainWidget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WidgetConfigActivity : AppCompatActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetType: WidgetType = WidgetType.MAIN
    private var profiles: List<PlayerAccount> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_config)

        // Get widget ID and type from intent
        val extras = intent.extras
        widgetId = extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        widgetType = WidgetType.valueOf(extras?.getString("widget_type") ?: "MAIN")

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setupViews()
        loadProfiles()
    }

    private fun setupViews() {
        findViewById<View>(R.id.btn_save).setOnClickListener { saveConfig() }
        findViewById<View>(R.id.btn_cancel).setOnClickListener { cancelConfig() }
    }

    private fun loadProfiles() {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@WidgetConfigActivity)
            profiles = database.playerAccountDao().getAllProfiles().first()

            populateProfileSpinner()
            if (widgetType == WidgetType.CLOSEST_UPGRADE) {
                populateUpgradeTypeSpinner()
            }
        }
    }

    private fun populateProfileSpinner() {
        val spinner = findViewById<Spinner>(R.id.spinner_profile)
        val items = mutableListOf("Auto (Last Used)")
        items.addAll(profiles.map { it.widgetDisplayName })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Load saved selection
        lifecycleScope.launch {
            val autoProfile = when (widgetType) {
                WidgetType.MAIN -> WidgetConfigStore.getMainWidgetAutoProfile(this@WidgetConfigActivity)
                WidgetType.CLOSEST_UPGRADE -> WidgetConfigStore.getClosestWidgetAutoProfile(this@WidgetConfigActivity)
            }
            if (autoProfile) {
                spinner.setSelection(0)
            } else {
                val profileIdStr = when (widgetType) {
                    WidgetType.MAIN -> WidgetConfigStore.getMainWidgetProfileId(this@WidgetConfigActivity)
                    WidgetType.CLOSEST_UPGRADE -> WidgetConfigStore.getClosestWidgetProfileId(this@WidgetConfigActivity)
                }
                if (profileIdStr != null) {
                    val index = profiles.indexOfFirst { it.id.toString() == profileIdStr } + 1
                    if (index > 0) spinner.setSelection(index)
                }
            }
        }
    }

    private fun populateUpgradeTypeSpinner() {
        val spinner = findViewById<Spinner>(R.id.spinner_upgrade_type)
        spinner.visibility = View.VISIBLE
        findViewById<View>(R.id.label_upgrade_type).visibility = View.VISIBLE

        val items = UpgradeCategory.entries.map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        lifecycleScope.launch {
            val currentType = WidgetConfigStore.getClosestWidgetUpgradeType(this@WidgetConfigActivity)
            val index = UpgradeCategory.entries.indexOfFirst { it == currentType }
            if (index >= 0) spinner.setSelection(index)
        }
    }

    private fun saveConfig() {
        val profileSpinner = findViewById<Spinner>(R.id.spinner_profile)
        val selectedIndex = profileSpinner.selectedItemPosition
        val isAuto = selectedIndex == 0

        lifecycleScope.launch {
            when (widgetType) {
                WidgetType.MAIN -> {
                    WidgetConfigStore.setMainWidgetAutoProfile(this@WidgetConfigActivity, isAuto)
                    if (!isAuto) {
                        val profile = profiles[selectedIndex - 1]
                        WidgetConfigStore.setMainWidgetProfileId(this@WidgetConfigActivity, profile.id.toString())
                    }
                }
                WidgetType.CLOSEST_UPGRADE -> {
                    WidgetConfigStore.setClosestWidgetAutoProfile(this@WidgetConfigActivity, isAuto)
                    if (!isAuto) {
                        val profile = profiles[selectedIndex - 1]
                        WidgetConfigStore.setClosestWidgetProfileId(this@WidgetConfigActivity, profile.id.toString())
                    }
                    // Save upgrade type
                    val typeSpinner = findViewById<Spinner>(R.id.spinner_upgrade_type)
                    val typeIndex = typeSpinner.selectedItemPosition
                    if (typeIndex >= 0) {
                        WidgetConfigStore.setClosestWidgetUpgradeType(
                            this@WidgetConfigActivity,
                            UpgradeCategory.entries[typeIndex]
                        )
                    }
                }
            }

            // Trigger widget update
            when (widgetType) {
                WidgetType.MAIN -> MainWidget.updateAll(this@WidgetConfigActivity)
                WidgetType.CLOSEST_UPGRADE -> ClosestUpgradeWidget.updateAll(this@WidgetConfigActivity)
            }

            // Return result
            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }

    private fun cancelConfig() {
        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        setResult(RESULT_CANCELED, resultValue)
        finish()
    }

    enum class WidgetType {
        MAIN, CLOSEST_UPGRADE
    }
}

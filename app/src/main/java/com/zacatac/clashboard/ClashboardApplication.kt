package com.zacatac.clashboard

import android.app.Application
import androidx.work.Configuration
import com.zacatac.clashboard.widget.UpgradeAlarmScheduler
import com.zacatac.clashboard.widget.WidgetTickScheduler
import com.zacatac.clashboard.widget.WidgetUpdateWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class ClashboardApplication : Application(), Configuration.Provider {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()
        // One-minute widget tick (refreshes every countdown)
        WidgetTickScheduler.start(this)

        // Immediate first update + schedule notifications for stored profiles
        appScope.launch {
            WidgetUpdateWorker.scheduleInitialUpdate(this@ClashboardApplication)
            runCatching { UpgradeAlarmScheduler.scheduleForAllProfiles(this@ClashboardApplication) }
        }
    }
}

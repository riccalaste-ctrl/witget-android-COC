package com.zacatac.clashboard

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.zacatac.clashboard.widget.WidgetUpdateWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ClashboardApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Schedule initial widget update
        WidgetUpdateWorker.scheduleInitialUpdate(this)
    }
}
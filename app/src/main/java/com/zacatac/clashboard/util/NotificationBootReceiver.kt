package com.zacatac.clashboard.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequest
import com.zacatac.clashboard.widget.WidgetUpdateWorker

class NotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Schedule initial widget update
            val workManager = WorkManager.getInstance(context)
            val updateWork = OneTimeWorkRequest.Builder(WidgetUpdateWorker::class.java)
                .setInitialDelay(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            workManager.enqueue(updateWork)
        }
    }
}
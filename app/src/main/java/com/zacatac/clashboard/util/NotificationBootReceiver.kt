package com.zacatac.clashboard.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zacatac.clashboard.widget.UpgradeAlarmScheduler
import com.zacatac.clashboard.widget.WidgetTickScheduler
import com.zacatac.clashboard.widget.WidgetUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** AlarmManager alarms do not survive a reboot, so they are re-planned from the stored villages. */
class NotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                WidgetTickScheduler.start(context)
                runCatching { UpgradeAlarmScheduler.scheduleForAllProfiles(context) }
                WidgetUpdateWorker.scheduleInitialUpdate(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

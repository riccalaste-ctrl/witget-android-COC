package com.zacatac.clashboard.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zacatac.clashboard.widget.closest.ClosestUpgradeWidget
import com.zacatac.clashboard.widget.main.MainWidget
import com.zacatac.clashboard.widget.war.WarStatusWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Re-fires every minute so widget countdowns stay fresh even without data changes.
 */
class WidgetTickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                updateAllWidgets(context)
            } finally {
                pendingResult.finish()
            }
        }
        WidgetTickScheduler.scheduleNextTick(context)
    }

    companion object {
        suspend fun updateAllWidgets(context: Context) {
            MainWidget.updateAll(context)
            ClosestUpgradeWidget.updateAll(context)
            WarStatusWidget.updateAll(context)
        }
    }
}

object WidgetTickScheduler {

    private const val ACTION_WIDGET_TICK = "com.zacatac.clashboard.action.WIDGET_TICK"
    private const val TICK_REQUEST_CODE = 42
    private const val TICK_INTERVAL_MS = 60_000L

    /** Starts (or restarts) the one-minute tick loop. */
    fun start(context: Context) = scheduleNextTick(context)

    fun scheduleNextTick(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WidgetTickReceiver::class.java).apply {
            action = ACTION_WIDGET_TICK
        }
        val pending = PendingIntent.getBroadcast(
            context,
            TICK_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + TICK_INTERVAL_MS
        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        try {
            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    android.os.SystemClock.elapsedRealtime() + TICK_INTERVAL_MS,
                    pending
                )
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pending)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pending)
        }
    }
}

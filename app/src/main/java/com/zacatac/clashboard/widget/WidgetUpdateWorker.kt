package com.zacatac.clashboard.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zacatac.clashboard.data.database.AppDatabase
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.widget.closest.ClosestUpgradeWidget
import com.zacatac.clashboard.widget.main.MainWidget
import com.zacatac.clashboard.widget.war.WarStatusWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.util.concurrent.TimeUnit
import kotlin.math.min

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val database = AppDatabase.getDatabase(applicationContext)
        val profiles = withContext(Dispatchers.IO) {
            database.playerAccountDao().getAllProfiles().first()
        }

        if (profiles.isNotEmpty()) {
            // Update all widget instances
            updateMainWidgets()
            updateClosestWidgets()
            updateWarWidgets()

            // Schedule next update based on urgency
            scheduleNextUpdate(profiles)
        }

        Result.success()
    } catch (e: Exception) {
        e.printStackTrace()
        Result.retry()
    }

    private suspend fun updateMainWidgets() {
        MainWidget.updateAll(applicationContext)
    }

    private suspend fun updateClosestWidgets() {
        ClosestUpgradeWidget.updateAll(applicationContext)
    }

    private suspend fun updateWarWidgets() {
        WarStatusWidget.updateAll(applicationContext)
    }

    private suspend fun scheduleNextUpdate(profiles: List<PlayerAccount>) {
        val now = Clock.System.now()
        var minDelayMinutes = 15 // Default

        for (profile in profiles) {
            val activeBoosts = profile.activeBoosts.filter { it.endTime > now }
            val hasActiveBoosts = activeBoosts.isNotEmpty()

            // Check urgency based on closest upgrade
            var closestRemaining = Double.MAX_VALUE
            for (upgrade in profile.activeUpgrades) {
                val remaining = upgrade.remainingSeconds(activeBoosts, now)
                if (remaining > 0 && remaining < closestRemaining) {
                    closestRemaining = remaining
                }
            }

            val closestMinutes = closestRemaining / 60.0

            when {
                hasActiveBoosts && closestMinutes <= 1 -> minDelayMinutes = min(minDelayMinutes, 1)
                hasActiveBoosts && closestMinutes <= 5 -> minDelayMinutes = min(minDelayMinutes, 5)
                hasActiveBoosts -> minDelayMinutes = min(minDelayMinutes, 5)
                closestMinutes <= 5 -> minDelayMinutes = min(minDelayMinutes, 1)
                else -> minDelayMinutes = min(minDelayMinutes, 15)
            }

            // Also schedule at boost expiry times
            if (hasActiveBoosts) {
                closeBoosts(activeBoosts, now)?.let { minDelayMinutes = min(minDelayMinutes, it) }
            }
        }

        // Schedule next work
        val delayMillis = (minDelayMinutes.coerceAtLeast(1) * 60 * 1000L).coerceAtLeast(60_000L)
        val nextWork = OneTimeWorkRequest.Builder(WidgetUpdateWorker::class.java)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            WIDGET_UPDATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            nextWork
        )
    }

    private fun closeBoosts(
        activeBoosts: List<com.zacatac.clashboard.data.model.ActiveBoost>,
        now: kotlinx.datetime.Instant
    ): Int? {
        var minutesUntilFirstExpiry: Int? = null
        for (boost in activeBoosts) {
            val minutes = (boost.endTime - now).inWholeMinutes
            if (minutes > 0 && (minutesUntilFirstExpiry == null || minutes < minutesUntilFirstExpiry)) {
                minutesUntilFirstExpiry = minutes.toInt()
            }
        }
        return minutesUntilFirstExpiry
    }        companion object {
        private const val WIDGET_UPDATE_WORK_NAME = "widget_update"

        /** Forces a widget refresh as soon as possible (e.g. right after an import). */
        fun scheduleImmediateUpdate(context: Context) {
            val work = OneTimeWorkRequest.Builder(WidgetUpdateWorker::class.java)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WIDGET_UPDATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                work
            )
        }

        fun scheduleInitialUpdate(context: Context) {
            val work = OneTimeWorkRequest.Builder(WidgetUpdateWorker::class.java)
                .setInitialDelay(30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WIDGET_UPDATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                work
            )
        }
    }
}

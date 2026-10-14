package com.zacatac.clashboard.widget

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.zacatac.clashboard.ui.MainActivity
import com.zacatac.clashboard.R
import com.zacatac.clashboard.data.database.AppDatabase
import com.zacatac.clashboard.data.model.PlayerAccount
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.random.Random

const val CHANNEL_UPGRADES = "upgrade_complete"
const val CHANNEL_BOOSTS = "boost_expire"
const val ACTION_UPGRADE_COMPLETE = "com.zacatac.clashboard.action.UPGRADE_COMPLETE"
const val ACTION_BOOST_EXPIRE = "com.zacatac.clashboard.action.BOOST_EXPIRE"

/** Receiver fired exactly when an upgrade countdown ends: posts a notification. */
class UpgradeAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra(EXTRA_UPGRADE_NAME) ?: "Upgrade"
        val level = intent.getIntExtra(EXTRA_TARGET_LEVEL, 0)
        val kind = intent.getStringExtra(EXTRA_KIND) ?: KIND_UPGRADE

        val title = if (kind == KIND_UPGRADE) "$name è pronto!" else "$name: boost scaduto"
        val text = if (kind == KIND_UPGRADE) {
            "Upgrade completato a livello $level"
        } else {
            "Boost scaduto per $name — riattivalo su Clash of Clans se serve"
        }

        postNotification(context, kind, title, text)
    }

    private fun postNotification(context: Context, kind: String, title: String, text: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_UPGRADES, "Upgrade completi", NotificationManager.IMPORTANCE_HIGH)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_BOOSTS, "Scadenza boost", NotificationManager.IMPORTANCE_DEFAULT)
        )

        val canPost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        if (!canPost) return

        val openApp = Intent(context, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            context, 0, openApp, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, kind)
            .setSmallIcon(R.drawable.ic_import)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        NotificationManagerCompat.from(context).notify(Random.nextInt(), builder.build())
    }

    companion object {
        const val EXTRA_UPGRADE_ID = "upgrade_id"
        const val EXTRA_UPGRADE_NAME = "upgrade_name"
        const val EXTRA_TARGET_LEVEL = "target_level"
        const val EXTRA_KIND = "kind"
        const val KIND_UPGRADE = "upgrade"
        const val KIND_BOOST = "boost"
    }
}

object UpgradeAlarmScheduler {

    /** Schedules a completion alarm for every active upgrade in [profile]. */
    suspend fun scheduleForProfile(context: Context, profile: PlayerAccount) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = Clock.System.now()

        for ((index, upgrade) in profile.activeUpgrades.sortedBy { it.endTime }.withIndex()) {
            val delayMs = upgrade.endTime.toEpochMilliseconds() - now.toEpochMilliseconds()
            if (delayMs <= 0) continue

            val intent = Intent(context, UpgradeAlarmReceiver::class.java).apply {
                action = ACTION_UPGRADE_COMPLETE
                putExtra(UpgradeAlarmReceiver.EXTRA_UPGRADE_ID, upgrade.id.toString())
                putExtra(UpgradeAlarmReceiver.EXTRA_UPGRADE_NAME, upgrade.name)
                putExtra(UpgradeAlarmReceiver.EXTRA_TARGET_LEVEL, upgrade.targetLevel)
                putExtra(UpgradeAlarmReceiver.EXTRA_KIND, UpgradeAlarmReceiver.KIND_UPGRADE)
            }
            val pending = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_BASE + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleAlarm(alarmManager, upgrade.endTime.toEpochMilliseconds(), pending)
        }
    }

    /** Schedules alarms for all stored profiles (called at app start). */
    suspend fun scheduleForAllProfiles(context: Context) {
        val profiles = AppDatabase.getDatabase(context).playerAccountDao().getAllProfiles().first()
        profiles.forEach { scheduleForProfile(context, it) }
    }

    private fun scheduleAlarm(alarmManager: AlarmManager, atEpochMs: Long, pending: PendingIntent) {
        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        try {
            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atEpochMs, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atEpochMs, pending)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atEpochMs, pending)
        }
    }

    private const val REQUEST_CODE_BASE = 10_000
}

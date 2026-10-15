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
import com.zacatac.clashboard.R
import com.zacatac.clashboard.data.database.AppDatabase
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.ui.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import java.util.UUID
import kotlin.random.Random

const val CHANNEL_UPGRADES = "upgrade_complete"
const val CHANNEL_BOOSTS = "boost_expire"
const val ACTION_UPGRADE_COMPLETE = "com.zacatac.clashboard.action.UPGRADE_COMPLETE"
const val ACTION_BOOST_EXPIRE = "com.zacatac.clashboard.action.BOOST_EXPIRE"

/** Receiver fired when an upgrade completes or a boost expires: posts a notification. */
class UpgradeAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra(EXTRA_UPGRADE_NAME) ?: "Upgrade"
        val level = intent.getIntExtra(EXTRA_TARGET_LEVEL, 0)
        val kind = intent.getStringExtra(EXTRA_KIND) ?: KIND_UPGRADE

        val title = if (kind == KIND_UPGRADE) "$name è pronto!" else "$name: boost scaduto"
        val text = if (kind == KIND_UPGRADE) {
            "Upgrade completato a livello $level"
        } else {
            "Boost scaduto — riattivalo su Clash of Clans se serve"
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

        val channel = if (kind == KIND_BOOST) CHANNEL_BOOSTS else CHANNEL_UPGRADES
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_import)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        NotificationManagerCompat.from(context).notify(Random.nextInt(), builder.build())
    }

    companion object {
        const val EXTRA_UPGRADE_NAME = "upgrade_name"
        const val EXTRA_TARGET_LEVEL = "target_level"
        const val EXTRA_KIND = "kind"
        const val KIND_UPGRADE = "upgrade"
        const val KIND_BOOST = "boost"
    }
}

/**
 * Owns the completion and boost-expiry alarms. Each registered alarm is remembered per profile
 * (action + request code) so it can be cancelled when the village is re-imported, deleted or reset.
 */
object UpgradeAlarmScheduler {
    private const val PREFS = "clashboard_alarms"
    private const val KEY_PREFIX = "codes_"

    /** Replaces this village's alarms with the ones planned from its current upgrades and settings. */
    suspend fun scheduleForProfile(context: Context, profile: PlayerAccount) {
        cancelForProfile(context, profile.id)
        val plans = planProfileAlarms(profile, Clock.System.now())
        if (plans.isEmpty()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val registered = mutableSetOf<String>()
        for (plan in plans) {
            val intent = Intent(context, UpgradeAlarmReceiver::class.java).apply {
                action = plan.action
                putExtra(UpgradeAlarmReceiver.EXTRA_UPGRADE_NAME, plan.name)
                putExtra(UpgradeAlarmReceiver.EXTRA_TARGET_LEVEL, plan.level)
                putExtra(UpgradeAlarmReceiver.EXTRA_KIND, plan.kind)
            }
            val pending = PendingIntent.getBroadcast(
                context,
                plan.requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleAlarm(alarmManager, plan.triggerAtMs, pending)
            registered += "${plan.action}#${plan.requestCode}"
        }
        prefs(context).edit().putStringSet(keyFor(profile.id), registered).apply()
    }

    /** Cancels every alarm registered for [profileId]. */
    fun cancelForProfile(context: Context, profileId: UUID) {
        val prefs = prefs(context)
        val key = keyFor(profileId)
        cancelEntries(context, prefs.getStringSet(key, emptySet()).orEmpty())
        prefs.edit().remove(key).apply()
    }

    /** Cancels every alarm of every village (used before a full reset or a full re-plan). */
    fun cancelAll(context: Context) {
        val prefs = prefs(context)
        for ((key, value) in prefs.all) {
            if (!key.startsWith(KEY_PREFIX)) continue
            cancelEntries(context, (value as? Set<*>)?.filterIsInstance<String>().orEmpty().toSet())
        }
        prefs.edit().clear().apply()
    }

    /** Re-plans alarms for every stored village, dropping alarms of villages that no longer exist. */
    suspend fun scheduleForAllProfiles(context: Context) {
        cancelAll(context)
        val profiles = AppDatabase.getDatabase(context).playerAccountDao().getAllProfiles().first()
        profiles.forEach { scheduleForProfile(context, it) }
    }

    private fun cancelEntries(context: Context, entries: Set<String>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (entry in entries) {
            val parts = entry.split("#", limit = 2)
            if (parts.size != 2) continue
            val requestCode = parts[1].toIntOrNull() ?: continue
            val intent = Intent(context, UpgradeAlarmReceiver::class.java).apply { action = parts[0] }
            val pending = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
        }
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

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun keyFor(profileId: UUID) = "$KEY_PREFIX$profileId"
}

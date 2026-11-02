package app.tendwell.now

import android.Manifest
import android.annotation.SuppressLint
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
import app.tendwell.MainActivity
import app.tendwell.R

/**
 * The gentle nudge when a timer runs out. Uses an inexact, idle-allowed alarm
 * (no SCHEDULE_EXACT_ALARM, which Play restricts), so in deep sleep it may
 * arrive a few minutes late. Home and the timer screen always show the true time left.
 */
object TimerAlarm {
    private const val CHANNEL_ID = "timer"
    private const val NOTIFICATION_ID = 1
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_BREAK = "break"

    fun schedule(context: Context, task: NowTask) {
        cancel(context)
        if (!task.running || !task.hasTimer || task.phase == NowPhase.Done) return
        val alarms = context.getSystemService(AlarmManager::class.java)
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.endAtMs, pending(context, task))
    }

    fun cancel(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        alarms.cancel(pending(context, null))
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun pending(context: Context, task: NowTask?): PendingIntent {
        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            if (task != null) {
                putExtra(EXTRA_TITLE, task.title)
                putExtra(EXTRA_BREAK, task.phase == NowPhase.Break)
            }
        }
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // Checked by canNotify() just above the call.
    internal fun show(context: Context, title: String, isBreak: Boolean) {
        if (!canNotify(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Timer alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "A nudge when your Now timer or break ends"
            },
        )
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (isBreak) "Break's over" else "Time's up")
            .setContentText(if (isBreak) "Back to it when you're ready." else title)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    class TimerAlarmReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            show(context, intent.getStringExtra(EXTRA_TITLE).orEmpty(), intent.getBooleanExtra(EXTRA_BREAK, false))
        }
    }
}

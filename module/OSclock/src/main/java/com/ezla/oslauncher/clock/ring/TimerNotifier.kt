package com.ezla.oslauncher.clock.ring

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ezla.oslauncher.clock.ClockActivity
import com.ezla.oslauncher.clock.R
import com.ezla.oslauncher.clock.core.ClockMath
import com.ezla.oslauncher.clock.data.Alarm
import com.ezla.oslauncher.clock.data.ClockStore

// Notification đếm ngược của hẹn giờ đang chạy (thay "Lock Screen / Dynamic Island" của iOS):
// dùng chronometer đếm ngược của hệ thống nên không cần service chạy nền.
object TimerNotifier {

    private const val CHANNEL = "osclock_timer"
    private const val NOTIFICATION_ID = 7102

    fun refresh(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val now = System.currentTimeMillis()
        val t = ClockStore(context).timers().filter { it.running && it.endAt > now }.minByOrNull { it.endAt }
        if (t == null) {
            nm.cancel(NOTIFICATION_ID)
            return
        }
        ensureChannel(context, nm)
        val open = PendingIntent.getActivity(
            context, NOTIFICATION_ID,
            Intent(context, ClockActivity::class.java).putExtra(ClockActivity.EXTRA_TAB, ClockActivity.TAB_TIMERS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.osclock_ic_tab_timer)
            .setContentTitle(t.label.ifBlank { context.getString(R.string.osclock_timer) })
            .setContentText(context.getString(R.string.osclock_timer_duration, ClockMath.countdown(t.durationMs)))
            .setWhen(t.endAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .build()
        runCatching { nm.notify(NOTIFICATION_ID, n) }
    }

    private fun ensureChannel(context: Context, nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || nm.getNotificationChannel(CHANNEL) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.osclock_channel_timer), NotificationManager.IMPORTANCE_LOW)
        )
    }
}

/** Notification "Snoozed · rings in 8:59" + nút Stop (Figma "Clock › Snoozed"). */
object SnoozeNotifier {

    private const val CHANNEL = "osclock_snooze"

    fun show(context: Context, a: Alarm) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.osclock_channel_snooze), NotificationManager.IMPORTANCE_LOW)
            )
        }
        val stop = PendingIntent.getBroadcast(
            context, AlarmScheduler.requestCode(RingService.KIND_ALARM, a.id),
            Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_CANCEL_SNOOZE)
                .putExtra(RingService.EXTRA_ID, a.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.osclock_ic_tab_alarm)
            .setContentTitle(a.label.ifBlank { context.getString(R.string.osclock_alarm) })
            .setContentText(context.getString(R.string.osclock_snoozed))
            .setWhen(a.snoozeUntil)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setTimeoutAfter((a.snoozeUntil - System.currentTimeMillis()).coerceAtLeast(1000))
            .addAction(0, context.getString(R.string.osclock_stop), stop)
            .build()
        runCatching { nm.notify(tag(a.id), 0, n) }
    }

    fun cancel(context: Context, id: Int) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(tag(id), 0)
    }

    private fun tag(id: Int) = "osclock_snooze_$id"
}

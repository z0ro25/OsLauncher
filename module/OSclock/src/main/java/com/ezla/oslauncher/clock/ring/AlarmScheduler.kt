package com.ezla.oslauncher.clock.ring

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.ezla.oslauncher.clock.ClockActivity
import com.ezla.oslauncher.clock.core.ClockMath
import com.ezla.oslauncher.clock.data.Alarm
import com.ezla.oslauncher.clock.data.ClockStore

// Lên lịch qua AlarmManager.setAlarmClock (chính xác, kêu cả khi Doze). Máy chưa cấp quyền
// exact alarm (API 31+) thì lùi về setAndAllowWhileIdle — vẫn kêu nhưng có thể trễ vài phút.
object AlarmScheduler {

    fun canExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager(context).canScheduleExactAlarms()

    fun exactAlarmSettingsIntent(context: Context): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))
        } else null

    /** Mốc kêu kế tiếp của [a], hoặc null nếu đang tắt. */
    fun nextTriggerOf(a: Alarm, now: Long = System.currentTimeMillis()): Long? {
        if (!a.enabled) return null
        if (a.snoozeUntil > now) return a.snoozeUntil
        return ClockMath.nextTrigger(now, a.hour, a.minute, a.days)
    }

    fun schedule(context: Context, a: Alarm) {
        cancelAlarm(context, a.id)
        val at = nextTriggerOf(a) ?: return
        set(context, at, firePending(context, RingService.KIND_ALARM, a.id))
    }

    fun cancelAlarm(context: Context, id: Int) =
        manager(context).cancel(firePending(context, RingService.KIND_ALARM, id))

    fun scheduleTimer(context: Context, id: Int, endAt: Long) {
        set(context, endAt, firePending(context, RingService.KIND_TIMER, id))
    }

    fun cancelTimer(context: Context, id: Int) =
        manager(context).cancel(firePending(context, RingService.KIND_TIMER, id))

    /** Sau boot / đổi giờ / đổi múi giờ / cập nhật app: AlarmManager đã xoá hết lịch. */
    fun rescheduleAll(context: Context) {
        val store = ClockStore(context)
        val now = System.currentTimeMillis()
        store.alarms().forEach { schedule(context, it) }
        store.timers().filter { it.running }.forEach { t ->
            // Hết giờ trong lúc máy tắt: kêu ngay thay vì bỏ lỡ.
            scheduleTimer(context, t.id, maxOf(t.endAt, now + 1000))
        }
        TimerNotifier.refresh(context)
    }

    private fun set(context: Context, at: Long, op: PendingIntent) {
        val am = manager(context)
        if (canExact(context)) {
            val show = PendingIntent.getActivity(
                context, 0, Intent(context, ClockActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            runCatching { am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), op) }
                .onFailure { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op) }
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
        }
    }

    private fun firePending(context: Context, kind: Int, id: Int): PendingIntent {
        val i = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE)
            .putExtra(RingService.EXTRA_KIND, kind)
            .putExtra(RingService.EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            context, requestCode(kind, id), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** id của alarm và timer cùng lấy từ ClockStore.nextId nên không đụng nhau; nhân 2 cho chắc. */
    fun requestCode(kind: Int, id: Int) = id * 2 + kind

    private fun manager(context: Context) = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
}

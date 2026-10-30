package com.ezla.oslauncher.clock.ring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Nhận mốc kêu từ AlarmManager và các sự kiện làm mất lịch (boot, đổi giờ, cập nhật app). */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FIRE -> RingService.fire(
                context,
                intent.getIntExtra(RingService.EXTRA_KIND, RingService.KIND_ALARM),
                intent.getIntExtra(RingService.EXTRA_ID, -1)
            )
            ACTION_SNOOZE -> RingService.snooze(context)
            ACTION_DISMISS -> RingService.dismiss(context)
            ACTION_CANCEL_SNOOZE -> RingService.cancelSnooze(context, intent.getIntExtra(RingService.EXTRA_ID, -1))
            else -> AlarmScheduler.rescheduleAll(context)
        }
    }

    companion object {
        const val ACTION_FIRE = "com.ezla.oslauncher.clock.FIRE"
        const val ACTION_SNOOZE = "com.ezla.oslauncher.clock.SNOOZE"
        const val ACTION_DISMISS = "com.ezla.oslauncher.clock.DISMISS"
        const val ACTION_CANCEL_SNOOZE = "com.ezla.oslauncher.clock.CANCEL_SNOOZE"
    }
}

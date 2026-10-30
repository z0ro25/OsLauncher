package com.ezla.oslauncher.clock.ring

import android.content.Context
import com.ezla.oslauncher.clock.R
import com.ezla.oslauncher.clock.core.ClockMath
import com.ezla.oslauncher.clock.data.ClockStore

/** Ảnh chụp thông tin chuông đang kêu, đọc từ ClockStore lúc kêu. */
data class RingInfo(
    val kind: Int,
    val id: Int,
    val label: String,
    val sound: String,
    val vibrate: Boolean,
    val snooze: Boolean,
    val hour: Int,
    val minute: Int,
    val durationMs: Long
) {
    fun title(context: Context): String = when {
        kind == RingService.KIND_TIMER -> label.ifBlank { context.getString(R.string.osclock_timer) }
        else -> label.ifBlank { context.getString(R.string.osclock_alarm) }
    }

    fun subtitle(context: Context): String =
        if (kind == RingService.KIND_TIMER) context.getString(R.string.osclock_timer_completed, ClockMath.countdown(durationMs))
        else ClockMath.hhmm(hour, minute)

    companion object {
        fun load(context: Context, kind: Int, id: Int): RingInfo? {
            val store = ClockStore(context)
            return if (kind == RingService.KIND_TIMER) {
                store.timer(id)?.let {
                    RingInfo(kind, id, it.label, it.sound, true, false, 0, 0, it.durationMs)
                }
            } else {
                store.alarm(id)?.let {
                    RingInfo(kind, id, it.label, it.sound, it.vibrate, it.snooze, it.hour, it.minute, 0)
                }
            }
        }
    }
}

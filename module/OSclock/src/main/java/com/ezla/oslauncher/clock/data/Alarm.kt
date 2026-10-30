package com.ezla.oslauncher.clock.data

import org.json.JSONObject

/** [days]: bitmask ClockMath (bit0 = CN). [sound]: URI chuông của máy, "" = mặc định, ClockSounds.SILENT = im. */
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val days: Int = 0,
    val label: String = "",
    val sound: String = "",
    val snooze: Boolean = true,
    val vibrate: Boolean = true,
    val enabled: Boolean = true,
    /** Mốc kêu lại sau Snooze (0 = không snooze). Ưu tiên hơn giờ gốc khi lên lịch. */
    val snoozeUntil: Long = 0L,
    /** Báo thức của Sleep | Wake Up: không hiện trong mục "Other". */
    val isSleep: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("h", hour).put("m", minute).put("d", days).put("l", label)
        .put("s", sound).put("z", snooze).put("v", vibrate).put("e", enabled)
        .put("su", snoozeUntil).put("sl", isSleep)

    companion object {
        fun fromJson(o: JSONObject) = Alarm(
            id = o.getInt("id"), hour = o.getInt("h"), minute = o.getInt("m"),
            days = o.optInt("d"), label = o.optString("l"), sound = o.optString("s"),
            snooze = o.optBoolean("z", true), vibrate = o.optBoolean("v", true),
            enabled = o.optBoolean("e", true), snoozeUntil = o.optLong("su"),
            isSleep = o.optBoolean("sl")
        )
    }
}

/** Hẹn giờ: chạy thì có [endAt], tạm dừng thì giữ [remainingMs]. */
data class ClockTimer(
    val id: Int,
    val durationMs: Long,
    val label: String = "",
    val sound: String = "",
    val endAt: Long = 0L,
    val remainingMs: Long = durationMs
) {
    val running: Boolean get() = endAt > 0L

    fun remaining(now: Long): Long = if (running) (endAt - now).coerceAtLeast(0) else remainingMs

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("dur", durationMs).put("l", label).put("s", sound)
        .put("end", endAt).put("rem", remainingMs)

    companion object {
        fun fromJson(o: JSONObject) = ClockTimer(
            id = o.getInt("id"), durationMs = o.getLong("dur"), label = o.optString("l"),
            sound = o.optString("s"), endAt = o.optLong("end"), remainingMs = o.optLong("rem")
        )
    }
}

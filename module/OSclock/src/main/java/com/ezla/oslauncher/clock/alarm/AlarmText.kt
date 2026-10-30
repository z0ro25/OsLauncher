package com.ezla.oslauncher.clock.alarm

import android.content.Context
import com.ezla.oslauncher.clock.R
import com.ezla.oslauncher.clock.core.ClockMath
import com.ezla.oslauncher.clock.data.Alarm
import java.text.DateFormatSymbols
import java.util.Calendar

/** Chuỗi hiển thị của báo thức theo locale hiện tại. */
object AlarmText {

    /** "Weekdays", "Weekends", "Every day", "Mon, Wed", "" (một lần). Thứ tự bắt đầu từ thứ Hai. */
    fun repeat(context: Context, days: Int): String {
        val d = days and ClockMath.EVERY_DAY
        return when (d) {
            0 -> ""
            ClockMath.EVERY_DAY -> context.getString(R.string.osclock_every_day)
            ClockMath.WEEKDAYS -> context.getString(R.string.osclock_weekdays)
            ClockMath.WEEKEND -> context.getString(R.string.osclock_weekends)
            else -> {
                val names = DateFormatSymbols.getInstance().shortWeekdays
                ORDER.filter { d and ClockMath.dayBit(it) != 0 }.joinToString(", ") { names[it] }
            }
        }
    }

    fun repeatOrNever(context: Context, days: Int) =
        repeat(context, days).ifEmpty { context.getString(R.string.osclock_never) }

    /** Dòng mô tả dưới giờ: "Morning run, weekdays" / "Gym · Sat, Sun" / "Alarm". */
    fun description(context: Context, a: Alarm): String {
        val label = a.label.ifBlank { context.getString(R.string.osclock_alarm) }
        val rep = repeat(context, a.days)
        return if (rep.isEmpty()) label else context.getString(R.string.osclock_label_repeat, label, rep)
    }

    /** "Alarm in 18 hr 4 min" cho toast sau khi lưu / bật. */
    fun until(context: Context, trigger: Long): String {
        val (h, m) = ClockMath.untilParts(System.currentTimeMillis(), trigger).let { it[0] to it[1] }
        return when {
            h == 0 && m == 0 -> context.getString(R.string.osclock_alarm_in_less_minute)
            h == 0 -> context.getString(R.string.osclock_alarm_in_min, m)
            m == 0 -> context.getString(R.string.osclock_alarm_in_hr, h)
            else -> context.getString(R.string.osclock_alarm_in_hr_min, h, m)
        }
    }

    /** Thứ tự hiển thị ngày: Chủ nhật đầu tiên như Figma "Repeat". */
    val REPEAT_ORDER = listOf(
        Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    )

    private val ORDER = REPEAT_ORDER.drop(1) + Calendar.SUNDAY
}

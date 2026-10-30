package com.ezla.oslauncher.clock.core

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

/** Phép tính thuần (không Android) của module Clock — tách riêng để unit test. */
object ClockMath {

    /** Bit 0 = Chủ nhật ... bit 6 = Thứ bảy (khớp Calendar.SUNDAY..SATURDAY trừ 1). */
    const val WEEKDAYS = 0b0111110
    const val WEEKEND = 0b1000001
    const val EVERY_DAY = 0b1111111

    fun dayBit(calendarDay: Int) = 1 shl (calendarDay - Calendar.SUNDAY)

    /** Lần kêu kế tiếp (> now) của báo thức hour:minute lặp theo [days] (0 = một lần). */
    fun nextTrigger(now: Long, hour: Int, minute: Int, days: Int, tz: TimeZone = TimeZone.getDefault()): Long {
        val c = Calendar.getInstance(tz)
        c.timeInMillis = now
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, minute)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        if (c.timeInMillis <= now) c.add(Calendar.DAY_OF_YEAR, 1)
        if (days and EVERY_DAY == 0) return c.timeInMillis
        repeat(7) {
            if (days and dayBit(c.get(Calendar.DAY_OF_WEEK)) != 0) return c.timeInMillis
            c.add(Calendar.DAY_OF_YEAR, 1)
        }
        return c.timeInMillis
    }

    /** [hours, minutes] làm tròn lên phút (iOS: còn 59 giây vẫn báo "1 min"). */
    fun untilParts(now: Long, trigger: Long): IntArray {
        val minutes = ((trigger - now + 59_999) / 60_000).coerceAtLeast(0)
        return intArrayOf((minutes / 60).toInt(), (minutes % 60).toInt())
    }

    /** Lệch giờ của [zone] so với [local] tại [now], đơn vị phút. */
    fun offsetMinutes(now: Long, zone: TimeZone, local: TimeZone): Int =
        (zone.getOffset(now) - local.getOffset(now)) / 60_000

    /** "+0HRS", "-7HRS", "+5:30HRS" như Figma. */
    fun offsetLabel(minutes: Int): String {
        val sign = if (minutes < 0) "-" else "+"
        val a = abs(minutes)
        val h = a / 60
        val m = a % 60
        return if (m == 0) "$sign${h}HRS" else "$sign$h:${m.toString().padStart(2, '0')}HRS"
    }

    /** -1 hôm qua, 0 hôm nay, 1 ngày mai: so ngày lịch giữa [zone] và [local]. */
    fun dayDelta(now: Long, zone: TimeZone, local: TimeZone): Int {
        val a = Calendar.getInstance(zone).apply { timeInMillis = now }
        val b = Calendar.getInstance(local).apply { timeInMillis = now }
        val ka = a.get(Calendar.YEAR) * 400 + a.get(Calendar.DAY_OF_YEAR)
        val kb = b.get(Calendar.YEAR) * 400 + b.get(Calendar.DAY_OF_YEAR)
        return (ka - kb).coerceIn(-1, 1)
    }

    fun two(v: Int) = v.toString().padStart(2, '0')

    fun hhmm(hour: Int, minute: Int) = "${two(hour)}:${two(minute)}"

    /** Bấm giờ "01:24,37" (phút:giây,phần trăm); quá 1 giờ thêm "1:01:24,37". */
    fun stopwatch(ms: Long): String {
        val cs = (ms / 10) % 100
        val totalSec = ms / 1000
        val s = totalSec % 60
        val m = (totalSec / 60) % 60
        val h = totalSec / 3600
        val base = "${two(m.toInt())}:${two(s.toInt())},${two(cs.toInt())}"
        return if (h > 0) "$h:$base" else base
    }

    /** Hẹn giờ "09:42" / "1:09:42"; làm tròn lên giây để không hiện 00:00 khi còn 0.4s. */
    fun countdown(ms: Long): String {
        val total = ((ms.coerceAtLeast(0) + 999) / 1000)
        val s = (total % 60).toInt()
        val m = ((total / 60) % 60).toInt()
        val h = (total / 3600).toInt()
        return if (h > 0) "$h:${two(m)}:${two(s)}" else "${two(m)}:${two(s)}"
    }

    /** Vòng ngủ: số phút từ giờ ngủ tới giờ dậy (qua nửa đêm vẫn dương), 0..1439. */
    fun sleepMinutes(bedMin: Int, wakeMin: Int): Int = ((wakeMin - bedMin) % 1440 + 1440) % 1440

    /** Chỉ số lap nhanh nhất / chậm nhất trong các lap đã xong; cần >= 2 lap mới tô màu. */
    fun bestWorst(laps: List<Long>): Pair<Int, Int> {
        if (laps.size < 2) return -1 to -1
        var best = 0
        var worst = 0
        laps.forEachIndexed { i, v ->
            if (v < laps[best]) best = i
            if (v > laps[worst]) worst = i
        }
        return best to worst
    }
}

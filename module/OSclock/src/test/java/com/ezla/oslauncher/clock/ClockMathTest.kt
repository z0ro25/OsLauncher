package com.ezla.oslauncher.clock

import com.ezla.oslauncher.clock.core.ClockMath
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ClockMathTest {

    private val utc = TimeZone.getTimeZone("UTC")

    // Thứ Năm 22/10/2026 10:00 UTC
    private val now = Calendar.getInstance(utc).apply {
        clear(); set(2026, Calendar.OCTOBER, 22, 10, 0, 0)
    }.timeInMillis

    private fun at(day: Int, h: Int, m: Int) = Calendar.getInstance(utc).apply {
        clear(); set(2026, Calendar.OCTOBER, day, h, m, 0)
    }.timeInMillis

    @Test fun oneShotLaterToday() = assertEquals(at(22, 18, 30), ClockMath.nextTrigger(now, 18, 30, 0, utc))

    @Test fun oneShotPassedGoesTomorrow() = assertEquals(at(23, 6, 30), ClockMath.nextTrigger(now, 6, 30, 0, utc))

    @Test fun sameMinuteCountsAsPassed() = assertEquals(at(23, 10, 0), ClockMath.nextTrigger(now, 10, 0, 0, utc))

    @Test fun weekdaysSkipWeekend() {
        // Thứ Sáu 23 6:30 -> kế tiếp; nếu đặt từ thứ Sáu sau giờ thì sang thứ Hai 26.
        assertEquals(at(23, 6, 30), ClockMath.nextTrigger(now, 6, 30, ClockMath.WEEKDAYS, utc))
        assertEquals(at(26, 6, 30), ClockMath.nextTrigger(at(23, 7, 0), 6, 30, ClockMath.WEEKDAYS, utc))
    }

    @Test fun weekendOnly() = assertEquals(at(24, 9, 0), ClockMath.nextTrigger(now, 9, 0, ClockMath.WEEKEND, utc))

    @Test fun singleDayRepeat() {
        val thursday = ClockMath.dayBit(Calendar.THURSDAY)
        assertEquals(at(22, 11, 0), ClockMath.nextTrigger(now, 11, 0, thursday, utc))
        assertEquals(at(29, 9, 0), ClockMath.nextTrigger(now, 9, 0, thursday, utc))
    }

    @Test fun untilRoundsUpToMinute() {
        assertArrayEquals(intArrayOf(18, 4), ClockMath.untilParts(now, now + (18 * 60 + 3) * 60_000L + 1))
        assertArrayEquals(intArrayOf(0, 1), ClockMath.untilParts(now, now + 59_000))
    }

    @Test fun offsetLabels() {
        assertEquals("+0HRS", ClockMath.offsetLabel(0))
        assertEquals("-7HRS", ClockMath.offsetLabel(-420))
        assertEquals("+5:30HRS", ClockMath.offsetLabel(330))
    }

    @Test fun offsetAndDayDelta() {
        val hanoi = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        val sf = TimeZone.getTimeZone("America/Los_Angeles")
        assertEquals(-14 * 60, ClockMath.offsetMinutes(now, sf, hanoi))
        // 10:00 UTC = 17:00 Hà Nội, 03:00 SF cùng ngày 22.
        assertEquals(0, ClockMath.dayDelta(now, sf, hanoi))
        assertEquals(-1, ClockMath.dayDelta(at(22, 2, 0), sf, hanoi))
    }

    @Test fun stopwatchFormat() {
        assertEquals("00:00,00", ClockMath.stopwatch(0))
        assertEquals("01:24,37", ClockMath.stopwatch(84_370))
        assertEquals("1:00:00,05", ClockMath.stopwatch(3_600_050))
    }

    @Test fun countdownFormat() {
        assertEquals("09:42", ClockMath.countdown(582_000))
        assertEquals("00:01", ClockMath.countdown(400))
        assertEquals("00:00", ClockMath.countdown(-5))
        assertEquals("1:00:00", ClockMath.countdown(3_600_000))
    }

    @Test fun sleepAcrossMidnight() {
        assertEquals(450, ClockMath.sleepMinutes(23 * 60, 6 * 60 + 30))
        assertEquals(60, ClockMath.sleepMinutes(60, 120))
    }

    @Test fun bestWorstLaps() {
        assertEquals(-1 to -1, ClockMath.bestWorst(listOf(5L)))
        assertEquals(2 to 0, ClockMath.bestWorst(listOf(34_770L, 31_080L, 18_520L)))
    }
}

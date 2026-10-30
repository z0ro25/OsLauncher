package com.ezla.oslauncher.clock.ui

import com.ezla.oslauncher.appbase.R as AppBaseR
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.ezla.oslauncher.clock.R
import com.ezla.oslauncher.clock.core.ClockMath
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private fun View.unit(design: Float) = min(width, height) / design

/** Mặt bấm giờ analog Figma (khung 280): 240 vạch/60 giây, số 5..60, kim cam. */
class StopwatchDialView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var elapsedMs = 0L
        set(value) { field = value; invalidate() }

    private val orange = ContextCompat.getColor(context, R.color.osclock_orange)
    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val hand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = orange; strokeCap = Paint.Cap.ROUND
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = orange }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, AppBaseR.font.appbase_sf_pro_regular)
    }

    override fun onDraw(canvas: Canvas) {
        val u = unit(280f)
        val cx = width / 2f
        val cy = height / 2f
        tick.strokeWidth = 1.4f * u
        for (i in 0 until 240) {
            val (len, color) = when {
                i % 20 == 0 -> 14f to Color.WHITE
                i % 4 == 0 -> 9f to Color.WHITE
                else -> 5f to 0x66FFFFFF
            }
            tick.color = color
            canvas.save()
            canvas.rotate(i * 1.5f, cx, cy)
            canvas.drawLine(cx, cy - 130f * u, cx, cy - (130f - len) * u, tick)
            canvas.restore()
        }
        text.textSize = 22f * u
        val fm = text.fontMetrics
        for (n in 1..12) {
            val a = Math.toRadians(n * 30.0)
            canvas.drawText((n * 5).toString(), cx + (98f * u * sin(a)).toFloat(),
                cy - (98f * u * cos(a)).toFloat() - (fm.ascent + fm.descent) / 2f, text)
        }
        // Figma: tâm chữ thời gian ở cy+53 (khung 280); trước vẽ baseline cy+82 nên bị sát đáy mặt số.
        text.textSize = 18f * u
        val tm = text.fontMetrics
        canvas.drawText(ClockMath.stopwatch(elapsedMs), cx, cy + 53f * u - (tm.ascent + tm.descent) / 2f, text)

        val a = Math.toRadians((elapsedMs % 60_000L) / 60_000.0 * 360.0)
        hand.strokeWidth = 2.5f * u
        canvas.drawLine(cx, cy, cx + (130f * u * sin(a)).toFloat(), cy - (130f * u * cos(a)).toFloat(), hand)
        canvas.drawCircle(cx, cy, 7f * u, dot)
    }
}

/** Vòng tiến độ hẹn giờ 64dp: nền #3a3a3c, phần còn lại cam, nét 4. */
class TimerRingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    /** 0..1 phần thời gian còn lại. */
    var fraction = 1f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = ContextCompat.getColor(context, R.color.osclock_ring_track)
    }
    private val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.osclock_orange)
    }
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        val u = unit(64f)
        bg.strokeWidth = 4f * u
        fg.strokeWidth = 4f * u
        val inset = 6f * u
        rect.set(inset, inset, width - inset, height - inset)
        canvas.drawOval(rect, bg)
        canvas.drawArc(rect, -90f, -360f * fraction, false, fg)
    }
}

/**
 * Vòng lịch ngủ Figma "Sleep Schedule" (khung 240): mặt 24 giờ, cung cam từ giờ ngủ tới giờ
 * dậy, kéo 2 đầu cung (bước 5 phút) để đổi giờ.
 */
class SleepDialView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var bedMinutes = 23 * 60
        private set
    var wakeMinutes = 6 * 60 + 30
        private set

    var onChanged: ((bed: Int, wake: Int) -> Unit)? = null

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = ContextCompat.getColor(context, R.color.osclock_glass_solid)
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.osclock_orange)
    }
    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = ContextCompat.getColor(context, R.color.osclock_gray)
    }
    private val knob = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1C1C1E.toInt() }
    private val rect = RectF()
    private var dragging = 0 // 1 = bed, 2 = wake

    fun set(bed: Int, wake: Int) {
        bedMinutes = bed
        wakeMinutes = wake
        invalidate()
    }

    private fun angleOf(min: Int) = min / 1440f * 360f // 0 = đỉnh, chiều kim đồng hồ

    override fun onDraw(canvas: Canvas) {
        val u = unit(240f)
        val cx = width / 2f
        val cy = height / 2f
        val r = 100f * u
        rect.set(cx - r, cy - r, cx + r, cy + r)
        track.strokeWidth = 40f * u
        canvas.drawOval(rect, track)
        arc.strokeWidth = 34f * u
        val start = angleOf(bedMinutes)
        val sweep = ClockMath.sleepMinutes(bedMinutes, wakeMinutes) / 1440f * 360f
        canvas.drawArc(rect, start - 90f, sweep, false, arc)
        // 2 núm ở đầu/cuối cung để người dùng biết kéo được.
        for (m in intArrayOf(bedMinutes, wakeMinutes)) {
            val a = Math.toRadians(angleOf(m).toDouble())
            canvas.drawCircle(cx + (r * sin(a)).toFloat(), cy - (r * cos(a)).toFloat(), 12f * u, knob)
        }
        for (i in 0 until 48) {
            val major = i % 4 == 0
            tick.strokeWidth = (if (major) 2f else 1.5f) * u
            tick.alpha = if (major) 255 else 150
            canvas.save()
            canvas.rotate(i * 7.5f, cx, cy)
            canvas.drawLine(cx, cy - 72f * u, cx, cy - (72f - if (major) 8f else 4f) * u, tick)
            canvas.restore()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val cx = width / 2f
        val cy = height / 2f
        val ang = (Math.toDegrees(atan2((e.x - cx).toDouble(), (cy - e.y).toDouble())).toFloat() + 360f) % 360f
        val minutes = ((ang / 360f * 1440f) / 5f).roundToInt() * 5 % 1440
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val u = unit(240f)
                if (hypot(e.x - cx, e.y - cy) < 70f * u) return false
                dragging = if (circDist(minutes, bedMinutes) <= circDist(minutes, wakeMinutes)) 1 else 2
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> if (dragging != 0) {
                if (dragging == 1) bedMinutes = minutes else wakeMinutes = minutes
                onChanged?.invoke(bedMinutes, wakeMinutes)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragging = 0
        }
        return true
    }

    private fun circDist(a: Int, b: Int): Int {
        val d = kotlin.math.abs(a - b) % 1440
        return min(d, 1440 - d)
    }
}

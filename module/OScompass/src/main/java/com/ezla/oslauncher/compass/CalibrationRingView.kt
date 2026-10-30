package com.ezla.oslauncher.compass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// Vòng hiệu chỉnh Figma "Compass › Calibrate" (khung 260): 60 vạch, bi đỏ lăn qua vạch nào thì
// vạch đó sáng trắng; đủ 60 vạch = 100%.
class CalibrationRingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var onProgress: ((Int) -> Unit)? = null

    private val visited = BooleanArray(TICKS)
    private var ballAngle = 225f
    private var hasBall = false

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.compass_red)
    }

    val progress: Int get() = visited.count { it } * 100 / TICKS

    fun reset() {
        visited.fill(false)
        hasBall = false
        onProgress?.invoke(0)
        invalidate()
    }

    /** [angle] tính theo chiều kim đồng hồ từ đỉnh. Lăn nhanh vẫn tô kín các vạch đã đi qua. */
    fun moveBall(angle: Float) {
        val target = HeadingMath.normalize(angle)
        if (hasBall) {
            var diff = target - ballAngle
            if (diff > 180f) diff -= 360f else if (diff < -180f) diff += 360f
            val steps = (kotlin.math.abs(diff) / STEP).toInt()
            for (s in 0..steps) mark(ballAngle + diff * s / (steps.coerceAtLeast(1)))
        }
        mark(target)
        ballAngle = target
        hasBall = true
        onProgress?.invoke(progress)
        invalidate()
    }

    private fun mark(angle: Float) {
        visited[(HeadingMath.normalize(angle) / STEP).roundToInt() % TICKS] = true
    }

    override fun onDraw(canvas: Canvas) {
        val u = min(width, height) / DESIGN_SIZE
        val cx = width / 2f
        val cy = height / 2f
        tickPaint.strokeWidth = 4f * u
        for (i in 0 until TICKS) {
            tickPaint.color = if (visited[i]) Color.WHITE else UNVISITED
            canvas.save()
            canvas.rotate(i * STEP, cx, cy)
            canvas.drawLine(cx, cy - TICK_OUTER * u, cx, cy - (TICK_OUTER - 12f) * u, tickPaint)
            canvas.restore()
        }
        val a = Math.toRadians(ballAngle.toDouble())
        canvas.drawCircle(
            cx + (BALL_RADIUS_PATH * u * sin(a)).toFloat(),
            cy - (BALL_RADIUS_PATH * u * cos(a)).toFloat(),
            17f * u, ballPaint
        )
    }

    private companion object {
        const val TICKS = 60
        const val STEP = 360f / TICKS
        const val DESIGN_SIZE = 260f
        const val TICK_OUTER = 122f
        const val BALL_RADIUS_PATH = 116f
        const val UNVISITED = 0x4DFFFFFF
    }
}

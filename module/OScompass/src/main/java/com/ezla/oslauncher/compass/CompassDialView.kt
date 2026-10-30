package com.ezla.oslauncher.compass

import com.ezla.oslauncher.appbase.R as AppBaseR
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

// Mặt la bàn Figma "Compass › Heading" (khung 312): vạch 2°, vạch 10° dài hơn, 30° đậm + nhãn.
// Vạch quay theo -heading; chữ tính vị trí riêng để luôn đứng thẳng như ảnh thiết kế.
class CompassDialView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var heading = 0f
        set(value) { field = value; invalidate() }

    /** Chữ N, E, S, W đã localize (string-array compass_cardinals). */
    var cardinals: Array<String> = arrayOf("N", "E", "S", "W")
        set(value) { field = value; invalidate() }

    // Vector "lên trên" (đã chuẩn hoá) chiếu lên mặt máy: bọt thuỷ chạy về phía cạnh cao hơn.
    private var levelX = 0f
    private var levelY = 0f

    private val red = ContextCompat.getColor(context, R.color.compass_red)

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }
    private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.compass_crosshair)
    }
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.compass_bubble)
    }
    private val numberPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(context, R.color.compass_label)
        typeface = ResourcesCompat.getFont(context, AppBaseR.font.appbase_sf_pro_regular)
    }
    private val cardinalPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, AppBaseR.font.appbase_sf_pro_bold)
    }

    fun setLevel(x: Float, y: Float) {
        levelX = x
        levelY = y
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val u = min(width, height) / DESIGN_SIZE
        val cx = width / 2f
        val cy = height / 2f

        canvas.save()
        canvas.rotate(-heading, cx, cy)
        for (i in 0 until 180) {
            val deg = i * 2
            val len: Float
            when {
                deg % 30 == 0 -> { len = 19f; tickPaint.strokeWidth = 2.5f * u; tickPaint.color = Color.WHITE }
                deg % 10 == 0 -> { len = 12f; tickPaint.strokeWidth = 1.25f * u; tickPaint.color = Color.WHITE }
                else -> { len = 7f; tickPaint.strokeWidth = 1.25f * u; tickPaint.color = MINOR_TICK }
            }
            if (deg == 0) tickPaint.color = red
            canvas.save()
            canvas.rotate(deg.toFloat(), cx, cy)
            canvas.drawLine(cx, cy - TICK_OUTER * u, cx, cy - (TICK_OUTER - len) * u, tickPaint)
            canvas.restore()
        }
        canvas.restore()

        numberPaint.textSize = 13.5f * u
        cardinalPaint.textSize = 27f * u
        for (deg in 0 until 360 step 30) {
            val cardinal = deg % 90 == 0
            val paint = if (cardinal) cardinalPaint else numberPaint
            val r = if (cardinal) CARDINAL_RADIUS else NUMBER_RADIUS
            if (cardinal) cardinalPaint.color = if (deg == 0) red else Color.WHITE
            val a = Math.toRadians((deg - heading).toDouble())
            val x = cx + (r * u * sin(a)).toFloat()
            val y = cy - (r * u * cos(a)).toFloat()
            val fm = paint.fontMetrics
            val text = if (cardinal) cardinals.getOrElse(deg / 90) { "" } else deg.toString()
            canvas.drawText(text, x, y - (fm.ascent + fm.descent) / 2f, paint)
        }

        // Vạch chỉ hướng cố định ở đỉnh (Figma: y 2..33 trong khung 312).
        indicatorPaint.strokeWidth = 4.16f * u
        canvas.drawLine(cx, cy - 154f * u, cx, cy - 123f * u, indicatorPaint)

        // Bọt thuỷ: lệch tối đa BUBBLE_TRAVEL, bão hoà khi máy nghiêng khoảng 20°.
        var dx = levelX * LEVEL_GAIN
        var dy = levelY * LEVEL_GAIN
        val mag = hypot(dx, dy)
        if (mag > 1f) { dx /= mag; dy /= mag }
        canvas.drawCircle(cx + dx * BUBBLE_TRAVEL * u, cy + dy * BUBBLE_TRAVEL * u, 40f * u, bubblePaint)

        crossPaint.strokeWidth = u
        canvas.drawLine(cx - 66f * u, cy, cx + 66f * u, cy, crossPaint)
        canvas.drawLine(cx, cy - 66.5f * u, cx, cy + 66.5f * u, crossPaint)
    }

    private companion object {
        const val DESIGN_SIZE = 312f
        const val TICK_OUTER = 145.5f
        const val NUMBER_RADIUS = 113f
        const val CARDINAL_RADIUS = 103f
        const val BUBBLE_TRAVEL = 50f
        const val LEVEL_GAIN = 3f
        const val MINOR_TICK = 0x8CFFFFFF.toInt()
    }
}

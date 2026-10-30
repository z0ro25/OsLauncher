package com.ezla.oslauncher.measure.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.ezla.oslauncher.measure.R
import com.ezla.oslauncher.measure.ar.MeasureFrame
import com.ezla.oslauncher.measure.ar.ScreenLine
import com.ezla.oslauncher.measure.ar.ScreenPoint
import com.ezla.oslauncher.measure.data.MeasureFormat
import kotlin.math.hypot

/** Lớp vẽ đè camera (Figma Measure): đường đo, nhãn, thước kẻ khi lại gần, tâm ngắm. */
class MeasureOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var frame = MeasureFrame(tracking = false)

    private val white = ContextCompat.getColor(context, R.color.measure_white)
    private val yellow = ContextCompat.getColor(context, R.color.measure_yellow)
    private val res = context.resources
    private val density = res.displayMetrics.density

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = res.getDimension(R.dimen.measure_line_width)
        strokeCap = Paint.Cap.ROUND
        color = white
    }
    private val dashPaint = Paint(linePaint).apply {
        pathEffect = DashPathEffect(floatArrayOf(2 * density, 9 * density), 0f)
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2 * density
        strokeCap = Paint.Cap.ROUND
        color = white
    }
    private val tickText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        textSize = 12 * res.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
        typeface = font(com.ezla.oslauncher.appbase.R.font.appbase_sf_pro_regular)
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white }
    private val reticlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = res.getDimension(R.dimen.measure_reticle_stroke)
    }
    private val labelBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white }
    private val labelText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.measure_bg)
        textSize = res.getDimension(R.dimen.measure_label_text)
        textAlign = Paint.Align.CENTER
        typeface = font(com.ezla.oslauncher.appbase.R.font.appbase_sf_pro_semibold)
    }
    private val dotRadius = res.getDimension(R.dimen.measure_dot_radius)
    private val reticleRadius = res.getDimension(R.dimen.measure_reticle_radius)
    private val reticleDot = res.getDimension(R.dimen.measure_reticle_dot)
    private val labelHeight = res.getDimension(R.dimen.measure_label_height)
    private val labelMinWidth = res.getDimension(R.dimen.measure_label_min_width)
    private val labelPad = 16 * density
    private val rect = RectF()
    private val labelHits = ArrayList<Pair<RectF, ScreenLine>>()

    private fun font(id: Int) = runCatching { ResourcesCompat.getFont(context, id) }.getOrNull()

    internal fun update(f: MeasureFrame) {
        frame = f
        invalidate()
    }

    /** Nhãn đoạn đã đo nằm dưới điểm chạm (mở thẻ chi tiết), null nếu không trúng nhãn nào. */
    internal fun lineAt(x: Float, y: Float): ScreenLine? =
        labelHits.lastOrNull { it.first.contains(x, y) && it.second in frame.lines }?.second

    /** Có đoạn đo nào, và có đoạn nào đang hiện thước không (cho chữ "Move closer to see ruler"). */
    internal val hasLines get() = frame.lines.isNotEmpty() || frame.liveLine != null
    internal val showingRuler get() = frame.lines.any { it.nearRuler } || frame.liveLine?.nearRuler == true

    override fun onDraw(canvas: Canvas) {
        labelHits.clear()
        if (!frame.tracking) return
        frame.lines.forEach { drawLine(canvas, it, true) }
        frame.liveLine?.let { drawLine(canvas, it, false) }
        // Chưa có điểm dưới tâm: chỉ chấm điểm đầu, nét đứt hướng về tâm ngắm (Figma "Point Added").
        if (frame.liveLine == null) frame.pendingPoint?.let { p ->
            canvas.drawLine(p.x, p.y, width / 2f, height / 2f, dashPaint)
            canvas.drawCircle(p.x, p.y, dotRadius, dotPaint)
        }
        drawReticle(canvas)
        frame.lines.forEach { drawLabel(canvas, it) }
        frame.liveLine?.let { drawLabel(canvas, it) }
    }

    private fun drawLine(canvas: Canvas, l: ScreenLine, endDot: Boolean) {
        val a = l.a ?: return
        val b = l.b ?: return
        canvas.drawLine(a.x, a.y, b.x, b.y, linePaint)
        canvas.drawCircle(a.x, a.y, dotRadius, dotPaint)
        if (endDot) canvas.drawCircle(b.x, b.y, dotRadius, dotPaint)
        if (l.nearRuler) drawRuler(canvas, a, b, l.meters)
    }

    // Figma "Ruler": vạch mỗi 1 cm về một phía đường đo, vạch dài + số mỗi 25 cm.
    private fun drawRuler(canvas: Canvas, a: ScreenPoint, b: ScreenPoint, meters: Float) {
        val totalCm = (meters * 100f).toInt()
        if (totalCm <= 0) return
        val len = hypot(b.x - a.x, b.y - a.y)
        if (len < 1f) return
        val ux = (b.x - a.x) / len
        val uy = (b.y - a.y) / len
        // Pháp tuyến hướng lên trên màn hình để số không bị đè dưới đường.
        var nx = uy
        var ny = -ux
        if (ny > 0) { nx = -nx; ny = -ny }
        val pxPerCm = len / (meters * 100f)
        val step = if (pxPerCm >= MIN_TICK_GAP * density) 1 else 5
        var cm = step
        while (cm < totalCm) {
            val t = cm * pxPerCm
            val x = a.x + ux * t
            val y = a.y + uy * t
            val major = cm % 25 == 0
            val tick = (if (major) 12f else 6f) * density
            canvas.drawLine(x, y, x + nx * tick, y + ny * tick, tickPaint)
            if (major) {
                val off = 24f * density
                canvas.drawText(cm.toString(), x + nx * off, y + ny * off - (tickText.ascent() + tickText.descent()) / 2f, tickText)
            }
            cm += step
        }
    }

    private fun drawReticle(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        // Tâm ngắm vàng khi đang kéo đoạn thẳng (Figma "Measuring"), trắng khi chờ chấm điểm.
        val color = if (frame.hasPending && frame.liveLine != null) yellow else white
        reticlePaint.color = color
        dotPaint.color = color
        canvas.drawCircle(cx, cy, reticleRadius, reticlePaint)
        if (frame.hasSurface) canvas.drawCircle(cx, cy, reticleDot, dotPaint)
        dotPaint.color = white
    }

    private fun drawLabel(canvas: Canvas, l: ScreenLine) {
        val a = l.a ?: return
        val b = l.b ?: return
        val text = MeasureFormat.cm(l.meters)
        val w = maxOf(labelMinWidth, labelText.measureText(text) + labelPad * 2)
        val mid = ScreenPoint((a.x + b.x) / 2f, (a.y + b.y) / 2f)
        rect.set(mid.x - w / 2f, mid.y - labelHeight / 2f, mid.x + w / 2f, mid.y + labelHeight / 2f)
        canvas.drawRoundRect(rect, labelHeight / 2f, labelHeight / 2f, labelBg)
        labelHits.add(RectF(rect) to l)
        val baseline = mid.y - (labelText.descent() + labelText.ascent()) / 2f
        canvas.drawText(text, mid.x, baseline, labelText)
    }

    private companion object {
        // Vạch 1 cm cách nhau dưới 4dp thì gộp thành mỗi 5 cm cho khỏi dính.
        const val MIN_TICK_GAP = 4f
    }
}

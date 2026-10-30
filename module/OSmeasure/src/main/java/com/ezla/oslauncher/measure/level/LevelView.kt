package com.ezla.oslauncher.measure.level

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.ezla.oslauncher.measure.R
import kotlin.math.abs
import kotlin.math.roundToInt

/** Tab Level (Figma "Level / Level 0° / Level Edge"): 2 vòng tròn lệch nhau khi nằm ngửa, đường chân trời khi dựng đứng. */
class LevelView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var reading = LevelSensor.LevelReading(LevelSensor.Mode.FLAT, 0f, 0f, 0f)

    private val black = ContextCompat.getColor(context, R.color.measure_bg)
    private val white = ContextCompat.getColor(context, R.color.measure_white)
    private val green = ContextCompat.getColor(context, R.color.measure_level_green)
    private val upper = ContextCompat.getColor(context, R.color.measure_level_upper)
    private val discZero = ContextCompat.getColor(context, R.color.measure_level_disc_zero)
    private val density = resources.displayMetrics.density
    private val radius = resources.getDimension(R.dimen.measure_level_radius)

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3 * density
        color = white
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = resources.getDimension(R.dimen.measure_level_text)
        textAlign = Paint.Align.CENTER
        typeface = runCatching {
            ResourcesCompat.getFont(context, com.ezla.oslauncher.appbase.R.font.appbase_sf_pro_light)
        }.getOrNull()
    }
    private val clip = Path()

    fun update(r: LevelSensor.LevelReading) {
        reading = r
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val deg = abs(reading.degrees).roundToInt()
        val label = "$deg°"
        if (reading.mode == LevelSensor.Mode.FLAT) drawFlat(canvas, deg, label) else drawEdge(canvas, deg, label)
    }

    // Figma: tâm khung 2 vòng ở y=340/800; lệch 25dp mỗi vòng ứng với 4°.
    private fun drawFlat(canvas: Canvas, deg: Int, label: String) {
        val cx = width / 2f
        val cy = height * FLAT_CENTER_Y
        val level = deg == 0
        canvas.drawColor(if (level) green else black)
        val k = (OFFSET_PER_TILT * density)
        val dx = (-reading.tiltX * k).coerceIn(-MAX_OFFSET * density, MAX_OFFSET * density)
        val dy = (reading.tiltY * k).coerceIn(-MAX_OFFSET * density, MAX_OFFSET * density)
        val fx = if (level) cx else cx + dx
        val fy = if (level) cy else cy + dy
        canvas.drawCircle(if (level) cx else cx - dx, if (level) cy else cy - dy, radius, ring)
        fill.color = if (level) discZero else white
        canvas.drawCircle(fx, fy, radius, fill)

        // Chữ đen trong đĩa trắng, trắng ngoài đĩa: vẽ 2 lần với vùng clip ngược nhau.
        val baseline = cy - (text.descent() + text.ascent()) / 2f
        clip.reset()
        clip.addCircle(fx, fy, radius, Path.Direction.CW)
        if (level) {
            text.color = black
            canvas.drawText(label, cx, baseline, text)
            return
        }
        canvas.save()
        canvas.clipPath(clip)
        text.color = black
        canvas.drawText(label, cx, baseline, text)
        canvas.restore()
        canvas.save()
        clipOut(canvas, clip)
        text.color = white
        canvas.drawText(label, cx, baseline, text)
        canvas.restore()
    }

    // Figma "Level Edge": nửa trên #1c1c1e xoay theo góc nghiêng, chữ trắng giữa màn.
    private fun drawEdge(canvas: Canvas, deg: Int, label: String) {
        val cx = width / 2f
        val cy = height / 2f
        val level = deg == 0
        canvas.drawColor(if (level) green else black)
        if (!level) {
            canvas.save()
            canvas.rotate(reading.degrees, cx, cy)
            fill.color = upper
            val ext = maxOf(width, height).toFloat()
            canvas.drawRect(cx - ext, cy - ext * 2, cx + ext, cy + EDGE_HORIZON_OFFSET * density, fill)
            canvas.restore()
        }
        text.color = if (level) black else white
        val baseline = cy - (text.descent() + text.ascent()) / 2f - EDGE_TEXT_LIFT * density
        canvas.drawText(label, cx, baseline, text)
    }

    @Suppress("DEPRECATION")
    private fun clipOut(canvas: Canvas, path: Path) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) canvas.clipOutPath(path)
        else canvas.clipPath(path, android.graphics.Region.Op.DIFFERENCE)
    }

    private companion object {
        const val FLAT_CENTER_Y = 340f / 800f
        // sin(4°) ≈ 0.07 -> 25dp: ~360dp mỗi đơn vị trọng lực chuẩn hoá.
        const val OFFSET_PER_TILT = 360f
        const val MAX_OFFSET = 120f
        const val EDGE_HORIZON_OFFSET = 40f
        const val EDGE_TEXT_LIFT = 32f
    }
}

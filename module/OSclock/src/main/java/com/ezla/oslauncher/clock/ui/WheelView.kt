package com.ezla.oslauncher.clock.ui

import com.ezla.oslauncher.appbase.R as AppBaseR
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import androidx.core.content.res.ResourcesCompat
import com.ezla.oslauncher.clock.R
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

// Cột bánh xe kiểu iOS (Figma "Wheel Picker"): giữa 22sp trắng, ±1 hàng 19sp a0.45,
// ±2 hàng 16sp a0.20. Dải nền chọn vẽ ở view cha. [cyclic] = cuộn vòng (giờ/phút).
class WheelView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var items: List<String> = emptyList()
        set(value) { field = value; offset = selected.toFloat() * rowPx; invalidate() }

    var cyclic = true

    /** Căn chữ trong cột: -1 trái, 0 giữa, 1 phải. */
    var align = 0

    var onSelected: ((Int) -> Unit)? = null

    private val rowPx = resources.displayMetrics.density * ROW_DP
    private var offset = 0f
    private val scroller = OverScroller(context)
    private var lastNotified = -1
    // Tự theo dõi ngón tay: view không setPressed nên isPressed luôn false, computeScroll snap giữa lúc kéo.
    private var touching = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = ResourcesCompat.getFont(context, AppBaseR.font.appbase_sf_pro_regular)
    }

    val selected: Int
        get() = if (items.isEmpty()) 0 else wrap((offset / rowPx).roundToInt())

    fun setSelected(index: Int, notify: Boolean = false) {
        scroller.forceFinished(true)
        offset = index * rowPx
        lastNotified = index
        invalidate()
        if (notify) onSelected?.invoke(index)
    }

    private fun wrap(i: Int): Int {
        val n = items.size
        return if (cyclic) ((i % n) + n) % n else i.coerceIn(0, n - 1)
    }

    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            scroller.forceFinished(true)
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            offset = clamp(offset + dy)
            notifyIfChanged()
            invalidate()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            // Lấy điểm dừng tự nhiên của fling rồi chạy lại tới đúng 1 hàng (không phải snap lần 2).
            scroller.fling(0, offset.toInt(), 0, -vy.toInt(), 0, 0, Int.MIN_VALUE / 2, Int.MAX_VALUE / 2)
            val natural = scroller.finalY.toFloat()
            scroller.forceFinished(true)
            val target = clamp((natural / rowPx).roundToInt() * rowPx)
            scroller.startScroll(0, offset.toInt(), 0, (target - offset).toInt(), FLING_MS)
            postInvalidateOnAnimation()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val rows = ((e.y - height / 2f) / rowPx).roundToInt()
            if (rows != 0) smoothTo(offset + rows * rowPx)
            return true
        }
    })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (items.isEmpty()) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> touching = true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> touching = false
        }
        val handled = detector.onTouchEvent(event)
        if ((event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) && scroller.isFinished) {
            smoothTo(offset)
        }
        return handled || super.onTouchEvent(event)
    }

    private fun smoothTo(raw: Float) {
        val target = clamp((raw / rowPx).roundToInt() * rowPx)
        scroller.startScroll(0, offset.toInt(), 0, (target - offset).toInt(), 220)
        postInvalidateOnAnimation()
    }

    private fun clamp(v: Float) = if (cyclic) v else v.coerceIn(0f, (items.size - 1) * rowPx)

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            offset = scroller.currY.toFloat()
            notifyIfChanged()
            postInvalidateOnAnimation()
        } else if (abs(offset - (offset / rowPx).roundToInt() * rowPx) > 0.5f && !touching) {
            offset = (offset / rowPx).roundToInt() * rowPx
            notifyIfChanged()
            invalidate()
        }
    }

    private fun notifyIfChanged() {
        val s = selected
        if (s != lastNotified) {
            lastNotified = s
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onSelected?.invoke(s)
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (items.isEmpty()) return
        val cy = height / 2f
        val first = floor(offset / rowPx).toInt() - 3
        for (i in first..first + 7) {
            if (!cyclic && (i < 0 || i >= items.size)) continue
            val dist = (i * rowPx - offset) / rowPx // -3..3, 0 = giữa
            val ad = abs(dist)
            if (ad > 2.6f) continue
            val size = when {
                ad <= 1f -> 22f - 3f * ad
                else -> 19f - 3f * (ad - 1f)
            }
            val alpha = when {
                ad <= 1f -> 1f - 0.55f * ad
                else -> (0.45f - 0.25f * (ad - 1f)).coerceAtLeast(0f)
            }
            paint.textSize = size * resources.displayMetrics.scaledDensity
            paint.alpha = (alpha * 255).toInt()
            // Hàng xa nén lại một chút (Figma: 36dp rồi ~32dp) cho cảm giác cong.
            val y = cy + dist * rowPx * (if (ad > 1f) 0.94f else 1f)
            val text = items[wrap(i)]
            val w = paint.measureText(text)
            val x = when (align) {
                -1 -> paddingLeft.toFloat()
                1 -> width - paddingRight - w
                else -> (width - w) / 2f
            }
            val fm = paint.fontMetrics
            canvas.drawText(text, x, y - (fm.ascent + fm.descent) / 2f, paint)
        }
    }

    companion object {
        const val ROW_DP = 36f
        private const val FLING_MS = 600
    }
}

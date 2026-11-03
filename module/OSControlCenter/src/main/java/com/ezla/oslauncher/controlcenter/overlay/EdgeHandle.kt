package com.ezla.oslauncher.controlcenter.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.PointF
import android.graphics.PorterDuff
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.ezla.oslauncher.controlcenter.R
import com.ezla.oslauncher.controlcenter.data.CcEdge
import kotlin.math.abs

// Thanh kéo ở mép màn: vùng chạm rộng hơn phần vẽ. Kéo vào trong quá ngưỡng thì gọi [onOpen],
// còn lại (chạm / vuốt hướng khác) báo [onPassThrough] để phát lại xuống app bên dưới.
@SuppressLint("ViewConstructor")
class EdgeHandle(context: Context, private val onOpen: () -> Unit) : FrameLayout(context) {

    private val bar = View(context).apply { setBackgroundResource(R.drawable.cc_bg_edge) }
    private val density = resources.displayMetrics.density
    private val trigger = 24 * density
    private var edge = CcEdge.TOP
    private var downX = 0f
    private var downY = 0f
    private var fired = false

    init {
        addView(bar)
    }

    // Đặt kiểu dáng thanh: [thickness]/[length] tính bằng px, bar nằm sát mép trong vùng chạm.
    // Mép trên/dưới: vùng chạm có thể rộng hơn thanh (cả tai thỏ), [barOffset] = lề trái của thanh.
    fun style(edge: CcEdge, thickness: Int, length: Int, color: Int, alpha: Float, barOffset: Int = 0) {
        this.edge = edge
        val lp = if (edge.isVertical) LayoutParams(thickness, length) else LayoutParams(length, thickness)
        lp.gravity = when (edge) {
            CcEdge.LEFT -> Gravity.START or Gravity.CENTER_VERTICAL
            CcEdge.RIGHT -> Gravity.END or Gravity.CENTER_VERTICAL
            CcEdge.TOP -> Gravity.TOP or Gravity.START
            CcEdge.BOTTOM -> Gravity.BOTTOM or Gravity.START
        }
        if (!edge.isVertical) lp.leftMargin = barOffset.coerceAtLeast(0)
        bar.layoutParams = lp
        bar.background.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN)
        bar.alpha = alpha
    }

    // Ghi lại đường đi ngón tay (toạ độ màn hình); không phải cử chỉ mở thì trả cho app bên dưới.
    var onPassThrough: ((points: List<PointF>, durationMs: Long) -> Unit)? = null
    private val points = ArrayList<PointF>()
    private var downTime = 0L

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.rawX
                downY = e.rawY
                downTime = e.eventTime
                fired = false
                points.clear()
                points += PointF(e.rawX, e.rawY)
            }
            MotionEvent.ACTION_MOVE -> if (!fired) {
                points += PointF(e.rawX, e.rawY)
                val dx = e.rawX - downX
                val dy = e.rawY - downY
                val inward = when (edge) {
                    CcEdge.TOP -> dy
                    CcEdge.BOTTOM -> -dy
                    CcEdge.LEFT -> dx
                    CcEdge.RIGHT -> -dx
                }
                val cross = if (edge.isVertical) abs(dy) else abs(dx)
                if (inward > trigger && inward > cross) {
                    fired = true
                    onOpen()
                }
            }
            MotionEvent.ACTION_UP -> if (!fired) {
                points += PointF(e.rawX, e.rawY)
                onPassThrough?.invoke(points.toList(), (e.eventTime - downTime).coerceAtLeast(1))
            }
        }
        return true
    }

    companion object {
        const val TAG = "CcEdge"

        fun params(): WindowManager.LayoutParams = WindowManager.LayoutParams(
            0, 0,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }
}

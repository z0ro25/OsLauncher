package com.ezla.oslauncher.controlcenter.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.ezla.oslauncher.controlcenter.R
import com.ezla.oslauncher.controlcenter.skin.CcAccent

// Thanh trượt dọc bo tròn kiểu iOS (Figma "Control Tile / Slider" 65x142): phần sáng dâng từ đáy.
// Kéo tương đối theo vị trí chạm đầu như iOS, không nhảy tới điểm chạm.
class CcVerticalSlider @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var value = 0.5f
        private set

    var icon: Drawable? = null
        set(v) {
            field = v?.mutate()
            invalidate()
        }

    // Ảnh nền ô 1x2 của theme vẽ thay nền kính (vẫn cắt theo khung bo tròn); null = mặc định.
    var skin: Drawable? = null
        set(v) {
            field = v
            invalidate()
        }

    // Màu phần sáng theo theme (1-3 màu, gradient chéo); null = trắng ngà mặc định.
    var accent: CcAccent? = null
        set(v) {
            field = v
            applyAccent()
            invalidate()
        }

    var onValueChanged: ((Float, fromUser: Boolean) -> Unit)? = null
    // Chạm khi chưa có quyền (vd WRITE_SETTINGS cho độ sáng): báo ra ngoài thay vì kéo.
    var isLocked = false
    var onLockedTouch: (() -> Unit)? = null

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.cc_tile_bg) }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.cc_slider_fill) }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density
        color = ContextCompat.getColor(context, R.color.cc_tile_stroke)
    }
    private val clip = Path()
    private val rect = RectF()
    private var downY = 0f
    private var downValue = 0f
    // Đang kéo thì bỏ qua cập nhật từ hệ thống (observer độ sáng bắn ngược làm thanh giật).
    private var tracking = false

    fun setValue(v: Float, fromUser: Boolean = false) {
        if (tracking && !fromUser) return
        val nv = v.coerceIn(0f, 1f)
        if (nv == value) return
        value = nv
        invalidate()
        onValueChanged?.invoke(nv, fromUser)
    }

    private fun applyAccent() {
        val a = accent
        if (a == null) {
            fillPaint.shader = null
            fillPaint.color = ContextCompat.getColor(context, R.color.cc_slider_fill)
        } else {
            a.applyTo(fillPaint, width.toFloat(), height.toFloat())
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        applyAccent()
        val r = w / 2f
        clip.reset()
        clip.addRoundRect(rect, r, r, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        val r = width / 2f
        canvas.save()
        canvas.clipPath(clip)
        val bg = skin
        if (bg != null) {
            bg.setBounds(0, 0, width, height)
            bg.draw(canvas)
        } else {
            canvas.drawRect(rect, bgPaint)
        }
        canvas.drawRect(0f, height * (1 - value), width.toFloat(), height.toFloat(), fillPaint)
        canvas.restore()
        canvas.drawRoundRect(rect, r, r, strokePaint)
        icon?.let { d ->
            val size = (width * 0.4f).toInt()
            val left = (width - size) / 2
            val top = height - size - (width * 0.25f).toInt()
            d.setBounds(left, top, left + size, top + size)
            // Icon nằm trên phần sáng thì theo màu tương phản của phần đó, trên nền tối thì trắng.
            val onFill = top + size / 2 > height * (1 - value)
            val fillIcon = accent?.iconColor ?: 0xFF000000.toInt()
            d.setTint(if (onFill) fillIcon else 0xFFFFFFFF.toInt())
            d.draw(canvas)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (isLocked) {
            if (e.action == MotionEvent.ACTION_UP) onLockedTouch?.invoke()
            return true
        }
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                tracking = true
                downY = e.y
                downValue = value
            }
            MotionEvent.ACTION_MOVE -> setValue(downValue + (downY - e.y) / height, true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                tracking = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }
}

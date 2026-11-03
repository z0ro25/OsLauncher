package com.ezla.oslauncher.controlcenter.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

// Vùng chọn Saturation (ngang) x Value (dọc) cho hue hiện tại (Figma "_Color" 255x190, bo 12).
class CcSatValView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {

    var hue = 0f
        set(v) {
            field = v
            shaderDirty = true
            invalidate()
        }
    var sat = 1f
        private set
    var value = 1f
        private set
    var onChanged: ((Float, Float) -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * resources.displayMetrics.density
        color = Color.WHITE
    }
    private val rect = RectF()
    private val clip = Path()
    private var shaderDirty = true
    private val radius = 12 * resources.displayMetrics.density

    fun set(s: Float, v: Float) {
        sat = s.coerceIn(0f, 1f)
        value = v.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        clip.reset()
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW)
        shaderDirty = true
    }

    override fun onDraw(c: Canvas) {
        if (shaderDirty && width > 0) {
            val pure = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
            val horizontal = LinearGradient(0f, 0f, width.toFloat(), 0f, Color.WHITE, pure, Shader.TileMode.CLAMP)
            val vertical = LinearGradient(0f, 0f, 0f, height.toFloat(), Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)
            paint.shader = ComposeShader(horizontal, vertical, PorterDuff.Mode.SRC_OVER)
            shaderDirty = false
        }
        c.save()
        c.clipPath(clip)
        c.drawRect(rect, paint)
        c.restore()
        c.drawCircle(sat * width, (1 - value) * height, 7.5f * resources.displayMetrics.density, ring)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        set(e.x / width, 1 - e.y / height)
        onChanged?.invoke(sat, value)
        return true
    }
}

// Thanh ngang 13dp: kiểu HUE (cầu vồng) hoặc ALPHA (trong suốt -> màu hiện tại trên nền ô vuông).
class CcColorBarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {

    var isAlpha = false
    var baseColor = Color.RED
        set(v) {
            field = v
            shaderDirty = true
            invalidate()
        }
    // 0..1 vị trí núm.
    var position = 0f
        set(v) {
            field = v.coerceIn(0f, 1f)
            invalidate()
        }
    var onChanged: ((Float) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val checker = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knob = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val knobStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density
        color = 0x33000000
    }
    private val rect = RectF()
    private val clip = Path()
    private var shaderDirty = true

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        clip.reset()
        clip.addRoundRect(rect, h / 2f, h / 2f, Path.Direction.CW)
        shaderDirty = true
    }

    override fun onDraw(c: Canvas) {
        val r = height / 2f
        if (shaderDirty && width > 0) {
            paint.shader = if (isAlpha) {
                val opaque = baseColor or 0xFF000000.toInt()
                LinearGradient(0f, 0f, width.toFloat(), 0f, opaque and 0x00FFFFFF, opaque, Shader.TileMode.CLAMP)
            } else {
                val colors = IntArray(7) { Color.HSVToColor(floatArrayOf(it * 60f, 1f, 1f)) }
                LinearGradient(0f, 0f, width.toFloat(), 0f, colors, null, Shader.TileMode.CLAMP)
            }
            shaderDirty = false
        }
        if (isAlpha) drawChecker(c)
        c.drawRoundRect(rect, r, r, paint)
        val kw = 5 * density
        val x = (position * (width - kw)).coerceIn(0f, width - kw)
        c.drawRoundRect(x, -1f, x + kw, height + 1f, 2 * density, 2 * density, knob)
        c.drawRoundRect(x, -1f, x + kw, height + 1f, 2 * density, 2 * density, knobStroke)
    }

    private fun drawChecker(c: Canvas) {
        val s = 4 * density
        c.save()
        c.clipPath(clip)
        var y = 0f
        var row = 0
        while (y < height) {
            var x = 0f
            var col = row
            while (x < width) {
                checker.color = if (col % 2 == 0) 0xFFFFFFFF.toInt() else 0xFFD9D9D9.toInt()
                c.drawRect(x, y, x + s, y + s, checker)
                x += s
                col++
            }
            y += s
            row++
        }
        c.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        position = e.x / width
        onChanged?.invoke(position)
        return true
    }
}

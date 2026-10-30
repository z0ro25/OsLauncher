package com.ezla.oslauncher.clock.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.ezla.oslauncher.clock.R

// Công tắc Figma "Toggle" 52x32: bật #30d158, tắt #39393d, núm trắng 28dp.
class IosToggle @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var onChanged: ((Boolean) -> Unit)? = null

    var isOn = false
        private set

    private var progress = 0f
    private var anim: ValueAnimator? = null
    private val onColor = ContextCompat.getColor(context, R.color.osclock_green)
    private val offColor = ContextCompat.getColor(context, R.color.osclock_toggle_off)
    private val track = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumb = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        setShadowLayer(2f, 0f, 1f, 0x33000000)
    }
    private val rect = RectF()

    init {
        isClickable = true
        setOnClickListener { setOn(!isOn, animate = true, fromUser = true) }
    }

    fun setOn(on: Boolean, animate: Boolean = false, fromUser: Boolean = false) {
        if (on == isOn && anim == null) {
            progress = if (on) 1f else 0f
            invalidate()
            return
        }
        isOn = on
        anim?.cancel()
        if (animate) {
            anim = ValueAnimator.ofFloat(progress, if (on) 1f else 0f).setDuration(180).apply {
                addUpdateListener { progress = it.animatedValue as Float; invalidate() }
                start()
            }
        } else {
            progress = if (on) 1f else 0f
            invalidate()
        }
        if (fromUser) onChanged?.invoke(on)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val d = resources.displayMetrics.density
        setMeasuredDimension((52 * d).toInt(), (32 * d).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val h = height.toFloat()
        rect.set(0f, 0f, width.toFloat(), h)
        track.color = ColorUtils.blendARGB(offColor, onColor, progress)
        canvas.drawRoundRect(rect, h / 2, h / 2, track)
        val r = 14 * d
        val cx = 2 * d + r + (width - 4 * d - 2 * r) * progress
        canvas.drawCircle(cx, h / 2, r, thumb)
    }
}

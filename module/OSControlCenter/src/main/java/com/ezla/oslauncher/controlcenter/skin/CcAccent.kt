package com.ezla.oslauncher.controlcenter.skin

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.ColorUtils
import com.ezla.oslauncher.controlcenter.CcPrefs

// Màu "đang bật" của theme: 1 màu = đơn sắc, 2-3 màu = gradient chéo (trái-trên -> phải-dưới).
// Bất biến: luôn 1..3 màu. Chưa áp theme = null, mỗi chỗ giữ màu gốc của nó (trắng / xanh...).
class CcAccent private constructor(val colors: IntArray) {

    val isGradient get() = colors.size > 1

    // Icon trên nền màu: tính theo độ sáng trung bình cả dải (nền sáng -> icon đen).
    val iconColor: Int
        get() {
            val lum = colors.map { ColorUtils.calculateLuminance(it) }.average()
            return if (lum > 0.5) Color.BLACK else Color.WHITE
        }

    // Nền đặc theo hình [shape] (OVAL cho chip / ô tròn, RECTANGLE + [radius] cho viên thuốc).
    fun drawable(shape: Int = GradientDrawable.OVAL, radius: Float = 0f): Drawable =
        GradientDrawable().apply {
            this.shape = shape
            if (shape == GradientDrawable.RECTANGLE) cornerRadius = radius
            if (isGradient) {
                orientation = GradientDrawable.Orientation.TL_BR
                this.colors = this@CcAccent.colors
            } else {
                setColor(this@CcAccent.colors[0])
            }
        }

    // Gắn shader chéo vào paint cho phần tô tự vẽ (thanh trượt dọc) theo khung [w]x[h].
    fun applyTo(paint: Paint, w: Float, h: Float) {
        if (isGradient) {
            paint.shader = LinearGradient(0f, 0f, w, h, colors, null, Shader.TileMode.CLAMP)
        } else {
            paint.shader = null
            paint.color = colors[0]
        }
    }

    fun toPrefs(): String = colors.joinToString(",") { String.format("#%08X", it) }

    override fun equals(other: Any?) = other is CcAccent && other.colors.contentEquals(colors)
    override fun hashCode() = colors.contentHashCode()

    companion object {
        const val MAX_COLORS = 3
        private const val KEY = "accent_colors"

        fun of(vararg colors: Int): CcAccent? =
            if (colors.isEmpty()) null else CcAccent(colors.take(MAX_COLORS).toIntArray())

        // Nhận mã hex "#RRGGBB" / "#AARRGGBB" từ theme; mã hỏng bị bỏ qua, không còn mã nào -> null.
        fun parse(hex: List<String>?): CcAccent? {
            val list = hex.orEmpty().mapNotNull { runCatching { Color.parseColor(it.trim()) }.getOrNull() }
            return of(*list.toIntArray())
        }

        fun installed(c: Context): CcAccent? =
            CcPrefs.prefs(c).getString(KEY, null)?.let { parse(it.split(',')) }

        // Module Themes gọi khi áp theme; null = bỏ màu theme. Ghi prefs -> panel đang mở tự vẽ lại.
        fun install(c: Context, accent: CcAccent?) {
            val e = CcPrefs.prefs(c).edit()
            if (accent == null) e.remove(KEY) else e.putString(KEY, accent.toPrefs())
            e.apply()
        }
    }
}

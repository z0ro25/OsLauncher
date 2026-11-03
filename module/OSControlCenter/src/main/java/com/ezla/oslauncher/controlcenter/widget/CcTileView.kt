package com.ezla.oslauncher.controlcenter.widget

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.ezla.oslauncher.controlcenter.R
import com.ezla.oslauncher.controlcenter.skin.CcAccent
import com.ezla.oslauncher.controlcenter.skin.CcSkinSlot
import com.ezla.oslauncher.controlcenter.skin.CcSkinSource

// Ô tròn 1x1 của panel (Figma "Control Tile / Toggle"): tắt = kính tối icon trắng, bật = nền trắng icon đen
// (hoặc màu theme [accent], icon tự đen/trắng theo độ sáng).
class CcTileView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val icon = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    var isOn = false
        set(v) {
            field = v
            render()
        }

    // Slot trống ở chế độ sửa: vòng tròn mờ, không icon, không bấm. Đặt lại false khi tái dùng holder.
    var isSlot = false
        set(v) {
            field = v
            isClickable = !v
            render()
        }

    // Ảnh nền ô 1x1 của theme: chỉ thay nền lúc TẮT; bật vẫn nền trắng để còn phân biệt trạng thái.
    var skin: CcSkinSource? = null
        set(v) {
            field = v
            render()
        }

    // Màu "đang bật" của theme (1-3 màu); null = nền trắng icon đen như cũ.
    var accent: CcAccent? = null
        set(v) {
            field = v
            render()
        }

    init {
        val size = resources.getDimensionPixelSize(R.dimen.cc_tile_icon)
        addView(icon, LayoutParams(size, size, Gravity.CENTER))
        isClickable = true
        isFocusable = true
        render()
    }

    fun setIcon(@DrawableRes res: Int) = icon.setImageResource(res)

    private fun render() {
        icon.visibility = if (isSlot) INVISIBLE else VISIBLE
        val skinned = if (isSlot || isOn) null else skin?.get(CcSkinSlot.TILE_1X1)
        val onAccent = if (isOn && !isSlot) accent?.drawable() else null
        if (skinned != null) background = skinned
        else if (onAccent != null) background = onAccent
        else setBackgroundResource(when {
            isSlot -> R.drawable.cc_bg_slot_empty
            isOn -> R.drawable.cc_bg_tile_on
            else -> R.drawable.cc_bg_tile
        })
        val onIcon = accent?.iconColor ?: 0xFF000000.toInt()
        icon.imageTintList = ColorStateList.valueOf(if (isOn) onIcon else 0xFFFFFFFF.toInt())
    }
}

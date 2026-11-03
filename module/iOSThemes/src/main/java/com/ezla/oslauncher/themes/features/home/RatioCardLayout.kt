package com.ezla.oslauncher.themes.features.home

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

// Gốc thẻ dọc tab Themes. [keepRatio] = true (lưới See more): cao = rộng x 228/118 để thẻ giữ tỉ lệ
// Figma khi lấp ô lưới; false (hàng ngang) đo theo kích thước cố định trong layout.
class RatioCardLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var keepRatio = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!keepRatio) return super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = w * RATIO_H / RATIO_W
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
        )
    }

    private companion object {
        const val RATIO_W = 118
        const val RATIO_H = 228
    }
}

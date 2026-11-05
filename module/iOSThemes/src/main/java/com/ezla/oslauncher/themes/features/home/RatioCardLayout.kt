package com.ezla.oslauncher.themes.features.home

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

// Gốc thẻ dọc tab Themes: cao = rộng x 690/320, đúng tỉ lệ ảnh thumbnail theme (960x2082) để fitXY
// không méo, không cắt. Rộng lấy từ layout: 118dp ở hàng ngang, lấp ô ở lưới See more.
class RatioCardLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = w * RATIO_H / RATIO_W
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
        )
    }

    private companion object {
        const val RATIO_W = 320
        const val RATIO_H = 690
    }
}

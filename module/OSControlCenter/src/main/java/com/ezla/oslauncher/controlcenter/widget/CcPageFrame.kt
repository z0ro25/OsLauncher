package com.ezla.oslauncher.controlcenter.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

// Khung 1 trang panel: con đặt giữa theo ngang, theo dọc ở [topOffset] (0 = giữa màn).
// Màn thấp không đủ chỗ thì thu nhỏ con cho vừa thay vì cuộn (cuộn sẽ tranh vuốt với pager dọc).
class CcPageFrame @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var topOffset = 0
    var bottomReserve = 0

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val child = getChildAt(0) ?: return
        val w = r - l
        val h = b - t
        val cw = child.measuredWidth
        val ch = child.measuredHeight
        val avail = (h - topOffset.coerceAtLeast(0) - bottomReserve).coerceAtLeast(1)
        val scale = if (ch > avail) avail / ch.toFloat() else 1f
        child.pivotX = cw / 2f
        child.pivotY = 0f
        child.scaleX = scale
        child.scaleY = scale
        val left = (w - cw) / 2
        val top = if (topOffset > 0) topOffset else ((h - ch * scale) / 2).toInt()
        child.layout(left, top, left + cw, top + ch)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val child = getChildAt(0) ?: return
        // Đo con không giới hạn chiều cao để biết cao thật, rồi mới quyết định thu nhỏ.
        child.measure(
            MeasureSpec.makeMeasureSpec(child.layoutParams.width.coerceAtLeast(0).let {
                if (it > 0) it else measuredWidth
            }, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
    }
}

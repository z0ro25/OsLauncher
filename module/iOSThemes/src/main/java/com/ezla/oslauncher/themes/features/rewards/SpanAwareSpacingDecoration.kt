package com.ezla.oslauncher.themes.features.rewards

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

// Như GridSpacingDecoration nhưng tính theo spanIndex/spanSize -> đúng cả khi có ô chiếm nhiều cột.
class SpanAwareSpacingDecoration(private val spacingPx: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val lp = view.layoutParams as? GridLayoutManager.LayoutParams ?: return
        val spanCount = (parent.layoutManager as? GridLayoutManager)?.spanCount ?: return
        val start = lp.spanIndex
        val end = start + lp.spanSize
        outRect.left = start * spacingPx / spanCount
        outRect.right = spacingPx - end * spacingPx / spanCount
        if (parent.getChildAdapterPosition(view) >= firstRowCount(parent, spanCount)) outRect.top = spacingPx
    }

    // Số item nằm ở hàng đầu (cộng spanSize tới khi đủ spanCount).
    private fun firstRowCount(parent: RecyclerView, spanCount: Int): Int {
        val lookup = (parent.layoutManager as GridLayoutManager).spanSizeLookup
        var used = 0
        var count = 0
        val total = parent.adapter?.itemCount ?: 0
        while (count < total && used + lookup.getSpanSize(count) <= spanCount) {
            used += lookup.getSpanSize(count)
            count++
        }
        return count
    }
}

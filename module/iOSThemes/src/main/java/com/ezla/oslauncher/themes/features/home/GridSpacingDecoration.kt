package com.ezla.oslauncher.themes.features.home

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

// Khoảng cách đều giữa các ô lưới, không thêm lề ngoài (lề ngoài do padding của RecyclerView).
class GridSpacingDecoration(private val spanCount: Int, private val spacingPx: Int) :
    RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        val column = position % spanCount
        outRect.left = column * spacingPx / spanCount
        outRect.right = spacingPx - (column + 1) * spacingPx / spanCount
        if (position >= spanCount) outRect.top = spacingPx
    }
}

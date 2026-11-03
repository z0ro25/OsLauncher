package com.ezla.oslauncher.controlcenter.panel

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

// Đặt 4 ô 1x1 sát hai mép panel, khoảng cách đều giữa các ô (Figma: x = 0, 78, 156, 235).
// [width] = bề rộng panel cố định: lần đo đầu RecyclerView chưa có width (= 0) nên không dùng parent.width.
class TileGridDecoration(
    private val width: Int, private val tile: Int, private val rowGap: Int, private val span: Int = 4
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(out: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val pos = parent.getChildAdapterPosition(view)
        if (pos == RecyclerView.NO_POSITION) return
        val col = pos % span
        val cell = width / span.toFloat()
        val step = (width - tile) / (span - 1).toFloat()
        val left = (col * step - col * cell).toInt()
        out.left = left
        out.right = (cell - tile - left).toInt()
        out.top = if (pos >= span) rowGap else 0
    }
}

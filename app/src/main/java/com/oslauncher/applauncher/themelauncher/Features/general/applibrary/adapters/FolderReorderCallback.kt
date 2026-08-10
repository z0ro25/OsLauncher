package com.oslauncher.applauncher.themelauncher.Features.general.applibrary.adapters

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * ItemTouchHelper.Callback Kotlin RIÊNG cho app layer (không dùng engine DragDropCallBack để tránh
 * kéo dependency).
 *  - Kéo dọc: kích hoạt bằng tay từ handle (isLongPressDragEnabled = false).
 *  - Vuốt ngang: xóa folder ([ManageFolderAdapter.onSwipeDelete]).
 */
class FolderReorderCallback(private val adapter: ManageFolderAdapter) :
    ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled(): Boolean = false
    override fun isItemViewSwipeEnabled(): Boolean = true

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        adapter.onItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        adapter.swipeDeleteAt(viewHolder.bindingAdapterPosition)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        // Thả tay sau khi kéo -> lưu thứ tự mới.
        adapter.onDropped()
    }
}

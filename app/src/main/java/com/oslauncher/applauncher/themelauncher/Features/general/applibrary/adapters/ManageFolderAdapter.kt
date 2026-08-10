package com.oslauncher.applauncher.themelauncher.Features.general.applibrary.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.data.AppLibraryFolder
import com.oslauncher.applauncher.themelauncher.databinding.ViewholderApplibManageFolderBinding
import java.util.Collections

/**
 * Danh sách folder custom ở màn Manage Library. Kéo sắp xếp CHỈ từ tay cầm hamburger
 * ([onStartDrag] gọi khi user chạm handle) — không long-press toàn dòng.
 * Sau khi thả, [onOrderChanged] báo thứ tự id mới để lưu bền.
 */
class ManageFolderAdapter(val listItem: MutableList<AppLibraryFolder>) : Adapter<ViewHolder>() {

    var onStartDrag: ((ViewHolder) -> Unit)? = null
    var onOrderChanged: ((List<String>) -> Unit)? = null
    var onSwipeDelete: ((AppLibraryFolder) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return FolderVH(
            ViewholderApplibManageFolderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount(): Int = listItem.size

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = listItem[position]
        (holder as FolderVH).binding.apply {
            tvFolderName.text = data.name
            ivDragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag?.invoke(holder)
                }
                false
            }
        }
    }

    /** Hoán vị khi kéo (gọi từ ItemTouchHelper callback). */
    fun onItemMove(from: Int, to: Int) {
        Collections.swap(listItem, from, to)
        notifyItemMoved(from, to)
    }

    /** Gọi khi thả tay -> báo thứ tự id mới. */
    fun onDropped() {
        onOrderChanged?.invoke(listItem.map { it.id })
    }

    /** Vuốt xóa 1 folder tại vị trí. */
    fun swipeDeleteAt(position: Int) {
        if (position < 0 || position >= listItem.size) return
        onSwipeDelete?.invoke(listItem[position])
    }

    class FolderVH(val binding: ViewholderApplibManageFolderBinding) : ViewHolder(binding.root)
}

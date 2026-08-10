package com.oslauncher.applauncher.themelauncher.Features.general.applibrary.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.oslauncher.applauncher.themelauncher.databinding.ViewholderApplibSelectFolderBinding

/**
 * 1 mục chọn ở màn Select Folder: id (targetId) + nhãn hiển thị. Single-select: chỉ [selectedTargetId]
 * hiện tick. Bấm -> [onSelect].
 */
class SelectFolderAdapter(
    val listItem: List<Pair<String, String>>, // (targetId, label)
    var selectedTargetId: String
) : Adapter<ViewHolder>() {

    var onSelect: ((targetId: String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return TargetVH(
            ViewholderApplibSelectFolderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount(): Int = listItem.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (targetId, label) = listItem[position]
        (holder as TargetVH).binding.apply {
            tvTargetName.text = label
            ivCheck.visibility = if (targetId == selectedTargetId) View.VISIBLE else View.INVISIBLE
            root.setOnClickListener {
                if (selectedTargetId != targetId) {
                    selectedTargetId = targetId
                    notifyDataSetChanged()
                }
                onSelect?.invoke(targetId)
            }
        }
    }

    class TargetVH(val binding: ViewholderApplibSelectFolderBinding) : ViewHolder(binding.root)
}

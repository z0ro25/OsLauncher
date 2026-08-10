package com.oslauncher.applauncher.themelauncher.Features.general.applibrary.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.data.InstalledAppItem
import com.oslauncher.applauncher.themelauncher.databinding.ViewholderApplibAppBinding

/**
 * Danh sách app thật ở màn App Library: icon + tên + nhãn folder hiện tại + chevron.
 * Bấm 1 dòng -> [onSelectFolder] (mở Select Folder cho app đó).
 */
class AppLibraryAdapter(val listItem: ArrayList<InstalledAppItem>) : Adapter<ViewHolder>() {

    var onSelectFolder: ((InstalledAppItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return AppVH(
            ViewholderApplibAppBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount(): Int = listItem.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = listItem[position]
        (holder as AppVH).binding.apply {
            ivAppIcon.setImageDrawable(data.icon)
            tvAppName.text = data.label
            tvFolderLabel.text = data.folderLabel
            root.setOnClickListener { onSelectFolder?.invoke(data) }
        }
    }

    class AppVH(val binding: ViewholderApplibAppBinding) : ViewHolder(binding.root)
}

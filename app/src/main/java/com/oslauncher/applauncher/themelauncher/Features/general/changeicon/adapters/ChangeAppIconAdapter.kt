package com.oslauncher.applauncher.themelauncher.Features.general.changeicon.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.oslauncher.applauncher.themelauncher.Features.general.changeicon.data.ChangeIconItem
import com.oslauncher.applauncher.themelauncher.databinding.ViewholderChangeAppIconBinding

/**
 * Danh sách app màn Change App Icon: icon + tên hiển thị + chevron ">". Bấm 1 dòng -> [onSelectApp].
 * Divider ẩn ở dòng cuối để trùng mép bo của card.
 */
class ChangeAppIconAdapter(private val listItem: List<ChangeIconItem>) : Adapter<ViewHolder>() {

    var onSelectApp: ((ChangeIconItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        AppViewHolder(
            ViewholderChangeAppIconBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun getItemCount(): Int = listItem.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = listItem[position]
        val vhd = holder as AppViewHolder
        vhd.binding.apply {
            tvAppName.text = data.displayLabel
            ivAppIcon.setImageDrawable(data.icon)
            divider.isVisible = position != listItem.size - 1
            root.setOnClickListener { onSelectApp?.invoke(data) }
        }
    }

    class AppViewHolder(val binding: ViewholderChangeAppIconBinding) : ViewHolder(binding.root)
}

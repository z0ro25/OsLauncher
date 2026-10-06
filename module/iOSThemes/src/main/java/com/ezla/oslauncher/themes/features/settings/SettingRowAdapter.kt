package com.ezla.oslauncher.themes.features.settings

import com.ezla.oslauncher.themes.Models.SettingRow
import com.ezla.oslauncher.themes.Models.SettingItem
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.themes.databinding.ThemesItemSettingRowBinding

class SettingRowAdapter(private val onClick: (SettingItem) -> Unit) :
    RecyclerView.Adapter<SettingRowAdapter.Holder>() {

    private val rows = mutableListOf<SettingRow>()

    fun submit(list: List<SettingRow>) {
        rows.clear()
        rows.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ThemesItemSettingRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        holder.binding.apply {
            imgRowIcon.setImageResource(row.icon)
            tvRowTitle.setText(row.title)
            root.setOnClickListener { onClick(row.item) }
        }
    }

    class Holder(val binding: ThemesItemSettingRowBinding) : RecyclerView.ViewHolder(binding.root)
}

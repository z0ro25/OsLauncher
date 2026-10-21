package com.ezla.oslauncher.themes.features.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.themes.databinding.ThemesItemThemePlaceholderBinding

// Thẻ chờ tải (Figma tab_Themes): gắn tạm vào lưới theme trong lúc gọi API.
class ThemePlaceholderAdapter(private val count: Int) :
    RecyclerView.Adapter<ThemePlaceholderAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ThemesItemThemePlaceholderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = count

    override fun onBindViewHolder(holder: Holder, position: Int) {}

    class Holder(binding: ThemesItemThemePlaceholderBinding) : RecyclerView.ViewHolder(binding.root)
}

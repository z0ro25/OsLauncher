package com.ezla.oslauncher.themes.features.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.databinding.ThemesItemThemeIconBinding

// Lưới icon (ảnh) trong trang preview của theme.
class ThemeIconPreviewAdapter(private val urls: List<String>) :
    RecyclerView.Adapter<ThemeIconPreviewAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ThemesItemThemeIconBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount(): Int = urls.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        Glide.with(holder.binding.imgIcon).load(urls[position]).into(holder.binding.imgIcon)
    }

    class Holder(val binding: ThemesItemThemeIconBinding) : RecyclerView.ViewHolder(binding.root)
}

package com.ezla.oslauncher.themes.features.home

import com.ezla.oslauncher.themes.Models.ThemeItem
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.databinding.ThemesItemThemeBinding

class ThemeAdapter(private val onClick: (ThemeItem) -> Unit) :
    RecyclerView.Adapter<ThemeAdapter.Holder>() {

    private val items = mutableListOf<ThemeItem>()

    fun submit(list: List<ThemeItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ThemesItemThemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        Glide.with(holder.binding.imgPreview).load(item.previewUrl).into(holder.binding.imgPreview)
        holder.binding.imgPreview.contentDescription = item.name
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ThemesItemThemeBinding) : RecyclerView.ViewHolder(binding.root)
}

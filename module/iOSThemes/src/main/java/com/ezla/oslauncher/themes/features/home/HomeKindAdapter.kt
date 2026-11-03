package com.ezla.oslauncher.themes.features.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.themes.Models.HomeKind
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.databinding.ThemesItemMineTabBinding

// Chip loại ở đầu tab Themes (Figma: chọn = nền hồng chữ trắng, thường = nền trắng chữ xám).
// Dùng lại layout chip của Mine cho cùng kích thước / font.
class HomeKindAdapter(
    private var selected: HomeKind,
    private val onSelect: (HomeKind) -> Unit
) : RecyclerView.Adapter<HomeKindAdapter.Holder>() {

    private val kinds = HomeKind.values().toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ThemesItemMineTabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount(): Int = kinds.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val kind = kinds[position]
        val active = kind == selected
        val ctx = holder.itemView.context
        holder.binding.tvMineTab.apply {
            setText(labelOf(kind))
            setBackgroundResource(if (active) R.drawable.themes_bg_chip_selected else R.drawable.themes_bg_chip_normal)
            setTextColor(ContextCompat.getColor(ctx, if (active) R.color.themes_white else R.color.themes_text_hint))
            setOnClickListener {
                if (selected == kind) return@setOnClickListener
                selected = kind
                notifyDataSetChanged()
                onSelect(kind)
            }
        }
    }

    class Holder(val binding: ThemesItemMineTabBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        fun labelOf(kind: HomeKind) = when (kind) {
            HomeKind.THEME -> R.string.themes_kind_theme
            HomeKind.ICON -> R.string.themes_kind_icons
            HomeKind.WALLPAPER -> R.string.themes_kind_wallpaper
            HomeKind.CONTROL_CENTER -> R.string.themes_kind_control_center
        }
    }
}

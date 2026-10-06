package com.ezla.oslauncher.themes.features.mine

import com.ezla.oslauncher.themes.Models.MineTab
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.databinding.ThemesItemMineTabBinding

class MineTabAdapter(
    private var selected: MineTab,
    private val onSelect: (MineTab) -> Unit
) : RecyclerView.Adapter<MineTabAdapter.Holder>() {

    private val tabs = MineTab.values().toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ThemesItemMineTabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount(): Int = tabs.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val tab = tabs[position]
        val active = tab == selected
        val ctx = holder.itemView.context
        holder.binding.tvMineTab.apply {
            setText(labelOf(tab))
            setBackgroundResource(if (active) R.drawable.themes_bg_chip_selected else R.drawable.themes_bg_chip_normal)
            setTextColor(ContextCompat.getColor(ctx, if (active) R.color.themes_white else R.color.themes_text_hint))
            setOnClickListener {
                if (selected == tab) return@setOnClickListener
                selected = tab
                notifyDataSetChanged()
                onSelect(tab)
            }
        }
    }

    private fun labelOf(tab: MineTab) = when (tab) {
        MineTab.THEME -> R.string.themes_mine_tab_theme
        MineTab.WALLPAPERS -> R.string.themes_mine_tab_wallpapers
        MineTab.ICON -> R.string.themes_mine_tab_icon
        MineTab.FAVORITES -> R.string.themes_mine_tab_favorites
    }

    class Holder(val binding: ThemesItemMineTabBinding) : RecyclerView.ViewHolder(binding.root)
}

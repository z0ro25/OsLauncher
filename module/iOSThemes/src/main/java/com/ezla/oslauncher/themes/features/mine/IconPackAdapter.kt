package com.ezla.oslauncher.themes.features.mine

import com.ezla.oslauncher.themes.Models.IconPack
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.databinding.ThemesItemIconPackBinding
import com.ezla.oslauncher.themes.databinding.ThemesItemPackIconBinding

// Thẻ icon pack: 6 icon đầu (lưới 3 cột) + tên.
class IconPackAdapter(private val onClick: (IconPack) -> Unit) :
    RecyclerView.Adapter<IconPackAdapter.Holder>() {

    private val items = mutableListOf<IconPack>()

    fun submit(list: List<IconPack>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ThemesItemIconPackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Không cuộn được thì RecyclerView.onTouchEvent trả false -> click rơi về root của thẻ.
        binding.rvIcons.layoutManager = object : GridLayoutManager(parent.context, ICON_SPAN) {
            override fun canScrollVertically() = false
            override fun canScrollHorizontally() = false
        }
        return Holder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val pack = items[position]
        holder.binding.tvPackName.text = pack.name
        val bg = holder.binding.imgPackBg
        if (pack.background != null) {
            bg.visibility = View.VISIBLE
            Glide.with(bg).load(pack.background).into(bg)
        } else {
            Glide.with(bg).clear(bg)
            bg.visibility = View.GONE
        }
        holder.binding.rvIcons.adapter = IconsAdapter(pack.icons.take(MAX_ICONS))
        holder.binding.root.setOnClickListener { onClick(pack) }
    }

    class Holder(val binding: ThemesItemIconPackBinding) : RecyclerView.ViewHolder(binding.root)

    private class IconsAdapter(private val icons: List<Any>) :
        RecyclerView.Adapter<IconsAdapter.IconHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = IconHolder(
            ThemesItemPackIconBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun getItemCount(): Int = icons.size

        override fun onBindViewHolder(holder: IconHolder, position: Int) {
            Glide.with(holder.binding.imgIcon).load(icons[position]).into(holder.binding.imgIcon)
        }

        class IconHolder(val binding: ThemesItemPackIconBinding) : RecyclerView.ViewHolder(binding.root)
    }

    companion object {
        private const val ICON_SPAN = 3
        private const val MAX_ICONS = 6
    }
}

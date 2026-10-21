package com.ezla.oslauncher.themes.features.mine

import com.ezla.oslauncher.themes.Models.IconPack
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.features.CardImageTracker
import com.ezla.oslauncher.themes.features.loadCardImage
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
        holder.bound = pack
        val icons = pack.icons.take(MAX_ICONS)
        // Vòng quay tắt khi nền + mọi icon đã tải xong; một ảnh lỗi -> gỡ cả thẻ.
        val tracker = CardImageTracker(icons.size + if (pack.background != null) 1 else 0) { ok ->
            if (holder.bound !== pack) return@CardImageTracker
            holder.binding.pbImage.visibility = View.GONE
            if (!ok) removeItem(pack)
        }
        holder.binding.pbImage.visibility = if (tracker.done) View.GONE else View.VISIBLE
        val bg = holder.binding.imgPackBg
        if (pack.background != null) {
            bg.visibility = View.VISIBLE
            bg.loadCardImage(pack.background, tracker::report)
        } else {
            Glide.with(bg).clear(bg)
            bg.visibility = View.GONE
        }
        holder.binding.rvIcons.adapter = IconsAdapter(icons, tracker::report)
        // RecyclerView lưới icon luôn nuốt chạm -> nhận click qua lớp phủ viewClick + phần tên.
        holder.binding.viewClick.setOnClickListener { onClick(pack) }
        holder.binding.root.setOnClickListener { onClick(pack) }
    }

    // Ảnh tải lỗi -> gỡ thẻ. Tìm theo đúng object (không theo vị trí) vì list có thể đã đổi.
    private fun removeItem(pack: IconPack) {
        val index = items.indexOfFirst { it === pack }
        if (index < 0) return
        items.removeAt(index)
        notifyItemRemoved(index)
    }

    class Holder(val binding: ThemesItemIconPackBinding) : RecyclerView.ViewHolder(binding.root) {
        var bound: IconPack? = null
    }

    private class IconsAdapter(
        private val icons: List<Any>,
        private val onResult: (Boolean) -> Unit
    ) :
        RecyclerView.Adapter<IconsAdapter.IconHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = IconHolder(
            ThemesItemPackIconBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun getItemCount(): Int = icons.size

        override fun onBindViewHolder(holder: IconHolder, position: Int) {
            holder.binding.imgIcon.loadCardImage(icons[position], onResult)
        }

        class IconHolder(val binding: ThemesItemPackIconBinding) : RecyclerView.ViewHolder(binding.root)
    }

    companion object {
        private const val ICON_SPAN = 3
        private const val MAX_ICONS = 6
    }
}

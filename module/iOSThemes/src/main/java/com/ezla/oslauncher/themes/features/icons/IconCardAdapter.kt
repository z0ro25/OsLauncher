package com.ezla.oslauncher.themes.features.icons

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.Models.IconThemeCard
import com.ezla.oslauncher.themes.databinding.ThemesItemIconCardBinding
import com.ezla.oslauncher.themes.databinding.ThemesItemIconCellBinding

// Hàng thẻ ngang của 1 section: nền = ảnh background theme, phủ lưới 3x2 icon (Figma Icon).
class IconCardAdapter(
    private val items: List<IconThemeCard>,
    private val onClick: (IconThemeCard) -> Unit
) : RecyclerView.Adapter<IconCardAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ThemesItemIconCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.rvIcons.layoutManager = object : GridLayoutManager(parent.context, COLUMNS) {
            override fun canScrollVertically() = false
            override fun canScrollHorizontally() = false
        }
        return Holder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val b = holder.binding
        Glide.with(b.imgBackground).load(item.background).into(b.imgBackground)
        b.rvIcons.adapter = IconCellAdapter(item.icons)
        b.tvName.text = item.name
        b.root.contentDescription = item.name
        // RecyclerView lưới icon luôn nuốt chạm -> nhận click qua lớp phủ viewClick + phần tên.
        b.viewClick.setOnClickListener { onClick(item) }
        b.root.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ThemesItemIconCardBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        const val COLUMNS = 3
    }
}

private class IconCellAdapter(private val icons: List<String>) :
    RecyclerView.Adapter<IconCellAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ThemesItemIconCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount(): Int = icons.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        Glide.with(holder.binding.imgIcon).load(icons[position]).into(holder.binding.imgIcon)
    }

    class Holder(val binding: ThemesItemIconCellBinding) : RecyclerView.ViewHolder(binding.root)
}

package com.ezla.oslauncher.themes.features.icons

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.data.DefaultThemeProvider
import com.ezla.oslauncher.themes.Models.IconThemeCard
import com.ezla.oslauncher.themes.features.CardImageTracker
import com.ezla.oslauncher.themes.features.loadCardImage
import com.ezla.oslauncher.themes.databinding.ThemesItemIconCardBinding
import com.ezla.oslauncher.themes.databinding.ThemesItemIconCellBinding

// Hàng thẻ ngang của 1 section: nền = ảnh background theme, phủ lưới 3x2 icon (Figma Icon).
// Vòng quay tắt khi nền + mọi icon đã tải xong; một ảnh lỗi -> gỡ cả thẻ.
class IconCardAdapter(
    items: List<IconThemeCard>,
    private val onClick: (IconThemeCard) -> Unit
) : RecyclerView.Adapter<IconCardAdapter.Holder>() {

    private val items = items.toMutableList()

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
        holder.bound = item
        // background null = theme không có nền (thẻ nền trắng như cũ), không tính là lỗi.
        val tracker = CardImageTracker(item.icons.size + if (item.background != null) 1 else 0) { ok ->
            if (holder.bound !== item) return@CardImageTracker
            b.pbImage.visibility = View.GONE
            if (!ok) removeItem(item)
        }
        b.pbImage.visibility = if (tracker.done) View.GONE else View.VISIBLE
        if (item.background != null) b.imgBackground.loadCardImage(item.background, tracker::report)
        else Glide.with(b.imgBackground).clear(b.imgBackground)
        b.rvIcons.adapter = IconCellAdapter(item.icons, tracker::report)
        b.tvName.text = item.name
        b.tvNew.visibility =
            if (item.isUnlocked || DefaultThemeProvider.isDefault(item.id)) View.GONE else View.VISIBLE
        b.root.contentDescription = item.name
        // RecyclerView lưới icon luôn nuốt chạm -> nhận click qua lớp phủ viewClick + phần tên.
        b.viewClick.setOnClickListener { onClick(item) }
        b.root.setOnClickListener { onClick(item) }
    }

    // Ảnh tải lỗi -> gỡ thẻ. Tìm theo đúng object (không theo vị trí) vì list có thể đã đổi.
    private fun removeItem(item: IconThemeCard) {
        val index = items.indexOfFirst { it === item }
        if (index < 0) return
        items.removeAt(index)
        notifyItemRemoved(index)
    }

    class Holder(val binding: ThemesItemIconCardBinding) : RecyclerView.ViewHolder(binding.root) {
        var bound: IconThemeCard? = null
    }

    companion object {
        const val COLUMNS = 3
    }
}

private class IconCellAdapter(
    private val icons: List<String>,
    private val onResult: (Boolean) -> Unit
) : RecyclerView.Adapter<IconCellAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ThemesItemIconCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount(): Int = icons.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.imgIcon.loadCardImage(icons[position], onResult)
    }

    class Holder(val binding: ThemesItemIconCellBinding) : RecyclerView.ViewHolder(binding.root)
}

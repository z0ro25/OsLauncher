package com.ezla.oslauncher.themes.features.home

import com.ezla.oslauncher.themes.Models.ThemeItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.themes.data.DefaultThemeProvider
import com.ezla.oslauncher.themes.features.loadCardImage
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

    // Ảnh tải lỗi -> gỡ thẻ. Tìm theo đúng object (không theo vị trí) vì list có thể đã đổi.
    private fun removeItem(item: ThemeItem) {
        val index = items.indexOfFirst { it === item }
        if (index < 0) return
        items.removeAt(index)
        notifyItemRemoved(index)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.pbImage.visibility = View.VISIBLE
        holder.binding.imgPreview.loadCardImage(item.previewUrl) { ok ->
            // Holder đã bị tái dùng cho thẻ khác thì không đụng vòng quay của thẻ mới.
            if (items.getOrNull(holder.bindingAdapterPosition) === item) holder.binding.pbImage.visibility = View.GONE
            if (!ok) removeItem(item)
        }
        holder.binding.imgPreview.contentDescription = item.name
        // New = chưa unlock (theo cờ server, không xét subs); theme mặc định miễn phí -> ẩn.
        holder.binding.tvNew.visibility =
            if (item.isUnlocked || DefaultThemeProvider.isDefault(item.id)) View.GONE else View.VISIBLE
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ThemesItemThemeBinding) : RecyclerView.ViewHolder(binding.root)
}

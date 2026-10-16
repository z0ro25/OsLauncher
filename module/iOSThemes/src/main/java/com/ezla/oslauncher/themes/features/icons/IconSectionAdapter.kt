package com.ezla.oslauncher.themes.features.icons

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.themes.Models.IconSection
import com.ezla.oslauncher.themes.Models.IconThemeCard
import com.ezla.oslauncher.themes.Models.ThemeCategory
import com.ezla.oslauncher.themes.databinding.ThemesItemIconSectionBinding

// Danh sách section tab Icons: tiêu đề danh mục + ">" (xem tất cả) + hàng thẻ cuộn ngang.
class IconSectionAdapter(
    private val onMore: (ThemeCategory) -> Unit,
    private val onCard: (IconThemeCard) -> Unit
) : RecyclerView.Adapter<IconSectionAdapter.Holder>() {

    private val items = mutableListOf<IconSection>()

    fun submit(list: List<IconSection>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    // Vị trí section của danh mục (chip cuộn tới); -1 nếu danh mục đó không có section.
    fun indexOf(categoryId: String): Int = items.indexOfFirst { it.category.id == categoryId }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ThemesItemIconSectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.rvCards.layoutManager =
            LinearLayoutManager(parent.context, LinearLayoutManager.HORIZONTAL, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val section = items[position]
        val b = holder.binding
        val ctx = b.root.context
        val category = section.category
        b.tvSectionTitle.text = if (category.titleRes != 0) ctx.getString(category.titleRes) else category.title
        b.layoutHeader.setOnClickListener { onMore(category) }
        b.rvCards.adapter = IconCardAdapter(section.themes, onCard)
    }

    class Holder(val binding: ThemesItemIconSectionBinding) : RecyclerView.ViewHolder(binding.root)
}

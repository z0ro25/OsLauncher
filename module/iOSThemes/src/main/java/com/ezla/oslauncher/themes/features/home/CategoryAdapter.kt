package com.ezla.oslauncher.themes.features.home

import com.ezla.oslauncher.themes.Models.ThemeCategory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.databinding.ThemesItemCategoryBinding

// Chip danh mục: chọn = nền hồng + chữ trắng Quicksand SemiBold; thường = nền trắng + chữ xám.
class CategoryAdapter(private val onSelect: (ThemeCategory) -> Unit) :
    RecyclerView.Adapter<CategoryAdapter.Holder>() {

    private val items = mutableListOf<ThemeCategory>()
    private var selectedId: String? = null

    fun submit(list: List<ThemeCategory>, selected: String?) {
        items.clear()
        items.addAll(list)
        selectedId = selected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ThemesItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context
        val selected = item.id == selectedId
        holder.binding.apply {
            imgCategory.setImageResource(item.iconRes)
            tvCategory.text = if (item.titleRes != 0) ctx.getString(item.titleRes) else item.title
            root.setBackgroundResource(if (selected) R.drawable.themes_bg_chip_selected else R.drawable.themes_bg_chip_normal)
            tvCategory.setTextColor(ContextCompat.getColor(ctx, if (selected) R.color.themes_white else R.color.themes_text_hint))
            tvCategory.typeface = if (selected) ResourcesCompat.getFont(ctx, R.font.themes_quicksand_semibold) else null
            root.setOnClickListener {
                if (selectedId == item.id) return@setOnClickListener
                selectedId = item.id
                notifyDataSetChanged()
                onSelect(item)
            }
        }
    }

    class Holder(val binding: ThemesItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)
}

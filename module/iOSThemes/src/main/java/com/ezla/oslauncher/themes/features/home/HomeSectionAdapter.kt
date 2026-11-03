package com.ezla.oslauncher.themes.features.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.Models.HomeKind
import com.ezla.oslauncher.themes.Models.HomeSection
import com.ezla.oslauncher.themes.Models.ThemeCategory
import com.ezla.oslauncher.themes.databinding.ThemesItemHomeSectionBinding

// Danh sách section tab Themes. Section được cập nhật lẻ từng cái (danh mục nào tải xong trước
// hiện trước); section rỗng / mọi thẻ lỗi ảnh thì gỡ khỏi danh sách.
class HomeSectionAdapter(
    private val onMore: (ThemeCategory) -> Unit,
    private val onCard: (ThemeCategory, Any) -> Unit
) : RecyclerView.Adapter<HomeSectionAdapter.Holder>() {

    private val items = mutableListOf<HomeSection>()
    private var kind = HomeKind.THEME

    // Danh mục mới: mọi section ở trạng thái đang tải.
    fun reset(kind: HomeKind, categories: List<ThemeCategory>) {
        this.kind = kind
        items.clear()
        items.addAll(categories.map { HomeSection(it) })
        notifyDataSetChanged()
    }

    fun update(categoryId: String, cards: List<Any>) {
        val index = items.indexOfFirst { it.category.id == categoryId }
        if (index < 0) return
        if (cards.isEmpty()) remove(categoryId) else {
            items[index] = items[index].copy(items = cards)
            notifyItemChanged(index)
        }
    }

    private fun remove(categoryId: String) {
        val index = items.indexOfFirst { it.category.id == categoryId }
        if (index < 0) return
        items.removeAt(index)
        notifyItemRemoved(index)
    }

    // Còn section đang tải hoặc có thẻ -> true; dùng để hiện lỗi khi mọi danh mục đều trống.
    fun hasContent() = items.isNotEmpty()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val b = ThemesItemHomeSectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        b.rvCards.layoutManager = LinearLayoutManager(parent.context, LinearLayoutManager.HORIZONTAL, false)
        return Holder(b)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val section = items[position]
        val b = holder.binding
        val category = section.category
        val ctx = b.root.context
        b.tvSectionTitle.text = if (category.titleRes != 0) ctx.getString(category.titleRes) else category.title
        // Icon danh mục lấy từ thumbnail trên server; không có thì chỉ còn chữ.
        if (category.iconUrl.isNullOrEmpty()) {
            Glide.with(b.imgSectionIcon).clear(b.imgSectionIcon)
            b.imgSectionIcon.visibility = View.GONE
        } else {
            b.imgSectionIcon.visibility = View.VISIBLE
            Glide.with(b.imgSectionIcon).load(category.iconUrl).into(b.imgSectionIcon)
        }
        b.tvSeeMore.visibility = if (section.items.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        b.tvSeeMore.setOnClickListener { onMore(category) }
        b.rvCards.adapter = HomeCardAdapter(
            kind = kind,
            items = section.items,
            onClick = { card: Any -> onCard(category, card) },
            onEmpty = { remove(category.id) }
        )
    }

    class Holder(val binding: ThemesItemHomeSectionBinding) : RecyclerView.ViewHolder(binding.root)
}

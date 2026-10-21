package com.ezla.oslauncher.themes.features.home

import com.ezla.oslauncher.themes.Models.ThemeCategory
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
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
            bindIcon(this, item.iconUrl)
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

    // Icon lấy từ thumbnail danh mục trên server; không có ảnh hoặc tải lỗi -> ẩn, chip chỉ còn chữ.
    private fun bindIcon(binding: ThemesItemCategoryBinding, url: String?) {
        val img = binding.imgCategory
        if (url.isNullOrEmpty()) {
            Glide.with(img).clear(img)
            img.visibility = View.GONE
            return
        }
        img.visibility = View.VISIBLE
        Glide.with(img).load(url).listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean
            ): Boolean {
                img.visibility = View.GONE
                return false
            }

            override fun onResourceReady(
                resource: Drawable, model: Any, target: Target<Drawable>?,
                dataSource: DataSource, isFirstResource: Boolean
            ): Boolean = false
        }).into(img)
    }

    class Holder(val binding: ThemesItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)
}

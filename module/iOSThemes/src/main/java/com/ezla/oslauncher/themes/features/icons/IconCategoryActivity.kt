package com.ezla.oslauncher.themes.features.icons

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.themes.Models.ThemeCategory
import com.ezla.oslauncher.themes.Models.ThemeItem
import com.ezla.oslauncher.themes.Models.ThemeTab
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.data.DefaultThemeProvider
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
import com.ezla.oslauncher.themes.features.loadCardImage
import com.ezla.oslauncher.themes.databinding.ThemesActivityIconCategoryBinding
import com.ezla.oslauncher.themes.databinding.ThemesItemCategoryThemeBinding
import com.ezla.oslauncher.themes.features.detail.ThemeViewActivity
import com.ezla.oslauncher.themes.features.home.GridSpacingDecoration
import kotlinx.coroutines.launch

// Màn xem tất cả theme của 1 danh mục (mở từ ">" ở tab Icons, Figma "Wallpapers/Anime"):
// lưới 3 cột thumbnail lớn từ /themes; bấm thẻ -> màn View theme.
class IconCategoryActivity : BaseActivity<ThemesActivityIconCategoryBinding>() {

    override val setViewBinding: ThemesActivityIconCategoryBinding
        get() = ThemesActivityIconCategoryBinding.inflate(LayoutInflater.from(this))

    private val repository = ThemeRepositoryProvider.repository
    private val categoryId get() = intent.getStringExtra(EXTRA_CATEGORY_ID).orEmpty()

    private val adapter = CategoryThemeAdapter { theme ->
        ThemeViewActivity.start(this, theme.id, categoryId)
    }

    override fun initView() {
        binding.header.tvHeaderTitle.text = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        binding.rvThemes.layoutManager = GridLayoutManager(this, SPAN)
        binding.rvThemes.addItemDecoration(GridSpacingDecoration(SPAN, dp(GAP_DP)))
        binding.rvThemes.adapter = adapter
    }

    override fun viewListener() {
        binding.header.imgBack.setOnClickListener { finish() }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            binding.pbLoading.visibility = View.VISIBLE
            adapter.submit(repository.getThemes(ThemeTab.FEATURED, categoryId))
            binding.pbLoading.visibility = View.GONE
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val EXTRA_CATEGORY_ID = "extra_category_id"
        private const val EXTRA_TITLE = "extra_title"
        private const val SPAN = 3
        private const val GAP_DP = 8

        fun start(context: Context, category: ThemeCategory) {
            val title = if (category.titleRes != 0) context.getString(category.titleRes) else category.title
            context.startActivity(
                Intent(context, IconCategoryActivity::class.java)
                    .putExtra(EXTRA_CATEGORY_ID, category.id)
                    .putExtra(EXTRA_TITLE, title)
            )
        }
    }
}

private class CategoryThemeAdapter(private val onClick: (ThemeItem) -> Unit) :
    RecyclerView.Adapter<CategoryThemeAdapter.Holder>() {

    private val items = mutableListOf<ThemeItem>()

    fun submit(list: List<ThemeItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ThemesItemCategoryThemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
        holder.binding.imgThumb.loadCardImage(item.previewUrl) { ok ->
            // Holder đã bị tái dùng cho thẻ khác thì không đụng vòng quay của thẻ mới.
            if (items.getOrNull(holder.bindingAdapterPosition) === item) holder.binding.pbImage.visibility = View.GONE
            if (!ok) removeItem(item)
        }
        holder.binding.imgThumb.contentDescription = item.name
        holder.binding.tvNew.visibility =
            if (item.isUnlocked || DefaultThemeProvider.isDefault(item.id)) View.GONE else View.VISIBLE
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ThemesItemCategoryThemeBinding) : RecyclerView.ViewHolder(binding.root)
}

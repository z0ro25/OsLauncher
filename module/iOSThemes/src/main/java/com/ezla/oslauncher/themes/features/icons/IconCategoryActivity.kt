package com.ezla.oslauncher.themes.features.icons

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.Models.ThemeCategory
import com.ezla.oslauncher.themes.Models.ThemeItem
import com.ezla.oslauncher.themes.Models.ThemeTab
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
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

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        Glide.with(holder.binding.imgThumb).load(item.previewUrl).into(holder.binding.imgThumb)
        holder.binding.imgThumb.contentDescription = item.name
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ThemesItemCategoryThemeBinding) : RecyclerView.ViewHolder(binding.root)
}

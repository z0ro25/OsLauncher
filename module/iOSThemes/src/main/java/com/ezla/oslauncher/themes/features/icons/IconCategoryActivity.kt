package com.ezla.oslauncher.themes.features.icons

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ezla.oslauncher.themes.Models.IconPack
import com.ezla.oslauncher.themes.Models.ThemeCategory
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
import com.ezla.oslauncher.themes.databinding.ThemesActivityIconCategoryBinding
import com.ezla.oslauncher.themes.features.ThemesInter
import com.ezla.oslauncher.themes.features.detail.IconDetailActivity
import com.ezla.oslauncher.themes.features.home.GridSpacingDecoration
import com.ezla.oslauncher.themes.features.mine.IconPackAdapter
import kotlinx.coroutines.launch

// Màn xem tất cả bộ icon của 1 danh mục (mở từ ">" ở tab Icons): lưới 2 cột thẻ icon pack giống
// Mine > Icon; bấm thẻ -> Icon detail (qua inter như thẻ ở tab Icons).
class IconCategoryActivity : BaseActivity<ThemesActivityIconCategoryBinding>() {

    override val setViewBinding: ThemesActivityIconCategoryBinding
        get() = ThemesActivityIconCategoryBinding.inflate(LayoutInflater.from(this))

    private val repository = ThemeRepositoryProvider.repository
    private val categoryId get() = intent.getStringExtra(EXTRA_CATEGORY_ID).orEmpty()

    private val adapter = IconPackAdapter { pack ->
        ThemesInter.run(this) { if (!isFinishing) IconDetailActivity.start(this, pack.id) }
    }

    override fun initView() {
        binding.header.tvHeaderTitle.text = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        binding.rvThemes.layoutManager = GridLayoutManager(this, SPAN)
        binding.rvThemes.addItemDecoration(
            GridSpacingDecoration(SPAN, resources.getDimensionPixelSize(R.dimen.themes_mine_gap))
        )
        binding.rvThemes.adapter = adapter
    }

    override fun viewListener() {
        binding.header.imgBack.setOnClickListener { finish() }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            binding.pbLoading.visibility = View.VISIBLE
            adapter.submit(repository.getIconCards(categoryId).map {
                IconPack(id = it.id, name = it.name, icons = it.icons, background = it.background)
            })
            binding.pbLoading.visibility = View.GONE
        }
    }

    companion object {
        private const val EXTRA_CATEGORY_ID = "extra_category_id"
        private const val EXTRA_TITLE = "extra_title"
        private const val SPAN = 2

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

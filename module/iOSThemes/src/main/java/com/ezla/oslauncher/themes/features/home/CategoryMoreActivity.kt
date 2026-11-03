package com.ezla.oslauncher.themes.features.home

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ezla.oslauncher.themes.Models.ControlCenterCard
import com.ezla.oslauncher.themes.Models.HomeKind
import com.ezla.oslauncher.themes.Models.IconPack
import com.ezla.oslauncher.themes.Models.IconThemeCard
import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.Models.ThemeCategory
import com.ezla.oslauncher.themes.Models.ThemeItem
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
import com.ezla.oslauncher.themes.databinding.ThemesActivityIconCategoryBinding
import com.ezla.oslauncher.themes.features.ThemesInter
import com.ezla.oslauncher.themes.features.controlcenter.ControlCenterDetailActivity
import com.ezla.oslauncher.themes.features.detail.IconDetailActivity
import com.ezla.oslauncher.themes.features.detail.ThemeViewActivity
import com.ezla.oslauncher.themes.features.detail.WallpaperDetailActivity
import com.ezla.oslauncher.themes.features.mine.IconPackAdapter
import kotlinx.coroutines.launch

// "See more" của 1 section tab Themes (Figma Wallpapers/Anime, Control Center/Anime): header back +
// tên danh mục, lưới 3 cột thẻ dọc; loại Icons dùng lưới 2 cột thẻ bộ icon như màn danh mục cũ.
class CategoryMoreActivity : BaseActivity<ThemesActivityIconCategoryBinding>() {
    override val setViewBinding: ThemesActivityIconCategoryBinding
        get() = ThemesActivityIconCategoryBinding.inflate(LayoutInflater.from(this))

    private val repository = ThemeRepositoryProvider.repository
    private val kind get() = HomeKind.valueOf(intent.getStringExtra(EXTRA_KIND) ?: HomeKind.THEME.name)
    private val categoryId get() = intent.getStringExtra(EXTRA_CATEGORY_ID).orEmpty()

    // Đã mở chi tiết -> quay về thì tải lại để nhãn New theo is_unlocked mới.
    private var reloadOnResume = false

    private val packs = IconPackAdapter { pack -> open { IconDetailActivity.start(this, pack.id) } }

    override fun initView() {
        binding.header.tvHeaderTitle.text = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val span = if (kind == HomeKind.ICON) ICON_SPAN else CARD_SPAN
        val gap = resources.getDimensionPixelSize(
            if (kind == HomeKind.ICON) R.dimen.themes_mine_gap else R.dimen.themes_grid_gap
        )
        binding.rvThemes.layoutManager = GridLayoutManager(this, span)
        binding.rvThemes.addItemDecoration(GridSpacingDecoration(span, gap))
    }

    override fun viewListener() {
        binding.header.imgBack.setOnClickListener { finish() }
    }

    override fun dataObservable() = load()

    override fun onResume() {
        super.onResume()
        if (reloadOnResume) {
            reloadOnResume = false
            load()
        }
    }

    private fun load() {
        lifecycleScope.launch {
            binding.pbLoading.visibility = View.VISIBLE
            val items = repository.getHomeItems(kind, categoryId)
            binding.rvThemes.adapter = if (kind == HomeKind.ICON) {
                packs.also {
                    it.submit(items.filterIsInstance<IconThemeCard>().map { c ->
                        IconPack(id = c.id, name = c.name, icons = c.icons, background = c.background)
                    })
                }
            } else {
                HomeCardAdapter(kind, items, ::onCard, onEmpty = {}, grid = true)
            }
            binding.pbLoading.visibility = View.GONE
        }
    }

    private fun onCard(card: Any) = open {
        when (card) {
            is ThemeItem -> ThemeViewActivity.start(this, card.id, categoryId)
            is PreviewCard -> WallpaperDetailActivity.start(
                this, card.image.toString(), card.name, card.backgroundId, card.coins, card.isUnlocked
            )
            is ControlCenterCard -> ControlCenterDetailActivity.start(this, card.themeId)
        }
    }

    // Inter (nếu có) rồi mở màn chi tiết; đánh dấu tải lại khi quay về.
    private fun open(action: () -> Unit) {
        ThemesInter.run(this) {
            if (isFinishing) return@run
            reloadOnResume = true
            action()
        }
    }

    companion object {
        private const val EXTRA_KIND = "extra_kind"
        private const val EXTRA_CATEGORY_ID = "extra_category_id"
        private const val EXTRA_TITLE = "extra_title"
        private const val CARD_SPAN = 3
        private const val ICON_SPAN = 2

        fun start(context: Context, kind: HomeKind, category: ThemeCategory) {
            val title = if (category.titleRes != 0) context.getString(category.titleRes) else category.title
            context.startActivity(
                Intent(context, CategoryMoreActivity::class.java)
                    .putExtra(EXTRA_KIND, kind.name)
                    .putExtra(EXTRA_CATEGORY_ID, category.id)
                    .putExtra(EXTRA_TITLE, title)
            )
        }
    }
}

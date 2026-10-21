package com.ezla.oslauncher.themes.features.mine

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ezla.oslauncher.themes.Models.MineTab
import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.data.DefaultThemeProvider
import com.ezla.oslauncher.themes.data.MineRepositoryProvider
import com.ezla.oslauncher.themes.databinding.ThemesActivityIconCategoryBinding
import com.ezla.oslauncher.themes.features.detail.IconDetailActivity
import com.ezla.oslauncher.themes.features.detail.ThemeViewActivity
import com.ezla.oslauncher.themes.features.detail.WallpaperDetailActivity
import com.ezla.oslauncher.themes.features.home.GridSpacingDecoration
import kotlinx.coroutines.launch

// Nút "more" ở Mine: toàn bộ My Library / Suggest của tab đang chọn (Mine chỉ hiện vài thẻ Suggest).
// Cùng nguồn + cùng hành động bấm thẻ như MineFragment; My Library vẫn có thẻ mặc định đứng đầu.
class MineMoreActivity : BaseActivity<ThemesActivityIconCategoryBinding>() {

    override val setViewBinding: ThemesActivityIconCategoryBinding
        get() = ThemesActivityIconCategoryBinding.inflate(LayoutInflater.from(this))

    private val repository = MineRepositoryProvider.repository
    private val tab get() = MineTab.valueOf(intent.getStringExtra(EXTRA_TAB) ?: MineTab.THEME.name)
    private val isLibrary get() = intent.getBooleanExtra(EXTRA_LIBRARY, true)

    private val cards = PreviewCardAdapter(small = false) { onCardClick(it) }
    private val packs = IconPackAdapter { IconDetailActivity.start(this, it.id) }

    override fun initView() {
        binding.header.tvHeaderTitle.setText(
            if (isLibrary) R.string.themes_mine_my_library else R.string.themes_mine_suggest
        )
        binding.rvThemes.layoutManager = GridLayoutManager(this, SPAN)
        binding.rvThemes.addItemDecoration(
            GridSpacingDecoration(SPAN, resources.getDimensionPixelSize(R.dimen.themes_mine_gap))
        )
        binding.rvThemes.adapter = if (tab == MineTab.ICON) packs else cards
    }

    override fun viewListener() {
        binding.header.imgBack.setOnClickListener { finish() }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            binding.pbLoading.visibility = View.VISIBLE
            val ctx = this@MineMoreActivity
            when (tab) {
                MineTab.ICON -> packs.submit(
                    if (isLibrary) listOf(DefaultThemeProvider.iconPack(ctx)) + repository.getOwnedIconPacks()
                    else repository.getSuggestIconPacks()
                )
                MineTab.WALLPAPERS -> cards.submit(
                    if (isLibrary) listOf(DefaultThemeProvider.wallpaper(ctx)) + repository.getOwnedWallpapers()
                    else repository.getSuggestWallpapers()
                )
                else -> cards.submit(
                    if (isLibrary) listOf(DefaultThemeProvider.theme(ctx)) + repository.getOwnedThemes()
                    else repository.getSuggestThemes()
                )
            }
            binding.pbLoading.visibility = View.GONE
        }
    }

    private fun onCardClick(card: PreviewCard) {
        if (tab != MineTab.WALLPAPERS) {
            ThemeViewActivity.start(this, card.id, null)
            return
        }
        if (card.isDefault) {
            WallpaperDetailActivity.startDefault(this, card.name)
            return
        }
        val url = card.image as? String ?: return
        WallpaperDetailActivity.start(this, url, card.name, card.backgroundId, card.coins, card.isUnlocked)
    }

    companion object {
        private const val EXTRA_TAB = "extra_tab"
        private const val EXTRA_LIBRARY = "extra_library"
        private const val SPAN = 2

        fun start(context: Context, tab: MineTab, isLibrary: Boolean) {
            context.startActivity(
                Intent(context, MineMoreActivity::class.java)
                    .putExtra(EXTRA_TAB, tab.name)
                    .putExtra(EXTRA_LIBRARY, isLibrary)
            )
        }
    }
}

package com.ezla.oslauncher.themes.features.detail

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.Models.ThemeTab
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.data.DefaultThemeProvider
import com.ezla.oslauncher.themes.data.MineRepositoryProvider
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
import com.ezla.oslauncher.themes.databinding.ThemesActivityThemeViewBinding
import com.ezla.oslauncher.themes.features.home.GridSpacingDecoration
import com.ezla.oslauncher.themes.features.mine.PreviewCardAdapter
import kotlinx.coroutines.launch
import kotlin.math.abs

// Màn View theme (Figma): carousel Home | Lock | Home của theme, Suggest = theme khác cùng danh mục,
// nút Install mở màn Install. Bấm thẻ Suggest -> xem theme đó (thay màn hiện tại).
class ThemeViewActivity : BaseActivity<ThemesActivityThemeViewBinding>() {

    override val setViewBinding: ThemesActivityThemeViewBinding
        get() = ThemesActivityThemeViewBinding.inflate(LayoutInflater.from(this))

    private val repository = ThemeRepositoryProvider.repository
    private val pagerAdapter = ThemeViewPagerAdapter()
    private val suggestAdapter = PreviewCardAdapter(small = true) { card ->
        start(this, card.id, categoryId)
        finish()
    }

    private val themeId get() = intent.getStringExtra(EXTRA_THEME_ID).orEmpty()
    private val categoryId get() = intent.getStringExtra(EXTRA_CATEGORY_ID)

    override fun initView() {
        binding.pagerPreview.adapter = pagerAdapter
        // Giữ sẵn cả 3 trang + tắt overscroll/hoạt ảnh item để vuốt không giật khi Glide nạp ảnh.
        binding.pagerPreview.offscreenPageLimit = 2
        (binding.pagerPreview.getChildAt(0) as? RecyclerView)?.apply {
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            itemAnimator = null
            setHasFixedSize(true)
        }
        fitPagePadding()
        binding.pagerPreview.setPageTransformer(sidePageTransformer())
        binding.pagerPreview.setCurrentItem(ThemeViewPagerAdapter.PAGE_LOCK, false)

        binding.rvSuggest.layoutManager = object : GridLayoutManager(this, SUGGEST_SPAN) {
            override fun canScrollVertically() = false
        }
        binding.rvSuggest.addItemDecoration(
            GridSpacingDecoration(SUGGEST_SPAN, resources.getDimensionPixelSize(R.dimen.themes_mine_gap))
        )
        binding.rvSuggest.adapter = suggestAdapter
    }

    override fun viewListener() {
        binding.imgClose.setOnClickListener { finish() }
        binding.btnInstall.setOnClickListener { ThemeDetailActivity.start(this, themeId) }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            val detail = DefaultThemeProvider.loadDetail(this@ThemeViewActivity, repository, themeId)
            pagerAdapter.submit(
                background = detail?.wallpapers?.firstOrNull() ?: detail?.previewUrl,
                thumbnail = detail?.previewUrl
            )
        }
        // Có danh mục -> theme cùng danh mục; không có (mở từ Mine / theme mặc định) -> gợi ý chung
        // từ /themes như tab Mine (bỏ theme đã mở khoá).
        lifecycleScope.launch {
            val category = categoryId
            val suggest = if (category != null) {
                repository.getThemes(ThemeTab.FEATURED, category)
                    .map { PreviewCard(it.id, it.name, it.previewUrl) }
            } else {
                MineRepositoryProvider.repository.getSuggestThemes()
            }
            suggestAdapter.submit(suggest.filter { it.id != themeId })
        }
    }

    // Bề ngang trang = chiều cao x tỉ lệ ảnh (960x2082) để fitXY không méo; phần dư làm padding 2 bên.
    private fun fitPagePadding() {
        val pageWidth = (resources.getDimensionPixelSize(R.dimen.themes_view_preview_height) * IMAGE_RATIO).toInt()
        val side = ((resources.displayMetrics.widthPixels - pageWidth) / 2).coerceAtLeast(0)
        binding.pagerPreview.setPadding(side, 0, side, 0)
    }

    // Trang giữa giữ nguyên; trang 2 bên thu nhỏ còn 124/146 (tỉ lệ Figma) và ép sát vào giữa.
    private fun sidePageTransformer() = ViewPager2.PageTransformer { page, position ->
        val scale = 1f - (1f - SIDE_SCALE) * minOf(abs(position), 1f)
        page.scaleX = scale
        page.scaleY = scale
        page.translationX = -position * page.width * PAGE_OVERLAP
    }

    companion object {
        private const val EXTRA_THEME_ID = "extra_theme_id"
        private const val EXTRA_CATEGORY_ID = "extra_category_id"
        private const val SUGGEST_SPAN = 3
        private const val SIDE_SCALE = 124f / 146f
        private const val PAGE_OVERLAP = 0.04f
        private const val IMAGE_RATIO = 1041f / 2082f

        fun start(context: Context, themeId: String, categoryId: String?) {
            context.startActivity(
                Intent(context, ThemeViewActivity::class.java)
                    .putExtra(EXTRA_THEME_ID, themeId)
                    .putExtra(EXTRA_CATEGORY_ID, categoryId)
            )
        }
    }
}

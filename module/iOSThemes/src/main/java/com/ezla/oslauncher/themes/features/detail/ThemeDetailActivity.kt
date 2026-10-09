package com.ezla.oslauncher.themes.features.detail

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.ezla.oslauncher.themes.Models.ThemeDetail
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.apply.ThemeApplyEngine
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.data.CoinStore
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
import com.ezla.oslauncher.themes.databinding.ThemesActivityThemeDetailBinding
import com.ezla.oslauncher.themes.features.rewards.RewardsActivity
import kotlinx.coroutines.launch

// Màn Install theme (Figma bg6). Preview VUỐT giữa wallpaper và icons. Bấm Unlock -> trừ xu
// (thiếu thì hiện bg7) -> nút chuyển thành Apply; bấm Apply -> thay nền + icon luôn (cả Home & Lock).
// Trạng thái mở khoá KHÔNG lưu (theo yêu cầu) nên vào lại màn là về Unlock.
class ThemeDetailActivity : BaseActivity<ThemesActivityThemeDetailBinding>() {

    override val setViewBinding: ThemesActivityThemeDetailBinding
        get() = ThemesActivityThemeDetailBinding.inflate(LayoutInflater.from(this))

    private val repository = ThemeRepositoryProvider.repository
    private var detail: ThemeDetail? = null
    private var unlocked = false

    // Màn IAP bổ sung sau: để callback cho nút premium / Unlock All Icon.
    var onPremiumClick: ((ThemeDetail?) -> Unit)? = null

    private val themeId get() = intent.getStringExtra(EXTRA_THEME_ID).orEmpty()

    override fun initView() {
        binding.tvTitle.setText(R.string.themes_install_title)
        binding.tvStep1Badge.text = "1"
        binding.tvStep1Label.setText(R.string.themes_nav_wallpapers)
        binding.tvStep2Badge.text = "2"
        binding.tvStep2Label.setText(R.string.themes_nav_icons)
        binding.pagerPreview.offscreenPageLimit = 1
        renderStep(PAGE_WALLPAPER)
        refreshCoin()
        listenNotEnough()
        loadDetail()
    }

    override fun viewListener() {
        binding.imgBack.setOnClickListener { finish() }
        binding.layoutCoin.setOnClickListener { onPremiumClick?.invoke(detail) }
        binding.btnUnlock.setOnClickListener { onPrimaryClick() }
        binding.stepWallpapers.setOnClickListener { binding.pagerPreview.setCurrentItem(PAGE_WALLPAPER, true) }
        binding.stepIcons.setOnClickListener { binding.pagerPreview.setCurrentItem(PAGE_ICONS, true) }
        binding.pagerPreview.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = renderStep(position)
        })
    }

    override fun dataObservable() {}

    private fun loadDetail() {
        lifecycleScope.launch {
            val d = repository.getThemeDetail(themeId)
            detail = d
            renderButton()
            binding.pagerPreview.adapter = ThemePreviewAdapter(
                wallpaperUrl = d?.wallpapers?.firstOrNull() ?: d?.previewUrl,
                iconUrls = d?.logos?.map { it.imageUrl }.orEmpty(),
                gapPx = resources.getDimensionPixelSize(R.dimen.themes_grid_gap)
            )
        }
    }

    // Nút: chưa mở -> "Unlock with N"; đã mở -> "Apply" (ẩn icon xu).
    private fun renderButton() {
        if (unlocked) {
            binding.tvBtnLabel.setText(R.string.themes_apply)
            binding.imgBtnCoin.visibility = View.GONE
        } else {
            binding.tvBtnLabel.setText(getString(R.string.themes_unlock_with, detail?.coins ?: 0))
            binding.imgBtnCoin.visibility = View.VISIBLE
        }
    }

    private fun onPrimaryClick() {
        val d = detail
        if (d == null) {
            onPremiumClick?.invoke(null)
            return
        }
        if (unlocked) {
            applyTheme(d)
            return
        }
        val price = d.coins
        // Giá > 0 mà không đủ xu -> bg7 Not Enough Coins (không trừ gì).
        if (price > 0 && !CoinStore.spend(this, price)) {
            NotEnoughCoinsDialog.newInstance().show(supportFragmentManager, TAG_NOT_ENOUGH)
            return
        }
        unlocked = true
        refreshCoin()
        renderButton()
    }

    // Apply: thay nền + icon luôn (cả Home lẫn Lock) — không hỏi nơi áp.
    private fun applyTheme(d: ThemeDetail) {
        lifecycleScope.launch {
            val ok = ThemeApplyEngine.applyTheme(this@ThemeDetailActivity, d, applyHome = true, applyLock = true)
            if (ok) SuccessfullySetDialog.newInstance(d.wallpapers.firstOrNull() ?: d.previewUrl)
                .show(supportFragmentManager, TAG_SUCCESS)
        }
    }

    private fun listenNotEnough() {
        supportFragmentManager.setFragmentResultListener(
            NotEnoughCoinsDialog.REQUEST_KEY, this
        ) { _, bundle ->
            when (bundle.getString(NotEnoughCoinsDialog.KEY_ACTION)) {
                NotEnoughCoinsDialog.ACTION_GET_COIN -> showActivity(RewardsActivity::class.java, null)
                NotEnoughCoinsDialog.ACTION_UNLOCK_ALL -> onPremiumClick?.invoke(detail)
            }
        }
    }

    private fun refreshCoin() {
        binding.tvCoin.text = CoinStore.balance(this).toString()
    }

    // Đổi màu chữ + gạch chân theo trang đang xem (0 = Wallpapers, 1 = Icons).
    private fun renderStep(position: Int) {
        val onWallpaper = position == PAGE_WALLPAPER
        val active = ContextCompat.getColor(this, R.color.themes_pink)
        val inactive = ContextCompat.getColor(this, R.color.themes_text_hint)
        binding.tvStep1Label.setTextColor(if (onWallpaper) active else inactive)
        binding.tvStep2Label.setTextColor(if (onWallpaper) inactive else active)
        binding.lineWallpapers.visibility = if (onWallpaper) View.VISIBLE else View.INVISIBLE
        binding.lineIcons.visibility = if (onWallpaper) View.INVISIBLE else View.VISIBLE
    }

    companion object {
        private const val EXTRA_THEME_ID = "extra_theme_id"
        private const val TAG_NOT_ENOUGH = "not_enough_coins"
        private const val TAG_SUCCESS = "successfully_set"
        private const val PAGE_WALLPAPER = 0
        private const val PAGE_ICONS = 1

        fun start(context: Context, themeId: String) {
            context.startActivity(
                Intent(context, ThemeDetailActivity::class.java).putExtra(EXTRA_THEME_ID, themeId)
            )
        }
    }
}

package com.ezla.oslauncher.themes.features.detail

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.apply.ThemeApplyEngine
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.data.CoinStore
import com.ezla.oslauncher.themes.databinding.ThemesActivityWallpaperDetailBinding
import com.ezla.oslauncher.themes.features.rewards.RewardsActivity
import kotlinx.coroutines.launch

// Màn wallpaper detail (Figma bg11). Apply -> "Wallpaper locked" (bg8) -> "Choose where" (bg9)
// -> áp nền -> "Successfully Set!" (bg14). Nút Go Premium để callback IAP (màn IAP bổ sung sau).
class WallpaperDetailActivity : BaseActivity<ThemesActivityWallpaperDetailBinding>() {

    override val setViewBinding: ThemesActivityWallpaperDetailBinding
        get() = ThemesActivityWallpaperDetailBinding.inflate(LayoutInflater.from(this))

    private val imageUrl get() = intent.getStringExtra(EXTRA_URL).orEmpty()
    private val wallpaperName get() = intent.getStringExtra(EXTRA_NAME).orEmpty()

    var onPremiumClick: ((String) -> Unit)? = null

    override fun initView() {
        binding.tvTitle.setText(R.string.themes_install_title)
        Glide.with(this).load(imageUrl).into(binding.imgWallpaper)
        refreshCoin()
        listenLocked()
        listenChooseWhere()
        listenNotEnough()
    }

    override fun viewListener() {
        binding.imgBack.setOnClickListener { finish() }
        binding.layoutCoin.setOnClickListener { onPremiumClick?.invoke(imageUrl) }
        binding.btnFavorite.setOnClickListener { /* Yêu thích: bổ sung sau. */ }
        binding.btnShare.setOnClickListener { shareWallpaper() }
        binding.btnApply.setOnClickListener {
            WallpaperLockedDialog.newInstance().show(supportFragmentManager, TAG_LOCKED)
        }
    }

    override fun dataObservable() {}

    // bg8 -> Use coins: trừ xu rồi sang chọn nơi áp (thiếu thì bg7); Go Premium: callback IAP.
    private fun listenLocked() {
        supportFragmentManager.setFragmentResultListener(
            WallpaperLockedDialog.REQUEST_KEY, this
        ) { _, bundle ->
            when (bundle.getString(WallpaperLockedDialog.KEY_ACTION)) {
                WallpaperLockedDialog.ACTION_USE_COINS -> useCoins()
                WallpaperLockedDialog.ACTION_PREMIUM -> onPremiumClick?.invoke(imageUrl)
            }
        }
    }

    private fun useCoins() {
        if (CoinStore.spend(this, WallpaperLockedDialog.PRICE)) {
            refreshCoin()
            showChooseWhere()
        } else {
            NotEnoughCoinsDialog.newInstance().show(supportFragmentManager, TAG_NOT_ENOUGH)
        }
    }

    private fun listenNotEnough() {
        supportFragmentManager.setFragmentResultListener(
            NotEnoughCoinsDialog.REQUEST_KEY, this
        ) { _, bundle ->
            when (bundle.getString(NotEnoughCoinsDialog.KEY_ACTION)) {
                NotEnoughCoinsDialog.ACTION_GET_COIN -> showActivity(RewardsActivity::class.java, null)
                NotEnoughCoinsDialog.ACTION_UNLOCK_ALL -> onPremiumClick?.invoke(imageUrl)
            }
        }
    }

    private fun refreshCoin() {
        binding.tvCoin.text = CoinStore.balance(this).toString()
    }

    // bg9 Next -> áp nền rồi mở sheet "Successfully Set" (bg14).
    private fun listenChooseWhere() {
        supportFragmentManager.setFragmentResultListener(
            ChooseWhereToApplyDialog.REQUEST_KEY, this
        ) { _, bundle ->
            val lock = bundle.getBoolean(ChooseWhereToApplyDialog.KEY_LOCK)
            val home = bundle.getBoolean(ChooseWhereToApplyDialog.KEY_HOME)
            lifecycleScope.launch {
                val ok = ThemeApplyEngine.applyWallpaper(this@WallpaperDetailActivity, imageUrl, home, lock)
                if (ok) SuccessfullySetDialog.newInstance(imageUrl)
                    .show(supportFragmentManager, TAG_SUCCESS)
            }
        }
    }

    private fun showChooseWhere() {
        ChooseWhereToApplyDialog.newInstance(lockPreview = imageUrl, homePreview = imageUrl)
            .show(supportFragmentManager, TAG_CHOOSE)
    }

    private fun shareWallpaper() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, wallpaperName)
        }
        runCatching { startActivity(Intent.createChooser(send, getString(R.string.themes_cd_share))) }
    }

    companion object {
        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_NAME = "extra_name"
        private const val TAG_LOCKED = "wallpaper_locked"
        private const val TAG_CHOOSE = "choose_where_apply"
        private const val TAG_SUCCESS = "successfully_set"
        private const val TAG_NOT_ENOUGH = "not_enough_coins"

        fun start(context: Context, imageUrl: String, name: String) {
            context.startActivity(
                Intent(context, WallpaperDetailActivity::class.java)
                    .putExtra(EXTRA_URL, imageUrl)
                    .putExtra(EXTRA_NAME, name)
            )
        }
    }
}

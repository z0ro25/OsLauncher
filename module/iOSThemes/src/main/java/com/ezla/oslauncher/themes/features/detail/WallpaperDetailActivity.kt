package com.ezla.oslauncher.themes.features.detail

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.ThemesEntry
import com.ezla.oslauncher.themes.apply.ThemeApplyEngine
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.base.ApplyTask
import com.ezla.oslauncher.themes.base.LoadingOverlay
import com.ezla.oslauncher.themes.data.CreditRejectedException
import com.ezla.oslauncher.themes.data.CreditRepository
import com.ezla.oslauncher.themes.data.DefaultThemeProvider
import com.ezla.oslauncher.themes.data.SubsRepository
import com.ezla.oslauncher.themes.data.favorite.FavoriteRepository
import com.ezla.oslauncher.themes.features.ApplyTracker
import com.ezla.oslauncher.themes.features.ThemesInter
import com.ezla.oslauncher.themes.features.loadCreditInto
import com.ezla.oslauncher.themes.features.shareAppOnStore
import com.ezla.oslauncher.themes.databinding.ThemesActivityWallpaperDetailBinding
import com.ezla.oslauncher.themes.features.rewards.RewardsActivity
import kotlinx.coroutines.launch

// Màn wallpaper detail (Figma bg11). Apply: chưa mở khoá -> "Wallpaper locked" (bg8) -> /unlocks
// -> "Choose where" (bg9); đã mở khoá -> thẳng bg9. Áp nền -> "Successfully Set!" (bg14).
// Nút Go Premium để callback IAP (màn IAP bổ sung sau).
class WallpaperDetailActivity : BaseActivity<ThemesActivityWallpaperDetailBinding>() {

    override val setViewBinding: ThemesActivityWallpaperDetailBinding
        get() = ThemesActivityWallpaperDetailBinding.inflate(LayoutInflater.from(this))

    private val imageUrl get() = intent.getStringExtra(EXTRA_URL).orEmpty()
    private val wallpaperName get() = intent.getStringExtra(EXTRA_NAME).orEmpty()
    private val backgroundId get() = intent.getIntExtra(EXTRA_BACKGROUND_ID, 0)
    private val price get() = intent.getIntExtra(EXTRA_COINS, 0)
    // Nền mặc định của launcher: luôn mở khoá, Apply = đặt lại ảnh drawable mặc định.
    private val isDefault get() = intent.getBooleanExtra(EXTRA_DEFAULT, false)

    // Khởi tạo từ is_unlocked server trả; mở khoá thành công thì bật lên (không cần mở lại).
    private var unlocked = false
    private var unlocking = false
    private val loading by lazy { LoadingOverlay(this) }
    private var favorite = false
    private val favoriteKey get() = FavoriteRepository.wallpaperKey(isDefault, backgroundId, imageUrl)

    var onPremiumClick: ((String) -> Unit)? = null

    override fun initView() {
        showBannerOverlay(binding.frBanner, binding.mainLayout, false)
        // Đổi nền -> màn bị tạo lại giữa lúc áp: nhận kết quả + hiện lại loading.
        applyTask.attach(this)
        if (applyTask.running) loading.show()
        unlocked = intent.getBooleanExtra(EXTRA_UNLOCKED, false)
        binding.tvTitle.setText(R.string.themes_install_title)
        Glide.with(this).load(imageUrl).into(binding.imgWallpaper)
        listenLocked()
        listenChooseWhere()
        listenNotEnough()
        loadFavorite()
    }

    override fun viewListener() {
        binding.imgBack.setOnClickListener { finish() }
        // Cùng hành vi badge xu ở Themes/Theme detail/Icon detail: mở màn kiếm xu.
        binding.layoutCoin.setOnClickListener { showActivity(RewardsActivity::class.java, null) }
        binding.btnFavorite.setOnClickListener { toggleFavorite() }
        binding.btnShare.setOnClickListener { shareAppOnStore() }
        binding.btnApply.setOnClickListener {
            when {
                // Đi tới bước áp -> inter (nếu có) trước; mở sheet khoá (dùng xu) thì không inter.
                unlocked -> ThemesInter.run(this) { if (!isFinishing) showChooseWhere() }
                // Subs: bỏ sheet "Wallpaper locked", /unlocks (server miễn xu) rồi sang chọn nơi áp.
                SubsRepository.isSubscribed(this) -> ThemesInter.run(this) { if (!isFinishing) useCoins() }
                else -> WallpaperLockedDialog.newInstance(price).show(supportFragmentManager, TAG_LOCKED)
            }
        }
    }

    override fun dataObservable() {}

    // Có thể vừa mua subs ở màn Subs rồi quay lại -> ẩn/hiện badge xu theo trạng thái mới.
    override fun onResume() {
        super.onResume()
        val subscribed = SubsRepository.isSubscribed(this)
        binding.layoutCoin.visibility = if (subscribed) View.GONE else View.VISIBLE
        if (!subscribed) refreshCoin()
    }

    // bg8 -> Use coins: mở khoá qua /unlocks rồi sang chọn nơi áp; Go Premium: callback IAP.
    private fun listenLocked() {
        supportFragmentManager.setFragmentResultListener(
            WallpaperLockedDialog.REQUEST_KEY, this
        ) { _, bundle ->
            when (bundle.getString(WallpaperLockedDialog.KEY_ACTION)) {
                WallpaperLockedDialog.ACTION_USE_COINS -> useCoins()
                WallpaperLockedDialog.ACTION_PREMIUM -> openPremium()
            }
        }
    }

    // Mở khoá nền qua /unlocks (server trừ xu, trả số dư mới). Server từ chối (thiếu xu) -> Not
    // Enough Coins; lỗi mạng -> toast. Chặn bấm lặp để không gửi 2 lần.
    private fun useCoins() {
        if (unlocking) return
        if (backgroundId <= 0) return
        unlocking = true
        lifecycleScope.launch {
            CreditRepository.unlock(CreditRepository.UNLOCK_BACKGROUND, backgroundId)
                .onSuccess { credit ->
                    unlocked = true
                    binding.tvCoin.text = credit.toString()
                    showChooseWhere()
                }
                .onFailure { error ->
                    if (error is CreditRejectedException) {
                        NotEnoughCoinsDialog.newInstance().show(supportFragmentManager, TAG_NOT_ENOUGH)
                    } else {
                        Toast.makeText(this@WallpaperDetailActivity, R.string.themes_network_error, Toast.LENGTH_SHORT).show()
                    }
                }
            unlocking = false
        }
    }

    // Go Premium / Unlock All -> màn Subs (IAP) của :app qua ThemesEntry; callback cũ vẫn được báo.
    private fun openPremium() {
        onPremiumClick?.invoke(imageUrl)
        ThemesEntry.openPremium(this)
    }

    private fun listenNotEnough() {
        supportFragmentManager.setFragmentResultListener(
            NotEnoughCoinsDialog.REQUEST_KEY, this
        ) { _, bundle ->
            when (bundle.getString(NotEnoughCoinsDialog.KEY_ACTION)) {
                NotEnoughCoinsDialog.ACTION_GET_COIN -> showActivity(RewardsActivity::class.java, null)
                NotEnoughCoinsDialog.ACTION_UNLOCK_ALL -> openPremium()
            }
        }
    }

    private fun refreshCoin() {
        loadCreditInto(binding.tvCoin)
    }

    // bg9 Next -> áp nền rồi mở sheet "Successfully Set" (bg14).
    private fun listenChooseWhere() {
        supportFragmentManager.setFragmentResultListener(
            ChooseWhereToApplyDialog.REQUEST_KEY, this
        ) { _, bundle ->
            val lock = bundle.getBoolean(ChooseWhereToApplyDialog.KEY_LOCK)
            val home = bundle.getBoolean(ChooseWhereToApplyDialog.KEY_HOME)
            if (applyTask.running) return@setFragmentResultListener
            loading.show()
            val appCtx = applicationContext
            val default = isDefault
            val url = imageUrl
            val name = wallpaperName
            applyTask.run({
                if (default) {
                    ThemeApplyEngine.restoreDefaultWallpaper(appCtx, DefaultThemeProvider.WALLPAPER_RES, home, lock)
                } else {
                    ThemeApplyEngine.applyWallpaper(appCtx, url, home, lock)
                }
            }) { live, ok ->
                live?.loading?.hide()
                if (ok) ApplyTracker.log(name, ApplyTracker.TYPE_BACKGROUND)
                if (ok) live?.let {
                    SuccessfullySetDialog.newInstance(url).show(it.supportFragmentManager, TAG_SUCCESS)
                }
            }
        }
    }

    override fun onDestroy() {
        applyTask.detach(this)
        super.onDestroy()
    }

    private fun showChooseWhere() {
        ChooseWhereToApplyDialog.newInstance(lockPreview = imageUrl, homePreview = imageUrl)
            .show(supportFragmentManager, TAG_CHOOSE)
    }

    private fun loadFavorite() {
        lifecycleScope.launch {
            favorite = FavoriteRepository.isWallpaperFavorite(this@WallpaperDetailActivity, favoriteKey)
            renderFavorite()
        }
    }

    // Thả tim / bỏ tim (local); lưu kèm id + giá để mở lại từ Mine > Favorites.
    private fun toggleFavorite() {
        lifecycleScope.launch {
            favorite = FavoriteRepository.toggleWallpaper(
                this@WallpaperDetailActivity, favoriteKey, wallpaperName, imageUrl, backgroundId, price
            )
            renderFavorite()
        }
    }

    private fun renderFavorite() {
        binding.imgFavorite.setImageResource(
            if (favorite) R.drawable.themes_ic_heart_filled else R.drawable.themes_ic_heart
        )
    }

    companion object {
        // Sống ngoài instance: lượt áp phải vượt qua recreate của màn (xem ApplyTask).
        private val applyTask = ApplyTask<WallpaperDetailActivity>()

        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_NAME = "extra_name"
        private const val EXTRA_BACKGROUND_ID = "extra_background_id"
        private const val EXTRA_COINS = "extra_coins"
        private const val EXTRA_UNLOCKED = "extra_unlocked"
        private const val EXTRA_DEFAULT = "extra_default"
        private const val TAG_LOCKED = "wallpaper_locked"
        private const val TAG_CHOOSE = "choose_where_apply"
        private const val TAG_SUCCESS = "successfully_set"
        private const val TAG_NOT_ENOUGH = "not_enough_coins"

        // backgroundId/coins/unlocked lấy từ background của theme (API) để gọi /unlocks.
        fun start(
            context: Context,
            imageUrl: String,
            name: String,
            backgroundId: Int,
            coins: Int,
            unlocked: Boolean
        ) {
            context.startActivity(
                Intent(context, WallpaperDetailActivity::class.java)
                    .putExtra(EXTRA_URL, imageUrl)
                    .putExtra(EXTRA_NAME, name)
                    .putExtra(EXTRA_BACKGROUND_ID, backgroundId)
                    .putExtra(EXTRA_COINS, coins)
                    .putExtra(EXTRA_UNLOCKED, unlocked)
            )
        }

        fun startDefault(context: Context, name: String) {
            context.startActivity(
                Intent(context, WallpaperDetailActivity::class.java)
                    .putExtra(EXTRA_URL, DefaultThemeProvider.wallpaperUri(context))
                    .putExtra(EXTRA_NAME, name)
                    .putExtra(EXTRA_UNLOCKED, true)
                    .putExtra(EXTRA_DEFAULT, true)
            )
        }
    }
}

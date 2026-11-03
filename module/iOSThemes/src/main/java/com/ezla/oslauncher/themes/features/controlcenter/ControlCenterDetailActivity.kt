package com.ezla.oslauncher.themes.features.controlcenter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.Models.ControlCenterCard
import com.ezla.oslauncher.themes.Models.ThemeDetail
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.apply.ControlCenterSkinApplier
import com.ezla.oslauncher.themes.base.ApplyTask
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.base.LoadingOverlay
import com.ezla.oslauncher.themes.data.DefaultThemeProvider
import com.ezla.oslauncher.themes.data.SubsRepository
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
import com.ezla.oslauncher.themes.data.favorite.FavoriteRepository
import com.ezla.oslauncher.themes.databinding.ThemesActivityControlCenterDetailBinding
import com.ezla.oslauncher.themes.features.ApplyTracker
import com.ezla.oslauncher.themes.features.ThemesInter
import com.ezla.oslauncher.themes.features.loadCreditInto
import com.ezla.oslauncher.themes.features.rewards.RewardsActivity
import com.ezla.oslauncher.themes.features.shareAppOnStore
import kotlinx.coroutines.launch

// Màn Control Center detail (Figma Unlock Control Center): khung nền theme + panel xem trước skin,
// hàng [tim][Apply][chia sẻ]. Apply tải ảnh control_systems rồi cài vào Control Center -> sheet
// Successfully Set. Server chưa có giá / unlock cho Control Center nên Apply không trừ xu.
class ControlCenterDetailActivity : BaseActivity<ThemesActivityControlCenterDetailBinding>() {

    override val setViewBinding: ThemesActivityControlCenterDetailBinding
        get() = ThemesActivityControlCenterDetailBinding.inflate(LayoutInflater.from(this))

    private val repository = ThemeRepositoryProvider.repository
    private val themeId get() = intent.getStringExtra(EXTRA_THEME_ID).orEmpty()
    private val isDefault get() = DefaultThemeProvider.isDefault(themeId)

    private var detail: ThemeDetail? = null
    private var favorite = false
    private val loading by lazy { LoadingOverlay(this) }
    private val preview by lazy { CcPreviewBinder(binding.frCcPreview) }

    override fun initView() {
        showBannerOverlay(binding.frBanner, binding.mainLayout, false)
        // Màn có thể bị tạo lại giữa lúc áp: nhận kết quả + hiện lại loading.
        applyTask.attach(this)
        if (applyTask.running) loading.show()
        binding.tvTitle.setText(R.string.themes_install_title)
        loadDetail()
    }

    override fun viewListener() {
        binding.imgBack.setOnClickListener { finish() }
        binding.layoutCoin.setOnClickListener { showActivity(RewardsActivity::class.java, null) }
        binding.btnFavorite.setOnClickListener { toggleFavorite() }
        binding.btnShare.setOnClickListener { shareAppOnStore() }
        binding.btnApply.setOnClickListener {
            ThemesInter.run(this) { if (!isFinishing) apply() }
        }
    }

    override fun dataObservable() {}

    override fun onResume() {
        super.onResume()
        val subscribed = SubsRepository.isSubscribed(this)
        binding.layoutCoin.visibility = if (subscribed) View.GONE else View.VISIBLE
        if (!subscribed) loadCreditInto(binding.tvCoin)
    }

    // Theme mặc định = Control Center gốc (nền kính); theme khác lấy chi tiết từ API.
    private fun loadDetail() {
        lifecycleScope.launch {
            binding.pbLoading.visibility = View.VISIBLE
            val d = DefaultThemeProvider.loadDetail(this@ControlCenterDetailActivity, repository, themeId)
            detail = d
            Glide.with(this@ControlCenterDetailActivity)
                .load(d?.wallpapers?.firstOrNull() ?: d?.previewUrl)
                .into(binding.imgBackground)
            preview.bind(d?.controlSystems.orEmpty()) { binding.pbLoading.visibility = View.GONE }
            favorite = FavoriteRepository.isControlCenterFavorite(this@ControlCenterDetailActivity, themeId)
            renderFavorite()
            binding.btnApply.isEnabled = d != null && (isDefault || d.controlSystems.isNotEmpty())
        }
    }

    private fun apply() {
        val d = detail ?: return
        if (applyTask.running) return
        loading.show()
        val appCtx = applicationContext
        val id = d.id
        val skins = d.controlSystems
        val default = isDefault
        val name = d.name
        val previewUrl = d.wallpapers.firstOrNull() ?: d.previewUrl
        applyTask.run({
            if (default) {
                ControlCenterSkinApplier.restoreDefault(appCtx)
                true
            } else {
                ControlCenterSkinApplier.apply(appCtx, id, skins)
            }
        }) { live, ok ->
            live?.loading?.hide()
            if (!ok) {
                Toast.makeText(live ?: appCtx, R.string.themes_apply_failed, Toast.LENGTH_SHORT).show()
            } else {
                ApplyTracker.log(name, ApplyTracker.TYPE_CONTROL_CENTER)
                live?.let {
                    ControlCenterSuccessDialog.newInstance(previewUrl, skins)
                        .show(it.supportFragmentManager, TAG_SUCCESS)
                }
            }
        }
    }

    // Thả tim / bỏ tim (local); lưu nền + skin để mở lại từ Mine > Favorites.
    private fun toggleFavorite() {
        val d = detail ?: return
        lifecycleScope.launch {
            favorite = FavoriteRepository.toggleControlCenter(
                this@ControlCenterDetailActivity,
                ControlCenterCard(d.id, d.name, d.wallpapers.firstOrNull() ?: d.previewUrl, d.controlSystems)
            )
            renderFavorite()
        }
    }

    private fun renderFavorite() {
        binding.imgFavorite.setImageResource(
            if (favorite) R.drawable.themes_ic_heart_filled else R.drawable.themes_ic_heart
        )
    }

    override fun onDestroy() {
        applyTask.detach(this)
        super.onDestroy()
    }

    companion object {
        // Sống ngoài instance: lượt áp phải vượt qua recreate của màn (xem ApplyTask).
        private val applyTask = ApplyTask<ControlCenterDetailActivity>()

        private const val EXTRA_THEME_ID = "extra_theme_id"
        private const val TAG_SUCCESS = "cc_successfully_set"

        fun start(context: Context, themeId: String) {
            context.startActivity(
                Intent(context, ControlCenterDetailActivity::class.java).putExtra(EXTRA_THEME_ID, themeId)
            )
        }
    }
}

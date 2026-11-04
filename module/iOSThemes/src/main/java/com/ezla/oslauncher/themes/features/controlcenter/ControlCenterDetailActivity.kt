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
import com.ezla.oslauncher.themes.ThemesEntry
import com.ezla.oslauncher.themes.apply.ControlCenterSkinApplier
import com.ezla.oslauncher.themes.base.ApplyTask
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.base.LoadingOverlay
import com.ezla.oslauncher.themes.data.CreditRejectedException
import com.ezla.oslauncher.themes.data.CreditRepository
import com.ezla.oslauncher.themes.data.DefaultThemeProvider
import com.ezla.oslauncher.themes.data.SubsRepository
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
import com.ezla.oslauncher.themes.data.favorite.FavoriteRepository
import com.ezla.oslauncher.themes.databinding.ThemesActivityControlCenterDetailBinding
import com.ezla.oslauncher.themes.features.ApplyTracker
import com.ezla.oslauncher.themes.features.ThemesInter
import com.ezla.oslauncher.themes.features.detail.NotEnoughCoinsDialog
import com.ezla.oslauncher.themes.features.loadCreditInto
import com.ezla.oslauncher.themes.features.rewards.RewardsActivity
import com.ezla.oslauncher.themes.features.shareAppOnStore
import kotlinx.coroutines.launch

// Màn Control Center detail (Figma Unlock Control Center): khung nền theme + panel xem trước skin,
// hàng [tim][Apply][chia sẻ]. Chưa mở khoá: "Unlock with N" -> /unlocks theme_control_systems; đã mở
// (hoặc subs): Apply tải ảnh ô + màu control_systems rồi cài vào Control Center -> sheet Successfully Set.
class ControlCenterDetailActivity : BaseActivity<ThemesActivityControlCenterDetailBinding>() {

    override val setViewBinding: ThemesActivityControlCenterDetailBinding
        get() = ThemesActivityControlCenterDetailBinding.inflate(LayoutInflater.from(this))

    private val repository = ThemeRepositoryProvider.repository
    private val themeId get() = intent.getStringExtra(EXTRA_THEME_ID).orEmpty()
    private val isDefault get() = DefaultThemeProvider.isDefault(themeId)

    private var detail: ThemeDetail? = null
    private var favorite = false
    private var unlocked = false
    private var unlocking = false
    private var subscribed = false
    private val loading by lazy { LoadingOverlay(this) }
    private val preview by lazy { CcPreviewBinder(binding.frCcPreview) }

    override fun initView() {
        showBannerOverlay(binding.frBanner, binding.mainLayout, false)
        // Màn có thể bị tạo lại giữa lúc áp: nhận kết quả + hiện lại loading.
        applyTask.attach(this)
        if (applyTask.running) loading.show()
        binding.tvTitle.setText(R.string.themes_install_title)
        listenNotEnough()
        loadDetail()
    }

    override fun viewListener() {
        binding.imgBack.setOnClickListener { finish() }
        binding.layoutCoin.setOnClickListener { showActivity(RewardsActivity::class.java, null) }
        binding.btnFavorite.setOnClickListener { toggleFavorite() }
        binding.btnShare.setOnClickListener { shareAppOnStore() }
        binding.btnApply.setOnClickListener {
            when {
                // Nút đang là "Unlock with N": chỉ mở khoá (dùng xu), không inter.
                !unlocked && !subscribed -> unlock()
                // Subs còn chưa sở hữu: /unlocks (server miễn xu) rồi áp.
                !unlocked -> ThemesInter.run(this) { if (!isFinishing) unlock { apply() } }
                else -> ThemesInter.run(this) { if (!isFinishing) apply() }
            }
        }
    }

    override fun dataObservable() {}

    override fun onResume() {
        super.onResume()
        subscribed = SubsRepository.isSubscribed(this)
        binding.layoutCoin.visibility = if (subscribed) View.GONE else View.VISIBLE
        if (!subscribed) loadCreditInto(binding.tvCoin)
        renderApply()
    }

    // Chưa mở khoá (và không subs): "Unlock with N" + xu; còn lại: Apply.
    private fun renderApply() {
        val locked = !unlocked && !subscribed
        binding.tvApply.text = if (locked) {
            getString(R.string.themes_unlock_with, detail?.controlSkin?.coins ?: 0)
        } else {
            getString(R.string.themes_apply)
        }
        binding.imgApplyCoin.visibility = if (locked) View.VISIBLE else View.GONE
    }

    // /unlocks theme_control_systems (id = theme; server trừ xu, trả số dư mới). Thiếu xu -> Not Enough
    // Coins; lỗi mạng -> toast. Chặn bấm lặp để không gửi 2 lần.
    private fun unlock(onUnlocked: (() -> Unit)? = null) {
        val id = detail?.id?.toIntOrNull() ?: return
        if (unlocking || id <= 0) return
        unlocking = true
        lifecycleScope.launch {
            val result = CreditRepository.unlock(CreditRepository.UNLOCK_CONTROL_SYSTEMS, id)
            unlocking = false
            result
                .onSuccess { credit ->
                    unlocked = true
                    binding.tvCoin.text = credit.toString()
                    renderApply()
                    onUnlocked?.invoke()
                }
                .onFailure { error ->
                    if (error is CreditRejectedException) {
                        NotEnoughCoinsDialog.newInstance().show(supportFragmentManager, TAG_NOT_ENOUGH)
                    } else {
                        Toast.makeText(this@ControlCenterDetailActivity, R.string.themes_network_error, Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun listenNotEnough() {
        supportFragmentManager.setFragmentResultListener(NotEnoughCoinsDialog.REQUEST_KEY, this) { _, bundle ->
            when (bundle.getString(NotEnoughCoinsDialog.KEY_ACTION)) {
                NotEnoughCoinsDialog.ACTION_GET_COIN -> showActivity(RewardsActivity::class.java, null)
                NotEnoughCoinsDialog.ACTION_UNLOCK_ALL -> ThemesEntry.openPremium(this, "themes_cc_detail")
            }
        }
    }

    // Theme mặc định = Control Center gốc (nền kính); theme khác lấy chi tiết từ API.
    private fun loadDetail() {
        lifecycleScope.launch {
            binding.pbLoading.visibility = View.VISIBLE
            val d = DefaultThemeProvider.loadDetail(this@ControlCenterDetailActivity, repository, themeId)
            detail = d
            // Theme mặc định = Control Center gốc nên coi như đã mở.
            unlocked = isDefault || d?.controlSkin?.isUnlocked == true
            renderApply()
            Glide.with(this@ControlCenterDetailActivity)
                .load(d?.wallpapers?.firstOrNull() ?: d?.previewUrl)
                .into(binding.imgBackground)
            preview.bind(d?.controlSystems.orEmpty(), d?.controlSkin?.colors.orEmpty()) {
                binding.pbLoading.visibility = View.GONE
            }
            favorite = FavoriteRepository.isControlCenterFavorite(this@ControlCenterDetailActivity, themeId)
            renderFavorite()
            // Theme chỉ có màu (không ảnh ô) vẫn áp được.
            binding.btnApply.isEnabled = d != null && (isDefault || d.controlSystems.isNotEmpty() ||
                d.controlSkin?.colors.orEmpty().isNotEmpty())
        }
    }

    private fun apply() {
        val d = detail ?: return
        if (applyTask.running) return
        loading.show()
        val appCtx = applicationContext
        val id = d.id
        val skins = d.controlSystems
        val colors = d.controlSkin?.colors.orEmpty()
        val default = isDefault
        val name = d.name
        val previewUrl = d.wallpapers.firstOrNull() ?: d.previewUrl
        applyTask.run({
            if (default) {
                ControlCenterSkinApplier.restoreDefault(appCtx)
                true
            } else {
                ControlCenterSkinApplier.apply(appCtx, id, skins, colors)
            }
        }) { live, ok ->
            live?.loading?.hide()
            if (!ok) {
                Toast.makeText(live ?: appCtx, R.string.themes_apply_failed, Toast.LENGTH_SHORT).show()
            } else {
                ApplyTracker.log(name, ApplyTracker.TYPE_CONTROL_CENTER)
                live?.let {
                    ControlCenterSuccessDialog.newInstance(previewUrl, skins, colors)
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
        private const val TAG_NOT_ENOUGH = "not_enough_coins"

        fun start(context: Context, themeId: String) {
            context.startActivity(
                Intent(context, ControlCenterDetailActivity::class.java).putExtra(EXTRA_THEME_ID, themeId)
            )
        }
    }
}

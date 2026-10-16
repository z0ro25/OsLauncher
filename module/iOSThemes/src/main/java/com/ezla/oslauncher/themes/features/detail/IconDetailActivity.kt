package com.ezla.oslauncher.themes.features.detail

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.Models.ThemeDetail
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.apply.ThemeApplyEngine
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.data.CreditRejectedException
import com.ezla.oslauncher.themes.data.CreditRepository
import com.ezla.oslauncher.themes.data.DefaultThemeProvider
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
import com.ezla.oslauncher.themes.databinding.ThemesActivityIconDetailBinding
import com.ezla.oslauncher.themes.features.loadCreditInto
import com.ezla.oslauncher.themes.features.rewards.RewardsActivity
import kotlinx.coroutines.launch

// Màn Icon detail (Figma "Unlock icon"): xem 1 bộ icon trên nền theme. Chưa mở khoá -> "Unlock with N"
// (/unlocks, thiếu xu thì Not Enough Coins); đã mở -> Apply chỉ thay icon, xong hiện sheet Successfully Set.
// Tim / chia sẻ để callback, làm sau.
class IconDetailActivity : BaseActivity<ThemesActivityIconDetailBinding>() {

    override val setViewBinding: ThemesActivityIconDetailBinding
        get() = ThemesActivityIconDetailBinding.inflate(LayoutInflater.from(this))

    private val repository = ThemeRepositoryProvider.repository
    private var detail: ThemeDetail? = null
    private var unlocked = false
    private var busy = false

    var onFavoriteClick: ((ThemeDetail?) -> Unit)? = null
    var onShareClick: ((ThemeDetail?) -> Unit)? = null
    // Màn IAP bổ sung sau: callback cho "Unlock All Icon" ở sheet Not Enough Coins.
    var onPremiumClick: ((ThemeDetail?) -> Unit)? = null

    private val themeId get() = intent.getStringExtra(EXTRA_THEME_ID).orEmpty()

    override fun initView() {
        listenNotEnough()
        renderAction()
        loadDetail()
    }

    override fun viewListener() {
        binding.imgBack.setOnClickListener { finish() }
        binding.layoutCoin.setOnClickListener { showActivity(RewardsActivity::class.java, null) }
        binding.btnAction.setOnClickListener { onAction() }
        binding.btnFavorite.setOnClickListener { onFavoriteClick?.invoke(detail) }
        binding.btnShare.setOnClickListener { onShareClick?.invoke(detail) }
    }

    override fun dataObservable() {}

    override fun onResume() {
        super.onResume()
        loadCreditInto(binding.tvCoin)
    }

    private fun loadDetail() {
        lifecycleScope.launch {
            val d = DefaultThemeProvider.loadDetail(this@IconDetailActivity, repository, themeId)
            detail = d
            unlocked = d?.iconUnlocked ?: false
            Glide.with(this@IconDetailActivity)
                .load(d?.wallpapers?.firstOrNull() ?: d?.previewUrl)
                .into(binding.imgPreviewBg)
            IconPreviewBinder.bind(d?.logos.orEmpty(), binding.pagerIcons, binding.layoutDock)
            renderAction()
        }
    }

    private fun renderAction() {
        if (unlocked) {
            binding.tvAction.setText(R.string.themes_apply)
            binding.imgActionCoin.visibility = View.GONE
        } else {
            binding.tvAction.text = getString(R.string.themes_unlock_with, detail?.iconCoins ?: 0)
            binding.imgActionCoin.visibility = View.VISIBLE
        }
    }

    private fun onAction() {
        val d = detail ?: return
        if (busy) return
        if (unlocked) apply(d) else unlock(d)
    }

    // Mở khoá bộ icon qua /unlocks (server trừ xu, trả số dư mới); thiếu xu -> Not Enough Coins.
    private fun unlock(d: ThemeDetail) {
        val id = d.id.toIntOrNull() ?: return
        busy = true
        lifecycleScope.launch {
            CreditRepository.unlock(CreditRepository.UNLOCK_THEME_ICONS, id)
                .onSuccess { credit ->
                    unlocked = true
                    binding.tvCoin.text = credit.toString()
                    renderAction()
                }
                .onFailure { error ->
                    if (error is CreditRejectedException) {
                        NotEnoughCoinsDialog.newInstance().show(supportFragmentManager, TAG_NOT_ENOUGH)
                    } else {
                        Toast.makeText(this@IconDetailActivity, R.string.themes_network_error, Toast.LENGTH_SHORT).show()
                    }
                }
            busy = false
        }
    }

    // Chỉ thay icon (giữ nền); xong -> sheet Successfully Set (biến thể icon), lỗi -> toast.
    private fun apply(d: ThemeDetail) {
        busy = true
        lifecycleScope.launch {
            val ok = if (DefaultThemeProvider.isDefault(d.id)) {
                // Bộ mặc định = icon gốc của launcher; đang dùng sẵn rồi thì vẫn coi là áp xong.
                ThemeApplyEngine.restoreDefaultIcons(this@IconDetailActivity)
                true
            } else {
                ThemeApplyEngine.applyIcons(this@IconDetailActivity, d)
            }
            busy = false
            if (ok) {
                SuccessfullySetDialog.newInstance(
                    d.wallpapers.firstOrNull() ?: d.previewUrl,
                    forIcon = true,
                    iconUrls = d.logos.map { it.imageUrl }
                )
                    .show(supportFragmentManager, TAG_SUCCESS)
            } else {
                Toast.makeText(this@IconDetailActivity, R.string.themes_apply_failed, Toast.LENGTH_SHORT).show()
            }
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

    companion object {
        private const val EXTRA_THEME_ID = "extra_theme_id"
        private const val TAG_NOT_ENOUGH = "not_enough_coins"
        private const val TAG_SUCCESS = "successfully_set"

        fun start(context: Context, themeId: String) {
            context.startActivity(
                Intent(context, IconDetailActivity::class.java).putExtra(EXTRA_THEME_ID, themeId)
            )
        }
    }
}

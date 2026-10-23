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
import com.ezla.oslauncher.themes.ThemesEntry
import com.ezla.oslauncher.themes.apply.ThemeApplyEngine
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.base.ApplyTask
import com.ezla.oslauncher.themes.base.LoadingOverlay
import com.ezla.oslauncher.themes.data.CreditRejectedException
import com.ezla.oslauncher.themes.data.CreditRepository
import com.ezla.oslauncher.themes.data.DefaultThemeProvider
import com.ezla.oslauncher.themes.data.SubsRepository
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
import com.ezla.oslauncher.themes.data.favorite.FavoriteRepository
import com.ezla.oslauncher.themes.databinding.ThemesActivityIconDetailBinding
import com.ezla.oslauncher.themes.features.ApplyTracker
import com.ezla.oslauncher.themes.features.ThemesInter
import com.ezla.oslauncher.themes.features.loadCreditInto
import com.ezla.oslauncher.themes.features.shareAppOnStore
import com.ezla.oslauncher.themes.features.rewards.RewardsActivity
import kotlinx.coroutines.launch

// Màn Icon detail (Figma "Unlock icon"): xem 1 bộ icon trên nền theme. Chưa mở khoá -> "Unlock with N"
// (/unlocks, thiếu xu thì Not Enough Coins); đã mở -> Apply chỉ thay icon, xong hiện sheet Successfully Set.
// Tim = yêu thích local (Mine > Favorites); chia sẻ = link app trên store.
class IconDetailActivity : BaseActivity<ThemesActivityIconDetailBinding>() {

    override val setViewBinding: ThemesActivityIconDetailBinding
        get() = ThemesActivityIconDetailBinding.inflate(LayoutInflater.from(this))

    private val repository = ThemeRepositoryProvider.repository
    private var detail: ThemeDetail? = null
    private var unlocked = false
    private var busy = false
    private val loading by lazy { LoadingOverlay(this) }
    private var subscribed = false
    private var favorite = false

    var onFavoriteClick: ((ThemeDetail?) -> Unit)? = null
    var onShareClick: ((ThemeDetail?) -> Unit)? = null
    // Màn IAP bổ sung sau: callback cho "Unlock All Icon" ở sheet Not Enough Coins.
    var onPremiumClick: ((ThemeDetail?) -> Unit)? = null

    private val themeId get() = intent.getStringExtra(EXTRA_THEME_ID).orEmpty()

    override fun initView() {
        showBannerOverlay(binding.frBanner, binding.mainLayout, false)
        // Màn tạo lại giữa lúc áp: nhận kết quả + hiện lại loading, khoá nút như lượt cũ.
        applyTask.attach(this)
        if (applyTask.running) {
            busy = true
            loading.show()
        }
        listenNotEnough()
        renderAction()
        loadDetail()
    }

    override fun viewListener() {
        binding.imgBack.setOnClickListener { finish() }
        binding.layoutCoin.setOnClickListener { showActivity(RewardsActivity::class.java, null) }
        binding.btnAction.setOnClickListener { onAction() }
        binding.btnFavorite.setOnClickListener {
            onFavoriteClick?.invoke(detail)
            toggleFavorite()
        }
        binding.btnShare.setOnClickListener {
            onShareClick?.invoke(detail)
            shareAppOnStore()
        }
    }

    override fun dataObservable() {}

    override fun onResume() {
        super.onResume()
        // Có thể vừa mua subs ở màn Subs rồi quay lại -> đọc lại trạng thái mỗi lần.
        subscribed = SubsRepository.isSubscribed(this)
        binding.layoutCoin.visibility = if (subscribed) View.GONE else View.VISIBLE
        if (!subscribed) loadCreditInto(binding.tvCoin)
        renderAction()
    }

    private fun loadDetail() {
        lifecycleScope.launch {
            favorite = FavoriteRepository.isIconPackFavorite(this@IconDetailActivity, themeId)
            renderFavorite()
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

    // Subs: luôn "Apply" (không giá xu); user thường: chưa mở -> "Unlock with N".
    private fun renderAction() {
        if (unlocked || subscribed) {
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
        if (!unlocked && !subscribed) {
            // Nút đang là "Unlock with N": chỉ mở khoá, không inter.
            unlock(d)
            return
        }
        // Nút đang là Apply -> inter (nếu có) rồi áp; subs chưa sở hữu thì /unlocks (miễn xu) trước.
        ThemesInter.run(this) {
            if (isFinishing) return@run
            if (unlocked) apply(d) else unlock(d, applyAfter = true)
        }
    }

    // Mở khoá bộ icon qua /unlocks (server trừ xu, trả số dư mới); thiếu xu -> Not Enough Coins.
    private fun unlock(d: ThemeDetail, applyAfter: Boolean = false) {
        val id = d.id.toIntOrNull() ?: return
        busy = true
        lifecycleScope.launch {
            val result = CreditRepository.unlock(CreditRepository.UNLOCK_THEME_ICONS, id)
            // Hạ cờ TRƯỚC khi áp: apply() tự bật lại busy cho lượt áp của nó.
            busy = false
            result
                .onSuccess { credit ->
                    unlocked = true
                    binding.tvCoin.text = credit.toString()
                    renderAction()
                    if (applyAfter) apply(d)
                }
                .onFailure { error ->
                    if (error is CreditRejectedException) {
                        NotEnoughCoinsDialog.newInstance().show(supportFragmentManager, TAG_NOT_ENOUGH)
                    } else {
                        Toast.makeText(this@IconDetailActivity, R.string.themes_network_error, Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    // Chỉ thay icon (giữ nền); xong -> sheet Successfully Set (biến thể icon), lỗi -> toast.
    // Qua ApplyTask: reload desktop có thể làm màn bị tạo lại, kết quả phải về instance đang sống.
    private fun apply(d: ThemeDetail) {
        if (applyTask.running) return
        busy = true
        loading.show()
        val appCtx = applicationContext
        applyTask.run({
            if (DefaultThemeProvider.isDefault(d.id)) {
                // Bộ mặc định = icon gốc của launcher; đang dùng sẵn rồi thì vẫn coi là áp xong.
                ThemeApplyEngine.restoreDefaultIcons(appCtx)
                true
            } else {
                ThemeApplyEngine.applyIcons(appCtx, d)
            }
        }) { live, ok ->
            // Log trước nhánh live == null: màn bị tạo lại vẫn là 1 lần áp thành công.
            if (ok) ApplyTracker.log(d.name, ApplyTracker.TYPE_ICONS)
            if (live == null) {
                if (!ok) Toast.makeText(appCtx, R.string.themes_apply_failed, Toast.LENGTH_SHORT).show()
                return@run
            }
            live.busy = false
            live.loading.hide()
            if (ok) {
                SuccessfullySetDialog.newInstance(
                    d.wallpapers.firstOrNull() ?: d.previewUrl,
                    forIcon = true,
                    iconUrls = d.logos.map { it.imageUrl }
                )
                    .show(live.supportFragmentManager, TAG_SUCCESS)
            } else {
                Toast.makeText(live, R.string.themes_apply_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        applyTask.detach(this)
        super.onDestroy()
    }

    // Cần detail đã tải (thẻ yêu thích lưu tên + icon + nền của theme); chưa tải xong thì bỏ qua.
    private fun toggleFavorite() {
        val d = detail ?: return
        lifecycleScope.launch {
            favorite = FavoriteRepository.toggleIconPack(this@IconDetailActivity, d)
            renderFavorite()
        }
    }

    private fun renderFavorite() {
        binding.imgFavorite.setImageResource(
            if (favorite) R.drawable.themes_ic_heart_filled else R.drawable.themes_ic_heart
        )
    }

    // Unlock All Icon -> màn Subs (IAP) của :app qua ThemesEntry; callback cũ vẫn được báo.
    private fun openPremium(d: ThemeDetail?) {
        onPremiumClick?.invoke(d)
        ThemesEntry.openPremium(this)
    }

    private fun listenNotEnough() {
        supportFragmentManager.setFragmentResultListener(
            NotEnoughCoinsDialog.REQUEST_KEY, this
        ) { _, bundle ->
            when (bundle.getString(NotEnoughCoinsDialog.KEY_ACTION)) {
                NotEnoughCoinsDialog.ACTION_GET_COIN -> showActivity(RewardsActivity::class.java, null)
                NotEnoughCoinsDialog.ACTION_UNLOCK_ALL -> openPremium(detail)
            }
        }
    }

    companion object {
        // Sống ngoài instance: lượt áp phải vượt qua recreate của màn (xem ApplyTask).
        private val applyTask = ApplyTask<IconDetailActivity>()

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

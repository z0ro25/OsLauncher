package com.ezla.oslauncher.themes.features.detail

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.Models.ThemeDetail
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.ThemesEntry
import com.ezla.oslauncher.themes.apply.ThemeApplyEngine
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.base.ApplyTask
import com.ezla.oslauncher.themes.base.LoadingOverlay
import com.ezla.oslauncher.themes.features.ApplyTracker
import com.ezla.oslauncher.themes.features.ThemesInter
import com.ezla.oslauncher.themes.features.loadCreditInto
import com.ezla.oslauncher.themes.data.CreditRejectedException
import com.ezla.oslauncher.themes.data.CreditRepository
import com.ezla.oslauncher.themes.data.DefaultThemeProvider
import com.ezla.oslauncher.themes.data.SubsRepository
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
import com.ezla.oslauncher.themes.databinding.ThemesActivityThemeDetailBinding
import com.ezla.oslauncher.themes.features.home.ThemesActivity
import com.ezla.oslauncher.themes.features.rewards.RewardsActivity
import kotlinx.coroutines.launch

// Màn Install theme (Figma Install/*): tab Icon = nền + lưới app vuốt + dock, tab Wallpaper = chỉ nền.
// Unlock (/unlocks, trạng thái từ is_unlocked) rồi Apply; mở đủ 2 phần -> header "Apply All".
// Subs: ẩn xu, luôn Apply / Apply All, phần chưa sở hữu tự /unlocks rồi áp. Áp xong: toast + về Main.
class ThemeDetailActivity : BaseActivity<ThemesActivityThemeDetailBinding>() {

    override val setViewBinding: ThemesActivityThemeDetailBinding
        get() = ThemesActivityThemeDetailBinding.inflate(LayoutInflater.from(this))

    private enum class Tab { ICON, WALLPAPER }

    private val repository = ThemeRepositoryProvider.repository
    private var detail: ThemeDetail? = null
    private var tab = Tab.ICON
    private var iconUnlocked = false
    private var wallpaperUnlocked = false
    private val loading by lazy { LoadingOverlay(this) }
    private var unlocking = false
    private var subscribed = false

    // Màn IAP bổ sung sau: callback cho "Unlock All Icon" ở sheet Not Enough Coins.
    var onPremiumClick: ((ThemeDetail?) -> Unit)? = null

    private val themeId get() = intent.getStringExtra(EXTRA_THEME_ID).orEmpty()

    override fun initView() {
        showBannerOverlay(binding.frBanner, binding.mainLayout, false)
        // Màn tạo lại giữa lúc áp (đổi nền -> recreate): nhận kết quả + hiện lại loading.
        applyTask.attach(this)
        if (applyTask.running) loading.show()
        listenNotEnough()
        listenChooseWhere()
        listenApplyTheme()
        renderTab()
        renderActions()
        loadDetail()
    }

    override fun viewListener() {
        binding.imgBack.setOnClickListener { finish() }
        binding.tabIcon.setOnClickListener { selectTab(Tab.ICON) }
        binding.tabWallpaper.setOnClickListener { selectTab(Tab.WALLPAPER) }
        binding.btnTabAction.setOnClickListener { onTabAction() }
        // Apply All -> inter (nếu có) rồi mới mở sheet Apply Theme.
        binding.btnApplyTheme.setOnClickListener {
            ThemesInter.run(this) { if (!isFinishing) showApplyTheme() }
        }
        binding.layoutCoin.setOnClickListener { showActivity(RewardsActivity::class.java, null) }
    }

    override fun dataObservable() {}

    override fun onResume() {
        super.onResume()
        // Có thể vừa mua subs ở màn Subs rồi quay lại -> đọc lại trạng thái mỗi lần.
        subscribed = SubsRepository.isSubscribed(this)
        if (!subscribed) loadCreditInto(binding.tvCoin)
        renderActions()
    }

    private fun loadDetail() {
        lifecycleScope.launch {
            val d = DefaultThemeProvider.loadDetail(this@ThemeDetailActivity, repository, themeId)
            detail = d
            iconUnlocked = d?.iconUnlocked ?: false
            wallpaperUnlocked = d?.wallpaperUnlocked ?: false
            Glide.with(this@ThemeDetailActivity)
                .load(d?.wallpapers?.firstOrNull() ?: d?.previewUrl)
                .into(binding.imgPreviewBg)
            bindIcons(d)
            renderActions()
        }
    }

    // Dock cố định 4 app + lưới vuốt: dùng chung IconPreviewBinder với màn Icon detail.
    private fun bindIcons(d: ThemeDetail?) {
        val logos = d?.logos.orEmpty()
        IconPreviewBinder.bind(logos, binding.pagerIcons, binding.layoutDock)
    }

    private fun selectTab(newTab: Tab) {
        if (tab == newTab) return
        tab = newTab
        renderTab()
        renderActions()
    }

    private fun renderTab() {
        val onIcon = tab == Tab.ICON
        val active = ContextCompat.getColor(this, R.color.themes_text_title)
        val inactive = ContextCompat.getColor(this, R.color.themes_text_hint)
        binding.tvTabIcon.setTextColor(if (onIcon) active else inactive)
        binding.tvTabWallpaper.setTextColor(if (onIcon) inactive else active)
        binding.lineIcon.visibility = if (onIcon) View.VISIBLE else View.INVISIBLE
        binding.lineWallpaper.visibility = if (onIcon) View.INVISIBLE else View.VISIBLE
        binding.layoutIconPreview.visibility = if (onIcon) View.VISIBLE else View.GONE
    }

    // Nút dưới theo tab; header: badge xu khi chưa mở đủ, "Apply All" khi đã mở cả 2.
    // Subs: không hiện giá xu, nút luôn là Apply; header luôn "Apply All", ẩn badge xu.
    private fun renderActions() {
        if (isUnlocked(tab) || subscribed) {
            binding.tvTabAction.setText(
                if (tab == Tab.ICON) R.string.themes_apply_icons else R.string.themes_apply_wallpaper
            )
            binding.imgTabActionCoin.visibility = View.GONE
        } else {
            binding.tvTabAction.text = getString(R.string.themes_unlock_with, priceOf(tab))
            binding.imgTabActionCoin.visibility = View.VISIBLE
        }
        val all = (iconUnlocked && wallpaperUnlocked) || subscribed
        binding.btnApplyTheme.visibility = if (all) View.VISIBLE else View.GONE
        binding.layoutCoin.visibility = if (all) View.GONE else View.VISIBLE
    }

    private fun onTabAction() {
        val d = detail ?: return
        if (!isUnlocked(tab) && !subscribed) {
            // Nút đang là "Unlock with N": chỉ mở khoá, không inter.
            unlock(tab)
            return
        }
        // Nút đang là Apply -> inter (nếu có) rồi áp; subs còn phần chưa sở hữu thì /unlocks trước.
        ThemesInter.run(this) {
            if (isFinishing) return@run
            if (isUnlocked(tab)) applyTab(d) else unlock(tab) { applyTab(d) }
        }
    }

    private fun applyTab(d: ThemeDetail) {
        when (tab) {
            Tab.ICON -> runApply(ApplyTracker.TYPE_ICONS) { applyIcons(d) }
            Tab.WALLPAPER -> chooseWhere(d)
        }
    }

    // Mở khoá qua /unlocks (server trừ xu, trả số dư mới). Server từ chối (thiếu xu) -> Not Enough
    // Coins; lỗi mạng -> toast. Chặn bấm lặp để không gửi 2 lần.
    private fun unlock(target: Tab, onUnlocked: (() -> Unit)? = null) {
        val d = detail ?: return
        if (unlocking) return
        val (type, id) = when (target) {
            Tab.ICON -> CreditRepository.UNLOCK_THEME_ICONS to (d.id.toIntOrNull() ?: 0)
            Tab.WALLPAPER -> CreditRepository.UNLOCK_BACKGROUND to d.backgroundId
        }
        if (id <= 0) return
        unlocking = true
        lifecycleScope.launch {
            val result = CreditRepository.unlock(type, id)
            // Hạ cờ TRƯỚC onUnlocked: bước kế (mở tiếp phần còn lại / áp) có thể gọi lại unlock().
            unlocking = false
            result
                .onSuccess { credit ->
                    when (target) {
                        Tab.ICON -> iconUnlocked = true
                        Tab.WALLPAPER -> wallpaperUnlocked = true
                    }
                    binding.tvCoin.text = credit.toString()
                    renderActions()
                    onUnlocked?.invoke()
                }
                .onFailure { error ->
                    if (error is CreditRejectedException) {
                        NotEnoughCoinsDialog.newInstance().show(supportFragmentManager, TAG_NOT_ENOUGH)
                    } else {
                        Toast.makeText(this@ThemeDetailActivity, R.string.themes_network_error, Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun isUnlocked(target: Tab) = when (target) {
        Tab.ICON -> iconUnlocked
        Tab.WALLPAPER -> wallpaperUnlocked
    }

    // Giá mở khoá từ API: icon = icons_coins của theme, nền = coins của background đầu.
    private fun priceOf(target: Tab) = when (target) {
        Tab.ICON -> detail?.iconCoins ?: 0
        Tab.WALLPAPER -> detail?.wallpaperCoins ?: 0
    }

    // Apply Wallpaper: vẫn hỏi Lock/Home trước khi áp nền.
    private fun chooseWhere(d: ThemeDetail) {
        val bg = d.wallpapers.firstOrNull() ?: d.previewUrl
        ChooseWhereToApplyDialog.newInstance(lockPreview = bg, homePreview = d.previewUrl)
            .show(supportFragmentManager, TAG_CHOOSE)
    }

    private fun listenChooseWhere() {
        supportFragmentManager.setFragmentResultListener(
            ChooseWhereToApplyDialog.REQUEST_KEY, this
        ) { _, bundle ->
            val d = detail ?: return@setFragmentResultListener
            val url = d.wallpapers.firstOrNull() ?: return@setFragmentResultListener
            val lock = bundle.getBoolean(ChooseWhereToApplyDialog.KEY_LOCK)
            val home = bundle.getBoolean(ChooseWhereToApplyDialog.KEY_HOME)
            runApply(ApplyTracker.TYPE_BACKGROUND) { applyWallpaper(d, url, home, lock) }
        }
    }

    // Apply All: sheet Apply Theme (preview Lock + Home) -> Apply Now áp cả theme lên 2 nơi.
    private fun showApplyTheme() {
        val d = detail ?: return
        ApplyThemeDialog.newInstance(
            lockPreview = d.wallpapers.firstOrNull() ?: d.previewUrl,
            homePreview = d.previewUrl
        ).show(supportFragmentManager, TAG_APPLY_THEME)
    }

    private fun listenApplyTheme() {
        supportFragmentManager.setFragmentResultListener(ApplyThemeDialog.REQUEST_KEY, this) { _, _ ->
            val d = detail ?: return@setFragmentResultListener
            // Subs bấm Apply All khi còn phần chưa sở hữu: mở khoá lần lượt icon rồi nền, đủ mới áp.
            when {
                !subscribed || isDefault(d) -> applyAll(d)
                !iconUnlocked -> unlock(Tab.ICON) {
                    if (wallpaperUnlocked) applyAll(d) else unlock(Tab.WALLPAPER) { applyAll(d) }
                }
                !wallpaperUnlocked -> unlock(Tab.WALLPAPER) { applyAll(d) }
                else -> applyAll(d)
            }
        }
    }

    private fun applyAll(d: ThemeDetail) {
        runApply(ApplyTracker.TYPE_BOTH) {
            if (isDefault(d)) applyIcons(d) && applyWallpaper(d, "", home = true, lock = true)
            else ThemeApplyEngine.applyTheme(this, d, applyHome = true, applyLock = true)
        }
    }

    // Theme mặc định: Apply = khôi phục icon gốc / nền mặc định của launcher thay vì tải từ URL.
    private fun isDefault(d: ThemeDetail) = DefaultThemeProvider.isDefault(d.id)

    private suspend fun applyIcons(d: ThemeDetail): Boolean {
        if (!isDefault(d)) return ThemeApplyEngine.applyIcons(this, d)
        ThemeApplyEngine.restoreDefaultIcons(this)
        return true
    }

    private suspend fun applyWallpaper(d: ThemeDetail, url: String, home: Boolean, lock: Boolean) =
        if (isDefault(d)) ThemeApplyEngine.restoreDefaultWallpaper(this, DefaultThemeProvider.WALLPAPER_RES, home, lock)
        else ThemeApplyEngine.applyWallpaper(this, url, home, lock)

    // Chạy 1 lần áp (chặn bấm lặp); thành công -> toast + về Main, lỗi -> toast lỗi, ở lại màn.
    // Qua ApplyTask: đổi nền làm màn bị tạo lại, kết quả phải về instance đang sống.
    private fun runApply(type: String, block: suspend () -> Boolean) {
        if (applyTask.running) return
        loading.show()
        val appCtx = applicationContext
        val themeName = detail?.name.orEmpty()
        applyTask.run(block) { live, ok ->
            live?.loading?.hide()
            val msg = if (ok) R.string.themes_apply_success else R.string.themes_apply_failed
            Toast.makeText(live ?: appCtx, msg, Toast.LENGTH_SHORT).show()
            if (ok) ApplyTracker.log(themeName, type)
            if (ok) live?.backToMain()
        }
    }

    override fun onDestroy() {
        applyTask.detach(this)
        super.onDestroy()
    }

    // Về Main: CLEAR_TOP gỡ View theme + Install khỏi stack, SINGLE_TOP giữ instance Main đang có.
    private fun backToMain() {
        startActivity(
            Intent(this, ThemesActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    // Unlock All Icon -> màn Subs (IAP) của :app qua ThemesEntry; callback cũ vẫn được báo.
    private fun openPremium(d: ThemeDetail?) {
        onPremiumClick?.invoke(d)
        ThemesEntry.openPremium(this, "themes_detail")
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
        private val applyTask = ApplyTask<ThemeDetailActivity>()

        private const val EXTRA_THEME_ID = "extra_theme_id"
        private const val TAG_NOT_ENOUGH = "not_enough_coins"
        private const val TAG_CHOOSE = "choose_where_apply"
        private const val TAG_APPLY_THEME = "apply_theme"

        fun start(context: Context, themeId: String) {
            context.startActivity(
                Intent(context, ThemeDetailActivity::class.java).putExtra(EXTRA_THEME_ID, themeId)
            )
        }
    }
}

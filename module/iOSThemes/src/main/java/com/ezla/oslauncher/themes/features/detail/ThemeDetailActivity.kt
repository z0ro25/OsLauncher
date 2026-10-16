package com.ezla.oslauncher.themes.features.detail

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.Api.ApiLog
import com.ezla.oslauncher.themes.Models.ThemeDetail
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.apply.ThemeApplyEngine
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.features.loadCreditInto
import com.ezla.oslauncher.themes.data.CreditRejectedException
import com.ezla.oslauncher.themes.data.CreditRepository
import com.ezla.oslauncher.themes.data.DefaultThemeProvider
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
import com.ezla.oslauncher.themes.databinding.ThemesActivityThemeDetailBinding
import com.ezla.oslauncher.themes.features.home.ThemesActivity
import com.ezla.oslauncher.themes.features.rewards.RewardsActivity
import kotlinx.coroutines.launch

// Màn Install theme (Figma Install/*): tab Icon = nền + lưới app vuốt + dock, tab Wallpaper = chỉ nền.
// Nút dưới: Unlock (API /unlocks, server trừ xu) rồi Apply Icons / Apply Wallpaper; mở đủ 2 phần thì
// header đổi badge xu thành "Apply All" (sheet Apply Theme). Áp xong: toast + về Main.
// Trạng thái mở khoá lấy từ is_unlocked của server.
class ThemeDetailActivity : BaseActivity<ThemesActivityThemeDetailBinding>() {

    override val setViewBinding: ThemesActivityThemeDetailBinding
        get() = ThemesActivityThemeDetailBinding.inflate(LayoutInflater.from(this))

    private enum class Tab { ICON, WALLPAPER }

    private val repository = ThemeRepositoryProvider.repository
    private var detail: ThemeDetail? = null
    private var tab = Tab.ICON
    private var iconUnlocked = false
    private var wallpaperUnlocked = false
    private var applying = false
    private var unlocking = false

    // Màn IAP bổ sung sau: callback cho "Unlock All Icon" ở sheet Not Enough Coins.
    var onPremiumClick: ((ThemeDetail?) -> Unit)? = null

    private val themeId get() = intent.getStringExtra(EXTRA_THEME_ID).orEmpty()

    override fun initView() {
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
        binding.btnApplyTheme.setOnClickListener { showApplyTheme() }
        binding.layoutCoin.setOnClickListener { showActivity(RewardsActivity::class.java, null) }
    }

    override fun dataObservable() {}

    override fun onResume() {
        super.onResume()
        loadCreditInto(binding.tvCoin)
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
        ApiLog.d("install theme=${d?.id} ${d?.name} logos=${logos.map { it.packageName }}")
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
    private fun renderActions() {
        if (isUnlocked(tab)) {
            binding.tvTabAction.setText(
                if (tab == Tab.ICON) R.string.themes_apply_icons else R.string.themes_apply_wallpaper
            )
            binding.imgTabActionCoin.visibility = View.GONE
        } else {
            binding.tvTabAction.text = getString(R.string.themes_unlock_with, priceOf(tab))
            binding.imgTabActionCoin.visibility = View.VISIBLE
        }
        val all = iconUnlocked && wallpaperUnlocked
        binding.btnApplyTheme.visibility = if (all) View.VISIBLE else View.GONE
        binding.layoutCoin.visibility = if (all) View.GONE else View.VISIBLE
    }

    private fun onTabAction() {
        val d = detail ?: return
        if (!isUnlocked(tab)) {
            unlock(tab)
            return
        }
        when (tab) {
            Tab.ICON -> runApply { applyIcons(d) }
            Tab.WALLPAPER -> chooseWhere(d)
        }
    }

    // Mở khoá qua /unlocks (server trừ xu, trả số dư mới). Server từ chối (thiếu xu) -> Not Enough
    // Coins; lỗi mạng -> toast. Chặn bấm lặp để không gửi 2 lần.
    private fun unlock(target: Tab) {
        val d = detail ?: return
        if (unlocking) return
        val (type, id) = when (target) {
            Tab.ICON -> CreditRepository.UNLOCK_THEME_ICONS to (d.id.toIntOrNull() ?: 0)
            Tab.WALLPAPER -> CreditRepository.UNLOCK_BACKGROUND to d.backgroundId
        }
        if (id <= 0) {
            ApiLog.e("unlock $type: thiếu id (theme=${d.id})", null)
            return
        }
        unlocking = true
        lifecycleScope.launch {
            CreditRepository.unlock(type, id)
                .onSuccess { credit ->
                    when (target) {
                        Tab.ICON -> iconUnlocked = true
                        Tab.WALLPAPER -> wallpaperUnlocked = true
                    }
                    binding.tvCoin.text = credit.toString()
                    renderActions()
                }
                .onFailure { error ->
                    if (error is CreditRejectedException) {
                        NotEnoughCoinsDialog.newInstance().show(supportFragmentManager, TAG_NOT_ENOUGH)
                    } else {
                        Toast.makeText(this@ThemeDetailActivity, R.string.themes_network_error, Toast.LENGTH_SHORT).show()
                    }
                }
            unlocking = false
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
            runApply { applyWallpaper(d, url, home, lock) }
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
            runApply {
                if (isDefault(d)) applyIcons(d) && applyWallpaper(d, "", home = true, lock = true)
                else ThemeApplyEngine.applyTheme(this, d, applyHome = true, applyLock = true)
            }
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
    private fun runApply(block: suspend () -> Boolean) {
        if (applying) return
        applying = true
        lifecycleScope.launch {
            val ok = block()
            applying = false
            if (ok) {
                Toast.makeText(this@ThemeDetailActivity, R.string.themes_apply_success, Toast.LENGTH_SHORT).show()
                backToMain()
            } else {
                Toast.makeText(this@ThemeDetailActivity, R.string.themes_apply_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Về Main: CLEAR_TOP gỡ View theme + Install khỏi stack, SINGLE_TOP giữ instance Main đang có.
    private fun backToMain() {
        startActivity(
            Intent(this, ThemesActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
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
        private const val TAG_CHOOSE = "choose_where_apply"
        private const val TAG_APPLY_THEME = "apply_theme"

        fun start(context: Context, themeId: String) {
            context.startActivity(
                Intent(context, ThemeDetailActivity::class.java).putExtra(EXTRA_THEME_ID, themeId)
            )
        }
    }
}

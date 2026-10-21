package com.ezla.oslauncher.themes.features.home

import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.features.ThemesInter
import com.ezla.oslauncher.themes.features.loadCreditInto
import com.ezla.oslauncher.themes.databinding.ThemesActivityMainBinding
import com.ezla.oslauncher.themes.databinding.ThemesItemNavBinding
import com.ezla.oslauncher.themes.features.icons.IconsFragment
import com.ezla.oslauncher.themes.features.mine.MineFragment
import com.ezla.oslauncher.themes.data.DailyCheckinStore
import com.ezla.oslauncher.themes.data.SubsRepository
import com.ezla.oslauncher.themes.features.rewards.CheckinDialog
import com.ezla.oslauncher.themes.features.rewards.RewardDialog
import com.ezla.oslauncher.themes.features.rewards.RewardsActivity
import com.ezla.oslauncher.themes.features.settings.SettingsActivity

// Home của trang Themes. Router/IOSShortcut mở màn này bằng tên class dạng chuỗi: đổi
// package/tên class thì phải sửa cả Router.ACTIVITY_THEMECLUB và IOSShortcut.CLASS_NAME_THEMECLUB.
class ThemesActivity : BaseActivity<ThemesActivityMainBinding>() {
    override val setViewBinding: ThemesActivityMainBinding
        get() = ThemesActivityMainBinding.inflate(LayoutInflater.from(this))

    private class NavTab(
        val tag: String,
        val view: ThemesItemNavBinding,
        @StringRes val label: Int,
        @DrawableRes val icon: Int,
        @DrawableRes val iconSelected: Int,
        val create: () -> Fragment
    )

    private val tabs by lazy {
        listOf(
            NavTab("themes", binding.navThemes, R.string.themes_nav_themes,
                R.drawable.themes_ic_nav_theme, R.drawable.themes_ic_nav_theme_selected) { ThemesFragment() },
            NavTab("icons", binding.navIcons, R.string.themes_nav_icons,
                R.drawable.themes_ic_nav_icon, R.drawable.themes_ic_nav_icon_selected) { IconsFragment() },
            NavTab("wallpapers", binding.navWallpapers, R.string.themes_nav_wallpapers,
                R.drawable.themes_ic_nav_wallpaper, R.drawable.themes_ic_nav_wallpaper_selected) { WallpapersFragment() },
            NavTab("mine", binding.navMine, R.string.themes_nav_mine,
                R.drawable.themes_ic_nav_mine, R.drawable.themes_ic_nav_mine_selected) { MineFragment() }
        )
    }

    override fun initView() {
        showBannerIfEnabled(binding.frBanner, false)
        tabs.forEach { it.view.tvNav.setText(it.label) }
        // Sau recreate (đổi ngôn ngữ...) FragmentManager tự khôi phục; chỉ chọn tab đầu khi mở mới.
        val current = tabs.firstOrNull { supportFragmentManager.findFragmentByTag(it.tag)?.isHidden == false }
        selectTab(current ?: tabs.first())
        listenCheckin()
        maybeShowCheckin()
        // Tải sẵn inter Themes để lần bấm thẻ đầu tiên đã có ad.
        ThemesInter.preload()
    }

    // Lần mở Themes đầu tiên mỗi ngày (chưa nhận) -> dialog check-in. Ghi cờ sau khi đã show.
    private fun maybeShowCheckin() {
        // Đã subs thì không dùng xu nữa -> không mời check-in nhận xu.
        if (SubsRepository.isSubscribed(this)) return
        if (!DailyCheckinStore.shouldShowDialog(this)) return
        if (supportFragmentManager.findFragmentByTag(CheckinDialog.TAG) != null) return
        CheckinDialog().show(supportFragmentManager, CheckinDialog.TAG)
        DailyCheckinStore.markDialogShown(this)
    }

    // Nhận xu ở dialog -> cập nhật số dư trên header + dialog "Check-in Successful +N".
    private fun listenCheckin() {
        supportFragmentManager.setFragmentResultListener(CheckinDialog.REQUEST_KEY, this) { _, bundle ->
            binding.tvCoin.text = bundle.getInt(CheckinDialog.KEY_BALANCE).toString()
            RewardDialog(this, RewardDialog.Type.CHECKIN, bundle.getInt(CheckinDialog.KEY_COINS)).show()
        }
    }

    override fun viewListener() {
        tabs.forEach { tab -> tab.view.root.setOnClickListener { selectTab(tab) } }
        binding.imgMenu.setOnClickListener { showActivity(SettingsActivity::class.java, null) }
        binding.layoutCoin.setOnClickListener { showActivity(RewardsActivity::class.java, null) }
    }

    override fun dataObservable() {}

    // Lấy lại số dư từ server mỗi lần quay về (màn con có thể đã cộng/trừ xu).
    // Đã subs: ẩn hẳn badge xu (mất luôn lối vào Rewards), không gọi /credits.
    override fun onResume() {
        super.onResume()
        val subscribed = SubsRepository.isSubscribed(this)
        binding.layoutCoin.visibility = if (subscribed) View.GONE else View.VISIBLE
        if (!subscribed) loadCreditInto(binding.tvCoin)
    }

    private fun selectTab(selected: NavTab) {
        tabs.forEach { tab ->
            val active = tab === selected
            tab.view.imgNav.setImageResource(if (active) tab.iconSelected else tab.icon)
            tab.view.tvNav.setTextColor(
                ContextCompat.getColor(this, if (active) R.color.themes_primary else R.color.themes_text_hint)
            )
        }

        val fm = supportFragmentManager
        val transaction = fm.beginTransaction()
        tabs.forEach { tab ->
            fm.findFragmentByTag(tab.tag)?.let { if (tab !== selected) transaction.hide(it) }
        }
        val target = fm.findFragmentByTag(selected.tag)
        if (target == null) transaction.add(R.id.frContainer, selected.create(), selected.tag)
        else transaction.show(target)
        transaction.commit()
    }
}

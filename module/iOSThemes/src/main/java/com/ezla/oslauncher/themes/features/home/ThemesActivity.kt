package com.ezla.oslauncher.themes.features.home

import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.databinding.ThemesActivityMainBinding
import com.ezla.oslauncher.themes.databinding.ThemesItemNavBinding
import com.ezla.oslauncher.themes.features.mine.MineFragment
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
                R.drawable.themes_ic_nav_icon, R.drawable.themes_ic_nav_icon_selected) { PlaceholderFragment() },
            NavTab("wallpapers", binding.navWallpapers, R.string.themes_nav_wallpapers,
                R.drawable.themes_ic_nav_wallpaper, R.drawable.themes_ic_nav_wallpaper_selected) { PlaceholderFragment() },
            NavTab("mine", binding.navMine, R.string.themes_nav_mine,
                R.drawable.themes_ic_nav_mine, R.drawable.themes_ic_nav_mine_selected) { MineFragment() }
        )
    }

    override fun initView() {
        // Số xu tạm hardcode theo Figma tới khi có hệ thống xu.
        binding.tvCoin.text = "150"
        tabs.forEach { it.view.tvNav.setText(it.label) }
        // Sau recreate (đổi ngôn ngữ...) FragmentManager tự khôi phục; chỉ chọn tab đầu khi mở mới.
        val current = tabs.firstOrNull { supportFragmentManager.findFragmentByTag(it.tag)?.isHidden == false }
        selectTab(current ?: tabs.first())
    }

    override fun viewListener() {
        tabs.forEach { tab -> tab.view.root.setOnClickListener { selectTab(tab) } }
        binding.imgMenu.setOnClickListener { showActivity(SettingsActivity::class.java, null) }
        binding.layoutCoin.setOnClickListener { showActivity(RewardsActivity::class.java, null) }
    }

    override fun dataObservable() {}

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

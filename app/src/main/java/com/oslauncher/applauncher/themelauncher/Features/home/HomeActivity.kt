package com.oslauncher.applauncher.themelauncher.Features.home

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.provider.Settings
import androidx.activity.addCallback
import androidx.core.view.isVisible
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.general.GeneralActivity
import com.oslauncher.applauncher.themelauncher.Features.hello.HelloActivity
import com.oslauncher.applauncher.themelauncher.Features.setting.SettingActivity
import com.oslauncher.applauncher.themelauncher.Features.wallpaper.selectwallpaper.SelectWallpaperActivity
import com.oslauncher.applauncher.themelauncher.databinding.ActivityHomeBinding
import com.oslauncher.applauncher.themelauncher.extensions.launchActivity
import com.oslauncher.applauncher.themelauncher.extensions.tap
import com.oslauncher.applauncher.themelauncher.tool.sharePreferenceTool.SharePrefUtils
import com.oslauncher.applauncher.themelauncher.utils.PermissionManager


class HomeActivity : BaseActivity<ActivityHomeBinding>() {
    override val setViewBinding: ActivityHomeBinding
        get() = ActivityHomeBinding.inflate(layoutInflater)

    override fun initView() {
        SharePrefUtils.increaseCountOpenApp(this)
        val openAppCount = SharePrefUtils.getCountOpenApp(this) == 1
        binding.setDefaultLauncherWarnning.isVisible = openAppCount

        onBackPressedDispatcher.addCallback {
            finishAffinity()
        }

        PermissionManager.initLauncher(this)

        binding.frNativeHome.isVisible = false
        binding.frBannerHome.isVisible = false
    }

    override fun viewListener() {
        binding.btnExplore.tap {
            launchActivity<HelloActivity>()
        }
        binding.btnSetDefault.tap {
            if (getDefaultLauncherPackage().equals("android")) {
                selectDefault()
            } else {
                val intent = Intent(Settings.ACTION_HOME_SETTINGS);
                startActivity(intent)
                SharePrefUtils.putBoolean(this, "is_login", false)
            }
        }

        binding.llSelectDefault.tap {
            if (getDefaultLauncherPackage().equals("android")) {
                selectDefault()
            } else {
                val intent = Intent(Settings.ACTION_HOME_SETTINGS);
                startActivity(intent)
                SharePrefUtils.putBoolean(this, "is_login", false)
            }
        }

        binding.llGeneral.tap {
            launchActivity<GeneralActivity>()
        }

        binding.llWallpaper.tap {
            launchActivity<SelectWallpaperActivity>()
        }

        binding.ivMenu.tap {
            launchActivity<SettingActivity>()
        }
    }

    override fun dataObservable() {}

    fun selectDefault() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            startActivity(intent)
        } else {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            startActivity(intent)
        }
        SharePrefUtils.putBoolean(this, "is_login", false)
    }

    private fun getDefaultLauncherPackage(): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val packageManager = packageManager
        val resolveInfo: ResolveInfo? =
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)

        return resolveInfo?.activityInfo?.packageName
    }
}

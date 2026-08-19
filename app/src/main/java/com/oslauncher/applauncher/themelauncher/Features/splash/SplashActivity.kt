package com.oslauncher.applauncher.themelauncher.Features.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.home.HomeActivity
import com.oslauncher.applauncher.themelauncher.Features.languageStart.LanguageStartActivity
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.ActivitySplashBinding
import com.oslauncher.applauncher.themelauncher.extensions.hideNavigation
import com.oslauncher.applauncher.themelauncher.tool.languageTool.LanguageUtil
import com.oslauncher.applauncher.themelauncher.tool.sharePreferenceTool.SharePrefUtils
import com.oslauncher.applauncher.themelauncher.utils.EventTrackingHelper
import com.oslauncher.applauncher.themelauncher.utils.YourWallpaperDataManager

class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    override val setViewBinding: ActivitySplashBinding
        get() = ActivitySplashBinding.inflate(layoutInflater)

    override fun initView() {
        if (!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intent.action != null && intent.action.equals(
                Intent.ACTION_MAIN
            )
        ) {
            finish()
            return
        }
        window?.hideNavigation()

        EventTrackingHelper.logEvent(this, "splash_open")

        onBackPressedDispatcher.addCallback {

        }

        YourWallpaperDataManager.addYourWallpaper(this, YourWallpaperDataManager.DefaultData)

        // Không còn quảng cáo: hiển thị splash ngắn rồi điều hướng.
        // Lần đầu cài -> onboarding (chọn ngôn ngữ...); đã hoàn tất onboarding -> vào thẳng Home.
        Handler(Looper.getMainLooper()).postDelayed({ goNext() }, 1000)
    }

    override fun viewListener() {

    }

    override fun dataObservable() {

    }

    private fun goNext() {
        if (isFinishing) return
        // "PERMISSION_SHOWED" = true nghĩa là người dùng đã đi hết luồng onboarding lần đầu
        // (ngôn ngữ -> intro -> quyền, đặt ở bước cuối PermissionActivity). Từ lần sau:
        // splash -> Home luôn, KHÔNG hiện lại màn chọn ngôn ngữ.
        if (SharePrefUtils.getBoolean(this, "PERMISSION_SHOWED", false)) {
            showActivity(HomeActivity::class.java, null)
        } else {
            val bundle = Bundle()
            bundle.putString("screen", "SplashActivity")
            showActivity(LanguageStartActivity::class.java, bundle)
        }
        finish()
    }
}

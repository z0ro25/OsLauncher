package com.ezla.oslauncher.beautylauncher.Features.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.addCallback
import com.ezla.oslauncher.beautylauncher.Base.BaseActivity
import com.ezla.oslauncher.beautylauncher.Features.home.HomeActivity
import com.ezla.oslauncher.beautylauncher.Features.languageStart.LanguageStartActivity
import com.ezla.oslauncher.beautylauncher.extensions.hideNavigation
import com.ezla.oslauncher.beautylauncher.tool.sharePreferenceTool.SharePrefUtils
import com.ezla.oslauncher.beautylauncher.utils.EventTrackingHelper
import com.ezla.oslauncher.beautylauncher.databinding.ActivitySplashBinding

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

        // Không còn quảng cáo: hiển thị splash ngắn rồi điều hướng.
        // Lần đầu cài -> onboarding (chọn ngôn ngữ...); đã hoàn tất onboarding -> vào thẳng Home.
        Handler(Looper.getMainLooper()).postDelayed({ goNext() }, 3000)
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

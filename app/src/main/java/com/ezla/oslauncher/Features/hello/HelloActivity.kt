package com.ezla.oslauncher.Features.hello

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import androidx.core.view.isVisible
import com.truongnt.ios.launcher.searchlauncher.SearchLauncher
import com.ezla.oslauncher.Base.BaseActivity
import com.ezla.oslauncher.Features.wallpaperonboarding.SelectBackgroundActivity
import com.ezla.oslauncher.R
import com.ezla.oslauncher.databinding.ActivityHelloBinding
import com.ezla.oslauncher.extensions.launchActivity
import com.ezla.oslauncher.theme.AppThemeManager
import com.ezla.oslauncher.tool.sharePreferenceTool.SharePrefUtils


class HelloActivity : BaseActivity<ActivityHelloBinding>() {
    override val setViewBinding: ActivityHelloBinding
        get() = ActivityHelloBinding.inflate(layoutInflater)

    override fun initView() {
        binding.frAdsHello.isVisible = false
        val dark = AppThemeManager.isDark(this)
        // Nền màn Hello: ưu tiên hình nền user đã chọn ở màn Chọn hình nền (onboarding); nếu không có
        // (chưa chọn / lỗi đọc) thì fallback nền theo theme (bg_hello / bg_hello_dark).
        if (!applyOnboardingBackground()) {
            binding.root.setBackgroundResource(if (dark) R.drawable.bg_hello_dark else R.drawable.bg_hello)
        }
        binding.lottieAnimationView.repeatCount = com.airbnb.lottie.LottieDrawable.INFINITE
        binding.lottieAnimationView.setAnimation(
            if (dark) R.raw.hello_dark else R.raw.hello_light
        )
        binding.lottieAnimationView.playAnimation()
        Handler().postDelayed({
            binding.tvTaptostart.isVisible = true
            binding.root.setOnClickListener {
                launchActivity<SearchLauncher>()
                finishAffinity()
            }

        }, 3000)

    }

    override fun viewListener() {

    }

    override fun dataObservable() {

    }

    /**
     * Đặt nền màn Hello = hình nền user đã chọn ở onboarding (asset đã lưu qua SharePref).
     * @return true nếu đã đặt được; false nếu không có/đọc lỗi (để caller fallback nền theme).
     */
    private fun applyOnboardingBackground(): Boolean {
        val assetPath = SharePrefUtils.getString(
            this, SelectBackgroundActivity.HELLO_BG_ASSET_KEY, ""
        )
        if (assetPath.isNullOrEmpty()) return false
        return try {
            val bm = assets.open(assetPath).use { BitmapFactory.decodeStream(it) } ?: return false
            binding.root.background = BitmapDrawable(resources, bm)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

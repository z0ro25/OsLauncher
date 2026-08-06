package com.oslauncher.applauncher.themelauncher.Features.hello

import android.os.Handler
import androidx.core.view.isVisible
import com.amz.ios.launcher.searchlauncher.SearchLauncher
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.ActivityHelloBinding
import com.oslauncher.applauncher.themelauncher.extensions.launchActivity
import com.oslauncher.applauncher.themelauncher.theme.AppThemeManager


class HelloActivity : BaseActivity<ActivityHelloBinding>() {
    override val setViewBinding: ActivityHelloBinding
        get() = ActivityHelloBinding.inflate(layoutInflater)

    override fun initView() {
        binding.frAdsHello.isVisible = false
        val dark = AppThemeManager.isDark(this)
        // Nền + Lottie theo theme: dark -> bg_hello_dark/hello_dark, light -> bg_hello/hello_light.
        binding.root.setBackgroundResource(if (dark) R.drawable.bg_hello_dark else R.drawable.bg_hello)
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
}

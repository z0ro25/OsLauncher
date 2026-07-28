package com.oslauncher.applauncher.themelauncher.Features.hello

import android.os.Handler
import androidx.core.view.isVisible
import com.amz.ios.launcher.searchlauncher.SearchLauncher
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.databinding.ActivityHelloBinding
import com.oslauncher.applauncher.themelauncher.extensions.launchActivity


class HelloActivity : BaseActivity<ActivityHelloBinding>() {
    override val setViewBinding: ActivityHelloBinding
        get() = ActivityHelloBinding.inflate(layoutInflater)

    override fun initView() {
        binding.frAdsHello.isVisible = false
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

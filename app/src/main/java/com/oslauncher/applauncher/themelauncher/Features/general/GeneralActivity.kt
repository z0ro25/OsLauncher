package com.oslauncher.applauncher.themelauncher.Features.general

import androidx.activity.addCallback
import androidx.core.view.isVisible
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.general.hiddenapp.HiddenAppActivity
import com.oslauncher.applauncher.themelauncher.Features.general.transitionpage.PageTransitionActivity
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.ActivityGeneralBinding
import com.oslauncher.applauncher.themelauncher.extensions.hideNavigation
import com.oslauncher.applauncher.themelauncher.extensions.launchActivity
import com.oslauncher.applauncher.themelauncher.extensions.showNav
import com.oslauncher.applauncher.themelauncher.tool.sharePreferenceTool.SharePrefUtils
import com.oslauncher.applauncher.themelauncher.utils.Constant
import com.amz.ios.launcher.PagedView
import com.amz.ios.utils.LauncherInteractor

class GeneralActivity : BaseActivity<ActivityGeneralBinding>() {
    override val setViewBinding: ActivityGeneralBinding
        get() = ActivityGeneralBinding.inflate(layoutInflater)

    override fun initView() {
        onBackPressedDispatcher.addCallback {
            finish()
        }

        binding.swHideNav.isChecked = SharePrefUtils.getBoolean(this, Constant.IS_HIDE_NAV, true)

        loadnative()
    }

    override fun viewListener() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.swHideNav.setOnCheckedChangeListener { buttonView, isChecked ->
            SharePrefUtils.putBoolean(
                this,
                Constant.IS_HIDE_NAV,
                isChecked
            )

            if (isChecked) {
                window?.hideNavigation()
            } else window?.showNav()
        }

        binding.llHiddenApp.setOnClickListener {
            launchActivity<HiddenAppActivity>()
        }

        binding.llPageTransition.setOnClickListener {
            launchActivity<PageTransitionActivity>()
        }
    }

    override fun onResume() {
        super.onResume()

        binding.tvPageTransition.text = when (LauncherInteractor.mPageAnimationType) {
            PagedView.PageAnimationType.NONE -> getString(R.string.default_)
            PagedView.PageAnimationType.ROTATE -> getString(R.string.rotation)
            PagedView.PageAnimationType.FLIP -> getString(R.string.flip)
            PagedView.PageAnimationType.ZOOM_IN -> getString(R.string.zoom_in)
        }
    }

    override fun dataObservable() {

    }

    private fun loadnative(){
        binding.frNativeGeneral.isVisible = false
    }
}
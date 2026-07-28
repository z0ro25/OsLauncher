package com.oslauncher.applauncher.themelauncher.Features.intro

import android.view.LayoutInflater
import androidx.activity.addCallback
import androidx.viewpager2.widget.ViewPager2
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.home.HomeActivity
import com.oslauncher.applauncher.themelauncher.Features.permission.PermissionActivity
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.ActivityIntroBinding
import com.oslauncher.applauncher.themelauncher.extensions.launchActivity
import com.oslauncher.applauncher.themelauncher.model.IntroModel
import com.oslauncher.applauncher.themelauncher.tool.sharePreferenceTool.SharePrefUtils

class IntroActivity : BaseActivity<ActivityIntroBinding>() {
    override val setViewBinding: ActivityIntroBinding
        get() = ActivityIntroBinding.inflate(LayoutInflater.from(this))

    var listIntro: ArrayList<IntroModel> = arrayListOf()
    val adapter: IntroAdapter by lazy { IntroAdapter(this, listIntro) }

    override fun initView() {
        SharePrefUtils.putBoolean(this, "IS_FIRST_TIME", false)
        listIntro.clear()
        listIntro.add(
            IntroModel(
                getString(R.string.app_name), getString(R.string.message1), R.drawable.bg_page_1
            )
        )
        listIntro.add(
            IntroModel(
                getString(R.string.app_name), getString(R.string.message2), R.drawable.bg_page_2
            )
        )
        listIntro.add(
            IntroModel(
                getString(R.string.app_name), getString(R.string.message3), R.drawable.bg_page_3
            )
        )
        binding.viewPager2.adapter = adapter

        onBackPressedDispatcher.addCallback {
            finishAffinity()
        }
    }

    override fun viewListener() {
        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
            }
        })

        adapter.onNextPageListener = {
            if (binding.viewPager2.currentItem < listIntro.size - 1) {
                binding.viewPager2.currentItem += 1
            } else {
                goToHome()
            }
        }
    }


    override fun dataObservable() {

    }

    private fun goToHome() {
        if (!SharePrefUtils.getBoolean(this, "PERMISSION_SHOWED", false)) {
            launchActivity<PermissionActivity> { }
        } else launchActivity<HomeActivity> { }
    }

}

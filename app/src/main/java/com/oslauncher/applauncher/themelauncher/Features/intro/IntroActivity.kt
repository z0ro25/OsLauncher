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
import com.oslauncher.applauncher.themelauncher.theme.AppThemeManager
import com.oslauncher.applauncher.themelauncher.tool.sharePreferenceTool.SharePrefUtils

class IntroActivity : BaseActivity<ActivityIntroBinding>() {
    override val setViewBinding: ActivityIntroBinding
        get() = ActivityIntroBinding.inflate(layoutInflater)

    var listIntro: ArrayList<IntroModel> = arrayListOf()
    val adapter: IntroAdapter by lazy { IntroAdapter(this, listIntro) }

    override fun initView() {
        SharePrefUtils.putBoolean(this, "IS_FIRST_TIME", false)
        listIntro.clear()
        // Dùng isDark() (không phải getMode()) để ảnh khớp nền onb_bg dưới mọi policy (MANUAL/AUTO/SYSTEM).
        if (AppThemeManager.isDark(this)){
            // Dark: ảnh nền đen (img_onbN)
            listIntro = arrayListOf(
                IntroModel(
                    getString(R.string.title1), getString(R.string.message1), R.drawable.img_onb1
                ),
                IntroModel(
                    getString(R.string.title2), getString(R.string.message2), R.drawable.img_onb2
                ),
                IntroModel(
                    getString(R.string.title3), getString(R.string.message3), R.drawable.img_onb3
                )
            )
        }else{
            // Light: ảnh nền trắng (img_onbN_light)
            listIntro = arrayListOf(
                IntroModel(
                    getString(R.string.title1), getString(R.string.message1), R.drawable.img_onb1_light
                ),
                IntroModel(
                    getString(R.string.title2), getString(R.string.message2), R.drawable.img_onb2_light
                ),
                IntroModel(
                    getString(R.string.title3), getString(R.string.message3), R.drawable.img_onb3_light
                )
            )
        }



        binding.viewPager2.adapter = adapter
        binding.dotindicator.attachTo(binding.viewPager2)
        binding.tvTitle.text = listIntro[0].title
        onBackPressedDispatcher.addCallback {
            finishAffinity()
        }
    }

    override fun viewListener() {
        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.tvTitle.text = listIntro[position].title
                binding.tvContinue.text = if (position == listIntro.size - 1) getString(R.string.continue_) else getString(R.string.next)
            }
        })

        binding.tvContinue.setOnClickListener {
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

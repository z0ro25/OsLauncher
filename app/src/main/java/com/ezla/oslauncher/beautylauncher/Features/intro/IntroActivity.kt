package com.ezla.oslauncher.beautylauncher.Features.intro

import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.ezla.oslauncher.beautylauncher.Base.BaseActivity
import com.ezla.oslauncher.beautylauncher.Features.home.HomeActivity
import com.ezla.oslauncher.beautylauncher.Features.languageStart.LanguageStartActivity
import com.ezla.oslauncher.beautylauncher.Features.permission.PermissionActivity
import com.ezla.oslauncher.beautylauncher.R
import com.ezla.oslauncher.beautylauncher.databinding.ActivityIntroBinding
import com.ezla.oslauncher.beautylauncher.extensions.launchActivity
import com.ezla.oslauncher.beautylauncher.model.IntroModel
import com.ezla.oslauncher.beautylauncher.theme.AppThemeManager
import com.truongnt.ios.ioslite.common.ads.AdsError
import com.truongnt.ios.ioslite.common.ads.AdsNative
import com.truongnt.ios.ioslite.common.ads.AdsNativeCallback
import com.truongnt.ios.ioslite.common.ads.AdsSlot
import com.truongnt.ios.ioslite.common.config.RemoteConfigs
import com.truongnt.ios.ioslite.common.config.SharePrefUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IntroActivity : BaseActivity<ActivityIntroBinding>() {
    override val setViewBinding: ActivityIntroBinding
        get() = ActivityIntroBinding.inflate(layoutInflater)

    var listIntro: ArrayList<IntroModel> = arrayListOf()
    val adapter: IntroAdapter by lazy { IntroAdapter(this, listIntro) }

    override fun initView() {
        SharePrefUtils.putBoolean(this, "IS_FIRST_TIME", false)
        listIntro.clear()
        // Dùng isDark() (không phải getMode()) để ảnh khớp nền onb_bg dưới mọi policy (MANUAL/AUTO/SYSTEM).
        if (AppThemeManager.isDark(this)) {
            // Dark: ảnh nền đen (img_onbN)
            listIntro = arrayListOf(
                IntroModel(
                    "onb1",
                    getString(R.string.title1),
                    getString(R.string.message1),
                    R.drawable.img_onb1
                ),
                IntroModel(
                    "onb2",
                    getString(R.string.title2),
                    getString(R.string.message2),
                    R.drawable.img_onb2
                ),
                IntroModel(
                    "onb3",
                    getString(R.string.title3),
                    getString(R.string.message3),
                    R.drawable.img_onb3
                )
            )
        } else {
            // Light: ảnh nền trắng (img_onbN_light)
            listIntro = arrayListOf(
                IntroModel(
                    "onb1",
                    getString(R.string.title1),
                    getString(R.string.message1),
                    R.drawable.img_onb1_light
                ),
                IntroModel(
                    "onb2",
                    getString(R.string.title2),
                    getString(R.string.message2),
                    R.drawable.img_onb2_light
                ),
                IntroModel(
                    "onb3",
                    getString(R.string.title3),
                    getString(R.string.message3),
                    R.drawable.img_onb3_light
                )
            )
        }

        if (LanguageStartActivity.nativeOnbFull) {
            listIntro.add(2, IntroModel("native_full"))
        }

        binding.viewPager2.adapter = adapter
        binding.dotindicator.attachTo(binding.viewPager2)
        binding.tvTitle.text = listIntro[0].title
        onBackPressedDispatcher.addCallback {
            finishAffinity()
        }

        MainScope().launch {
            delay(300)
            if (RemoteConfigs.isAdsEnabled(RemoteConfigs.NATIVE_ONB1)) {
                AdsNative.show(
                    binding.frNative,
                    AdsSlot.NATIVE_ONBOARDING_1,
                    com.truongnt.ios.ioslite.common.R.layout.layout_native_onb,
                    object : AdsNativeCallback() {
                        override fun onLoadFailed(error: AdsError) {
                            super.onLoadFailed(error)
                            binding.frNative.isVisible = false
                        }
                    })
            }
        }

        preloadnative()
    }

    override fun viewListener() {
        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val model = listIntro[position]
                if (model.position == "native_full") {
                    binding.llContent.isVisible = false
                    binding.frNative.isVisible = false
                    binding.icClose.isVisible = true

                } else {
                    binding.icClose.isVisible = false
                    binding.llContent.isVisible = true
                    binding.tvTitle.text = listIntro[position].title
                    binding.tvContinue.text =
                        if (position == listIntro.size - 1) getString(R.string.continue_) else getString(
                            R.string.next
                        )
                    binding.frNative.isVisible = true
                    binding.frNative.removeAllViews()
                    when (model.position) {
                        "onb1" -> {
                            if (RemoteConfigs.isAdsEnabled(RemoteConfigs.NATIVE_ONB1)) {
                                AdsNative.show(
                                    binding.frNative,
                                    AdsSlot.NATIVE_ONBOARDING_1,
                                    com.truongnt.ios.ioslite.common.R.layout.layout_native_onb,
                                    object : AdsNativeCallback() {
                                        override fun onLoadFailed(error: AdsError) {
                                            super.onLoadFailed(error)
                                            binding.frNative.isVisible = false
                                        }
                                    })
                            }
                        }

                        "onb2" -> {
                            if (RemoteConfigs.isAdsEnabled(RemoteConfigs.NATIVE_ONB2)) {
                                AdsNative.show(
                                    binding.frNative,
                                    AdsSlot.NATIVE_ONBOARDING_2,
                                    com.truongnt.ios.ioslite.common.R.layout.layout_native_onb,
                                    object : AdsNativeCallback() {
                                        override fun onLoadFailed(error: AdsError) {
                                            super.onLoadFailed(error)
                                            binding.frNative.isVisible = false
                                        }
                                    })
                            }
                        }

                        "onb3" -> {
                            if (RemoteConfigs.isAdsEnabled(RemoteConfigs.NATIVE_ONB3)) {
                                AdsNative.show(
                                    binding.frNative,
                                    AdsSlot.NATIVE_ONBOARDING_3,
                                    com.truongnt.ios.ioslite.common.R.layout.layout_native_onb,
                                    object : AdsNativeCallback() {
                                        override fun onLoadFailed(error: AdsError) {
                                            super.onLoadFailed(error)
                                            binding.frNative.isVisible = false
                                        }
                                    })
                            }
                        }
                    }
                }
            }
        })

        binding.tvContinue.setOnClickListener {
            if (binding.viewPager2.currentItem < listIntro.size - 1) {
                binding.viewPager2.currentItem += 1
            } else {
                goToHome()
            }
        }

        binding.icClose.setOnClickListener {
            binding.viewPager2.currentItem++
        }
    }


    override fun dataObservable() {

    }

    private fun goToHome() {
        if (!SharePrefUtils.getBoolean(this, "PERMISSION_SHOWED", false)) {
            launchActivity<PermissionActivity> { }
        } else launchActivity<HomeActivity> { }
    }

    fun preloadnative() {
        if (RemoteConfigs.isAdsEnabled(RemoteConfigs.NATIVE_PERMISSION)) {
            AdsNative.preload(this, AdsSlot.NATIVE_PERMISSION, object : AdsNativeCallback() {

            })

        }
    }

}

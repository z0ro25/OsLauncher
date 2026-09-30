package com.ezla.oslauncher.beautylauncher.Features.intro

import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.ezla.oslauncher.beautylauncher.Base.BaseActivity
import com.ezla.oslauncher.beautylauncher.Features.home.HomeActivity
import com.ezla.oslauncher.beautylauncher.Features.permission.PermissionActivity
import com.ezla.oslauncher.beautylauncher.R
import com.ezla.oslauncher.beautylauncher.databinding.ActivityIntroBinding
import com.ezla.oslauncher.beautylauncher.extensions.launchActivity
import com.ezla.oslauncher.beautylauncher.model.IntroModel
import com.ezla.oslauncher.beautylauncher.theme.AppThemeManager
import com.ezt.v2.ezt.admobdemo.ads.NativeAds
import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk
import com.ezt.v2.ezt.admobdemo.ads.placement.AdPreloadState
import com.truongnt.ios.ioslite.common.config.AppAds
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

        val ok = AppAds.kit.nativeInlinePreloadStates.value["native_onb_full"] == AdPreloadState.READY
        if (ok) listIntro.add(2, IntroModel("native_full"))

        binding.viewPager2.adapter = adapter
        binding.dotindicator.attachTo(binding.viewPager2)
        binding.tvTitle.text = listIntro[0].title
        onBackPressedDispatcher.addCallback {
            finishAffinity()
        }

        preloadnative()
    }

    // Gom 1 chỗ vì onb1/onb2/onb3 dùng chung layout, chỉ khác placement key.
    private fun showIntroNative(adUnitId: String) {
        AppAds.kit.showNativeInline(
            this,
            adUnitId,
            binding.frNative, R.layout.layout_native_onb
        ){ state ->
            binding.viewPager2.isUserInputEnabled = state != AdPreloadState.LOADING
        }
    }

    companion object {
        /** Hạn chờ SDK trả native; 15000ms là đúng default của {@code NativeAds.initNativeInline}. */
        private const val NATIVE_LOAD_TIMEOUT_MS = 15_000L
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
                            if (!AdsSdk.isAdFree) {
                                showIntroNative("native_onb1")
                            }
                        }

                        "onb2" -> {
                            if (!AdsSdk.isAdFree) {
                                showIntroNative("native_onb2")
                            }
                        }

                        "onb3" -> {
                            if (!AdsSdk.isAdFree) {
                                showIntroNative("native_onb3")
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
        if (!AdsSdk.isAdFree) {
            // Hâm nóng native cho màn Permission kế tiếp. SDK không có callback ở bước này nên
            // không cần truyền gì thêm; lượt bind thật ở PermissionActivity vẫn tự tải nếu chưa có.
            AppAds.kit.preloadNativeInline( "native_permission")
        }
    }

}

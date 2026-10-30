package com.ezla.oslauncher.appbase.onboarding

import android.view.LayoutInflater
import android.view.View
import androidx.activity.addCallback
import androidx.viewpager2.widget.ViewPager2
import com.ezla.oslauncher.appbase.R
import com.ezla.oslauncher.appbase.base.BaseActivity
import com.ezla.oslauncher.appbase.databinding.AppbaseActivityOnboardingBinding
import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk
import com.truongnt.ios.ioslite.common.config.AppAds

// Onboarding 3 trang dùng chung cho app tiện ích (bố cục Figma, hành vi như ThemesOnboardingActivity).
// Module con chỉ khai trang + việc làm khi xong (lưu cờ, mở màn chính).
abstract class BaseOnboardingActivity : BaseActivity<AppbaseActivityOnboardingBinding>() {

    override val setViewBinding: AppbaseActivityOnboardingBinding
        get() = AppbaseActivityOnboardingBinding.inflate(LayoutInflater.from(this))

    // Ảnh tràn lên dưới status bar như Figma.
    override val padStatusBar = false

    protected abstract val pages: List<OnboardingPage>

    protected abstract fun onOnboardingDone()

    override fun initView() {
        binding.vpOnboarding.adapter = OnboardingAdapter(pages)
        binding.dotIndicator.attachTo(binding.vpOnboarding)
        render(0)
    }

    override fun viewListener() {
        binding.vpOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = render(position)
        })
        binding.tvNext.setOnClickListener {
            val current = binding.vpOnboarding.currentItem
            if (current < pages.lastIndex) {
                binding.vpOnboarding.currentItem = current + 1
            } else {
                // Khoá nút trước khi mở màn chính: bấm đúp không mở 2 lần.
                it.isEnabled = false
                onOnboardingDone()
                finish()
            }
        }
        // Back lùi trang; ở trang đầu thì thoát (chưa lưu cờ nên lần sau vẫn vào onboarding).
        onBackPressedDispatcher.addCallback(this) {
            val current = binding.vpOnboarding.currentItem
            if (current > 0) binding.vpOnboarding.currentItem = current - 1 else finish()
        }
    }

    override fun dataObservable() {}

    private fun render(position: Int) {
        binding.tvNext.setText(
            if (position == pages.lastIndex) R.string.appbase_onboarding_get_started
            else R.string.appbase_onboarding_next
        )
        showPageNative(position)
    }

    // Ad-free / chưa có kit / ad lỗi -> INVISIBLE (không GONE) để giữ chỗ 300dp, ảnh không bị kéo giãn.
    // Ad về muộn của trang cũ thì bỏ qua.
    private fun showPageNative(position: Int) {
        val container = binding.frAds
        container.removeAllViews()
        val key = NATIVE_KEYS.getOrNull(position)
        if (key == null || AdsSdk.isAdFree || !AppAds.hasKit()) {
            container.visibility = View.INVISIBLE
            return
        }
        container.visibility = View.VISIBLE
        AppAds.showNativeFill(this, key, container, R.layout.appbase_layout_native_onb,
            com.truongnt.ios.ioslite.common.R.layout.shimmer_native_large) { hasAd ->
            if (!hasAd && binding.vpOnboarding.currentItem == position) container.visibility = View.INVISIBLE
        }
    }

    private companion object {
        // Dùng chung key native onb của Themes theo yêu cầu (cùng ad unit, báo cáo gộp với Themes).
        val NATIVE_KEYS = listOf("native_themes_onb1", "native_themes_onb2", "native_themes_onb3")
    }
}

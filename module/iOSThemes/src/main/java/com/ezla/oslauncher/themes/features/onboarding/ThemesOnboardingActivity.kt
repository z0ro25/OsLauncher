package com.ezla.oslauncher.themes.features.onboarding

import com.ezla.oslauncher.themes.Models.OnboardingPage
import android.view.LayoutInflater
import android.view.View
import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk
import com.truongnt.ios.ioslite.common.config.AppAds
import androidx.activity.addCallback
import androidx.viewpager2.widget.ViewPager2
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.ThemesEntry
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.databinding.ThemesActivityOnboardingBinding
import com.ezla.oslauncher.themes.extensions.tap
import com.ezla.oslauncher.themes.features.home.ThemesActivity

class ThemesOnboardingActivity : BaseActivity<ThemesActivityOnboardingBinding>() {
    override val setViewBinding: ThemesActivityOnboardingBinding
        get() = ThemesActivityOnboardingBinding.inflate(LayoutInflater.from(this))

    private val pages = listOf(
        OnboardingPage(R.drawable.themes_img_onboarding_1, R.string.themes_onboarding_title_1),
        OnboardingPage(R.drawable.themes_img_onboarding_2, R.string.themes_onboarding_title_2),
        OnboardingPage(R.drawable.themes_img_onboarding_3, R.string.themes_onboarding_title_3)
    )

    override fun initView() {
        binding.vpOnboarding.adapter = OnboardingAdapter(pages)
        // DotsIndicator tự animate theo pager; số dot = số trang, không cần tự vẽ.
        binding.dotIndicator.attachTo(binding.vpOnboarding)
        render(0)
    }

    override fun viewListener() {
        binding.vpOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = render(position)
        })
        binding.tvNext.tap {
            val current = binding.vpOnboarding.currentItem
            if (current < pages.lastIndex) binding.vpOnboarding.currentItem = current + 1
            else finishOnboarding()
        }
        // Back lùi trang; ở trang đầu thì thoát như mặc định.
        onBackPressedDispatcher.addCallback(this) {
            val current = binding.vpOnboarding.currentItem
            if (current > 0) binding.vpOnboarding.currentItem = current - 1 else finish()
        }
    }

    override fun dataObservable() {}

    // Chỉ đổi nhãn nút + native của trang; trạng thái dot do DotsIndicator tự lo.
    private fun render(position: Int) {
        binding.tvNext.setText(
            if (position == pages.lastIndex) R.string.themes_get_started else R.string.themes_next
        )
        showPageNative(position)
    }

    // Key riêng của Themes (tách báo cáo), tạm trỏ ad unit của native_onb1/2/3 trong placements JSON.
    private val nativeKeys = listOf("native_themes_onb1", "native_themes_onb2", "native_themes_onb3")

    // Ad-free / chưa có kit / ad lỗi -> ẩn frAds. Ad về muộn của trang cũ thì bỏ qua.
    private fun showPageNative(position: Int) {
        val container = binding.frAds
        container.removeAllViews()
        val key = nativeKeys.getOrNull(position)
        if (key == null || AdsSdk.isAdFree || !AppAds.hasKit()) {
            container.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        AppAds.showNativeFill(this, key, container, R.layout.themes_layout_native_onb,
            com.truongnt.ios.ioslite.common.R.layout.shimmer_native_large) { hasAd ->
            if (!hasAd && binding.vpOnboarding.currentItem == position) container.visibility = View.GONE
        }
    }

    private fun finishOnboarding() {
        ThemesEntry.markOnboardingDone(this)
        showActivity(ThemesActivity::class.java, null)
        finish()
    }
}

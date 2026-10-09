package com.ezla.oslauncher.themes.features.onboarding

import com.ezla.oslauncher.themes.Models.OnboardingPage
import android.view.LayoutInflater
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

    // Chỉ đổi nhãn nút; trạng thái dot do DotsIndicator tự lo.
    private fun render(position: Int) {
        binding.tvNext.setText(
            if (position == pages.lastIndex) R.string.themes_get_started else R.string.themes_next
        )
    }

    private fun finishOnboarding() {
        ThemesEntry.markOnboardingDone(this)
        showActivity(ThemesActivity::class.java, null)
        finish()
    }
}

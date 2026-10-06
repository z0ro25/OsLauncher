package com.ezla.oslauncher.themes.features.onboarding

import com.ezla.oslauncher.themes.Models.OnboardingPage
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.viewpager2.widget.ViewPager2
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.ThemesEntry
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.databinding.ThemesActivityOnboardingBinding
import com.ezla.oslauncher.themes.extensions.dpToPx
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
        buildSteps()
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

    private fun buildSteps() {
        binding.layoutStep.removeAllViews()
        repeat(pages.size) {
            binding.layoutStep.addView(View(this))
        }
    }

    private fun render(position: Int) {
        for (i in 0 until binding.layoutStep.childCount) {
            val active = i == position
            val step = binding.layoutStep.getChildAt(i)
            step.setBackgroundResource(if (active) R.drawable.themes_bg_step_active else R.drawable.themes_bg_step_inactive)
            step.layoutParams = LinearLayout.LayoutParams(
                dpToPx(if (active) 18 else 6, this), dpToPx(6, this)
            ).apply { if (i > 0) marginStart = dpToPx(3, this@ThemesOnboardingActivity) }
        }
        binding.tvNext.setText(if (position == pages.lastIndex) R.string.themes_get_started else R.string.themes_next)
    }

    private fun finishOnboarding() {
        ThemesEntry.markOnboardingDone(this)
        showActivity(ThemesActivity::class.java, null)
        finish()
    }
}

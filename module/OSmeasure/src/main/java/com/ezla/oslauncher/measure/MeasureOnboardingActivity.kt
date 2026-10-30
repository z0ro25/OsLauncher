package com.ezla.oslauncher.measure

import android.content.Intent
import com.ezla.oslauncher.appbase.onboarding.BaseOnboardingActivity
import com.ezla.oslauncher.appbase.onboarding.OnboardingPage

/** Onboarding lần đầu mở Measure (Figma "Onboarding 25..27"). */
class MeasureOnboardingActivity : BaseOnboardingActivity() {

    override val pages = listOf(
        OnboardingPage(R.drawable.measure_img_onboarding_1, R.string.measure_onboarding_title_1, R.string.measure_onboarding_desc_1),
        OnboardingPage(R.drawable.measure_img_onboarding_2, R.string.measure_onboarding_title_2, R.string.measure_onboarding_desc_2),
        OnboardingPage(R.drawable.measure_img_onboarding_3, R.string.measure_onboarding_title_3, R.string.measure_onboarding_desc_3)
    )

    override fun onOnboardingDone() {
        MeasureEntry.markOnboardingDone(this)
        startActivity(Intent(this, MeasureActivity::class.java))
    }
}

package com.ezla.oslauncher.clock

import android.content.Intent
import com.ezla.oslauncher.appbase.onboarding.BaseOnboardingActivity
import com.ezla.oslauncher.appbase.onboarding.OnboardingPage

/** Onboarding lần đầu mở Clock (Figma "Onboarding 40..42"). */
class ClockOnboardingActivity : BaseOnboardingActivity() {

    override val pages = listOf(
        OnboardingPage(R.drawable.osclock_img_onboarding_1, R.string.osclock_onboarding_title_1, R.string.osclock_onboarding_desc_1),
        OnboardingPage(R.drawable.osclock_img_onboarding_2, R.string.osclock_onboarding_title_2, R.string.osclock_onboarding_desc_2),
        OnboardingPage(R.drawable.osclock_img_onboarding_3, R.string.osclock_onboarding_title_3, R.string.osclock_onboarding_desc_3)
    )

    override fun onOnboardingDone() {
        ClockEntry.markOnboardingDone(this)
        startActivity(Intent(this, ClockActivity::class.java))
    }
}

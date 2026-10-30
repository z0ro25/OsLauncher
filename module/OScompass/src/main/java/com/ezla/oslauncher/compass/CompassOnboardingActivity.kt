package com.ezla.oslauncher.compass

import android.content.Intent
import com.ezla.oslauncher.appbase.onboarding.BaseOnboardingActivity
import com.ezla.oslauncher.appbase.onboarding.OnboardingPage

/** Onboarding lần đầu mở Compass (Figma "Onboarding 22..24"). */
class CompassOnboardingActivity : BaseOnboardingActivity() {

    override val pages = listOf(
        OnboardingPage(R.drawable.compass_img_onboarding_1, R.string.compass_onboarding_title_1, R.string.compass_onboarding_desc_1),
        OnboardingPage(R.drawable.compass_img_onboarding_2, R.string.compass_onboarding_title_2, R.string.compass_onboarding_desc_2),
        OnboardingPage(R.drawable.compass_img_onboarding_3, R.string.compass_onboarding_title_3, R.string.compass_onboarding_desc_3)
    )

    override fun onOnboardingDone() {
        CompassEntry.markOnboardingDone(this)
        startActivity(Intent(this, CompassActivity::class.java))
    }
}

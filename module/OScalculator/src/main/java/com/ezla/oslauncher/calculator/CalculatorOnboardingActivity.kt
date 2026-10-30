package com.ezla.oslauncher.calculator

import android.content.Intent
import com.ezla.oslauncher.appbase.onboarding.BaseOnboardingActivity
import com.ezla.oslauncher.appbase.onboarding.OnboardingPage

/** Onboarding lần đầu mở Calculator (Figma "Onboarding 55..57"). */
class CalculatorOnboardingActivity : BaseOnboardingActivity() {

    override val pages = listOf(
        OnboardingPage(R.drawable.calc_img_onboarding_1, R.string.calc_onboarding_title_1, R.string.calc_onboarding_desc_1),
        OnboardingPage(R.drawable.calc_img_onboarding_2, R.string.calc_onboarding_title_2, R.string.calc_onboarding_desc_2),
        OnboardingPage(R.drawable.calc_img_onboarding_3, R.string.calc_onboarding_title_3, R.string.calc_onboarding_desc_3)
    )

    override fun onOnboardingDone() {
        CalculatorEntry.markOnboardingDone(this)
        startActivity(Intent(this, CalculatorActivity::class.java))
    }
}

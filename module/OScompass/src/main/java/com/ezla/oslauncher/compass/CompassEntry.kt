package com.ezla.oslauncher.compass

import android.content.Context
import android.content.Intent
import com.truongnt.ios.ioslite.common.config.SharePrefUtils

// Cổng vào duy nhất của module (giống CalculatorEntry): nơi gọi không cần biết tên Activity.
object CompassEntry {

    private const val KEY_ONBOARDING_DONE = "compass_onboarding_done"

    fun open(context: Context) {
        val intent = Intent(context, CompassActivity::class.java)
        // Luôn NEW_TASK: Activity có taskAffinity riêng, phải mở thành task riêng như Themes.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun isOnboardingDone(context: Context): Boolean =
        SharePrefUtils.getBoolean(context, KEY_ONBOARDING_DONE, false)

    fun markOnboardingDone(context: Context) {
        SharePrefUtils.putBoolean(context, KEY_ONBOARDING_DONE, true)
    }
}

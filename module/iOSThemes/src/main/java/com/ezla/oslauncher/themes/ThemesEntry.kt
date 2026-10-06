package com.ezla.oslauncher.themes

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.ezla.oslauncher.themes.features.home.ThemesActivity
import com.ezla.oslauncher.themes.features.onboarding.ThemesOnboardingActivity
import com.truongnt.ios.ioslite.common.config.SharePrefUtils

// Cổng vào duy nhất của module cho :app (Splash). Lần đầu -> Onboarding, sau đó -> Home.
object ThemesEntry {

    private const val KEY_ONBOARDING_DONE = "themes_onboarding_done"

    fun open(activity: Activity) {
        val target = if (isOnboardingDone(activity)) ThemesActivity::class.java
        else ThemesOnboardingActivity::class.java
        activity.startActivity(Intent(activity, target))
    }

    fun isOnboardingDone(context: Context): Boolean =
        SharePrefUtils.getBoolean(context, KEY_ONBOARDING_DONE, false)

    fun markOnboardingDone(context: Context) {
        SharePrefUtils.putBoolean(context, KEY_ONBOARDING_DONE, true)
    }
}

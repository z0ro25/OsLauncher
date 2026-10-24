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

    // Màn Subs nằm ở :app nên module không mở trực tiếp được; :app gắn hàm mở ở Application.
    var premiumOpener: ((Activity) -> Unit)? = null

    fun openPremium(activity: Activity) {
        premiumOpener?.invoke(activity)
    }

    // Kèm vị trí mở (vd "themes_settings") để :app log user_purchase_subs_<source> khi mua xong.
    var premiumOpenerWithSource: ((Activity, String) -> Unit)? = null

    fun openPremium(activity: Activity, source: String) {
        val opener = premiumOpenerWithSource
        if (opener != null) opener(activity, source) else openPremium(activity)
    }

    // Dialog Rate dùng chung của :app (RatingDialog + In-App Review); :app gắn ở Application.
    // onRated chạy khi user đã rate xong (để ẩn dòng Rate).
    var rateOpener: ((Activity, onRated: () -> Unit) -> Unit)? = null

    fun openRate(activity: Activity, onRated: () -> Unit) {
        rateOpener?.invoke(activity, onRated)
    }

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

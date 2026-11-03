package com.ezla.oslauncher.controlcenter

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.ezla.oslauncher.controlcenter.features.main.CcMainActivity
import com.ezla.oslauncher.controlcenter.features.onboarding.CcOnboardingActivity

// Cổng vào duy nhất của module cho :app (giống ThemesEntry). Lần đầu -> Onboarding, sau đó -> Main.
object ControlCenterEntry {

    // Các màn nằm ở :app / module khác nên module không mở trực tiếp được; :app gắn ở Application.
    var languageOpener: ((Activity) -> Unit)? = null
    var themesOpener: ((Activity) -> Unit)? = null

    // Ô Báo thức / Máy tính trên panel mở app Clock / Calculator của launcher (context là service).
    var alarmOpener: ((Context) -> Unit)? = null
    var calculatorOpener: ((Context) -> Unit)? = null

    fun open(activity: Activity) {
        val target = if (CcPrefs.isOnboardingDone(activity)) CcMainActivity::class.java
        else CcOnboardingActivity::class.java
        activity.startActivity(Intent(activity, target))
    }

    // Từ overlay (service, không có Activity) mở màn Main: bắt buộc NEW_TASK.
    fun openFromService(context: Context) {
        context.startActivity(
            Intent(context, CcMainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openLanguage(activity: Activity) {
        languageOpener?.invoke(activity)
    }

    fun openThemes(activity: Activity) {
        themesOpener?.invoke(activity)
    }
}

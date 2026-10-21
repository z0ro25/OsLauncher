package com.ezla.oslauncher.themes.base

import android.app.Activity
import android.app.Application
import android.os.Bundle

// SDK eztech không báo lúc App Open hiện/đóng -> bám vòng đời AdActivity của GMA (activity trong suốt
// dùng chung cho open/inter/rewarded). Đếm số AdActivity đang sống: > 0 là đang có ad full màn.
object FullscreenAdTracker : Application.ActivityLifecycleCallbacks {

    private const val AD_ACTIVITY = "com.google.android.libraries.ads.mobile.sdk.common.AdActivity"

    private var registered = false
    private var adCount = 0
    private val listeners = mutableSetOf<(Boolean) -> Unit>()

    val showing get() = adCount > 0

    fun register(app: Application) {
        if (registered) return
        registered = true
        app.registerActivityLifecycleCallbacks(this)
    }

    fun addListener(listener: (Boolean) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (Boolean) -> Unit) {
        listeners.remove(listener)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity.javaClass.name != AD_ACTIVITY) return
        adCount++
        notifyListeners()
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity.javaClass.name != AD_ACTIVITY) return
        adCount = (adCount - 1).coerceAtLeast(0)
        notifyListeners()
    }

    // Copy trước khi duyệt: listener có thể tự gỡ mình trong lúc được gọi.
    private fun notifyListeners() {
        val value = showing
        listeners.toList().forEach { it(value) }
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}

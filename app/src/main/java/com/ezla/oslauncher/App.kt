package com.ezla.oslauncher

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.truongnt.ios.launcher.Utilities
import com.ezla.oslauncher.Base.BaseLauncherApplication
import com.ezla.oslauncher.theme.AppThemeManager

class App : BaseLauncherApplication(), ActivityLifecycleCallbacks {
    override fun onCreate() {
        super.onCreate()
        Utilities.setScreenWidth(this)
        // Áp Dark/Light đã lưu cho toàn bộ màn app (chỉ module app).
        AppThemeManager.applySavedMode(this)
    }

    override fun onTerminate() {
        super.onTerminate()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
        
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        
    }

    override fun onActivityDestroyed(activity: Activity) {
        
    }

    private fun setUpAdjust() {

    }

    fun buildDebug(): Boolean? = false
}
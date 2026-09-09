package com.oslauncher.applauncher.themelauncher

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.Log
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.LogLevel
import com.amz.ios.launcher.Utilities
import com.oslauncher.applauncher.themelauncher.Base.BaseLauncherApplication
import com.oslauncher.applauncher.themelauncher.theme.AppThemeManager

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
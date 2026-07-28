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

class App : BaseLauncherApplication(), ActivityLifecycleCallbacks {
    override fun onCreate() {
        super.onCreate()
        Utilities.setScreenWidth(this)
    }

    override fun onTerminate() {
        super.onTerminate()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        setUpAdjust()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        
    }

    override fun onActivityResumed(activity: Activity) {
        Adjust.onResume()
    }

    override fun onActivityPaused(activity: Activity) {
        Adjust.onPause()
    }

    override fun onActivityStopped(activity: Activity) {
        
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        
    }

    override fun onActivityDestroyed(activity: Activity) {
        
    }

    private fun setUpAdjust() {
        val environment: String = AdjustConfig.ENVIRONMENT_PRODUCTION
        val config: AdjustConfig = AdjustConfig(this, "adjust key", environment)
        config.setLogLevel(LogLevel.VERBOSE)
        config.setFbAppId(getString(R.string.facebook_app_id))
        config.setDefaultTracker("adjust key")
        config.setSendInBackground(true)
        Adjust.onCreate(config)
        // Enable the SDK
        Adjust.setEnabled(true)
    }

    fun buildDebug(): Boolean? = false
}
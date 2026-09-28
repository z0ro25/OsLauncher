package com.ezla.oslauncher.beautylauncher

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.truongnt.ios.launcher.Utilities
import com.ezla.oslauncher.beautylauncher.Base.BaseLauncherApplication
import com.ezla.oslauncher.beautylauncher.theme.AppThemeManager
import com.ezla.oslauncher.beautylauncher.utils.BillingUtils

class App : BaseLauncherApplication() {
    override fun onCreate() {
        super.onCreate()
        Utilities.setScreenWidth(this)
        // Áp Dark/Light đã lưu cho toàn bộ màn app (chỉ module app).
        AppThemeManager.applySavedMode(this)

        BillingUtils.initBilling(this)
    }
}
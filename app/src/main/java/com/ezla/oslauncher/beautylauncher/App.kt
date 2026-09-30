package com.ezla.oslauncher.beautylauncher

import android.os.Bundle
import android.util.Log
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.ezla.oslauncher.beautylauncher.Base.BaseLauncherApplication
import com.ezla.oslauncher.beautylauncher.theme.AppThemeManager
import com.ezla.oslauncher.beautylauncher.utils.BillingUtils
import com.ezt.v2.ezt.admobdemo.ads.core.AdUnitDefaults
import com.ezt.v2.ezt.admobdemo.ads.core.AdsHostConfig
import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk
import com.google.firebase.analytics.FirebaseAnalytics
import com.truongnt.ios.launcher.Utilities

class App : BaseLauncherApplication() {
    override fun onCreate() {
        super.onCreate()
        Utilities.setScreenWidth(this)

        // Áp Dark/Light đã lưu cho toàn bộ màn app (chỉ module app).
        AppThemeManager.applySavedMode(this)

        BillingUtils.initBilling(this)

        val preferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        val analytics = FirebaseAnalytics.getInstance(this)
        val accepted = AdsSdk.configure(
            AdsHostConfig(
                adUnits = AdUnitDefaults(getString(R.string.admob_app_id)),
                // SDK gọi lại lambda này để kiểm tra premium/remove ads trước khi phục vụ quảng cáo.
                isAdFree = {
                    BillingUtils.isSubsCached(this) || preferences.getBoolean("RemoveAd", false)
                },
                onEvent = {
                    analytics.logEvent(
                        it,
                        null
                    )
                }, // Nhận tên event ads và gửi vào Firebase của app.
                // Nhận doanh thu từ SDK; micros là một phần triệu đơn vị tiền tệ.
                onPaid = { micros, currency ->
                    recordRevenue(analytics, micros, currency)
                    runCatching {
                        Adjust.trackAdRevenue(AdjustAdRevenue("admob_sdk").apply {
                            setRevenue(micros / 1_000_000.0, currency)
                        })
                    }.onFailure { Log.w("adjustError", "Cannot report paid event", it) }
                },
            )
        )

    }

    @Synchronized
    private fun recordRevenue(analytics: FirebaseAnalytics, micros: Long, currency: String) {
        if (micros < 0 || currency.isBlank()) return
        val prefs = getSharedPreferences("ad_revenue", MODE_PRIVATE)
        val key = "revenue_micros_$currency"
        val total = prefs.getLong(key, 0) + micros
        if (total >= 10_000) {
            analytics.logEvent("Daily_Ads_Revenue", Bundle().apply {
                putDouble(FirebaseAnalytics.Param.VALUE, total / 1_000_000.0)
                putString(FirebaseAnalytics.Param.CURRENCY, currency)
            })
            prefs.edit().putLong(key, 0).apply()
        } else prefs.edit().putLong(key, total).apply()
    }
}
package com.ezla.oslauncher.beautylauncher

import android.os.Bundle
import android.util.Log
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.ezla.oslauncher.beautylauncher.Base.BaseLauncherApplication
import com.ezla.oslauncher.beautylauncher.theme.AppThemeManager
import android.content.Intent
import com.ezla.oslauncher.beautylauncher.Features.subs.SubsAct
import com.ezla.oslauncher.beautylauncher.dialog.RateFlow
import com.ezla.oslauncher.beautylauncher.utils.BillingUtils
import com.ezla.oslauncher.themes.ThemesEntry
import com.ezt.v2.ezt.admobdemo.ads.core.AdUnitDefaults
import com.ezt.v2.ezt.admobdemo.ads.core.AdsHostConfig
import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk
import com.google.firebase.analytics.FirebaseAnalytics
import com.truongnt.ios.ioslite.common.analytics.AnalyticsDelegate
import com.truongnt.ios.launcher.Utilities

class App : BaseLauncherApplication() {
    override fun onCreate() {
        super.onCreate()

        Utilities.setScreenWidth(this)

        // Áp Dark/Light đã lưu cho toàn bộ màn app (chỉ module app).
        AppThemeManager.applySavedMode(this)

        BillingUtils.initBilling(this)
        // Nút Premium / Unlock all trong module Themes mở màn Subs của :app.
        ThemesEntry.premiumOpener = { it.startActivity(Intent(it, SubsAct::class.java)) }
        // Rate us ở Settings module Themes dùng đúng dialog rate của :app.
        ThemesEntry.rateOpener = { activity, onRated -> RateFlow.show(activity, false, onRated) }

        val preferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        val analytics = FirebaseAnalytics.getInstance(this)
        // Click trong module launcher (icon app / icon Themes trên desktop) đi qua đây vào Firebase.
        AnalyticsDelegate.setEventLogger { event -> analytics.logEvent(event, null) }
        // Event có param từ các module (vd user_apply ở module Themes).
        AnalyticsDelegate.setParamEventLogger { event, params ->
            analytics.logEvent(event, Bundle().apply { params.forEach { (k, v) -> putString(k, v) } })
        }
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
package com.ezla.oslauncher.themes.features.settings

import com.ezla.oslauncher.themes.Models.SettingItem
import android.app.Activity

// Hành động màn Settings — để sẵn, chưa nối (Premium, gửi đánh giá, contact/update/share/...).
interface SettingsCallback {
    fun onPremiumClick(activity: Activity) {}
    fun onRateSubmit(activity: Activity, stars: Int) {}
    fun onItemClick(activity: Activity, item: SettingItem) {}
}

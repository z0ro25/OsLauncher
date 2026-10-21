package com.ezla.oslauncher.themes.features.settings

import com.ezla.oslauncher.themes.Models.SettingItem
import android.app.Activity

// Hành động màn Settings — để sẵn, chưa nối (Premium, contact/update/share/...).
// Rate us không qua đây: dùng dialog rate của :app (ThemesEntry.openRate).
interface SettingsCallback {
    fun onPremiumClick(activity: Activity) {}
    fun onItemClick(activity: Activity, item: SettingItem) {}
}

package com.ezla.oslauncher.themes.apply

import com.ezla.oslauncher.themes.Models.ThemeItem
import android.content.Context

// Chỗ gọi sang launcher để áp theme. Chưa nối: định dạng theme từ API chưa chốt
// (APK riêng -> LauncherAppState.applyNewTheme, hay bộ icon + wallpaper tải về).
interface ThemeApplier {
    fun apply(context: Context, theme: ThemeItem, onResult: (Boolean) -> Unit)
}

object NotImplementedThemeApplier : ThemeApplier {
    override fun apply(context: Context, theme: ThemeItem, onResult: (Boolean) -> Unit) {
        onResult(false)
    }
}

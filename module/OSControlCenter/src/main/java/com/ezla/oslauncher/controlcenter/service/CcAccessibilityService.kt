package com.ezla.oslauncher.controlcenter.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.ezla.oslauncher.controlcenter.CcPrefs
import com.ezla.oslauncher.controlcenter.overlay.OverlayController
import com.ezla.oslauncher.controlcenter.system.CcWallpaper

// Service chỉ dùng để vẽ overlay (TYPE_ACCESSIBILITY_OVERLAY), không đọc nội dung màn hình.
// Tự hiện thanh kéo khi kết nối nếu user đã bật -> khởi động lại máy vẫn còn Control Center.
class CcAccessibilityService : AccessibilityService() {

    private var overlay: OverlayController? = null

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            CcPrefs.KEY_ENABLED -> syncEnabled()
            else -> overlay?.onSettingsChanged()
        }
    }

    private val wallpaperReceiver = object : BroadcastReceiver() {
        @Suppress("DEPRECATION")
        override fun onReceive(c: Context, i: Intent) = CcWallpaper.invalidate(c)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        CcPrefs.prefs(this).registerOnSharedPreferenceChangeListener(prefsListener)
        @Suppress("DEPRECATION")
        ContextCompat.registerReceiver(this, wallpaperReceiver,
            IntentFilter(Intent.ACTION_WALLPAPER_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        syncEnabled()
    }

    private fun syncEnabled() {
        if (CcPrefs.isEnabled(this)) {
            // Lỗi dựng overlay không được làm crash service: crash là hệ thống tự tắt Accessibility.
            if (overlay == null) overlay = runCatching { OverlayController(this).also { it.attach() } }
                .onFailure { android.util.Log.e("CcEdge", "attach overlay lỗi", it) }.getOrNull()
        } else {
            overlay?.detach()
            overlay = null
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlay?.onConfigurationChanged()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        CcPrefs.prefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        runCatching { unregisterReceiver(wallpaperReceiver) }
        overlay?.detach()
        overlay = null
        if (instance === this) instance = null
    }

    // Đóng panel đang mở (vd trước khi mở màn quay màn hình từ ô Record).
    fun collapsePanels() {
        overlay?.closeAll()
    }

    companion object {
        @Volatile var instance: CcAccessibilityService? = null
            private set
    }
}

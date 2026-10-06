package com.ezla.oslauncher.themes.Api

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log

// Log gọi API, lọc logcat theo tag "ThemesApi". Chỉ bật khi APP debuggable (đọc từ ApplicationInfo,
// không dùng BuildConfig.DEBUG của library); bản release không in token/secret ra logcat.
object ApiLog {
    const val TAG = "ThemesApi"
    private const val CHUNK = 3500

    @Volatile
    var enabled = false
        private set

    fun init(context: Context) {
        enabled = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    // Logcat cắt dòng dài ~4000 ký tự -> chia nhỏ để body JSON không bị mất đoạn cuối.
    fun d(message: String) {
        if (!enabled) return
        if (message.length <= CHUNK) {
            Log.d(TAG, message)
            return
        }
        message.chunked(CHUNK).forEachIndexed { i, part -> Log.d(TAG, "[${i + 1}] $part") }
    }

    fun e(message: String, error: Throwable? = null) {
        if (enabled) Log.e(TAG, message, error)
    }
}

package com.ezla.oslauncher.themes.Api

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log

// Log của luồng API (tag ThemesApi). Logcat chỉ bật khi app debuggable; file Download
// (ApiFileLog) ghi ở MỌI bản để đọc lỗi release, token/chữ ký đã được che trước khi ghi.
object ApiLog {
    const val TAG = "ThemesApi"
    private const val CHUNK = 3500

    @Volatile
    var enabled = false
        private set

    fun init(context: Context) {
        enabled = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        ApiFileLog.init(context)
    }

    // Logcat cắt dòng dài ~4000 ký tự -> chia nhỏ để không mất đoạn cuối.
    fun d(message: String) {
        ApiFileLog.write("D", message)
        if (!enabled) return
        if (message.length <= CHUNK) {
            Log.d(TAG, message)
            return
        }
        message.chunked(CHUNK).forEachIndexed { i, part -> Log.d(TAG, "[${i + 1}] $part") }
    }

    fun e(message: String, error: Throwable? = null) {
        // stackTraceToString thay Log.getStackTraceString: hàm kia trả rỗng với UnknownHostException.
        ApiFileLog.write("E", if (error == null) message else "$message\n${error.stackTraceToString()}")
        if (enabled) Log.e(TAG, message, error)
    }
}

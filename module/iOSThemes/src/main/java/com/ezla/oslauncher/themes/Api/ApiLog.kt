package com.ezla.oslauncher.themes.Api

import android.util.Log
import com.ezla.oslauncher.themes.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

// Log đầy đủ request/response (kể cả Bearer token, header ký SAG), lọc logcat theo tag "ThemesApi".
// CHỈ bản debug: BuildConfig.DEBUG là hằng số nên R8 xoá hẳn nhánh này ở release.
object ApiLog {
    const val TAG = "ThemesApi"
    private const val CHUNK = 3500

    // Network interceptor để thấy header SAG gắn ở application interceptor; release trả nguyên client.
    fun attach(client: OkHttpClient): OkHttpClient {
        if (!BuildConfig.DEBUG) return client
        val logging = HttpLoggingInterceptor { d(it) }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return client.newBuilder().addNetworkInterceptor(logging).build()
    }

    // Logcat cắt dòng ~4000 ký tự -> chia nhỏ để body JSON không mất đoạn cuối.
    private fun d(message: String) {
        if (message.length <= CHUNK) {
            Log.d(TAG, message)
            return
        }
        message.chunked(CHUNK).forEachIndexed { i, part -> Log.d(TAG, "[${i + 1}] $part") }
    }
}

package com.ezla.oslauncher.themes.Api

import android.content.Context
import com.ezla.oslauncher.themes.BuildConfig
import com.mct.sag.SagClient
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Client dùng chung của module Themes (md §3: tạo MỘT SagClient, không tạo mới cho từng request).
object ApiClient {
    private const val BASE_URL = "https://launcher-os.eztechglobal.com/api/v1/"
    private const val TIMEOUT_SECONDS = 30L

    @Volatile
    private var api: ThemesApi? = null

    fun api(context: Context): ThemesApi =
        api ?: synchronized(this) { api ?: build(context.applicationContext).also { api = it } }

    private fun build(appContext: Context): ThemesApi {
        ApiLog.init(appContext)

        // Base client CHỈ chứa application interceptor (md §6 cho phép; network interceptor bị cấm
        // vì đọc được JWT/signature).
        val baseClient = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .apply {
                if (ApiLog.enabled) {
                    addInterceptor(
                        HttpLoggingInterceptor { ApiLog.d(it) }
                            .setLevel(HttpLoggingInterceptor.Level.BODY)
                    )
                }
            }
            .build()

        // SagClient dùng chung: ở request đầu tự tạo device session + Key Attestation + ký (Bearer JWT,
        // X-Timestamp/Nonce/Signature). Secret lấy từ gradle.properties -> BuildConfig, KHÔNG commit (md §3).
        val sag = SagClient.builder(appContext, BASE_URL, BuildConfig.SAG_BOOTSTRAP_SECRET)
            .baseClient(baseClient)
            .build()

        return Retrofit.Builder()
            .baseUrl(sag.baseUrl())
            .client(sag.okHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ThemesApi::class.java)
    }
}

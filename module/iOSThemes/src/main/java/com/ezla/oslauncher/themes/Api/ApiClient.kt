package com.ezla.oslauncher.themes.Api

import android.content.Context
import com.ezla.oslauncher.themes.BuildConfig
import com.ezla.oslauncher.themes.test.FileLogger
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
        api ?: synchronized(this) { api ?: buildLogged(context.applicationContext).also { api = it } }

    // Bọc build để FileLogger ghi được lỗi khởi tạo SagClient/Retrofit; vẫn ném lại nên hành vi không đổi.
    private fun buildLogged(appContext: Context): ThemesApi {
        FileLogger.init(appContext)
        FileLogger.d(TAG, "build start: baseUrl=$BASE_URL secretLength=${BuildConfig.SAG_BOOTSTRAP_SECRET.length}")
        return try {
            build(appContext).also { FileLogger.d(TAG, "build ok") }
        } catch (e: Throwable) {
            FileLogger.e(TAG, "build ✘ ${e.message}", e)
            throw e
        }
    }

    private const val TAG = "ApiClient"

    private fun build(appContext: Context): ThemesApi {
        ApiLog.init(appContext)
        // Chỉ ghi độ dài secret (không ghi giá trị) để biết bản release có được nhúng secret không.
        ApiLog.d("ApiClient build: baseUrl=$BASE_URL secretLength=${BuildConfig.SAG_BOOTSTRAP_SECRET.length}")

        // Base client CHỈ chứa application interceptor (md §6 cho phép; network interceptor bị cấm
        // vì đọc được JWT/signature).
        val baseClient = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // Gắn ở MỌI bản: ApiLog.d tự quyết logcat (chỉ debug) + luôn ghi file Download để đọc lỗi release.
            .addInterceptor(
                HttpLoggingInterceptor { ApiLog.d(it) }
                    .setLevel(HttpLoggingInterceptor.Level.BODY)
            )
            .build()

        // SagClient dùng chung: ở request đầu tự tạo device session + Key Attestation + ký (Bearer JWT,
        // X-Timestamp/Nonce/Signature). Secret lấy từ gradle.properties -> BuildConfig, KHÔNG commit (md §3).
        val sag = SagClient.builder(appContext, BASE_URL, BuildConfig.SAG_BOOTSTRAP_SECRET)
            .baseClient(baseClient)
            .build()

        return Retrofit.Builder()
            .baseUrl(sag.baseUrl())
            .client(withTokenLog(sag.okHttpClient()))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ThemesApi::class.java)
    }

    // CHỈ DEBUG: in Bearer token để test API khác (tag ThemesApi). Token chỉ có sau khi SAG ký
    // nên phải đặt network interceptor ở client của SAG; release không gắn nên không lộ token.
    private fun withTokenLog(client: OkHttpClient): OkHttpClient {
        if (!ApiLog.enabled) return client
        return client.newBuilder()
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                request.header("Authorization")?.let {
                    ApiLog.d("access_token [${request.method} ${request.url.encodedPath}] $it")
                }
                chain.proceed(request)
            }
            .build()
    }
}

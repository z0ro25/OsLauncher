package com.ezla.oslauncher.themes.Api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Retrofit dùng chung cho module Themes. Giữ applicationContext nên an toàn để là singleton.
object ApiClient {
    private const val BASE_URL = "https://launcher-os.eztechglobal.com/api/v1/"
    private const val TIMEOUT_SECONDS = 30L

    @Volatile
    private var api: ThemesApi? = null

    fun api(context: Context): ThemesApi =
        api ?: synchronized(this) { api ?: build(context.applicationContext).also { api = it } }

    private fun build(appContext: Context): ThemesApi {
        ApiLog.init(appContext)
        val client = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(appContext))
            .apply {
                // Logger riêng -> cùng tag ThemesApi (logger mặc định OkHttp in dưới tag khác/không ra).
                if (ApiLog.enabled) {
                    addInterceptor(
                        HttpLoggingInterceptor { ApiLog.d(it) }.setLevel(HttpLoggingInterceptor.Level.BODY)
                    )
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ThemesApi::class.java)
    }

    // Header chung theo Postman; có token thì gắn Bearer (add-device chạy khi chưa có token).
    private class AuthInterceptor(private val appContext: Context) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val builder = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
            TokenStore.get(appContext)?.let { builder.header("Authorization", "Bearer $it") }
            return chain.proceed(builder.build())
        }
    }
}

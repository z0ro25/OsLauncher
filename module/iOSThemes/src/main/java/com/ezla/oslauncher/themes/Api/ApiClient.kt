package com.ezla.oslauncher.themes.Api

import android.content.Context
import com.ezla.oslauncher.themes.BuildConfig
import com.mct.sag.SagClient
import com.truongnt.ios.ioslite.common.config.SharePrefUtils
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Client dùng chung của module Themes (md §3: tạo MỘT SagClient, không tạo mới cho từng request).
object ApiClient {
    // Base url KHÔNG fix cứng: lấy từ Remote Config domain_api do :app đẩy xuống, lưu prefs
    // để lần mở app sau dùng ngay khi chưa fetch kịp.
    private const val KEY_DOMAIN_API = "themes_domain_api"
    private const val TIMEOUT_SECONDS = 30L

    @Volatile
    private var api: ThemesApi? = null

    // Url mà `api` hiện tại đã build với; null khi chưa build.
    private var builtUrl: String? = null

    // Chưa có url hợp lệ thì ném lỗi; mọi repository gọi trong runCatching nên chỉ fail request đó.
    fun api(context: Context): ThemesApi =
        api ?: synchronized(this) {
            api ?: run {
                val ctx = context.applicationContext
                val url = normalize(SharePrefUtils.getString(ctx, KEY_DOMAIN_API, null))
                    ?: error("domain_api chưa có giá trị hợp lệ")
                build(ctx, url).also { api = it; builtUrl = url }
            }
        }

    // :app gọi lúc khởi động với default trong remote_config_defaults.xml; chỉ ghi khi prefs
    // còn trống để không đè url đã fetch ở phiên trước.
    fun applyDefaultDomain(context: Context, rawUrl: String?) {
        val url = normalize(rawUrl) ?: return
        synchronized(this) {
            val ctx = context.applicationContext
            if (normalize(SharePrefUtils.getString(ctx, KEY_DOMAIN_API, null)) == null) {
                SharePrefUtils.putString(ctx, KEY_DOMAIN_API, url)
            }
        }
    }

    // :app gọi sau khi fetch Remote Config. Giá trị sai thì bỏ qua, giữ url đang dùng; url khác
    // url đang dùng thì bỏ client cũ, request kế tiếp build lại (SagClient tạo session mới).
    fun applyDomain(context: Context, rawUrl: String?) {
        val url = normalize(rawUrl) ?: return
        synchronized(this) {
            SharePrefUtils.putString(context.applicationContext, KEY_DOMAIN_API, url)
            if (builtUrl != null && builtUrl != url) {
                api = null
                builtUrl = null
            }
        }
    }

    // Rỗng/không phải http(s) -> null; thiếu "/" cuối thì thêm vì Retrofit sẽ throw.
    private fun normalize(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.toHttpUrlOrNull() == null) return null
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    private fun build(appContext: Context, baseUrl: String): ThemesApi {
        // Base client CHỈ chứa application interceptor (md §6 cho phép; network interceptor bị cấm
        // vì đọc được JWT/signature; ngoại lệ duy nhất là ApiLog, chỉ gắn ở bản debug).
        val baseClient = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        // SagClient dùng chung: ở request đầu tự tạo device session + Key Attestation + ký (Bearer JWT,
        // X-Timestamp/Nonce/Signature). Secret lấy từ gradle.properties -> BuildConfig, KHÔNG commit (md §3).
        val sag = SagClient.builder(appContext, baseUrl, BuildConfig.SAG_BOOTSTRAP_SECRET)
            .baseClient(baseClient)
            .build()

        return Retrofit.Builder()
            .baseUrl(sag.baseUrl())
            .client(ApiLog.attach(sag.okHttpClient()))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ThemesApi::class.java)
    }
}

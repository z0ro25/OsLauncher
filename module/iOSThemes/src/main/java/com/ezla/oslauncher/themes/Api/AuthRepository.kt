package com.ezla.oslauncher.themes.Api

import com.ezla.oslauncher.themes.Models.AddDeviceRequest
import com.ezla.oslauncher.themes.Models.AddDeviceData
import android.content.Context
import com.ezla.oslauncher.themes.utils.EncryptionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Đăng ký thiết bị với server và lưu access_token. Gọi mỗi lần vào luồng Themes ở SplashActivity (:app).
class AuthRepository(context: Context) {

    private val appContext = context.applicationContext
    private val api = ApiClient.api(appContext)

    fun hasToken(): Boolean = TokenStore.get(appContext) != null

    // Thành công thì lưu token và trả Result.success(data); mọi lỗi (mạng/HTTP/parse) gói vào Result.failure.
    suspend fun addDevice(): Result<AddDeviceData> = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        ApiLog.d("add-device ▶ payload (trước mã hoá) = ${EncryptionUtil.describePayload(appContext)}")
        runCatching {
            val secret = EncryptionUtil.encrypt(appContext)
            ApiLog.d("add-device ▶ secret = $secret")
            val response = api.addDevice(AddDeviceRequest(secret))
            val body = response.body()
            val data = body?.data
            if (!response.isSuccessful || data == null) {
                // errorBody chỉ đọc được 1 lần -> đọc ngay để log nội dung server trả về khi lỗi.
                val errorBody = response.errorBody()?.string()
                error("add-device failed: http=${response.code()} status=${body?.status} error=$errorBody")
            }
            data.accessToken?.takeIf { it.isNotEmpty() }?.let { TokenStore.save(appContext, it) }
            data
        }.onSuccess {
            ApiLog.d(
                "add-device ✔ ${System.currentTimeMillis() - startMs}ms device=${it.device}" +
                    " token=${if (it.accessToken.isNullOrEmpty()) "<rỗng>" else "đã lưu"}"
            )
        }.onFailure {
            ApiLog.e("add-device ✘ ${System.currentTimeMillis() - startMs}ms ${it.message}", it)
        }
    }

    // Gọi add-device chỉ khi chưa có token; đã có thì bỏ qua (không tốn request).
    suspend fun ensureToken(): Result<Unit> =
        if (hasToken()) Result.success(Unit) else addDevice().map { }
}

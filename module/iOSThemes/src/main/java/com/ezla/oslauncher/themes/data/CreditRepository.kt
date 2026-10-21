package com.ezla.oslauncher.themes.data

import com.ezla.oslauncher.themes.Api.ApiClient
import com.ezla.oslauncher.themes.Api.ThemesApi
import com.ezla.oslauncher.themes.Models.ApiResponse
import com.ezla.oslauncher.themes.Models.Credit
import com.ezla.oslauncher.themes.Models.CreditData
import com.ezla.oslauncher.themes.Models.CreditRequest
import com.ezla.oslauncher.themes.Models.Unlock
import com.ezla.oslauncher.themes.utils.EncryptionUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.truongnt.ios.ioslite.common.CommonSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

// Server đã trả lời nhưng từ chối ({"data":{"error":...}}, vd thiếu xu), khác lỗi mạng/SAG.
class CreditRejectedException(message: String) : Exception(message)

// Số dư xu lấy từ server qua SAG (không lưu local). Mọi hàm trả số dư MỚI sau thao tác.
object CreditRepository {

    const val UNLOCK_THEME_ICONS = "theme_icons"
    const val UNLOCK_BACKGROUND = "background"

    private const val STATUS_SUCCESS = "Success"
    private const val STATUS_UNLOCKED = "unlocked"
    private val errorType = object : TypeToken<ApiResponse<CreditData>>() {}.type

    suspend fun getCredit(): Result<Int> = call("credits") { it.getCredits() }

    // Mở khoá icon (id = theme) hoặc nền (id = background); server tự trừ xu, trả số dư mới.
    // Thiếu xu -> CreditRejectedException.
    suspend fun unlock(type: String, unlockableId: Int): Result<Int> =
        call("unlocks $type#$unlockableId", STATUS_UNLOCKED) {
            it.unlock(CreditRequest(EncryptionUtil.encryptUnlock(
                Unlock(type, unlockableId, System.currentTimeMillis() / 1000)
            )))
        }

    // Cộng xu (vd type "view_ads"). Để sẵn, chưa gắn vào màn nào.
    suspend fun addCredit(amount: Int, type: String): Result<Int> =
        call("credits add") { it.addCredits(body(amount, type)) }

    // Trừ xu. Để sẵn, chưa gắn: mở khoá theme/nền dùng unlock() ở trên.
    suspend fun consumeCredit(amount: Int, type: String): Result<Int> =
        call("credits consume") { it.consumeCredits(body(amount, type)) }

    // Mỗi lần gọi mã hoá lại với time mới (epoch GIÂY) -> không gửi lại payload cũ.
    private fun body(amount: Int, type: String) = CreditRequest(
        EncryptionUtil.encryptCredit(Credit(amount, type, System.currentTimeMillis() / 1000))
    )

    private suspend fun call(
        name: String,
        okStatus: String = STATUS_SUCCESS,
        block: suspend (ThemesApi) -> Response<ApiResponse<CreditData>>
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val ctx = CommonSdk.getApplicationContext() ?: error("$name: CommonSdk chưa init")
            val response = block(ApiClient.api(ctx))
            val data = response.body()?.data
            val credit = data?.credit
            // GET không có status; POST phải đúng okStatus ("Success" / "unlocked") mới coi là xong.
            val statusOk = data?.status == null || data.status.equals(okStatus, true)
            if (response.isSuccessful && credit != null && statusOk) return@runCatching credit
            val rawError = response.errorBody()?.string()
            val serverError = data?.error ?: parseError(rawError)
            val detail = "$name failed: http=${response.code()} status=${data?.status} error=${serverError ?: rawError}"
            if (serverError != null) throw CreditRejectedException(detail)
            error(detail)
        }
    }

    // errorBody (HTTP lỗi) cũng có dạng {"data":{"error":...}} -> đọc ra để phân loại.
    private fun parseError(raw: String?): String? = runCatching {
        Gson().fromJson<ApiResponse<CreditData>>(raw, errorType)?.data?.error
    }.getOrNull()
}

package com.ezla.oslauncher.themes.data

import android.content.Context
import com.ezla.oslauncher.themes.Api.ApiClient
import com.ezla.oslauncher.themes.Api.ApiLog
import com.ezla.oslauncher.themes.Models.CreditRequest
import com.ezla.oslauncher.themes.Models.SubsModel
import com.ezla.oslauncher.themes.utils.EncryptionUtil
import com.truongnt.ios.ioslite.common.util.PreferencesUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Trạng thái subs của module Themes. Billing ở :app gọi [onSubsState] mỗi khi Play báo trạng thái;
// module dùng [isSubscribed] để ẩn logic xu và đồng bộ lên server qua POST /device/sub.
object SubsRepository {

    private const val KEY_SUBSCRIBED = "themes_subs_active"
    // 1 = server đã nhận is_sub=1 cho gói hiện tại; khác 1 = chưa gửi được.
    private const val KEY_SYNCED = "themes_subs_synced"
    private const val STATUS_SUCCESS = "Success"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Mutex()

    fun isSubscribed(context: Context): Boolean =
        PreferencesUtil.getBoolean(context, KEY_SUBSCRIBED, false)

    // :app gọi mỗi lần billing check subs. CHỈ gọi /device/sub khi đang có gói, và chỉ 1 lần
    // (server nhận rồi thì thôi); không có gói thì không gọi, chỉ xoá cờ để lần mua lại gửi tiếp.
    fun onSubsState(context: Context, subscribed: Boolean) {
        val appContext = context.applicationContext
        PreferencesUtil.putBoolean(appContext, KEY_SUBSCRIBED, subscribed)
        if (!subscribed) {
            PreferencesUtil.putInt(appContext, KEY_SYNCED, -1)
            return
        }
        scope.launch { sync(appContext) }
    }

    private suspend fun sync(context: Context) = gate.withLock {
        val value = 1
        if (PreferencesUtil.getInt(context, KEY_SYNCED, -1) == value) return@withLock
        runCatching {
            val body = CreditRequest(
                EncryptionUtil.encryptSubs(SubsModel(value, System.currentTimeMillis() / 1000))
            )
            val response = ApiClient.api(context).syncSub(body)
            val data = response.body()?.data
            if (!response.isSuccessful || !STATUS_SUCCESS.equals(data?.status, true)) {
                error("device/sub failed: http=${response.code()} status=${data?.status} error=${data?.error ?: response.errorBody()?.string()}")
            }
            PreferencesUtil.putInt(context, KEY_SYNCED, value)
        }.onSuccess { ApiLog.d("device/sub ✔ is_sub=$value") }
            .onFailure { ApiLog.e("device/sub ✘ ${it.message}", it) }
    }
}

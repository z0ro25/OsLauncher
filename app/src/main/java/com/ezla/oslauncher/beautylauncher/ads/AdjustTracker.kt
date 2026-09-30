package com.ezla.oslauncher.beautylauncher.ads

import android.app.Application
import android.util.Log
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig

// Init Adjust (token là cấu hình của app nên nằm ở :app). Bất biến: không bao giờ ném ra
// ngoài — lỗi chỉ log, luồng khởi động app phải chạy tiếp.
object AdjustTracker {

    private const val TAG = "AdjustTracker"

    /** Token rỗng thì bỏ qua (chỉ log cảnh báo), app vẫn chạy bình thường. */
    @JvmStatic
    fun init(application: Application, token: String, isDebug: Boolean) {
        if (token.isBlank()) {
            Log.w(TAG, "Chưa có Adjust token — bỏ qua init Adjust.")
            return
        }
        try {
            val environment =
                if (isDebug) AdjustConfig.ENVIRONMENT_SANDBOX else AdjustConfig.ENVIRONMENT_PRODUCTION
            Adjust.initSdk(AdjustConfig(application, token, environment))
        } catch (t: Throwable) {
            Log.e(TAG, "init Adjust lỗi", t)
        }
    }
}

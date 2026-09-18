package com.truongnt.ios.ioslite.common.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.truongnt.fsd.nttads.FsdAds
import com.truongnt.ios.ioslite.common.BuildConfig

object Ads {

    private const val TAG = "Ads"

    /** Delay tối thiểu giữa hai lần hiện interstitial (ms). FSDAds tự áp. */
    const val DEFAULT_INTER_DELAY_MS = 20_000L

    @Volatile
    private var initialized = false

    @Volatile
    private var premium = false

    @Volatile
    private var tier1 = false

    @Volatile
    private var interDelayMs = DEFAULT_INTER_DELAY_MS

    /** Đã gọi [init] chưa. Các lớp con nên kiểm tra trước khi phát request. */
    @JvmStatic
    val isInitialized: Boolean
        get() = initialized

    // ── Cờ bật/tắt theo loại ─────────────────────────────────────────────
    // Đặt từ Remote Config ở :app. Tắt thì tầng `ads` chặn luôn, không phát request.

    @JvmField
    @Volatile
    var interstitialEnabled: Boolean = true

    @JvmField
    @Volatile
    var nativeEnabled: Boolean = true

    @JvmField
    @Volatile
    var bannerEnabled: Boolean = true

    @JvmField
    @Volatile
    var rewardEnabled: Boolean = true

    @JvmField
    @Volatile
    var openEnabled: Boolean = true

    /** Policy tần suất hiển thị. null = cho hiển thị mọi lần. */
    @JvmField
    @Volatile
    var policy: Policy? = null

    /** Policy tần suất — thay cho `AdDisplayHelper` cũ. */
    interface Policy {
        /** Có được hiển thị quảng cáo ở [slot] lúc này không. */
        fun shouldShow(slot: AdsSlot): Boolean

        /** Gọi sau khi quảng cáo đã thực sự hiển thị (để đếm/tính mốc lần sau). */
        fun onShown(slot: AdsSlot)
    }

    /**
     * Khởi tạo SDK. Gọi một lần trong `Application.onCreate()`.
     *
     * @param application  Application context.
     * @param adjustToken  Token Adjust. **Để rỗng thì bỏ qua init Adjust** (chỉ log cảnh báo)
     *                     — hạ tầng ads vẫn chạy bình thường, chỉ mất revenue tracking.
     * @param isDebug      true → Adjust chạy sandbox, false → production.
     * @param interDelayMs Delay giữa hai lần hiện interstitial.
     * @param onReady      Chạy khi MobileAds khởi tạo xong. Có thể null.
     *
     * [onReady] **luôn được gọi đúng một lần**, kể cả khi init lỗi — để không bao giờ
     * chặn luồng khởi động của app.
     */
    @JvmStatic
    @JvmOverloads
    fun init(
        application: Application,
        adjustToken: String = "",
        isDebug: Boolean = BuildConfig.DEBUG,
        interDelayMs: Long = DEFAULT_INTER_DELAY_MS,
        onReady: Runnable? = null,
    ) {
        if (initialized) {
            Log.w(TAG, "Ads.init() đã gọi trước đó — bỏ qua lần gọi thứ hai")
            onReady?.run()
            return
        }
        initialized = true
        this.interDelayMs = interDelayMs

        initAdjust(application, adjustToken, isDebug)

        // Phải chốt config TRƯỚC khi init, vì FSDAds giữ cờ premium theo giá trị tại
        // thời điểm setupConfig (xem Ads.setPremium).
        applyFsdConfig()

        try {
            FsdAds.init(application) { onReady?.run() }
        } catch (t: Throwable) {
            Log.e(TAG, "FsdAds.init lỗi", t)
            onReady?.run()
        }
    }

    /**
     * Cập nhật trạng thái premium. Gọi lại mỗi khi trạng thái mua hàng đổi.
     *
     * Lưu ý: FSDAds chốt cờ premium theo **giá trị** lúc gọi `setupConfig`, nên ở đây
     * phải gọi lại `setupConfig` mỗi lần thay đổi — không truyền được lambda sống.
     */
    @JvmStatic
    fun setPremium(isPremium: Boolean, isTier1: Boolean) {
        premium = isPremium
        tier1 = isTier1
        if (initialized) {
            applyFsdConfig()
        }
    }

    /** User có đang ở trạng thái không nên thấy quảng cáo không. */
    @JvmStatic
    fun isPremium(): Boolean = premium || tier1

    /**
     * Slot có được phép hiển thị quảng cáo không — xét premium, cờ loại, và policy.
     *
     * Dùng để quyết định có chừa chỗ cho ô quảng cáo hay không, TRƯỚC khi biết ad đã tải
     * xong chưa (việc tải là bất đồng bộ). Đây là bản public của [canUse] cho call-site Java.
     */
    @JvmStatic
    fun isSlotAllowed(slot: AdsSlot): Boolean = canUse(slot)

    /**
     * Dò `Activity` từ [context] — đi ngược chuỗi `ContextWrapper`.
     *
     * Nhiều API của SDK bắt buộc nhận `Activity`, mà view trong RecyclerView thường giữ
     * `ContextThemeWrapper` chứ không phải chính `Activity`. Đây là tiện ích dùng chung cho
     * mọi loại quảng cáo.
     */
    @JvmStatic
    fun findActivity(context: Context?): Activity? {
        var current = context
        while (current is ContextWrapper) {
            if (current is Activity) {
                return current
            }
            current = current.baseContext
        }
        return null
    }

    // ── Nội bộ, dùng chung cho các lớp Ads* cùng package ─────────────────

    private fun applyFsdConfig() {
        FsdAds.setupConfig(
            isSubs = premium,
            tier1 = tier1,
            interDelay = interDelayMs,
        )
    }

    private fun initAdjust(application: Application, token: String, isDebug: Boolean) {
        if (token.isBlank()) {
            Log.w(
                TAG,
                "Chưa có Adjust token — bỏ qua init Adjust. Điền token vào lời gọi " +
                    "Ads.init() ở BaseLauncherApplication.setupAdEnvironment().",
            )
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

    /** Slot này có được phép dùng quảng cáo lúc này không. */
    internal fun canUse(slot: AdsSlot): Boolean {
        if (isPremium()) {
            return false
        }
        if (!isTypeEnabled(slot)) {
            return false
        }
        return policy?.shouldShow(slot) ?: true
    }

    /** Ghi nhận đã hiển thị — để policy tính mốc cho lần sau. */
    internal fun markShown(slot: AdsSlot) {
        policy?.onShown(slot)
    }

    private fun isTypeEnabled(slot: AdsSlot): Boolean = when (slot) {
        AdsSlot.INTER_IN_APP, AdsSlot.INTER_SPLASH -> interstitialEnabled
        AdsSlot.APP_OPEN -> openEnabled
        AdsSlot.BANNER_IN_APP, AdsSlot.BANNER_COLLAPSE -> bannerEnabled
        AdsSlot.REWARD_IN_APP -> rewardEnabled
        // Toàn bộ slot NATIVE_* còn lại.
        else -> nativeEnabled
    }
}

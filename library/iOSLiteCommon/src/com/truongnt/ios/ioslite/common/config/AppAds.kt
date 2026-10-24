package com.truongnt.ios.ioslite.common.config

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.MainThread
import androidx.fragment.app.FragmentActivity
import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk
import com.ezt.v2.ezt.admobdemo.ads.placement.AdFullScreenInfo
import com.ezt.v2.ezt.admobdemo.ads.placement.AdPreloadState
import com.ezt.v2.ezt.admobdemo.ads.placement.AdReward
import com.ezt.v2.ezt.admobdemo.ads.placement.AdRewardedCallback
import com.ezt.v2.ezt.admobdemo.ads.placement.AdsConfigSource
import com.ezt.v2.ezt.admobdemo.ads.placement.AdsConfiguration
import com.ezt.v2.ezt.admobdemo.ads.placement.AdsKit
import com.ezt.v2.ezt.admobdemo.ads.placement.AssetAdsConfigSource
import com.ezt.v2.ezt.admobdemo.ads.placement.InlineAdBinding
import com.ezt.v2.ezt.admobdemo.ads.placement.StartupAdsConfig
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.truongnt.ios.ioslite.common.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

// Đầu mối AdsKit duy nhất cho mọi module. Ở library để :module:* dùng được; phần fetch Firebase
// vẫn nằm ở :app và truyền vào qua fetchRemote (library không được phụ thuộc Firebase).
object AppAds {
    private const val TAG = "AppAds"
    private const val CACHE_KEY = "validated_json"

    // Scope của app: xoay màn hay huỷ Splash không làm init chạy lại.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var initialization: Deferred<Unit>? = null
    @Volatile private var activeJson: String? = null

    /** Chỉ dùng khi [isInitialized] = true. */
    lateinit var kit: AdsKit
        private set
    lateinit var configuration: AdsConfiguration
        private set
    @Volatile
    @JvmStatic
    var isInitialized = false
        private set

    // Splash gọi, có fetch Firebase. Gọi lại (kể cả sau initializeOffline) chỉ chờ lần đang chạy.
    @MainThread
    suspend fun initialize(context: Context, fetchRemote: suspend () -> String): AdsKit {
        start(context.applicationContext, fetchRemote)
        initialization!!.await()
        return kit
    }

    // Launcher mở thẳng từ nút Home (không qua Splash): init bằng cache/asset, không chờ Firebase.
    @MainThread
    @JvmStatic
    fun initializeOffline(context: Context) {
        if (initialization == null) offlineOnly = true
        start(context.applicationContext) { error("offline init: bỏ qua remote") }
    }

    // true khi lần init của process này đi đường offline -> remote chưa được fetch lần nào.
    private var offlineOnly = false

    // Như initialize, nhưng process đã init offline trước (Home/màn khôi phục) thì vẫn fetch remote
    // rồi applyConfig đè lên; fetch lỗi/quá REMOTE_REFRESH_MS thì giữ config đang chạy.
    @MainThread
    suspend fun initializeRemote(context: Context, fetchRemote: suspend () -> String): AdsKit {
        val app = context.applicationContext
        start(app, fetchRemote)
        initialization!!.await()
        if (!offlineOnly) return kit
        offlineOnly = false
        try {
            val json = withTimeout(REMOTE_REFRESH_MS) { fetchRemote() }
            kit.applyConfig(json)
            activeJson = json
            withContext(Dispatchers.IO) { prefs(app).edit().putString(CACHE_KEY, json).commit() }
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            Log.w(TAG, "Refresh remote sau offline init lỗi, giữ config hiện tại", e)
        }
        return kit
    }

    private const val REMOTE_REFRESH_MS = 5_000L

    // Tách cache debug/release để JSON test không lẫn vào bản production.
    private fun prefs(app: Context) = app.getSharedPreferences(
        if (BuildConfig.DEBUG) "startup_ads_debug" else "startup_ads_release", Context.MODE_PRIVATE)

    // Launcher mở thẳng (bỏ qua Splash): UMP buộc requestConsentInfoUpdate mỗi phiên, trước đó
    // canRequestAds() = false nên SDK chặn hết ads. Không hiện form ở đây; chưa có consent thì chờ Splash.
    @MainThread
    @JvmStatic
    fun prepareWithoutForm(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            if (info.canRequestAds()) AdsSdk.initialize(activity.applicationContext)
        }, { error ->
            Log.w(TAG, "Consent update lỗi ${error.errorCode}: ${error.message}")
            // Lỗi mạng: UMP vẫn giữ trạng thái phiên trước, đã đồng ý thì vẫn cho init.
            if (info.canRequestAds()) AdsSdk.initialize(activity.applicationContext)
        })
    }

    private fun start(app: Context, fetchRemote: suspend () -> String) {
        if (initialization != null) return
        val prefs = prefs(app)
        kit = AdsKit.Builder(app)
            .manageHelperAds()
            // Ai gọi reloadConfig nhầm thì chỉ đọc lại JSON đã chọn, không load asset đè remote.
            .configSource(AdsConfigSource { checkNotNull(activeJson) { "Initialize ads first" } })
            .build()
        initialization = scope.async {
            val startup = StartupAdsConfig(
                fetchRemote = { fetchRemote() },
                readCached = { prefs.getString(CACHE_KEY, null) },
                readFallback = {
                    val asset = if (BuildConfig.DEBUG) "ads/app-placements.json" else "ads/placements.json"
                    AssetAdsConfigSource(app, asset).read()
                },
                apply = { json ->
                    kit.applyConfig(json)
                    activeJson = json
                },
                saveRemote = { json ->
                    check(prefs.edit().putString(CACHE_KEY, json).commit()) { "Cannot save validated ads config" }
                },
                onError = { Log.w(TAG, "Startup configuration source unavailable", it) },
            )
            configuration = withContext(Dispatchers.IO) { startup.initialize() }
            isInitialized = true
            Log.d(TAG, "Configuration ready: revision=${configuration.revision}")
        }
    }

    /** Xoá cache fullscreen/rewarded; không đụng JSON, banner/native hay App Open observer. */
    @MainThread
    suspend fun clearPreloads() {
        if (isInitialized) {
            kit.clearFullscreenPreloads()
            kit.clearRewardedPreloads()
        }
    }

    // kit có ngay khi bắt đầu init; native của SDK tự chờ config nên gọi show lúc này vẫn được.
    @JvmStatic
    fun hasKit(): Boolean = ::kit.isInitialized

    // Native inline qua placement [key]. onResult(true) khi READY, false khi FAILED/DISABLED hoặc
    // chưa có kit. shimmerResId != 0 thì hiện skeleton tới khi ad vào, tối thiểu SHIMMER_MIN_MS.
    @JvmStatic
    @JvmOverloads
    fun showNative(
        activity: FragmentActivity,
        key: String,
        container: FrameLayout,
        layoutResId: Int,
        shimmerResId: Int = 0,
        onResult: NativeResult?,
    ): InlineAdBinding? = bindNative(activity, key, container, layoutResId, shimmerResId, false, onResult)

    // Như showNative nhưng native lấp đầy container cao cố định. SDK gắn host trung gian với
    // LayoutParams cứng MATCH_PARENT x WRAP_CONTENT (bindInlineAd$attachHost) -> ép lại chiều cao.
    @JvmStatic
    @JvmOverloads
    fun showNativeFill(
        activity: FragmentActivity,
        key: String,
        container: FrameLayout,
        layoutResId: Int,
        shimmerResId: Int = 0,
        onResult: NativeResult?,
    ): InlineAdBinding? = bindNative(activity, key, container, layoutResId, shimmerResId, true, onResult)

    private const val SHIMMER_MIN_MS = 1000L
    private const val SHIMMER_SAFETY_MS = 3000L

    private fun bindNative(
        activity: FragmentActivity,
        key: String,
        container: FrameLayout,
        layoutResId: Int,
        shimmerResId: Int,
        fill: Boolean,
        onResult: NativeResult?,
    ): InlineAdBinding? {
        if (!hasKit()) {
            onResult?.onResult(false)
            return null
        }
        // Tạo skeleton TRƯỚC khi gọi SDK: có ad preload sẵn thì SDK gắn view ad ngay trong lời gọi,
        // lúc đó shimmer đã != null nên onChildViewAdded biết mà huỷ, không để skeleton đè lên ad.
        var shimmer: View? = if (shimmerResId != 0) {
            LayoutInflater.from(container.context).inflate(shimmerResId, container, false)
        } else {
            null
        }
        var adAttached = false
        val shownAt = SystemClock.uptimeMillis()
        // Ad về quá nhanh (nhất là ad test) vẫn giữ skeleton đủ SHIMMER_MIN_MS cho mắt kịp thấy.
        val hideShimmer = { immediate: Boolean ->
            shimmer?.let { s ->
                shimmer = null
                val wait = if (immediate) 0L else shownAt + SHIMMER_MIN_MS - SystemClock.uptimeMillis()
                container.postDelayed({ (s.parent as? ViewGroup)?.removeView(s) }, wait.coerceAtLeast(0L))
            }
        }
        if (fill || shimmerResId != 0) {
            container.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View, child: View) {
                    if (child === shimmer) return
                    adAttached = true
                    if (fill) {
                        val lp = child.layoutParams
                        if (lp != null && lp.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                            lp.height = ViewGroup.LayoutParams.MATCH_PARENT
                            child.layoutParams = lp
                        }
                    }
                    hideShimmer(false)
                }

                override fun onChildViewRemoved(parent: View, child: View) = Unit
            })
        }
        // Gọi SDK TRƯỚC: nó removeAllViews + GONE container ngay trong lời gọi này.
        val binding = kit.showNativeInline(activity, key, container, layoutResId) { state ->
            when (state) {
                AdPreloadState.READY -> {
                    // SDK báo READY lúc ad TẢI xong, gắn view (attachHost) là bước sau -> skeleton
                    // gỡ ở onChildViewAdded; đây chỉ là chốt an toàn nếu view không bao giờ được gắn.
                    container.postDelayed({ hideShimmer(true) }, SHIMMER_SAFETY_MS)
                    onResult?.onResult(true)
                }
                AdPreloadState.FAILED, AdPreloadState.DISABLED -> {
                    hideShimmer(true)
                    onResult?.onResult(false)
                }
                else -> Unit
            }
        }
        // shimmer == null nghĩa là SDK đã có kết quả ngay trong lời gọi trên (ad gắn sẵn / lỗi).
        val s = shimmer
        if (s != null && !adAttached) {
            // Z > 0 để skeleton luôn vẽ đè lên ad trong lúc giữ tối thiểu; bỏ outline để khỏi đổ bóng.
            s.outlineProvider = null
            s.translationZ = 1f
            container.addView(s)
            container.visibility = View.VISIBLE
        } else {
            shimmer = null
        }
        return binding
    }

    @JvmStatic
    fun preloadNative(key: String) {
        if (hasKit()) kit.preloadNativeInline(key)
    }

    fun interface NativeResult {
        fun onResult(hasAd: Boolean)
    }

    // ── Cho call-site Java: các hàm fullscreen của AdsKit là suspend ──

    @JvmStatic
    fun isFullScreenReady(key: String): Boolean =
        hasKit() && kit.fullscreenPreloadStates.value[key] == AdPreloadState.READY

    @JvmStatic
    fun preloadFullScreen(key: String) {
        if (!hasKit()) return
        scope.launch { runCatching { kit.preloadAdFullScreen(key) } }
    }

    // Bất biến: onDone chạy đúng 1 lần sau khi ad đóng, lỗi, hay không có gì để hiện.
    @JvmStatic
    fun showFullScreen(activity: FragmentActivity, key: String, onDone: Runnable) {
        if (!hasKit()) {
            onDone.run()
            return
        }
        scope.launch {
            try {
                kit.showAdFullScreen(activity, key)
            } catch (t: Throwable) {
                Log.w(TAG, "showAdFullScreen lỗi key=$key", t)
            } finally {
                onDone.run()
            }
        }
    }

    @JvmStatic
    fun preloadRewarded(key: String) {
        if (!hasKit()) return
        scope.launch { runCatching { kit.preloadRewarded(key) } }
    }

    // Rewarded qua placement [key]. onDone(true) chỉ khi SDK báo onEarnedReward (xem đủ); đóng sớm,
    // không có ad hay chưa có kit -> false. onDone chạy đúng 1 lần, trên main.
    @JvmStatic
    fun showRewarded(activity: FragmentActivity, key: String, onDone: (earned: Boolean) -> Unit) {
        if (!hasKit()) {
            onDone(false)
            return
        }
        scope.launch {
            var earned = false
            try {
                kit.showRewarded(activity, key, object : AdRewardedCallback {
                    override fun onEarnedReward(info: AdFullScreenInfo, reward: AdReward) {
                        earned = true
                    }
                })
            } catch (t: Throwable) {
                Log.w(TAG, "showRewarded lỗi key=$key", t)
            } finally {
                onDone(earned)
            }
        }
    }
}

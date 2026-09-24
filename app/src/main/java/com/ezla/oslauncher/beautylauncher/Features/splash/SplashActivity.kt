package com.ezla.oslauncher.beautylauncher.Features.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.addCallback
import com.ezla.oslauncher.beautylauncher.Base.BaseActivity
import com.ezla.oslauncher.beautylauncher.Features.home.HomeActivity
import com.ezla.oslauncher.beautylauncher.Features.languageStart.LanguageStartActivity
import com.ezla.oslauncher.beautylauncher.databinding.ActivitySplashBinding
import com.ezla.oslauncher.beautylauncher.extensions.haveNetworkConnection
import com.ezla.oslauncher.beautylauncher.extensions.hideNavigation
import com.ezla.oslauncher.beautylauncher.utils.EventTrackingHelper
import com.ezla.oslauncher.beautylauncher.utils.GDPRRequestable
import com.ezla.oslauncher.beautylauncher.utils.RemoteConfigUtils
import com.truongnt.ios.ioslite.common.ads.AdsInterstitial
import com.truongnt.ios.ioslite.common.ads.AdsNative
import com.truongnt.ios.ioslite.common.ads.AdsNativeCallback
import com.truongnt.ios.ioslite.common.ads.AdsSlot
import com.truongnt.ios.ioslite.common.config.RemoteConfigs
import com.truongnt.ios.ioslite.common.config.SharePrefUtils

class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    override val setViewBinding: ActivitySplashBinding
        get() = ActivitySplashBinding.inflate(layoutInflater)

    private val mainHandler = Handler(Looper.getMainLooper())

    /** Splash đã hiển thị đủ [MIN_SPLASH_MS] chưa. */
    private var minSplashElapsed = false

    /** Luồng ad ở splash đã kết thúc chưa (ad đóng / không có ad / lỗi / quá hạn). */
    private var adFlowDone = false

    /** Đã rời splash chưa — chốt để [goNext] chạy đúng một lần. */
    private var navigated = false

    private val minSplashRunnable = Runnable {
        minSplashElapsed = true
        goNextIfReady()
    }

    private val adTimeoutRunnable = Runnable {
        Log.w(TAG, "quá ${AD_TIMEOUT_MS}ms luồng ad vẫn chưa xong — đi tiếp để không treo splash")
        adFlowDone = true
        goNextIfReady()
    }

    override fun initView() {
        if (!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intent.action != null && intent.action.equals(
                Intent.ACTION_MAIN
            )
        ) {
            finish()
            return
        }
        window?.hideNavigation()

        EventTrackingHelper.logEvent(this, "splash_open")

        onBackPressedDispatcher.addCallback {

        }

        // Mốc thời gian splash tối thiểu — CHỈ bật cờ, KHÔNG tự đi tiếp. Trước đây chỗ này
        // postDelayed thẳng goNext() nên thành trần cứng: hết 3s là finish() SplashActivity
        // trong lúc interstitial đang mở, khiến ad vừa hiện đã bị đóng.
        // Chốt chặn cuối: quá hạn mà luồng ad vẫn không có kết quả (SDK không callback,
        // mạng treo) thì đi tiếp, tuyệt đối không để splash treo vô hạn.
        mainHandler.postDelayed(adTimeoutRunnable, AD_TIMEOUT_MS)

        requestUmp {
            // Watchdog đã cho đi tiếp rồi thì không show ad nữa: showInter trên Activity
            // đang finish sẽ thất bại và tốn mất lượt ad của slot.
            if (navigated || isFinishing || isDestroyed) return@requestUmp

            // Remote Config tắt interstitial splash -> không gọi showSplash, nhưng VẪN phải
            // nhả vế "luồng ad đã xong", nếu không splash ngồi chờ hết AD_TIMEOUT_MS mới đi tiếp.
            if (!RemoteConfigs.isAdsEnabled(this, RemoteConfigs.INTER_SPLASH)) {
                adFlowDone = true
                goNextIfReady()
                return@requestUmp
            }

            AdsInterstitial.showSplash(
                this,
                AdsSlot.INTER_SPLASH
            ) {
                // Tải trước interstitial in-app cho các lần bấm ở HomeActivity — chỉ khi vị
                // trí đó còn bật.
                if (RemoteConfigs.isAdsEnabled(this, RemoteConfigs.INTER_INAPP)) {
                    AdsInterstitial.load(this, AdsSlot.INTER_IN_APP) { }
                }
                // Ad đã đóng (hoặc không có ad / lỗi) — nhả vế thứ hai của điều kiện đi tiếp.
                adFlowDone = true
                goNextIfReady()
            }
        }
    }

    override fun viewListener() {}
    override fun dataObservable() {}

    private fun requestUmp(action: () -> Unit) {
        Log.e("ạkshdfkjhasdjkfk", "ump")

        GDPRRequestable.getGdprRequestable(this).setOnRequestGDPRCompleted {
            initRemoteConfig {
                initAds {
                    action.invoke()
                }
            }
        }
        GDPRRequestable.getGdprRequestable(this).requestGDPR()
    }


    private fun initRemoteConfig(action: () -> Unit) {
        Log.e("ạkshdfkjhasdjkfk", "remote")
        RemoteConfigUtils.initRemoteConfig { result ->
            if (haveNetworkConnection()) {
                if (result.isSuccessful) {
                    Log.e("ạkshdfkjhasdjkfk", "success")
                    val update = result.result as Boolean
                    if (update) {
                        Log.e("ạkshdfkjhasdjkfk", "update")
                        // Lưu TOÀN BỘ key khai ở RemoteConfigs xuống SharedPreferences để cả
                        // :app lẫn các module library đọc lại qua RemoteConfigs.get*().
                        // Mọi key ở đây đều là cờ bật/tắt (Boolean) — TRỪ daily_noti_time.
                        RemoteConfigUtils.apply {
                            // Công tắc tổng + interstitial
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.ADS_ENABLE)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.INTER_SPLASH)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.INTER_INAPP)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.INTER_OPEN_APP)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.INTER_NATIVE_FULL)
                            saveRemoteConfigInt(this@SplashActivity, RemoteConfigs.INTER_INTERVAL)

                            // Native
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.NATIVE_LANG1)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.NATIVE_LANG2)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.NATIVE_ONB1)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.NATIVE_ONB2)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.NATIVE_ONB3)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.NATIVE_ONB4)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.NATIVE_ONB_FULL)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.NATIVE_CLP)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.NATIVE_PERMISSION)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.NATIVE_APP_LIBRARY)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.NATIVE_APP_SEARCH)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.NATIVE_LEFT_PAGE)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.NATIVE_FULL)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.NATIVE_INAPP)
                            saveRemoteConfigBoolean(
                                this@SplashActivity,
                                RemoteConfigs.NATIVE_THEME_CLUB
                            )

                            // Reward
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.REWARD_INAPP)

                            // Banner
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.BANNER_CLP)
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.BANNER_INAPP)

                            // App-open khi quay lại app
                            saveRemoteConfigBoolean(this@SplashActivity, RemoteConfigs.RESUME)

                            // daily_noti_time là CHUỖI mốc giờ ("08:00"), không phải cờ bật/tắt —
                            // lưu bằng hàm String, lưu nhầm hàm Boolean sẽ ném lỗi lúc đọc.
                            saveRemoteConfigString(this@SplashActivity, RemoteConfigs.daily_noti_time)
                        }
                    }
                }
            }

            action.invoke()
        }
    }


    private fun initAds(action: () -> Unit) {
        // Remote Config tắt interstitial splash -> KHÔNG phát request nào, đi tiếp ngay.
        if (!RemoteConfigs.isAdsEnabled(this, RemoteConfigs.INTER_SPLASH)) {
            action.invoke()
            return
        }
        AdsInterstitial.preloadSplash(this, AdsSlot.INTER_SPLASH) {
            // Load đã có kết quả (được ad hay không) nên hết giai đoạn cần watchdog.
            // Huỷ ở đây để watchdog không cắt ngang lúc ad đang hiển thị.
            mainHandler.removeCallbacks(adTimeoutRunnable)
            if (!SharePrefUtils.getBoolean(this, "PERMISSION_SHOWED", false)) {
                // Mỗi vị trí native có cờ Remote Config riêng — tắt thì không tải trước,
                // đỡ một request thừa lên AdMob.
                if (RemoteConfigs.isAdsEnabled(this, RemoteConfigs.NATIVE_LANG1)) {
                    AdsNative.preload(this, AdsSlot.NATIVE_LANGUAGE_1, object : AdsNativeCallback() {

                    })
                }

                if (RemoteConfigs.isAdsEnabled(this, RemoteConfigs.NATIVE_LANG2)) {
                    AdsNative.preload(this, AdsSlot.NATIVE_LANGUAGE_2, object : AdsNativeCallback() {

                    })
                }
            }
            action.invoke()
        }
    }

    /**
     * Rời splash khi đã hội đủ hai vế: splash hiển thị đủ lâu VÀ luồng ad đã xong.
     *
     * Hai vế đến từ hai luồng bất đồng bộ độc lập, theo thứ tự bất kỳ, nên phải gộp lại ở
     * đây. Không được để luồng nào tự ý gọi [goNext] — đó chính là lỗi cũ: timer 3s gọi
     * thẳng goNext() nên SplashActivity bị finish ngay khi interstitial vừa hiện.
     */
    private fun goNextIfReady() {
        if (!adFlowDone) return
        goNext()
    }

    private fun goNext() {
        if (navigated || isFinishing) return
        navigated = true
        if (SharePrefUtils.getBoolean(this, "PERMISSION_SHOWED", false)) {
            showActivity(HomeActivity::class.java, null)
        } else {
            val bundle = Bundle()
            bundle.putString("screen", "SplashActivity")
            showActivity(LanguageStartActivity::class.java, bundle)
        }
        finish()
    }

    override fun onDestroy() {
        // Huỷ hết callback đang chờ để không giữ Activity và không goNext() sau khi màn chết.
        mainHandler.removeCallbacks(minSplashRunnable)
        mainHandler.removeCallbacks(adTimeoutRunnable)
        super.onDestroy()
    }

    private companion object {
        const val TAG = "SplashActivity"

        /** Trần chờ luồng ad ở splash: quá hạn thì đi tiếp thay vì treo màn (ms). */
        const val AD_TIMEOUT_MS = 60_000L
    }
}

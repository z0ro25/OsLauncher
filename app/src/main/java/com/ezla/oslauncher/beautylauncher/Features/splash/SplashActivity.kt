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
import com.ezla.oslauncher.beautylauncher.tool.sharePreferenceTool.SharePrefUtils
import com.ezla.oslauncher.beautylauncher.utils.EventTrackingHelper
import com.ezla.oslauncher.beautylauncher.utils.GDPRRequestable
import com.ezla.oslauncher.beautylauncher.utils.RemoteConfigUtils
import com.truongnt.ios.ioslite.common.ads.AdsInterstitial
import com.truongnt.ios.ioslite.common.ads.AdsSlot

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
            AdsInterstitial.showSplash(
                this,
                AdsSlot.INTER_SPLASH
            ) {
                AdsInterstitial.load(this, AdsSlot.INTER_IN_APP) { }
                // Ad đã đóng (hoặc không có ad / lỗi) — nhả vế thứ hai của điều kiện đi tiếp.
                adFlowDone = true
                goNextIfReady()
            }
        }
    }

    override fun viewListener() {

    }

    override fun dataObservable() {

    }

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
                        RemoteConfigUtils.apply {

                        }
                    }
                }
            }

            action.invoke()
        }
    }


    private fun initAds(action: () -> Unit) {
        AdsInterstitial.preloadSplash(this, AdsSlot.INTER_SPLASH) {
            // Load đã có kết quả (được ad hay không) nên hết giai đoạn cần watchdog.
            // Huỷ ở đây để watchdog không cắt ngang lúc ad đang hiển thị.
            mainHandler.removeCallbacks(adTimeoutRunnable)
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

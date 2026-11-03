package com.ezla.oslauncher.controlcenter

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk
import com.truongnt.ios.ioslite.common.config.AppAds

// Interstitial khi chuyển màn trong module (giống ThemesInter, dùng chung placement "inter_inapp").
// Bất biến: [action] luôn chạy đúng 1 lần, không bao giờ chặn thao tác để chờ tải ad.
object CcInter {

    private const val TAG = "CcInter"
    private const val PLACEMENT = "inter_inapp"

    fun preload() {
        if (AdsSdk.isAdFree || !AppAds.hasKit() || AppAds.isFullScreenReady(PLACEMENT)) return
        AppAds.preloadFullScreen(PLACEMENT)
    }

    fun run(activity: FragmentActivity?, action: () -> Unit) {
        var fired = false
        val finish = Runnable {
            if (fired) return@Runnable
            fired = true
            preload()
            action()
        }
        try {
            if (activity == null || AdsSdk.isAdFree || !AppAds.hasKit() || !AppAds.isFullScreenReady(PLACEMENT)) {
                finish.run()
                return
            }
            AppAds.showFullScreen(activity, PLACEMENT, finish)
        } catch (t: Throwable) {
            Log.e(TAG, "lỗi khi hiển thị interstitial", t)
            finish.run()
        }
    }
}

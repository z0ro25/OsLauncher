package com.ezla.oslauncher.themes.features

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk
import com.truongnt.ios.ioslite.common.config.AppAds

// Interstitial của luồng Themes (placement "inter_themes"): bấm thẻ ở home + nút Apply ở màn detail.
// Bất biến: [action] luôn chạy đúng 1 lần — user subs / chưa có ad sẵn / lỗi thì chạy ngay, có ad
// thì chạy khi ad đóng. Không bao giờ chặn thao tác của user để chờ tải ad.
object ThemesInter {

    private const val TAG = "ThemesInter"
    const val PLACEMENT = "inter_themes"

    fun preload() {
        if (AdsSdk.isAdFree || AppAds.isFullScreenReady(PLACEMENT)) return
        AppAds.preloadFullScreen(PLACEMENT)
    }

    fun run(activity: FragmentActivity?, action: () -> Unit) {
        var fired = false
        val finish = Runnable {
            if (fired) return@Runnable
            fired = true
            // Ad vừa dùng (hoặc chưa có) -> tải sẵn cho lần bấm sau.
            preload()
            action()
        }
        try {
            if (activity == null || AdsSdk.isAdFree) {
                finish.run()
                return
            }
            if (!AppAds.isFullScreenReady(PLACEMENT)) {
                // Chưa có ad -> đi thẳng (finish tự tải trước cho lần bấm sau).
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

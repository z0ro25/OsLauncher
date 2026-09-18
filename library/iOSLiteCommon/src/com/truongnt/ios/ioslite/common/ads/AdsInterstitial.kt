package com.truongnt.ios.ioslite.common.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.truongnt.fsd.nttads.FsdAds
import com.truongnt.fsd.nttads.InterAdsCallBack

object AdsInterstitial {

    private const val TAG = "AdsInterstitial"

    /** Interstitial đã tải xong, chờ hiển thị — khoá theo slot. */
    private val loaded = HashMap<AdsSlot, InterstitialAd>()

    /**
     * Tải trước interstitial cho màn splash.
     *
     * KHÔNG dùng `FsdAds.loadInterSplash` — API đó cất ad vào `mInterstitialAd`, một biến
     * DUY NHẤT mà FSDAds dùng chung cho mọi ad unit interstitial, và `showInter` không xoá
     * nó sau khi ad đóng. Kết quả: ad in-app còn nằm lại đó, `initInterSplash` thấy
     * `mInterstitialAd != null` nên lần sau KHÔNG load nữa, rồi `showAdsSplash` đem ad đã
     * dùng ra show và thất bại → splash không còn quảng cáo từ lần mở thứ hai.
     *
     * Nên splash đi chung đường [load]/[show] và giữ ad trong map riêng ở đây.
     */
    @JvmStatic
    @JvmOverloads
    fun preloadSplash(context: Context, slot: AdsSlot, action: (Boolean) -> Unit) {
        val activity = Ads.findActivity(context)
        if (activity == null) {
            Log.e(TAG, "preloadSplash: không dò được Activity từ context")
            action.invoke(false)
            return
        }
        load(activity, slot, action)
    }

    /**
     * Hiện interstitial đã tải trước ở splash. Chạy [action] trong MỌI trường hợp — ad đóng,
     * không có ad, hay lỗi — nên luồng ra khỏi splash không bao giờ bị chặn.
     */
    @JvmStatic
    fun showSplash(activity: Activity, slot: AdsSlot, action: () -> Unit) {
        show(activity, slot, action)
    }

    /**
     * Tải trước một interstitial cho [slot] và giữ trong nội bộ.
     *
     * [action] chạy khi xong, true nghĩa FSDAds báo đã tải xong — cùng cảnh báo như
     * [preloadSplash]: không bảo đảm có ad.
     */
    @JvmStatic
    fun load(activity: Activity, slot: AdsSlot, action: (Boolean) -> Unit) {
        if (!Ads.canUse(slot)) {
            action.invoke(false)
            return
        }
        try {
            FsdAds.loadInter(
                activity,
                AdsIds.idFor(slot),
                InterAdapter(
                    onLoaded = { ad ->
                        loaded[slot] = ad
                        action.invoke(true)
                    },
                    onLoadFailed = { error ->
                        loaded.remove(slot)
                        Log.e(TAG, "load thất bại slot=${slot.alias}: ${error.message}")
                        action.invoke(false)
                    },
                ),
            )
        } catch (t: Throwable) {
            Log.e(TAG, "load lỗi slot=${slot.alias}", t)
            action.invoke(false)
        }
    }

    /** [slot] đã có interstitial tải xong, sẵn sàng hiển thị chưa. */
    @JvmStatic
    fun isReady(slot: AdsSlot): Boolean = loaded[slot] != null

    /**
     * Hiển thị interstitial đã tải trước. Ad dùng một lần — muốn hiện lần nữa phải [load] lại.
     *
     * [action] chạy khi xong, trong MỌI trường hợp: ad đóng, ad lỗi, hoặc chưa có ad để hiện.
     * Nhờ vậy hành động của người dùng không bao giờ bị nuốt.
     */
    @JvmStatic
    fun show(activity: Activity, slot: AdsSlot, action: () -> Unit) {
        val ad = loaded.remove(slot)
        if (ad == null) {
            action.invoke()
            return
        }

        // Chốt để action chạy đúng một lần, phòng khi SDK gọi cả onShowFailed lẫn onDismiss.
        val fired = booleanArrayOf(false)
        val finish: () -> Unit = {
            if (!fired[0]) {
                fired[0] = true
                action.invoke()
            }
        }

        try {
            FsdAds.showInter(
                activity,
                ad,
                InterAdapter(
                    onShown = { Ads.markShown(slot) },
                    onShowFailed = {
                        Log.e(TAG, "show thất bại slot=${slot.alias}")
                        finish()
                    },
                    onDismiss = { finish() },
                ),
            )
        } catch (t: Throwable) {
            Log.e(TAG, "show lỗi slot=${slot.alias}", t)
            finish()
        }
    }

    /** Xoá ad đã tải của [slot] mà không hiển thị (dùng khi màn hình bị huỷ). */
    @JvmStatic
    fun clear(slot: AdsSlot) {
        loaded.remove(slot)
    }
}

/**
 * Bọc `InterAdsCallBack` của FSDAds thành các lambda rời.
 *
 * FSDAds bắt implement đủ 6 method, mà mỗi call-site chỉ quan tâm một vài method —
 * lớp này để chỗ gọi chỉ truyền đúng cái mình cần.
 */
private class InterAdapter(
    private val onLoaded: ((InterstitialAd) -> Unit)? = null,
    private val onLoadFailed: ((LoadAdError) -> Unit)? = null,
    private val onShown: (() -> Unit)? = null,
    private val onShowFailed: (() -> Unit)? = null,
    private val onDismiss: (() -> Unit)? = null,
    private val onClick: (() -> Unit)? = null,
) : InterAdsCallBack {

    override fun onAdsLoaded(ad: InterstitialAd) {
        onLoaded?.invoke(ad)
    }

    override fun onAdsLoadFailed(error: LoadAdError) {
        onLoadFailed?.invoke(error)
    }

    override fun onAdsShow() {
        onShown?.invoke()
    }

    override fun onAdsFailedShow() {
        onShowFailed?.invoke()
    }

    override fun onAdsDismiss() {
        onDismiss?.invoke()
    }

    override fun onAdsClick() {
        onClick?.invoke()
    }
}

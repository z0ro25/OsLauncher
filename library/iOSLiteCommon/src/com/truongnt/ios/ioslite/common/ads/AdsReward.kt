package com.truongnt.ios.ioslite.common.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.rewarded.RewardedAd
import com.truongnt.fsd.nttads.FsdAds
import com.truongnt.fsd.nttads.RewardAdsCallback

/**
 * Reward ad — **loại hoàn toàn mới**, hạ tầng cũ (`common/ad`) không có.
 *
 * Kiểu `RewardedAd` của AdMob không lộ ra ngoài; lớp này tự giữ lấy.
 *
 * Hai luồng dùng được:
 *  - [load] rồi [show] — tách rời, dùng khi muốn kiểm tra [isReady] trước.
 *  - [loadAndShow] — gộp một bước, dùng khi bấm nút là hiện ngay.
 */
object AdsReward {

    private const val TAG = "AdsReward"

    private val loaded = HashMap<AdsSlot, RewardedAd>()

    /** Tải trước reward ad cho [slot]. */
    @JvmStatic
    fun load(activity: Activity, slot: AdsSlot, callback: AdsRewardCallback?) {
        if (!Ads.canUse(slot)) {
            callback?.onLoadFailed(AdsError(AdsError.CODE_BLOCKED, "slot bị chặn: ${slot.alias}"))
            return
        }
        try {
            FsdAds.loadReward(activity, AdsIds.idFor(slot)) { ad ->
                if (ad == null) {
                    loaded.remove(slot)
                    callback?.onLoadFailed(
                        AdsError(AdsError.CODE_NOT_READY, "không tải được reward ad"),
                    )
                } else {
                    loaded[slot] = ad
                    callback?.onLoaded()
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "load lỗi slot=${slot.alias}", t)
            callback?.onLoadFailed(AdsError(AdsError.CODE_NOT_READY, t.message))
        }
    }

    /** [slot] đã có reward ad tải xong chưa. */
    @JvmStatic
    fun isReady(slot: AdsSlot): Boolean = loaded[slot] != null

    /** Hiển thị reward ad đã tải trước. Ad dùng một lần — muốn hiện lại phải [load] lại. */
    @JvmStatic
    fun show(activity: Activity, slot: AdsSlot, callback: AdsRewardCallback?) {
        val ad = loaded.remove(slot)
        if (ad == null) {
            callback?.onShowFailed(
                AdsError(AdsError.CODE_NOT_READY, "chưa có reward ad nào cho ${slot.alias}"),
            )
            return
        }
        if (!Ads.canUse(slot)) {
            callback?.onShowFailed(AdsError(AdsError.CODE_BLOCKED, "slot bị chặn: ${slot.alias}"))
            return
        }
        try {
            FsdAds.showReward(activity, ad, Wrapper(slot, callback))
        } catch (t: Throwable) {
            Log.e(TAG, "show lỗi slot=${slot.alias}", t)
            callback?.onShowFailed(AdsError(AdsError.CODE_SHOW_FAILED, t.message))
        }
    }

    /** Tải và hiện luôn trong một bước. */
    @JvmStatic
    fun loadAndShow(activity: Activity, slot: AdsSlot, callback: AdsRewardCallback?) {
        if (!Ads.canUse(slot)) {
            callback?.onShowFailed(AdsError(AdsError.CODE_BLOCKED, "slot bị chặn: ${slot.alias}"))
            return
        }
        try {
            FsdAds.loadAndShowReward(activity, AdsIds.idFor(slot), Wrapper(slot, callback))
        } catch (t: Throwable) {
            Log.e(TAG, "loadAndShow lỗi slot=${slot.alias}", t)
            callback?.onShowFailed(AdsError(AdsError.CODE_SHOW_FAILED, t.message))
        }
    }

    /** Bỏ ad đã tải của [slot] mà không hiển thị. */
    @JvmStatic
    fun clear(slot: AdsSlot) {
        loaded.remove(slot)
    }

    /** Nối `RewardAdsCallback` của FSDAds sang `AdsRewardCallback`, kèm ghi nhận policy. */
    private class Wrapper(
        private val slot: AdsSlot,
        private val callback: AdsRewardCallback?,
    ) : RewardAdsCallback {

        override fun onAdShowed() {
            callback?.onShown()
        }

        override fun onAdDismiss() {
            Ads.markShown(slot)
            callback?.onDismiss()
        }

        override fun onAdFailedToShow() {
            callback?.onShowFailed(AdsError(AdsError.CODE_SHOW_FAILED, "SDK báo hiển thị thất bại"))
        }

        override fun onEarnedReward() {
            callback?.onEarnedReward()
        }
    }
}

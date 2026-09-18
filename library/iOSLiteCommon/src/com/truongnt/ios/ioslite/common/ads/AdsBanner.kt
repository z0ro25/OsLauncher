package com.truongnt.ios.ioslite.common.ads

import android.app.Activity
import android.util.Log
import com.truongnt.fsd.nttads.FsdAds

/**
 * Banner ad — loại mới, hạ tầng cũ (`common/ad`) không có.
 *
 * ### Yêu cầu bắt buộc với layout của màn hình
 *
 * FSDAds **tự đi tìm view theo id**, không nhận container từ ta. Activity gọi [show] phải
 * có sẵn hai view sau, đúng tên và đúng loại:
 *
 *  - `@+id/banner_ad` — `ViewGroup`, khung ngoài để FSDAds ẩn/hiện cả cụm.
 *  - `@+id/adView_container` — `LinearLayout`, nơi `AdView` được nhồi vào.
 *
 * Thiếu một trong hai thì `findViewById` trả `null` và banner **im lặng không hiện** —
 * không crash, nhưng cũng không có log nào trong code của ta. Đây là cái bẫy dễ mất thời
 * gian nhất khi gắn banner.
 */
object AdsBanner {

    private const val TAG = "AdsBanner"

    /**
     * Hiện banner trong [activity].
     *
     * [AdsBannerCallback.onLoaded] chạy khi AdView đã được nhồi vào `adView_container`.
     * Ad thật sự hiển thị hay không phụ thuộc vào layout — xem mô tả class.
     */
    @JvmStatic
    fun show(activity: Activity, slot: AdsSlot, callback: AdsBannerCallback?) {
        if (!Ads.canUse(slot)) {
            callback?.onLoadFailed(AdsError(AdsError.CODE_BLOCKED, "slot bị chặn: ${slot.alias}"))
            return
        }
        try {
            FsdAds.showBanner(activity, AdsIds.idFor(slot)) {
                callback?.onLoaded()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "show lỗi slot=${slot.alias}", t)
            callback?.onLoadFailed(AdsError(AdsError.CODE_SHOW_FAILED, t.message))
        }
    }

    /**
     * Nạp lại banner cho [activity] — dùng khi quay lại màn hình.
     *
     * FSDAds nhớ cấu hình collapsible của lần [show] trước theo ad unit id, nên phải
     * [show] ít nhất một lần trước khi [reload] có ý nghĩa.
     */
    @JvmStatic
    fun reload(activity: Activity, slot: AdsSlot, callback: AdsBannerCallback?) {
        if (!Ads.canUse(slot)) {
            callback?.onLoadFailed(AdsError(AdsError.CODE_BLOCKED, "slot bị chặn: ${slot.alias}"))
            return
        }
        try {
            FsdAds.reloadBanner(activity, AdsIds.idFor(slot)) {
                callback?.onLoaded()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "reload lỗi slot=${slot.alias}", t)
            callback?.onLoadFailed(AdsError(AdsError.CODE_SHOW_FAILED, t.message))
        }
    }
}

package com.truongnt.ios.ioslite.common.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.truongnt.fsd.nttads.FsdAds
import com.truongnt.fsd.nttads.NativeAdsCallback

/**
 * Toàn bộ native ad.
 *
 * Khác biệt căn bản với hạ tầng cũ: FSDAds **không trả về một `View`** cho ta tự gắn.
 * Nó preload theo một khoá gọi là *alias* ([AdsSlot.alias]), rồi tự đổ nội dung vào một
 * `FrameLayout` do ta đưa. Vì vậy API ở đây là `container`, không phải `getAdContentView()`.
 *
 * ### Vì sao có hai đường gọi FSDAds
 *
 * FSDAds có hai nhóm API:
 *  - `showPreloadNative(...)` — chỉ nhận `FragmentActivity`.
 *  - `reloadNative(...)`      — nhận `Activity` thường.
 *
 * Mà `Launcher` của dự án kế thừa `android.app.Activity` trần (qua `CommonActivity`),
 * **không phải** `FragmentActivity`, trong khi các màn ThemeClub lại là `AppCompatActivity`.
 * [show] tự dò Activity từ `container` rồi chọn đường phù hợp, nên call-site ở cả hai nơi
 * dùng chung một API.
 */
object AdsNative {

    private const val TAG = "AdsNative"

    /**
     * Tải trước native ad cho [slot].
     *
     * Nội dung được FSDAds giữ theo [AdsSlot.alias]; [show] sau đó lấy ra đổ vào container.
     * Gọi càng sớm càng tốt (lúc dựng màn hình) để lúc cần là có sẵn.
     */
    /** Các slot đã bắt đầu tải trước — tránh phát lại request mỗi lần màn hình hiện lại. */
    private val preloadStarted = java.util.Collections.synchronizedSet(HashSet<AdsSlot>())

    @JvmStatic
    fun preload(activity: Activity, slot: AdsSlot, callback: AdsNativeCallback?) {
        if (!Ads.canUse(slot)) {
            callback?.onLoadFailed(AdsError(AdsError.CODE_BLOCKED, "slot bị chặn: ${slot.alias}"))
            return
        }
        if (!preloadStarted.add(slot)) {
            // Đã tải trước rồi. FSDAds giữ ad theo alias nên gọi lại chỉ tốn thêm request.
            return
        }
        try {
            FsdAds.preLoadNative(activity, slot.alias, AdsIds.idFor(slot)) { ad ->
                if (ad == null) {
                    callback?.onLoadFailed(
                        AdsError(AdsError.CODE_NOT_READY, "preload không trả về ad nào"),
                    )
                } else {
                    callback?.onLoaded()
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "preload lỗi slot=${slot.alias}", t)
            callback?.onLoadFailed(AdsError(AdsError.CODE_NOT_READY, t.message))
        }
    }

    /**
     * Đổ native ad của [slot] vào [container], dùng layout mặc định của FSDAds.
     *
     * Không tự gọi [preload] — phải preload trước, nếu không FSDAds không có gì để đổ và
     * sẽ báo lỗi qua [AdsNativeCallback.onLoadFailed].
     */
    @JvmStatic
    fun show(container: FrameLayout, slot: AdsSlot, callback: AdsNativeCallback?) {
        val activity = beginShow(slot, callback, container.context) ?: return
        // Khai báo kiểu tường minh: nếu để Kotlin tự suy luận thì `callback?.onLoaded()` trả
        // về Unit? và lambda thành `() -> Unit?` — không khớp tham số `() -> Unit` của FSDAds.
        val onDone: (View) -> Unit = { _ -> callback?.onLoaded() }
        val onFailed: () -> Unit = {
            callback?.onLoadFailed(
                AdsError(AdsError.CODE_NOT_READY, "FSDAds không đổ được native vào container"),
            )
        }
        try {
            if (activity is FragmentActivity) {
                // Bỏ trống layoutResId -> FSDAds dùng layout mặc định của nó.
                FsdAds.showPreloadNative(activity, slot.alias, container, onDone, onFailed)
            } else {
                reloadInto(activity, container, slot, callback)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "show lỗi slot=${slot.alias}", t)
            callback?.onLoadFailed(AdsError(AdsError.CODE_SHOW_FAILED, t.message))
        }
    }

    /**
     * Như [show] nhưng chỉ định layout của chính FSDAds để inflate.
     *
     * Hai layout có sẵn: `com.truongnt.fsd.nttads.R.layout.fsd_ads_native_large` và
     * `..._small`.
     *
     * Lưu ý: khi Activity dò được **không phải** `FragmentActivity`, đường đi buộc phải
     * là `reloadNative` — mà API đó không nhận tham số layout, nên [layoutResId] bị bỏ qua.
     */
    @JvmStatic
    fun show(container: FrameLayout, slot: AdsSlot, layoutResId: Int, callback: AdsNativeCallback?) {
        val activity = beginShow(slot, callback, container.context) ?: return
        // Khai báo kiểu tường minh: nếu để Kotlin tự suy luận thì `callback?.onLoaded()` trả
        // về Unit? và lambda thành `() -> Unit?` — không khớp tham số `() -> Unit` của FSDAds.
        val onDone: (View) -> Unit = { _ -> callback?.onLoaded() }
        val onFailed: () -> Unit = {
            callback?.onLoadFailed(
                AdsError(AdsError.CODE_NOT_READY, "FSDAds không đổ được native vào container"),
            )
        }
        try {
            if (activity is FragmentActivity) {
                FsdAds.showPreloadNative(activity, slot.alias, container, onDone, onFailed, layoutResId)
            } else {
                Log.w(TAG, "Activity không phải FragmentActivity — bỏ qua layoutResId")
                reloadInto(activity, container, slot, callback)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "show lỗi slot=${slot.alias}", t)
            callback?.onLoadFailed(AdsError(AdsError.CODE_SHOW_FAILED, t.message))
        }
    }

    /** Yêu cầu FSDAds nạp lại native vào [container] (đường đi dùng được với `Activity` thường). */
    @JvmStatic
    fun reload(activity: Activity, container: FrameLayout, slot: AdsSlot, callback: AdsNativeCallback?) {
        if (!Ads.canUse(slot)) {
            callback?.onLoadFailed(AdsError(AdsError.CODE_BLOCKED, "slot bị chặn: ${slot.alias}"))
            return
        }
        reloadInto(activity, container, slot, callback)
    }

    private fun reloadInto(
        activity: Activity,
        container: FrameLayout,
        slot: AdsSlot,
        callback: AdsNativeCallback?,
    ) {
        try {
            FsdAds.reloadNative(
                activity,
                slot.alias,
                container,
                object : NativeAdsCallback {
                    override fun onLoaded() {
                        callback?.onLoaded()
                    }

                    override fun onError() {
                        callback?.onLoadFailed(
                            AdsError(AdsError.CODE_NOT_READY, "reloadNative báo lỗi"),
                        )
                    }
                },
            )
        } catch (t: Throwable) {
            Log.e(TAG, "reload lỗi slot=${slot.alias}", t)
            callback?.onLoadFailed(AdsError(AdsError.CODE_SHOW_FAILED, t.message))
        }
    }

    /** Kiểm tra điều kiện chung rồi dò Activity. Trả null nghĩa là đã báo lỗi và nên dừng. */
    private fun beginShow(slot: AdsSlot, callback: AdsNativeCallback?, context: Context?): Activity? {
        if (!Ads.canUse(slot)) {
            callback?.onLoadFailed(AdsError(AdsError.CODE_BLOCKED, "slot bị chặn: ${slot.alias}"))
            return null
        }
        val activity = Ads.findActivity(context)
        if (activity == null) {
            callback?.onLoadFailed(
                AdsError(AdsError.CODE_NO_ACTIVITY, "không dò được Activity từ container"),
            )
            return null
        }
        return activity
    }
}

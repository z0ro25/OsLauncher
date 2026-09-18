package com.truongnt.ios.ioslite.common.ads

import android.app.Application
import android.util.Log
import com.truongnt.fsd.nttads.FsdAds

/**
 * App-open ad — quảng cáo hiện khi người dùng quay lại app.
 *
 * ### Cách nó chạy: KHÔNG có hàm `show()`
 *
 * [setup] đăng ký luôn một `ActivityLifecycleCallbacks` lên `Application`. Từ đó FSDAds
 * **tự** hiện app-open mỗi khi một Activity được tạo, không cần ta gọi gì thêm. Đây là
 * khác biệt lớn so với `LauncherAdTrigger` — vốn phải tự chạy một hành động sau khi ad đóng.
 *
 * Hai hệ quả cần nhớ:
 *  - Muốn **chặn** app-open ở một màn cụ thể thì gọi [disable] với class của màn đó.
 *  - FSDAds tự bỏ qua nếu ad đã tải quá **4 giờ** — không hiện ad cũ.
 */
object AdsOpen {

    private const val TAG = "AdsOpen"

    /**
     * Chặn gọi [setup] lần thứ hai.
     *
     * Bắt buộc phải có: FSDAds đăng ký một `ActivityLifecycleCallbacks` MỖI LẦN setup, nên
     * gọi hai lần sẽ khiến `onActivityCreated` chạy hai lượt cho cùng một Activity và
     * app-open có thể hiện hai lần.
     */
    @Volatile
    private var setupDone = false

    /**
     * Bật app-open. Gọi một lần trong `Application.onCreate()`.
     *
     * Gọi lại lần nữa là no-op (xem [setupDone]).
     *
     * [AdsOpenCallback.onAvailabilityChanged] cho biết đã tải được app-open ad hay chưa;
     * `false` thường là do chưa có mạng, FSDAds sẽ tự thử lại.
     */
    @JvmStatic
    fun setup(application: Application, slot: AdsSlot, callback: AdsOpenCallback?) {
        if (setupDone) {
            Log.w(TAG, "AdsOpen.setup() đã gọi trước đó — bỏ qua lần gọi thứ hai")
            return
        }
        if (!Ads.canUse(slot)) {
            callback?.onAvailabilityChanged(false)
            return
        }
        setupDone = true
        try {
            FsdAds.setupOpenAds(application, AdsIds.idFor(slot)) { available ->
                callback?.onAvailabilityChanged(available)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "setup lỗi slot=${slot.alias}", t)
            callback?.onAvailabilityChanged(false)
        }
    }

    /** Cho phép app-open hiện ở [activityClass] (mặc định là cho phép). */
    @JvmStatic
    fun enable(activityClass: Class<*>) {
        try {
            FsdAds.enableOpenAdsActivity(activityClass)
        } catch (t: Throwable) {
            Log.e(TAG, "enable lỗi", t)
        }
    }

    /** Chặn app-open ở [activityClass]. */
    @JvmStatic
    fun disable(activityClass: Class<*>) {
        try {
            FsdAds.disableOpenAdsActivity(activityClass)
        } catch (t: Throwable) {
            Log.e(TAG, "disable lỗi", t)
        }
    }
}

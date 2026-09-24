package com.truongnt.ios.ioslite.common.ads

/**
 * Callback của package `ads`.
 *
 * Vì sao dùng `abstract class` chứ không phải `interface` Kotlin: interface có default
 * method chỉ override tuỳ chọn được từ Java khi bật `-Xjvm-default=all`, mà dự án chưa bật.
 * Toàn bộ call-site sẽ migrate đều là Java, nên `abstract class` với thân rỗng là cách
 * để chúng chỉ override đúng cái mình cần.
 */

/** Vòng đời native ad. */
abstract class AdsNativeCallback {
    open fun onLoaded() {}
    open fun onLoadFailed(error: AdsError) {}
    open fun onClick() {}
}

/** Vòng đời reward ad. */
abstract class AdsRewardCallback {
    open fun onLoaded() {}
    open fun onLoadFailed(error: AdsError) {}
    open fun onShown() {}
    open fun onShowFailed(error: AdsError) {}
    open fun onDismiss() {}
    open fun onEarnedReward() {}
}

/** Vòng đời banner. */
abstract class AdsBannerCallback {
    open fun onLoaded() {}
    open fun onLoadFailed(error: AdsError) {}
}

/** Trạng thái sẵn sàng của app-open ad. */
abstract class AdsOpenCallback {
    open fun onAvailabilityChanged(available: Boolean) {}
}

/**
 * Kết quả load của cặp interstitial + native full ([InterNativeFull]).
 *
 * Bất biến: đúng MỘT trong hai hàm chạy cho mỗi lần [InterNativeFull.load], và chỉ chạy sau
 * khi phần native có kết quả (thành công hay thất bại đều báo) — không chờ interstitial,
 * vì interstitial load chậm hay lỗi cũng không quyết định được việc có dialog hay không.
 */
abstract class AdsInterNativeFullCallback {
    /** Native full đã có ad — [InterNativeFull.show] lúc này mới hiện được dialog. */
    open fun onLoaded() {}

    /** Không có native full nào để hiện. Interstitial (nếu tải được) vẫn dùng bình thường. */
    open fun onLoadFailed(error: AdsError) {}
}

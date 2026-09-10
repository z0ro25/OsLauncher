package com.truongnt.ios.launcher.ad;

import android.app.Activity;

import com.truongnt.ios.ioslite.common.ad.IOSAdConfig;
import com.truongnt.ios.ioslite.common.ad.IOSAdError;
import com.truongnt.ios.ioslite.common.ad.IOSAdManager;
import com.truongnt.ios.ioslite.common.ad.IOSFullScreenAd;
import com.truongnt.ios.ioslite.common.ad.IOSFullScreenAdListener;

/**
 * Trigger interstitial "mở app" cho launcher.
 *
 * Ý tưởng: một hành động (mở app / click widget có sẵn) được "bọc" sau interstitial.
 * Nếu ad sẵn sàng và policy cho phép -> show interstitial, ĐÓNG ad xong mới chạy hành động.
 * Nếu chưa có ad (SDK chưa gắn / chưa load / policy chặn) -> chạy hành động NGAY (no-op an toàn),
 * nên hành vi baseline không đổi khi chưa cắm SDK.
 *
 * Bất biến quan trọng: {@code onContinue} LUÔN được chạy đúng MỘT lần, dù ad lỗi hay đóng.
 * Không bao giờ được nuốt mất hành động của người dùng.
 */
public final class LauncherAdTrigger {

    private LauncherAdTrigger() {
    }

    /**
     * Bọc hành động mở app sau interstitial ID_INTERSTITIAL_OPEN_APP.
     *
     * @param activity   activity đang hiển thị (để show ad); null -> chạy thẳng onContinue.
     * @param onContinue hành động thật (mở app / click widget). Bắt buộc.
     */
    public static void openAppWithInterstitial(Activity activity, Runnable onContinue) {
        runWithInterstitial(activity, IOSAdConfig.ID_INTERSTITIAL_OPEN_APP, onContinue);
    }

    /**
     * Bọc {@code onContinue} sau interstitial của {@code adId}.
     * Xem mô tả class về bất biến "chạy đúng 1 lần" + "no-op an toàn khi chưa có ad".
     */
    public static void runWithInterstitial(final Activity activity, final int adId, final Runnable onContinue) {
        if (onContinue == null) {
            return;
        }
        // Không có activity -> không thể show ad, chạy thẳng.
        if (activity == null) {
            onContinue.run();
            return;
        }
        try {
            // Policy tần suất (AdDisplayHelper). Không cho hiển thị -> chạy thẳng.
            if (!IOSAdManager.shouldShowAd(adId)) {
                onContinue.run();
                return;
            }
            IOSFullScreenAd ad = IOSAdManager.getInstance(activity).getFullScreenAd(adId);
            // Chưa có ad / chưa load xong -> chạy thẳng (no-op an toàn).
            if (ad == null || !ad.isReady()) {
                onContinue.run();
                return;
            }

            // Chốt để onContinue chỉ chạy 1 lần (đóng ad hoặc lỗi đều nhả tiếp).
            final boolean[] fired = {false};
            ad.setListener(new IOSFullScreenAdListener() {
                @Override
                public void onLoaded() {
                }

                @Override
                public void onError(IOSAdError error) {
                    // Ad lỗi lúc show -> vẫn phải mở app (không tính là đã hiển thị).
                    fireOnce(false);
                }

                @Override
                public void onShown() {
                }

                @Override
                public void onClosed() {
                    // Đã xem xong ad -> ghi nhận hiển thị rồi mở app.
                    fireOnce(true);
                }

                private void fireOnce(boolean shown) {
                    if (fired[0]) {
                        return;
                    }
                    fired[0] = true;
                    if (shown) {
                        IOSAdManager.afterShowAd(adId);
                    }
                    onContinue.run();
                }
            });
            ad.show(activity);
        } catch (Throwable t) {
            // Bất kỳ sự cố nào với ad cũng KHÔNG được chặn hành động của người dùng.
            onContinue.run();
        }
    }
}

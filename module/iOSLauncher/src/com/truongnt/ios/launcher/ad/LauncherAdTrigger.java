package com.truongnt.ios.launcher.ad;

import android.app.Activity;
import android.util.Log;

import com.truongnt.ios.ioslite.common.ads.AdsInterstitial;
import com.truongnt.ios.ioslite.common.ads.AdsSlot;

import kotlin.Unit;

/**
 * Trigger interstitial "mở app" cho launcher.
 *
 * <p>Ý tưởng: một hành động (mở app / click widget có sẵn) được "bọc" sau interstitial.
 * Nếu ad sẵn sàng và policy cho phép -> show interstitial, ĐÓNG ad xong mới chạy hành động.
 * Nếu chưa có ad (chưa load / policy chặn / user đã mua bản không quảng cáo) -> chạy hành
 * động NGAY, nên hành vi baseline không đổi.
 *
 * <p><b>Bất biến quan trọng:</b> {@code onContinue} LUÔN được chạy đúng MỘT lần, dù ad lỗi,
 * bị đóng, hay ném exception. Không bao giờ được nuốt mất hành động của người dùng.
 *
 * <p>Khác bản cũ ở một điểm: khi chưa có ad sẵn, lớp này <b>tải trước</b> cho lần mở app sau
 * rồi vẫn chạy thẳng hành động ngay. Bản cũ chỉ chạy thẳng mà không chuẩn bị gì, nên sẽ
 * không bao giờ có ad để hiện. Nhờ vậy lần mở app thứ hai trở đi mới thực sự có interstitial.
 *
 * <p>Việc ghi nhận policy tần suất ({@code shouldShow} / {@code onShown}) do tầng
 * {@link AdsInterstitial} tự lo — ở đây không gọi lại, tránh đếm hai lần.
 */
public final class LauncherAdTrigger {

    private static final String TAG = "LauncherAdTrigger";

    private LauncherAdTrigger() {
    }

    /**
     * Bọc hành động mở app sau interstitial {@link AdsSlot#INTER_IN_APP}.
     *
     * @param activity   activity đang hiển thị (để show ad); null -> chạy thẳng onContinue.
     * @param onContinue hành động thật (mở app / click widget). Bắt buộc.
     */
    public static void openAppWithInterstitial(Activity activity, Runnable onContinue) {
        runWithInterstitial(activity, AdsSlot.INTER_IN_APP, onContinue);
    }

    /**
     * Bọc {@code onContinue} sau interstitial của {@code slot}.
     * Xem mô tả class về bất biến "chạy đúng 1 lần" + "no-op an toàn khi chưa có ad".
     */
    public static void runWithInterstitial(final Activity activity, final AdsSlot slot,
                                           final Runnable onContinue) {
        if (onContinue == null) {
            return;
        }
        // Không có activity -> không thể show ad, chạy thẳng.
        if (activity == null) {
            onContinue.run();
            return;
        }

        // Chốt để onContinue chỉ chạy 1 lần: đóng ad, lỗi show, hay exception đều nhả qua đây.
        final boolean[] fired = {false};
        final Runnable finish = new Runnable() {
            @Override
            public void run() {
                if (fired[0]) {
                    return;
                }
                fired[0] = true;
                onContinue.run();
            }
        };

        try {
            if (!AdsInterstitial.isReady(slot)) {
                // Chưa có ad sẵn -> KHÔNG được chặn người dùng. Tải trước cho lần sau,
                // lần này đi thẳng.
                AdsInterstitial.load(activity, slot, ignored -> Unit.INSTANCE);
                finish.run();
                return;
            }

            AdsInterstitial.show(activity, slot, () -> {
                // Ad đóng, ad lỗi, hay chưa có ad để hiện — đều phải mở app.
                // `() -> Unit` của Kotlin hiện ra Java là Function0<Unit> nên phải trả Unit.INSTANCE.
                finish.run();
                return Unit.INSTANCE;
            });
        } catch (Throwable t) {
            // Bất kỳ sự cố nào với ad cũng KHÔNG được chặn hành động của người dùng.
            Log.e(TAG, "lỗi khi hiển thị interstitial cho slot " + slot, t);
            finish.run();
        }
    }
}

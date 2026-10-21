package com.truongnt.ios.launcher.ad;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk;
import com.truongnt.ios.ioslite.common.config.AppAds;

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
 * <p>Khoảng nghỉ giữa hai lần hiện ad do AdsKit tự lo (fullscreenIntervalMs trong JSON) —
 * ở đây không đếm lại, tránh tính hai lần.
 *
 * <p><b>Tần suất hiển thị:</b> riêng {@link #openAppWithInterstitial} còn lọc thêm một lớp
 * đếm click của {@link AdClickCounter} — cứ 5 lần bấm mới chen ad một lần. Lớp đếm này nằm
 * TRÊN {@link #runWithInterstitial} nên {@code runWithInterstitial} vẫn giữ nguyên hành vi
 * cũ cho mọi call-site khác.
 */
public final class LauncherAdTrigger {

    private static final String TAG = "LauncherAdTrigger";

    // Khoá tách bộ đếm click theo vị trí (SDK eztech không có khái niệm slot).
    private static final String INTER_SLOT_KEY = "inter_in_app";

    // Placement interstitial của launcher trong JSON (ads/app-placements.json, placements.json).
    private static final String INTER_PLACEMENT = "inter_launcher";

    private LauncherAdTrigger() {
    }

    /**
     * Bọc hành động mở app sau interstitial in-app.
     *
     * <p><b>Tần suất:</b> chỉ mỗi lần bấm thứ {@link AdClickCounter#SHOW_EVERY} mới thực sự chen
     * interstitial (xem {@link AdClickCounter}). Các lần bấm còn lại vẫn TẢI TRƯỚC ad nhưng mở
     * app ngay, nên người dùng không bị chặn và tới lượt thứ 5 ad đã sẵn sàng.
     *
     * @param activity   activity đang hiển thị (để show ad); null -> chạy thẳng onContinue.
     * @param onContinue hành động thật (mở app / click widget). Bắt buộc.
     */
    public static void openAppWithInterstitial(Activity activity, Runnable onContinue) {
        if (onContinue == null) {
            return;
        }
        // Đã mua bản không quảng cáo -> không tải trước, không chèn ad, mở app ngay. Đặt TRƯỚC
        // cả nhánh preloadQuietly để không phát request thừa lên AdMob.
        if (activity == null || AdsSdk.INSTANCE.isAdFree()) {
            onContinue.run();
            return;
        }
        // Chưa tới lượt hiện ad -> tải trước cho lần thứ 5 rồi mở app ngay. Đây là nhánh
        // thường gặp, KHÔNG được chặn người dùng.
        if (activity != null
                && !AdClickCounter.markClickAndShouldShow(activity, INTER_SLOT_KEY)) {
            preloadQuietly(activity);
            onContinue.run();
            return;
        }
        runWithInterstitial(activity, onContinue);
    }

    /**
     * Như {@link #openAppWithInterstitial(Activity, Runnable)} nhưng có thêm {@code target} để
     * biết app sắp mở là app nào.
     *
     * <p><b>Loại trừ app launcher của chính mình:</b> nếu {@code target} nhắm vào package của
     * app này (icon launcher trong App Library, app trong widget gợi ý...) thì mở thẳng, KHÔNG
     * tính vào bộ đếm và KHÔNG chen ad. Bấm vào chính mình không phải là "dùng app" nên không
     * được tính công cho lượt quảng cáo.
     *
     * <p>Lưu ý: iOS shortcut nội bộ (Wallpaper, Theme, Battery Save, Discovery...) đã bị
     * {@code Launcher.handlerIOSShortcutClick} chặn từ trước, không đi qua đây.
     *
     * @param activity   activity đang hiển thị; null -> chạy thẳng onContinue.
     * @param target     intent thật sẽ được mở. null -> coi như không phải app của mình.
     * @param onContinue hành động thật. Bắt buộc.
     */
    public static void openAppWithInterstitial(Activity activity, Intent target,
                                               Runnable onContinue) {
        if (onContinue == null) {
            return;
        }
        if (activity != null && targetsSelf(activity, target)) {
            onContinue.run();
            return;
        }
        openAppWithInterstitial(activity, onContinue);
    }

    /**
     * Tải trước interstitial mà không chặn ai — dùng cho những lần bấm chưa tới lượt hiện ad.
     *
     * <p>Chỉ phát request khi trong tay CHƯA có ad, tránh mỗi lần bấm lại ném thêm một request
     * thừa lên AdMob.
     */
    private static void preloadQuietly(Activity activity) {
        if (AppAds.isFullScreenReady(INTER_PLACEMENT)) {
            return;
        }
        AppAds.preloadFullScreen(INTER_PLACEMENT);
    }

    /**
     * {@code target} có nhắm vào chính app launcher này không.
     *
     * <p>Xét {@code component} trước (đường đi thường gặp), rồi tới {@code package} — có app
     * chỉ set package chứ không set component.
     */
    private static boolean targetsSelf(Context context, Intent target) {
        if (target == null) {
            return false;
        }
        String self = context.getPackageName();
        ComponentName component = target.getComponent();
        if (component != null) {
            return self.equals(component.getPackageName());
        }
        return self.equals(target.getPackage());
    }

    /**
     * Bọc {@code onContinue} sau interstitial in-app.
     * Xem mô tả class về bất biến "chạy đúng 1 lần" + "no-op an toàn khi chưa có ad".
     */
    public static void runWithInterstitial(final Activity activity, final Runnable onContinue) {
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
            if (!AppAds.isFullScreenReady(INTER_PLACEMENT)) {
                // Chưa có ad sẵn -> KHÔNG được chặn người dùng. Tải trước cho lần sau,
                // lần này đi thẳng.
                AppAds.preloadFullScreen(INTER_PLACEMENT);
                finish.run();
                return;
            }

            // AdsKit chỉ show từ FragmentActivity (Launcher đã kế thừa CommonFragmentActivity);
            // nhánh này là chốt cho call-site đưa Activity trần vào.
            if (!(activity instanceof FragmentActivity)) {
                Log.w(TAG, "activity không phải FragmentActivity — bỏ interstitial, mở app ngay");
                finish.run();
                return;
            }

            // finish chạy khi ad đóng, lỗi, hay không có gì để hiện — xem AppAds.showFullScreen.
            AppAds.showFullScreen((FragmentActivity) activity, INTER_PLACEMENT, finish);
        } catch (Throwable t) {
            // Bất kỳ sự cố nào với ad cũng KHÔNG được chặn hành động của người dùng.
            Log.e(TAG, "lỗi khi hiển thị interstitial in-app", t);
            finish.run();
        }
    }
}

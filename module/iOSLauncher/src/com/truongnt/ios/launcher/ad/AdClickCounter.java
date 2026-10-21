package com.truongnt.ios.launcher.ad;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Đếm số lần người dùng bấm vào một hành động "mở app" để quyết định lần bấm nào được chen
 * interstitial vào.
 *
 * <p>Bộ đếm lưu trong SharedPreferences nên đếm DỒN qua nhiều phiên: bấm 3 lần hôm nay, mai
 * bấm 2 lần nữa là tới lượt hiện ad. Đếm trong bộ nhớ (biến static) sẽ bị launcher kill nền
 * xoá sạch, khiến người dùng ít bấm gần như không bao giờ thấy ad.
 *
 * <p>Cố ý KHÔNG cài đặt qua cơ chế delay của SDK: delay đó được kiểm tra ngay bên trong
 * {@code AdsKit.showAdFullScreen()}, nên nó chặn luôn cả khâu TẢI TRƯỚC — tới lần bấm thứ 5 sẽ
 * không có ad sẵn mà hiện. Ở đây chỉ quyết định lúc NÀO hiện, còn việc tải vẫn diễn ra ở mọi
 * lần bấm (xem {@link LauncherAdTrigger#openAppWithInterstitial}).
 *
 * <p>Bộ đếm tách riêng theo {@code slotKey} để sau này thêm vị trí khác không đụng vào nhau.
 * Mọi call-site đều ở main thread nên không cần đồng bộ.
 */
final class AdClickCounter {

    private static final String TAG = "AdClickCounter";

    private static final String PREFS = "ad_click_counter";

    /** Cứ bao nhiêu lần bấm thì cho hiện interstitial một lần. */
    static final int SHOW_EVERY = 5;

    private AdClickCounter() {
    }

    /**
     * Ghi nhận thêm một lần bấm cho {@code slot} và cho biết lần bấm này có tới lượt hiện
     * interstitial hay không.
     *
     * <p>Lần thứ {@value #SHOW_EVERY}, 10, 15... trả về {@code true}.
     *
     * <p><b>Bất biến:</b> hàm này chỉ QUYẾT ĐỊNH, không bao giờ ném ra ngoài — lỗi đọc/ghi
     * prefs cùng lắm là trả {@code false} (mở app ngay), tuyệt đối không được làm hỏng hành
     * động mở app của người dùng.
     */
    static boolean markClickAndShouldShow(Context context, String slotKey) {
        if (context == null || slotKey == null) {
            return false;
        }
        try {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String key = "click_count_" + slotKey;
            int count = prefs.getInt(key, 0) + 1;
            prefs.edit().putInt(key, count).apply();
            return count % SHOW_EVERY == 0;
        } catch (Throwable t) {
            Log.e(TAG, "không đọc/ghi được bộ đếm click cho vị trí " + slotKey, t);
            return false;
        }
    }
}

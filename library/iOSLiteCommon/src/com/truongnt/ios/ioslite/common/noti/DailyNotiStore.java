package com.truongnt.ios.ioslite.common.noti;

import android.content.Context;

import com.truongnt.ios.ioslite.common.debug.DebugLog;
import com.truongnt.ios.ioslite.common.util.PreferencesUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Lưu trạng thái notification hằng ngày vào SharedPreferences dùng chung
 * ({@link PreferencesUtil#PREFERENCE_NAME}).
 *
 * <p>Bất biến cần giữ:
 * <ul>
 *   <li>{@link #KEY_FIRED_DAY} là chốt chặn "mỗi ngày tối đa 1 lần" — mọi đường bắn noti đều
 *       phải đi qua {@link #hasFiredToday(Context)} trước và ghi lại ngay sau khi chạy xong.
 *       Ghi theo NGÀY (yyyy-MM-dd) chứ không theo timestamp, nên đổi mốc giờ trong ngày không
 *       làm bắn lặp.</li>
 *   <li>{@link #KEY_EVER_SCHEDULED} phân biệt "vừa cài, chưa từng hẹn" với "đã hẹn rồi nhưng
 *       lỡ mốc". Chỉ trường hợp thứ hai mới bắn bù — nhờ vậy người mới cài app lúc 3h chiều
 *       không bị bắn noti ngay lập tức.</li>
 * </ul>
 */
final class DailyNotiStore {

    private static final String TAG = "DailyNoti";

    /** Key Remote Config đẩy từ :app xuống — cũng là key lưu trong prefs. */
    static final String KEY_ENABLED = "daily_noti_enabled";
    static final String KEY_TIME = "daily_noti_time";

    private static final String KEY_FIRED_DAY = "daily_noti_fired_day";
    private static final String KEY_EVER_SCHEDULED = "daily_noti_ever_scheduled";

    /** Mốc giờ dùng khi Remote Config chưa có giá trị hoặc trả về chuỗi không đọc được. */
    static final String DEFAULT_TIME = "08:00";

    private static final String DAY_PATTERN = "yyyy-MM-dd";

    private DailyNotiStore() {
    }

    static boolean isEnabled(Context context) {
        return PreferencesUtil.getBoolean(context, KEY_ENABLED, false);
    }

    static void setEnabled(Context context, boolean enabled) {
        PreferencesUtil.putBoolean(context, KEY_ENABLED, enabled);
    }

    /** Trả về chuỗi thô từ Remote Config, đã thay bằng mốc mặc định nếu rỗng. */
    static String getRawTime(Context context) {
        String raw = PreferencesUtil.getString(context, KEY_TIME, DEFAULT_TIME);
        return (raw == null || raw.trim().isEmpty()) ? DEFAULT_TIME : raw;
    }

    static void setRawTime(Context context, String rawTime) {
        PreferencesUtil.putString(context, KEY_TIME, rawTime == null ? DEFAULT_TIME : rawTime);
    }

    /**
     * Đọc mốc giờ đã lưu thành {giờ, phút}.
     *
     * <p>Chuỗi sai định dạng thì rơi về {@link #DEFAULT_TIME} chứ không ném lỗi — giá trị này
     * đến từ Remote Config, gõ sai trên console không được phép làm chết luồng hẹn giờ.
     */
    static int[] parseTime(Context context) {
        return parseTime(getRawTime(context));
    }

    private static int[] parseTime(String raw) {
        int[] fallback = parseDefault();
        if (raw == null) {
            return fallback;
        }
        String[] parts = raw.trim().split(":");
        if (parts.length < 2) {
            DebugLog.w(TAG, "mốc giờ không đọc được: \"" + raw + "\" -> dùng " + DEFAULT_TIME);
            return fallback;
        }
        try {
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                DebugLog.w(TAG, "mốc giờ ngoài khoảng hợp lệ: \"" + raw + "\" -> dùng " + DEFAULT_TIME);
                return fallback;
            }
            return new int[]{hour, minute};
        } catch (NumberFormatException e) {
            DebugLog.w(TAG, "mốc giờ không phải số: \"" + raw + "\" -> dùng " + DEFAULT_TIME);
            return fallback;
        }
    }

    private static int[] parseDefault() {
        // Tách thẳng từ DEFAULT_TIME để mốc mặc định chỉ có MỘT nguồn; viết cứng {8, 0} ở đây
        // sẽ tạo nguồn thứ hai, sửa một chỗ quên chỗ kia là lệch âm thầm.
        String[] parts = DEFAULT_TIME.split(":");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }

    static boolean hasFiredToday(Context context) {
        return today().equals(PreferencesUtil.getString(context, KEY_FIRED_DAY, ""));
    }

    static void markFiredToday(Context context) {
        PreferencesUtil.putString(context, KEY_FIRED_DAY, today());
    }

    static boolean isEverScheduled(Context context) {
        return PreferencesUtil.getBoolean(context, KEY_EVER_SCHEDULED, false);
    }

    static void setEverScheduled(Context context) {
        PreferencesUtil.putBoolean(context, KEY_EVER_SCHEDULED, true);
    }

    /** Ngày hiện tại theo giờ máy, dạng yyyy-MM-dd. */
    static String today() {
        return new SimpleDateFormat(DAY_PATTERN, Locale.US).format(new Date());
    }

    /**
     * Mốc giờ gần nhất đã qua trong hôm nay (dùng để phát hiện "lỡ mốc"), tính theo giờ máy.
     * Trả về 0 nếu hôm nay chưa tới mốc.
     */
    static long todayTriggerAt(int hour, int minute, long now) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long at = c.getTimeInMillis();
        return at <= now ? at : 0L;
    }

    /**
     * Mốc giờ kế tiếp: hôm nay nếu chưa tới, còn không thì đẩy sang ngày mai.
     *
     * <p>Cố ý KHÔNG dùng {@link Calendar#add(int, int)} với DAY_OF_YEAR trên mốc đã qua để tránh
     * lệch khi đổi giờ tiết kiệm ánh sáng; cộng thẳng 1 ngày vào mốc hôm nay rồi để Calendar
     * chuẩn hoá lại là đủ và luôn ra đúng giờ tường.
     */
    static long nextTriggerAt(int hour, int minute, long now) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= now) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        return c.getTimeInMillis();
    }
}

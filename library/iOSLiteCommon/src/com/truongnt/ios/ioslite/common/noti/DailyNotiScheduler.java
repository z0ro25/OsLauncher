package com.truongnt.ios.ioslite.common.noti;

import android.content.Context;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.truongnt.ios.ioslite.common.CommonSdk;
import com.truongnt.ios.ioslite.common.debug.DebugLog;

import java.util.concurrent.TimeUnit;

/**
 * Điểm vào DUY NHẤT của tính năng notification hằng ngày, dành cho {@code :app} gọi.
 *
 * <p>Kiến trúc: iOSLiteCommon không kéo Firebase vào. {@code :app} đọc Remote Config rồi đẩy
 * xuống qua {@link #applyRemoteConfig(boolean, String)} — nhờ vậy đổi mốc giờ trên console có
 * hiệu lực ngay ở lần mở app kế tiếp, không cần build lại.
 *
 * <p>Vì sao dùng OneTime + tự hẹn lại thay vì PeriodicWorkRequest: PeriodicWorkRequest không
 * hứa hẹn mốc giờ, hệ thống dồn dịch theo cửa sổ tối thiểu 15 phút và có thể lệch hàng giờ —
 * không dùng được cho mốc "8h sáng" cố định. OneTime với initialDelay bám mốc sát hơn nhiều,
 * và vẫn không cần quyền exact alarm.
 *
 * <p>WorkManager tự lưu work vào DB nội bộ nên work sống qua reboot: máy khởi động lại thì
 * JobScheduler chạy lại work đã hẹn, không cần RECEIVE_BOOT_COMPLETED.
 */
public final class DailyNotiScheduler {

    private static final String TAG = "DailyNoti";

    /**
     * Tên work duy nhất — dùng kèm {@link ExistingWorkPolicy#REPLACE} để mỗi lần đổi mốc giờ
     * chỉ còn ĐÚNG MỘT work đang chờ, không tích tụ work cũ bắn sai giờ.
     */
    private static final String UNIQUE_WORK_NAME = "daily_noti_work";

    private DailyNotiScheduler() {
    }

    /**
     * Nhận cấu hình từ Remote Config và hẹn lại work. Gọi được nhiều lần, idempotent.
     *
     * @param enabled {@code daily_noti_enabled} — false thì huỷ hết work đang chờ
     * @param time    {@code daily_noti_time} dạng "HH:mm", sai định dạng sẽ rơi về 08:00
     */
    public static void applyRemoteConfig(boolean enabled, String time) {
        Context context = CommonSdk.getApplicationContext();
        if (context == null) {
            // Chưa qua CommonSdk.initalize() -> không có context để hẹn giờ. Bỏ qua thay vì
            // ném lỗi: đây là luồng phụ, không được phép làm chết khởi động app.
            DebugLog.w(TAG, "CommonSdk chưa init, bỏ qua hẹn notification hằng ngày");
            return;
        }

        DailyNotiStore.setEnabled(context, enabled);
        DailyNotiStore.setRawTime(context, time);

        if (!enabled) {
            DebugLog.d(TAG, "Remote Config tắt -> huỷ work đang chờ");
            cancel(context);
            return;
        }

        int[] hm = DailyNotiStore.parseTime(context);
        long now = System.currentTimeMillis();

        if (!DailyNotiStore.isEverScheduled(context)) {
            // Lần đầu hẹn (vừa cài / vừa bật): hẹn thẳng mốc KẾ TIẾP, không bắn bù cho hôm nay.
            // Nếu không chặn ở đây thì ai cài app lúc 3h chiều sẽ ăn noti ngay lập tức.
            DailyNotiStore.setEverScheduled(context);
            enqueueAt(context, DailyNotiStore.nextTriggerAt(hm[0], hm[1], now), now);
            return;
        }

        // Đã từng hẹn mà hôm nay vẫn chưa bắn và mốc hôm nay đã trôi qua -> lỡ mốc (máy tắt,
        // doze, hoặc user chưa mở app). Bắn bù ngay, đúng lựa chọn "bắn bù 1 lần/ngày".
        long todayAt = DailyNotiStore.todayTriggerAt(hm[0], hm[1], now);
        boolean missedToday = todayAt > 0 && !DailyNotiStore.hasFiredToday(context);
        long targetAt = missedToday ? now : DailyNotiStore.nextTriggerAt(hm[0], hm[1], now);

        if (missedToday) {
            DebugLog.d(TAG, "lỡ mốc hôm nay, bắn bù ngay");
        }
        enqueueAt(context, targetAt, now);
    }

    /** Huỷ work đang chờ — dùng khi Remote Config tắt tính năng. */
    public static void cancel() {
        Context context = CommonSdk.getApplicationContext();
        if (context == null) {
            return;
        }
        cancel(context);
    }

    private static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME);
    }

    /**
     * Hẹn cho mốc kế tiếp. {@link DailyNotiWorker} gọi lại sau mỗi lần chạy để tự duy trì chuỗi
     * hằng ngày — WorkManager không có cơ chế "chạy lại sau N giờ" cho OneTime nên phải tự nối.
     */
    static void enqueueNextDay(Context context) {
        int[] hm = DailyNotiStore.parseTime(context);
        long now = System.currentTimeMillis();
        enqueueAt(context, DailyNotiStore.nextTriggerAt(hm[0], hm[1], now), now);
    }

    private static void enqueueAt(Context context, long targetAt, long now) {
        long delay = Math.max(0L, targetAt - now);
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DailyNotiWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(TAG)
                .build();
        WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request);
        DebugLog.d(TAG, "đã hẹn work sau " + (delay / 1000) + "s");
    }
}

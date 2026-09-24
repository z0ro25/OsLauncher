package com.truongnt.ios.ioslite.common.noti;

import android.content.Context;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.truongnt.ios.ioslite.common.CommonSdk;
import com.truongnt.ios.ioslite.common.debug.DebugLog;
import com.truongnt.ios.ioslite.common.util.PreferencesUtil;

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
 *
 * <p>Bất biến quan trọng nhất: chuỗi hằng ngày nối bằng HAI tên work luân phiên — xem
 * {@link #WORK_NAMES}. Gộp về một tên là chuỗi đứt sau lần bắn đầu tiên.
 */
public final class DailyNotiScheduler {

    private static final String TAG = "DailyNoti";

    /**
     * HAI tên work dùng LUÂN PHIÊN, không phải một. Lý do: {@link ExistingWorkPolicy#REPLACE}
     * cancel + xoá mọi work cùng tên KỂ CẢ work đang RUNNING — worker mà hẹn ngày mai bằng chính
     * tên nó đang chạy thì tự huỷ mình, chuỗi đứt ngay sau lần bắn đầu tiên.
     */
    private static final String[] WORK_NAMES = {"daily_noti_work", "daily_noti_work_b"};

    /** Tên work đang chờ; lưu để worker biết phải hẹn ngày mai bằng tên CÒN LẠI. */
    private static final String KEY_ACTIVE_SLOT = "daily_noti_work_slot";

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
            enqueueAt(context, DailyNotiStore.nextTriggerAt(hm[0], hm[1], now), now, false);
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
        enqueueAt(context, targetAt, now, false);
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
        // Huỷ cả hai tên: không biết chắc tên nào đang giữ work chờ.
        WorkManager manager = WorkManager.getInstance(context);
        for (String name : WORK_NAMES) {
            manager.cancelUniqueWork(name);
        }
    }

    /**
     * Hẹn cho mốc kế tiếp. {@link DailyNotiWorker} gọi lại sau mỗi lần chạy để tự duy trì chuỗi
     * hằng ngày — WorkManager không có cơ chế "chạy lại sau N giờ" cho OneTime nên phải tự nối.
     */
    static void enqueueNextDay(Context context) {
        int[] hm = DailyNotiStore.parseTime(context);
        long now = System.currentTimeMillis();
        enqueueAt(context, DailyNotiStore.nextTriggerAt(hm[0], hm[1], now), now, true);
    }

    /**
     * @param fromWorker gọi từ trong {@link DailyNotiWorker} hay không. Quyết định có được dọn
     *                   work ở slot cũ không: lúc worker chạy, slot cũ CHÍNH LÀ work chứa nó,
     *                   dọn đi là tự huỷ mình giữa chừng.
     */
    private static void enqueueAt(Context context, long targetAt, long now, boolean fromWorker) {
        long delay = Math.max(0L, targetAt - now);
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DailyNotiWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(TAG)
                .build();

        int activeSlot = PreferencesUtil.getInt(context, KEY_ACTIVE_SLOT, 1);
        int nextSlot = 1 - activeSlot;
        PreferencesUtil.putInt(context, KEY_ACTIVE_SLOT, nextSlot);

        WorkManager manager = WorkManager.getInstance(context);
        if (!fromWorker) {
            // Gọi từ app: slot cũ có thể còn work CHỜ từ lần mở trước. Không dọn thì mỗi lần mở
            // app lại thêm một work, hôm sau chúng nối tiếp nhau nhân đôi vô hạn.
            manager.cancelUniqueWork(WORK_NAMES[activeSlot]);
        }
        manager.enqueueUniqueWork(WORK_NAMES[nextSlot], ExistingWorkPolicy.REPLACE, request);
        DebugLog.d(TAG, "đã hẹn work \"" + WORK_NAMES[nextSlot] + "\" sau " + (delay / 1000) + "s");
    }
}

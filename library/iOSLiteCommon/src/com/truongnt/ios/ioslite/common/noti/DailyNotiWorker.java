package com.truongnt.ios.ioslite.common.noti;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.truongnt.ios.ioslite.common.debug.DebugLog;

/**
 * Chạy ở mốc giờ đã hẹn: bắn 2 notification rồi tự nối work cho ngày mai.
 *
 * <p>Bất biến cần giữ — thứ tự trong {@link #doWork()} không được đảo:
 * <ol>
 *   <li>Chặn nếu {@link DailyNotiStore#hasFiredToday(Context)} đã đúng — chốt "mỗi ngày tối đa
 *       1 lần" nằm ở đây, không nằm ở scheduler.</li>
 *   <li>Ghi {@link DailyNotiStore#markFiredToday(Context)} TRƯỚC khi bắn. Nếu bắn lỗi giữa
 *       chừng thì hôm đó coi như đã dùng lượt, KHÔNG được thử lại.</li>
 *   <li>Bắn noti (bỏ qua nếu tính năng bị tắt hoặc user chưa cấp quyền thông báo).</li>
 *   <li>Hẹn lại ngày mai, đặt trong finally để một lỗi ở bước 3 không làm đứt chuỗi hằng ngày.</li>
 * </ol>
 *
 * <p>Work này chạy ở main process. {@code :app} đã xin quyền POST_NOTIFICATIONS ở
 * PermissionActivity nên ở đây chỉ kiểm tra chứ không xin.
 */
public class DailyNotiWorker extends Worker {

    private static final String TAG = "DailyNoti";

    public DailyNotiWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // Chốt "1 lần/ngày" kiểm tra ở ĐÂY chứ không chỉ ở DailyNotiScheduler: scheduler dùng
        // ExistingWorkPolicy.REPLACE, nên nếu app được mở đúng lúc work đang chạy thì work có
        // thể bị huỷ rồi enqueue lại — không có chốt này sẽ bắn 2 lần trong cùng một ngày.
        if (DailyNotiStore.hasFiredToday(context)) {
            DebugLog.d(TAG, "hôm nay đã bắn rồi, bỏ qua");
            DailyNotiScheduler.enqueueNextDay(context);
            return Result.success();
        }

        // Đánh dấu ngay sau chốt trên — xem javadoc class về thứ tự bắt buộc.
        DailyNotiStore.markFiredToday(context);

        try {
            if (!DailyNotiStore.isEnabled(context)) {
                DebugLog.d(TAG, "bỏ qua: Remote Config đã tắt tính năng");
            } else if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                // Không phải lỗi: user chưa cấp quyền thông báo. Vẫn tính là đã dùng lượt
                // hôm nay để không dồn thông báo khi họ bật quyền trở lại.
                DebugLog.d(TAG, "bỏ qua: user chưa cấp quyền thông báo");
            } else {
                DailyNotiBuilder.postAll(context);
            }
        } catch (Exception e) {
            DebugLog.e(TAG, "bắn notification lỗi: " + e.getMessage(), e);
        } finally {
            DailyNotiScheduler.enqueueNextDay(context);
        }

        return Result.success();
    }
}

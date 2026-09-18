package com.truongnt.ios.ioslite.common.noti;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.truongnt.ios.ioslite.common.R;

/**
 * Dựng và bắn notification hằng ngày.
 *
 * <p>Hiện có 2 slot. Khi có nội dung thật: chỉ cần sửa 3 mảng {@link #SLOT_NOTI_IDS},
 * {@link #SLOT_TITLE_RES}, {@link #SLOT_TEXT_RES} và phần text trong
 * {@code res/values/strings.xml} — không phải đụng tới phần hẹn giờ.
 *
 * <p>Vì sao mở app bằng {@code getLaunchIntentForPackage} thay vì trỏ thẳng class:
 * {@code SplashActivity} nằm ở {@code :app}, mà iOSLiteCommon là thư viện được :app phụ thuộc
 * — tham chiếu ngược lên class của :app sẽ tạo phụ thuộc vòng. Cách này cũng không hardcode tên
 * class, nên đổi package/tên màn hình không làm chết intent. SplashActivity chính là activity
 * MAIN/LAUNCHER của app nên đây đúng là màn cần mở.
 */
final class DailyNotiBuilder {

    private static final String CHANNEL_ID = "daily_noti";

    // Mỗi slot một notification id riêng — trùng id thì cái sau ghi đè cái trước, chỉ còn 1 noti.
    private static final int[] SLOT_NOTI_IDS = {10001, 10002};
    private static final int[] SLOT_TITLE_RES = {
            R.string.daily_noti_slot1_title,
            R.string.daily_noti_slot2_title,
    };
    private static final int[] SLOT_TEXT_RES = {
            R.string.daily_noti_slot1_text,
            R.string.daily_noti_slot2_text,
    };

    private DailyNotiBuilder() {
    }

    @SuppressLint("MissingPermission")
    static void postAll(Context context) {
        ensureChannel(context);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        for (int i = 0; i < SLOT_NOTI_IDS.length; i++) {
            int notiId = SLOT_NOTI_IDS[i];
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_daily_noti)
                    .setContentTitle(context.getString(SLOT_TITLE_RES[i]))
                    .setContentText(context.getString(SLOT_TEXT_RES[i]))
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(context.getString(SLOT_TEXT_RES[i])))
                    .setContentIntent(launchAppIntent(context, notiId))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            manager.notify(notiId, builder.build());
        }
    }

    /**
     * Kênh thông báo riêng cho tính năng này. Tạo lại mỗi lần bắn là vô hại — hệ thống chỉ ghi
     * đè khi nội dung kênh thay đổi.
     */
    private static void ensureChannel(Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.daily_noti_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        manager.createNotificationChannel(channel);
    }

    private static PendingIntent launchAppIntent(Context context, int requestCode) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent == null) {
            // Hiếm: không resolve được launcher intent. Vẫn phải trả về PendingIntent hợp lệ
            // thay vì null, nếu không notification sẽ không bấm được.
            intent = new Intent();
            intent.setPackage(context.getPackageName());
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // FLAG_IMMUTABLE là bắt buộc từ API 31; setAutoCancel + FLAG_UPDATE_CURRENT để mốc
        // giờ/nội dung đổi trên Remote Config thì PendingIntent cũ được làm mới theo.
        return PendingIntent.getActivity(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}

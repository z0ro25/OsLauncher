package com.truongnt.ios.ioslite.common.noti;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.truongnt.ios.ioslite.common.R;

/**
 * Dựng và bắn notification hằng ngày.
 *
 * <p>Hiện có 2 slot. Thêm/bớt slot: sửa 3 mảng {@link #SLOT_NOTI_IDS}, {@link #SLOT_TITLE_RES},
 * {@link #SLOT_TEXT_RES} và text trong các thư mục {@code res/values-*} — không đụng phần hẹn giờ.
 *
 * <p>Dùng RemoteViews custom thay vì noti hệ thống vì thiết kế cần nút nền xanh bo tròn, mà
 * {@code addAction()} chỉ vẽ được chữ phẳng theo màu accent hệ thống. Đánh đổi: từ API 24 hệ
 * thống vẫn tự vẽ header (tên app + giờ) đè lên trên, không tắt được.
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

    /** Cộng vào noti id để ra requestCode riêng cho nút action; phải lớn hơn khoảng cách các id. */
    private static final int ACTION_REQUEST_OFFSET = 1000;

    /** Cạnh bitmap icon. Vẽ dư so với 40-44dp trong layout để không bị vỡ trên màn mật độ cao. */
    private static final int ICON_DP = 96;

    /** Bán kính bo góc icon theo % cạnh — giữ tỉ lệ bo giống nhau ở mọi mật độ màn. */
    private static final int ICON_CORNER_PERCENT = 22;

    private DailyNotiBuilder() {
    }

    @SuppressLint("MissingPermission")
    static void postAll(Context context) {
        ensureChannel(context);

        Bitmap icon = roundedAppIcon(context);
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        for (int i = 0; i < SLOT_NOTI_IDS.length; i++) {
            int notiId = SLOT_NOTI_IDS[i];
            String title = context.getString(SLOT_TITLE_RES[i]);
            String text = context.getString(SLOT_TEXT_RES[i]);
            PendingIntent openApp = launchAppIntent(context, notiId);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_daily_noti)
                    .setCustomContentView(
                            remoteViews(context, R.layout.layout_daily_noti_collapsed, icon, title, text, null))
                    .setCustomBigContentView(
                            remoteViews(context, R.layout.layout_daily_noti_expanded, icon, title, text,
                                    // Nút và thân noti cùng mở app nhưng PHẢI khác requestCode,
                                    // nếu không PendingIntent sau chỉ là alias của cái trước.
                                    launchAppIntent(context, notiId + ACTION_REQUEST_OFFSET)))
                    // Vẫn set title/text chuẩn: đồng hồ, Wear, Android Auto và trình đọc màn hình
                    // KHÔNG đọc RemoteViews, chỉ đọc 2 trường này.
                    .setContentTitle(title)
                    .setContentText(text)
                    .setContentIntent(openApp)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    // DecoratedCustomView giữ header hệ thống (tên app + giờ) rồi nhét layout của
                    // mình vào thân — bỏ nó thì trên nhiều đời máy noti ra trắng trơn.
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle());
            manager.notify(notiId, builder.build());
        }
    }

    /** Đổ dữ liệu vào layout noti; {@code actionIntent} null nghĩa là layout không có nút. */
    private static RemoteViews remoteViews(Context context, int layoutRes, Bitmap icon,
                                           String title, String text, PendingIntent actionIntent) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutRes);
        views.setTextViewText(R.id.tvDailyNotiTitle, title);
        views.setTextViewText(R.id.tvDailyNotiText, text);
        if (icon != null) {
            views.setImageViewBitmap(R.id.ivDailyNotiIcon, icon);
        }
        if (actionIntent != null) {
            views.setOnClickPendingIntent(R.id.tvDailyNotiAction, actionIntent);
        }
        return views;
    }

    /**
     * Icon app đã bo góc sẵn. Lấy qua PackageManager thay vì {@code R.mipmap} vì icon nằm ở
     * {@code :app}, library không đọc được R của module phụ thuộc ngược.
     *
     * <p>Bo góc phải làm trong bitmap: RemoteViews không nhận ShapeableImageView, cũng không
     * áp được clip/outline. Trả null khi lỗi — noti vẫn bắn, chỉ mất ảnh.
     */
    private static Bitmap roundedAppIcon(Context context) {
        try {
            int size = dp(context, ICON_DP);
            Drawable icon = context.getPackageManager().getApplicationIcon(context.getPackageName());

            Bitmap src = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas srcCanvas = new Canvas(src);
            icon.setBounds(0, 0, size, size);
            icon.draw(srcCanvas);

            return roundCorners(src, size * ICON_CORNER_PERCENT / 100f);
        } catch (Exception e) {
            return null;
        }
    }

    /** Bo góc bằng PorterDuff SRC_IN: vẽ mặt nạ bo góc trước rồi ghép ảnh gốc vào trong mặt nạ. */
    private static Bitmap roundCorners(Bitmap src, float radius) {
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF rect = new RectF(0, 0, src.getWidth(), src.getHeight());

        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, 0, 0, paint);
        return out;
    }

    private static int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics());
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

package com.amz.ios.launcher.widget.widgetprovider;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.amz.ios.launcher.IOSAppWidget;
import com.amz.ios.launcher.IconCache;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.widget.configure.PictureAppWidgetProviderConfigureActivity;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PictureAppWidgetProvider extends AppWidgetProvider implements IOSAppWidget {

    public static final String APP_WIDGET_PREFIX = "appwidget_";

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.picture_app_widget_provider);
        String configuredPath = sharedPreferences.getString("appwidget_" + appWidgetId, null);

        Bitmap bitmap = null;

        // 1) Ưu tiên ảnh người dùng tự chọn qua config activity (nếu có).
        if (configuredPath != null) {
            try {
                bitmap = decodeScaledFile(new File(URI.create(configuredPath)).getAbsolutePath());
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }

        long dateMillis = 0L;

        // 2) Nếu chưa cấu hình: tự lấy một ảnh gần đây (ưu tiên camera) + ngày chụp.
        if (bitmap == null) {
            PhotoInfo photo = loadRecentPhoto(context);
            if (photo != null && photo.path != null) {
                try {
                    bitmap = decodeScaledFile(photo.path);
                    dateMillis = photo.dateMillis;
                } catch (Throwable th) {
                    th.printStackTrace();
                }
            }
        }

        // 3) Fallback ảnh mẫu khi không có ảnh nào (chưa cấp quyền / thư viện trống).
        if (bitmap != null) {
            rv.setImageViewBitmap(R.id.widget_picture_image, bitmap);
        } else {
            rv.setImageViewResource(R.id.widget_picture_image, R.drawable.sample_photo_widget);
        }

        // Overlay ("ON THIS DAY" + ngày + scrim): hiện khi có ảnh thật + ngày hợp lệ.
        boolean hasDate = bitmap != null && dateMillis > 0L;
        if (hasDate) {
            rv.setTextViewText(R.id.widget_picture_date, formatPhotoDate(dateMillis));
            rv.setViewVisibility(R.id.widget_picture_title, View.VISIBLE);
            rv.setViewVisibility(R.id.widget_picture_date, View.VISIBLE);
            rv.setViewVisibility(R.id.widget_picture_scrim, View.VISIBLE);
        } else {
            rv.setViewVisibility(R.id.widget_picture_title, View.GONE);
            rv.setViewVisibility(R.id.widget_picture_date, View.GONE);
            rv.setViewVisibility(R.id.widget_picture_scrim, View.GONE);
        }

        // Bấm widget mở lại trình chọn ảnh.
        Intent intent = new Intent();
        intent.setClass(context, PictureAppWidgetProviderConfigureActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setAction("android.appwidget.action.APPWIDGET_CONFIGURE" + appWidgetId);
        try {
            rv.setOnClickPendingIntent(R.id.widget_picture_layout,
                    PendingIntent.getActivity(context, appWidgetId, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        } catch (Throwable ignored) {
        }

        // RemoteViews có GIỚI HẠN bộ nhớ bitmap (~1.5x màn hình, vd 15MB). Ảnh full-res (50MB) sẽ
        // ném IllegalArgumentException -> crash-loop tiến trình. Ảnh đã downscale ở decodeScaledFile,
        // nhưng vẫn bọc try-catch để mọi lỗi RemoteViews không giết launcher.
        try {
            appWidgetManager.updateAppWidget(appWidgetId, rv);
        } catch (Throwable th) {
            th.printStackTrace();
            try {
                // Fallback tối thiểu: bỏ ảnh, chỉ để layout trống còn hơn crash.
                RemoteViews safe = new RemoteViews(context.getPackageName(), R.layout.picture_app_widget_provider);
                safe.setImageViewResource(R.id.widget_picture_image, R.drawable.sample_photo_widget);
                appWidgetManager.updateAppWidget(appWidgetId, safe);
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Giới hạn cạnh dài nhất của ảnh khi đưa vào RemoteViews (widget hệ thống). RemoteViews chỉ cho
     * tổng bitmap ~15MB; ảnh camera thường 12MP (~50MB ARGB) sẽ làm crash. Cap 1024px -> tối đa
     * ~4MB, dư an toàn. Dùng inSampleSize (đọc bounds trước) để không nạp cả ảnh gốc vào RAM.
     */
    /**
     * Cache ảnh đã decode cho luồng PREVIEW trong khay widget.
     *
     * [TỐI ƯU] bindInflatedView() chạy trên MAIN THREAD và mỗi lần lại query MediaStore +
     * decodeFile 2 lượt (đo bounds, rồi decode thật). Trong khay widget, thẻ bị RecyclerView dựng
     * lại liên tục khi cuộn -> treo vài giây mỗi lần. Giữ lại bitmap đã decode theo ĐƯỜNG DẪN ảnh:
     * cùng ảnh thì dùng lại ngay, ảnh mới (người dùng chụp thêm) thì path đổi -> tự decode lại.
     * Chỉ giữ MỘT bitmap (ảnh gần nhất) nên không phình RAM.
     */
    private static String sCachedPhotoPath;
    private static Bitmap sCachedPhotoBitmap;

    private static Bitmap decodeScaledFileCached(String path) {
        if (path == null) return null;
        if (path.equals(sCachedPhotoPath)
                && sCachedPhotoBitmap != null && !sCachedPhotoBitmap.isRecycled()) {
            return sCachedPhotoBitmap;
        }
        Bitmap bmp = decodeScaledFile(path);
        if (bmp != null) {
            sCachedPhotoPath = path;
            sCachedPhotoBitmap = bmp;
        }
        return bmp;
    }

    private static Bitmap decodeScaledFile(String path) {
        if (path == null) return null;
        final int MAX_DIM = 1024;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        int w = bounds.outWidth, h = bounds.outHeight;
        if (w <= 0 || h <= 0) {
            // Không đọc được kích thước -> thử decode thường (có thể null).
            return BitmapFactory.decodeFile(path);
        }
        int sample = 1;
        while ((w / sample) > MAX_DIM || (h / sample) > MAX_DIM) {
            sample *= 2;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        return BitmapFactory.decodeFile(path, opts);
    }

    /**
     * Widget nội bộ (IOS_WIDGET_ID = -100) KHÔNG đi qua AppWidgetHost nên onUpdate()
     * không bao giờ được gọi -> RemoteViews.updateWidget() vô tác dụng. Launcher chỉ
     * inflate initialLayout một lần qua createView(). Vì vậy ta gắn ảnh trực tiếp vào
     * View đã inflate ngay tại thời điểm đó.
     */
    public static void bindInflatedView(Context context, View root) {
        if (root == null) return;
        ImageView imageView = root.findViewById(R.id.widget_picture_image);
        TextView titleView = root.findViewById(R.id.widget_picture_title);
        TextView dateView = root.findViewById(R.id.widget_picture_date);
        View scrim = root.findViewById(R.id.widget_picture_scrim);
        // Tương thích ngược nếu root chính là ImageView cũ.
        if (imageView == null && root instanceof ImageView) {
            imageView = (ImageView) root;
        }
        if (imageView == null) return;

        PhotoInfo photo = loadRecentPhoto(context);
        Bitmap bitmap = null;
        if (photo != null && photo.path != null) {
            try {
                // Dùng bản CACHE: hàm này chạy trên main thread và bị gọi lại mỗi lần thẻ preview
                // được dựng lại khi cuộn khay. Xem decodeScaledFileCached().
                bitmap = decodeScaledFileCached(photo.path);
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
        if (bitmap == null) {
            try {
                bitmap = BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.sample_photo_widget);
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }

        // Bo góc bằng Drawable tự vẽ (BitmapShader + drawRoundRect) theo bounds thực
        // tại thời điểm draw. Không phụ thuộc clipToOutline (launcher vẽ widget vào
        // hardware layer khiến clip không ăn) cũng không phụ thuộc view đã layout hay
        // chưa (post() có thể chạy khi w/h chưa sẵn sàng).
        if (bitmap != null) {
            float radiusPx = context.getResources().getDimension(R.dimen.widget_round_corner);
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            imageView.setImageDrawable(new RoundedCropDrawable(bitmap, radiusPx));
        }

        // Overlay ("ON THIS DAY" + ngày + scrim): chỉ hiện khi có ảnh THẬT từ thư viện + ngày
        // hợp lệ. Ảnh default (chưa cấp quyền / thư viện trống) -> ẩn toàn bộ overlay, chỉ còn ảnh.
        boolean hasDate = photo != null && photo.dateMillis > 0L && bitmap != null;
        if (titleView != null) {
            titleView.setVisibility(hasDate ? View.VISIBLE : View.GONE);
        }
        if (dateView != null) {
            if (hasDate) {
                dateView.setText(formatPhotoDate(photo.dateMillis));
                dateView.setVisibility(View.VISIBLE);
            } else {
                dateView.setVisibility(View.GONE);
            }
        }
        if (scrim != null) {
            scrim.setVisibility(hasDate ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Gắn action BẤM widget ảnh (đã đặt màn) mở trình chọn ảnh của widget
     * ({@link PictureAppWidgetProviderConfigureActivity}) — giống đường RemoteViews trong
     * {@link #updateWidget}, nhưng widget iOS (id -100) không đi qua AppWidgetHost nên phải
     * gắn OnClickListener trực tiếp sau khi inflate.
     *
     * <p>Gắn vào VIEW CON {@code widget_picture_layout} chứ KHÔNG phải host view, vì
     * {@code LauncherAppWidgetHostView.onTouchEvent} luôn trả false (không tự xử click) — chỉ view
     * con thường mới nhận được tap. CHỈ gọi ở đường đặt màn (LauncherAppWidgetHost.createView),
     * KHÔNG gọi ở preview (LiveWidgetPreviewHelper đã disableTouch để tap lọt xuống cell).
     */
    public static void attachOpenPickerClick(final Context context, View root,
                                             final int appWidgetId) {
        if (root == null) return;
        View clickTarget = root.findViewById(R.id.widget_picture_layout);
        if (clickTarget == null) clickTarget = root;
        clickTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent();
                    intent.setClass(context, PictureAppWidgetProviderConfigureActivity.class);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    context.startActivity(intent);
                } catch (Throwable ignored) {
                }
            }
        });
    }

    /**
     * Drawable vẽ ảnh center-crop có bo góc, tự tính theo bounds tại lúc draw. Bo góc
     * luôn đúng bất kể launcher clip hay chưa layout xong.
     */
    private static class RoundedCropDrawable extends android.graphics.drawable.Drawable {
        private final Bitmap mBitmap;
        private final float mRadius;
        private final Paint mPaint;
        private final BitmapShader mShader;
        private final Matrix mMatrix = new Matrix();
        private final RectF mRect = new RectF();
        private int mLastW = -1, mLastH = -1;

        RoundedCropDrawable(Bitmap bitmap, float radiusPx) {
            mBitmap = bitmap;
            mRadius = radiusPx;
            mShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setShader(mShader);
        }

        @Override
        public void draw(Canvas canvas) {
            android.graphics.Rect b = getBounds();
            int w = b.width(), h = b.height();
            if (w <= 0 || h <= 0) return;
            if (w != mLastW || h != mLastH) {
                // Center-crop: phủ kín khung, giữ tỉ lệ, canh giữa.
                float scale = Math.max(
                        w / (float) mBitmap.getWidth(),
                        h / (float) mBitmap.getHeight());
                float dx = b.left + (w - mBitmap.getWidth() * scale) * 0.5f;
                float dy = b.top + (h - mBitmap.getHeight() * scale) * 0.5f;
                mMatrix.setScale(scale, scale);
                mMatrix.postTranslate(dx, dy);
                mShader.setLocalMatrix(mMatrix);
                mRect.set(b.left, b.top, b.right, b.bottom);
                mLastW = w;
                mLastH = h;
            }
            canvas.drawRoundRect(mRect, mRadius, mRadius, mPaint);
        }

        @Override
        public int getIntrinsicWidth() {
            return mBitmap.getWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            return mBitmap.getHeight();
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }

    /** Định dạng ngày chụp kiểu design "June 7, 2025" (Locale.ENGLISH cho khớp overlay "ON THIS DAY"). */
    private static String formatPhotoDate(long millis) {
        try {
            return new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
                    .format(new Date(millis));
        } catch (Throwable th) {
            return "";
        }
    }

    /** Đường dẫn + ngày chụp của một ảnh. */
    private static class PhotoInfo {
        final String path;
        final long dateMillis;

        PhotoInfo(String path, long dateMillis) {
            this.path = path;
            this.dateMillis = dateMillis;
        }
    }

    /**
     * Lấy ảnh gần đây nhất: ưu tiên ảnh chụp từ camera (DCIM/Camera), nếu không có
     * thì lấy ảnh mới nhất bất kỳ trong thư viện. Trả về null nếu chưa cấp quyền
     * hoặc thư viện trống.
     */
    // [TỐI ƯU] Cache ngắn hạn cho kết quả query MediaStore.
    //   loadRecentPhoto() chạy trên MAIN THREAD và query tới 2 lượt (camera, rồi toàn thư viện).
    //   Trong khay widget, thẻ preview bị dựng lại liên tục khi cuộn -> query dồn dập gây giật/treo.
    //   Giữ kết quả trong 30 giây: đủ để cuộn qua-lại mượt, mà vẫn cập nhật khi người dùng chụp ảnh
    //   mới rồi quay lại khay. TTL ngắn nên không cần lắng nghe ContentObserver.
    private static PhotoInfo sCachedPhotoInfo;
    private static long sCachedPhotoAtMs;
    private static final long PHOTO_CACHE_TTL_MS = 30_000L;

    private static PhotoInfo loadRecentPhoto(Context context) {
        String permission = Build.VERSION.SDK_INT >= 33
                ? "android.permission.READ_MEDIA_IMAGES"
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(context, permission)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        long now = android.os.SystemClock.uptimeMillis();
        if (sCachedPhotoInfo != null && (now - sCachedPhotoAtMs) < PHOTO_CACHE_TTL_MS) {
            return sCachedPhotoInfo;
        }

        // 1) Ưu tiên ảnh từ camera.
        PhotoInfo photo = queryRecentPhoto(context, true);
        if (photo == null) {
            // 2) Fallback: ảnh mới nhất bất kỳ.
            photo = queryRecentPhoto(context, false);
        }
        if (photo != null) {
            sCachedPhotoInfo = photo;
            sCachedPhotoAtMs = now;
        }
        return photo;
    }

    /**
     * Truy vấn một ảnh mới nhất. Nếu cameraOnly = true, chỉ lấy ảnh nằm trong
     * DCIM/Camera (ảnh do máy ảnh chụp).
     */
    private static PhotoInfo queryRecentPhoto(Context context, boolean cameraOnly) {
        Cursor cursor = null;
        try {
            String[] projection = {
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.DATE_ADDED
            };
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            String selection = null;
            String[] selectionArgs = null;
            if (cameraOnly) {
                if (Build.VERSION.SDK_INT >= 29) {
                    selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
                    selectionArgs = new String[]{"%DCIM/Camera%"};
                } else {
                    selection = MediaStore.Images.Media.DATA + " LIKE ?";
                    selectionArgs = new String[]{"%DCIM/Camera%"};
                }
            }

            String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";
            if (Build.VERSION.SDK_INT >= 26) {
                Bundle args = new Bundle();
                args.putInt("android:query-arg-limit", 1);
                args.putString("android:query-arg-sql-sort-order", sortOrder);
                if (selection != null) {
                    args.putString("android:query-arg-sql-selection", selection);
                    args.putStringArray("android:query-arg-sql-selection-args", selectionArgs);
                }
                cursor = context.getContentResolver().query(uri, projection, args, null);
            } else {
                cursor = context.getContentResolver().query(uri, projection,
                        selection, selectionArgs, sortOrder + " LIMIT 1");
            }

            if (cursor != null && cursor.moveToFirst()) {
                int dataIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                int takenIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                int addedIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED);
                String path = dataIdx >= 0 ? cursor.getString(dataIdx) : null;
                if (path == null) return null;

                long dateMillis = 0L;
                if (takenIdx >= 0) {
                    dateMillis = cursor.getLong(takenIdx); // đã tính bằng mili-giây
                }
                if (dateMillis <= 0L && addedIdx >= 0) {
                    dateMillis = cursor.getLong(addedIdx) * 1000L; // DATE_ADDED tính bằng giây
                }
                return new PhotoInfo(path, dateMillis);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        for (int appWidgetId : appWidgetIds){
            String key = APP_WIDGET_PREFIX + appWidgetId;
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.remove(key);
            if (!editor.commit()) editor.apply();
            new File(new ContextWrapper(context).getDir("image", 0), key.replace("/", "_") + ".jpg").delete();
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        for (int appWidgetId : appWidgetIds){
            Log.e("Update Widget", String.valueOf(appWidgetId));
            updateWidget(context,appWidgetManager,appWidgetId);
        }
    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public int getPreviewImage() {
        return 0;
    }

    @Override
    public int getIcon() {
        return 0;
    }

    @Override
    public int getWidgetLayout() {
        return 0;
    }

    @Override
    public ComponentName getConfigure() {
        return null;
    }

    @Override
    public int getSpanX() {
        return 2;
    }

    @Override
    public int getSpanY() {
        return 2;
    }

    @Override
    public int getMinSpanX() {
        return 2;
    }

    @Override
    public int getMinSpanY() {
        return 2;
    }

    @Override
    public int getResizeMode() {
        return 0;
    }
}

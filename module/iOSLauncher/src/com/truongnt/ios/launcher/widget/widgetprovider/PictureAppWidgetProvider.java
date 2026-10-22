package com.truongnt.ios.launcher.widget.widgetprovider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;


import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.truongnt.ios.launcher.IOSAppWidget;
import com.truongnt.ios.launcher.IconCache;
import com.truongnt.ios.launcher.R;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PictureAppWidgetProvider extends AppWidgetProvider implements IOSAppWidget {

    public static final String APP_WIDGET_PREFIX = "appwidget_";

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId){
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.picture_app_widget_provider);

        Bitmap bitmap = null;
        long dateMillis = 0L;

        // Ảnh do người dùng chọn qua Photo Picker (dùng chung mọi widget ảnh), không đọc thư viện.
        PhotoInfo photo = loadChosenPhoto(context);
        if (photo != null && photo.path != null) {
            try {
                bitmap = decodeScaledFile(photo.path);
                dateMillis = photo.dateMillis;
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }

        // Fallback ảnh mẫu khi chưa chọn ảnh.
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

        // Chưa chọn ảnh -> phủ lời mời "chọn ảnh" lên ảnh mẫu.
        rv.setViewVisibility(R.id.widget_picture_permission,
                bitmap != null ? View.GONE : View.VISIBLE);

        // RemoteViews không mở được picker có kết quả -> mở Launcher kèm action, Launcher mở picker.
        try {
            rv.setOnClickPendingIntent(R.id.widget_picture_layout,
                    PendingIntent.getActivity(context, appWidgetId,
                            PhotoWidgetPicker.buildLauncherPickIntent(context),
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
    // Cache bitmap cho preview khay widget (bindInflatedView chạy main thread, bị gọi lại khi cuộn).
    // File ảnh luôn cùng path -> đổi ảnh phải gọi invalidatePhotoCache(), không tự nhận ra được.
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

        PhotoInfo photo = loadChosenPhoto(context);
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
        boolean hasRealPhoto = bitmap != null;
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

        // Overlay ngày chỉ hiện khi có ảnh đã chọn + ngày chụp đọc được; ảnh mẫu thì ẩn hết.
        boolean hasDate = hasRealPhoto && photo.dateMillis > 0L;
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

        // Chưa chọn ảnh -> phủ lời mời "chọn ảnh" (cùng ý nghĩa với updateWidget).
        View permissionOverlay = root.findViewById(R.id.widget_picture_permission);
        if (permissionOverlay != null) {
            permissionOverlay.setVisibility(hasRealPhoto ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Bấm widget ảnh đã đặt màn -> mở Photo Picker để chọn/đổi ảnh. Gắn vào view con vì
     * LauncherAppWidgetHostView không tự xử click; KHÔNG gọi ở preview khay (đã disableTouch).
     */
    public static void attachOpenGalleryClick(final Context context, View root,
                                             final int appWidgetId) {
        if (root == null) return;
        View clickTarget = root.findViewById(R.id.widget_picture_layout);
        if (clickTarget == null) clickTarget = root;
        clickTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (context instanceof android.app.Activity) {
                        PhotoWidgetPicker.launch((android.app.Activity) context);
                    } else {
                        context.startActivity(PhotoWidgetPicker.buildLauncherPickIntent(context));
                    }
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

    // Ảnh người dùng đã chọn qua Photo Picker; null khi chưa chọn. Chỉ là 1 file nội bộ nên đọc thẳng.
    private static PhotoInfo loadChosenPhoto(Context context) {
        String path = PhotoWidgetStore.photoPath(context);
        if (path == null) return null;
        return new PhotoInfo(path, PhotoWidgetStore.dateMillis(context));
    }

    /**
     * Dọn dữ liệu của widget bị xoá.
     *
     * Widget không còn dùng ảnh "đã chọn" nữa (nay luôn lấy ảnh mới nhất), nhưng vẫn giữ hàm này để
     * quét sạch pref + file ảnh crop CÒN SÓT từ các bản trước — máy đã dùng bản cũ sẽ có chúng.
     */
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

    // Gọi ngay sau khi lưu ảnh mới: file luôn cùng path nên cache bitmap không tự nhận ra ảnh đã đổi.
    static void invalidatePhotoCache() {
        sCachedPhotoPath = null;
        sCachedPhotoBitmap = null;
    }

    /** Vẽ lại mọi widget ảnh (gọi ở Launcher.onResume và sau khi chọn ảnh mới). */
    public static void refreshAll(Context context) {

        // 1) Widget qua AppWidgetHost thật (nếu có).
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            Class<?>[] providers = {
                    PictureAppWidgetProvider.class,
                    PictureMediumWidgetProvider.class,
                    PictureLargeWidgetProvider.class,
            };
            for (Class<?> provider : providers) {
                int[] ids = manager.getAppWidgetIds(new ComponentName(context, provider));
                if (ids == null) continue;
                for (int id : ids) {
                    updateWidget(context, manager, id);
                }
            }
        } catch (Throwable ignored) {
            // Không có widget ảnh nào, hoặc AppWidgetManager chưa sẵn sàng -> bỏ qua.
        }

        // 2) Widget NỘI BỘ (đường thực tế của launcher này): chúng không đi qua onUpdate mà được
        //    bindInflatedView() gắn nội dung MỘT LẦN lúc inflate (xem LauncherAppWidgetHost:139),
        //    nên updateWidget ở trên không chạm tới. Phải tự duyệt cây view và bind lại.
        try {
            View root = ((android.app.Activity) context).getWindow().getDecorView();
            rebindInflatedPictureWidgets(context, root);
        } catch (Throwable ignored) {
            // context không phải Activity (vd gọi từ receiver) -> bỏ qua nhánh này.
        }
    }

    /** Duyệt cây view, gặp widget ảnh nội bộ nào thì bind lại bằng ảnh mới nhất. */
    private static void rebindInflatedPictureWidgets(Context context, View view) {
        if (view == null) return;

        // [BUG FIX] Bản trước nhận diện bằng view.findViewById(...) != null. Sai vì findViewById tìm
        //   TRONG CẢ CÂY CON: decorView cũng "chứa" các id đó nên khớp ngay ở lần gọi đầu -> bind
        //   nhầm vào decorView rồi return, không duyệt xuống widget thật. Chạy được với MỘT widget
        //   chỉ là may (bindInflatedView tra view theo id nên vô tình vẫn tìm đúng), nhưng có nhiều
        //   widget ảnh thì chỉ cái đầu tiên được cập nhật.
        //   Nay nhận diện bằng KIỂU host view — thứ chỉ đúng ở đúng một cấp trong cây.
        if (view instanceof com.truongnt.ios.launcher.LauncherAppWidgetHostView) {
            com.truongnt.ios.launcher.LauncherAppWidgetProviderInfo info =
                    ((com.truongnt.ios.launcher.LauncherAppWidgetHostView) view)
                            .getLauncherAppWidgetProviderInfo();
            if (info != null && info.provider != null
                    && isPictureProvider(info.provider.getClassName())) {
                bindInflatedView(context, view);
            }
            return;   // host view là một widget hoàn chỉnh, không đi sâu hơn
        }

        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                rebindInflatedPictureWidgets(context, group.getChildAt(i));
            }
        }
    }

    /** Tên class có phải một trong 3 provider ảnh không. */
    private static boolean isPictureProvider(String className) {
        return PictureAppWidgetProvider.class.getName().equals(className)
                || PictureMediumWidgetProvider.class.getName().equals(className)
                || PictureLargeWidgetProvider.class.getName().equals(className);
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

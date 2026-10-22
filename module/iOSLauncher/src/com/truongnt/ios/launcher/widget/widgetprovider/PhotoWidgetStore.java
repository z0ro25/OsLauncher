package com.truongnt.ios.launcher.widget.widgetprovider;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Ảnh DUY NHẤT dùng chung cho mọi widget ảnh (desktop 3 cỡ + trang trái), do người dùng chọn qua
 * Photo Picker. Chép bản thu nhỏ vào bộ nhớ riêng của app nên không cần quyền đọc thư viện ảnh.
 */
public final class PhotoWidgetStore {

    private static final String PREFS = "photo_widget_store";
    private static final String KEY_DATE = "date_millis";
    private static final String FILE_NAME = "photo_widget.jpg";
    private static final int MAX_DIM = 1024;

    private PhotoWidgetStore() {
    }

    public static File photoFile(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }

    public static boolean hasPhoto(Context context) {
        return photoFile(context).isFile();
    }

    /** Đường dẫn ảnh đã lưu, null khi chưa chọn. */
    public static String photoPath(Context context) {
        File file = photoFile(context);
        return file.isFile() ? file.getAbsolutePath() : null;
    }

    /** Ngày chụp (ms) của ảnh đã lưu; 0 = không rõ -> widget ẩn overlay ngày. */
    public static long dateMillis(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_DATE, 0L);
    }

    /**
     * Thu nhỏ ảnh từ uri rồi ghi đè ảnh cũ. Đọc I/O nặng -> chỉ gọi ở luồng nền.
     * Ghi ra file tạm rồi rename để widget không bao giờ đọc phải file ghi dở.
     */
    public static boolean save(Context context, Uri uri) {
        Context app = context.getApplicationContext();
        try {
            Bitmap bitmap = decodeScaled(app, uri);
            if (bitmap == null) return false;
            int orientation = readOrientation(app, uri);
            bitmap = rotate(bitmap, orientation);

            File target = photoFile(app);
            File tmp = new File(app.getFilesDir(), FILE_NAME + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            }
            if (!tmp.renameTo(target)) {
                target.delete();
                if (!tmp.renameTo(target)) return false;
            }
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putLong(KEY_DATE, readDateMillis(app, uri)).apply();
            return true;
        } catch (Throwable th) {
            th.printStackTrace();
            return false;
        }
    }

    private static Bitmap decodeScaled(Context context, Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        int w = bounds.outWidth, h = bounds.outHeight;
        if (w <= 0 || h <= 0) return null;
        int sample = 1;
        while ((w / sample) > MAX_DIM * 2 || (h / sample) > MAX_DIM * 2) {
            sample *= 2;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap decoded;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            decoded = BitmapFactory.decodeStream(in, null, opts);
        }
        if (decoded == null) return null;
        // inSampleSize chỉ chia luỹ thừa 2 -> scale nốt cho cạnh dài đúng MAX_DIM.
        float scale = MAX_DIM / (float) Math.max(decoded.getWidth(), decoded.getHeight());
        if (scale >= 1f) return decoded;
        Bitmap scaled = Bitmap.createScaledBitmap(decoded,
                Math.round(decoded.getWidth() * scale), Math.round(decoded.getHeight() * scale), true);
        if (scaled != decoded) decoded.recycle();
        return scaled;
    }

    private static int readOrientation(Context context, Uri uri) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) return ExifInterface.ORIENTATION_NORMAL;
            return new ExifInterface(in).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (Throwable th) {
            return ExifInterface.ORIENTATION_NORMAL;
        }
    }

    // File lưu ra không mang EXIF -> phải xoay pixel ngay, nếu không ảnh dọc hiện bị nằm ngang.
    private static Bitmap rotate(Bitmap bitmap, int orientation) {
        int degrees;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90: degrees = 90; break;
            case ExifInterface.ORIENTATION_ROTATE_180: degrees = 180; break;
            case ExifInterface.ORIENTATION_ROTATE_270: degrees = 270; break;
            default: return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotated != bitmap) bitmap.recycle();
        return rotated;
    }

    // Ưu tiên EXIF DateTimeOriginal, sau đó cột DATE_TAKEN của uri; không có thì trả 0.
    private static long readDateMillis(Context context, Uri uri) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in != null) {
                ExifInterface exif = new ExifInterface(in);
                String raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
                if (raw == null) raw = exif.getAttribute(ExifInterface.TAG_DATETIME);
                if (raw != null) {
                    Date date = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(raw);
                    if (date != null && date.getTime() > 0L) return date.getTime();
                }
            }
        } catch (Throwable ignored) {
        }
        try (Cursor cursor = context.getContentResolver().query(uri,
                new String[]{MediaStore.Images.ImageColumns.DATE_TAKEN}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                long taken = cursor.getLong(0);
                if (taken > 0L) return taken;
            }
        } catch (Throwable ignored) {
        }
        return 0L;
    }
}

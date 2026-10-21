package com.ezla.oslauncher.themes.test;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Ghi log ra Downloads/OsLauncherLogs/log_yyyyMMdd.txt (1 file/ngày) qua MediaStore, đồng thời in Logcat.
 * Chỉ ghi file trên Android 10+ (không cần quyền); dưới 10 chỉ in Logcat.
 */
public final class FileLogger {
    private static final String TAG = "FileLogger";
    private static final String DIR = Environment.DIRECTORY_DOWNLOADS + "/OsLauncherLogs/";

    // 1 thread để các dòng log ghi đúng thứ tự và không chặn main thread.
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private static Context appContext;
    private static String cachedDay;
    private static Uri cachedUri;

    private FileLogger() {
    }

    /** Gọi 1 lần (vd trong Application.onCreate) trước khi log; tự cài bắt crash. */
    public static synchronized void init(Context context) {
        if (appContext != null) return;
        appContext = context.getApplicationContext();
        installCrashHandler();
    }

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
        enqueue("D", tag, msg, null);
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
        enqueue("I", tag, msg, null);
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
        enqueue("W", tag, msg, null);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
        enqueue("E", tag, msg, null);
    }

    public static void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, tr);
        enqueue("E", tag, msg, tr);
    }

    public static void e(String tag, Throwable tr) {
        e(tag, String.valueOf(tr.getMessage()), tr);
    }

    private static void enqueue(String level, String tag, String msg, Throwable tr) {
        // Định dạng ngay lúc gọi để giữ đúng thời điểm + tên thread gốc.
        String line = format(level, tag, msg, tr);
        try {
            EXECUTOR.execute(() -> write(line));
        } catch (Exception ignored) {
        }
    }

    // Chuỗi handler: ghi crash vào file rồi trả lại handler cũ để Crashlytics/hệ thống vẫn xử lý.
    private static void installCrashHandler() {
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            String line = format("CRASH", "Uncaught", "thread=" + thread.getName(), ex);
            Log.e(TAG, "Uncaught exception", ex);
            try {
                // Process sắp chết: phải chờ ghi xong (sau các log đang xếp hàng) mới chuyển tiếp.
                EXECUTOR.submit(() -> write(line)).get(2, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
            if (previous != null) previous.uncaughtException(thread, ex);
        });
    }

    private static String format(String level, String tag, String msg, Throwable tr) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
        StringBuilder sb = new StringBuilder()
                .append(time).append(' ')
                .append(level).append('/').append(tag)
                .append(" [").append(Thread.currentThread().getName()).append("] ")
                .append(msg).append('\n');
        if (tr != null) {
            StringWriter sw = new StringWriter();
            tr.printStackTrace(new PrintWriter(sw));
            sb.append(sw).append('\n');
        }
        return sb.toString();
    }

    // Chỉ chạy trên EXECUTOR nên cachedDay/cachedUri không cần đồng bộ thêm.
    private static void write(String line) {
        if (appContext == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
        try {
            Uri uri = getTodayUri();
            if (uri == null) return;
            try (OutputStream os = appContext.getContentResolver().openOutputStream(uri, "wa")) {
                if (os != null) os.write(line.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ex) {
            cachedUri = null;
            Log.e(TAG, "Ghi log thất bại", ex);
        }
    }

    private static Uri getTodayUri() {
        String day = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        if (day.equals(cachedDay) && cachedUri != null) return cachedUri;

        String name = "log_" + day + ".txt";
        ContentResolver resolver = appContext.getContentResolver();
        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        Uri uri = null;

        // Chỉ thấy được file do chính app tạo; cài lại app thì sẽ tạo file mới "log_x (1).txt".
        try (Cursor c = resolver.query(collection,
                new String[]{MediaStore.Downloads._ID},
                MediaStore.Downloads.DISPLAY_NAME + "=? AND " + MediaStore.Downloads.RELATIVE_PATH + "=?",
                new String[]{name, DIR}, null)) {
            if (c != null && c.moveToFirst()) {
                uri = Uri.withAppendedPath(collection, String.valueOf(c.getLong(0)));
            }
        }

        if (uri == null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, name);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
            values.put(MediaStore.Downloads.RELATIVE_PATH, DIR);
            uri = resolver.insert(collection, values);
        }

        cachedDay = day;
        cachedUri = uri;
        return uri;
    }
}

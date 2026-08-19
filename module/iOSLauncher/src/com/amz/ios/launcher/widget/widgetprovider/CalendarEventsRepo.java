package com.amz.ios.launcher.widget.widgetprovider;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Nguồn dữ liệu sự kiện lịch DÙNG CHUNG cho họ widget Calendar kiểu iOS (Up Next nhỏ/vừa/lớn).
 * <p>
 * Tách riêng để 4 provider gọi chung 1 chỗ; KHÔNG sửa {@code CalendarWidget.java} (Today View)
 * để tránh ảnh hưởng màn khác. Chỉ đọc {@link CalendarContract.Instances} trong 1 khoảng thời
 * gian [from, to) và trả danh sách sự kiện tối giản (tên/giờ bắt đầu-kết thúc/màu/cả-ngày).
 */
public final class CalendarEventsRepo {

    private CalendarEventsRepo() {
    }

    /** 1 sự kiện tối giản cho widget (không phụ thuộc model của Today View). */
    public static final class Ev {
        public final long startMs;
        public final long endMs;
        public final String title;
        public final int color;
        public final boolean allDay;

        Ev(long startMs, long endMs, String title, int color, boolean allDay) {
            this.startMs = startMs;
            this.endMs = endMs;
            this.title = title;
            this.color = color;
            this.allDay = allDay;
        }
    }

    /** Đã cấp quyền đọc lịch chưa (Android 6+ cần cấp runtime). */
    public static boolean hasPermission(Context ctx) {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Mốc 00:00 hôm nay (+ offsetDays ngày) theo múi giờ máy, tính bằng millis. */
    public static long startOfDay(int offsetDays) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_MONTH, offsetDays);
        return c.getTimeInMillis();
    }

    /**
     * Đọc các sự kiện có instance nằm trong [fromMs, toMs), sắp theo thời điểm bắt đầu.
     * Chưa cấp quyền hoặc lỗi -> trả list rỗng (không ném ngoại lệ).
     */
    public static List<Ev> queryEvents(Context ctx, long fromMs, long toMs) {
        ArrayList<Ev> out = new ArrayList<>();
        if (!hasPermission(ctx)) return out;

        Cursor c = null;
        try {
            Uri.Builder b = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(b, fromMs);
            ContentUris.appendId(b, toMs);
            // Dùng chính cột Instances để đúng thời điểm từng lần lặp (begin/end), lọc bản ghi hiển thị.
            c = ctx.getContentResolver().query(
                    b.build(),
                    new String[]{
                            CalendarContract.Instances.EVENT_ID,
                            CalendarContract.Instances.TITLE,
                            CalendarContract.Instances.BEGIN,
                            CalendarContract.Instances.END,
                            CalendarContract.Instances.ALL_DAY,
                            CalendarContract.Instances.DISPLAY_COLOR
                    },
                    "visible=1",
                    null,
                    CalendarContract.Instances.BEGIN + " ASC");
            if (c != null) {
                int iTitle = c.getColumnIndex(CalendarContract.Instances.TITLE);
                int iBegin = c.getColumnIndex(CalendarContract.Instances.BEGIN);
                int iEnd = c.getColumnIndex(CalendarContract.Instances.END);
                int iAllDay = c.getColumnIndex(CalendarContract.Instances.ALL_DAY);
                int iColor = c.getColumnIndex(CalendarContract.Instances.DISPLAY_COLOR);
                while (c.moveToNext()) {
                    long start = iBegin >= 0 ? c.getLong(iBegin) : 0L;
                    long end = iEnd >= 0 ? c.getLong(iEnd) : start;
                    boolean allDay = iAllDay >= 0 && c.getInt(iAllDay) == 1;
                    int color = iColor >= 0 ? c.getInt(iColor) : 0;
                    if (color == 0) color = 0xFF9C9CB0; // màu mặc định khi lịch không đặt màu
                    String title = iTitle >= 0 ? c.getString(iTitle) : null;
                    if (title == null || title.trim().isEmpty()) title = "(Không tiêu đề)";
                    out.add(new Ev(start, end, title, color, allDay));
                }
            }
        } catch (Throwable ignored) {
            // Thiếu quyền/không có provider lịch -> coi như rỗng.
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (Throwable ignored) {
                }
            }
        }
        return out;
    }
}

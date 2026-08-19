package com.amz.ios.launcher.widget.view;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.amz.ios.launcher.R;
import com.amz.ios.launcher.widget.widgetprovider.CalendarEventsRepo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Widget lịch "Up Next" kiểu iOS, tự vẽ bằng Canvas (widget nội bộ IOS_WIDGET id -100 được
 * launcher inflate thành View thật — xem {@link AnalogClockView}). Một class dùng chung cho
 * 3 cỡ qua thuộc tính {@code cunVariant}: small (2x2), medium (4x2), large (4x4).
 *
 * Dữ liệu sự kiện lấy từ {@link CalendarEventsRepo} (cần quyền READ_CALENDAR). Thiếu quyền →
 * vẽ gợi ý "chạm để cấp quyền"; chạm khi đã có quyền → mở app Lịch. Tự truy vấn lại mỗi 60s.
 *
 * SMALL vẽ 1 cột (hôm nay + tối đa 2 chip). MEDIUM/LARGE dùng chung {@link #drawTwoColumns}
 * (2 cột hôm nay / ngày mai); large cao gấp đôi nên hiển thị nhiều sự kiện mỗi cột hơn.
 */
public class CalendarUpNextView extends View implements View.OnClickListener {

    public static final int VARIANT_SMALL = 0;
    public static final int VARIANT_MEDIUM = 1;
    public static final int VARIANT_LARGE = 2;

    // Bảng màu iOS.
    private static final int CARD_BG_COLOR = Color.WHITE;
    private static final int WEEKDAY_COLOR = 0xFFFF3B30; // đỏ
    private static final int DAY_COLOR = 0xFF1C1C1E;      // gần đen
    private static final int SECONDARY_COLOR = 0xFF8E8E93; // xám
    private static final int CHIP_TEXT_COLOR = 0xFF1C1C1E;

    private final float mDensity;
    private int mVariant = VARIANT_SMALL;

    private final Paint mBgPaint;
    private final Paint mChipBgPaint;
    private final Paint mAccentPaint;
    private final TextPaint mWeekdayPaint;
    private final TextPaint mDayPaint;
    private final TextPaint mTitlePaint;
    private final TextPaint mTimePaint;
    private final TextPaint mHintPaint;
    private final Rect mTextBound = new Rect();
    private final RectF mRectF = new RectF();

    private final List<CalendarEventsRepo.Ev> mEvents = new ArrayList<>();
    private boolean mHasPermission = false;

    private final Runnable mTicker = new Runnable() {
        @Override
        public void run() {
            reloadEvents();
            invalidate();
            postDelayed(this, 60_000L);
        }
    };

    public CalendarUpNextView(Context context) {
        this(context, null);
    }

    public CalendarUpNextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalendarUpNextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDensity = getResources().getDisplayMetrics().density;

        mBgPaint = fill(CARD_BG_COLOR);
        mChipBgPaint = fill(Color.WHITE);
        mAccentPaint = fill(WEEKDAY_COLOR);

        mWeekdayPaint = text(WEEKDAY_COLOR, true, Paint.Align.LEFT);
        mDayPaint = text(DAY_COLOR, true, Paint.Align.LEFT);
        mTitlePaint = text(CHIP_TEXT_COLOR, true, Paint.Align.LEFT);
        mTimePaint = text(SECONDARY_COLOR, false, Paint.Align.LEFT);
        mHintPaint = text(SECONDARY_COLOR, false, Paint.Align.LEFT);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CalendarUpNextView);
            mVariant = a.getInt(R.styleable.CalendarUpNextView_cunVariant, VARIANT_SMALL);
            a.recycle();
        }
        setOnClickListener(this);
    }

    private Paint fill(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStyle(Paint.Style.FILL);
        return p;
    }

    private TextPaint text(int color, boolean bold, Paint.Align align) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setFakeBoldText(bold);
        p.setTextAlign(align);
        return p;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(mTicker);
        post(mTicker);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mTicker);
    }

    /** Truy vấn sự kiện từ bây giờ tới hết ngày mai (cửa sổ đủ cho các cỡ small/medium). */
    private void reloadEvents() {
        mHasPermission = CalendarEventsRepo.hasPermission(getContext());
        mEvents.clear();
        if (!mHasPermission) return;
        long now = System.currentTimeMillis();
        long to = CalendarEventsRepo.startOfDay(2); // 00:00 ngày kia
        for (CalendarEventsRepo.Ev ev : CalendarEventsRepo.queryEvents(getContext(), now, to)) {
            if (ev.endMs > now) mEvents.add(ev);
        }
    }

    @Override
    public void onClick(View v) {
        if (!mHasPermission) {
            requestCalendarPermission();
        } else {
            openCalendarApp();
        }
    }

    /** Yêu cầu quyền lịch nếu context là Activity; nếu không, mở màn cài đặt app. */
    private void requestCalendarPermission() {
        Activity act = findActivity(getContext());
        if (act != null) {
            try {
                ActivityCompat.requestPermissions(act,
                        new String[]{android.Manifest.permission.READ_CALENDAR}, 4021);
                return;
            } catch (Throwable ignored) {
            }
        }
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + getContext().getPackageName()))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
        } catch (Throwable ignored) {
        }
    }

    private void openCalendarApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setData(CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        } catch (Throwable ignored) {
        }
    }

    private static Activity findActivity(Context c) {
        while (c instanceof ContextWrapper) {
            if (c instanceof Activity) return (Activity) c;
            c = ((ContextWrapper) c).getBaseContext();
        }
        return null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // Thẻ nền trắng bo góc lấp đầy ô.
        float inset = mDensity * 1f;
        float radius = mDensity * 20f;
        mRectF.set(inset, inset, w - inset, h - inset);
        canvas.drawRoundRect(mRectF, radius, radius, mBgPaint);

        switch (mVariant) {
            case VARIANT_MEDIUM:
                // 4x2: 2 cột, tối đa 2 sự kiện mỗi cột.
                drawTwoColumns(canvas, mRectF, 2);
                break;
            case VARIANT_LARGE:
                // 4x4: cùng bố cục 2 cột nhưng cao gấp đôi -> tối đa 4 sự kiện mỗi cột.
                drawTwoColumns(canvas, mRectF, 4);
                break;
            case VARIANT_SMALL:
            default:
                drawSmall(canvas, mRectF);
                break;
        }
    }

    /** Vẽ header "thứ (đỏ) + số ngày lớn (đen)" tại (left, top); trả về Y đáy header. */
    private float drawTodayHeader(Canvas canvas, float left, float top, float width) {
        Calendar cal = Calendar.getInstance();
        Locale locale = Locale.getDefault();
        String weekday = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, locale);
        if (weekday == null) weekday = "";
        weekday = weekday.toUpperCase(locale);
        String dayNum = Integer.toString(cal.get(Calendar.DAY_OF_MONTH));

        mWeekdayPaint.setTextSize(mDensity * 11f);
        mWeekdayPaint.getTextBounds("MG", 0, 2, mTextBound);
        float y = top + mTextBound.height();
        canvas.drawText(ellipsize(weekday, mWeekdayPaint, width), left, y, mWeekdayPaint);

        mDayPaint.setTextSize(mDensity * 28f);
        mDayPaint.getTextBounds("30", 0, 2, mTextBound);
        y += mDensity * 3f + mTextBound.height();
        canvas.drawText(dayNum, left, y, mDayPaint);
        return y;
    }

    /** Vẽ header 1 dòng chữ đỏ (vd "TOMORROW") tại (left, top); trả về Y đáy header. */
    private float drawLabelHeader(Canvas canvas, String label, float left, float top, float width) {
        mWeekdayPaint.setTextSize(mDensity * 11f);
        mWeekdayPaint.getTextBounds("MG", 0, 2, mTextBound);
        float y = top + mTextBound.height();
        canvas.drawText(ellipsize(label, mWeekdayPaint, width), left, y, mWeekdayPaint);
        return y;
    }

    /**
     * Vẽ tối đa {@code max} chip trong khung [top,bottom]; trả về Y đáy phần đã vẽ.
     * Danh sách rỗng -> không vẽ gì. Chip cao đều, cách nhau 6dp.
     */
    private float drawChipList(Canvas canvas, List<CalendarEventsRepo.Ev> list, int max,
                               float left, float top, float width, float bottom) {
        int count = Math.min(max, list.size());
        if (count <= 0) return top;
        float gap = mDensity * 6f;
        float minChip = mDensity * 30f;
        float areaH = bottom - top;
        float chipH = (areaH - gap * (count - 1)) / count;
        if (chipH < minChip) chipH = minChip;
        float y = top;
        for (int i = 0; i < count; i++) {
            drawChip(canvas, list.get(i), left, y, width, chipH);
            y += chipH + gap;
        }
        return y - gap;
    }

    /**
     * Vẽ bố cục 2 cột hôm nay / ngày mai + "N more events" + "Up next".
     * Dùng chung cho medium (4x2, {@code maxPerCol}=2) và large (4x4, {@code maxPerCol}=4).
     */
    private void drawTwoColumns(Canvas canvas, RectF card, int maxPerCol) {
        float pad = mDensity * 14f;
        float left = card.left + pad;
        float right = card.right - pad;
        float top = card.top + pad;
        float bottom = card.bottom - pad;
        float contentW = right - left;
        if (contentW <= 0) return;

        // Footer "Up next" bám đáy.
        mTimePaint.setTextSize(mDensity * 10f);
        String footer = getResources().getString(R.string.calendar_up_next_footer);
        canvas.drawText(footer, left, bottom, mTimePaint);
        float colBottom = bottom - mDensity * 16f;

        // Thiếu quyền / rỗng: vẽ gợi ý trải ngang, không chia cột.
        if (!mHasPermission || mEvents.isEmpty()) {
            drawTodayHeader(canvas, left, top, contentW);
            mHintPaint.setTextSize(mDensity * 12f);
            String msg = getResources().getString(!mHasPermission
                    ? R.string.calendar_up_next_grant : R.string.calendar_up_next_empty);
            float hy = top + mDensity * 60f;
            drawWrapped(canvas, msg, mHintPaint, left, hy, contentW, colBottom - hy);
            return;
        }

        float colGap = mDensity * 14f;
        float colW = (contentW - colGap) / 2f;
        float leftX = left;
        float rightX = left + colW + colGap;

        // Chia sự kiện theo ngày.
        long tomorrowStart = CalendarEventsRepo.startOfDay(1);
        long dayAfter = CalendarEventsRepo.startOfDay(2);
        List<CalendarEventsRepo.Ev> todayList = new ArrayList<>();
        List<CalendarEventsRepo.Ev> tomList = new ArrayList<>();
        for (CalendarEventsRepo.Ev ev : mEvents) {
            if (ev.startMs < tomorrowStart) todayList.add(ev);
            else if (ev.startMs < dayAfter) tomList.add(ev);
        }

        // Cột trái: hôm nay.
        float ly = drawTodayHeader(canvas, leftX, top, colW);
        float chipsTop = ly + mDensity * 8f;
        if (todayList.isEmpty()) {
            mHintPaint.setTextSize(mDensity * 11f);
            canvas.drawText(getResources().getString(R.string.calendar_up_next_empty),
                    leftX, chipsTop + mDensity * 12f, mHintPaint);
        } else {
            drawChipList(canvas, todayList, maxPerCol, leftX, chipsTop, colW, colBottom);
        }

        // Cột phải: ngày mai.
        String tomorrowLabel = getResources().getString(R.string.calendar_tomorrow).toUpperCase(Locale.getDefault());
        float ry = drawLabelHeader(canvas, tomorrowLabel, rightX, top, colW);
        float rChipsTop = ry + mDensity * 8f;
        if (tomList.isEmpty()) {
            mHintPaint.setTextSize(mDensity * 11f);
            canvas.drawText(getResources().getString(R.string.calendar_up_next_empty),
                    rightX, rChipsTop + mDensity * 12f, mHintPaint);
        } else {
            int shown = Math.min(maxPerCol, tomList.size());
            // Chừa chỗ cho dòng "N more events" nếu còn dư.
            boolean hasMore = tomList.size() > shown;
            float moreH = hasMore ? mDensity * 16f : 0f;
            float chipsBottom = colBottom - moreH;
            drawChipList(canvas, tomList, shown, rightX, rChipsTop, colW, chipsBottom);
            if (hasMore) {
                mTimePaint.setTextSize(mDensity * 10f);
                String more = getResources().getString(
                        R.string.calendar_more_events, tomList.size() - shown);
                canvas.drawText(ellipsize(more, mTimePaint, colW),
                        rightX, colBottom - mDensity * 2f, mTimePaint);
            }
        }
    }

    /** Vẽ biến thể 2x2: header hôm nay + tối đa 2 sự kiện + "Up next". */
    private void drawSmall(Canvas canvas, RectF card) {
        float pad = mDensity * 12f;
        float left = card.left + pad;
        float right = card.right - pad;
        float top = card.top + pad;
        float bottom = card.bottom - pad;
        float contentW = right - left;
        if (contentW <= 0) return;

        Calendar cal = Calendar.getInstance();
        Locale locale = Locale.getDefault();
        String weekday = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, locale);
        if (weekday == null) weekday = "";
        weekday = weekday.toUpperCase(locale);
        String dayNum = Integer.toString(cal.get(Calendar.DAY_OF_MONTH));

        // Header: thứ (đỏ) + số ngày lớn (đen).
        mWeekdayPaint.setTextSize(mDensity * 11f);
        mWeekdayPaint.getTextBounds("MG", 0, 2, mTextBound);
        float y = top + mTextBound.height();
        canvas.drawText(ellipsize(weekday, mWeekdayPaint, contentW), left, y, mWeekdayPaint);

        mDayPaint.setTextSize(mDensity * 30f);
        mDayPaint.getTextBounds("30", 0, 2, mTextBound);
        y += mDensity * 4f + mTextBound.height();
        canvas.drawText(dayNum, left, y, mDayPaint);

        float chipsTop = y + mDensity * 8f;

        // Footer "Up next" bám đáy.
        mTimePaint.setTextSize(mDensity * 10f);
        String footer = getResources().getString(R.string.calendar_up_next_footer);
        float footerY = bottom;
        canvas.drawText(footer, left, footerY, mTimePaint);
        float chipsBottom = footerY - mTimePaint.getFontMetrics().ascent - mDensity * 6f;
        chipsBottom = bottom - (mDensity * 16f);

        float areaH = chipsBottom - chipsTop;
        if (areaH <= 0) return;

        if (!mHasPermission) {
            mHintPaint.setTextSize(mDensity * 11f);
            String hint = getResources().getString(R.string.calendar_up_next_grant);
            drawWrapped(canvas, hint, mHintPaint, left, chipsTop, contentW, areaH);
            return;
        }
        if (mEvents.isEmpty()) {
            mHintPaint.setTextSize(mDensity * 11f);
            String none = getResources().getString(R.string.calendar_up_next_empty);
            drawWrapped(canvas, none, mHintPaint, left, chipsTop, contentW, areaH);
            return;
        }

        int maxChips = 2;
        int count = Math.min(maxChips, mEvents.size());
        float gap = mDensity * 6f;
        float chipH = (areaH - gap * (count - 1)) / count;
        if (chipH < mDensity * 22f) chipH = mDensity * 22f;

        for (int i = 0; i < count; i++) {
            CalendarEventsRepo.Ev ev = mEvents.get(i);
            float chipTop = chipsTop + i * (chipH + gap);
            drawChip(canvas, ev, left, chipTop, contentW, chipH);
        }
    }

    /** 1 sự kiện: nền tint nhạt màu lịch + thanh màu bên trái + tiêu đề + giờ. */
    private void drawChip(Canvas canvas, CalendarEventsRepo.Ev ev, float left, float top,
                          float width, float height) {
        float r = mDensity * 8f;
        mRectF.set(left, top, left + width, top + height);
        mChipBgPaint.setColor(withAlpha(ev.color, 0x24));
        canvas.drawRoundRect(mRectF, r, r, mChipBgPaint);

        // Thanh màu bên trái.
        float barW = mDensity * 3.5f;
        mAccentPaint.setColor(ev.color | 0xFF000000);
        mRectF.set(left, top, left + barW, top + height);
        canvas.drawRoundRect(mRectF, barW / 2f, barW / 2f, mAccentPaint);

        float txLeft = left + barW + mDensity * 7f;
        float txWidth = width - (barW + mDensity * 10f);
        if (txWidth <= 0) return;

        mTitlePaint.setTextSize(mDensity * 10.5f);
        mTimePaint.setTextSize(mDensity * 9.5f);

        mTitlePaint.getTextBounds("Mg", 0, 2, mTextBound);
        float titleH = mTextBound.height();
        mTimePaint.getTextBounds("Mg", 0, 2, mTextBound);
        float timeH = mTextBound.height();
        float gap = mDensity * 2f;
        float blockH = titleH + gap + timeH;
        float cy = top + height / 2f;
        float titleBase = cy - blockH / 2f + titleH;

        canvas.drawText(ellipsize(ev.title, mTitlePaint, txWidth), txLeft, titleBase, mTitlePaint);
        String time = formatTime(ev);
        canvas.drawText(ellipsize(time, mTimePaint, txWidth), txLeft, titleBase + gap + timeH, mTimePaint);
    }

    private String formatTime(CalendarEventsRepo.Ev ev) {
        if (ev.allDay) return getResources().getString(R.string.calendar_up_next_all_day);
        Calendar s = Calendar.getInstance();
        s.setTimeInMillis(ev.startMs);
        Calendar e = Calendar.getInstance();
        e.setTimeInMillis(ev.endMs);
        return hm(s) + "–" + hm(e) + " " + ampm(e);
    }

    private static String hm(Calendar c) {
        int h = c.get(Calendar.HOUR);
        if (h == 0) h = 12;
        int m = c.get(Calendar.MINUTE);
        return h + ":" + (m < 10 ? "0" + m : Integer.toString(m));
    }

    private static String ampm(Calendar c) {
        return c.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private String ellipsize(String s, TextPaint p, float maxWidth) {
        if (s == null) return "";
        return TextUtils.ellipsize(s, p, maxWidth, TextUtils.TruncateAt.END).toString();
    }

    /** Vẽ text nhiều dòng đơn giản (tối đa 3 dòng) trong khung cho trạng thái gợi ý. */
    private void drawWrapped(Canvas canvas, String s, TextPaint p, float left, float top,
                             float width, float height) {
        p.getTextBounds("Mg", 0, 2, mTextBound);
        float lineH = mTextBound.height() * 1.4f;
        float y = top + mTextBound.height();
        int start = 0;
        int len = s.length();
        int lines = 0;
        while (start < len && lines < 3 && y <= top + height) {
            int count = p.breakText(s, start, len, true, width, null);
            if (count <= 0) break;
            // Cắt theo khoảng trắng gần nhất cho gọn.
            int end = start + count;
            if (end < len) {
                int sp = s.lastIndexOf(' ', end);
                if (sp > start) end = sp;
            }
            canvas.drawText(s.substring(start, end).trim(), left, y, p);
            start = end;
            while (start < len && s.charAt(start) == ' ') start++;
            y += lineH;
            lines++;
        }
    }
}

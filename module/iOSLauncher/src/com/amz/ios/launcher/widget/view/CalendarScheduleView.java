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
 * Widget lịch kiểu iOS vẽ DÒNG THỜI GIAN THEO GIỜ (schedule/timeline), tự vẽ bằng Canvas — dùng cho
 * 2 cỡ cuối của họ Calendar: Up Next VỪA (4x2, 1 cột = hôm nay) và Up Next LỚN (4x4, 2 cột = hôm
 * nay + ngày mai). Bố cục bám theo ảnh mẫu: header (thứ đỏ + số ngày lớn / "TOMORROW" xám), hàng
 * sự kiện CẢ NGÀY dạng viên thuốc trên cùng, lưới giờ (nhãn giờ + đường kẻ mảnh), khối sự kiện đặt
 * đúng vị trí theo giờ (nền tint + thanh màu trái + tên + giờ), vạch ĐỎ "giờ hiện tại" có chấm tròn
 * (chỉ cột hôm nay), và chân "N more events" kèm các vạch màu.
 * <p>
 * TÁCH RIÊNG khỏi {@link CalendarUpNextView} (bản small dạng chip-list) để không ảnh hưởng cỡ 2x2.
 * Dữ liệu lấy từ {@link CalendarEventsRepo} (cần quyền READ_CALENDAR); thiếu quyền → gợi ý "chạm để
 * cấp quyền"; chạm khi đã có quyền → mở app Lịch. Tự truy vấn lại mỗi 60s.
 */
public class CalendarScheduleView extends View implements View.OnClickListener {

    // Bảng màu iOS.
    private static final int CARD_BG_COLOR = Color.WHITE;
    private static final int WEEKDAY_COLOR = 0xFFFF3B30;  // đỏ
    private static final int DAY_COLOR = 0xFF1C1C1E;       // gần đen
    private static final int SECONDARY_COLOR = 0xFF8E8E93; // xám
    private static final int GRID_LINE_COLOR = 0x22000000; // đường kẻ giờ mảnh
    private static final int NOW_LINE_COLOR = 0xFFFF3B30;  // vạch giờ hiện tại (đỏ)
    private static final int ALLDAY_BG_COLOR = 0xFFEAEAEF; // nền viên "cả ngày"

    private final float mDensity;
    private boolean mTwoColumns = false;

    private final Paint mBgPaint;
    private final Paint mFillPaint;
    private final Paint mLinePaint;
    private final TextPaint mWeekdayPaint;
    private final TextPaint mDayPaint;
    private final TextPaint mLabelPaint;   // "TOMORROW" + nhãn giờ + phụ
    private final TextPaint mTitlePaint;   // tên sự kiện
    private final TextPaint mTimePaint;    // giờ sự kiện
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

    public CalendarScheduleView(Context context) {
        this(context, null);
    }

    public CalendarScheduleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalendarScheduleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDensity = getResources().getDisplayMetrics().density;

        mBgPaint = fill(CARD_BG_COLOR);
        mFillPaint = fill(Color.WHITE);
        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setColor(GRID_LINE_COLOR);
        mLinePaint.setStrokeWidth(Math.max(1f, mDensity * 0.7f));

        mWeekdayPaint = text(WEEKDAY_COLOR, true, Paint.Align.LEFT);
        mDayPaint = text(DAY_COLOR, true, Paint.Align.LEFT);
        mLabelPaint = text(SECONDARY_COLOR, true, Paint.Align.LEFT);
        mTitlePaint = text(DAY_COLOR, true, Paint.Align.LEFT);
        mTimePaint = text(SECONDARY_COLOR, false, Paint.Align.LEFT);
        mHintPaint = text(SECONDARY_COLOR, false, Paint.Align.LEFT);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CalendarScheduleView);
            mTwoColumns = a.getBoolean(R.styleable.CalendarScheduleView_csvTwoColumns, false);
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

    /** Truy vấn từ 00:00 hôm nay tới 00:00 ngày kia (đủ cho cột hôm nay + ngày mai). */
    private void reloadEvents() {
        mHasPermission = CalendarEventsRepo.hasPermission(getContext());
        mEvents.clear();
        if (!mHasPermission) return;
        long from = CalendarEventsRepo.startOfDay(0);
        long to = CalendarEventsRepo.startOfDay(2);
        mEvents.addAll(CalendarEventsRepo.queryEvents(getContext(), from, to));
    }

    @Override
    public void onClick(View v) {
        if (!mHasPermission) {
            requestCalendarPermission();
        } else {
            openCalendarApp();
        }
    }

    private void requestCalendarPermission() {
        Activity act = findActivity(getContext());
        if (act != null) {
            try {
                ActivityCompat.requestPermissions(act,
                        new String[]{android.Manifest.permission.READ_CALENDAR}, 4022);
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

        float pad = mDensity * 14f;
        float left = mRectF.left + pad;
        float top = mRectF.top + pad;
        float right = mRectF.right - pad;
        float bottom = mRectF.bottom - pad;
        float contentW = right - left;
        float contentH = bottom - top;
        if (contentW <= 0 || contentH <= 0) return;

        // Thiếu quyền / rỗng: gợi ý trải ngang, không vẽ lưới.
        if (!mHasPermission) {
            drawTodayHeader(canvas, left, top, contentW);
            mHintPaint.setTextSize(mDensity * 12f);
            drawWrapped(canvas, getResources().getString(R.string.calendar_up_next_grant),
                    mHintPaint, left, top + mDensity * 56f, contentW, bottom - (top + mDensity * 56f));
            return;
        }

        // Tách sự kiện theo ngày.
        long todayStart = CalendarEventsRepo.startOfDay(0);
        long tomorrowStart = CalendarEventsRepo.startOfDay(1);
        long dayAfter = CalendarEventsRepo.startOfDay(2);
        List<CalendarEventsRepo.Ev> todayList = new ArrayList<>();
        List<CalendarEventsRepo.Ev> tomList = new ArrayList<>();
        for (CalendarEventsRepo.Ev ev : mEvents) {
            if (ev.startMs < tomorrowStart) todayList.add(ev);
            else if (ev.startMs < dayAfter) tomList.add(ev);
        }

        if (!mTwoColumns) {
            // 1 cột: chỉ hôm nay.
            drawColumn(canvas, left, top, right, bottom, todayStart, true, todayList);
            return;
        }

        // 2 cột: hôm nay | ngày mai. Chừa khoảng giữa; căn đáy lưới thẳng hàng nhờ tham số chung.
        float colGap = mDensity * 16f;
        float colW = (contentW - colGap) / 2f;
        float rightColX = left + colW + colGap;
        // Đường kẻ dọc phân cách mờ giữa 2 cột.
        float sepX = left + colW + colGap / 2f;
        canvas.drawLine(sepX, top + mDensity * 4f, sepX, bottom - mDensity * 4f, mLinePaint);

        drawColumn(canvas, left, top, left + colW, bottom, todayStart, true, todayList);
        drawColumn(canvas, rightColX, top, rightColX + colW, bottom, tomorrowStart, false, tomList);
    }

    /**
     * Vẽ 1 cột timeline trong khung [colLeft,colTop,colRight,colBottom].
     *
     * @param dayStartMs mốc 00:00 của ngày cột này
     * @param isToday    true = cột hôm nay (header số ngày lớn + vạch giờ hiện tại)
     * @param dayEvents  sự kiện thuộc ngày này (đã lọc)
     */
    private void drawColumn(Canvas canvas, float colLeft, float colTop, float colRight,
                            float colBottom, long dayStartMs, boolean isToday,
                            List<CalendarEventsRepo.Ev> dayEvents) {
        float colW = colRight - colLeft;
        if (colW <= 0) return;

        // 1) Header.
        float headerBottom;
        if (isToday) {
            headerBottom = drawTodayHeader(canvas, colLeft, colTop, colW);
        } else {
            String label = getResources().getString(R.string.calendar_tomorrow)
                    .toUpperCase(Locale.getDefault());
            mLabelPaint.setTextSize(mDensity * 11f);
            mLabelPaint.getTextBounds("MG", 0, 2, mTextBound);
            headerBottom = colTop + mTextBound.height();
            canvas.drawText(ellipsize(label, mLabelPaint, colW), colLeft, headerBottom, mLabelPaint);
            // Chừa cùng chiều cao header như cột hôm nay để 2 lưới thẳng hàng.
            headerBottom = colTop + headerHeight();
        }

        // 2) Tách sự kiện cả-ngày và có-giờ.
        List<CalendarEventsRepo.Ev> allDay = new ArrayList<>();
        List<CalendarEventsRepo.Ev> timed = new ArrayList<>();
        for (CalendarEventsRepo.Ev ev : dayEvents) {
            if (ev.allDay) allDay.add(ev);
            else timed.add(ev);
        }

        float y = colTop + headerHeight() + mDensity * 4f;

        // 3) Hàng sự kiện cả-ngày (viên thuốc). Vẽ tối đa 1 viên; dư dồn vào "more".
        int extraAllDay = 0;
        if (!allDay.isEmpty()) {
            drawAllDayPill(canvas, allDay.get(0), colLeft, y, colW);
            y += allDayPillHeight() + mDensity * 6f;
            extraAllDay = allDay.size() - 1;
        }

        // 4) Lưới giờ + sự kiện có-giờ.
        float footerH = mDensity * 20f;
        float gridTop = y;
        float gridBottom = colBottom - footerH;
        if (gridBottom - gridTop < mDensity * 30f) {
            // Quá thấp để vẽ lưới: chỉ liệt kê chữ đơn giản.
            drawCompactList(canvas, timed, colLeft, gridTop, colW, colBottom);
            return;
        }

        float hourTarget = mDensity * 40f;
        int numHours = (int) Math.floor((gridBottom - gridTop) / hourTarget);
        if (numHours < 2) numHours = 2;
        float hourPx = (gridBottom - gridTop) / numHours;

        // Giờ bắt đầu cửa sổ: hôm nay = giờ hiện tại; ngày mai = giờ sự kiện đầu (mặc định 8h).
        int startHour;
        Calendar nowCal = Calendar.getInstance();
        if (isToday) {
            startHour = nowCal.get(Calendar.HOUR_OF_DAY);
        } else {
            startHour = firstEventHour(timed, dayStartMs, 8);
        }
        // Kẹp để cửa sổ không vượt quá 24h.
        if (startHour > 24 - numHours) startHour = 24 - numHours;
        if (startHour < 0) startHour = 0;

        long windowStart = dayStartMs + startHour * 3600_000L;
        long windowEnd = dayStartMs + (startHour + numHours) * 3600_000L;

        // Nhãn giờ + đường kẻ.
        float labelColW = mDensity * 26f;
        float lineLeft = colLeft + labelColW;
        mLabelPaint.setTextSize(mDensity * 10f);
        mLabelPaint.setColor(SECONDARY_COLOR);
        for (int i = 0; i <= numHours; i++) {
            float ly = gridTop + i * hourPx;
            canvas.drawLine(lineLeft, ly, colRight, ly, mLinePaint);
            if (i < numHours) {
                int hour24 = startHour + i;
                int h12 = hour24 % 12;
                if (h12 == 0) h12 = 12;
                mLabelPaint.getTextBounds("9", 0, 1, mTextBound);
                canvas.drawText(Integer.toString(h12), colLeft,
                        ly + mTextBound.height() + mDensity * 2f, mLabelPaint);
            }
        }

        // Khối sự kiện có-giờ; đếm phần vượt cửa sổ để đưa vào "more".
        List<CalendarEventsRepo.Ev> more = new ArrayList<>();
        float blockLeft = lineLeft + mDensity * 3f;
        float blockW = colRight - blockLeft;
        for (CalendarEventsRepo.Ev ev : timed) {
            if (ev.endMs <= windowStart) continue;          // đã qua hẳn cửa sổ -> bỏ
            if (ev.startMs >= windowEnd) {                    // sau cửa sổ -> gộp vào "more"
                more.add(ev);
                continue;
            }
            float yTop = gridTop + hoursBetween(windowStart, ev.startMs) * hourPx;
            float yBot = gridTop + hoursBetween(windowStart, ev.endMs) * hourPx;
            if (yTop < gridTop) yTop = gridTop;
            if (yBot > gridBottom) yBot = gridBottom;
            if (yBot - yTop < mDensity * 26f) yBot = yTop + mDensity * 26f;
            if (yBot > gridBottom) yBot = gridBottom;
            drawEventBlock(canvas, ev, blockLeft, yTop, yBot, blockW);
        }

        // 5) Vạch giờ hiện tại (đỏ + chấm) — chỉ cột hôm nay và nằm trong cửa sổ.
        if (isToday) {
            long now = System.currentTimeMillis();
            if (now >= windowStart && now <= windowEnd) {
                float ny = gridTop + hoursBetween(windowStart, now) * hourPx;
                mFillPaint.setColor(NOW_LINE_COLOR);
                float dotR = mDensity * 3.5f;
                canvas.drawCircle(lineLeft, ny, dotR, mFillPaint);
                mLinePaint.setColor(NOW_LINE_COLOR);
                mLinePaint.setStrokeWidth(Math.max(1.5f, mDensity * 1.2f));
                canvas.drawLine(lineLeft, ny, colRight, ny, mLinePaint);
                // Khôi phục bút kẻ lưới cho lần vẽ sau.
                mLinePaint.setColor(GRID_LINE_COLOR);
                mLinePaint.setStrokeWidth(Math.max(1f, mDensity * 0.7f));
            }
        }

        // 6) Chân "N more events" + các vạch màu.
        int moreCount = more.size() + extraAllDay;
        if (moreCount > 0) {
            drawMoreFooter(canvas, more, extraAllDay, moreCount, colLeft, colBottom, colW);
        }
    }

    /** Header "thứ (đỏ) + số ngày lớn (đen)"; trả về Y đáy phần đã vẽ. */
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

        mDayPaint.setTextSize(mDensity * 26f);
        mDayPaint.getTextBounds("30", 0, 2, mTextBound);
        y += mDensity * 3f + mTextBound.height();
        canvas.drawText(dayNum, left, y, mDayPaint);
        return y;
    }

    /** Chiều cao header cố định (dùng để căn 2 cột thẳng hàng). */
    private float headerHeight() {
        return mDensity * 45f;
    }

    private float allDayPillHeight() {
        return mDensity * 30f;
    }

    /** Viên "cả ngày": nền xám bo tròn + chấm màu lịch + tên. */
    private void drawAllDayPill(Canvas canvas, CalendarEventsRepo.Ev ev, float left, float top, float width) {
        float h = allDayPillHeight();
        float r = h / 2f;
        mRectF.set(left, top, left + width, top + h);
        mFillPaint.setColor(ALLDAY_BG_COLOR);
        canvas.drawRoundRect(mRectF, r, r, mFillPaint);

        float cy = top + h / 2f;
        float dotR = h * 0.30f;
        float cx = left + r;
        mFillPaint.setColor(ev.color | 0xFF000000);
        canvas.drawCircle(cx, cy, dotR, mFillPaint);

        float txLeft = cx + dotR + mDensity * 8f;
        float txWidth = left + width - txLeft - mDensity * 8f;
        if (txWidth <= 0) return;
        mTitlePaint.setColor(DAY_COLOR);
        mTitlePaint.setTextSize(mDensity * 11f);
        mTitlePaint.getTextBounds("Mg", 0, 2, mTextBound);
        canvas.drawText(ellipsize(ev.title, mTitlePaint, txWidth),
                txLeft, cy + mTextBound.height() / 2f, mTitlePaint);
    }

    /** Khối 1 sự kiện có-giờ: nền tint + thanh màu trái + tên (màu lịch) + giờ. */
    private void drawEventBlock(Canvas canvas, CalendarEventsRepo.Ev ev, float left, float top,
                                float bottom, float width) {
        if (width <= 0) return;
        float r = mDensity * 6f;
        int color = ev.color | 0xFF000000;
        mRectF.set(left, top, left + width, bottom);
        mFillPaint.setColor(withAlpha(color, 0x22));
        canvas.drawRoundRect(mRectF, r, r, mFillPaint);

        // Thanh màu bên trái.
        float barW = mDensity * 3f;
        mFillPaint.setColor(color);
        mRectF.set(left, top, left + barW, bottom);
        canvas.drawRoundRect(mRectF, barW / 2f, barW / 2f, mFillPaint);

        float txLeft = left + barW + mDensity * 6f;
        float txWidth = width - (barW + mDensity * 9f);
        if (txWidth <= 0) return;
        float height = bottom - top;

        mTitlePaint.setColor(darken(color));
        mTitlePaint.setTextSize(mDensity * 10.5f);
        mTitlePaint.getTextBounds("Mg", 0, 2, mTextBound);
        float titleH = mTextBound.height();
        float titleBase = top + mDensity * 4f + titleH;
        canvas.drawText(ellipsize(ev.title, mTitlePaint, txWidth), txLeft, titleBase, mTitlePaint);

        // Giờ (nếu còn chỗ theo chiều cao).
        if (height >= mDensity * 34f) {
            mTimePaint.setColor(darken(color));
            mTimePaint.setTextSize(mDensity * 9f);
            mTimePaint.getTextBounds("Mg", 0, 2, mTextBound);
            canvas.drawText(ellipsize(formatStart(ev), mTimePaint, txWidth),
                    txLeft, titleBase + mDensity * 3f + mTextBound.height(), mTimePaint);
        }
    }

    /** Chân "N more events" + tối đa 4 vạch màu của các sự kiện bị ẩn. */
    private void drawMoreFooter(Canvas canvas, List<CalendarEventsRepo.Ev> more, int extraAllDay,
                                int moreCount, float left, float colBottom, float colW) {
        float y = colBottom - mDensity * 4f;
        float tickW = mDensity * 3f;
        float tickH = mDensity * 11f;
        float gap = mDensity * 3f;
        float x = left;
        int ticks = Math.min(4, more.size());
        for (int i = 0; i < ticks; i++) {
            mFillPaint.setColor(more.get(i).color | 0xFF000000);
            mRectF.set(x, y - tickH, x + tickW, y);
            canvas.drawRoundRect(mRectF, tickW / 2f, tickW / 2f, mFillPaint);
            x += tickW + gap;
        }
        String txt = getResources().getString(R.string.calendar_more_events, moreCount);
        mLabelPaint.setColor(SECONDARY_COLOR);
        mLabelPaint.setTextSize(mDensity * 10f);
        float txLeft = x + (ticks > 0 ? mDensity * 4f : 0f);
        canvas.drawText(ellipsize(txt, mLabelPaint, left + colW - txLeft),
                txLeft, y - mDensity * 1f, mLabelPaint);
    }

    /** Dự phòng khi cột quá thấp: liệt kê tên + giờ dạng dòng đơn giản. */
    private void drawCompactList(Canvas canvas, List<CalendarEventsRepo.Ev> timed,
                                 float left, float top, float width, float bottom) {
        if (timed.isEmpty()) {
            mHintPaint.setTextSize(mDensity * 11f);
            canvas.drawText(getResources().getString(R.string.calendar_up_next_empty),
                    left, top + mDensity * 12f, mHintPaint);
            return;
        }
        mTitlePaint.setColor(DAY_COLOR);
        mTitlePaint.setTextSize(mDensity * 11f);
        float lineH = mDensity * 18f;
        float y = top + mDensity * 11f;
        for (CalendarEventsRepo.Ev ev : timed) {
            if (y > bottom) break;
            canvas.drawText(ellipsize(formatStart(ev) + "  " + ev.title, mTitlePaint, width),
                    left, y, mTitlePaint);
            y += lineH;
        }
    }

    // Giờ (12h) của sự kiện đầu tiên trong ngày, để chọn giờ bắt đầu cửa sổ cột ngày mai.
    private int firstEventHour(List<CalendarEventsRepo.Ev> timed, long dayStartMs, int fallback) {
        for (CalendarEventsRepo.Ev ev : timed) {
            if (ev.startMs >= dayStartMs) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(ev.startMs);
                return c.get(Calendar.HOUR_OF_DAY);
            }
        }
        return fallback;
    }

    private static float hoursBetween(long fromMs, long toMs) {
        return (toMs - fromMs) / 3600_000f;
    }

    /** "9:45AM" / "10AM" (bỏ phút khi tròn giờ). */
    private String formatStart(CalendarEventsRepo.Ev ev) {
        if (ev.allDay) return getResources().getString(R.string.calendar_up_next_all_day);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ev.startMs);
        int h = c.get(Calendar.HOUR);
        if (h == 0) h = 12;
        int m = c.get(Calendar.MINUTE);
        String ap = c.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
        return m == 0 ? (h + ap) : (h + ":" + (m < 10 ? "0" + m : Integer.toString(m)) + ap);
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    /** Làm tối màu ~25% để chữ trên nền tint đủ tương phản. */
    private static int darken(int color) {
        int a = (color >>> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * 0.75f);
        int g = (int) (((color >> 8) & 0xFF) * 0.75f);
        int b = (int) ((color & 0xFF) * 0.75f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private String ellipsize(String s, TextPaint p, float maxWidth) {
        if (s == null) return "";
        if (maxWidth <= 0) return "";
        return TextUtils.ellipsize(s, p, maxWidth, TextUtils.TruncateAt.END).toString();
    }

    /** Vẽ text nhiều dòng đơn giản (tối đa 3 dòng) cho trạng thái gợi ý. */
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

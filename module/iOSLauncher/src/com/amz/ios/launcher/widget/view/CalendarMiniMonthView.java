package com.amz.ios.launcher.widget.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Locale;

/**
 * Lịch THÁNG mini kiểu iOS (widget Calendar - Month 2x2), tự vẽ bằng Canvas.
 *
 * Vì widget nội bộ (IOS_WIDGET id -100) được launcher inflate thành View THẬT
 * (xem LauncherAppWidgetHost.createView) chứ không chạy trong RemoteViews, ta được
 * phép dùng custom View + Canvas như {@link AnalogClockView}. Nhờ vậy preview trong
 * sheet chọn size (LiveWidgetPreviewHelper inflate initialLayout) cũng tự vẽ đúng.
 *
 * Chỉ hiển thị NGÀY/THÁNG hiện tại (không đọc sự kiện, không cần quyền lịch): tên
 * tháng đỏ ở trên, hàng chữ cái thứ, lưới ngày 1..N với hôm nay khoanh tròn đỏ đặc.
 * Tự làm mới mỗi 60s để qua nửa đêm đổi "hôm nay" mà không cần AlarmManager.
 */
public class CalendarMiniMonthView extends View {

    // Bảng màu iOS: nền thẻ trắng, tên tháng + hôm nay đỏ, chữ thứ xám, số ngày đen.
    private static final int CARD_BG_COLOR = Color.WHITE;
    private static final int MONTH_COLOR = 0xFFFF3B30;   // đỏ iOS
    private static final int TODAY_BG_COLOR = 0xFFFF3B30; // vòng tròn hôm nay
    private static final int TODAY_TEXT_COLOR = Color.WHITE;
    private static final int WEEKDAY_COLOR = 0xFF8E8E93; // xám nhạt
    private static final int DAY_COLOR = 0xFF1C1C1E;     // gần đen

    // Chữ cái thứ, Chủ nhật đầu tuần (khớp ảnh iOS: S M T W T F S).
    private static final String[] WEEKDAY_LETTERS = {"S", "M", "T", "W", "T", "F", "S"};

    private final Paint mBgPaint;
    private final Paint mMonthPaint;
    private final Paint mWeekdayPaint;
    private final Paint mDayPaint;
    private final Paint mTodayBgPaint;
    private final Paint mTodayTextPaint;
    private final Rect mTextBound = new Rect();
    private final RectF mCardRect = new RectF();

    private final float mDensity;

    // Làm mới định kỳ để đổi hôm nay khi qua nửa đêm (chỉ invalidate, rất nhẹ).
    private final Runnable mTicker = new Runnable() {
        @Override
        public void run() {
            invalidate();
            postDelayed(this, 60_000L);
        }
    };

    public CalendarMiniMonthView(Context context) {
        this(context, null);
    }

    public CalendarMiniMonthView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalendarMiniMonthView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDensity = getResources().getDisplayMetrics().density;

        mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBgPaint.setColor(CARD_BG_COLOR);
        mBgPaint.setStyle(Paint.Style.FILL);

        mMonthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMonthPaint.setColor(MONTH_COLOR);
        mMonthPaint.setFakeBoldText(true);
        mMonthPaint.setTextAlign(Paint.Align.LEFT);

        mWeekdayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWeekdayPaint.setColor(WEEKDAY_COLOR);
        mWeekdayPaint.setFakeBoldText(true);
        mWeekdayPaint.setTextAlign(Paint.Align.CENTER);

        mDayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDayPaint.setColor(DAY_COLOR);
        mDayPaint.setFakeBoldText(true);
        mDayPaint.setTextAlign(Paint.Align.CENTER);

        mTodayBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTodayBgPaint.setColor(TODAY_BG_COLOR);
        mTodayBgPaint.setStyle(Paint.Style.FILL);

        mTodayTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTodayTextPaint.setColor(TODAY_TEXT_COLOR);
        mTodayTextPaint.setFakeBoldText(true);
        mTodayTextPaint.setTextAlign(Paint.Align.CENTER);
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

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // Thẻ nền trắng bo góc lấp đầy ô (chừa 1 viền nhỏ cho đỡ sát mép).
        float inset = mDensity * 1f;
        float radius = mDensity * 20f;
        mCardRect.set(inset, inset, w - inset, h - inset);
        canvas.drawRoundRect(mCardRect, radius, radius, mBgPaint);

        float padH = mDensity * 10f;
        float padTop = mDensity * 9f;
        float padBottom = mDensity * 8f;
        float contentLeft = mCardRect.left + padH;
        float contentRight = mCardRect.right - padH;
        float contentTop = mCardRect.top + padTop;
        float contentBottom = mCardRect.bottom - padBottom;
        float contentW = contentRight - contentLeft;
        float contentH = contentBottom - contentTop;
        if (contentW <= 0 || contentH <= 0) return;

        Calendar cal = Calendar.getInstance();
        int todayDay = cal.get(Calendar.DAY_OF_MONTH);
        Locale locale = Locale.getDefault();
        String monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, locale);
        if (monthName == null) monthName = "";
        monthName = monthName.toUpperCase(locale);

        // Số ngày trong tháng + thứ của ngày 1 (Chủ nhật=0..Thứ 7=6).
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar first = (Calendar) cal.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int firstOffset = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY; // 0..6

        int weeks = (int) Math.ceil((firstOffset + daysInMonth) / 7f);
        if (weeks < 1) weeks = 1;

        // Chia chiều cao: tên tháng + hàng thứ + N hàng ngày.
        float colW = contentW / 7f;
        float monthTextSize = clamp(colW * 0.62f, mDensity * 9f, mDensity * 15f);
        mMonthPaint.setTextSize(monthTextSize);
        mMonthPaint.getTextBounds("MG", 0, 2, mTextBound);
        float monthRowH = mTextBound.height() + mDensity * 5f;

        float gridTop = contentTop + monthRowH;
        float gridH = contentBottom - gridTop;
        float weekdayRowH = clamp(colW * 0.85f, mDensity * 10f, mDensity * 16f);
        float dayRowsH = gridH - weekdayRowH;
        if (dayRowsH <= 0) return;
        float rowH = dayRowsH / weeks;

        float cellMin = Math.min(colW, rowH);
        float daySize = clamp(cellMin * 0.5f, mDensity * 8f, mDensity * 13f);
        float weekdaySize = clamp(cellMin * 0.44f, mDensity * 7f, mDensity * 11f);
        mDayPaint.setTextSize(daySize);
        mTodayTextPaint.setTextSize(daySize);
        mWeekdayPaint.setTextSize(weekdaySize);

        // Tên tháng (đỏ, trên cùng, canh trái).
        float monthBaseline = contentTop + mTextBound.height();
        canvas.drawText(monthName, contentLeft, monthBaseline, mMonthPaint);

        // Hàng chữ cái thứ.
        float weekdayCy = gridTop + weekdayRowH / 2f;
        for (int i = 0; i < 7; i++) {
            float cx = contentLeft + colW * (i + 0.5f);
            canvas.drawText(WEEKDAY_LETTERS[i], cx, baselineY(mWeekdayPaint, weekdayCy), mWeekdayPaint);
        }

        // Lưới ngày.
        float radiusToday = Math.min(colW, rowH) * 0.42f;
        for (int day = 1; day <= daysInMonth; day++) {
            int index = firstOffset + (day - 1);
            int col = index % 7;
            int row = index / 7;
            float cx = contentLeft + colW * (col + 0.5f);
            float cy = gridTop + weekdayRowH + rowH * (row + 0.5f);
            String s = Integer.toString(day);
            if (day == todayDay) {
                canvas.drawCircle(cx, cy, radiusToday, mTodayBgPaint);
                canvas.drawText(s, cx, baselineY(mTodayTextPaint, cy), mTodayTextPaint);
            } else {
                canvas.drawText(s, cx, baselineY(mDayPaint, cy), mDayPaint);
            }
        }
    }

    /** Baseline để chữ canh giữa theo trục dọc quanh tâm cy. */
    private static float baselineY(Paint p, float cy) {
        Paint.FontMetrics fm = p.getFontMetrics();
        return cy - (fm.ascent + fm.descent) / 2f;
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}

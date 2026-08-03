package com.amz.ios.launcher.widget.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.amz.ios.launcher.R;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Đồng hồ kim tự vẽ bằng Canvas, tự tick khi được gắn vào cửa sổ.
 *
 * Dùng cho widget nội bộ (IOS_WIDGET id -100): các widget này KHÔNG chạy trong
 * sandbox RemoteViews mà được launcher inflate thành View thật trong process của
 * mình (xem LauncherAppWidgetHost.createView), nên ta được phép dùng custom View
 * vẽ Canvas + tự invalidate — điều RemoteViews không cho phép.
 *
 * Thuật toán mặt số / góc kim tái sử dụng từ
 * {@link com.amz.ios.launcher.leftpage.drawables.ClockDrawable}. Điểm khác: KHÔNG
 * vẽ nền đen squircle (nền glass đặt ở background của layout bao ngoài), có thể
 * ẩn/hiện số 1-12 và kim giây để phù hợp mặt đồng hồ lớn (page 0) lẫn mini analog.
 */
public class AnalogClockView extends View {

    private static final int SECOND_HAND_COLOR = -37632; // cam iOS (0xFFFF6D00)
    private static final int[] HOUR_VALUES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

    private static final int TICK_HOUR_COLOR = 0xFF3C3C3C;   // vạch giờ đậm
    private static final int TICK_MINUTE_COLOR = 0xFFB0B0B0; // vạch phút xám nhạt

    private final Paint mDialPaint;
    private final Paint mBlackPaint;
    private final Paint mHandPaint;
    private final Paint mSecondHandPaint;
    private final Paint mTickPaint;
    private final Rect mTextBound = new Rect();

    private TimeZone mTimeZone = TimeZone.getDefault();
    private boolean mShowSecond = false;
    private boolean mShowNumbers = true;
    private boolean mShowTicks = false;

    private int mHandPadding;
    private int mHandDiffLength;
    private int mContentRadius;
    private int mHandWidth;
    private int mClockPadding;

    private final Runnable mTicker = new Runnable() {
        @Override
        public void run() {
            invalidate();
            long period = mShowSecond ? 1000L : 20000L;
            // Canh vào đầu chu kỳ tiếp theo để kim nhảy đúng nhịp.
            long now = System.currentTimeMillis();
            long delay = period - (now % period);
            postDelayed(this, delay);
        }
    };

    public AnalogClockView(Context context) {
        this(context, null);
    }

    public AnalogClockView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnalogClockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        float density = getResources().getDisplayMetrics().density;
        mHandWidth = Math.max(1, Math.round(density * 2f)); // ~2dp kim giờ/phút
        mClockPadding = Math.round(density * 3f);            // 3dp (clock_padding)

        mDialPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDialPaint.setColor(Color.WHITE);
        mDialPaint.setStyle(Paint.Style.FILL);

        mBlackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBlackPaint.setColor(Color.BLACK);
        mBlackPaint.setStyle(Paint.Style.FILL);

        mHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHandPaint.setColor(Color.BLACK);
        mHandPaint.setStyle(Paint.Style.STROKE);
        mHandPaint.setStrokeCap(Paint.Cap.ROUND);
        mHandPaint.setStrokeWidth(mHandWidth);

        mSecondHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSecondHandPaint.setColor(SECOND_HAND_COLOR);
        mSecondHandPaint.setStyle(Paint.Style.STROKE);
        mSecondHandPaint.setStrokeCap(Paint.Cap.ROUND);
        mSecondHandPaint.setStrokeWidth(Math.max(1f, mHandWidth * 2f / 3f));

        mTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTickPaint.setStyle(Paint.Style.STROKE);
        mTickPaint.setStrokeCap(Paint.Cap.ROUND);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AnalogClockView);
            String tzId = a.getString(R.styleable.AnalogClockView_acvTimeZone);
            if (!TextUtils.isEmpty(tzId)) {
                mTimeZone = TimeZone.getTimeZone(tzId);
            }
            mShowSecond = a.getBoolean(R.styleable.AnalogClockView_acvShowSecond, false);
            mShowNumbers = a.getBoolean(R.styleable.AnalogClockView_acvShowNumbers, true);
            mShowTicks = a.getBoolean(R.styleable.AnalogClockView_acvShowTicks, false);
            a.recycle();
        }
    }

    /** Đặt múi giờ hiển thị (world clock). null = múi giờ máy. */
    public void setTimeZone(@Nullable TimeZone tz) {
        mTimeZone = tz != null ? tz : TimeZone.getDefault();
        invalidate();
    }

    public void setTimeZone(@Nullable String id) {
        setTimeZone(id != null ? TimeZone.getTimeZone(id) : null);
    }

    /** Bật/tắt kim giây. Page 0 để false (chỉ giờ/phút). */
    public void setShowSecond(boolean show) {
        if (mShowSecond != show) {
            mShowSecond = show;
            if (isAttachedToWindow()) {
                removeCallbacks(mTicker);
                post(mTicker);
            }
        }
    }

    /** Ẩn/hiện số 1-12 (mini analog nhỏ nên ẩn cho gọn). */
    public void setShowNumbers(boolean show) {
        if (mShowNumbers != show) {
            mShowNumbers = show;
            invalidate();
        }
    }

    /** Bật/tắt vạch chia giờ/phút quanh mặt số. */
    public void setShowTicks(boolean show) {
        if (mShowTicks != show) {
            mShowTicks = show;
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int min = Math.min(w, h);
        mContentRadius = (min / 2) - mClockPadding;
        mHandPadding = min / 16;
        mHandDiffLength = min / 9;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }

        Calendar calendar = Calendar.getInstance(mTimeZone);

        float cx = w / 2f;
        float cy = h / 2f;

        // Mặt số trắng + chấm tâm.
        canvas.drawCircle(cx, cy, mContentRadius, mDialPaint);

        // Vạch chia giờ/phút (60 vạch) kiểu iOS: mỗi 5 vạch là vạch giờ dài+đậm,
        // còn lại là vạch phút ngắn+xám. Vẽ trước số/kim.
        if (mShowTicks) {
            // Lùi vạch vào trong mép mặt số 1 khoảng nhỏ cho thoáng.
            float outer = mContentRadius - mContentRadius / 14f;
            float hourTickLen = mContentRadius / 12f;
            float minuteTickLen = mContentRadius / 22f;
            for (int i = 0; i < 60; i++) {
                boolean isHourTick = (i % 5) == 0;
                double radian = i * Math.PI / 30.0 - Math.PI * 0.5;
                float cos = (float) Math.cos(radian);
                float sin = (float) Math.sin(radian);
                float inner = outer - (isHourTick ? hourTickLen : minuteTickLen);
                mTickPaint.setColor(isHourTick ? TICK_HOUR_COLOR : TICK_MINUTE_COLOR);
                mTickPaint.setStrokeWidth(isHourTick
                        ? Math.max(1.5f, mHandWidth * 0.9f)
                        : Math.max(1f, mHandWidth * 0.5f));
                canvas.drawLine(cx + cos * inner, cy + sin * inner,
                        cx + cos * outer, cy + sin * outer, mTickPaint);
            }
        }

        canvas.drawCircle(cx, cy, mHandWidth * 1.5f, mBlackPaint);

        // Số 1-12.
        if (mShowNumbers) {
            float textSize = w / 13f;
            mBlackPaint.setTextSize(textSize);
            // Khi có vạch chia thì đẩy số vào trong để cách vạch 1 khoảng cho thoáng.
            float numberInset = mShowTicks ? mContentRadius / 7f : 0f;
            for (int hour : HOUR_VALUES) {
                String s = String.valueOf(hour);
                mBlackPaint.getTextBounds(s, 0, s.length(), mTextBound);
                double radianAngle = (hour - 3) * Math.PI / 6;
                float radius = mContentRadius - 0.8f * textSize - numberInset;
                float midX = (float) (Math.cos(radianAngle) * radius + cx);
                float midY = (float) (Math.sin(radianAngle) * radius + cy);
                float startX = midX - mTextBound.width() / 2f;
                canvas.drawText(s, startX, midY + mTextBound.height() * 0.5f, mBlackPaint);
            }
        }

        // Kim.
        float second = calendar.get(Calendar.SECOND);
        float minute = calendar.get(Calendar.MINUTE);
        float hourAmount = (minute / 60f) + calendar.get(Calendar.HOUR_OF_DAY);
        if (hourAmount > 12f) {
            hourAmount -= 12f;
        }

        drawHand(canvas, cx, cy, hourAmount * 5.0, true, false);   // kim giờ
        drawHand(canvas, cx, cy, minute, false, false);            // kim phút
        if (mShowSecond) {
            drawHand(canvas, cx, cy, second, false, true);         // kim giây
        }
    }

    /**
     * Vẽ 1 kim. {@code angle} theo đơn vị "vạch" (0-60), {@code isHour} kim giờ
     * (ngắn hơn), {@code isSecond} kim giây (mảnh, cam, có đuôi ngược).
     * Sao chép logic từ ClockDrawable.drawClockHand.
     */
    private void drawHand(Canvas canvas, float cx, float cy, double angle,
                          boolean isHour, boolean isSecond) {
        double radian = (Math.PI * angle) / 30.0 - Math.PI * 0.5;
        int length = mContentRadius - mHandPadding;
        if (isHour) {
            length -= mHandDiffLength;
        }
        double cos = Math.cos(radian);
        double sin = Math.sin(radian);

        float startX, startY, endX, endY;
        Paint paint;
        if (isSecond) {
            mSecondHandPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, mHandWidth, mSecondHandPaint);
            mSecondHandPaint.setStyle(Paint.Style.STROKE);
            startX = (float) (cx - (cos * length) / 4.0);
            startY = (float) (cy - (sin * length) / 4.0);
            endX = (float) (cos * length + cx);
            endY = (float) (sin * length + cy);
            paint = mSecondHandPaint;
        } else {
            startX = cx;
            startY = cy;
            endX = (float) (cos * length + cx);
            endY = (float) (sin * length + cy);
            paint = mHandPaint;
        }
        canvas.drawLine(startX, startY, endX, endY, paint);
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
}

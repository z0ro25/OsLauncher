package com.amz.ios.launcher.leftpage.drawables;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CalendarDrawable extends Drawable {

    public boolean isFirstDraw = false;
    public int mWidth;
    public int mHeight;
    public Paint mBgPaint;
    public Paint mWeekDayPaint;
    public Paint mDayOfMonthPaint;
    public SimpleDateFormat mDateFormat;
    public final Rect mTextBound = new Rect();

    public CalendarDrawable(Context context, int width, int height){

        this.mDateFormat = new SimpleDateFormat("EEE", Locale.UK);
        this.mWidth = width;
        this.mHeight = height;

        this.mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mBgPaint.setColor(Color.WHITE);
        this.mBgPaint.setStyle(Paint.Style.FILL);

        this.mWeekDayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface SFProTextMedium = Typeface.createFromAsset(context.getAssets(), "fonts/SFProTextMedium.otf");
        this.mWeekDayPaint.setTypeface(SFProTextMedium);
        this.mWeekDayPaint.setColor(Color.RED);
        this.mWeekDayPaint.setTextAlign(Paint.Align.CENTER);

        this.mDayOfMonthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mDayOfMonthPaint.setColor(Color.BLACK);
        this.mDayOfMonthPaint.setStyle(Paint.Style.FILL);
        Typeface createFromAsset2 = Typeface.createFromAsset(context.getAssets(), "fonts/SFProTextLight.otf");
        this.mDayOfMonthPaint.setTypeface(createFromAsset2);
        this.mDayOfMonthPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        // Vẽ theo ĐÚNG ô launcher cấp (getBounds()) thay vì kích thước bitmap nguồn cố định.
        // Trước đây dùng mWidth/mHeight (size png nguồn) và bỏ qua bounds → khi png nguồn khác
        // cỡ ô icon thì nền + chữ bị lệch phải và cắt mép. Giờ canh giữa đúng ô.
        Rect bounds = getBounds();
        float w = bounds.width();
        float h = bounds.height();
        if (w <= 0 || h <= 0) {
            w = this.mWidth;
            h = this.mHeight;
        }

        int save = canvas.save();
        canvas.translate(bounds.left, bounds.top);
        canvas.clipRect(0f, 0f, w, h);

        float midX = w / 2f;

        ClockDrawable.drawBg(canvas, w, h, this.mBgPaint);

        Calendar calendar = Calendar.getInstance();
        String weekDay = this.mDateFormat.format(calendar.getTime()).toUpperCase();
        this.mTextBound.setEmpty();
        String dayOfMonth = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        this.mDayOfMonthPaint.setTextSize(h / 1.5f);
        this.mDayOfMonthPaint.getTextBounds(dayOfMonth, 0, dayOfMonth.length(), this.mTextBound);
        canvas.drawText(dayOfMonth, midX, h - (h * 0.15f), this.mDayOfMonthPaint);

        this.mTextBound.setEmpty();
        float textSize = h / 6.0f;
        this.mWeekDayPaint.setTextSize(textSize);
        this.mWeekDayPaint.getTextBounds(weekDay, 0, weekDay.length(), this.mTextBound);
        this.mWeekDayPaint.setTextSize(Math.min(((w * 0.9f) * textSize) / this.mTextBound.width(), textSize));
        this.mWeekDayPaint.getTextBounds(weekDay, 0, weekDay.length(), this.mTextBound);
        canvas.drawText(weekDay, midX, h * 0.26f, this.mWeekDayPaint);

        canvas.restoreToCount(save);
    }

    @Override
    public int getIntrinsicWidth() {
        return this.mWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mHeight;
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

}

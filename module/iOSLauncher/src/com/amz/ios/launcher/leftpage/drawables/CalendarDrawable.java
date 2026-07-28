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
        int save = canvas.save();
        if (!this.isFirstDraw) {
            canvas.clipRect(
                    new RectF(
                            .0f, .0f, this.mWidth, this.mHeight
                    )
            );
            this.isFirstDraw = true;
        }

        int midX = this.mWidth / 2;

        ClockDrawable.drawBg(canvas, this.mWidth, this.mHeight, this.mBgPaint);

        Calendar calendar = Calendar.getInstance();
        String weekDay = this.mDateFormat.format(calendar.getTime()).toUpperCase();
        this.mTextBound.setEmpty();
        String dayOfMonth = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        this.mDayOfMonthPaint.setTextSize(this.mHeight / 1.5f);
        this.mDayOfMonthPaint.getTextBounds(dayOfMonth, 0, dayOfMonth.length(), this.mTextBound);
        canvas.drawText(dayOfMonth, (float) midX, this.mHeight - (this.mHeight * 0.15f), this.mDayOfMonthPaint);

        this.mTextBound.setEmpty();
        float textSize = this.mHeight / 6.0f;
        this.mWeekDayPaint.setTextSize(textSize);
        this.mWeekDayPaint.getTextBounds(weekDay, 0, weekDay.length(), this.mTextBound);
        this.mWeekDayPaint.setTextSize(Math.min(((this.mWidth * 0.9f) * textSize) / this.mTextBound.width(), textSize));
        this.mWeekDayPaint.getTextBounds(weekDay, 0, weekDay.length(), this.mTextBound);
        canvas.drawText(weekDay, (float) midX, this.mHeight * 0.26f, this.mWeekDayPaint);

        canvas.restoreToCount(save);
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

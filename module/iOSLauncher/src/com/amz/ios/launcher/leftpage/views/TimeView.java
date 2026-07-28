package com.amz.ios.launcher.leftpage.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;

import com.amz.ios.ioslite.common.ContextHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeView extends View {

    public Context context;

    public Paint timePaint;

    public Paint datePaint;

    public SimpleDateFormat time;

    public SimpleDateFormat date;

    public Rect rect;

    public BroadcastReceiver timeChangeReceiver;

    public class TimeChangeReceiver extends BroadcastReceiver {
        public TimeChangeReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            TimeView.this.invalidate();
        }
    }

    public TimeView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.datePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.rect = new Rect();
        this.timeChangeReceiver = new TimeChangeReceiver();
        this.context = context;
        Locale locale = Locale.ENGLISH;
        this.time = new SimpleDateFormat("HH:mm", locale);
        this.date = new SimpleDateFormat("EEEE, MMMM d", locale);
        if ("vi".equals(locale.getLanguage())) {
            this.date = new SimpleDateFormat("EEEE, d MMMM", locale);
        }
        Typeface createFromAsset = Typeface.createFromAsset(this.context.getAssets(), "fonts/SFProTextUltralight.otf");
        this.timePaint.setColor(-1);
        this.timePaint.setTypeface(createFromAsset);
        Typeface createFromAsset2 = Typeface.createFromAsset(this.context.getAssets(), "fonts/SFProTextLight.otf");
        this.datePaint.setColor(-1);
        this.datePaint.setTypeface(createFromAsset2);
    }

    @Override
    public void onAttachedToWindow() {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.TIME_SET");
            intentFilter.addAction("android.intent.action.SCREEN_ON");
            intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
            intentFilter.addAction("android.intent.action.TIME_TICK");
            intentFilter.addAction("android.intent.action.DATE_CHANGED");
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            ContextHelper.registerReceiver(context, this.timeChangeReceiver, intentFilter);
        } catch (Throwable unused) {
        }
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        try {
            BroadcastReceiver broadcastReceiver = this.timeChangeReceiver;
            if (broadcastReceiver != null) {
                this.context.unregisterReceiver(broadcastReceiver);
            }
        } catch (Throwable unused) {
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void onDraw(Canvas canvas) {
        SimpleDateFormat simpleDateFormat;
        String str;
        try {
            if (DateFormat.is24HourFormat(this.context)) {
                simpleDateFormat = this.time;
                str = "HH:mm";
            } else {
                simpleDateFormat = this.time;
                str = "h:mm";
            }
            simpleDateFormat.applyPattern(str);
        } catch (Throwable unused) {
        }
        int width = getWidth();
        int height = getHeight();
        this.timePaint.setTextSize((height * 3) / 4.0f);
        this.datePaint.setTextSize(height / 5.0f);
        Date time = Calendar.getInstance().getTime();
        String timeTxt = this.time.format(time);
        String dateTxt = this.date.format(time);
        this.rect.setEmpty();
        this.timePaint.getTextBounds(timeTxt, 0, timeTxt.length(), this.rect);
        float midX = width / 2.0f;
        canvas.drawText(timeTxt, midX - (this.rect.width() / 2.0f), this.rect.height() + 1, this.timePaint);
        this.rect.setEmpty();
        this.datePaint.getTextBounds(dateTxt, 0, dateTxt.length(), this.rect);
        canvas.drawText(dateTxt, midX - (this.rect.width() / 2.0f), (height - this.rect.bottom) - 1, this.datePaint);
    }
}

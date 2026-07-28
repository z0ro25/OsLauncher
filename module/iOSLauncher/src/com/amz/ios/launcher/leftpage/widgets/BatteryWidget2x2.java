package com.amz.ios.launcher.leftpage.widgets;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.amz.ios.ioslite.common.ContextHelper;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;

public class BatteryWidget2x2 extends BlurConstraintLayoutWidget {

    private final int mBatteryWidgetProgressSize;
    private final float mBatteryWidgetPaddingSize;
    Launcher mLauncher;
    int mMargin;
    Handler mHandler;
    Runnable mBatteryReloadRunnable = new ReloadBatteryRunnable();
    ImageView mBatteryPercentIV;
    TextView mBatteryPercentTV;
    Paint mUnFillPaint;
    Paint mFillPaint;
    Drawable mPhoneImgDrawable;
    RectF mRectF = new RectF();

    public BatteryWidget2x2(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public BatteryWidget2x2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = (Launcher) context;
        mMargin = mLauncher.getDeviceProfile().edgeMarginPx;
        mBatteryWidgetProgressSize = this.mLauncher.getResources().getDimensionPixelSize(R.dimen.battery_widget_progress_size);
        mBatteryWidgetPaddingSize = this.mLauncher.getResources().getDimensionPixelSize(R.dimen.battery_widget_progress_stroke) * 1.1f;
        mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFillPaint.setStyle(Paint.Style.STROKE);
        mFillPaint.setStrokeCap(Paint.Cap.ROUND);
        mFillPaint.setColor(-16711936);
        mUnFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mUnFillPaint.setStyle(Paint.Style.STROKE);
        mUnFillPaint.setStrokeCap(Paint.Cap.ROUND);
        mUnFillPaint.setColor(-2130706433);
        mHandler = new Handler();
        mPhoneImgDrawable = ContextCompat.getDrawable(context,R.drawable.ic_phone);
        LayoutInflater.from(context).inflate(R.layout.battery_widget_2x2,this,true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mHandler.removeCallbacksAndMessages(null);
        this.mHandler.postDelayed(this.mBatteryReloadRunnable, 2000L);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mBatteryPercentIV = findViewById(R.id.widget_battery_progress);
        mBatteryPercentTV = findViewById(R.id.widget_battery_text);
        x();
    }

    @Override
    public void setAppSuggestionViewMargin() {
        super.setAppSuggestionViewMargin();
        this.mHandler.removeCallbacksAndMessages(null);
        this.mHandler.postDelayed(this.mBatteryReloadRunnable, 2000L);
    }

    public final void x() {
        LinearLayoutCompat linearLayoutCompat = (LinearLayoutCompat) findViewById(R.id.battery_widget_content);
//        ((ConstraintLayout.LayoutParams) linearLayoutCompat.getLayoutParams()).setMargins(mMargin, mMargin, mMargin, mMargin);
        setTextAndBackgroundColor(linearLayoutCompat);
    }

    public class ReloadBatteryRunnable implements Runnable {
        @Override
        public final void run() {
            Intent registerReceiver = ContextHelper.registerReceiver(mLauncher,null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
            int level = registerReceiver.getIntExtra("level", 0);
            int scale = registerReceiver.getIntExtra("scale", 1);
            if (scale <= 0) {
                scale = 100;
            }
            int batteryPercent = (level * 100) / scale;
            ImageView imageView = mBatteryPercentIV;
            int i2 = mBatteryWidgetProgressSize;
            Bitmap createBitmap = Bitmap.createBitmap(i2, i2, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(createBitmap);
            float f2 = mBatteryWidgetProgressSize - mBatteryWidgetPaddingSize;
            mRectF.set(mBatteryWidgetPaddingSize, mBatteryWidgetPaddingSize, f2, f2);
            mUnFillPaint.setStrokeWidth(mBatteryWidgetPaddingSize);
            mFillPaint.setColor(batteryPercent < 10 ? -65536 : -16711936);
            mFillPaint.setStrokeWidth(mBatteryWidgetPaddingSize);
            canvas.drawArc(mRectF, 0.0f, 360.0f, false, mUnFillPaint);
            canvas.drawArc(mRectF, 270.0f, batteryPercent * 3.6f, false, mFillPaint);

            int i3 = (int) (mBatteryWidgetPaddingSize * 3.2d);
            int i4 = mBatteryWidgetProgressSize - i3;

            mPhoneImgDrawable.setBounds(i3, i3, i4, i4);
            mPhoneImgDrawable.draw(canvas);
            imageView.setImageBitmap(createBitmap);
            mBatteryPercentTV.setText(String.format("%s%%", batteryPercent));
        }
    }

}

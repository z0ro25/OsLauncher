package com.ios.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class ViewProgress extends View {

    Paint mPaint;
    float mProgress;

    public ViewProgress(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.parseColor("#asbe"));
        mPaint.setStrokeWidth(
                Math.max(getResources().getDisplayMetrics().widthPixels / 250.0f, 3.0f)
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float border = mPaint.getStrokeWidth() * .5f;
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(border,border, getWidth() - border, getHeight() - border, getHeight() * .5f, getHeight() * .5f, mPaint);
        float fill = border * 3;
        canvas.drawRoundRect(fill, fill, (getWidth() - fill * 2) * mProgress, getHeight() - fill, getHeight() * .5f, getHeight() * .5f, mPaint );
    }

    public void setPro(float pro){
        mProgress = pro;
        if (pro > 1.0f)
            mProgress = 1;
        if (pro < 0)
            mProgress = 0;
        invalidate();
    }
}

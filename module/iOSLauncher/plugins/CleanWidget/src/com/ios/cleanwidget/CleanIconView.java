package com.ios.cleanwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.amz.ios.ioslite.common.Partner;

public class CleanIconView extends androidx.appcompat.widget.AppCompatImageView {
    private static final String TAG = "CleanProgressView";
    private RectF oval;
    private int mWidth, mHeight;

    private Paint mPaint;
    private Paint mTextPaint;

    private static final int COLOR_GREEN = Color.parseColor("#1BD163");
    private static final int COLOR_ORANGE = Color.parseColor("#FF7F24");
    private static final int COLOR_RED = Color.parseColor("#FF3030");


    private float outerCircleRadiusParam;
    private float outerCircleWidthParam;
    private float innerCircleWidthParam;
    private float indicatorTextSizeParam;
    private float outerCircleOffsetParam;

    private float progress;
    private float centerX;
    private float centerY;


    private boolean mShowPercentText;
    private float indicatorTextSize;


    public CleanIconView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public CleanIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mShowPercentText = Partner.getBoolean(context, Partner.DEF_CLEAN_WIDGET_SHOW_PERCENT);
        if (mShowPercentText) {
            outerCircleRadiusParam = 0.36f;
            outerCircleWidthParam = 0.08f;
            innerCircleWidthParam = 0.08f;
            indicatorTextSizeParam = 0.24f;
        } else {
            outerCircleRadiusParam = 0.32f;
            outerCircleWidthParam = 0.15f;
            innerCircleWidthParam = 0.10f;
            indicatorTextSizeParam = 0.22f;
        }
        outerCircleOffsetParam = 0.5f - (outerCircleRadiusParam + outerCircleWidthParam / 2) + innerCircleWidthParam / 2;

        oval = new RectF();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeCap(Cap.ROUND);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        setProgress(0.7f);
    }


    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);

        oval.left = outerCircleOffsetParam * mWidth;
        oval.top = outerCircleOffsetParam * mWidth;
        oval.right = mWidth - outerCircleOffsetParam * mWidth;
        oval.bottom = mWidth - outerCircleOffsetParam * mWidth;
        centerX = mWidth / 2;
        centerY = mHeight / 2;

        indicatorTextSize = indicatorTextSizeParam * mWidth;

        setMeasuredDimension(mWidth, mHeight);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int color;
        if (progress >= 0.75) {
            color = COLOR_ORANGE;
        } else if (progress >= 0.85) {
            color = COLOR_RED;
        } else {
            color = COLOR_GREEN;
        }

        mPaint.setColor(color);
        mPaint.setAlpha(50);
        mPaint.setStrokeWidth(mWidth * outerCircleWidthParam);
        canvas.drawCircle(centerX, centerY, mWidth * outerCircleRadiusParam, mPaint);


        mPaint.setStrokeWidth(innerCircleWidthParam * mWidth);
        mPaint.setAlpha(255);
        int degree = (int) (progress * 360);
        canvas.drawArc(oval, -90, degree, false, mPaint);

        if (mShowPercentText) {
            mTextPaint.setTextSize(indicatorTextSize);
            mTextPaint.setColor(color);
            int percent = (int) (progress * 100);
            String indicatorText = percent + "%";
            float textWidth = mTextPaint.measureText(indicatorText);
            canvas.drawText(indicatorText, centerX - (textWidth / 2), centerY - (indicatorTextSize / 2) + indicatorTextSize - 3, mTextPaint);
        }
    }


    public void setProgress(float value) {
        if (progress != value) {
            progress = value;
            invalidate();
        }
    }

}

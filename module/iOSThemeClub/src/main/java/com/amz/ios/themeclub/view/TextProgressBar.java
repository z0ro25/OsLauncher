package com.amz.ios.themeclub.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ProgressBar;

/**
 * Created by lideqian on 16-11-23.
 */
public class TextProgressBar extends ProgressBar {
    private String mTextStr;
    private Paint mTextPaint;

    private int width;
    private int height;

    public TextProgressBar(Context context) {
        super(context);
        initText();
    }

    public TextProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initText();
    }

    public TextProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initText();
    }

    @Override
    public void setProgress(int progress) {
        setText(progress);
        super.setProgress(progress);

    }


    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            if (widthMode == MeasureSpec.AT_MOST) {
                if (widthSize < 3 * getScreenWidth() / 5 * 1.0) {
                    widthSize = (int) (3 * getScreenWidth() / 5 * 1.0);
                }
            } else {
                widthSize = (int) (3 * getScreenWidth() / 5 * 1.0);
            }
        }

        if (heightMode != MeasureSpec.EXACTLY) {
            if (heightMode == MeasureSpec.AT_MOST) {
                if (heightSize < dp2px(30)) {
                    heightSize = dp2px(30);
                }
            } else {
                heightSize = (int) (3 * getMeasuredWidth() / 5 * 1.0);
            }

        }
        width = widthSize;
        height = heightSize;
    }

    private int getScreenWidth() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    private int dp2px(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextPaint.setTextSize(35);
        float allHeight = fontMetrics.descent - fontMetrics.ascent;
        canvas.drawText(mTextStr, (float) (width * 1.0 / 2), (float) (height * 1.0 / 2 - allHeight / 2 - fontMetrics
                .ascent), mTextPaint);
    }

    private void initText() {
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.BLACK);
    }

    private void setText(int progress) {
        int i = (int) ((progress * 1.0f / getMax()) * 100);
        mTextStr = String.valueOf(i) + "%";
    }
}

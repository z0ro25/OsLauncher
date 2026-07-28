package com.amz.ios.launcher.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Scale Seekbar ;
 * Used in screen edit view;
 *
 * @author huangshuai;
 */
public class ScaleSeekBar extends View {
    private int mTopPadding;
    private int downX = 0;
    private int moveX = 0;
    private int upX = 0;
    private int mPerWidth;
    private Paint mPaint;
    private int mCurrentLevel;
    private SlideResponseOnTouch mSlideResponse;
    private int[] mColors = {Color.parseColor("#0F9F5A"), Color.parseColor("#DADADA")};
    private int mCircleRadius;
    private int mWidth;
    private int mLineWidth;
    private int mLineEnd;
    private int mLevelNum;
    private int mSelectedLevel = -1;

    public ScaleSeekBar(Context paramContext) {
        super(paramContext);
    }

    public ScaleSeekBar(Context paramContext, AttributeSet paramAttributeSet) {
        this(paramContext, paramAttributeSet, 0);
    }

    public ScaleSeekBar(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
        mCircleRadius = dip2px(paramContext, 8.0F);
        mPaint = new Paint(Paint.DITHER_FLAG);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(dip2px(getContext(), 2.0F));
        mTopPadding = getPaddingTop();
        mLevelNum = 7;
        mCurrentLevel = 3;
    }

    private void responseTouch(int x) {
        if (x <= mLineWidth - mCircleRadius) {
            mCurrentLevel = (x + mPerWidth / 3) / mPerWidth;
        } else {
            mCurrentLevel = mLevelNum - 1;
        }

        if (mSelectedLevel != mCurrentLevel && mSlideResponse != null) {
            mSlideResponse.onResponse(mCurrentLevel);
            mSelectedLevel = mCurrentLevel;
        }
        invalidate();
    }

    protected void onDraw(Canvas paramCanvas) {
        super.onDraw(paramCanvas);
        mPaint.setColor(this.mColors[1]);
        paramCanvas.drawLine(mCircleRadius, mCircleRadius + mTopPadding, mLineEnd, mCircleRadius + mTopPadding, mPaint);
        mPaint.setColor(this.mColors[0]);
        paramCanvas.drawLine(mCircleRadius, mCircleRadius * 2 / 3 + mTopPadding, mCircleRadius, mCircleRadius * 4 / 3 + mTopPadding, mPaint);
        paramCanvas.drawLine(mCircleRadius, mCircleRadius + mTopPadding, mCircleRadius + mPerWidth * this.mCurrentLevel, mCircleRadius + mTopPadding, mPaint);

        int level = 1;
        while (level < mLevelNum) {
            if (level < mCurrentLevel) {
                mPaint.setColor(mColors[0]);
            } else {
                mPaint.setColor(mColors[1]);
            }
            paramCanvas.drawLine(mCircleRadius + mPerWidth * level, mCircleRadius * 3 / 4 + mTopPadding, mCircleRadius + mPerWidth * level, mCircleRadius * 5 / 4 + mTopPadding, mPaint);

            level++;
        }


        paramCanvas.drawLine(mCircleRadius + mPerWidth * (mLevelNum - 1), mCircleRadius * 2 / 3 + mTopPadding, mLineEnd, mCircleRadius * 4 / 3 + mTopPadding, mPaint);

        mPaint.setColor(this.mColors[0]);
        paramCanvas.drawCircle(mCircleRadius + mPerWidth * mCurrentLevel, mCircleRadius + mTopPadding, mCircleRadius, mPaint);
    }

    protected void onMeasure(int paramInt1, int paramInt2) {
        super.onMeasure(paramInt1, paramInt2);
        mWidth = getMeasuredWidth();
        mLineWidth = (mWidth - mCircleRadius * 2);
        mPerWidth = (mLineWidth / (mLevelNum - 1));
        mLineEnd = (mCircleRadius + mPerWidth * (mLevelNum - 1));
    }

    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = (int) event.getX();
                responseTouch(downX);
                break;
            case MotionEvent.ACTION_MOVE:
                moveX = (int) event.getX();
                responseTouch(moveX);
                break;
            case MotionEvent.ACTION_UP:
                upX = (int) event.getX();
                responseTouch(upX);
                break;
        }
        return true;
    }

    public void setProgress(int level) {
        mSelectedLevel = mCurrentLevel = level;
        invalidate();
    }

    public void setSlideResponseOnTouch(SlideResponseOnTouch response) {
        this.mSlideResponse = response;
    }

    public interface SlideResponseOnTouch {
        void onResponse(int index);
    }

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
}

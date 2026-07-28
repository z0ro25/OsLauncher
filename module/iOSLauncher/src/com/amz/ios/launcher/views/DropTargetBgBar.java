package com.amz.ios.launcher.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.amz.ios.launcher.R;

/**
 * Created by server on 17-1-18.
 */
public class DropTargetBgBar extends View {

    private static final int mDefaultColor = Color.argb(40, 0, 0, 0);
    private RectF mTargetBarRect;
    private Paint mPaint;
    private float mScreenWidthPx;
    private float mScreenHeightPx;
    private float mTargetBarHeightPx;
    private float mCenterX;
    private float mCenterY;
    private float mRadius;
    private float mStartRadius;
    private float mEndRadius;
    private int mAlpha;
    private int mRed;
    private int mDropTargetBarTime;
    private boolean mDropEnter = false;
    private ValueAnimator mEnterAnimator;
    private ValueAnimator mExitAnimator;

    public DropTargetBgBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);

        mScreenWidthPx = dm.widthPixels;
        mScreenHeightPx = dm.heightPixels;

        final Resources res = getResources();
        mTargetBarHeightPx = res.getDimension(R.dimen.drop_target_bar_height);
        mDropTargetBarTime = res.getInteger(R.integer.config_dropTargetBarTime);

        mTargetBarRect = new RectF(0.0F, 0.0F, mScreenWidthPx, mTargetBarHeightPx);
        mCenterX = mScreenWidthPx / 2.0F;
        mCenterY = 0;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.clipRect(mTargetBarRect);

        if (mDropEnter) {
            mPaint.setColor(Color.argb(mAlpha, mRed, 0, 20));
            canvas.drawCircle(mCenterX, mCenterY, mRadius, mPaint);
            return;
        }

//        mPaint.setColor(mDefaultColor);
//        canvas.drawRect(mTargetBarRect, mPaint);
    }

    public void cancel() {
        stopAnimation();
    }


    private void startAnimation() {
        mEnterAnimator = ValueAnimator.ofFloat(0.0F, 1.0F);
        mEnterAnimator.setDuration(mDropTargetBarTime);
        mEnterAnimator.setInterpolator(new BgBarTimeInterpolator());
        mEnterAnimator.addUpdateListener(new EnterAnimUpdateListener());
        mEnterAnimator.addListener(new EnterAnimeListener());
        mEnterAnimator.start();
    }


    private void stopAnimation() {
        mExitAnimator = ValueAnimator.ofFloat(0.0F, 1.0F);
        mExitAnimator.setDuration(mDropTargetBarTime);
        mExitAnimator.setInterpolator(new BgBarTimeInterpolator());
        mExitAnimator.addUpdateListener(new ExitAnimUpdateListener());
        mExitAnimator.addListener(new ExitAnimeListener());
        mExitAnimator.start();
    }

    public void onTargetEnter(Rect location) {
        mCenterX = (location.left + (location.right - location.left) / 2);
        mStartRadius = ((float) Math.sqrt(Math.pow(Math.max(location.left, mScreenWidthPx - location.right) + (location.right - location.left) / 2, 2.0D) + Math.pow(mTargetBarHeightPx, 2.0D)));
        mEndRadius = Math.min(mScreenWidthPx / 4.0F, (location.right - location.left) / 2);
        startAnimation();
    }


    class BgBarTimeInterpolator implements TimeInterpolator {
        public float getInterpolation(float paramFloat) {
            if (paramFloat == 1.0F) {
                return 1.0F;
            }
            return (float) (1.0D - Math.pow(2.0D, -10.0F * paramFloat));
        }
    }

    class EnterAnimUpdateListener implements ValueAnimator.AnimatorUpdateListener {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float value = ((Float) animation.getAnimatedValue()).floatValue();
            update(1.0F - value, value);
        }

        public void update(float reduceValue, float newValue) {
            if (newValue < 1.0F) {
                mRadius = (mStartRadius - Math.abs(mStartRadius - mEndRadius) * newValue);
                mAlpha = (int) (40.0F + 160.0F * newValue);
                mRed = (int) (255.0F * newValue);
                invalidate();
            }
        }
    }

    class EnterAnimeListener
            extends AnimatorListenerAdapter {
        public void onAnimationEnd(Animator paramAnimator) {
            mRadius = mEndRadius;
            mAlpha = 200;
            mRed = 255;
            invalidate();
        }

        public void onAnimationStart(Animator paramAnimator) {
            if (mExitAnimator != null && mExitAnimator.isRunning()) {
                mExitAnimator.cancel();
            }
            mDropEnter = true;
        }
    }


    class ExitAnimUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float value = ((Float) animation.getAnimatedValue()).floatValue();
            update(1.0F - value, value);
        }

        public void update(float reduceValue, float newValue) {
            if (newValue < 1.0F) {
                mRadius = (mEndRadius + Math.abs(mStartRadius - mEndRadius) * newValue);
                mAlpha = (int) (200.0F - 160.0F * newValue);
                mRed = (int) (255.0F * reduceValue);
                invalidate();
            }
        }
    }

    class ExitAnimeListener extends AnimatorListenerAdapter {
        public void onAnimationEnd(Animator paramAnimator) {
            mRadius = mStartRadius;
            mAlpha = 40;
            mRed = 0;
            mDropEnter = false;
            invalidate();
        }

        public void onAnimationStart(Animator paramAnimator) {
            if (mEnterAnimator != null && mEnterAnimator.isRunning()) {
                mEnterAnimator.cancel();
            }
        }
    }


}

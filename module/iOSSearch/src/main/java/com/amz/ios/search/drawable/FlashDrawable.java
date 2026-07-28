package com.amz.ios.search.drawable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateInterpolator;

import com.amz.ios.ioslite.common.util.TimeUtil;

/**
 * Created by liaozhongjun on 2017/2/15.
 */

public class FlashDrawable extends Drawable implements Animatable {
    private Path mPath;
    private Paint mPaint;
    private int mAnimLeftPos;
    private ValueAnimator mAnimator;
    private TimeInterpolator interpolator = new AccelerateInterpolator();
    private static final int DURATION = 1500;
    private static final long DURATION_DELAYH = 1 * TimeUtil.MINUTE_OF_MILLISECONDS;

    private FlashEndListener mListener;
    private Handler mHandler;
    private boolean isRunning;

    public FlashEndListener getListener() {
        return mListener;
    }

    public void setListener(FlashEndListener listener) {
        mListener = listener;
    }

    public interface FlashEndListener {
        void onFlashEnd();
    }

    public FlashDrawable() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPath = new Path();
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        Shader linearGradient = new LinearGradient(0, 0, bounds.width(), 0,
                new int[]{0xaafafafa, 0x77fafafa, 0x00fafafa},
                new float[]{0, 0.5f, 1}, Shader.TileMode.CLAMP);
        mPaint.setShader(linearGradient);

    }

    private Path createPath(int left, int top, int right, int bottom) {
        mPath.reset();
        mPath.moveTo(right, bottom);
        mPath.lineTo(right, top);
        int w = 30;
        mPath.lineTo(left + w / 2, top);
        mPath.quadTo(left, (bottom + top) / 2, left + w / 2, bottom);
        mPath.lineTo(right, bottom);
        mPath.close();
        return mPath;
    }


    private ValueAnimator createAnimator() {
        final ValueAnimator animator = new ValueAnimator();
        animator.setInterpolator(interpolator);
        animator.setDuration(DURATION);
        final int r = 1200;
        animator.setIntValues(r, -20);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int v = (int) valueAnimator.getAnimatedValue();
                mAnimLeftPos = v;
                invalidateSelf();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                mAnimLeftPos = r;
                invalidateSelf();
                if (mListener != null) {
                    mListener.onFlashEnd();
                }
            }
        });
        return animator;
    }

    @Override
    public void start() {
        if (mAnimator == null) {
            mAnimator = createAnimator();
        }
        if (isRunning()) {
            return;
        }
        isRunning = true;
        mAnimator.start();
    }

    @Override
    public void stop() {
        if (mAnimator != null && isRunning) {
            mAnimator.end();
            isRunning = false;
        }
    }

    @Override
    public boolean isRunning() {
        return mAnimator != null && isRunning;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect rect = getBounds();
        canvas.drawPath(createPath(mAnimLeftPos, rect.top, rect.right, rect.bottom), mPaint);
    }

    @Override
    public void setAlpha(int i) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}

package com.amz.ios.launcher.widget;

import android.content.Context;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

public class IOSScroller {
    private static final int DEFAULT_DURATION = 250;
    private static final int DEFAULT_TIME_GAP = 15;
    private static final int FLING_MODE = 1;
    private static final int FLING_SCROLL_BACK_DURATION = 750;
    private static final int FLING_SCROLL_BACK_MODE = 3;
    private static final int FLING_SPRING_MODE = 2;
    private static final int GALLERY_LIST_MODE = 5;
    private static final int GALLERY_TIME_GAP = 25;
    private static final int SCROLL_LIST_MODE = 4;
    private static final int SCROLL_MODE = 0;
    final boolean DEBUG_SPRING;
    final String TAG;
    private float mCoeffX;
    private float mCoeffY;
    private int mCount;
    private int mCurrVX;
    private int mCurrVY;
    private int mCurrX;
    private int mCurrY;
    public final float mDeceleration;
    private float mDeltaX;
    private float mDeltaY;
    private int mDuration;
    private float mDurationReciprocal;
    private int mFinalX;
    private int mFinalY;
    private boolean mFinished;
    private Interpolator mInterpolator;
    private int mLastCurrY;
    private int mMaxX;
    private int mMaxY;
    private int mMinX;
    private int mMinY;
    private int mMode;
    private int mSpringOffsetX;
    private int mSpringOffsetY;
    private long mStartTime;
    private int mStartX;
    private int mStartY;
    private float mVelocity;
    private float mViscousFluidNormalize;
    private float mViscousFluidScale;

    public IOSScroller(Context context) {
        this(context, null);
    }

    public IOSScroller(Context context, Interpolator interpolator) {
        this.TAG = "IOSScroller";
        this.DEBUG_SPRING = false;
        this.mCoeffX = 0.0f;
        this.mCoeffY = 1.0f;
        this.mCount = 1;
        this.mLastCurrY = 0;
        this.mFinished = true;
        this.mInterpolator = interpolator;
        this.mDeceleration = 386.0878f * context.getResources().getDisplayMetrics().density * 160.0f * ViewConfiguration.getScrollFriction();
    }

    private float getInterpolation(float f) {
        return (1.0f - ((float) Math.exp((double) (-(8.0f * f))))) * 0.63212097f * this.mViscousFluidNormalize;
    }

    private float viscousFluid(float f) {
        float f2 = this.mViscousFluidScale * f;
        return (f2 >= 1.0f ? ((1.0f - ((float) Math.exp((double) (1.0f - f2)))) * 0.63212097f) + 0.367879f : f2 - (1.0f - ((float) Math.exp((double) (-f2))))) * this.mViscousFluidNormalize;
    }

    public void abortAnimation() {
        this.mCurrX = this.mFinalX;
        this.mCurrY = this.mFinalY;
        this.mFinished = true;
    }

    public boolean computeScrollOffset() {
        if (this.mFinished) {
            return false;
        }
        int currentAnimationTimeMillis = (int) (AnimationUtils.currentAnimationTimeMillis() - this.mStartTime);
        if (4 == this.mMode) {
            currentAnimationTimeMillis = this.mCount * 15;
        } else if (5 == this.mMode) {
            currentAnimationTimeMillis = this.mCount * 25;
        }
        if (currentAnimationTimeMillis < this.mDuration) {
            switch (this.mMode) {
                case 0:
                case 5:
                    float f = ((float) currentAnimationTimeMillis) * this.mDurationReciprocal;
                    float viscousFluid = this.mInterpolator == null ? viscousFluid(f) : this.mInterpolator.getInterpolation(f);
                    this.mCurrX = this.mStartX + Math.round(this.mDeltaX * viscousFluid);
                    this.mCurrY = Math.round(viscousFluid * this.mDeltaY) + this.mStartY;
                    if (this.mCurrX == this.mFinalX && this.mCurrY == this.mFinalY) {
                        this.mFinished = true;
                        break;
                    }
                case 1:
                    float f2 = ((float) currentAnimationTimeMillis) / 1000.0f;
                    float f3 = (this.mVelocity * f2) - (((this.mDeceleration * f2) * f2) / 2.0f);
                    this.mCurrX = this.mStartX + Math.round(this.mCoeffX * f3);
                    this.mCurrX = Math.min(this.mCurrX, this.mMaxX);
                    this.mCurrX = Math.max(this.mCurrX, this.mMinX);
                    this.mCurrY = Math.round(f3 * this.mCoeffY) + this.mStartY;
                    this.mCurrY = Math.min(this.mCurrY, this.mMaxY);
                    this.mCurrY = Math.max(this.mCurrY, this.mMinY);
                    float f4 = this.mVelocity - (f2 * this.mDeceleration);
                    this.mCurrVX = Math.round(this.mCoeffX * f4);
                    this.mCurrVY = Math.round(f4 * this.mCoeffY);
                    if (this.mCurrX == this.mFinalX && this.mCurrY == this.mFinalY) {
                        this.mFinished = true;
                        break;
                    }
                case 2:
                    float f5 = ((float) currentAnimationTimeMillis) / 1000.0f;
                    float f6 = (this.mVelocity * f5) - ((f5 * (this.mDeceleration * f5)) / 2.0f);
                    this.mCurrX = this.mStartX + Math.round(this.mCoeffX * f6);
                    this.mCurrX = Math.min(this.mCurrX, this.mMaxX);
                    this.mCurrX = Math.max(this.mCurrX, this.mMinX);
                    this.mCurrY = Math.round(f6 * this.mCoeffY) + this.mStartY;
                    this.mCurrY = Math.min(this.mCurrY, this.mMaxY);
                    this.mCurrY = Math.max(this.mCurrY, this.mMinY);
                    if (this.mCurrX == this.mFinalX || this.mCurrY == this.mFinalY) {
                        startScroll(this.mCurrX, this.mCurrY, (int) (this.mDeltaX + ((float) (this.mFinalX - this.mCurrX))), (int) (this.mDeltaY + ((float) (this.mFinalY - this.mCurrY))), FLING_SCROLL_BACK_DURATION);
                        this.mMode = 3;
                        break;
                    }
                case 3:
                    float viscousFluid2 = viscousFluid(((float) currentAnimationTimeMillis) * this.mDurationReciprocal);
                    this.mCurrX = this.mStartX + Math.round(this.mDeltaX * viscousFluid2);
                    this.mCurrY = Math.round(viscousFluid2 * this.mDeltaY) + this.mStartY;
                    if (this.mCurrX == this.mFinalX && this.mCurrY == this.mFinalY) {
                        this.mFinished = true;
                        break;
                    }
                case 4:
                    float interpolation = getInterpolation(((float) currentAnimationTimeMillis) * this.mDurationReciprocal);
                    this.mCurrX = this.mStartX + Math.round(this.mDeltaX * interpolation);
                    this.mCurrY = Math.round(interpolation * this.mDeltaY) + this.mStartY;
                    this.mCurrVY = Math.round((float) (((this.mCurrY - this.mLastCurrY) * 250) / 15));
                    this.mLastCurrY = this.mCurrY;
                    if ((this.mCurrX == this.mFinalX && this.mCurrY == this.mFinalY) || this.mCurrVY != 0) {
                        this.mFinalY = this.mCurrY;
                        this.mFinished = true;
                        break;
                    }
            }
        } else if (this.mMode != 2) {
            this.mCurrX = this.mFinalX;
            this.mCurrY = this.mFinalY;
            this.mFinished = true;
        } else {
            startScroll(this.mCurrX, this.mCurrY, (int) (this.mDeltaX + ((float) (this.mFinalX - this.mCurrX))), (int) (this.mDeltaY + ((float) (this.mFinalY - this.mCurrY))), FLING_SCROLL_BACK_DURATION);
            this.mMode = 3;
        }
        return true;
    }

    public void extendDuration(int i) {
        this.mDuration = timePassed() + i;
        this.mDurationReciprocal = 1.0f / ((float) this.mDuration);
        this.mFinished = false;
    }

    public void fling(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        float f = 1.0f;
        this.mMode = 1;
        this.mFinished = false;
        float hypot = (float) Math.hypot((double) i3, (double) i4);
        this.mVelocity = hypot;
        this.mDuration = (int) ((1000.0f * hypot) / this.mDeceleration);
        this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
        this.mStartX = i;
        this.mStartY = i2;
        this.mCoeffX = hypot == 0.0f ? 1.0f : ((float) i3) / hypot;
        if (hypot != 0.0f) {
            f = ((float) i4) / hypot;
        }
        this.mCoeffY = f;
        int i9 = (int) ((hypot * hypot) / (2.0f * this.mDeceleration));
        this.mMinX = i5;
        this.mMaxX = i6;
        this.mMinY = i7;
        this.mMaxY = i8;
        this.mFinalX = Math.round(((float) i9) * this.mCoeffX) + i;
        this.mFinalX = Math.min(this.mFinalX, this.mMaxX);
        this.mFinalX = Math.max(this.mFinalX, this.mMinX);
        this.mFinalY = Math.round(((float) i9) * this.mCoeffY) + i2;
        this.mFinalY = Math.min(this.mFinalY, this.mMaxY);
        this.mFinalY = Math.max(this.mFinalY, this.mMinY);
    }

    public void fling(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10) {
        fling(i, i2, i3, i4, i5 - i9, i6 + i9, i7 - i10, i8 + i10);
        this.mDeltaY = 0.0f;
        this.mDeltaX = 0.0f;
        if (this.mFinalX > i6 || this.mFinalX < i5) {
            this.mMode = 2;
            if (this.mFinalX > i6) {
                this.mDeltaX = (float) (i6 - this.mFinalX);
            } else {
                this.mDeltaX = (float) (i5 - this.mFinalX);
            }
        }
        if (this.mFinalY > i8 || this.mFinalY < i7) {
            this.mMode = 2;
            if (this.mFinalY > i8) {
                this.mDeltaY = (float) (i8 - this.mFinalY);
            } else {
                this.mDeltaY = (float) (i7 - this.mFinalY);
            }
        }
    }

    public final void forceFinished(boolean z) {
        this.mFinished = z;
    }

    public final int getCurrVX() {
        return this.mCurrVX;
    }

    public final int getCurrVY() {
        return this.mCurrVY;
    }

    public float getCurrVelocity() {
        return this.mVelocity - ((this.mDeceleration * ((float) timePassed())) / 2000.0f);
    }

    public final int getCurrX() {
        return this.mCurrX;
    }

    public final int getCurrY() {
        return this.mCurrY;
    }

    public final int getDuration() {
        return this.mDuration;
    }

    public final int getFinalX() {
        return this.mFinalX;
    }

    public final int getFinalY() {
        return this.mFinalY;
    }

    public final int getStartX() {
        return this.mStartX;
    }

    public final int getStartY() {
        return this.mStartY;
    }

    public final boolean isFinished() {
        return this.mFinished;
    }

    public void setCount(int i) {
        this.mCount = i;
    }

    public void setFinalX(int i) {
        this.mFinalX = i;
        this.mDeltaX = (float) (this.mFinalX - this.mStartX);
        this.mFinished = false;
    }

    public void setFinalY(int i) {
        this.mFinalY = i;
        this.mDeltaY = (float) (this.mFinalY - this.mStartY);
        this.mFinished = false;
    }

    public void startGalleryList(int i, int i2, int i3, int i4, int i5) {
        startScroll(i, i2, i3, i4, i5);
        this.mMode = 5;
        this.mCount = 1;
    }

    public void startScroll(int i, int i2, int i3, int i4) {
        startScroll(i, i2, i3, i4, 250);
    }

    public void startScroll(int i, int i2, int i3, int i4, int i5) {
        this.mMode = 0;
        this.mFinished = false;
        this.mDuration = i5;
        this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
        this.mStartX = i;
        this.mStartY = i2;
        this.mFinalX = i + i3;
        this.mFinalY = i2 + i4;
        this.mDeltaX = (float) i3;
        this.mDeltaY = (float) i4;
        this.mDurationReciprocal = 1.0f / ((float) this.mDuration);
        this.mViscousFluidScale = 8.0f;
        this.mViscousFluidNormalize = 1.0f;
        this.mViscousFluidNormalize = 1.0f / viscousFluid(1.0f);
    }

    public void startScrollList(int i, int i2, int i3, int i4, int i5) {
        startScroll(i, i2, i3, i4, i5);
        this.mMode = 4;
        this.mViscousFluidNormalize = 1.0f;
        this.mViscousFluidNormalize = 1.0f / getInterpolation(1.0f);
        this.mLastCurrY = 0;
        this.mCount = 1;
    }

    public int timePassed() {
        return (int) (AnimationUtils.currentAnimationTimeMillis() - this.mStartTime);
    }
}

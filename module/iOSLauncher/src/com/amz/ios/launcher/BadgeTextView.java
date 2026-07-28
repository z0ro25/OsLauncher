package com.amz.ios.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

/* loaded from: classes.dex */
public class BadgeTextView extends TextViewCustomFont {

    public Paint mPaint;
    public int mBgColor;
    public int mTextColor;
    public int mWidth;
    public int mHeight;
    public RectF mRectF = new RectF();

    static final int[] BadgeTextView = new int[] {
            R.attr.badgeBackgroundColor,
            R.attr.badgeTextColor
    };

    public BadgeTextView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public BadgeTextView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, 0);
        setGravity(17);

        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, BadgeTextView);
        this.mBgColor = obtainStyledAttributes.getColor(1, -65536);
        this.mTextColor = obtainStyledAttributes.getColor(0, -1);
        obtainStyledAttributes.recycle();

        this.mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mPaint.setTextAlign(Paint.Align.CENTER);
        this.mPaint.setTypeface(getTypeface());
    }

    public final void show(int count, boolean z) {
        String str = "";
        if (count > 0 && count <= 99) {
            str = String.valueOf(count);
        } else if (count <= 99) {
            setText("0");
            if (z) {
                setVisibility(View.GONE);
                invalidate();
                return;
            }
            else {
                setVisibility(View.VISIBLE);
                invalidate();
            }
        } else {
            str = "99+";
            setVisibility(View.VISIBLE);
        }
        setText(str);
        invalidate();
    }

    @Override
    public final void onDraw(Canvas canvas) {
        CharSequence text = getText();
        if (text == null) {
            return;
        }
        if (text.length() == 1) {
            this.mPaint.setColor(this.mBgColor);
            canvas.drawCircle(this.mWidth / 2.0f, this.mHeight / 2.0f, this.mHeight / 2.0f, this.mPaint);
        } else {
            this.mPaint.setColor(this.mBgColor);
            this.mRectF.set(
                    0.0f, 0.0f, this.mWidth, this.mHeight
            );
            canvas.drawRoundRect(mRectF, mHeight, mHeight, this.mPaint);
        }
        this.mPaint.setColor(this.mTextColor);
        this.mPaint.setTextSize((this.mHeight * 2) / 3.0f);
        canvas.drawText(text.toString(), this.mWidth / 2.0f, (this.mHeight * 0.73f), this.mPaint);
    }

    @Override
    public final void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mWidth = w;
        this.mHeight = h;
    }

    public void setBadgeCount(int i) {
        show(i, true);
    }

    public void setBadgeCount(String str) {
        int i;
        try {
            i = Integer.parseInt(str);
        } catch (Exception e) {
            e.getMessage();
            i = -1;
        }
        if (i != -1) {
            show(i, false);
        }
    }

    @Override
    public void setVisibility(int i) {
        if (getVisibility() != i) {
            if (i == VISIBLE) {
                setEnabled(true);
                ObjectAnimator animator = LauncherAnimUtils.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat("scaleX", 1.0f), PropertyValuesHolder.ofFloat("scaleY", 1.0f));
                animator.setDuration(300L);
                animator.addListener(new AnimatorStarter(this));
                if (Build.VERSION.SDK_INT >= 21) {
                    animator.setInterpolator(Launcher.initInterpolator(0.02f, 0.11f, 0.13f, 1.0f));
                }
                animator.start();
            } else if (i == INVISIBLE || i == GONE) {
                ObjectAnimator animator = LauncherAnimUtils.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat("scaleX", 0.0f), PropertyValuesHolder.ofFloat("scaleY", 0.0f));
                animator.setDuration(300L);
                animator.addListener(new AnimatorEnder(this));
                animator.setInterpolator(Launcher.initInterpolator(0.02f, 0.11f, 0.13f, 1.0f));
                animator.start();
            }
        }
    }

    public class AnimatorStarter extends AnimatorListenerAdapter {

        BadgeTextView mBadgeTextView;

        public AnimatorStarter(BadgeTextView badgeTextView) {
            super();
            mBadgeTextView = badgeTextView;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            mBadgeTextView.setVisibility(View.VISIBLE);
            super.onAnimationStart(animation);
        }
    }

    public final class AnimatorEnder extends AnimatorListenerAdapter {

        public final BadgeTextView mBadgeTextView;

        public AnimatorEnder(BadgeTextView badgeTextView) {
            this.mBadgeTextView = badgeTextView;
        }

        @Override
        public final void onAnimationEnd(Animator animator) {
            mBadgeTextView.setVisibility(View.GONE);
            super.onAnimationEnd(animator);
        }
    }

}

package com.amz.ios.launcher.leftpage.widgets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.TextViewCustomFont;

public class BlurConstraintLayoutWidget extends WidgetBaseLayout {

    Launcher mLauncher;
    int mState;
    ValueAnimator mValueAnimator;

    public BlurConstraintLayoutWidget(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public BlurConstraintLayoutWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = (Launcher) context;
    }

    public void setTextAndBackgroundColor(ViewGroup viewGroup) {
        setAllTextColor(viewGroup, -16777216);
        viewGroup.setBackgroundResource(R.drawable.widget_left_page_background);
    }

    public static void setAllTextColor(ViewGroup viewGroup, int color) {
        int childCount = viewGroup.getChildCount();
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = viewGroup.getChildAt(i3);
            if (childAt instanceof ViewGroup) {
                setAllTextColor((ViewGroup) childAt, color);
            } else if ((childAt instanceof TextViewCustomFont)) {
                ((TextViewCustomFont) childAt).setTextColor(color);
            }
        }
    }

    public final void w(View view, boolean visible) {
        int i;
        int i2;
        view.measure(-1, -2);
        int measuredHeight = view.getMeasuredHeight();

        if (visible) {
            view.setVisibility(View.VISIBLE);
            this.mState = 1;
            i2 = measuredHeight;
            i = 0;
        } else {
            this.mState = 0;
            i = measuredHeight;
            i2 = 0;
        }

        final int value = measuredHeight;
        final View v = view;

        mValueAnimator = ValueAnimator.ofInt(i, i2);
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                int intValue = (Integer) animation.getAnimatedValue();
                v.getLayoutParams().height = intValue;
                v.setAlpha(intValue * 1.f / value);
                v.requestLayout();
            }
        });
        this.mValueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                super.onAnimationEnd(animation);
                float f;
                if (mState == 0) {
                    v.setVisibility(View.GONE);
                    f = 0.0f;
                } else {
                    f = 1.0f;
                }
                v.setAlpha(f);
            }
        });
        this.mValueAnimator.setDuration((int) ((measuredHeight * 2.6f) / getResources().getDisplayMetrics().density));
        this.mValueAnimator.setInterpolator(Launcher.initInterpolator(0.25f, 0.1f, 0.25f, 1.0f));
        this.mValueAnimator.start();
    }
}

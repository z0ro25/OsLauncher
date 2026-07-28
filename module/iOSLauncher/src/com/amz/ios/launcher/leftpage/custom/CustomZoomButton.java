package com.amz.ios.launcher.leftpage.custom;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatButton;

import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAnimUtils;

public class CustomZoomButton extends AppCompatButton {

    public CustomZoomButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void setVisibility(final int visibility) {
        if (getVisibility() != visibility) {
            if (visibility == View.VISIBLE) {
                setEnabled(true);
                ObjectAnimator animator = LauncherAnimUtils.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat("scaleX", 1.0f), PropertyValuesHolder.ofFloat("scaleY", 1.0f));
                animator.setDuration(300L);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        CustomZoomButton.super.setVisibility(View.VISIBLE);
                        super.onAnimationStart(animation);
                    }
                });
                animator.setInterpolator(Launcher.initInterpolator(0.02f, 0.11f, 0.13f, 1.0f));
                animator.start();
            } else if (visibility == View.INVISIBLE || visibility == View.GONE) {
                ObjectAnimator animator = LauncherAnimUtils.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat("scaleX", 0.0f), PropertyValuesHolder.ofFloat("scaleY", 0.0f));
                animator.setDuration(300L);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        CustomZoomButton.super.setVisibility(visibility);
                    }
                });
                animator.setInterpolator(Launcher.initInterpolator(0.02f, 0.11f, 0.13f, 1.0f));
                animator.start();
            }
        }
    }
}

package com.amz.ios.launcher.leftpage.custom;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAnimUtils;

public class CustomZoomImageView extends AppCompatImageView {
    public CustomZoomImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void setVisibility(final int i) {
        if (getVisibility() != i) {
            if (i == VISIBLE) {
                setEnabled(true);
                ObjectAnimator animator = LauncherAnimUtils.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat("scaleX", 1.0f), PropertyValuesHolder.ofFloat("scaleY", 1.0f));
                animator.setDuration(300L);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        CustomZoomImageView.super.setVisibility(View.VISIBLE);
                    }
                });
                if (Build.VERSION.SDK_INT >= 21) {
                    animator.setInterpolator(Launcher.initInterpolator(0.02f, 0.11f, 0.13f, 1.0f));
                }
                animator.start();
            } else if (i == INVISIBLE || i == GONE) {
                ObjectAnimator animator = LauncherAnimUtils.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat("scaleX", 0.0f), PropertyValuesHolder.ofFloat("scaleY", 0.0f));
                animator.setDuration(300L);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        CustomZoomImageView.super.setVisibility(i);
                    }
                });
                if (Build.VERSION.SDK_INT >= 21) {
                    animator.setInterpolator(Launcher.initInterpolator(0.02f, 0.11f, 0.13f, 1.0f));
                }
                animator.start();
            }
        }
    }
}

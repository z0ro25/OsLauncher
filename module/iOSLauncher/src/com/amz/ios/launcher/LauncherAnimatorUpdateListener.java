package com.amz.ios.launcher;

import android.animation.ValueAnimator;

public abstract class LauncherAnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener {
    public abstract void onAnimationUpdate(float f, float f2);

    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        onAnimationUpdate(1.0f - floatValue, floatValue);
    }
}

package com.ios.boot.iosboot.animation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;

public class MorphingAnimation {

    private Animator.AnimatorListener mAnimatorListener;

    public MorphingAnimation(){

    }

    public void smallStart(final View v, long duration) {
        ObjectAnimator cornerAnimation = ObjectAnimator.ofFloat(v.getBackground(), "cornerRadius", 0, v.getHeight());
        cornerAnimation.setDuration(duration);
        final ObjectAnimator heightAnimation = ObjectAnimator.ofInt(v, "xxx", v.getHeight(), v.getHeight());
        heightAnimation.setDuration(duration);
        heightAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                v.getLayoutParams().height = (int) heightAnimation.getAnimatedValue();
                v.requestLayout();
            }
        });
        ObjectAnimator animator = ObjectAnimator.ofInt(v, "xx", v.getWidth(), v.getHeight()).setDuration(duration);
        animator.addUpdateListener(new ObjectAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int value = (int) valueAnimator.getAnimatedValue();
                v.getLayoutParams().width = value;
                v.requestLayout();
            }
        });
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(cornerAnimation).with(heightAnimation).with(animator);
        if(mAnimatorListener!=null){
            animatorSet.addListener(mAnimatorListener);
        }
        animatorSet.start();
    }

    public void setAnimatorListener(Animator.AnimatorListener animatorListener){
        mAnimatorListener = animatorListener;
    }
}
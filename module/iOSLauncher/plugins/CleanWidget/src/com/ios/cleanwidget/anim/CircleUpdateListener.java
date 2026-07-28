package com.ios.cleanwidget.anim;

import android.animation.ValueAnimator;

import com.ios.cleanwidget.CleanCircleView;

/**
 * Created by server on 16-11-3.
 */
public class CircleUpdateListener implements ValueAnimator.AnimatorUpdateListener {
    private CleanCircleView circleView;

    public CircleUpdateListener(CleanCircleView view) {
        circleView = view;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float f = ((Float) animation.getAnimatedValue()).floatValue();
        circleView.background.setScaleX(f);
        circleView.background.setScaleY(f);
        circleView.inClicle.setScaleX(f);
        circleView.inClicle.setScaleY(f);
        circleView.outCircle.setScaleX(f);
        circleView.outCircle.setScaleY(f);
    }
}

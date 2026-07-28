package com.ios.cleanwidget.anim;

import android.animation.ValueAnimator;

import com.ios.cleanwidget.CleanCircleView;

/**
 * Created by server on 16-11-3.
 */
public class OutCircleUpdateListener implements ValueAnimator.AnimatorUpdateListener{
    private CleanCircleView circleView;

    public OutCircleUpdateListener(CleanCircleView view){
        circleView = view;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float f = ((Float)animation.getAnimatedValue()).floatValue();
        circleView.outCircle.setScaleX(f);
        circleView.outCircle.setScaleY(f);
    }
}

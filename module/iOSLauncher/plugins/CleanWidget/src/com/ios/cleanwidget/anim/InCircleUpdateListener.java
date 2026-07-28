package com.ios.cleanwidget.anim;

/**
 * Created by server on 16-11-3.
 */

import android.animation.ValueAnimator;

import com.ios.cleanwidget.CleanCircleView;

/**
 * Created by server on 16-11-3.
 */
public class InCircleUpdateListener implements ValueAnimator.AnimatorUpdateListener{
    private CleanCircleView circleView;

    public InCircleUpdateListener(CleanCircleView view){
        circleView = view;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float f = ((Float)animation.getAnimatedValue()).floatValue();
        circleView.inClicle.setScaleX(f);
        circleView.inClicle.setScaleY(f);
    }
}

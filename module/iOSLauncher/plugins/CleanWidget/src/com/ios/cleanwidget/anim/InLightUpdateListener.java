package com.ios.cleanwidget.anim;

import android.animation.ValueAnimator;

import com.ios.cleanwidget.CleanCircleView;

/**
 * Created by server on 16-11-3.
 */
public class InLightUpdateListener implements ValueAnimator.AnimatorUpdateListener{
    private CleanCircleView circleView;

    public InLightUpdateListener(CleanCircleView view){
        circleView = view;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float f = ((Float)animation.getAnimatedValue()).floatValue();
        circleView.inLight.setScaleX(f);
        circleView.inLight.setScaleY(f);
    }
}

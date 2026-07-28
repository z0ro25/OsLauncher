package com.ios.cleanwidget.anim;

import android.animation.ValueAnimator;

import com.ios.cleanwidget.CleanCircleView;

/**
 * Created by server on 16-11-3.
 */
public class OutLightUpdateListener implements ValueAnimator.AnimatorUpdateListener{
    private CleanCircleView circleView;

    public OutLightUpdateListener(CleanCircleView view){
        circleView = view;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float f = ((Float)animation.getAnimatedValue()).floatValue();
        circleView.outLight.setScaleX(f);
        circleView.outLight.setScaleY(f);
    }
}


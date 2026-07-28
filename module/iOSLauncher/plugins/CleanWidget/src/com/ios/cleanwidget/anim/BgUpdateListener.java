package com.ios.cleanwidget.anim;

import android.animation.ValueAnimator;

import com.ios.cleanwidget.CleanCircleView;

/**
 * Created by server on 16-11-3.
 */
public class BgUpdateListener implements ValueAnimator.AnimatorUpdateListener{
    private CleanCircleView circleView;
    public BgUpdateListener(CleanCircleView view){
        circleView = view;
    }
    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float f = ((Float)animation.getAnimatedValue()).floatValue();
        circleView.background.setScaleX(f);
        circleView.background.setScaleY(f);
    }
}

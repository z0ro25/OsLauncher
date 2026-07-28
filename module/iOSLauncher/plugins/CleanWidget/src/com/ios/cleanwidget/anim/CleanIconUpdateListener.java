package com.ios.cleanwidget.anim;

import android.animation.ValueAnimator;

import com.ios.cleanwidget.CleanCircleView;
import com.ios.cleanwidget.CleanIconView;

/**
 * Created by server on 16-11-4.
 */
public class CleanIconUpdateListener implements ValueAnimator.AnimatorUpdateListener {
    private CleanIconView iconView;

    public CleanIconUpdateListener(CleanIconView view) {
        iconView = view;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float f = ((Float) animation.getAnimatedValue()).floatValue();
        iconView.setScaleX(f);
        iconView.setScaleY(f);
    }
}

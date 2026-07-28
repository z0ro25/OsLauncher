package com.ios.cleanwidget.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import com.ios.cleanwidget.CleanCircleView;

/**
 * Created by server on 16-11-3.
 */
public class OutLightAnimatorListener extends AnimatorListenerAdapter {
    private CleanCircleView circleView;
    public OutLightAnimatorListener(CleanCircleView view){
        circleView = view;
    }

    public void onAnimationEnd(Animator paramAnimator)
    {
        circleView.outLight.setAlpha(0.0F);
        circleView.outLight.setScaleX(1.0F);
        circleView.outLight.setScaleY(1.0F);
    }
}

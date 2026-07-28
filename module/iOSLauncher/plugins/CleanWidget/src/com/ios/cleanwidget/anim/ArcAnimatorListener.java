package com.ios.cleanwidget.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

import com.ios.cleanwidget.CleanCircleView;
import com.ios.cleanwidget.CleanWidgetView;

/**
 * Created by server on 16-11-3.
 */
public class ArcAnimatorListener extends AnimatorListenerAdapter {
    private CleanCircleView circleView;

    public ArcAnimatorListener(CleanCircleView view) {
        circleView = view;
    }

    public void onAnimationEnd(Animator paramAnimator) {
        View view;
        int i = 0;
        while (i <circleView.arcLayout.getChildCount()) {
            view = circleView.arcLayout.getChildAt(i);
            view.setScaleX(0.0F);
            view.setScaleY(0.0F);
            view.setAlpha(0.0F);
            i += 1;
        }
        circleView.arcLayout.setScaleX(1.0F);
        circleView.arcLayout.setScaleY(1.0F);
    }


}

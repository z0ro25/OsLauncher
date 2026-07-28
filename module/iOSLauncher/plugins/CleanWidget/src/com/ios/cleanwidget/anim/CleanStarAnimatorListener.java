package com.ios.cleanwidget.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

import com.ios.cleanwidget.CleanCircleView;

/**
 * Created by server on 16-11-3.
 */
public class CleanStarAnimatorListener extends AnimatorListenerAdapter {
    private CleanCircleView circleView;

    public CleanStarAnimatorListener(CleanCircleView view) {
        circleView = view;
    }

    public void onAnimationEnd(Animator paramAnimator) {
        circleView.cleanStart.setVisibility(View.GONE);
        circleView.animateShowLauncher();
    }

    public void onAnimationStart(Animator paramAnimator) {
        circleView.animateHideLauncher();
        circleView.cleanStart.setVisibility(View.VISIBLE);
        circleView.cleanStart.setRotation(0);
        circleView.cleanStart.setScaleX(1.0f);
        circleView.cleanStart.setScaleY(1.0f);
    }
}

package com.ios.cleanwidget.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.util.Log;
import android.view.View;

import com.ios.cleanwidget.CleanWidgetView;

/**
 * Created by server on 16-11-3.
 */
public class CleanAnimatorListener extends AnimatorListenerAdapter {
    private CleanWidgetView mCleanWidgetView;

    public CleanAnimatorListener(CleanWidgetView view) {
        mCleanWidgetView = view;
    }

    public void onAnimationEnd(Animator paramAnimator) {
        mCleanWidgetView.postIconZoomInAnimator();
    }
}


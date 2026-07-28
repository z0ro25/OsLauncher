package com.amz.ios.ioslite.common.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;

/**
 * A convenience class to update a view's visibility state after an alpha animation.
 * <p/>
 * Created by huangshuai;
 */
public class AlphaUpdateListener extends AnimatorListenerAdapter implements ValueAnimator.AnimatorUpdateListener {
    private static final float ALPHA_CUTOFF_THRESHOLD = 0.01f;

    private View mView;

    public AlphaUpdateListener(View v) {
        mView = v;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator arg0) {
        updateVisibility(mView);
    }


    @Override
    public void onAnimationEnd(Animator arg0) {
        updateVisibility(mView);
    }

    @Override
    public void onAnimationStart(Animator arg0) {
        // We want the views to be visible for animation, so fade-in/out is visible
        mView.setVisibility(View.VISIBLE);
    }

    public static void updateVisibility(View view) {
        // We want to avoid the extra layout pass by setting the views to GONE;
        int invisibleState = View.GONE;
        if (view.getAlpha() < ALPHA_CUTOFF_THRESHOLD && view.getVisibility() != invisibleState) {
            view.setVisibility(invisibleState);
        } else if (view.getAlpha() > ALPHA_CUTOFF_THRESHOLD
                && view.getVisibility() != View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
        }
    }

}


package com.amz.ios.launcher;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.amz.ios.ioslite.common.anim.PropertyHolderUtis;

public class LoadingView extends RelativeLayout {
    private boolean mIsVisible;
    private Launcher mLauncher;

    private View imgLoading;

    private ObjectAnimator mRotate;

    public LoadingView(Context context) {
        this(context, null);
    }

    public LoadingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = (Launcher) context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        imgLoading = findViewById(R.id.ic_loading);
    }


    public void show() {
        mIsVisible = true;
        final DragLayer dragLayer = mLauncher.getDragLayer();
        dragLayer.setVisibility(INVISIBLE);
        setVisibility(VISIBLE);
        setAlpha(1.0f);
        imgLoading.setScaleX(1.0f);
        imgLoading.setScaleY(1.0f);

        mRotate = ObjectAnimator.ofPropertyValuesHolder(imgLoading, PropertyHolderUtis.rotation(0, 360));
        mRotate.setDuration(1000);
        mRotate.setRepeatCount(ValueAnimator.INFINITE);
        mRotate.start();
    }

    public void dismiss() {
        mIsVisible = false;
        cancelRotate();
        final int duration = 240;
        final DragLayer dragLayer = mLauncher.getDragLayer();
        dragLayer.setAlpha(0f);
        dragLayer.setVisibility(VISIBLE);
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleDragLayer = ObjectAnimator.ofPropertyValuesHolder(dragLayer,
                PropertyHolderUtis.alpha(0f, 1.0f));
        scaleDragLayer.setDuration(duration);

        ObjectAnimator alpha = ObjectAnimator.ofPropertyValuesHolder(this,
                PropertyHolderUtis.alpha(1.0f, 0f));
        alpha.setDuration(duration);

        animatorSet.playTogether(scaleDragLayer, alpha);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                LoadingView.this.setVisibility(GONE);
            }
        });

        animatorSet.start();
    }

    private void cancelRotate() {
        if (mRotate != null) {
            mRotate.cancel();
        }
        mRotate = null;
    }

    public boolean isVisible() {
        return mIsVisible;
    }
}

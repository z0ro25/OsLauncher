package com.amz.ios.launcher.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.ios.boot.iosboot.LauncherGuideManager;
import com.amz.ios.launcher.BorderCropDrawable;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAnimUtils;
import com.amz.ios.launcher.LogDecelerateInterpolator;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.util.Thunk;

/**
 * Created by huangshuai on 16-12-14.
 */
public class LauncherGuide {
    private static final String ALL_APPS_GUIDE_DISMISSED_KEY = "all_apps_guide_dismissed";


    private static final int SHOW_GUIDE_DURATION = 250;
    private static final int DISMISS_GUIDE_DURATION = 250;

    private Launcher mLauncher;
    private LayoutInflater mInflater;


    private ObjectAnimator mIndicatorAnimator;

    /**
     * Ctor
     */
    public LauncherGuide(Launcher launcher) {
        mLauncher = launcher;
        mInflater = LayoutInflater.from(mLauncher);
    }


    public void showAllAppsGuide() {
        LauncherGuideManager.getInstance(mLauncher.getApplicationContext()).markAllAppsGuideShown();
        ViewGroup root = (ViewGroup) mLauncher.findViewById(R.id.launcher);
        View guide = mInflater.inflate(R.layout.all_apps_guide_view, root, false);
        final View indicator = guide.findViewById(R.id.guide_indicator);

        PropertyValuesHolder scaleY =
                PropertyValuesHolder.ofFloat("scaleY", 1.0f, 0.9f);
        PropertyValuesHolder scaleX =
                PropertyValuesHolder.ofFloat("scaleX", 1.0f, 0.9f);
        mIndicatorAnimator = ObjectAnimator.ofPropertyValuesHolder(indicator, scaleX, scaleY);
        mIndicatorAnimator.setDuration(300);
        mIndicatorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mIndicatorAnimator.setRepeatCount(ValueAnimator.INFINITE);
        guide.post(new Runnable() {
            @Override
            public void run() {
                mIndicatorAnimator.start();
            }
        });

        final View.OnClickListener onClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismissAllAppsGuide(false);
            }
        };

        guide.setOnClickListener(onClickListener);
        guide.findViewById(R.id.guide_card_got_it).setOnClickListener(onClickListener);

        guide.findViewById(R.id.guide_allapps).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismissAllAppsGuide(true);
            }
        });

        guide.setAlpha(0);
        guide.setScaleX(0.8f);
        root.addView(guide);

        ObjectAnimator panelAlpha = ObjectAnimator.ofFloat(guide, "alpha", new float[]{0.0F, 1.0f}).setDuration(300);
        ObjectAnimator panelScaleX = ObjectAnimator.ofFloat(guide, "scaleX", new float[]{0.8F, 1.0F}).setDuration(300);
        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(panelAlpha, panelScaleX);
        guide.postDelayed(new Runnable() {
            @Override
            public void run() {
                animatorSet.start();
            }
        }, 50);
    }
    private void dismissAllAppsGuide(final boolean showAllApps) {
        mIndicatorAnimator.cancel();
        final View guideView = mLauncher.findViewById(R.id.all_apps_guide);
        ObjectAnimator panelAlpha = ObjectAnimator.ofFloat(guideView, "alpha", new float[]{1.0F, 0.0f}).setDuration(DISMISS_GUIDE_DURATION);
        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(panelAlpha);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                guideView.setVisibility(View.GONE);
            }
        });

        animatorSet.start();
    }
}

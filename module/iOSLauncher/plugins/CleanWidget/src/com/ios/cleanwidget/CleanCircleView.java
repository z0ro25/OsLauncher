package com.ios.cleanwidget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.ios.cleanwidget.anim.ArcAnimatorListener;
import com.ios.cleanwidget.anim.BgUpdateListener;
import com.ios.cleanwidget.anim.CircleUpdateListener;
import com.ios.cleanwidget.anim.CleanStarAnimatorListener;
import com.ios.cleanwidget.anim.FourInterpolator;
import com.ios.cleanwidget.anim.OutCircleUpdateListener;
import com.ios.cleanwidget.anim.OutLightAnimatorListener;
import com.ios.cleanwidget.anim.ThreeInterpolator;
import com.ios.cleanwidget.anim.InCircleUpdateListener;
import com.ios.cleanwidget.anim.InLightUpdateListener;
import com.ios.cleanwidget.anim.OutLightUpdateListener;
import com.ios.cleanwidget.arc.ArcLayout;
import com.amz.ios.ioslite.common.anim.PropertyHolderUtis;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by server on 16-11-3.
 */
public class CleanCircleView extends FrameLayout {
    private static final String TAG = "CleanCircleView";
    private static int ANIM_DURATION = 4000;

    public ImageView background;
    public ImageView inLight;
    public ImageView inClicle;
    public ImageView outLight;
    public ImageView outCircle;
    public ImageView cleanStart;

    public ArcLayout arcLayout;

    private ValueAnimator circleZoomIn;
    private ValueAnimator bgZoomOut;
    private ValueAnimator inLightZoomIn;
    private ValueAnimator inCircleZoomOut;
    private ValueAnimator outLightZoomOut;
    private Animator outLightFadeIn;
    private ValueAnimator inLightZoomOut;
    private ValueAnimator outCircleZoomOut;
    private Animator outCircleRotate;
    private Animator inCircleRotate;
    private AnimatorSet mCircleAnimator;
    private AnimatorSet mGloabalAnimator;

    private Launcher mLauncher;
    private CleanWidgetView mWidgetView;

    Runnable mAnimatorSetRunnable = new Runnable() {
        @Override
        public void run() {
            Log.w(TAG, " mAnimatorSetRunnable run ");
            mGloabalAnimator.start();
            invalidate();
        }
    };


    public void start() {
        Log.w(TAG, " start animator ");
        if (mGloabalAnimator == null) {
            Log.w(TAG, " mGloabalAnimator is null ");
            return;
        }

        if (mGloabalAnimator.isRunning()) {
            Log.w(TAG, " mGloabalAnimator is running ");
            return;
        }

        post(mAnimatorSetRunnable);
    }

    public CleanCircleView(Context context) {
        this(context, null);
    }

    public CleanCircleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CleanCircleView(Context context, AttributeSet attrs, int def) {
        super(context, attrs, def);
        mLauncher = (Launcher) context;
        init();
    }


    private void init() {
        LayoutInflater infalter = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        View contentView = infalter.inflate(R.layout.clean_widget_circle_view, this, true);
        background = (ImageView) contentView.findViewById(R.id.iv_clean_bg);
        inLight = (ImageView) contentView.findViewById(R.id.iv_in_light);
        inClicle = (ImageView) contentView.findViewById(R.id.iv_in_circle);
        outLight = (ImageView) contentView.findViewById(R.id.iv_out_light);
        outCircle = (ImageView) contentView.findViewById(R.id.iv_out_circle);
        cleanStart = (ImageView) contentView.findViewById(R.id.clean_start);
        arcLayout = (ArcLayout) contentView.findViewById(R.id.arcLayout);

        initAnim();
    }

    private void initAnim() {
        background.setScaleX(0);
        background.setScaleY(0);
        inLight.setScaleX(0);
        inLight.setScaleY(0);
        inClicle.setScaleX(0);
        inClicle.setScaleY(0);
        outLight.setAlpha(0.0F);
        outCircle.setScaleX(0);
        outCircle.setScaleY(0);

        mCircleAnimator = new AnimatorSet();
        mGloabalAnimator = new AnimatorSet();

        // background, outCircle, inCircle scale from 0 to 1.0f;
        circleZoomIn = ValueAnimator.ofFloat(0.0F, 1.0F);
        circleZoomIn.setDuration(450);
        circleZoomIn.setInterpolator(new ThreeInterpolator());
        circleZoomIn.addUpdateListener(new CircleUpdateListener(this));

        // outCircle rotate always;
        outCircleRotate = ObjectAnimator.ofFloat(outCircle, "rotation", 0.0F, -720.0F);
        outCircleRotate.setInterpolator(new LinearInterpolator());
        outCircleRotate.setDuration(ANIM_DURATION);


        //  background, cleanstar  scale to 0 at end;
        inLightZoomOut = ValueAnimator.ofFloat(1.0F, 0.0F);
        inLightZoomOut.setInterpolator(new LinearInterpolator());
        inLightZoomOut.addUpdateListener(new InLightUpdateListener(this));
        inLightZoomOut.setStartDelay(ANIM_DURATION - 400);
        inLightZoomOut.setDuration(400L);

        bgZoomOut = ValueAnimator.ofFloat(1.0F, 0.0F);
        bgZoomOut.setInterpolator(new LinearInterpolator());
        bgZoomOut.addUpdateListener(new BgUpdateListener(this));
        bgZoomOut.setStartDelay(ANIM_DURATION - 400);
        bgZoomOut.setDuration(400L);

        AnimatorSet animatorSet1 = new AnimatorSet();
        animatorSet1.playTogether(outCircleRotate, bgZoomOut, inLightZoomOut);

        // inCircle rotate always;
        inCircleRotate = ObjectAnimator.ofFloat(inClicle, "rotation", 0.0F, 1080.0F);
        inCircleRotate.setInterpolator(new ThreeInterpolator());
        inCircleRotate.setDuration(ANIM_DURATION);

        inCircleZoomOut = ValueAnimator.ofFloat(1.0F, 0.0F);
        inCircleZoomOut.setInterpolator(new LinearInterpolator());
        inCircleZoomOut.addUpdateListener(new InCircleUpdateListener(this));
        inCircleZoomOut.setStartDelay(ANIM_DURATION - 400);
        inCircleZoomOut.setDuration(400L);

        AnimatorSet animatorSet2 = new AnimatorSet();
        animatorSet2.playTogether(inCircleRotate, inCircleZoomOut);

        outLightZoomOut = ValueAnimator.ofFloat(1.0F, 0.0F);
        outLightZoomOut.setInterpolator(new FourInterpolator());
        outLightZoomOut.addUpdateListener(new OutLightUpdateListener(this));
        outLightZoomOut.setDuration(2600L);

        outLightFadeIn = ObjectAnimator.ofFloat(outLight, "alpha", 0.0F, 1.0F);
        outLightFadeIn.setDuration(400L);

        AnimatorSet animatorSet3 = new AnimatorSet();
        animatorSet3.addListener(new OutLightAnimatorListener(this));
        animatorSet3.playSequentially(outLightFadeIn, outLightZoomOut);
        animatorSet3.setStartDelay(1000L);


        inLightZoomIn = ValueAnimator.ofFloat(0.0F, 1.0F);
        inLightZoomIn.addUpdateListener(new InLightUpdateListener(this));
        inLightZoomIn.setStartDelay(1000L);
        inLightZoomIn.setDuration(700L);

        outCircleZoomOut = ValueAnimator.ofFloat(1.0F, 0.0F);
        outCircleZoomOut.addUpdateListener(new OutCircleUpdateListener(this));
        outCircleZoomOut.setStartDelay(1600L);
        outCircleZoomOut.setDuration(300L);

        AnimatorSet animatorSet4 = new AnimatorSet();
        animatorSet4.playSequentially(inLightZoomIn, outCircleZoomOut);

        mCircleAnimator.playTogether(circleZoomIn, animatorSet1, animatorSet2, animatorSet3, animatorSet4, getIconAnimSet());
        mGloabalAnimator.playSequentially(mCircleAnimator, getStarLightAnim());
    }

    private AnimatorSet getIconAnimSet() {
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(arcLayout,
                PropertyHolderUtis.rotation(0.0f, 1440.f),
                PropertyHolderUtis.scaleX(1.0F, 0.0F),
                PropertyHolderUtis.scaleY(1.0F, 0.0F));
        animator.setStartDelay(1500L);
        animator.setDuration(2000L);
        animator.setInterpolator(new AccelerateInterpolator());

        List<Animator> animatorList = new ArrayList<>();
        for (int i = 0; i < arcLayout.getChildCount(); i++) {
            animatorList.add(getZoomOutAmimator(arcLayout.getChildAt(i)));
        }

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(animatorList);
        animatorSet.setStartDelay(1400);

        AnimatorSet animatorSetArc = new AnimatorSet();
        animatorSetArc.playTogether(animatorSet, animator);
        animatorSetArc.addListener(new ArcAnimatorListener(this));
        return animatorSetArc;
    }

    private AnimatorSet getStarLightAnim() {
        ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(cleanStart, "rotation", 0.0F, -270.0F);
        rotateAnimator.setInterpolator(new LinearInterpolator());
        rotateAnimator.setDuration(640L);

        ObjectAnimator zoomOutAnimator = ObjectAnimator.ofPropertyValuesHolder(cleanStart,
                PropertyHolderUtis.scaleX(1.0F, 0.0F), PropertyHolderUtis.scaleY(1.0F, 0.0F));
        zoomOutAnimator.setInterpolator(new LinearInterpolator());
        zoomOutAnimator.setStartDelay(400L);
        zoomOutAnimator.setDuration(240L);

        AnimatorSet localAnimatorSet = new AnimatorSet();
        localAnimatorSet.playTogether(rotateAnimator, zoomOutAnimator);
        localAnimatorSet.addListener(new CleanStarAnimatorListener(this));
        return localAnimatorSet;
    }

    private Animator getZoomOutAmimator(View view) {
        view.setVisibility(VISIBLE);
        view.setScaleX(0.0F);
        view.setScaleY(0.0F);
        view.setAlpha(0.0F);
        Animator animator = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyHolderUtis.scaleX(0.0F, 1.0F),
                PropertyHolderUtis.scaleY(0.0F, 1.0F),
                PropertyHolderUtis.alpha(0.0F, 1.0F));
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(100L);
        return animator;
    }


    public void setAnimtorListener(AnimatorListenerAdapter paramAnimatorListenerAdapter) {
        if (mGloabalAnimator == null) {
            return;
        }
        mGloabalAnimator.addListener(paramAnimatorListenerAdapter);
    }

    public void setWidgetView(CleanWidgetView view) {
        mWidgetView = view;
    }


    public void animateHideLauncher() {
        mWidgetView.animateLauncherStartClean();
    }

    public void animateShowLauncher() {
        mWidgetView.animateLauncherEndClean();
    }

}

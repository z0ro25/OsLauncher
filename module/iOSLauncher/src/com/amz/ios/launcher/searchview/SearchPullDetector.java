package com.amz.ios.launcher.searchview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAnimUtils;

public class SearchPullDetector {

    public GestureDetector mGestureDetector;
    public Launcher mLauncher;
    public SpringAnimation mSpringAnimation;
    public int mTouchSlop;
    public int mMinimumFlingVelocity;
    public SearchViewLayout mSearchViewLayout;

    public boolean isContinueScroll;
    public int borderCenter;
    public int mHeight;
    public float borderTop;
    public float borderBottom;
    public boolean i = true;

    public class PullEndAnimListenerAdapter extends AnimatorListenerAdapter {
        @Override
        public void onAnimationEnd(Animator animation) {
            if (mSearchViewLayout.mState == SearchViewLayout.SearchViewState.CLOSED) {
                return;
            }
            mSearchViewLayout.mState = SearchViewLayout.SearchViewState.CLOSED;
            mSearchViewLayout.mSearchKeyEDT.removeTextChangedListener(mSearchViewLayout.mSearchKeywordTextWatcher);
            mSearchViewLayout.mSearchKeyEDT.setText("");
            mSearchViewLayout.setClearBtnVisible(false);
            mSearchViewLayout.setVoiceBtnVisible(true);
            mSearchViewLayout.postOnAnimationDelayed(new Runnable() {
                @Override
                public void run() {
                    mSearchViewLayout.clearFocus();
                }
            }, 200L);
            if (mSearchViewLayout.mOtherSearchLayout != null && mSearchViewLayout.mSearchKeyEDT.length() == 0) {
                mSearchViewLayout.mOtherSearchLayout.setVisibility(View.GONE);
            }
            mSearchViewLayout.setVisibility(View.GONE);
            if (mSearchViewLayout.mSearchViewAdapter != null) {
                mSearchViewLayout.mSearchViewAdapter.getFilter().filter(null);
            }
            if (mSearchViewLayout.mSearchViewLayoutDelegate != null) {
                mSearchViewLayout.mSearchViewLayoutDelegate.onSearchViewClosed();
            }
        }
    }

    public class PullGestureDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            if (mSearchViewLayout.isOpening()){
                float y = e2.getY() - e1.getY();
                if (Math.abs(borderTop) > mTouchSlop){
                    float absVy = Math.abs(velocityY);
                    if (absVy > mMinimumFlingVelocity && y > 0){
                        open(borderTop, velocityY);
                        return true;
                    }
                    else {
                        close(borderTop);
                    }
                }
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {

            if (isContinueScroll){
                borderTop += distanceY;
                borderBottom += distanceY;
                float min = Math.min(1.0f, (1.0f - Math.min(new AccelerateInterpolator(0.18f).getInterpolation(0.39f), 0.9f)) * Math.abs(borderTop / borderCenter));
                float min2 = (1.0f - Math.min(new AccelerateInterpolator(0.25f).getInterpolation(0.39f), 0.9f)) * borderBottom;
                if (mSearchViewLayout.isOpening()) {
                    mSearchViewLayout.setAlpha(min);
                    mSearchViewLayout.setTranslationY(-min2);
                    mLauncher.showFolderBlurBackground(min);
                    return true;
                }
            }
            else if(Math.abs(distanceX) <= Math.abs(distanceY) && Math.abs(distanceX) < mTouchSlop) {
                float absDistanceY = Math.abs(distanceY);
                if (absDistanceY > mTouchSlop && distanceY < 0.0f) {

                    isContinueScroll = true;

                    if (mLauncher.isShaking() || mLauncher.isWidgetsViewVisible()){
                        return false;
                    }

                    if (mLauncher.getDragController().mDragging){
                        mLauncher.getDragController().cancelDrag();
                        return false;
                    }

                    if ((mSearchViewLayout.mState == SearchViewLayout.SearchViewState.CLOSED) &&
                            !mLauncher.isOpeningFolder() &&
                            !mLauncher.isOpeningFloatingMenu() &&
                            !mLauncher.isOpeningAppsLibrary() &&
                            !mLauncher.isOpeningLeftPage()
                    ) {
                        Log.e("Start Open","Search View");
                        mSearchViewLayout.startOpen();
                        return true;
                    }
                }
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onSingleTapUp(@NonNull MotionEvent e) {
            return super.onSingleTapUp(e);
        }
    }

    public SearchPullDetector(Context context, int height) {
        mLauncher = (Launcher) context;

        this.borderTop = 0.0f;
        this.borderCenter = height / 10;
        this.borderBottom = height / 6.0f;

        this.mHeight = height;

        mGestureDetector = new GestureDetector(context, new PullGestureDetector());
        mSearchViewLayout = mLauncher.getSearchViewLayout();

        ViewConfiguration viewConfiguration = ViewConfiguration.get(mLauncher);
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        mMinimumFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        mSpringAnimation = new SpringAnimation(mSearchViewLayout, DynamicAnimation.TRANSLATION_Y);
        SpringForce springForce = new SpringForce();
        springForce.setDampingRatio(1f);
        springForce.setStiffness(200f);
        mSpringAnimation.setSpring(springForce);

        mSpringAnimation.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() {
            @Override
            public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
                Log.e("Anim Update","True");
            }
        });

        mSpringAnimation.addEndListener(
                new DynamicAnimation.OnAnimationEndListener() {
                    @Override
                    public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
                        if (mSearchViewLayout.isOpening()) {
                            mSearchViewLayout.setState(true);
                        }
                        if (mSearchViewLayout.mSearchKeyEDT != null) {
                            mSearchViewLayout.showKeyboard(mSearchViewLayout.mSearchKeyEDT);
                        }
                    }
                }
        );
    }

    public boolean dispatchTouchEvent(MotionEvent motionEvent){
        boolean flag =  mGestureDetector.onTouchEvent(motionEvent);
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

            // TODO: 2023.12.24 Check Why always isOpening returns false

            if (this.i && this.mSearchViewLayout.isOpening()) {
                if (Math.abs(this.borderTop) > this.borderCenter) {
                    open(this.borderTop, this.mMinimumFlingVelocity);
                } else {
                    close(this.borderTop);
                }
            }
            this.i = true;
            this.isContinueScroll = false;
            this.borderTop = 0.0f;
            this.borderBottom = this.mHeight / 6.0f;
        }
        return flag;
    }

    public void close(float f){
        int max = Math.max(168, (int) ((f / this.mHeight) * 3689.0f));
        AnimatorSet animatorSet = LauncherAnimUtils.createAnimatorSet();
        animatorSet.playTogether(ObjectAnimator.ofPropertyValuesHolder(mSearchViewLayout,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, (-this.mHeight) / 6.0f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f)),
                ObjectAnimator.ofFloat(this.mLauncher.getBlurBackground(), "alpha", 0.0f));
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.setDuration(max);
        animatorSet.addListener(new PullEndAnimListenerAdapter());
        animatorSet.start();
        this.i = false;
    }

    public void open(float f, float f2){
        int max = Math.max(289, (int) ((f / this.mHeight) * 3689.0f));
        int statusBarHeightPX = mLauncher.getDeviceProfile().statusBarHeightPx + 50;
        AnimatorSet animatorSet = LauncherAnimUtils.createAnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofPropertyValuesHolder(mSearchViewLayout, PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, statusBarHeightPX),PropertyValuesHolder.ofFloat(View.ALPHA, 1.0f)),
                ObjectAnimator.ofFloat(this.mLauncher.getBlurBackground(), "alpha", 1.0f)
        );
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.setDuration(max);
        animatorSet.start();

        if (mSearchViewLayout.isOpening()) {
            mSearchViewLayout.setState(true);
        }

        mSpringAnimation.setStartVelocity(f2);
        mSpringAnimation.start();
        this.i = false;
    }
}

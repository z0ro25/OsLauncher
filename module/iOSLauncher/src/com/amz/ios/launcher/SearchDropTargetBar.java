/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amz.ios.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;

import com.amz.ios.launcher.util.Thunk;
import com.amz.ios.launcher.views.DropTargetBgBar;

/*
 * Ths bar will manage the transition between the QSB search bar and the delete drop
 * targets so that each of the individual IconDropTargets don't have to.
 */
public class SearchDropTargetBar extends FrameLayout implements DragController.DragListener {
    /**
     * The different states that the search bar space can be in.
     */
    public enum State {
        INVISIBLE(0f, 0f),
        SEARCH_BAR(1f, 0f),
        DROP_TARGET(0f, 1f);

        private final float mSearchBarAlpha;
        private final float mDropTargetBarAlpha;

        State(float sbAlpha, float dtbAlpha) {
            mSearchBarAlpha = sbAlpha;
            mDropTargetBarAlpha = dtbAlpha;
        }

        float getSearchBarAlpha() {
            return mSearchBarAlpha;
        }

        float getDropTargetBarAlpha() {
            return mDropTargetBarAlpha;
        }
    }

    public static int DEFAULT_DRAG_FADE_DURATION = 175;
    private static int DEFAULT_ANIMATOR_DELAY = 200;

    private LauncherViewPropertyAnimator mDropTargetBarAnimator;
    private LauncherViewPropertyAnimator mQSBSearchBarAnimator;
    private static final AccelerateInterpolator sAccelerateInterpolator =
            new AccelerateInterpolator();

    private State mState = State.SEARCH_BAR;
    @Thunk
    View mQSB;
    @Thunk
    View mDropTargetBar;
    DropTargetBgBar mDropTargetBgBar;
    private boolean mDeferOnDragEnd = false;
    @Thunk
    boolean mAccessibilityEnabled = false;

    // Drop targets
    private ButtonDropTarget mInfoDropTarget;
    private ButtonDropTarget mDeleteDropTarget;
    private ButtonDropTarget mUninstallDropTarget;
    private ButtonDropTarget mDesktopDropTarget;
    private ButtonDropTarget mUnavaliableDropTarget;

    private FrameLayout mSearchbarContainer;

    private int mTranslateY;

    private Launcher mLauncher;

    public SearchDropTargetBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchDropTargetBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        Resources res = context.getResources();
        mTranslateY = res.getDimensionPixelSize(R.dimen.drop_target_bar_height);
    }

    public void setup(Launcher launcher, DragController dragController) {
        mLauncher = launcher;
        dragController.addDragListener(this);
        dragController.setFlingToDeleteDropTarget(mDeleteDropTarget);

        dragController.addDragListener(mInfoDropTarget);
        dragController.addDragListener(mDeleteDropTarget);
        dragController.addDragListener(mUninstallDropTarget);
        dragController.addDragListener(mDesktopDropTarget);
        dragController.addDragListener(mUnavaliableDropTarget);

        dragController.addDropTarget(mInfoDropTarget);
        dragController.addDropTarget(mDeleteDropTarget);
        dragController.addDropTarget(mUninstallDropTarget);
        dragController.addDropTarget(mDesktopDropTarget);

        mInfoDropTarget.setLauncher(launcher);
        mDeleteDropTarget.setLauncher(launcher);
        mUninstallDropTarget.setLauncher(launcher);
        mDesktopDropTarget.setLauncher(launcher);
        mUnavaliableDropTarget.setLauncher(launcher);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mSearchbarContainer = (FrameLayout) findViewById(R.id.search_bar_container);

        mSearchbarContainer.setVisibility(View.GONE);
        // Get the individual components
        mDropTargetBar = findViewById(R.id.drag_target_bar);
        mInfoDropTarget = (ButtonDropTarget) mDropTargetBar.findViewById(R.id.info_drop_target);
        mDeleteDropTarget = (ButtonDropTarget) mDropTargetBar.findViewById(R.id.delete_drop_target);
        mUninstallDropTarget = (ButtonDropTarget) mDropTargetBar.findViewById(R.id.uninstall_drop_target);
        mDesktopDropTarget = (ButtonDropTarget) mDropTargetBar.findViewById(R.id.desktop_drop_target);
        mUnavaliableDropTarget = (ButtonDropTarget) mDropTargetBar.findViewById(R.id.unavaliable_drop_target);


        mDropTargetBgBar = (DropTargetBgBar) mDropTargetBar.findViewById(R.id.drop_target_bg_bar);

        mInfoDropTarget.setSearchDropTargetBar(this);
        mDeleteDropTarget.setSearchDropTargetBar(this);
        mUninstallDropTarget.setSearchDropTargetBar(this);
        mDesktopDropTarget.setSearchDropTargetBar(this);
        mUnavaliableDropTarget.setSearchDropTargetBar(this);

        mInfoDropTarget.setDropTargetBgBar(mDropTargetBgBar);
        mDeleteDropTarget.setDropTargetBgBar(mDropTargetBgBar);
        mUninstallDropTarget.setDropTargetBgBar(mDropTargetBgBar);
        mDesktopDropTarget.setDropTargetBgBar(mDropTargetBgBar);
        mUnavaliableDropTarget.setDropTargetBgBar(mDropTargetBgBar);

        // Create the various fade animations
        mDropTargetBar.setAlpha(0f);
        mDropTargetBarAnimator = new LauncherViewPropertyAnimator(mDropTargetBar);
        mDropTargetBarAnimator.setInterpolator(sAccelerateInterpolator);
        mDropTargetBarAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                // Ensure that the view is visible for the animation
                mDropTargetBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mDropTargetBar != null) {
                    AlphaUpdateListener.updateVisibility(mDropTargetBar, mAccessibilityEnabled);
                }

                if (mState != State.DROP_TARGET && mLauncher.getWorkspace() != null && !mLauncher.getWorkspace().isInOverviewMode()) {
                }
            }
        });
    }


    public void removeQsbSearchBar() {
        mSearchbarContainer.removeAllViews();
        setQsbSearchBar(null);
    }

    public void addQsbSearchBar(View qsb) {
        mSearchbarContainer.addView(qsb);
        setQsbSearchBar(qsb);
    }

    public void setQsbSearchBar(View qsb) {
        mQSB = qsb;
        if (mQSB != null) {
            // Update the search ber animation
            mQSBSearchBarAnimator = new LauncherViewPropertyAnimator(mQSB);
            mQSBSearchBarAnimator.setInterpolator(sAccelerateInterpolator);
            mQSBSearchBarAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    // Ensure that the view is visible for the animation
                    if (mQSB != null) {
                        mQSB.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mQSB != null) {
                        AlphaUpdateListener.updateVisibility(mQSB, mAccessibilityEnabled);
                    }
                }
            });
        } else {
            mQSBSearchBarAnimator = null;
        }
    }

    /**
     * Animates the current search bar state to a new state.  If the {@param duration} is 0, then
     * the state is applied immediately.
     */
    public void animateToState(State newState, int duration) {
        if (mState != newState) {
            mState = newState;

            int animatorDelay = 0;
            if (newState == State.DROP_TARGET) {
                animatorDelay = DEFAULT_ANIMATOR_DELAY;
            }

            // Update the accessibility state
            AccessibilityManager am = (AccessibilityManager)getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
            mAccessibilityEnabled = am.isEnabled();
            animateViewAlpha(mQSBSearchBarAnimator, mQSB, newState.getSearchBarAlpha(), duration, animatorDelay);
            animateViewAlphaWithTranslateY(mDropTargetBarAnimator, mDropTargetBar, newState.getDropTargetBarAlpha(), duration, animatorDelay);
        }
    }


    /**
     * Convenience method to animate the alpha of a view using hardware layers.
     */
    private void animateViewAlphaWithTranslateY(LauncherViewPropertyAnimator animator, View v, float fraction, int duration, int delay) {
        if (v == null) {
            return;
        }

        float alpha = fraction;
        float translateY = (fraction - 1) * mTranslateY;
        animator.cancel();

        animator.alpha(alpha).translationY(translateY).withLayer().setDuration(duration).setStartDelay(delay);
        animator.start();
    }

    private void animateViewAlpha(LauncherViewPropertyAnimator animator, View v, float fraction, int duration, int delay) {
        if (v == null) {
            return;
        }

        float alpha = fraction;
        animator.cancel();
        animator.alpha(alpha).withLayer().setDuration(duration).setStartDelay(delay);
        animator.start();
    }

    /*
     * DragController.DragListener implementation
     */
    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        animateToState(State.DROP_TARGET, DEFAULT_DRAG_FADE_DURATION);
    }

    /**
     * This is called to defer hiding the delete drop target until the drop animation has completed,
     * instead of hiding immediately when the drag has ended.
     */
    public void deferOnDragEnd() {
        mDeferOnDragEnd = true;
    }

    @Override
    public void onDragEnd() {
        if (!mDeferOnDragEnd) {
            if (mLauncher.isFolderOpen()) {
                animateToState(State.INVISIBLE, DEFAULT_DRAG_FADE_DURATION);
            } else {
                if (mLauncher.getWorkspace().getState() != Workspace.State.OVERVIEW) {
                    animateToState(State.SEARCH_BAR, DEFAULT_DRAG_FADE_DURATION);
                } else {
                    animateToState(State.INVISIBLE, DEFAULT_DRAG_FADE_DURATION);
                }
            }
        } else {
            mDeferOnDragEnd = false;
        }
    }

    /**
     * @return the bounds of the QSB search bar.
     */
    public Rect getSearchBarBounds() {
        if (mQSB != null) {
            final int[] pos = new int[2];
            mQSB.getLocationOnScreen(pos);

            final Rect rect = new Rect();
            rect.left = pos[0];
            rect.top = pos[1];
            rect.right = pos[0] + mQSB.getWidth();
            rect.bottom = pos[1] + mQSB.getHeight();
            return rect;
        } else {
            return null;
        }
    }

    public void enableAccessibleDrag(boolean enable) {
        if (mQSB != null) {
            mQSB.setVisibility(enable ? View.GONE : View.VISIBLE);
        }
        mInfoDropTarget.enableAccessibleDrag(enable);
        mDeleteDropTarget.enableAccessibleDrag(enable);
        mUninstallDropTarget.enableAccessibleDrag(enable);
    }

    public void setDeletDropTargetAnim(int anim) {
        mDeleteDropTarget.setOnDropAnimStyle(anim);
    }

    public void getViewRectRelativeToSelf(View v, Rect r) {
        int[] loc = new int[2];
        getLocationInWindow(loc);
        int x = loc[0];
        int y = loc[1];

        v.getLocationInWindow(loc);
        int vX = loc[0];
        int vY = loc[1];

        int left = vX - x;
        int top = vY - y;
        r.set(left, top, left + v.getMeasuredWidth(), top + v.getMeasuredHeight());
    }
}

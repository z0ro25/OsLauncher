/*
 * Copyright (C) 2015 The Android Open Source Project
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
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import androidx.appcompat.widget.AppCompatImageView;
import com.amz.ios.launcher.util.Thunk;
import java.util.HashMap;

/**
 * TODO: figure out what kind of tests we can write for this
 * <p/>
 * Things to test when changing the following class.
 * - Home from workspace
 * - from center screen
 * - from other screens
 * - Home from all apps
 * - from center screen
 * - from other screens
 * - Back from all apps
 * - from center screen
 * - from other screens
 * - Launch app from workspace and quit
 * - with back
 * - with home
 * - Launch app from all apps and quit
 * - with back
 * - with home
 * - Go to a screen that's not the default, then all
 * apps, and launch and app, and go back
 * - with back
 * -with home
 * - On workspace, long press power and go back
 * - with back
 * - with home
 * - On all apps, long press power and go back
 * - with back
 * - with home
 * - On workspace, power off
 * - On all apps, power off
 * - Launch an app and turn off the screen while in that app
 * - Go back with home key
 * - Go back with back key  TODO: make this not go to workspace
 * - From all apps
 * - From workspace
 * - Enter and exit car mode (becuase it causes an extra configuration changed)
 * - From all apps
 * - From the center workspace
 * - From another workspace
 */
public class LauncherStateTransitionAnimation {
    public static final String TAG = "LauncherStateTransitionAnimation";

    /**
     * Private callbacks made during transition setup.
     */
    static abstract class PrivateTransitionCallbacks {
        void onTransitionComplete() {
        }
    }

    // Flags to determine how to set the layers on views before the transition animation
    public static final int BUILD_LAYER = 0;
    public static final int BUILD_AND_SET_LAYER = 1;
    public static final int SINGLE_FRAME_DELAY = 16;
    public int[] mViewLocations = new int[2];

    public AppCompatImageView mCloseAnimIV;

    @Thunk
    Launcher mLauncher;
    @Thunk
    AnimatorSet mCurrentAnimation;
    @Thunk
    ObjectAnimator mCloseAnimator;

    public LauncherStateTransitionAnimation(Launcher l) {
        mLauncher = l;
    }

    /**
     * Starts an animation to the apps view.
     *
     * @param startSearchAfterTransition Immediately starts app search after the transition to
     *                                   All Apps is completed.
     */
    /*
    public void startAnimationToAllApps(final Workspace.State fromWorkspaceState,
                                        final boolean animated, final boolean startSearchAfterTransition) {
        final AllAppsContainerView toView = mLauncher.getAppsView();
        PrivateTransitionCallbacks cb = new PrivateTransitionCallbacks() {

            @Override
            void onTransitionComplete() {
                if (startSearchAfterTransition) {
                    toView.startAppsSearch();
                }
            }
        };
        // Only animate the search bar if animating from spring loaded mode back to all apps
        mCurrentAnimation = startAnimationToOverlay(fromWorkspaceState,
                Workspace.State.NORMAL_HIDDEN, toView, toView.getSearchBarView(), animated, cb);
    }

     */

    /**
     * Starts and animation to the workspace from the current overlay view.
     */
    public void startAnimationToWorkspace(final Launcher.State fromState,
                                          final Workspace.State fromWorkspaceState, final Workspace.State toWorkspaceState,
                                          final int toWorkspacePage, final boolean animated, final Runnable onCompleteRunnable) {
        if (toWorkspaceState != Workspace.State.NORMAL &&
                toWorkspaceState != Workspace.State.SPRING_LOADED &&
                toWorkspaceState != Workspace.State.OVERVIEW) {
            Log.e(TAG, "Unexpected call to startAnimationToWorkspace");
        }

        /*
        if (fromState == Launcher.State.APPS || fromState == Launcher.State.APPS_SPRING_LOADED) {
            startAnimationToWorkspaceFromAllApps(fromWorkspaceState, toWorkspaceState, toWorkspacePage, animated, onCompleteRunnable);
            mLauncher.hideBlurBg();
        }
        else


         */
            {
            startAnimationInWorkspace(fromWorkspaceState, toWorkspaceState, toWorkspacePage,
                    animated, onCompleteRunnable);
        }

        this.mLauncher.closeWidgetView(true);
    }

    /**
     * Creates and starts a new animation to a particular overlay view.
     */
    /*
    @SuppressLint("NewApi")
    private AnimatorSet startAnimationToOverlay(final Workspace.State fromWorkspaceState,
                                                final Workspace.State toWorkspaceState, final View toView,
                                                final View overlaySearchBarView,
                                                final boolean animated, final PrivateTransitionCallbacks pCb) {
        final AnimatorSet animation = LauncherAnimUtils.createAnimatorSet();
        final Resources res = mLauncher.getResources();
        final boolean material = Utilities.ATLEAST_LOLLIPOP;
        final int revealDuration = res.getInteger(R.integer.config_overlayRevealTime);
        final int itemsAlphaStagger =
                res.getInteger(R.integer.config_overlayItemsAlphaStagger);

        final View fromView = mLauncher.getWorkspace();

        final HashMap<View, Integer> layerViews = new HashMap<>();

        // Cancel the current animation
        cancelAnimation();

        // Create the workspace animation.
        // NOTE: this call apparently also sets the state for the workspace if !animated
        Animator workspaceAnim = mLauncher.startWorkspaceStateChangeAnimation(toWorkspaceState, -1,
                animated, layerViews);

        // Animate the search bar
        startWorkspaceSearchBarAnimation(animation, fromWorkspaceState, toWorkspaceState,
                animated ? revealDuration : 0, overlaySearchBarView);

        if (animated) {
            toView.setAlpha(0f);
            toView.setVisibility(View.VISIBLE);

            // Setup the reveal view animation
            int height = toView.getMeasuredHeight();

            final float fromAlpha = 0;
            final float fromScaleX = 0;
            final float fromScaleY = 0;

            // Create the animators
            PropertyValuesHolder panelAlpha =
                    PropertyValuesHolder.ofFloat("alpha", fromAlpha, 1f);
            PropertyValuesHolder scaleY =
                    PropertyValuesHolder.ofFloat("scaleY", fromScaleY, 1f);
            PropertyValuesHolder scaleX =
                    PropertyValuesHolder.ofFloat("scaleX", fromScaleX, 1f);

            ObjectAnimator panelAlphaAndDrift = ObjectAnimator.ofPropertyValuesHolder(toView, panelAlpha, scaleY, scaleX);
            panelAlphaAndDrift.setDuration(revealDuration);
            panelAlphaAndDrift.setInterpolator(new LogDecelerateInterpolator(100, 0));

            // Play the animation
            layerViews.put(toView, BUILD_AND_SET_LAYER);
            animation.play(panelAlphaAndDrift);


            animation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    dispatchOnLauncherTransitionEnd(fromView, animated, false);
                    dispatchOnLauncherTransitionEnd(toView, animated, false);


                    // Disable all necessary layers
                    for (View v : layerViews.keySet()) {
                        if (layerViews.get(v) == BUILD_AND_SET_LAYER) {
                            v.setLayerType(View.LAYER_TYPE_NONE, null);
                        }
                    }

                    // This can hold unnecessary references to views.
                    cleanupAnimation();
                    pCb.onTransitionComplete();
                }

            });

            // Play the workspace animation
            if (workspaceAnim != null) {
                animation.play(workspaceAnim);
            }

            // Dispatch the prepare transition signal
            dispatchOnLauncherTransitionPrepare(fromView, animated, false);
            dispatchOnLauncherTransitionPrepare(toView, animated, false);


            final AnimatorSet stateAnimation = animation;
            final Runnable startAnimRunnable = new Runnable() {
                public void run() {
                    // Check that mCurrentAnimation hasn't changed while
                    // we waited for a layout/draw pass
                    if (mCurrentAnimation != stateAnimation)
                        return;
                    dispatchOnLauncherTransitionStart(fromView, animated, false);
                    dispatchOnLauncherTransitionStart(toView, animated, false);

                    // Enable all necessary layers
                    for (View v : layerViews.keySet()) {
                        if (layerViews.get(v) == BUILD_AND_SET_LAYER) {
                            v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                        }
                        if (Utilities.ATLEAST_LOLLIPOP && Utilities.isViewAttachedToWindow(v)) {
                            v.buildLayer();
                        }
                    }

                    // Focus the new view
                    toView.requestFocus();

                    stateAnimation.start();
                }
            };
            toView.bringToFront();
            toView.setVisibility(View.VISIBLE);
            toView.post(startAnimRunnable);

            return animation;
        } else {
            toView.setTranslationX(0.0f);
            toView.setTranslationY(0.0f);
            toView.setScaleX(1.0f);
            toView.setScaleY(1.0f);
            toView.setAlpha(1.0f);
            toView.setVisibility(View.VISIBLE);
            toView.bringToFront();

            dispatchOnLauncherTransitionPrepare(fromView, animated, false);
            dispatchOnLauncherTransitionStart(fromView, animated, false);
            dispatchOnLauncherTransitionEnd(fromView, animated, false);
            dispatchOnLauncherTransitionPrepare(toView, animated, false);
            dispatchOnLauncherTransitionStart(toView, animated, false);
            dispatchOnLauncherTransitionEnd(toView, animated, false);
            pCb.onTransitionComplete();

            return null;
        }
    }

     */

    /**
     * Starts and animation to the workspace from the apps view.
     */
    /*
    private void startAnimationToWorkspaceFromAllApps(final Workspace.State fromWorkspaceState,
                                                      final Workspace.State toWorkspaceState, final int toWorkspacePage,
                                                      final boolean animated, final Runnable onCompleteRunnable) {
        AllAppsContainerView appsView = mLauncher.getAppsView();
        PrivateTransitionCallbacks cb = new PrivateTransitionCallbacks() {

        };
        // Only animate the search bar if animating to spring loaded mode from all apps
        mCurrentAnimation = startAnimationToWorkspaceFromOverlay(fromWorkspaceState, toWorkspaceState,
                toWorkspacePage, mLauncher.getAllAppsButton(), appsView, appsView.getSearchBarView(), animated,
                onCompleteRunnable, cb);
    }


     */
    /**
     * Starts and animation to the workspace from the widgets view.
     */
    private void startAnimationInWorkspace(final Workspace.State fromWorkspaceState,
                                           final Workspace.State toWorkspaceState, final int toWorkspacePage,
                                           final boolean animated, final Runnable onCompleteRunnable) {
        PrivateTransitionCallbacks cb = new PrivateTransitionCallbacks() {

        };
        mCurrentAnimation = startAnimationToWorkspaceFromOverlay(fromWorkspaceState,
                toWorkspaceState, toWorkspacePage, null, null, null, animated,
                onCompleteRunnable, cb);
    }

    /**
     * Creates and starts a new animation to the workspace.
     */
    private AnimatorSet startAnimationToWorkspaceFromOverlay(final Workspace.State fromWorkspaceState,
                                                             final Workspace.State toWorkspaceState, final int toWorkspacePage, final View buttonView,
                                                             final View fromView,
                                                             final View overlaySearchBarView, final boolean animated, final Runnable onCompleteRunnable,
                                                             final PrivateTransitionCallbacks pCb) {
        final AnimatorSet animation = LauncherAnimUtils.createAnimatorSet();
        final Resources res = mLauncher.getResources();
        final boolean material = Utilities.ATLEAST_LOLLIPOP;
        final int revealDuration = res.getInteger(R.integer.config_overlayRevealTime);
        final int itemsAlphaStagger = res.getInteger(R.integer.config_overlayItemsAlphaStagger);

        final View toView = mLauncher.getWorkspace();

        final HashMap<View, Integer> layerViews = new HashMap<>();

        // If for some reason our views aren't initialized, don't animate
        boolean animateButtonView = buttonView != null;

        // Cancel the current animation
        cancelAnimation();

        // Create the workspace animation.
        // NOTE: this call apparently also sets the state for the workspace if !animated
        Animator workspaceAnim = mLauncher.startWorkspaceStateChangeAnimation(toWorkspaceState,
                toWorkspacePage, animated, layerViews);

        // Animate the search bar

        /*
        startWorkspaceSearchBarAnimation(animation, fromWorkspaceState, toWorkspaceState,
                animated ? revealDuration : 0, overlaySearchBarView);
         */

        if (animated) {
            // Play the workspace animation
            if (workspaceAnim != null) {
                animation.play(workspaceAnim);
            }

            if (fromView != null && fromView.getVisibility() == View.VISIBLE) {
                final float fromAlpha = 1f;
                final float fromScaleX = 1f;
                final float fromScaleY = 1f;

                // Create the animators
                PropertyValuesHolder panelAlpha =
                        PropertyValuesHolder.ofFloat("alpha", fromAlpha, 0);
                PropertyValuesHolder scaleY =
                        PropertyValuesHolder.ofFloat("scaleY", fromScaleY, 0.7f);
                PropertyValuesHolder scaleX =
                        PropertyValuesHolder.ofFloat("scaleX", fromScaleX, 0.7f);
                ObjectAnimator panelAlphaAndDrift = ObjectAnimator.ofPropertyValuesHolder(fromView, panelAlpha, scaleX, scaleY);
                panelAlphaAndDrift.setDuration(revealDuration);
                panelAlphaAndDrift.setInterpolator(new LogDecelerateInterpolator(100, 0));

                // Play the animation
                layerViews.put(fromView, BUILD_AND_SET_LAYER);
                animation.play(panelAlphaAndDrift);

                dispatchOnLauncherTransitionPrepare(fromView, animated, true);
            }

            dispatchOnLauncherTransitionPrepare(toView, animated, true);

            animation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (fromView != null) {
                        fromView.setVisibility(View.GONE);
                        dispatchOnLauncherTransitionEnd(fromView, animated, true);
                    }
                    dispatchOnLauncherTransitionEnd(toView, animated, true);

                    // Run any queued runnables
                    if (onCompleteRunnable != null) {
                        onCompleteRunnable.run();
                    }

                    // Disable all necessary layers
                    for (View v : layerViews.keySet()) {
                        if (layerViews.get(v) == BUILD_AND_SET_LAYER) {
                            v.setLayerType(View.LAYER_TYPE_NONE, null);
                        }
                    }
                    // This can hold unnecessary references to views.
                    cleanupAnimation();
                    pCb.onTransitionComplete();
                }
            });

            final AnimatorSet stateAnimation = animation;
            final Runnable startAnimRunnable = new Runnable() {
                public void run() {
                    // Check that mCurrentAnimation hasn't changed while
                    // we waited for a layout/draw pass
                    if (mCurrentAnimation != stateAnimation)
                        return;

                    if (fromView != null) {
                        dispatchOnLauncherTransitionStart(fromView, animated, false);
                    }
                    dispatchOnLauncherTransitionStart(toView, animated, false);

                    // Enable all necessary layers
                    for (View v : layerViews.keySet()) {
                        if (layerViews.get(v) == BUILD_AND_SET_LAYER) {
                            v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                        }
                        if (Utilities.ATLEAST_LOLLIPOP && Utilities.isViewAttachedToWindow(v)) {
                            v.buildLayer();
                        }
                    }
                    stateAnimation.start();
                }
            };

            toView.post(startAnimRunnable);
            return animation;
        } else {
            if (fromView != null) {
                fromView.setVisibility(View.GONE);
                dispatchOnLauncherTransitionPrepare(fromView, animated, true);
                dispatchOnLauncherTransitionStart(fromView, animated, true);
                dispatchOnLauncherTransitionEnd(fromView, animated, true);
            }
            dispatchOnLauncherTransitionPrepare(toView, animated, true);
            dispatchOnLauncherTransitionStart(toView, animated, true);
            dispatchOnLauncherTransitionEnd(toView, animated, true);
            pCb.onTransitionComplete();

            // Run any queued runnables
            if (onCompleteRunnable != null) {
                onCompleteRunnable.run();
            }

            return null;
        }
    }

    /**
     * Dispatches the prepare-transition event to suitable views.
     */
    void dispatchOnLauncherTransitionPrepare(View v, boolean animated, boolean toWorkspace) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionPrepare(mLauncher, animated,
                    toWorkspace);
        }
    }

    /**
     * Dispatches the start-transition event to suitable views.
     */
    void dispatchOnLauncherTransitionStart(View v, boolean animated, boolean toWorkspace) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionStart(mLauncher, animated,
                    toWorkspace);
        }

        // Update the workspace transition step as well
        dispatchOnLauncherTransitionStep(v, 0f);
    }

    /**
     * Dispatches the step-transition event to suitable views.
     */
    void dispatchOnLauncherTransitionStep(View v, float t) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionStep(mLauncher, t);
        }
    }

    /**
     * Dispatches the end-transition event to suitable views.
     */
    void dispatchOnLauncherTransitionEnd(View v, boolean animated, boolean toWorkspace) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionEnd(mLauncher, animated,
                    toWorkspace);
        }

        // Update the workspace transition step as well
        dispatchOnLauncherTransitionStep(v, 1f);
    }

    /**
     * Cancels the current animation.
     */
    private void cancelAnimation() {
        if (mCurrentAnimation != null) {
            mCurrentAnimation.setDuration(0);
            mCurrentAnimation.cancel();
            mCurrentAnimation = null;
        }
    }

    @Thunk
    void cleanupAnimation() {
        mCurrentAnimation = null;
    }

    public void updateCloseAnimView(View view, Drawable drawable){
        if (view == null || drawable == null) {
            mCloseAnimIV = null;
            return;
        }
        if (mCloseAnimIV == null){
            mCloseAnimIV = mLauncher.findViewById(R.id.expanded_image_anim);
            mCloseAnimIV.setClickable(false);
            mCloseAnimIV.setVisibility(View.INVISIBLE);
            mCloseAnimIV.setFocusable(false);
        }
        mCloseAnimIV.setVisibility(View.INVISIBLE);
        mCloseAnimIV.setBackground(drawable);
        mCloseAnimIV.setScaleX(5.0f);
        mCloseAnimIV.setScaleY(5.0f);
        mLauncher.getDragLayer().getLocationInDragLayer(view, mViewLocations);
        DragLayer.LayoutParams layoutParams = new DragLayer.LayoutParams(-2,-2);
        ((FrameLayout.LayoutParams) layoutParams).leftMargin = mViewLocations[0];
        ((FrameLayout.LayoutParams) layoutParams).topMargin = mViewLocations[1];
        try {
            mLauncher.getDragLayer().updateViewLayout(mCloseAnimIV, layoutParams);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

}

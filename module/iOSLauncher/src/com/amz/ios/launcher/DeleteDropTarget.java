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

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.amz.ios.ioslite.common.anim.PropertyHolderUtis;
import com.amz.ios.launcher.config.Settings;
import com.amz.ios.launcher.folder.FolderLayout;
import com.amz.ios.launcher.util.FlingAnimation;
import com.amz.ios.launcher.util.Thunk;

public class DeleteDropTarget extends ButtonDropTarget {
    private AnimatorSet mEnterAnimator;
    private View mDelLine1;
    private View mDelLine2;

    public DeleteDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeleteDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnDropAnimStyle(Settings.getDropTargetAnimStyle(context));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTargetView = findViewById(R.id.delete_target_view);
        mDelLine1 = findViewById(R.id.delete_target_line1);
        mDelLine2 = findViewById(R.id.delete_target_line2);

        mDelLine1.setRotation(45);
        mDelLine2.setRotation(-45);

        mEnterAnimator = new AnimatorSet();
        ObjectAnimator animatorLine1 = LauncherAnimUtils.ofPropertyValuesHolder(mDelLine1, PropertyHolderUtis.rotation(45, 0, 45));
        animatorLine1.setDuration(150);

        ObjectAnimator animatorLine2 = LauncherAnimUtils.ofPropertyValuesHolder(mDelLine2, PropertyHolderUtis.rotation(-45, 0, -45));
        animatorLine2.setDuration(150);
        mEnterAnimator.playTogether(animatorLine1, animatorLine2);
    }

    @Override
    protected View getTargetView() {
        return mTargetView;
    }

    @Override
    public void onDragEnter(DragObject d) {
        super.onDragEnter(d);
        mDelLine1.setPivotX(mDelLine1.getMeasuredWidth() / 2);
        mDelLine1.setPivotY(mDelLine1.getMeasuredHeight() / 2);
        mDelLine2.setPivotX(mDelLine2.getMeasuredWidth() / 2);
        mDelLine2.setPivotY(mDelLine2.getMeasuredHeight() / 2);
        mEnterAnimator.start();
    }

    public static boolean supportsDrop(Object info) {
        return (info instanceof ShortcutInfo && (((ShortcutInfo) info).isShortcut()
                || ((ShortcutInfo) info).isDeepShortcut()))
                || (info instanceof LauncherAppWidgetInfo);
    }

    @Override
    protected boolean supportsDrop(DragSource source, Object info) {
        return source.supportsDeleteDropTarget() && supportsDrop(info);
    }


    @Override
    @Thunk
    void completeDrop(DragObject d) {
        ItemInfo item = (ItemInfo) d.dragInfo;
        if ((d.dragSource instanceof Workspace) || (d.dragSource instanceof FolderLayout)) {
            removeWorkspaceOrFolderItem(mLauncher, item, null);
        }
    }

    /**
     * Removes the item from the workspace. If the view is not null, it also removes the view.
     *
     * @return true if the item was removed.
     */
    public static boolean removeWorkspaceOrFolderItem(Launcher launcher, ItemInfo item, View view) {
        if (item instanceof ShortcutInfo) {
            LauncherModel.deleteItemFromDatabase(launcher, item);
        } else if (item instanceof FolderInfo) {
            FolderInfo folder = (FolderInfo) item;
            LauncherModel.deleteFolderContentsFromDatabase(launcher, folder);
        } else if (item instanceof LauncherAppWidgetInfo) {
            final LauncherAppWidgetInfo widget = (LauncherAppWidgetInfo) item;

            // Remove the widget from the workspace
            launcher.removeAppWidget(widget);
            LauncherModel.deleteItemFromDatabase(launcher, widget);

            final LauncherAppWidgetHost appWidgetHost = launcher.getAppWidgetHost();

            if (appWidgetHost != null && !widget.isIOSWidget()
                    && widget.isWidgetIdValid()) {
                // Deleting an app widget ID is a void call but writes to disk before returning
                // to the caller...
                new AsyncTask<Void, Void, Void>() {
                    public Void doInBackground(Void... args) {
                        appWidgetHost.deleteAppWidgetId(widget.appWidgetId);
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        } else {
            return false;
        }

        if (view != null) {
            launcher.getWorkspace().removeWorkspaceItem(view);
            launcher.getWorkspace().stripEmptyScreens(false);
        }
        return true;
    }

    @Override
    public void onFlingToDelete(final DragObject d, PointF vel) {
        super.onFlingToDelete(d,vel);
        // Don't highlight the icon as it's animating
        d.dragView.setColor(0);
        d.dragView.updateInitialScaleToCurrentScale();

        final DragLayer dragLayer = mLauncher.getDragLayer();
        FlingAnimation fling = new FlingAnimation(d, vel,
                getIconRect(d.dragView.getMeasuredWidth(), d.dragView.getMeasuredHeight()),
                dragLayer);

        final int duration = fling.getDuration();
        final long startTime = AnimationUtils.currentAnimationTimeMillis();

        // NOTE: Because it takes time for the first frame of animation to actually be
        // called and we expect the animation to be a continuation of the fling, we have
        // to account for the time that has elapsed since the fling finished.  And since
        // we don't have a startDelay, we will always get call to update when we call
        // start() (which we want to ignore).
        final TimeInterpolator tInterpolator = new TimeInterpolator() {
            private int mCount = -1;
            private float mOffset = 0f;

            @Override
            public float getInterpolation(float t) {
                if (mCount < 0) {
                    mCount++;
                } else if (mCount == 0) {
                    mOffset = Math.min(0.5f, (float) (AnimationUtils.currentAnimationTimeMillis() -
                            startTime) / duration);
                    mCount++;
                }
                return Math.min(1f, mOffset + t);
            }
        };

        Runnable onAnimationEndRunnable = new Runnable() {
            @Override
            public void run() {
                mLauncher.exitSpringLoadedDragMode();
                completeDrop(d);
                mLauncher.getDragController().onDeferredEndFling(d);
            }
        };

        dragLayer.animateView(d.dragView, fling, duration, tInterpolator, onAnimationEndRunnable,
                DragLayer.ANIMATION_END_DISAPPEAR, null);
    }

    @Override
    protected String getAccessibilityDropConfirmation() {
        return getResources().getString(R.string.item_removed);
    }
}

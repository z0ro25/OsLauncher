/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.content.ComponentName;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import com.amz.ios.ioslite.common.CommonSdk;
import com.amz.ios.launcher.compat.LauncherActivityInfoCompat;
import com.amz.ios.launcher.compat.LauncherAppsCompat;
import com.amz.ios.launcher.compat.UserHandleCompat;
import com.amz.ios.launcher.ota.IOSOtaHandler;
import com.amz.ios.launcher.util.Thunk;
import com.amz.ios.launcher.views.DropTargetBgBar;

import java.util.List;

/**
 * Implements a DropTarget.
 */
public abstract class ButtonDropTarget extends FrameLayout
        implements DropTarget, DragController.DragListener, OnClickListener {

    private static int DRAG_VIEW_DROP_DURATION = 285;

    public static final int ANIM_DROP_SYSTEM = 0;
    public static final int ANIM_DROP_EXPLOSION = 1;

    protected Launcher mLauncher;
    private int mBottomDragPadding;
    protected SearchDropTargetBar mSearchDropTargetBar;
    protected DropTargetBgBar mDropTargetBgBar;

    protected View mTargetView;

    /**
     * Whether this drop target is active for the current drag
     */
    protected boolean mActive;

    protected int mOnDropAnimStyle;
    private AnimatorSet mCurrentColorAnim;

    public ButtonDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ButtonDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mBottomDragPadding = getResources().getDimensionPixelSize(R.dimen.drop_target_drag_padding);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }


    public void setLauncher(Launcher launcher) {
        mLauncher = launcher;
    }

    public void setSearchDropTargetBar(SearchDropTargetBar searchDropTargetBar) {
        mSearchDropTargetBar = searchDropTargetBar;
    }

    public void setDropTargetBgBar(DropTargetBgBar bgBar) {
        mDropTargetBgBar = bgBar;
    }

    @Override
    public void onFlingToDelete(DragObject d, PointF vec) {
        if (enableExplosionAnimation()) {
            DragLayer dragLayer = mLauncher.getDragLayer();
            Rect from = new Rect();
            dragLayer.getViewRectRelativeToSelf(d.dragView, from);
            dragLayer.explodeView(d.dragView, from);
        }
    }

    @Override
    public void onDragEnter(DragObject d) {
        final Rect rect = new Rect();
        mSearchDropTargetBar.getViewRectRelativeToSelf(this, rect);
        mDropTargetBgBar.onTargetEnter(rect);
    }

    @Override
    public void onDragOver(DragObject d) {
        // Do nothing
    }


    @Override
    public void onDragExit(DragObject d) {
        mDropTargetBgBar.cancel();
    }

    @Override
    public final void onDragStart(DragSource source, Object info, int dragAction) {
        mActive = supportsDrop(source, info);
        if (mCurrentColorAnim != null) {
            mCurrentColorAnim.cancel();
            mCurrentColorAnim = null;
        }
        setVisibility(mActive ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean acceptDrop(DragObject dragObject) {
        return supportsDrop(dragObject.dragSource, dragObject.dragInfo);
    }

    protected abstract boolean supportsDrop(DragSource source, Object info);

    @Override
    public boolean isDropEnabled() {
        return mActive;
    }

    @Override
    public void onDragEnd() {
        mActive = false;
    }

    protected boolean enableExplosionAnimation() {
        return mOnDropAnimStyle == ANIM_DROP_EXPLOSION;
    }

    protected void setOnDropAnimStyle(int anim) {
        mOnDropAnimStyle = anim;
    }


    /**
     * On drop animate the dropView to the icon.
     */
    @Override
    public void onDrop(final DragObject d) {
        final DragLayer dragLayer = mLauncher.getDragLayer();
        final Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(d.dragView, from);

        final Rect to = getIconRect(d.dragView.getMeasuredWidth(), d.dragView.getMeasuredHeight());
        final float scale = (float) to.width() / from.width();
        mSearchDropTargetBar.deferOnDragEnd();

        final Runnable onAnimationEndRunnable = new Runnable() {
            @Override
            public void run() {
                completeDrop(d);
                mSearchDropTargetBar.onDragEnd();
                mLauncher.exitSpringLoadedDragModeDelayed(true, 0, null);
            }
        };

        if (enableExplosionAnimation()) {
            dragLayer.explodeView(d.dragView, from);
        }
        dragLayer.animateView(d.dragView, from, to, scale, 1f, 1f, 0.1f, 0.1f,
                DRAG_VIEW_DROP_DURATION, new DecelerateInterpolator(2),
                new LinearInterpolator(), onAnimationEndRunnable,
                DragLayer.ANIMATION_END_DISAPPEAR, null);
    }

    @Override
    public void prepareAccessibilityDrop() {
    }

    @Thunk
    abstract void completeDrop(DragObject d);

    @Override
    public void getHitRectRelativeToDragLayer(android.graphics.Rect outRect) {
        super.getHitRect(outRect);
        outRect.bottom += mBottomDragPadding;

        int[] coords = new int[2];
        mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, coords);
        outRect.offsetTo(coords[0], coords[1]);
    }

    protected Rect getIconRect(int viewWidth, int viewHeight) {
        final View targetView = getTargetView();
        DragLayer dragLayer = mLauncher.getDragLayer();

        // Find the rect to animate to (the view is center aligned)
        Rect to = new Rect();
        dragLayer.getViewRectRelativeToSelf(targetView, to);

        int left = to.left + (targetView.getMeasuredWidth() - viewWidth) / 2;
        int right = left + viewWidth;

        int top = to.top + (targetView.getMeasuredHeight() - viewHeight) / 2;
        int bottom = top + getMeasuredHeight();

        to.set(left, top, right, bottom);
        return to;
    }

    protected View getTargetView() {
        return this;
    }

    @Override
    public void getLocationInDragLayer(int[] loc) {
        mLauncher.getDragLayer().getLocationInDragLayer(this, loc);
    }

    public void enableAccessibleDrag(boolean enable) {
        setOnClickListener(enable ? this : null);
    }

    protected String getAccessibilityDropConfirmation() {
        return null;
    }

    @Override
    public void onClick(View v) {
        LauncherAppState.getInstance().getAccessibilityDelegate()
                .handleAccessibleDrop(this, null, getAccessibilityDropConfirmation());
    }

    /**
     * @return the component name and flags if {@param info} is an AppInfo or an app shortcut.
     */
    protected static Pair<ComponentName, Integer> getAppInfoFlags(Object item) {
        Pair<ComponentName, Integer> replaceHidePair;
        if (item instanceof AppInfo) {
            AppInfo info = (AppInfo) item;

            replaceHidePair = getHideAppInfoFlags(info.componentName);
            if (replaceHidePair != null) {
                return replaceHidePair;
            }

            //not_uninstall_app_list,不显示应用卸载入口白名单的特殊处理
            return Pair.create(info.componentName, info.getFlags());
        } else if (item instanceof ShortcutInfo) {
            ShortcutInfo info = (ShortcutInfo) item;
            ComponentName component = info.getTargetComponent();
            if (info.itemType == LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION
                    && component != null) {

                replaceHidePair = getHideAppInfoFlags(component);
                if (replaceHidePair != null) {
                    return replaceHidePair;
                }
                //not_uninstall_app_list,不显示应用卸载入口白名单的特殊处理
                return Pair.create(component, info.getFlags());
            }
        }
        return null;
    }

    /**
     *  IOS OTA 2.0 获取指向应用信息
     */
    private static Pair<ComponentName, Integer> getHideAppInfoFlags(ComponentName cn) {
        Context context = CommonSdk.getApplicationContext();
        ComponentName hideAppCN = IOSOtaHandler.getHideAppComponentName(context, cn.getClassName());
        if (hideAppCN != null) {
            final UserHandleCompat user = UserHandleCompat.myUserHandle();
            final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
            final List<LauncherActivityInfoCompat> matches =
                    launcherApps.getActivityList(hideAppCN.getPackageName(), user);
            if (matches.size() > 0) {
                LauncherActivityInfoCompat info = matches.get(0);
                return Pair.create(info.getComponentName(), AppInfo.initFlags(context, info));
            }
        }

        return null;
    }

    /**
     * not_uninstall_app_list,不显示应用卸载入口白名单的特殊处理
     */
    private static Pair<ComponentName, Integer> getNotUninstallAppInfoFlags(ComponentName cn) {
        Context context = CommonSdk.getApplicationContext();
        ComponentName hideAppCN = IOSOtaHandler.getHideAppComponentName(context, cn.getClassName());
        if (hideAppCN != null) {
            final UserHandleCompat user = UserHandleCompat.myUserHandle();
            final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
            final List<LauncherActivityInfoCompat> matches =
                    launcherApps.getActivityList(hideAppCN.getPackageName(), user);
            if (matches.size() > 0) {
                LauncherActivityInfoCompat info = matches.get(0);
                return Pair.create(info.getComponentName(), AppInfo.initFlags(context, info));
            }
        }

        return null;
    }
}

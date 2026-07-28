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
import android.content.ComponentName;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.amz.ios.ioslite.common.anim.PropertyHolderUtis;
import com.amz.ios.launcher.compat.UserHandleCompat;
import com.amz.ios.launcher.ota.IOSOtaHandler;

public class InfoDropTarget extends ButtonDropTarget {
    private AnimatorSet mEnterAnimator;
    private View mTargetLine;
    private View mTargetDot;

    public InfoDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InfoDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTargetView = findViewById(R.id.info_target_view);
        mTargetLine = findViewById(R.id.info_target_line);
        mTargetDot = findViewById(R.id.info_target_dot);

        mEnterAnimator = new AnimatorSet();
        ObjectAnimator animatorLine = LauncherAnimUtils.ofPropertyValuesHolder(mTargetLine, PropertyHolderUtis.rotation(0, 360));
        animatorLine.setDuration(300);

        ObjectAnimator animatorDot = LauncherAnimUtils.ofPropertyValuesHolder(mTargetDot, PropertyHolderUtis.rotation(0, 360));
        animatorDot.setDuration(300);
        animatorDot.setStartDelay(100);
        mEnterAnimator.playTogether(animatorLine, animatorDot);
    }

    @Override
    protected View getTargetView() {
        return mTargetView;
    }

    @Override
    public void onDragEnter(DragObject d) {
        super.onDragEnter(d);
        mTargetLine.setPivotX(mTargetLine.getMeasuredWidth()/2);
        mTargetLine.setPivotY(mTargetLine.getMeasuredHeight()/2);
        mTargetDot.setPivotX(mTargetDot.getMeasuredWidth()/2);
        mTargetDot.setPivotY(mTargetDot.getMeasuredHeight()/2);
        mEnterAnimator.start();
    }

    public static void startDetailsActivityForInfo(Object info, Launcher launcher) {
        ComponentName componentName = null;
        if (info instanceof AppInfo) {
            componentName = ((AppInfo) info).componentName;
        } else if (info instanceof ShortcutInfo) {
            componentName = ((ShortcutInfo) info).intent.getComponent();
        } else if (info instanceof PendingAddItemInfo) {
            componentName = ((PendingAddItemInfo) info).componentName;
        }
        final UserHandleCompat user;
        if (info instanceof ItemInfo) {
            user = ((ItemInfo) info).user;
        } else {
            user = UserHandleCompat.myUserHandle();
        }

        if (componentName != null) {
            ComponentName hideAppCN = IOSOtaHandler.getHideAppComponentName(launcher, componentName.getClassName());
            if (hideAppCN != null) {
                componentName = hideAppCN;
            }
            launcher.startApplicationDetailsActivity(componentName, user);
        }
    }

    @Override
    protected boolean supportsDrop(DragSource source, Object info) {
        return source.supportsAppInfoDropTarget() && supportsDrop(getContext(), info);
    }

    public static boolean supportsDrop(Context context, Object info) {
        return info instanceof AppInfo || info instanceof PendingAddItemInfo || (info instanceof ShortcutInfo && !((ShortcutInfo) info).isShortcut());
    }

    @Override
    public void onDrop(DragObject d) {
        if (d.dragSource instanceof InfoTargetSource){
            ((InfoTargetSource) d.dragSource).deferCompleteDropAfterInfoTarget();
        }
        super.onDrop(d);
    }

    @Override
    void completeDrop(DragObject d) {
        startDetailsActivityForInfo(d.dragInfo, mLauncher);
        if (d.dragSource instanceof InfoTargetSource){
            ((InfoTargetSource) d.dragSource).onInfoActivityReturned();
        }
    }


    public interface InfoTargetSource {
        void onInfoActivityReturned();

        void deferCompleteDropAfterInfoTarget();
    }
}

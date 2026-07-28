/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;

import com.amz.ios.launcher.util.Thunk;
import com.amz.ios.launcher.views.CustomTextView;

import java.util.Map;

class LauncherClings implements OnClickListener {
    private static final String TAG_CROP_TOP_AND_SIDES = "crop_bg_top_and_sides";

    private static final int SHOW_CLING_DURATION = 250;
    private static final int DISMISS_CLING_DURATION = 200;

    @Thunk
    Launcher mLauncher;
    private LayoutInflater mInflater;
    @Thunk
    boolean mIsVisible;

    public LauncherClings(Launcher launcher) {
        mLauncher = launcher;
        mInflater = LayoutInflater.from(mLauncher);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.category_dismiss) {
            dismissFolderCategoryCling();
        }
    }


    public void showCategoryFolderCling(Map<ShortcutInfo, FolderInfo> categoryMapMap) {
        mIsVisible = true;
        ViewGroup root = (ViewGroup) mLauncher.findViewById(R.id.launcher);
        View cling = mInflater.inflate(R.layout.folder_category_cling, root, false);

        cling.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                dismissFolderCategoryCling();
                return true;
            }
        });

        final ViewGroup content = (ViewGroup) cling.findViewById(R.id.cling_content);
        mInflater.inflate(R.layout.folder_category_cling_content
                , content);
        content.findViewById(R.id.category_dismiss).setOnClickListener(this);
        ViewGroup container = (ViewGroup) content.findViewById(R.id.category_container);
        final IconCache iconCache = LauncherAppState.getInstance().getIconCache();

        int limit = 3;
        int count = 0;
        for (Map.Entry<ShortcutInfo, FolderInfo> entry : categoryMapMap.entrySet()) {
            View item = mInflater.inflate(R.layout.folder_category_cling_item
                    , container, false);
            ImageView icon = (ImageView) item.findViewById(R.id.icon);
            CustomTextView folderName = (CustomTextView) item.findViewById(R.id.folder);
            icon.setImageBitmap(entry.getKey().getIcon(iconCache));
            folderName.setText(entry.getValue().title);
            container.addView(item);

            // 最多显示三条分类提示
            count += 1;
            if (count == limit) {
                break;
            }
        }

        if (TAG_CROP_TOP_AND_SIDES.equals(content.getTag())) {
            Drawable bg = new BorderCropDrawable(mLauncher.getResources().getDrawable(R.drawable.cling_bg),
                    true, true, true, false);
            content.setBackground(bg);
        }

        root.addView(cling);

        // Animate
        content.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                content.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                ObjectAnimator anim;
                if (TAG_CROP_TOP_AND_SIDES.equals(content.getTag())) {
                    content.setTranslationY(-content.getMeasuredHeight());
                    anim = LauncherAnimUtils.ofFloat(content, "translationY", 0);
                } else {
                    content.setScaleX(0);
                    content.setScaleY(0);
                    PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1);
                    PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1);
                    anim = LauncherAnimUtils.ofPropertyValuesHolder(content, scaleX, scaleY);
                }

                anim.setDuration(SHOW_CLING_DURATION);
                anim.setInterpolator(new LogDecelerateInterpolator(100, 0));
                anim.start();
            }
        });
    }

    @Thunk
    void dismissFolderCategoryCling() {
        Runnable dismissCb = new Runnable() {
            public void run() {
                dismissCling(mLauncher.findViewById(R.id.folder_category_cling), null, DISMISS_CLING_DURATION);
            }
        };
        mLauncher.getWorkspace().post(dismissCb);
    }

    /**
     * Hides the specified Cling
     */
    @Thunk
    void dismissCling(final View cling, final Runnable postAnimationCb, int duration) {
        // To catch cases where siblings of top-level views are made invisible, just check whether
        // the cling is directly set to GONE before dismissing it.
        if (cling != null) {
            final Runnable cleanUpClingCb = new Runnable() {
                public void run() {
                    ViewGroup root = (ViewGroup) mLauncher.findViewById(R.id.launcher);
                    root.removeView(cling);
                    mIsVisible = false;
                    if (postAnimationCb != null) {
                        postAnimationCb.run();
                    }
                }
            };
            if (duration <= 0) {
                cleanUpClingCb.run();
            } else {
                cling.animate().alpha(0).setDuration(duration).withEndAction(cleanUpClingCb);
            }
        }
    }

    public boolean isVisible() {
        return mIsVisible;
    }
}

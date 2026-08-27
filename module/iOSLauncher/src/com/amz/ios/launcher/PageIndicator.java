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
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import androidx.dynamicanimation.animation.SpringAnimation;

import java.util.ArrayList;

public class PageIndicator extends LinearLayout implements View.OnClickListener {
    @SuppressWarnings("unused")
    private static final String TAG = "PageIndicator";

    // Want this to look good? Keep it odd
    private static final boolean MODULATE_ALPHA_ENABLED = false;
    public static int PAGEDVIEW_WORKSPACE = 0;
    public static int PAGEDVIEW_THEMESPACE = 1;
    private LayoutInflater mLayoutInflater;
    private int[] mWindowRange = new int[2];
    private int mMaxWindowSize;
    private ArrayList<PageIndicatorMarker> mMarkers =
            new ArrayList<PageIndicatorMarker>();
    private int mActiveMarkerIndex;

    public Runnable mSearchAnimRunnable;
    public TextViewCustomFont mSearchTV;

    @Override
    public void onClick(View v) {
        Launcher launcher = Launcher.getLauncher(getContext());

        launcher.mSearchViewLayout.startOpen();
        launcher.showFolderBlurBackground(0.1f);
        AnimatorSet animationSet = LauncherAnimUtils.createAnimatorSet();
        animationSet.playTogether(
                ObjectAnimator.ofPropertyValuesHolder(launcher.mSearchViewLayout, PropertyValuesHolder.ofFloat(View.ALPHA, 1.0f)), ObjectAnimator.ofFloat(launcher.getBlurBackground(), "alpha", 1.0f)
        );
        animationSet.setInterpolator(new DecelerateInterpolator());
        animationSet.setDuration(268);
        animationSet.start();

        SpringAnimation springAnimation = launcher.mSearchPullDetector.mSpringAnimation;
        springAnimation.setStartVelocity(launcher.mSearchPullDetector.mMinimumFlingVelocity);
        springAnimation.animateToFinalPosition(100);
        springAnimation.start();
    }

    public static class PageMarkerResources {
        int activeId;
        int inactiveId;

        public PageMarkerResources(int space) {
            activeId = R.drawable.ic_pageindicator_current;
            inactiveId = R.drawable.ic_pageindicator_default;

            if (space == PAGEDVIEW_WORKSPACE){
                activeId = R.drawable.ic_pageindicator_current;
                inactiveId = R.drawable.ic_pageindicator_default;
            }
            else if (space == PAGEDVIEW_THEMESPACE){
                activeId = R.drawable.ic_theme_pageindicator_current;
                inactiveId = R.drawable.ic_theme_pageindicator_default;
            }

        }

        public PageMarkerResources(int aId, int iaId) {
            activeId = aId;
            inactiveId = iaId;
        }
    }

    public PageIndicator(Context context) {
        this(context, null);
    }

    public PageIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.PageIndicator, defStyle, 0);
        mMaxWindowSize = a.getInteger(R.styleable.PageIndicator_windowSize, 15);
        mWindowRange[0] = 0;
        mWindowRange[1] = 0;

        mLayoutInflater = LayoutInflater.from(context);
        a.recycle();

        // Set the layout transition properties
        LayoutTransition transition = getLayoutTransition();
        transition.setDuration(368L);
        setGravity(Gravity.CENTER);
        setOnClickListener(this);

        mSearchAnimRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    for (PageIndicatorMarker marker : mMarkers){
                        if (marker != null)
                            marker.setVisibility(View.GONE);
                    }
                    if (mSearchTV != null) {
                        mSearchTV.setVisibility(View.VISIBLE);
                        mSearchTV.animate().alpha(1.0f).setDuration(668L).start();
                    }
                } catch (Throwable th) {
                    th.getMessage();
                }
            }
        };
//        addView(mSearchBtn);
    }

    public void disableSearch(){
        if (this.mSearchTV != null){
            mSearchTV.setVisibility(View.GONE);
            mSearchTV.setAlpha(0.0f);
        }
        for (PageIndicatorMarker marker : mMarkers){
            marker.setVisibility(View.VISIBLE);
        }
        // [BUG FIX] Chấm trang bị LỆCH HẲN SANG TRÁI (đo trên Samsung A50 Android 9: khung
        // 402..678 = 276px nhưng 3 chấm chỉ nằm 47..143, dư 86px bên phải).
        // Nguyên nhân: khung từng được đo ở trạng thái search — lúc đó mSearchTV VISIBLE và rộng
        // 160px, trong khi 3 chấm chỉ rộng 96px. Khi quay lại chế độ chấm, mSearchTV đã GONE nhưng
        // container KHÔNG đo lại (LinearLayout giữ width cũ, lại có animateLayoutChanges) -> khung
        // vẫn rộng theo spotlight, gravity=center căn giữa CỦA KHUNG RỘNG nên cụm chấm lệch trái.
        // Ép đo lại để width bám đúng nội dung đang hiển thị -> cụm chấm về giữa màn.
        requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        // Nền kính mờ của thanh indicator được quản bởi GlassBlurWindowController (compositor blur,
        // window MEDIA riêng) — attach 1 lần trong Launcher sau findViewById(page_indicator). Không set
        // background ở đây nữa (controller bám bounds container mỗi frame, kể cả khi số trang đổi width).
    }

    private void enableLayoutTransitions() {
        LayoutTransition transition = getLayoutTransition();
        transition.enableTransitionType(LayoutTransition.APPEARING);
        transition.enableTransitionType(LayoutTransition.DISAPPEARING);
        transition.enableTransitionType(LayoutTransition.CHANGE_APPEARING);
        transition.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
    }

    private void disableLayoutTransitions() {
        LayoutTransition transition = getLayoutTransition();
        transition.disableTransitionType(LayoutTransition.APPEARING);
        transition.disableTransitionType(LayoutTransition.DISAPPEARING);
        transition.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
        transition.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
    }

    void offsetWindowCenterTo(final int activeIndex, boolean allowAnimations) {
        if (activeIndex < 0) {
            new Throwable().printStackTrace();
        }
        int windowSize = Math.min(mMarkers.size(), mMaxWindowSize);
        int hWindowSize = windowSize / 2;
        float hfWindowSize = windowSize / 2f;
        int windowStart = Math.max(0, activeIndex - hWindowSize);
        int windowEnd = Math.min(mMarkers.size(), windowStart + mMaxWindowSize);
        windowStart = windowEnd - Math.min(mMarkers.size(), windowSize);
        int windowMid = windowStart + (windowEnd - windowStart) / 2;
        boolean windowAtStart = (windowStart == 0);
        boolean windowAtEnd = (windowEnd == mMarkers.size());
        boolean windowMoved = (mWindowRange[0] != windowStart) ||
                (mWindowRange[1] != windowEnd);

        if (!allowAnimations) {
            disableLayoutTransitions();
        }

        // Remove all the previous children that are no longer in the window
        for (int i = getChildCount() - 1; i >= 0; --i) {
            if (getChildAt(i) instanceof PageIndicatorMarker) {
                PageIndicatorMarker marker = (PageIndicatorMarker) getChildAt(i);
                int markerIndex = mMarkers.indexOf(marker);
                if (markerIndex < windowStart || markerIndex >= windowEnd) {
                    removeView(marker);
                }
            }
        }

        // Add all the new children that belong in the window
        for (int i = 0; i < mMarkers.size(); ++i) {
            PageIndicatorMarker marker = mMarkers.get(i);
            if (windowStart <= i && i < windowEnd) {
                if (indexOfChild(marker) < 0) {
                    addView(marker, i - windowStart);
                }
                if (i == activeIndex) {
                    ViewPropertyAnimator animator = marker.mActiveMarker.animate();
                    if (windowMoved) {
                        animator.cancel();
                        marker.mActiveMarker.setAlpha(1.0f);
                        marker.mActiveMarker.setScaleX(1.0f);
                        marker.mActiveMarker.setScaleY(1.0f);
                        marker.mInactiveMarker.animate().cancel();
                        marker.mInactiveMarker.setAlpha(0.0f);
                    } else {
                        animator.alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(175L).start();
                        marker.mInactiveMarker.animate().alpha(0.0f).setDuration(175L).start();
                    }
                } else {
                    marker.inactivate(windowMoved);
                }
            } else {
                marker.inactivate(true);
            }

            if (MODULATE_ALPHA_ENABLED) {
                // Update the marker's alpha
                float alpha = 1f;
                if (mMarkers.size() > windowSize) {
                    if ((windowAtStart && i > hWindowSize) ||
                        (windowAtEnd && i < (mMarkers.size() - hWindowSize)) ||
                        (!windowAtStart && !windowAtEnd)) {
                        alpha = 1f - Math.abs((i - windowMid) / hfWindowSize);
                    }
                }
                marker.animate().alpha(alpha).setDuration(500).start();
            }
        }

        if (!allowAnimations) {
            enableLayoutTransitions();
        }

        mWindowRange[0] = windowStart;
        mWindowRange[1] = windowEnd;
    }

    public void addMarker(int index, PageMarkerResources marker, boolean allowAnimations) {
        index = Math.max(0, Math.min(index, mMarkers.size()));

        PageIndicatorMarker m =
            (PageIndicatorMarker) mLayoutInflater.inflate(R.layout.page_indicator_marker,
                    this, false);
        m.setMarkerDrawables(marker.activeId, marker.inactiveId);

        mMarkers.add(index, m);
        offsetWindowCenterTo(mActiveMarkerIndex, allowAnimations);
    }

    public void addMarkers(ArrayList<PageMarkerResources> markers, boolean allowAnimations) {
        for (int i = 0; i < markers.size(); ++i) {
            addMarker(Integer.MAX_VALUE, markers.get(i), allowAnimations);
        }
    }

    void updateMarker(int index, PageMarkerResources marker) {
        if (index >= mMarkers.size()) {
            return;
        }

        PageIndicatorMarker m = mMarkers.get(index);
        m.setMarkerDrawables(marker.activeId, marker.inactiveId);
    }

    public void removeMarker(int index, boolean allowAnimations) {
        if (mMarkers.size() > 0) {
            index = Math.max(0, Math.min(mMarkers.size() - 1, index));
            mMarkers.remove(index);
            offsetWindowCenterTo(mActiveMarkerIndex, allowAnimations);
        }
    }
    public void removeAllMarkers(boolean allowAnimations) {
        while (mMarkers.size() > 0) {
            removeMarker(Integer.MAX_VALUE, allowAnimations);
        }
    }

    public void setActiveMarker(final int index) {
        // Center the active marker

        if (index != this.mActiveMarkerIndex) {
            this.mActiveMarkerIndex = index;
            offsetWindowCenterTo(index, false);
        }

    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        if (child instanceof TextViewCustomFont) {
            this.mSearchTV = (TextViewCustomFont) findViewById(R.id.search_spotlight);
        }
    }
}

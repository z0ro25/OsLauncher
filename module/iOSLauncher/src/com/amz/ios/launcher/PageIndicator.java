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

    /**
     * Lượt thử lại của {@link #mSearchAnimRunnable} khi mSearchTV chưa đo được kích thước.
     * Xem ghi chú bug ở chỗ khởi tạo mSearchAnimRunnable.
     */
    private Runnable mSearchRetryRunnable;
    public TextViewCustomFont mSearchTV;

    /**
     * LayoutTransition gốc (sinh ra từ {@code animateLayoutChanges="true"}) của thanh indicator.
     * Giữ lại để tạm GỠ khi cần ép container co width NGAY (search -> chấm) rồi KHÔI PHỤC — xem
     * ghi chú bug ở {@link #disableSearch()}.
     */
    private LayoutTransition mSavedTransition;
    /** Khôi phục lại {@link #mSavedTransition} sau khi width đã co xong. */
    private Runnable mRestoreTransitionRunnable;

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
        // Bố cục mới: ô nhập ở ĐÁY -> search mở dừng ở translationY 0 (trước là 100 sẽ đẩy ô input
        // lọt khỏi đáy màn). Bàn phím do adjustPan pan lên khi et_search focus.
        springAnimation.animateToFinalPosition(0);
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
        mSavedTransition = transition;   // giữ ref để tạm gỡ/khôi phục trong disableSearch()
        setGravity(Gravity.CENTER);
        setOnClickListener(this);

        mSearchAnimRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    // [BUG FIX] "Dot indicator biến mất, khung rỗng, cũng không thấy ô Tìm kiếm."
                    //   Runnable này ẩn CHẤM rồi hiện SEARCH. Nhưng nếu mSearchTV không đo được kích
                    //   thước (w=0/h=0) thì nó VISIBLE mà chẳng vẽ ra gì — trong khi chấm đã bị ẩn
                    //   mất -> người dùng thấy khung indicator RỖNG HOÀN TOÀN.
                    //   Đo được trên Samsung A50 sau chuỗi giữ-app -> Edit Home Screen -> Done:
                    //     ind[0] PageIndicatorMarker vis=8 (GONE)
                    //     ind[1] PageIndicatorMarker vis=8 (GONE)
                    //     ind[2] TextViewCustomFont  vis=0 alpha=1.0 w=0 h=0   <- rỗng
                    //   (mSearchTV mất kích thước vì lượt layout của nó bị huỷ giữa chừng khi vào/ra
                    //   edit — cùng họ với các bug layout đã ghi ở Launcher.setStatusBarHiddenForEdit.)
                    //
                    //   Nguyên tắc: THÀ GIỮ CHẤM còn hơn để trống. Chỉ đổi sang search khi search
                    //   thực sự vẽ được; nếu chưa đo xong thì yêu cầu đo lại và thử lại một lần.
                    if (mSearchTV == null) {
                        return;   // không có search -> giữ nguyên chấm
                    }
                    boolean searchDoDuoc = mSearchTV.getWidth() > 0 && mSearchTV.getHeight() > 0;
                    if (!searchDoDuoc) {
                        // Chưa có kích thước: ép đo lại rồi thử lại SAU, và QUAN TRỌNG là chưa ẩn
                        // chấm — để khung không bao giờ rỗng trong lúc chờ.
                        mSearchTV.setVisibility(View.VISIBLE);
                        mSearchTV.setAlpha(0.0f);
                        requestLayout();
                        removeCallbacks(mSearchRetryRunnable);
                        postDelayed(mSearchRetryRunnable, 120L);
                        return;
                    }

                    for (PageIndicatorMarker marker : mMarkers){
                        if (marker != null)
                            marker.setVisibility(View.GONE);
                    }
                    mSearchTV.setVisibility(View.VISIBLE);
                    mSearchTV.animate().alpha(1.0f).setDuration(668L).start();
                } catch (Throwable th) {
                    th.getMessage();
                }
            }
        };

        // Lượt thử lại sau khi đã ép đo mSearchTV. Nếu vẫn không đo được thì BỎ CUỘC và giữ chấm —
        // khung có chấm vẫn đúng hơn khung trống trơn.
        mSearchRetryRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (mSearchTV == null) return;
                    if (mSearchTV.getWidth() > 0 && mSearchTV.getHeight() > 0) {
                        for (PageIndicatorMarker marker : mMarkers){
                            if (marker != null) marker.setVisibility(View.GONE);
                        }
                        mSearchTV.setVisibility(View.VISIBLE);
                        mSearchTV.animate().alpha(1.0f).setDuration(668L).start();
                    } else {
                        // Không đo được -> trả về trạng thái CHẤM cho chắc chắn nhìn thấy được.
                        mSearchTV.setVisibility(View.GONE);
                        mSearchTV.setAlpha(0.0f);
                        for (PageIndicatorMarker marker : mMarkers){
                            if (marker != null) marker.setVisibility(View.VISIBLE);
                        }
                        requestLayout();
                    }
                } catch (Throwable th) {
                    th.getMessage();
                }
            }
        };

        // Khôi phục LayoutTransition sau khi container đã co width xong. Đặt lại đúng ref gốc để các
        // lượt chuyển chấm<->search về sau vẫn animate mượt như baseline.
        mRestoreTransitionRunnable = new Runnable() {
            @Override
            public void run() {
                if (getLayoutTransition() == null && mSavedTransition != null) {
                    setLayoutTransition(mSavedTransition);
                }
            }
        };
//        addView(mSearchBtn);
    }

    public void disableSearch(){
        // Sắp quay về chế độ CHẤM -> huỷ lượt thử-lại-hiện-search đang chờ (nếu có), tránh nó chạy
        // sau lưng và ẩn chấm vừa hiện. Xem mSearchRetryRunnable.
        removeCallbacks(mSearchRetryRunnable);

        // Chỉ khi search ĐANG chiếm chỗ (VISIBLE) thì container mới đang bị phình theo bề rộng ô Tìm
        // kiếm -> mới cần ép co. Nếu vốn đã ở chế độ chấm thì không đụng gì -> giữ nguyên baseline.
        boolean searchDangChiemCho = mSearchTV != null && mSearchTV.getVisibility() == View.VISIBLE;

        if (this.mSearchTV != null){
            mSearchTV.setVisibility(View.GONE);
            mSearchTV.setAlpha(0.0f);
        }
        for (PageIndicatorMarker marker : mMarkers){
            marker.setVisibility(View.VISIBLE);
        }

        // [BUG FIX] Chấm trang bị LỆCH HẲN SANG TRÁI + (Android 9) NỀN KÍNH bọc quanh phình rộng bằng
        // khung ô Tìm kiếm dù chỉ còn 2 chấm.
        // Nguyên nhân: khung từng được đo ở trạng thái search — lúc đó mSearchTV VISIBLE và rộng
        // ~160px, trong khi cụm chấm chỉ rộng ~96px. Khi quay lại chế độ chấm, mSearchTV đã GONE nhưng
        // container KHÔNG co width: animateLayoutChanges (LayoutTransition) GIỮ LẠI khoảng trống của
        // mSearchTV trong lúc animate DISAPPEARING -> requestLayout() đơn thuần không đủ. Hệ quả trên
        // Android 9: nền kính đi nhánh fallback (GlassBlurDrawable vẽ theo bounds container) nên bám
        // đúng bề rộng phình -> nền dài lê thê ôm cả khung search rỗng.
        // Cách xử lý: chỉ ở đúng lượt search->chấm, TẠM GỠ LayoutTransition để width co NGAY theo nội
        // dung thật (chỉ còn chấm), rồi khôi phục transition ở frame sau (giữ animation cho lần sau).
        if (searchDangChiemCho) {
            setLayoutTransition(null);
            requestLayout();
            removeCallbacks(mRestoreTransitionRunnable);
            // > thời lượng transition (368ms) để layout co xong hẳn mới bật lại animation.
            postDelayed(mRestoreTransitionRunnable, 450L);
        } else {
            requestLayout();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        // [BUG FIX] Lớp bảo vệ CUỐI: khung indicator KHÔNG BAO GIỜ được rỗng.
        //   Sau mỗi lượt layout, nếu cả chấm lẫn search đều không hiển thị được thì trả về trạng
        //   thái CHẤM — đó là trạng thái mặc định luôn vẽ được. Đặt ở onLayout vì đây là nơi biết
        //   chắc kích thước thật của các con, không phải phỏng đoán.
        ensureIndicatorNotEmpty();
        // Nền kính mờ của thanh indicator được quản bởi GlassBlurWindowController (compositor blur,
        // window MEDIA riêng) — attach 1 lần trong Launcher sau findViewById(page_indicator). Không set
        // background ở đây nữa (controller bám bounds container mỗi frame, kể cả khi số trang đổi width).
    }

    /**
     * Bảo đảm thanh indicator luôn hiển thị được MỘT trong hai: cụm chấm, hoặc ô Tìm kiếm.
     *
     * [BUG FIX] "Khung dot indicator rỗng, không chấm cũng không search" (đo trên Samsung A50):
     *   mSearchAnimRunnable ẩn chấm rồi hiện search; nếu search không đo được kích thước (w=0,h=0)
     *   thì khung trống hoàn toàn. Ở đây bắt đúng trạng thái đó sau mỗi lượt layout và khôi phục
     *   cụm chấm — trạng thái mặc định luôn vẽ được.
     *
     * Không đụng gì khi đang bình thường (một trong hai đang hiện), nên an toàn với mọi luồng cũ.
     */
    private void ensureIndicatorNotEmpty() {
        boolean searchHienDuoc = mSearchTV != null
                && mSearchTV.getVisibility() == View.VISIBLE
                && mSearchTV.getWidth() > 0 && mSearchTV.getHeight() > 0;
        if (searchHienDuoc) return;

        boolean coChamHien = false;
        for (PageIndicatorMarker marker : mMarkers) {
            if (marker != null && marker.getVisibility() == View.VISIBLE) {
                coChamHien = true;
                break;
            }
        }
        if (coChamHien) return;

        // Cả hai đều không hiện được -> khung đang RỖNG. Trả về cụm chấm.
        if (mSearchTV != null) {
            mSearchTV.setVisibility(View.GONE);
            mSearchTV.setAlpha(0.0f);
        }
        for (PageIndicatorMarker marker : mMarkers) {
            if (marker != null) marker.setVisibility(View.VISIBLE);
        }
        // Đang trong lượt layout -> hoãn requestLayout sang frame sau, tránh cảnh báo
        // "requestLayout() improperly called during layout" và bị nuốt mất.
        post(new Runnable() {
            @Override
            public void run() {
                requestLayout();
            }
        });
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

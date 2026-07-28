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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.amz.ios.launcher.theme.ThemeManager;
import com.amz.ios.launcher.util.Thunk;

public abstract class BaseFolderIcon extends FrameLayout  {
    private static final String TAG = "BaseFolderIcon";

    // The number of icons to display in the
    public static final int NUM_ITEMS_IN_PREVIEW = 9;
    public static final int NUM_ITEMS_IN_PREVIEW_PER_ROW = 3;
    protected static final int CONSUMPTION_ANIMATION_DURATION = 100;
    protected static final int DROP_IN_ANIMATION_DURATION = 400;
    protected static final int INITIAL_ITEM_ANIMATION_DURATION = 350;
    protected static final int FINAL_ITEM_ANIMATION_DURATION = 200;

    // The degree to which the inner ring grows when accepting drop
    protected static final float INNER_RING_GROWTH_FACTOR = 0.15f;

    // The degree to which the outer ring is scaled in its natural state
    protected static final float OUTER_RING_GROWTH_FACTOR = 0.3f;

    // Flag as to whether or not to draw an outer ring. Currently none is designed.
    public static final boolean HAS_OUTER_RING = true;

    // Flag whether the folder should open itself when an item is dragged over is enabled.
    public static final boolean SPRING_LOADING_ENABLED = true;

    // The degree to which the item in the back of the stack is scaled [0...1]
    // (0 means it's not scaled at all, 1 means it's scaled to nothing)
    protected static final float PERSPECTIVE_SCALE_FACTOR = 0.35f;

    // Delay when drag enters until the folder opens, in miliseconds.
    protected static final int ON_OPEN_DELAY = 800;

    protected static boolean sStaticValuesDirty = true;

    public static Drawable sSharedFolderLeaveBehind = null;

    protected Launcher mLauncher;

    ImageView mPreviewBackground;

    // These variables are all associated with the drawing of the preview; they are stored
    // as member variables for shared usage and to avoid computation on each frame
    protected int mIntrinsicIconSize;
    protected float mBaselineIconScale;
    protected int mBaselineIconSize;
    protected int mBaselineIconGapSize;
    protected int mAvailableSpaceInPreview;
    protected int mTotalWidth = -1;
    protected int mPreviewOffsetX;
    protected int mPreviewOffsetY;
    protected boolean mAnimating = false;
    protected Rect mOldBounds = new Rect();
    protected float mSlop;



    public BaseFolderIcon(@NonNull Context context) {
        super(context);
    }

    public BaseFolderIcon(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public class PreviewItemDrawingParams {
        float transX;
        float transY;
        float scale;
        int overlayAlpha;
        Drawable drawable;

        PreviewItemDrawingParams(float transX, float transY, float scale, int overlayAlpha) {
            this.transX = transX;
            this.transY = transY;
            this.scale = scale;
            this.overlayAlpha = overlayAlpha;
        }
    }

    public static class FolderRingAnimator {
        public int mCellX;
        public int mCellY;
        @Thunk
        CellLayout mCellLayout;
        public float mOuterRingSize;
        public FolderIcon mFolderIcon = null;
        public static Drawable sSharedOuterRingDrawable = null;
        public static int sPreviewSize = -1;
        public static int sPreviewPadding = -1;
        public static int sPreviewOffsetX = -1;
        public static int sPreviewOffsetY = -1;
        public static int sPreviewSubIconGap = -1;

        private ValueAnimator mAcceptAnimator;
        private ValueAnimator mNeutralAnimator;

        public FolderRingAnimator(Launcher launcher, FolderIcon folderIcon) {
            mFolderIcon = folderIcon;
            Resources res = launcher.getResources();

            // We need to reload the static values when configuration changes in case they are
            // different in another configuration
            if (sStaticValuesDirty) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    throw new RuntimeException("FolderRingAnimator loading drawables on non-UI thread "
                            + Thread.currentThread());
                }

                DeviceProfile grid = launcher.getDeviceProfile();
                ThemeManager themeManager = LauncherAppState.getInstance().getThemeManager();
                DisplayMetrics dm = res.getDisplayMetrics();
                sPreviewSize = grid.folderIconSizePx;

                String folderPreviewValue = themeManager.readFolderPreviewValue();
                sSharedFolderLeaveBehind = sSharedOuterRingDrawable = themeManager.readFolderBgDrawable();
                if (!TextUtils.isEmpty(folderPreviewValue)) {
                    try {
                        String[] strs = folderPreviewValue.split(",");
                        float padding = Float.parseFloat(strs[0]);
                        float iconGap = Float.parseFloat(strs[1]);
                        float offsetX = Float.parseFloat(strs[2]);
                        float offsetY = Float.parseFloat(strs[3]);


                        float scale = 1.0f * sPreviewSize / Utilities.pxFromDp(ThemeManager.BASELINE_ICON_SIZE_DP, dm);

                        sPreviewPadding = (int) (Utilities.pxFromDp(padding*1.5f, dm) * scale);
                        sPreviewSubIconGap = (int) (Utilities.pxFromDp(iconGap, dm) * scale);
                        sPreviewOffsetX = (int) (Utilities.pxFromDp(offsetX, dm) * scale);
                        sPreviewOffsetY = (int) (Utilities.pxFromDp(offsetY, dm) * scale);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                sStaticValuesDirty = false;
            }
        }

        public void animateToAcceptState() {
            if (mNeutralAnimator != null) {
                mNeutralAnimator.cancel();
            }
            mAcceptAnimator = LauncherAnimUtils.ofFloat(mCellLayout, 0f, 1f);
            mAcceptAnimator.setDuration(CONSUMPTION_ANIMATION_DURATION);

            final int previewSize = sPreviewSize;
            mAcceptAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    final float percent = (Float) animation.getAnimatedValue();
                    mOuterRingSize = (1 + percent * OUTER_RING_GROWTH_FACTOR) * previewSize;
                    if (mCellLayout != null) {
                        mCellLayout.invalidate();
                    }
                }
            });
            mAcceptAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (mFolderIcon != null) {
                        mFolderIcon.mPreviewBackground.setVisibility(INVISIBLE);
                    }
                }
            });
            mAcceptAnimator.start();
        }

        public void animateToNaturalState() {
            if (mAcceptAnimator != null) {
                mAcceptAnimator.cancel();
            }
            mNeutralAnimator = LauncherAnimUtils.ofFloat(mCellLayout, 0f, 1f);
            mNeutralAnimator.setDuration(CONSUMPTION_ANIMATION_DURATION);

            final int previewSize = sPreviewSize;
            mNeutralAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    final float percent = (Float) animation.getAnimatedValue();
                    mOuterRingSize = (1 + (1 - percent) * OUTER_RING_GROWTH_FACTOR) * previewSize;
                    if (mCellLayout != null) {
                        mCellLayout.invalidate();
                    }
                }
            });
            mNeutralAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mCellLayout != null) {
                        mCellLayout.hideFolderAccept(FolderRingAnimator.this);
                    }
                    if (mFolderIcon != null) {
                        mFolderIcon.mPreviewBackground.setVisibility(VISIBLE);
                    }
                }
            });
            mNeutralAnimator.start();
        }

        // Location is expressed in window coordinates
        public void getCell(int[] loc) {
            loc[0] = mCellX;
            loc[1] = mCellY;
        }

        // Location is expressed in window coordinates
        public void setCell(int x, int y) {
            mCellX = x;
            mCellY = y;
        }

        public void setCellLayout(CellLayout layout) {
            mCellLayout = layout;
        }

        public float getOuterRingSize() {
            return mOuterRingSize;
        }
    }

    protected void computePreviewDrawingParams(int drawableSize, int totalSize) {
        if (mIntrinsicIconSize != drawableSize || mTotalWidth != totalSize) {
            DeviceProfile grid = mLauncher.getDeviceProfile();

            mIntrinsicIconSize = drawableSize;
            mTotalWidth = totalSize;

            final int previewSize = mPreviewBackground.getLayoutParams().height;
            final int previewPadding = FolderRingAnimator.sPreviewPadding;
            int previewSubIconGap = FolderRingAnimator.sPreviewSubIconGap;
            int previewOffsetX = FolderRingAnimator.sPreviewOffsetX;
            int previewOffsetY = FolderRingAnimator.sPreviewOffsetY;

            mAvailableSpaceInPreview = (previewSize - 2 * previewPadding);
            int ajustIconSize = (mAvailableSpaceInPreview - previewSubIconGap*2) / NUM_ITEMS_IN_PREVIEW_PER_ROW;
            mBaselineIconScale = (1.0f * ajustIconSize / mIntrinsicIconSize);
            mBaselineIconSize = (int) (mIntrinsicIconSize * mBaselineIconScale);
            mBaselineIconGapSize = previewSubIconGap;

            mPreviewOffsetX = (mTotalWidth - mAvailableSpaceInPreview) / 2 + previewOffsetX;
            mPreviewOffsetY = previewPadding + grid.folderBackgroundOffset + previewOffsetY;
        }
    }

    protected PreviewItemDrawingParams computePreviewItemDrawingParams(int index, PreviewItemDrawingParams params) {
        int row = index / NUM_ITEMS_IN_PREVIEW_PER_ROW;
        int col = index % NUM_ITEMS_IN_PREVIEW_PER_ROW;
        int transX = col * (mBaselineIconSize + mBaselineIconGapSize);
        int transY = row * (mBaselineIconSize + mBaselineIconGapSize) + getPaddingTop();
        float totalScale = mBaselineIconScale;
        int overlayAlpha = 1;
        if (params == null) {
            params = new PreviewItemDrawingParams(transX, transY, totalScale, overlayAlpha);
        } else {
            params.transX = transX;
            params.transY = transY;
            params.scale = totalScale;
            params.overlayAlpha = 1;
        }
        return params;
    }

    protected void drawPreviewItem(Canvas canvas, PreviewItemDrawingParams params) {
        Log.d(TAG, "drawPreviewItem");
        canvas.save();
        canvas.translate(params.transX + mPreviewOffsetX, params.transY + mPreviewOffsetY);
        canvas.scale(params.scale, params.scale);
        Drawable d = params.drawable;

        if (d != null) {
            mOldBounds.set(d.getBounds());
            d.setBounds(0, 0, mIntrinsicIconSize, mIntrinsicIconSize);
            if (d instanceof FastBitmapDrawable) {
                FastBitmapDrawable fd = (FastBitmapDrawable) d;
                int oldBrightness = fd.getBrightness();
                fd.setBrightness(params.overlayAlpha);
                d.draw(canvas);
                fd.setBrightness(oldBrightness);
            } else {
                d.setColorFilter(Color.argb(params.overlayAlpha, 255, 255, 255),
                        PorterDuff.Mode.SRC_ATOP);
                d.draw(canvas);
                d.clearColorFilter();
            }
            d.setBounds(mOldBounds);
        }
        canvas.restore();
    }

    private static final int SC_MULTIPLE_CHOICES = 200;
    public void zoomPreviewBackground(boolean isDelay) {
        float fScale = isDelay ? 1.2f : 1.0f;
        int nDelay = isDelay ? 100 : SC_MULTIPLE_CHOICES;
        LauncherViewPropertyAnimator launcherViewPropertyAnimator = new LauncherViewPropertyAnimator(this.mPreviewBackground);
        launcherViewPropertyAnimator.scaleX(fScale).scaleY(fScale).setDuration(200);
        launcherViewPropertyAnimator.setInterpolator(isDelay ? new DecelerateInterpolator(1.5f) : new OvershootInterpolator(5.0f));
        launcherViewPropertyAnimator.setStartDelay((long) nDelay);
        launcherViewPropertyAnimator.start();
    }
}

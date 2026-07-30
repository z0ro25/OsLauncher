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
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;

import com.amz.ios.launcher.BubbleTextView.BubbleTextShadowHandler;
import com.amz.ios.launcher.BaseFolderIcon.FolderRingAnimator;
import com.amz.ios.launcher.accessibility.FolderAccessibilityHelper;
import com.amz.ios.launcher.accessibility.WorkspaceAccessibilityHelper;
import com.amz.ios.launcher.config.Settings;
import com.amz.ios.launcher.util.Thunk;
import com.github.mmin18.widget.RealtimeBlurView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class CellLayout extends BaseCellLayout implements BubbleTextShadowHandler {

    static final String TAG = "CellLayout";

    private Drawable mNewInstallPrefix;

    private long mScreenId = -999;

    public CellLayout(Context context) {
        this(context, null);
    }

    public CellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // A ViewGroup usually does not draw, but CellLayout needs to draw a rectangle to show
        // the user where a dragged item will land when dropped.
        setWillNotDraw(false);
        setClipToPadding(false);
        mLauncher = (Launcher) context;

        DeviceProfile grid = mLauncher.getDeviceProfile();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CellLayout, defStyle, 0);
        mScreenId = 0;
        mCellWidth = mCellHeight = -1;
        mFixedCellWidth = mFixedCellHeight = -1;
        mWidthGap = mOriginalWidthGap = 0;
        mHeightGap = mOriginalHeightGap = 0;
        mMaxGap = Integer.MAX_VALUE;
        mCountX = grid.inv.numColumns;
        mCountY = grid.inv.numRows;
        mOccupied = new boolean[mCountX][mCountY];
        mTmpOccupied = new boolean[mCountX][mCountY];
        mPreviousReorderDirection[0] = INVALID_DIRECTION;
        mPreviousReorderDirection[1] = INVALID_DIRECTION;

        a.recycle();

        setAlwaysDrawnWithCacheEnabled(false);

        final Resources res = getResources();
        mHotseatScale = (float) grid.hotseatIconSizePx / grid.iconSizePx;

        mBackground = (TransitionDrawable) res.getDrawable(R.drawable.cell_layout_panel);
        mBackground.setCallback(this);
        mBackground.setAlpha((int) (mBackgroundAlpha * 255));

        mReorderPreviewAnimationMagnitude = (REORDER_PREVIEW_MAGNITUDE *
                grid.iconSizePx);

        // Initialize the data structures used for the drag visualization.
        mEaseOutInterpolator = new DecelerateInterpolator(2.5f); // Quint ease out
        mDragCell[0] = mDragCell[1] = -1;
        for (int i = 0; i < mDragOutlines.length; i++) {
            mDragOutlines[i] = new Rect(-1, -1, -1, -1);
        }

        // When dragging things around the home screens, we show a green outline of
        // where the item will land. The outlines gradually fade out, leaving a trail
        // behind the drag path.
        // Set up all the animations that are used to implement this fading.
        final int duration = res.getInteger(R.integer.config_dragOutlineFadeTime);
        final float fromAlphaValue = 0;
        final float toAlphaValue = (float) res.getInteger(R.integer.config_dragOutlineMaxAlpha);

        Arrays.fill(mDragOutlineAlphas, fromAlphaValue);

        for (int i = 0; i < mDragOutlineAnims.length; i++) {
            final InterruptibleInOutAnimator anim =
                    new InterruptibleInOutAnimator(this, duration, fromAlphaValue, toAlphaValue);
            anim.getAnimator().setInterpolator(mEaseOutInterpolator);
            final int thisIndex = i;
            anim.getAnimator().addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    final Bitmap outline = (Bitmap) anim.getTag();

                    // If an animation is started and then stopped very quickly, we can still
                    // get spurious updates we've cleared the tag. Guard against this.
                    if (outline == null) {
                        @SuppressWarnings("all") // suppress dead code warning
                        final boolean debug = false;
                        if (debug) {
                            Object val = animation.getAnimatedValue();
                            Log.d(TAG, "anim " + thisIndex + " update: " + val + ", isStopped " + anim.isStopped());
                        }
                        // Try to prevent it from continuing to run
                        animation.cancel();
                    } else {
                        mDragOutlineAlphas[thisIndex] = (Float) animation.getAnimatedValue();
                        CellLayout.this.invalidate(mDragOutlines[thisIndex]);
                    }
                }
            });
            // The animation holds a reference to the drag outline bitmap as long is it's
            // running. This way the bitmap can be GCed when the animations are complete.
            anim.getAnimator().addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if ((Float) ((ValueAnimator) animation).getAnimatedValue() == 0f) {
                        anim.setTag(null);
                    }
                }
            });
            mDragOutlineAnims[i] = anim;
        }

        mShortcutsAndWidgets = new ShortcutAndWidgetContainer(context);
        mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap, mHeightGap,
                mCountX, mCountY);
        mStylusEventHelper = new StylusEventHelper(this);
        mTouchFeedbackView = new ClickShadowView(context);
        this.mNewInstallPrefix = Utilities.getNewInstallPrefix(context);
        addView(mTouchFeedbackView);
        addView(mShortcutsAndWidgets);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void enableAccessibleDrag(boolean enable, int dragType) {
        mUseTouchHelper = enable;
        if (!enable) {
            ViewCompat.setAccessibilityDelegate(this, null);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            getShortcutsAndWidgets().setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            setOnClickListener(mLauncher);
        } else {
            if (dragType == WORKSPACE_ACCESSIBILITY_DRAG &&
                    !(mTouchHelper instanceof WorkspaceAccessibilityHelper)) {
                mTouchHelper = new WorkspaceAccessibilityHelper(this);
            } else if (dragType == FOLDER_ACCESSIBILITY_DRAG &&
                    !(mTouchHelper instanceof FolderAccessibilityHelper)) {
                mTouchHelper = new FolderAccessibilityHelper(this);
            }
            ViewCompat.setAccessibilityDelegate(this, mTouchHelper);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            getShortcutsAndWidgets().setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            setOnClickListener(mTouchHelper);
        }

        // Invalidate the accessibility hierarchy
        if (getParent() != null) {
            getParent().notifySubtreeAccessibilityStateChanged(
                    this, this, AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE);
        }
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        // Always attempt to dispatch hover events to accessibility first.
        if (mUseTouchHelper && mTouchHelper.dispatchHoverEvent(event)) {
            return true;
        }
        return super.dispatchHoverEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mUseTouchHelper;// || (mInterceptTouchListener != null && mInterceptTouchListener.onTouch(this, ev));
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled = super.onTouchEvent(ev);
        // Stylus button press on a home screen should not switch between overview mode and
        // the home screen mode, however, once in overview mode stylus button press should be
        // enabled to allow rearranging the different home screens. So check what mode
        // the workspace is in, and only perform stylus button presses while in overview mode.
//        if (mLauncher.mWorkspace.isInOverviewMode()
//                && mStylusEventHelper.checkAndPerformStylusEvent(ev)) {
//            return true;
//        }
        return handled;
    }

    public void enableHardwareLayer(boolean hasLayer) {
        mShortcutsAndWidgets.setLayerType(hasLayer ? LAYER_TYPE_HARDWARE : LAYER_TYPE_NONE, sPaint);
    }

    public void buildHardwareLayer() {
        mShortcutsAndWidgets.buildLayer();
    }

    public float getChildrenScale() {
        return mIsHotseat ? mHotseatScale : 1.0f;
    }

    public void setCellDimensions(int width, int height) {
        mFixedCellWidth = mCellWidth = width;
        mFixedCellHeight = mCellHeight = height;
        mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap, mHeightGap,
                mCountX, mCountY);
    }

    public void setGridSize(int x, int y) {
        mCountX = x;
        mCountY = y;
        mOccupied = new boolean[mCountX][mCountY];
        mTmpOccupied = new boolean[mCountX][mCountY];
        mTempRectStack.clear();
        mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap, mHeightGap,
                mCountX, mCountY);
        requestLayout();
    }

    // Set whether or not to invert the layout horizontally if the layout is in RTL mode.
    public void setInvertIfRtl(boolean invert) {
        mShortcutsAndWidgets.setInvertIfRtl(invert);
    }

    public void setDropPending(boolean pending) {
        mDropPending = pending;
    }

    public boolean isDropPending() {
        return mDropPending;
    }

    @Override
    public void setPressedIcon(BubbleTextView icon, Bitmap background) {
        if (icon == null || background == null) {
            mTouchFeedbackView.setBitmap(null);
            mTouchFeedbackView.animate().cancel();
        } else {
            if (mTouchFeedbackView.setBitmap(background)) {
                mTouchFeedbackView.alignWithIconView(icon, mShortcutsAndWidgets);
                mTouchFeedbackView.animateShadow();
            }
        }
    }

    void disableDragTarget() {
        mIsDragTarget = false;
    }

    boolean isDragTarget() {
        return mIsDragTarget;
    }

    void setIsDragOverlapping(boolean isDragOverlapping) {
        if (mIsDragOverlapping != isDragOverlapping) {
            mIsDragOverlapping = isDragOverlapping;
            if (mIsDragOverlapping) {
                mBackground.startTransition(BACKGROUND_ACTIVATE_DURATION);
            } else {
                if (mBackgroundAlpha > 0f) {
                    mBackground.reverseTransition(BACKGROUND_ACTIVATE_DURATION);
                } else {
                    mBackground.resetTransition();
                }
            }
            invalidate();
        }
    }

    boolean getIsDragOverlapping() {
        return mIsDragOverlapping;
    }


    public void transiteBackground(boolean trans) {
        if (trans) {
            mBackground.startTransition(BACKGROUND_ACTIVATE_DURATION);
        } else {
            mBackground.reverseTransition(BACKGROUND_ACTIVATE_DURATION);
        }
    }

    private void drawPlusMark(Canvas canvas) {
        Drawable drawable = mLauncher.getResources().getDrawable(R.drawable.screen_add_icon);
        if (drawable instanceof BitmapDrawable) {
            int canvasWidth = canvas.getWidth();
            int canvasHeight = canvas.getHeight();
            Rect drawRect = new Rect((canvasWidth)/2-100, (canvasHeight)/2-100, (canvasWidth)/2+100, (canvasHeight)/2+100);

            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            canvas.drawBitmap(bitmap, null, drawRect, null);
        }
    }

    private void drawRemoveMark(Canvas canvas) {
        if (mLauncher.getWorkspace().isInOverviewMode() == false)
            return;

        Drawable drawable = mLauncher.getResources().getDrawable(R.drawable.screen_remove_icon);
        if (drawable instanceof BitmapDrawable) {
            int canvasWidth = canvas.getWidth();
            int canvasHeight = canvas.getHeight();
            Rect drawRect = new Rect((canvasWidth)/2-100, (canvasHeight)/2-100, (canvasWidth)/2+100, (canvasHeight)/2+100);

            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            canvas.drawBitmap(bitmap, null, drawRect, null);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (!mIsDragTarget) {
            return;
        }

        // When we're large, we are either drawn in a "hover" state (ie when dragging an item to
        // a neighboring page) or with just a normal background (if backgroundAlpha > 0.0f)
        // When we're small, we are either drawn normally or in the "accepts drops" state (during
        // a drag). However, we also drag the mini hover background *over* one of those two
        // backgrounds
        // Dock (hotseat) dùng khung glass riêng (dock_glass_bg trong Hotseat) ôm sát
        // hàng icon, nên KHÔNG vẽ panel nền mặc định của CellLayout ở hotseat để
        // tránh hiện 2 background (1 rộng cách đều mép + 1 ôm app).
        if (mBackgroundAlpha > 0.0f && !mIsHotseat) {
            mBackground.draw(canvas);
        }

//        if (mIsNullScreen) {
//            drawPlusMark(canvas);
//        } else if (getAppChildCount() <= 0) {
//            drawRemoveMark(canvas);
//        }

        final Paint paint = mDragOutlinePaint;
        for (int i = 0; i < mDragOutlines.length; i++) {
            final float alpha = mDragOutlineAlphas[i];
            if (alpha > 0) {
                final Rect r = mDragOutlines[i];
                mTempRect.set(r);
                Utilities.scaleRectAboutCenter(mTempRect, getChildrenScale());
                final Bitmap b = (Bitmap) mDragOutlineAnims[i].getTag();
                paint.setAlpha((int) (alpha + .5f));
                canvas.drawBitmap(b, null, mTempRect, paint);
            }
        }

        if (DEBUG_VISUALIZE_OCCUPIED) {
            int[] pt = new int[2];
            ColorDrawable cd = new ColorDrawable(Color.RED);
            cd.setBounds(0, 0, mCellWidth, mCellHeight);
            for (int i = 0; i < mCountX; i++) {
                for (int j = 0; j < mCountY; j++) {
                    if (mOccupied[i][j]) {
                        cellToPoint(i, j, pt);
                        canvas.save();
                        canvas.translate(pt[0], pt[1]);
                        cd.draw(canvas);
                        canvas.restore();
                    }
                }
            }
        }

        int previewOffset = FolderRingAnimator.sPreviewSize;

        // The folder outer / inner ring image(s)
        DeviceProfile grid = mLauncher.getDeviceProfile();
        for (int i = 0; i < mFolderOuterRings.size(); i++) {
            FolderRingAnimator fra = mFolderOuterRings.get(i);

            Drawable d;
            int width, height;
            cellToPoint(fra.mCellX, fra.mCellY, mTempLocation);
            View child = getChildAt(fra.mCellX, fra.mCellY);

            if (child != null) {
                int centerX = mTempLocation[0] + mCellWidth / 2;
                int centerY = mTempLocation[1] + previewOffset / 2 +
                        child.getPaddingTop() + grid.folderBackgroundOffset;

                // Draw outer ring, if it exists
                if (FolderIcon.HAS_OUTER_RING) {
                    d = FolderRingAnimator.sSharedOuterRingDrawable;
                    width = (int) (fra.getOuterRingSize() * getChildrenScale());
                    height = width;
                    canvas.save();
                    canvas.translate(centerX - width / 2, centerY - height / 2);
                    d.setBounds(0, 0, width, height);
                    d.draw(canvas);
                    canvas.restore();
                }
            }
        }
    }

    public void showFolderAccept(FolderRingAnimator fra) {
        mFolderOuterRings.add(fra);
    }

    public void hideFolderAccept(FolderRingAnimator fra) {
        if (mFolderOuterRings.contains(fra)) {
            mFolderOuterRings.remove(fra);
        }
        invalidate();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public void restoreInstanceState(SparseArray<Parcelable> states) {
        try {
            dispatchRestoreInstanceState(states);
        } catch (Exception ex) {
            if (LauncherAppState.isDogfoodBuild()) {
                throw ex;
            }
            // Mismatched viewId / viewType preventing restore. Skip restore on production builds.
            Log.e(TAG, "Ignoring an error while restoring a view instance state", ex);
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        // Cancel long press for all children
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.cancelLongPress();
        }
    }

    public void setOnInterceptTouchListener(View.OnTouchListener listener) {
        mInterceptTouchListener = listener;
    }

    public int getCountX() {
        return mCountX;
    }

    public int getCountY() {
        return mCountY;
    }

    public void setIsHotseat(boolean isHotseat) {

        mIsHotseat = isHotseat;
        mShortcutsAndWidgets.setIsHotseat(isHotseat);
    }

    public boolean isHotseat() {
        return mIsHotseat;
    }

    public void setIsFolder(boolean isFolder) {
        mIsFolder = isFolder;
    }

    public boolean isFolder() {
        return mIsFolder;
    }

    public void setIsFull(boolean isFull) {
        mIsFull = isFull;
    }

    public boolean isFull() {
        return mIsFull;
    }

    public boolean addViewToCellLayout(View child, int index, int childId, LayoutParams params,
                                       boolean markCells) {
        final LayoutParams lp = params;

        // Hotseat icons - remove text
        if (child instanceof BubbleTextView) {
            BubbleTextView bubbleChild = (BubbleTextView) child;
            if (mIsHotseat) {
                boolean showText = Settings.isHotseatAppNameVisibility(mLauncher);
//                bubbleChild.setTextVisibility(showText);
                bubbleChild.setTextVisibility(false);
            } else {
                bubbleChild.setTextVisibility(true);
            }
        }

        child.setScaleX(getChildrenScale());
        child.setScaleY(getChildrenScale());

        // Generate an id for each view, this assumes we have at most 256x256 cells
        // per workspace screen
        if (lp.cellX >= 0 && lp.cellX <= mCountX - 1 && lp.cellY >= 0 && lp.cellY <= mCountY - 1) {
            // If the horizontal or vertical span is set to -1, it is taken to
            // mean that it spans the extent of the CellLayout
            if (lp.cellHSpan < 0) lp.cellHSpan = mCountX;
            if (lp.cellVSpan < 0) lp.cellVSpan = mCountY;

            child.setId(childId);

            try {
                mShortcutsAndWidgets.addView(child, index, lp);
            } catch (Exception e) {
                Log.e(TAG, "addViewToCellLayout fail", e);
                if (child instanceof LauncherAppWidgetHostView) {
                    LauncherAppWidgetHostView hostView = (LauncherAppWidgetHostView) child;
                    hostView.addView(hostView.getErrorView());
                }
            }

            if (markCells) markCellsAsOccupiedForView(child);

            return true;
        }
        return false;
    }

    @Override
    public void removeAllViews() {
        clearOccupiedCells();
        mShortcutsAndWidgets.removeAllViews();
    }

    @Override
    public void removeAllViewsInLayout() {
        if (mShortcutsAndWidgets.getChildCount() > 0) {
            clearOccupiedCells();
            mShortcutsAndWidgets.removeAllViewsInLayout();
        }
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);

        markCellsAsUnoccupiedForView(view);
        try {
            mShortcutsAndWidgets.removeView(view);
        } catch (Exception e) {
            Log.e(TAG, "removeView fail", e);
        }

        reSetDrawDel();
        updatePageListView();
    }

    @Override
    public void removeViewAt(int index) {
        markCellsAsUnoccupiedForView(mShortcutsAndWidgets.getChildAt(index));
        mShortcutsAndWidgets.removeViewAt(index);
    }

    @Override
    public void removeViewInLayout(View view) {
        markCellsAsUnoccupiedForView(view);
        mShortcutsAndWidgets.removeViewInLayout(view);
    }

    @Override
    public void removeViews(int start, int count) {
        for (int i = start; i < start + count; i++) {
            markCellsAsUnoccupiedForView(mShortcutsAndWidgets.getChildAt(i));
        }
        mShortcutsAndWidgets.removeViews(start, count);
    }

    public View getShortcutViewAt(int index) {
        return mShortcutsAndWidgets.getChildAt(index);
    }


    @Override
    public void removeViewsInLayout(int start, int count) {
        for (int i = start; i < start + count; i++) {
            markCellsAsUnoccupiedForView(mShortcutsAndWidgets.getChildAt(i));
        }
        mShortcutsAndWidgets.removeViewsInLayout(start, count);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int childWidthSize = widthSize - (getPaddingLeft() + getPaddingRight());
        int childHeightSize = heightSize - (getPaddingTop() + getPaddingBottom());

        if (isFolder()) {
            int cw = DeviceProfile.calculateCellWidth(childWidthSize, mCountX);
            int ch = mLauncher.getDeviceProfile().caculateFolderCellHeight();
            if (cw != mCellWidth || ch != mCellHeight) {
                mCellWidth = cw;
                mCellHeight = ch;
                mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap,
                        mHeightGap, mCountX, mCountY);
            }
        } else if (mFixedCellWidth < 0 || mFixedCellHeight < 0) {
            int cw = DeviceProfile.calculateCellWidth(childWidthSize, mCountX);
            int ch = DeviceProfile.calculateCellHeight(childHeightSize, mCountY);
            if (cw != mCellWidth || ch != mCellHeight) {
                mCellWidth = cw;
                mCellHeight = ch;
                mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap,
                        mHeightGap, mCountX, mCountY);
            }
        }

        int newWidth = childWidthSize;
        int newHeight = childHeightSize;
        if (isFolder()) {
            newHeight = (mCountY * mCellHeight) + ((mCountY - 1) * mHeightGap);
        } else if (mFixedWidth > 0 && mFixedHeight > 0) {
            newWidth = mFixedWidth;
            newHeight = mFixedHeight;
        } else if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }

        int numWidthGaps = mCountX - 1;
        int numHeightGaps = mCountY - 1;

        if (mOriginalWidthGap < 0 || mOriginalHeightGap < 0) {
            int hSpace = childWidthSize;
            int vSpace = childHeightSize;
            int hFreeSpace = hSpace - (mCountX * mCellWidth);
            int vFreeSpace = vSpace - (mCountY * mCellHeight);
            mWidthGap = Math.min(mMaxGap, numWidthGaps > 0 ? (hFreeSpace / numWidthGaps) : 0);
            mHeightGap = Math.min(mMaxGap, numHeightGaps > 0 ? (vFreeSpace / numHeightGaps) : 0);
            mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap,
                    mHeightGap, mCountX, mCountY);
        } else {
            mWidthGap = mOriginalWidthGap;
            mHeightGap = mOriginalHeightGap;
        }

        // Make the feedback view large enough to hold the blur bitmap.
        mTouchFeedbackView.measure(
                MeasureSpec.makeMeasureSpec(mCellWidth + mTouchFeedbackView.getExtraSize(),
                        MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mCellHeight + mTouchFeedbackView.getExtraSize(),
                        MeasureSpec.EXACTLY));

        mShortcutsAndWidgets.measure(
                MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY));

        int maxWidth = mShortcutsAndWidgets.getMeasuredWidth();
        int maxHeight = mShortcutsAndWidgets.getMeasuredHeight();
        if (isFolder() && isFull()) {
            heightSize = newHeight + getPaddingTop() + getPaddingBottom();
        }
        if(heightSize == 0){
            heightSize = newHeight;
        }
        if (mFixedWidth > 0 && mFixedHeight > 0) {
            setMeasuredDimension(maxWidth, maxHeight);
        } else {
            setMeasuredDimension(widthSize, heightSize);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int offset = getMeasuredWidth() - getPaddingLeft() - getPaddingRight() -
                (mCountX * mCellWidth);
        int left = getPaddingLeft() + (int) Math.ceil(offset / 2f);
        int top = getPaddingTop();

        mTouchFeedbackView.layout(left, top,
                left + mTouchFeedbackView.getMeasuredWidth(),
                top + mTouchFeedbackView.getMeasuredHeight());
        mShortcutsAndWidgets.layout(left, top,
                left + r - l,
                top + b - t);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Expand the background drawing bounds by the padding baked into the background drawable
        mBackground.getPadding(mTempRect);
        mBackground.setBounds(-mTempRect.left, -mTempRect.top,
                w + mTempRect.right, h + mTempRect.bottom);
    }

    @Override
    protected void setChildrenDrawingCacheEnabled(boolean enabled) {
        mShortcutsAndWidgets.setChildrenDrawingCacheEnabled(enabled);
    }

    @Override
    protected void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        mShortcutsAndWidgets.setChildrenDrawnWithCacheEnabled(enabled);
    }

    public float getBackgroundAlpha() {
        return mBackgroundAlpha;
    }

    public void setBackgroundAlpha(float alpha) {
        if (mBackgroundAlpha != alpha) {
            mBackgroundAlpha = alpha;
            mBackground.setAlpha((int) (mBackgroundAlpha * 255));
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || (mIsDragTarget && who == mBackground);
    }

    public void setShortcutAndWidgetAlpha(float alpha) {
        mShortcutsAndWidgets.setAlpha(alpha);
    }

    public ShortcutAndWidgetContainer getShortcutsAndWidgets() {
        return mShortcutsAndWidgets;
    }

    public boolean isEmpty() {
        return mShortcutsAndWidgets.getChildCount() == 0;
    }

    public View getChildAt(int x, int y) {
        return mShortcutsAndWidgets.getChildAt(x, y);
    }

    public int getAppChildCount() {
        return mShortcutsAndWidgets.getChildCount();
    }

    public boolean animateChildToPosition(final View child, int cellX, int cellY, int duration,
                                          int delay, boolean permanent, boolean adjustOccupied) {
        ShortcutAndWidgetContainer clc = getShortcutsAndWidgets();
        boolean[][] occupied = mOccupied;
        if (!permanent) {
            occupied = mTmpOccupied;
        }

        if (clc.indexOfChild(child) != -1) {
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final ItemInfo info = (ItemInfo) child.getTag();

            // We cancel any existing animations
            if (mReorderAnimators.containsKey(lp)) {
                mReorderAnimators.get(lp).cancel();
                mReorderAnimators.remove(lp);
            }

            final int oldX = lp.x;
            final int oldY = lp.y;
            if (adjustOccupied) {
                occupied[lp.cellX][lp.cellY] = false;
                occupied[cellX][cellY] = true;
            }
            lp.isLockedToGrid = true;
            if (permanent) {
                lp.cellX = cellX;
                lp.cellY = cellY;
                if (info != null) {
                    info.cellX = cellX;
                    info.cellY = cellY;
                    info.requiresDbUpdate = true;
                }
            } else {
                lp.tmpCellX = cellX;
                lp.tmpCellY = cellY;
            }
            clc.setupLp(lp);
            lp.isLockedToGrid = false;
            final int newX = lp.x;
            final int newY = lp.y;

            lp.x = oldX;
            lp.y = oldY;

            // Exit early if we're not actually moving the view
            if (oldX == newX && oldY == newY) {
                lp.isLockedToGrid = true;
                return true;
            }

            ValueAnimator va = LauncherAnimUtils.ofFloat(child, 0f, 1f);
            va.setDuration(duration);
            mReorderAnimators.put(lp, va);

            va.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float r = ((Float) animation.getAnimatedValue()).floatValue();
                    lp.x = (int) ((1 - r) * oldX + r * newX);
                    lp.y = (int) ((1 - r) * oldY + r * newY);
                    child.requestLayout();
                }
            });
            va.addListener(new AnimatorListenerAdapter() {
                boolean cancelled = false;

                public void onAnimationEnd(Animator animation) {
                    // If the animation was cancelled, it means that another animation
                    // has interrupted this one, and we don't want to lock the item into
                    // place just yet.
                    if (!cancelled) {
                        lp.isLockedToGrid = true;
                        child.requestLayout();
                    }
                    if (mReorderAnimators.containsKey(lp)) {
                        mReorderAnimators.remove(lp);
                    }
                }

                public void onAnimationCancel(Animator animation) {
                    cancelled = true;
                }
            });
            va.setStartDelay(delay);
            va.start();
            return true;
        }
        return false;
    }

    public void visualizeDropLocation(View v, Bitmap dragOutline, int originX, int originY, int cellX,
                               int cellY, int spanX, int spanY, boolean resize, Point dragOffset, Rect dragRegion) {
        final int oldDragCellX = mDragCell[0];
        final int oldDragCellY = mDragCell[1];


        if (dragOutline == null && v == null) {
            return;
        }

        if (cellX != oldDragCellX || cellY != oldDragCellY) {
            mDragCell[0] = cellX;
            mDragCell[1] = cellY;
            // Find the top left corner of the rect the object will occupy
            final int[] topLeft = mTmpPoint;
            cellToPoint(cellX, cellY, topLeft);

            int left = topLeft[0];
            int top = topLeft[1];

            if (v != null && dragOffset == null) {
                // When drawing the drag outline, it did not account for margin offsets
                // added by the view's parent.
                MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
                left += lp.leftMargin;
                top += lp.topMargin;

                // Offsets due to the size difference between the View and the dragOutline.
                // There is a size difference to account for the outer blur, which may lie
                // outside the bounds of the view.
                top += (v.getHeight() - dragOutline.getHeight()) / 2;
                // We center about the x axis
                left += ((mCellWidth * spanX) + ((spanX - 1) * mWidthGap)
                        - dragOutline.getWidth()) / 2;
            } else {
                if (dragOffset != null && dragRegion != null) {
                    // Center the drag region *horizontally* in the cell and apply a drag
                    // outline offset
                    left += dragOffset.x + ((mCellWidth * spanX) + ((spanX - 1) * mWidthGap)
                            - dragRegion.width()) / 2;
                    int cHeight = getShortcutsAndWidgets().getCellContentHeight();
                    int cellPaddingY = (int) Math.max(0, ((mCellHeight - cHeight) / 2f));
                    top += dragOffset.y + cellPaddingY;
                } else {
                    // Center the drag outline in the cell
                    left += ((mCellWidth * spanX) + ((spanX - 1) * mWidthGap)
                            - dragOutline.getWidth()) / 2;
                    top += ((mCellHeight * spanY) + ((spanY - 1) * mHeightGap)
                            - dragOutline.getHeight()) / 2;
                }
            }
            final int oldIndex = mDragOutlineCurrent;
            mDragOutlineAnims[oldIndex].animateOut();
            mDragOutlineCurrent = (oldIndex + 1) % mDragOutlines.length;
            Rect r = mDragOutlines[mDragOutlineCurrent];
            r.set(left, top, left + dragOutline.getWidth(), top + dragOutline.getHeight());
            if (resize) {
                cellToRect(cellX, cellY, spanX, spanY, r);
            }

            mDragOutlineAnims[mDragOutlineCurrent].setTag(dragOutline);
            mDragOutlineAnims[mDragOutlineCurrent].animateIn();
        }
    }

    public void clearDragOutlines() {
        final int oldIndex = mDragOutlineCurrent;
        mDragOutlineAnims[oldIndex].animateOut();
        mDragCell[0] = mDragCell[1] = -1;
    }

    public int [] getFirstVacant(int spanX, int spanY) {
        int[] result = new int[2];

        if (spanX > 1 || spanY > 1) {
            cellToPoint(0, 0, result);
            return result;
        }

        for (int y = 0; y < mCountY; y++) {
            for (int x = 0; x < mCountX; x++) {
                if (!mOccupied[x][y]) {
                    cellToPoint(x, y, result);
                    return result;
                }
            }
        }

        cellToPoint(0, 0, result);
        return result;
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param spanX  Horizontal span of the object.
     * @param spanY  Vertical span of the object.
     * @param result Array in which to place the result, or null (in which case a new array will
     *               be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     * nearest the requested location.
     */
    int[] findNearestVacantArea(int pixelX, int pixelY, int spanX, int spanY, int[] result) {
        return findNearestVacantArea(pixelX, pixelY, spanX, spanY, spanX, spanY, result, null);
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX   The X location at which you want to search for a vacant area.
     * @param pixelY   The Y location at which you want to search for a vacant area.
     * @param minSpanX The minimum horizontal span required
     * @param minSpanY The minimum vertical span required
     * @param spanX    Horizontal span of the object.
     * @param spanY    Vertical span of the object.
     * @param result   Array in which to place the result, or null (in which case a new array will
     *                 be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     * nearest the requested location.
     */
    int[] findNearestVacantArea(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX,
                                int spanY, int[] result, int[] resultSpan) {
        return findNearestArea(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, true,
                result, resultSpan);
    }

    private final Stack<Rect> mTempRectStack = new Stack<Rect>();

    private void lazyInitTempRectStack() {
        if (mTempRectStack.isEmpty()) {
            for (int i = 0; i < mCountX * mCountY; i++) {
                mTempRectStack.push(new Rect());
            }
        }
    }

    private void recycleTempRects(Stack<Rect> used) {
        while (!used.isEmpty()) {
            mTempRectStack.push(used.pop());
        }
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX         The X location at which you want to search for a vacant area.
     * @param pixelY         The Y location at which you want to search for a vacant area.
     * @param minSpanX       The minimum horizontal span required
     * @param minSpanY       The minimum vertical span required
     * @param spanX          Horizontal span of the object.
     * @param spanY          Vertical span of the object.
     * @param ignoreOccupied If true, the result can be an occupied cell
     * @param result         Array in which to place the result, or null (in which case a new array will
     *                       be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     * nearest the requested location.
     */
    private int[] findNearestArea(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX,
                                  int spanY, boolean ignoreOccupied, int[] result, int[] resultSpan) {
        lazyInitTempRectStack();

        // For items with a spanX / spanY > 1, the passed in point (pixelX, pixelY) corresponds
        // to the center of the item, but we are searching based on the top-left cell, so
        // we translate the point over to correspond to the top-left.
        pixelX -= (mCellWidth + mWidthGap) * (spanX - 1) / 2f;
        pixelY -= (mCellHeight + mHeightGap) * (spanY - 1) / 2f;

        // Keep track of best-scoring drop area
        final int[] bestXY = result != null ? result : new int[2];
        double bestDistance = Double.MAX_VALUE;
        final Rect bestRect = new Rect(-1, -1, -1, -1);
        final Stack<Rect> validRegions = new Stack<Rect>();

        final int countX = mCountX;
        final int countY = mCountY;

        if (minSpanX <= 0 || minSpanY <= 0 || spanX <= 0 || spanY <= 0 ||
                spanX < minSpanX || spanY < minSpanY) {
            return bestXY;
        }

        for (int y = 0; y < countY - (minSpanY - 1); y++) {
            inner:
            for (int x = 0; x < countX - (minSpanX - 1); x++) {
                int ySize = -1;
                int xSize = -1;
                if (ignoreOccupied) {
                    // First, let's see if this thing fits anywhere
                    for (int i = 0; i < minSpanX; i++) {
                        for (int j = 0; j < minSpanY; j++) {
                            if (mOccupied[x + i][y + j]) {
                                continue inner;
                            }
                        }
                    }
                    xSize = minSpanX;
                    ySize = minSpanY;

                    // We know that the item will fit at _some_ acceptable size, now let's see
                    // how big we can make it. We'll alternate between incrementing x and y spans
                    // until we hit a limit.
                    boolean incX = true;
                    boolean hitMaxX = xSize >= spanX;
                    boolean hitMaxY = ySize >= spanY;
                    while (!(hitMaxX && hitMaxY)) {
                        if (incX && !hitMaxX) {
                            for (int j = 0; j < ySize; j++) {
                                if (x + xSize > countX - 1 || mOccupied[x + xSize][y + j]) {
                                    // We can't move out horizontally
                                    hitMaxX = true;
                                }
                            }
                            if (!hitMaxX) {
                                xSize++;
                            }
                        } else if (!hitMaxY) {
                            for (int i = 0; i < xSize; i++) {
                                if (y + ySize > countY - 1 || mOccupied[x + i][y + ySize]) {
                                    // We can't move out vertically
                                    hitMaxY = true;
                                }
                            }
                            if (!hitMaxY) {
                                ySize++;
                            }
                        }
                        hitMaxX |= xSize >= spanX;
                        hitMaxY |= ySize >= spanY;
                        incX = !incX;
                    }
                    incX = true;
                    hitMaxX = xSize >= spanX;
                    hitMaxY = ySize >= spanY;
                }
                final int[] cellXY = mTmpPoint;
                cellToCenterPoint(x, y, cellXY);

                // We verify that the current rect is not a sub-rect of any of our previous
                // candidates. In this case, the current rect is disqualified in favour of the
                // containing rect.
                Rect currentRect = mTempRectStack.pop();
                currentRect.set(x, y, x + xSize, y + ySize);
                boolean contained = false;
                for (Rect r : validRegions) {
                    if (r.contains(currentRect)) {
                        contained = true;
                        break;
                    }
                }
                validRegions.push(currentRect);
                double distance = Math.hypot(cellXY[0] - pixelX, cellXY[1] - pixelY);

                if ((distance <= bestDistance && !contained) ||
                        currentRect.contains(bestRect)) {
                    bestDistance = distance;
                    bestXY[0] = x;
                    bestXY[1] = y;
                    if (resultSpan != null) {
                        resultSpan[0] = xSize;
                        resultSpan[1] = ySize;
                    }
                    bestRect.set(currentRect);
                }
            }
        }

        // Return -1, -1 if no suitable location found
        if (bestDistance == Double.MAX_VALUE) {
            bestXY[0] = -1;
            bestXY[1] = -1;
        }
        recycleTempRects(validRegions);
        return bestXY;
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location, and will also weigh in a suggested direction vector of the
     * desired location. This method computers distance based on unit grid distances,
     * not pixel distances.
     *
     * @param cellX              The X cell nearest to which you want to search for a vacant area.
     * @param cellY              The Y cell nearest which you want to search for a vacant area.
     * @param spanX              Horizontal span of the object.
     * @param spanY              Vertical span of the object.
     * @param direction          The favored direction in which the views should move from x, y
     * @param blockOccupied      The array which represents which cells in the specified block (cellX,
     *                           cellY, spanX, spanY) are occupied. This is used when try to move a group of views.
     * @param result             Array in which to place the result, or null (in which case a new array will
     *                           be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     * nearest the requested location.
     */
    private int[] findNearestArea(int cellX, int cellY, int spanX, int spanY, int[] direction,
                                  boolean[][] occupied, boolean blockOccupied[][], int[] result) {
        // Keep track of best-scoring drop area
        final int[] bestXY = result != null ? result : new int[2];
        float bestDistance = Float.MAX_VALUE;
        int bestDirectionScore = Integer.MIN_VALUE;

        final int countX = mCountX;
        final int countY = mCountY;

        for (int y = 0; y < countY - (spanY - 1); y++) {
            inner:
            for (int x = 0; x < countX - (spanX - 1); x++) {
                // First, let's see if this thing fits anywhere
                for (int i = 0; i < spanX; i++) {
                    for (int j = 0; j < spanY; j++) {
                        if (occupied[x + i][y + j] && (blockOccupied == null || blockOccupied[i][j])) {
                            continue inner;
                        }
                    }
                }

                float distance = (float) Math.hypot(x - cellX, y - cellY);
                int[] curDirection = mTmpPoint;
                computeDirectionVector(x - cellX, y - cellY, curDirection);
                // The direction score is just the dot product of the two candidate direction
                // and that passed in.
                int curDirectionScore = direction[0] * curDirection[0] +
                        direction[1] * curDirection[1];
                boolean exactDirectionOnly = false;
                boolean directionMatches = direction[0] == curDirection[0] &&
                        direction[0] == curDirection[0];
                if ((directionMatches || !exactDirectionOnly) &&
                        Float.compare(distance, bestDistance) < 0 || (Float.compare(distance,
                        bestDistance) == 0 && curDirectionScore > bestDirectionScore)) {
                    bestDistance = distance;
                    bestDirectionScore = curDirectionScore;
                    bestXY[0] = x;
                    bestXY[1] = y;
                }
            }
        }

        // Return -1, -1 if no suitable location found
        if (bestDistance == Float.MAX_VALUE) {
            bestXY[0] = -1;
            bestXY[1] = -1;
        }
        return bestXY;
    }

    private boolean addViewToTempLocation(View v, Rect rectOccupiedByPotentialDrop,
                                          int[] direction, ItemConfiguration currentState) {
        CellAndSpan c = currentState.map.get(v);
        boolean success = false;
        markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
        markCellsForRect(rectOccupiedByPotentialDrop, mTmpOccupied, true);

        findNearestArea(c.x, c.y, c.spanX, c.spanY, direction, mTmpOccupied, null, mTempLocation);

        if (mTempLocation[0] >= 0 && mTempLocation[1] >= 0) {
            c.x = mTempLocation[0];
            c.y = mTempLocation[1];
            success = true;
        }
        markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
        return success;
    }

    /**
     * This helper class defines a cluster of views. It helps with defining complex edges
     * of the cluster and determining how those edges interact with other views. The edges
     * essentially define a fine-grained boundary around the cluster of views -- like a more
     * precise version of a bounding box.
     */
    private class ViewCluster {
        final static int LEFT = 0;
        final static int TOP = 1;
        final static int RIGHT = 2;
        final static int BOTTOM = 3;

        ArrayList<View> views;
        ItemConfiguration config;
        Rect boundingRect = new Rect();

        int[] leftEdge = new int[mCountY];
        int[] rightEdge = new int[mCountY];
        int[] topEdge = new int[mCountX];
        int[] bottomEdge = new int[mCountX];
        boolean leftEdgeDirty, rightEdgeDirty, topEdgeDirty, bottomEdgeDirty, boundingRectDirty;

        @SuppressWarnings("unchecked")
        public ViewCluster(ArrayList<View> views, ItemConfiguration config) {
            this.views = (ArrayList<View>) views.clone();
            this.config = config;
            resetEdges();
        }

        void resetEdges() {
            for (int i = 0; i < mCountX; i++) {
                topEdge[i] = -1;
                bottomEdge[i] = -1;
            }
            for (int i = 0; i < mCountY; i++) {
                leftEdge[i] = -1;
                rightEdge[i] = -1;
            }
            leftEdgeDirty = true;
            rightEdgeDirty = true;
            bottomEdgeDirty = true;
            topEdgeDirty = true;
            boundingRectDirty = true;
        }

        void computeEdge(int which, int[] edge) {
            int count = views.size();
            for (int i = 0; i < count; i++) {
                CellAndSpan cs = config.map.get(views.get(i));
                switch (which) {
                    case LEFT:
                        int left = cs.x;
                        for (int j = cs.y; j < cs.y + cs.spanY; j++) {
                            if (left < edge[j] || edge[j] < 0) {
                                edge[j] = left;
                            }
                        }
                        break;
                    case RIGHT:
                        int right = cs.x + cs.spanX;
                        for (int j = cs.y; j < cs.y + cs.spanY; j++) {
                            if (right > edge[j]) {
                                edge[j] = right;
                            }
                        }
                        break;
                    case TOP:
                        int top = cs.y;
                        for (int j = cs.x; j < cs.x + cs.spanX; j++) {
                            if (top < edge[j] || edge[j] < 0) {
                                edge[j] = top;
                            }
                        }
                        break;
                    case BOTTOM:
                        int bottom = cs.y + cs.spanY;
                        for (int j = cs.x; j < cs.x + cs.spanX; j++) {
                            if (bottom > edge[j]) {
                                edge[j] = bottom;
                            }
                        }
                        break;
                }
            }
        }

        boolean isViewTouchingEdge(View v, int whichEdge) {
            CellAndSpan cs = config.map.get(v);

            int[] edge = getEdge(whichEdge);

            switch (whichEdge) {
                case LEFT:
                    for (int i = cs.y; i < cs.y + cs.spanY; i++) {
                        if (edge[i] == cs.x + cs.spanX) {
                            return true;
                        }
                    }
                    break;
                case RIGHT:
                    for (int i = cs.y; i < cs.y + cs.spanY; i++) {
                        if (edge[i] == cs.x) {
                            return true;
                        }
                    }
                    break;
                case TOP:
                    for (int i = cs.x; i < cs.x + cs.spanX; i++) {
                        if (edge[i] == cs.y + cs.spanY) {
                            return true;
                        }
                    }
                    break;
                case BOTTOM:
                    for (int i = cs.x; i < cs.x + cs.spanX; i++) {
                        if (edge[i] == cs.y) {
                            return true;
                        }
                    }
                    break;
            }
            return false;
        }

        void shift(int whichEdge, int delta) {
            for (View v : views) {
                CellAndSpan c = config.map.get(v);
                switch (whichEdge) {
                    case LEFT:
                        c.x -= delta;
                        break;
                    case RIGHT:
                        c.x += delta;
                        break;
                    case TOP:
                        c.y -= delta;
                        break;
                    case BOTTOM:
                    default:
                        c.y += delta;
                        break;
                }
            }
            resetEdges();
        }

        public void addView(View v) {
            views.add(v);
            resetEdges();
        }

        public Rect getBoundingRect() {
            if (boundingRectDirty) {
                boolean first = true;
                for (View v : views) {
                    CellAndSpan c = config.map.get(v);
                    if (first) {
                        boundingRect.set(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
                        first = false;
                    } else {
                        boundingRect.union(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
                    }
                }
            }
            return boundingRect;
        }

        public int[] getEdge(int which) {
            switch (which) {
                case LEFT:
                    return getLeftEdge();
                case RIGHT:
                    return getRightEdge();
                case TOP:
                    return getTopEdge();
                case BOTTOM:
                default:
                    return getBottomEdge();
            }
        }

        public int[] getLeftEdge() {
            if (leftEdgeDirty) {
                computeEdge(LEFT, leftEdge);
            }
            return leftEdge;
        }

        public int[] getRightEdge() {
            if (rightEdgeDirty) {
                computeEdge(RIGHT, rightEdge);
            }
            return rightEdge;
        }

        public int[] getTopEdge() {
            if (topEdgeDirty) {
                computeEdge(TOP, topEdge);
            }
            return topEdge;
        }

        public int[] getBottomEdge() {
            if (bottomEdgeDirty) {
                computeEdge(BOTTOM, bottomEdge);
            }
            return bottomEdge;
        }

        PositionComparator comparator = new PositionComparator();

        class PositionComparator implements Comparator<View> {
            int whichEdge = 0;

            public int compare(View left, View right) {
                CellAndSpan l = config.map.get(left);
                CellAndSpan r = config.map.get(right);
                switch (whichEdge) {
                    case LEFT:
                        return (r.x + r.spanX) - (l.x + l.spanX);
                    case RIGHT:
                        return l.x - r.x;
                    case TOP:
                        return (r.y + r.spanY) - (l.y + l.spanY);
                    case BOTTOM:
                    default:
                        return l.y - r.y;
                }
            }
        }

        public void sortConfigurationForEdgePush(int edge) {
            comparator.whichEdge = edge;
            Collections.sort(config.sortedViews, comparator);
        }
    }

    private boolean pushViewsToTempLocation(ArrayList<View> views, Rect rectOccupiedByPotentialDrop,
                                            int[] direction, View dragView, ItemConfiguration currentState) {

        ViewCluster cluster = new ViewCluster(views, currentState);
        Rect clusterRect = cluster.getBoundingRect();
        int whichEdge;
        int pushDistance;
        boolean fail = false;

        // Determine the edge of the cluster that will be leading the push and how far
        // the cluster must be shifted.
        if (direction[0] < 0) {
            whichEdge = ViewCluster.LEFT;
            pushDistance = clusterRect.right - rectOccupiedByPotentialDrop.left;
        } else if (direction[0] > 0) {
            whichEdge = ViewCluster.RIGHT;
            pushDistance = rectOccupiedByPotentialDrop.right - clusterRect.left;
        } else if (direction[1] < 0) {
            whichEdge = ViewCluster.TOP;
            pushDistance = clusterRect.bottom - rectOccupiedByPotentialDrop.top;
        } else {
            whichEdge = ViewCluster.BOTTOM;
            pushDistance = rectOccupiedByPotentialDrop.bottom - clusterRect.top;
        }

        // Break early for invalid push distance.
        if (pushDistance <= 0) {
            return false;
        }

        // Mark the occupied state as false for the group of views we want to move.
        for (View v : views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
        }

        // We save the current configuration -- if we fail to find a solution we will revert
        // to the initial state. The process of finding a solution modifies the configuration
        // in place, hence the need for revert in the failure case.
        currentState.save();

        // The pushing algorithm is simplified by considering the views in the order in which
        // they would be pushed by the cluster. For example, if the cluster is leading with its
        // left edge, we consider sort the views by their right edge, from right to left.
        cluster.sortConfigurationForEdgePush(whichEdge);

        while (pushDistance > 0 && !fail) {
            for (View v : currentState.sortedViews) {
                // For each view that isn't in the cluster, we see if the leading edge of the
                // cluster is contacting the edge of that view. If so, we add that view to the
                // cluster.
                if (!cluster.views.contains(v) && v != dragView) {
                    if (cluster.isViewTouchingEdge(v, whichEdge)) {
                        LayoutParams lp = (LayoutParams) v.getLayoutParams();
                        if (!lp.canReorder) {
                            // The push solution includes the all apps button, this is not viable.
                            fail = true;
                            break;
                        }
                        cluster.addView(v);
                        CellAndSpan c = currentState.map.get(v);

                        // Adding view to cluster, mark it as not occupied.
                        markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
                    }
                }
            }
            pushDistance--;

            // The cluster has been completed, now we move the whole thing over in the appropriate
            // direction.
            cluster.shift(whichEdge, 1);
        }

        boolean foundSolution = false;
        clusterRect = cluster.getBoundingRect();

        // Due to the nature of the algorithm, the only check required to verify a valid solution
        // is to ensure that completed shifted cluster lies completely within the cell layout.
        if (!fail && clusterRect.left >= 0 && clusterRect.right <= mCountX && clusterRect.top >= 0 &&
                clusterRect.bottom <= mCountY) {
            foundSolution = true;
        } else {
            currentState.restore();
        }

        // In either case, we set the occupied array as marked for the location of the views
        for (View v : cluster.views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
        }

        return foundSolution;
    }

    private boolean addViewsToTempLocation(ArrayList<View> views, Rect rectOccupiedByPotentialDrop,
                                           int[] direction, View dragView, ItemConfiguration currentState) {
        if (views.size() == 0) return true;

        boolean success = false;
        Rect boundingRect = null;
        // We construct a rect which represents the entire group of views passed in
        for (View v : views) {
            CellAndSpan c = currentState.map.get(v);
            if (boundingRect == null) {
                boundingRect = new Rect(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
            } else {
                boundingRect.union(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
            }
        }

        // Mark the occupied state as false for the group of views we want to move.
        for (View v : views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
        }

        boolean[][] blockOccupied = new boolean[boundingRect.width()][boundingRect.height()];
        int top = boundingRect.top;
        int left = boundingRect.left;
        // We mark more precisely which parts of the bounding rect are truly occupied, allowing
        // for interlocking.
        for (View v : views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x - left, c.y - top, c.spanX, c.spanY, blockOccupied, true);
        }

        markCellsForRect(rectOccupiedByPotentialDrop, mTmpOccupied, true);

        findNearestArea(boundingRect.left, boundingRect.top, boundingRect.width(),
                boundingRect.height(), direction, mTmpOccupied, blockOccupied, mTempLocation);

        // If we successfuly found a location by pushing the block of views, we commit it
        if (mTempLocation[0] >= 0 && mTempLocation[1] >= 0) {
            int deltaX = mTempLocation[0] - boundingRect.left;
            int deltaY = mTempLocation[1] - boundingRect.top;
            for (View v : views) {
                CellAndSpan c = currentState.map.get(v);
                c.x += deltaX;
                c.y += deltaY;
            }
            success = true;
        }

        // In either case, we set the occupied array as marked for the location of the views
        for (View v : views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
        }
        return success;
    }

    private void markCellsForRect(Rect r, boolean[][] occupied, boolean value) {
        markCellsForView(r.left, r.top, r.width(), r.height(), occupied, value);
    }

    // This method tries to find a reordering solution which satisfies the push mechanic by trying
    // to push items in each of the cardinal directions, in an order based on the direction vector
    // passed.
    private boolean attemptPushInDirection(ArrayList<View> intersectingViews, Rect occupied,
                                           int[] direction, View ignoreView, ItemConfiguration solution) {
        if ((Math.abs(direction[0]) + Math.abs(direction[1])) > 1) {
            // If the direction vector has two non-zero components, we try pushing
            // separately in each of the components.
            int temp = direction[1];
            direction[1] = 0;

            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            direction[1] = temp;
            temp = direction[0];
            direction[0] = 0;

            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Revert the direction
            direction[0] = temp;

            // Now we try pushing in each component of the opposite direction
            direction[0] *= -1;
            direction[1] *= -1;
            temp = direction[1];
            direction[1] = 0;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }

            direction[1] = temp;
            temp = direction[0];
            direction[0] = 0;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // revert the direction
            direction[0] = temp;
            direction[0] *= -1;
            direction[1] *= -1;

        } else {
            // If the direction vector has a single non-zero component, we push first in the
            // direction of the vector
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Then we try the opposite direction
            direction[0] *= -1;
            direction[1] *= -1;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Switch the direction back
            direction[0] *= -1;
            direction[1] *= -1;

            // If we have failed to find a push solution with the above, then we try
            // to find a solution by pushing along the perpendicular axis.

            // Swap the components
            int temp = direction[1];
            direction[1] = direction[0];
            direction[0] = temp;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }

            // Then we try the opposite direction
            direction[0] *= -1;
            direction[1] *= -1;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Switch the direction back
            direction[0] *= -1;
            direction[1] *= -1;

            // Swap the components back
            temp = direction[1];
            direction[1] = direction[0];
            direction[0] = temp;
        }
        return false;
    }

    private boolean rearrangementExists(int cellX, int cellY, int spanX, int spanY, int[] direction,
                                        View ignoreView, ItemConfiguration solution) {
        // Return early if get invalid cell positions
        if (cellX < 0 || cellY < 0) return false;

        mIntersectingViews.clear();
        mOccupiedRect.set(cellX, cellY, cellX + spanX, cellY + spanY);

        // Mark the desired location of the view currently being dragged.
        if (ignoreView != null) {
            CellAndSpan c = solution.map.get(ignoreView);
            if (c != null) {
                c.x = cellX;
                c.y = cellY;
            }
        }
        Rect r0 = new Rect(cellX, cellY, cellX + spanX, cellY + spanY);
        Rect r1 = new Rect();
        for (View child : solution.map.keySet()) {
            if (child == ignoreView) continue;
            CellAndSpan c = solution.map.get(child);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            r1.set(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
            if (Rect.intersects(r0, r1)) {
                if (!lp.canReorder) {
                    return false;
                }
                mIntersectingViews.add(child);
            }
        }

        solution.intersectingViews = new ArrayList<View>(mIntersectingViews);

        // First we try to find a solution which respects the push mechanic. That is,
        // we try to find a solution such that no displaced item travels through another item
        // without also displacing that item.
        if (attemptPushInDirection(mIntersectingViews, mOccupiedRect, direction, ignoreView,
                solution)) {
            return true;
        }

        // Next we try moving the views as a block, but without requiring the push mechanic.
        if (addViewsToTempLocation(mIntersectingViews, mOccupiedRect, direction, ignoreView,
                solution)) {
            return true;
        }

        // Ok, they couldn't move as a block, let's move them individually
        for (View v : mIntersectingViews) {
            if (!addViewToTempLocation(v, mOccupiedRect, direction, solution)) {
                return false;
            }
        }
        return true;
    }

    /*
     * Returns a pair (x, y), where x,y are in {-1, 0, 1} corresponding to vector between
     * the provided point and the provided cell
     */
    private void computeDirectionVector(float deltaX, float deltaY, int[] result) {
        double angle = Math.atan(deltaY / deltaX);

        result[0] = 0;
        result[1] = 0;
        if (Math.abs(Math.cos(angle)) > 0.5f) {
            result[0] = (int) Math.signum(deltaX);
        }
        if (Math.abs(Math.sin(angle)) > 0.5f) {
            result[1] = (int) Math.signum(deltaY);
        }
    }

    private void copyOccupiedArray(boolean[][] occupied) {
        for (int i = 0; i < mCountX; i++) {
            for (int j = 0; j < mCountY; j++) {
                occupied[i][j] = mOccupied[i][j];
            }
        }
    }

    private ItemConfiguration findReorderSolution(int pixelX, int pixelY, int minSpanX, int minSpanY,
                                                  int spanX, int spanY, int[] direction, View dragView, boolean decX,
                                                  ItemConfiguration solution) {
        // Copy the current state into the solution. This solution will be manipulated as necessary.
        copyCurrentStateToSolution(solution, false);
        // Copy the current occupied array into the temporary occupied array. This array will be
        // manipulated as necessary to find a solution.
        copyOccupiedArray(mTmpOccupied);

        // We find the nearest cell into which we would place the dragged item, assuming there's
        // nothing in its way.
        int result[] = new int[2];
        result = findNearestArea(pixelX, pixelY, spanX, spanY, result);

        boolean success = false;
        // First we try the exact nearest position of the item being dragged,
        // we will then want to try to move this around to other neighbouring positions
        success = rearrangementExists(result[0], result[1], spanX, spanY, direction, dragView,
                solution);

        if (!success) {
            // We try shrinking the widget down to size in an alternating pattern, shrink 1 in
            // x, then 1 in y etc.
            if (spanX > minSpanX && (minSpanY == spanY || decX)) {
                return findReorderSolution(pixelX, pixelY, minSpanX, minSpanY, spanX - 1, spanY,
                        direction, dragView, false, solution);
            } else if (spanY > minSpanY) {
                return findReorderSolution(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY - 1,
                        direction, dragView, true, solution);
            }
            solution.isSolution = false;
        } else {
            solution.isSolution = true;
            solution.dragViewX = result[0];
            solution.dragViewY = result[1];
            solution.dragViewSpanX = spanX;
            solution.dragViewSpanY = spanY;
        }
        return solution;
    }

    private void copyCurrentStateToSolution(ItemConfiguration solution, boolean temp) {
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            CellAndSpan c;
            if (temp) {
                c = new CellAndSpan(lp.tmpCellX, lp.tmpCellY, lp.cellHSpan, lp.cellVSpan);
            } else {
                c = new CellAndSpan(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan);
            }
            solution.add(child, c);
        }
    }

    private void copySolutionToTempState(ItemConfiguration solution, View dragView) {
        for (int i = 0; i < mCountX; i++) {
            for (int j = 0; j < mCountY; j++) {
                mTmpOccupied[i][j] = false;
            }
        }

        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            if (child == dragView) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            CellAndSpan c = solution.map.get(child);
            if (c != null) {
                lp.tmpCellX = c.x;
                lp.tmpCellY = c.y;
                lp.cellHSpan = c.spanX;
                lp.cellVSpan = c.spanY;
                markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
            }
        }
        markCellsForView(solution.dragViewX, solution.dragViewY, solution.dragViewSpanX,
                solution.dragViewSpanY, mTmpOccupied, true);
    }

    private void animateItemsToSolution(ItemConfiguration solution, View dragView, boolean
            commitDragView) {

        boolean[][] occupied = DESTRUCTIVE_REORDER ? mOccupied : mTmpOccupied;
        for (int i = 0; i < mCountX; i++) {
            for (int j = 0; j < mCountY; j++) {
                occupied[i][j] = false;
            }
        }

        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            if (child == dragView) continue;
            CellAndSpan c = solution.map.get(child);
            if (c != null) {
                animateChildToPosition(child, c.x, c.y, REORDER_ANIMATION_DURATION, 0,
                        DESTRUCTIVE_REORDER, false);
                markCellsForView(c.x, c.y, c.spanX, c.spanY, occupied, true);
            }
        }
        if (commitDragView) {
            markCellsForView(solution.dragViewX, solution.dragViewY, solution.dragViewSpanX,
                    solution.dragViewSpanY, occupied, true);
        }
    }


    // This method starts or changes the reorder preview animations
    private void beginOrAdjustReorderPreviewAnimations(ItemConfiguration solution,
                                                       View dragView, int delay, int mode) {
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            if (child == dragView) continue;
            CellAndSpan c = solution.map.get(child);
            boolean skip = mode == ReorderPreviewAnimation.MODE_HINT && solution.intersectingViews
                    != null && !solution.intersectingViews.contains(child);

            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (c != null && !skip) {
                ReorderPreviewAnimation rha = new ReorderPreviewAnimation(child, mode, lp.cellX,
                        lp.cellY, c.x, c.y, c.spanX, c.spanY);
                rha.animate();
            }
        }
    }

    // Class which represents the reorder preview animations. These animations show that an item is
    // in a temporary state, and hint at where the item will return to.
    class ReorderPreviewAnimation {
        View child;
        float finalDeltaX;
        float finalDeltaY;
        float initDeltaX;
        float initDeltaY;
        float finalScale;
        float initScale;
        int mode;
        boolean repeating = false;
        private static final int PREVIEW_DURATION = 300;
        private static final int HINT_DURATION = Workspace.REORDER_TIMEOUT;

        public static final int MODE_HINT = 0;
        public static final int MODE_PREVIEW = 1;

        Animator a;

        public ReorderPreviewAnimation(View child, int mode, int cellX0, int cellY0, int cellX1,
                                       int cellY1, int spanX, int spanY) {
            regionToCenterPoint(cellX0, cellY0, spanX, spanY, mTmpPoint);
            final int x0 = mTmpPoint[0];
            final int y0 = mTmpPoint[1];
            regionToCenterPoint(cellX1, cellY1, spanX, spanY, mTmpPoint);
            final int x1 = mTmpPoint[0];
            final int y1 = mTmpPoint[1];
            final int dX = x1 - x0;
            final int dY = y1 - y0;
            finalDeltaX = 0;
            finalDeltaY = 0;
            int dir = mode == MODE_HINT ? -1 : 1;
            if (dX == dY && dX == 0) {
            } else {
                if (dY == 0) {
                    finalDeltaX = -dir * Math.signum(dX) * mReorderPreviewAnimationMagnitude;
                } else if (dX == 0) {
                    finalDeltaY = -dir * Math.signum(dY) * mReorderPreviewAnimationMagnitude;
                } else {
                    double angle = Math.atan((float) (dY) / dX);
                    finalDeltaX = (int) (-dir * Math.signum(dX) *
                            Math.abs(Math.cos(angle) * mReorderPreviewAnimationMagnitude));
                    finalDeltaY = (int) (-dir * Math.signum(dY) *
                            Math.abs(Math.sin(angle) * mReorderPreviewAnimationMagnitude));
                }
            }
            this.mode = mode;
            initDeltaX = child.getTranslationX();
            initDeltaY = child.getTranslationY();
            finalScale = getChildrenScale() - 4.0f / child.getWidth();
            initScale = child.getScaleX();
            this.child = child;
        }

        void animate() {
            if (mShakeAnimators.containsKey(child)) {
                ReorderPreviewAnimation oldAnimation = mShakeAnimators.get(child);
                oldAnimation.cancel();
                mShakeAnimators.remove(child);
                if (finalDeltaX == 0 && finalDeltaY == 0) {
                    completeAnimationImmediately();
                    return;
                }
            }
            if (finalDeltaX == 0 && finalDeltaY == 0) {
                return;
            }
            ValueAnimator va = LauncherAnimUtils.ofFloat(child, 0f, 1f);
            a = va;
            va.setRepeatMode(ValueAnimator.REVERSE);
            va.setRepeatCount(ValueAnimator.INFINITE);
            va.setDuration(mode == MODE_HINT ? HINT_DURATION : PREVIEW_DURATION);
            va.setStartDelay((int) (Math.random() * 60));
            va.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float r = ((Float) animation.getAnimatedValue()).floatValue();
                    float r1 = (mode == MODE_HINT && repeating) ? 1.0f : r;
                    float x = r1 * finalDeltaX + (1 - r1) * initDeltaX;
                    float y = r1 * finalDeltaY + (1 - r1) * initDeltaY;
                    child.setTranslationX(x);
                    child.setTranslationY(y);
                    float s = r * finalScale + (1 - r) * initScale;
                    child.setScaleX(s);
                    child.setScaleY(s);
                }
            });
            va.addListener(new AnimatorListenerAdapter() {
                public void onAnimationRepeat(Animator animation) {
                    // We make sure to end only after a full period
                    initDeltaX = 0;
                    initDeltaY = 0;
                    initScale = getChildrenScale();
                    repeating = true;
                }
            });
            mShakeAnimators.put(child, this);
            va.start();
        }

        private void cancel() {
            if (a != null) {
                a.cancel();
            }
        }

        @Thunk
        void completeAnimationImmediately() {
            if (a != null) {
                a.cancel();
            }

            AnimatorSet s = LauncherAnimUtils.createAnimatorSet();
            a = s;
            s.playTogether(
                    LauncherAnimUtils.ofFloat(child, "scaleX", getChildrenScale()),
                    LauncherAnimUtils.ofFloat(child, "scaleY", getChildrenScale()),
                    LauncherAnimUtils.ofFloat(child, "translationX", 0f),
                    LauncherAnimUtils.ofFloat(child, "translationY", 0f)
            );
            s.setDuration(REORDER_ANIMATION_DURATION);
            s.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
            s.start();
        }
    }

    private void completeAndClearReorderPreviewAnimations() {
        for (ReorderPreviewAnimation a : mShakeAnimators.values()) {
            a.completeAnimationImmediately();
        }
        mShakeAnimators.clear();
    }

    private void commitTempPlacement() {
        for (int i = 0; i < mCountX; i++) {
            for (int j = 0; j < mCountY; j++) {
                mOccupied[i][j] = mTmpOccupied[i][j];
            }
        }
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            ItemInfo info = (ItemInfo) child.getTag();
            // We do a null check here because the item info can be null in the case of the
            // AllApps button in the hotseat.
            if (info != null) {
                if (info.cellX != lp.tmpCellX || info.cellY != lp.tmpCellY ||
                        info.spanX != lp.cellHSpan || info.spanY != lp.cellVSpan) {
                    info.requiresDbUpdate = true;
                }
                info.cellX = lp.cellX = lp.tmpCellX;
                info.cellY = lp.cellY = lp.tmpCellY;
                info.spanX = lp.cellHSpan;
                info.spanY = lp.cellVSpan;
            }
        }
        mLauncher.getWorkspace().updateItemLocationsInDatabase(this);
    }

    private void setUseTempCoords(boolean useTempCoords) {
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            LayoutParams lp = (LayoutParams) mShortcutsAndWidgets.getChildAt(i).getLayoutParams();
            lp.useTmpCoords = useTempCoords;
        }
    }

    private ItemConfiguration findConfigurationNoShuffle(int pixelX, int pixelY, int minSpanX, int minSpanY,
                                                         int spanX, int spanY, View dragView, ItemConfiguration solution) {
        int[] result = new int[2];
        int[] resultSpan = new int[2];
        findNearestVacantArea(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, result,
                resultSpan);
        if (result[0] >= 0 && result[1] >= 0) {
            copyCurrentStateToSolution(solution, false);
            solution.dragViewX = result[0];
            solution.dragViewY = result[1];
            solution.dragViewSpanX = resultSpan[0];
            solution.dragViewSpanY = resultSpan[1];
            solution.isSolution = true;
        } else {
            solution.isSolution = false;
        }
        return solution;
    }

    public void prepareChildForDrag(View child) {
        markCellsAsUnoccupiedForView(child);
    }

    /* This seems like it should be obvious and straight-forward, but when the direction vector
    needs to match with the notion of the dragView pushing other views, we have to employ
    a slightly more subtle notion of the direction vector. The question is what two points is
    the vector between? The center of the dragView and its desired destination? Not quite, as
    this doesn't necessarily coincide with the interaction of the dragView and items occupying
    those cells. Instead we use some heuristics to often lock the vector to up, down, left
    or right, which helps make pushing feel right.
    */
    private void getDirectionVectorForDrop(int dragViewCenterX, int dragViewCenterY, int spanX,
                                           int spanY, View dragView, int[] resultDirection) {
        int[] targetDestination = new int[2];

        findNearestArea(dragViewCenterX, dragViewCenterY, spanX, spanY, targetDestination);
        Rect dragRect = new Rect();
        regionToRect(targetDestination[0], targetDestination[1], spanX, spanY, dragRect);
        dragRect.offset(dragViewCenterX - dragRect.centerX(), dragViewCenterY - dragRect.centerY());

        Rect dropRegionRect = new Rect();
        getViewsIntersectingRegion(targetDestination[0], targetDestination[1], spanX, spanY,
                dragView, dropRegionRect, mIntersectingViews);

        int dropRegionSpanX = dropRegionRect.width();
        int dropRegionSpanY = dropRegionRect.height();

        regionToRect(dropRegionRect.left, dropRegionRect.top, dropRegionRect.width(),
                dropRegionRect.height(), dropRegionRect);

        int deltaX = (dropRegionRect.centerX() - dragViewCenterX) / spanX;
        int deltaY = (dropRegionRect.centerY() - dragViewCenterY) / spanY;

        if (dropRegionSpanX == mCountX || spanX == mCountX) {
            deltaX = 0;
        }
        if (dropRegionSpanY == mCountY || spanY == mCountY) {
            deltaY = 0;
        }

        if (deltaX == 0 && deltaY == 0) {
            // No idea what to do, give a random direction.
            resultDirection[0] = 1;
            resultDirection[1] = 0;
        } else {
            computeDirectionVector(deltaX, deltaY, resultDirection);
        }
    }

    // For a given cell and span, fetch the set of views intersecting the region.
    private void getViewsIntersectingRegion(int cellX, int cellY, int spanX, int spanY,
                                            View dragView, Rect boundingRect, ArrayList<View> intersectingViews) {
        if (boundingRect != null) {
            boundingRect.set(cellX, cellY, cellX + spanX, cellY + spanY);
        }
        intersectingViews.clear();
        Rect r0 = new Rect(cellX, cellY, cellX + spanX, cellY + spanY);
        Rect r1 = new Rect();
        final int count = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            if (child == dragView) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            r1.set(lp.cellX, lp.cellY, lp.cellX + lp.cellHSpan, lp.cellY + lp.cellVSpan);
            if (Rect.intersects(r0, r1)) {
                mIntersectingViews.add(child);
                if (boundingRect != null) {
                    boundingRect.union(r1);
                }
            }
        }
    }

    boolean isNearestDropLocationOccupied(int pixelX, int pixelY, int spanX, int spanY,
                                          View dragView, int[] result) {
        result = findNearestArea(pixelX, pixelY, spanX, spanY, result);
        getViewsIntersectingRegion(result[0], result[1], spanX, spanY, dragView, null,
                mIntersectingViews);
        return !mIntersectingViews.isEmpty();
    }

    public void revertTempState() {
        completeAndClearReorderPreviewAnimations();
        if (isItemPlacementDirty() && !DESTRUCTIVE_REORDER) {
            final int count = mShortcutsAndWidgets.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = mShortcutsAndWidgets.getChildAt(i);
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.cellY) {
                    lp.tmpCellX = lp.cellX;
                    lp.tmpCellY = lp.cellY;
                    animateChildToPosition(child, lp.cellX, lp.cellY, REORDER_ANIMATION_DURATION,
                            0, false, false);
                }
            }
            setItemPlacementDirty(false);
        }
    }

    boolean createAreaForResize(int cellX, int cellY, int spanX, int spanY,
                                View dragView, int[] direction, boolean commit) {
        int[] pixelXY = new int[2];
        regionToCenterPoint(cellX, cellY, spanX, spanY, pixelXY);

        // First we determine if things have moved enough to cause a different layout
        ItemConfiguration swapSolution = findReorderSolution(pixelXY[0], pixelXY[1], spanX, spanY,
                spanX, spanY, direction, dragView, true, new ItemConfiguration());

        setUseTempCoords(true);
        if (swapSolution != null && swapSolution.isSolution) {
            // If we're just testing for a possible location (MODE_ACCEPT_DROP), we don't bother
            // committing anything or animating anything as we just want to determine if a solution
            // exists
            copySolutionToTempState(swapSolution, dragView);
            setItemPlacementDirty(true);
            animateItemsToSolution(swapSolution, dragView, commit);

            if (commit) {
                commitTempPlacement();
                completeAndClearReorderPreviewAnimations();
                setItemPlacementDirty(false);
            } else {
                beginOrAdjustReorderPreviewAnimations(swapSolution, dragView,
                        REORDER_ANIMATION_DURATION, ReorderPreviewAnimation.MODE_PREVIEW);
            }
            mShortcutsAndWidgets.requestLayout();
        }
        return swapSolution.isSolution;
    }

    int[] performReorder(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX, int spanY,
                         View dragView, int[] result, int resultSpan[], int mode) {
        // First we determine if things have moved enough to cause a different layout
        result = findNearestArea(pixelX, pixelY, spanX, spanY, result);

        if (resultSpan == null) {
            resultSpan = new int[2];
        }

        // When we are checking drop validity or actually dropping, we don't recompute the
        // direction vector, since we want the solution to match the preview, and it's possible
        // that the exact position of the item has changed to result in a new reordering outcome.
        if ((mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL || mode == MODE_ACCEPT_DROP)
                && mPreviousReorderDirection[0] != INVALID_DIRECTION) {
            mDirectionVector[0] = mPreviousReorderDirection[0];
            mDirectionVector[1] = mPreviousReorderDirection[1];
            // We reset this vector after drop
            if (mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
                mPreviousReorderDirection[0] = INVALID_DIRECTION;
                mPreviousReorderDirection[1] = INVALID_DIRECTION;
            }
        } else {
            getDirectionVectorForDrop(pixelX, pixelY, spanX, spanY, dragView, mDirectionVector);
            mPreviousReorderDirection[0] = mDirectionVector[0];
            mPreviousReorderDirection[1] = mDirectionVector[1];
        }

        // Find a solution involving pushing / displacing any items in the way
        ItemConfiguration swapSolution = findReorderSolution(pixelX, pixelY, minSpanX, minSpanY,
                spanX, spanY, mDirectionVector, dragView, true, new ItemConfiguration());

        // We attempt the approach which doesn't shuffle views at all
        ItemConfiguration noShuffleSolution = findConfigurationNoShuffle(pixelX, pixelY, minSpanX,
                minSpanY, spanX, spanY, dragView, new ItemConfiguration());

        ItemConfiguration finalSolution = null;

        // If the reorder solution requires resizing (shrinking) the item being dropped, we instead
        // favor a solution in which the item is not resized, but
        if (swapSolution.isSolution && swapSolution.area() >= noShuffleSolution.area()) {
            finalSolution = swapSolution;
        } else if (noShuffleSolution.isSolution) {
            finalSolution = noShuffleSolution;
        }

        if (mode == MODE_SHOW_REORDER_HINT) {
            if (finalSolution != null) {
                beginOrAdjustReorderPreviewAnimations(finalSolution, dragView, 0,
                        ReorderPreviewAnimation.MODE_HINT);
                result[0] = finalSolution.dragViewX;
                result[1] = finalSolution.dragViewY;
                resultSpan[0] = finalSolution.dragViewSpanX;
                resultSpan[1] = finalSolution.dragViewSpanY;
            } else {
                result[0] = result[1] = resultSpan[0] = resultSpan[1] = -1;
            }
            return result;
        }

        boolean foundSolution = true;
        if (!DESTRUCTIVE_REORDER) {
            setUseTempCoords(true);
        }

        if (finalSolution != null) {
            result[0] = finalSolution.dragViewX;
            result[1] = finalSolution.dragViewY;
            resultSpan[0] = finalSolution.dragViewSpanX;
            resultSpan[1] = finalSolution.dragViewSpanY;

            // If we're just testing for a possible location (MODE_ACCEPT_DROP), we don't bother
            // committing anything or animating anything as we just want to determine if a solution
            // exists
            if (mode == MODE_DRAG_OVER || mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
                if (!DESTRUCTIVE_REORDER) {
                    copySolutionToTempState(finalSolution, dragView);
                }
                setItemPlacementDirty(true);
                animateItemsToSolution(finalSolution, dragView, mode == MODE_ON_DROP);

                if (!DESTRUCTIVE_REORDER &&
                        (mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL)) {
                    commitTempPlacement();
                    completeAndClearReorderPreviewAnimations();
                    setItemPlacementDirty(false);
                } else {
                    beginOrAdjustReorderPreviewAnimations(finalSolution, dragView,
                            REORDER_ANIMATION_DURATION, ReorderPreviewAnimation.MODE_PREVIEW);
                }
            }
        } else {
            foundSolution = false;
            result[0] = result[1] = resultSpan[0] = resultSpan[1] = -1;
        }

        if ((mode == MODE_ON_DROP || !foundSolution) && !DESTRUCTIVE_REORDER) {
            setUseTempCoords(false);
        }

        mShortcutsAndWidgets.requestLayout();
        return result;
    }

    void setItemPlacementDirty(boolean dirty) {
        mItemPlacementDirty = dirty;
    }

    boolean isItemPlacementDirty() {
        return mItemPlacementDirty;
    }

    @Thunk
    class ItemConfiguration {
        HashMap<View, CellAndSpan> map = new HashMap<View, CellAndSpan>();
        private HashMap<View, CellAndSpan> savedMap = new HashMap<View, CellAndSpan>();
        ArrayList<View> sortedViews = new ArrayList<View>();
        ArrayList<View> intersectingViews;
        boolean isSolution = false;
        int dragViewX, dragViewY, dragViewSpanX, dragViewSpanY;

        void save() {
            // Copy current state into savedMap
            for (View v : map.keySet()) {
                map.get(v).copy(savedMap.get(v));
            }
        }

        void restore() {
            // Restore current state from savedMap
            for (View v : savedMap.keySet()) {
                savedMap.get(v).copy(map.get(v));
            }
        }

        void add(View v, CellAndSpan cs) {
            map.put(v, cs);
            savedMap.put(v, new CellAndSpan());
            sortedViews.add(v);
        }

        int area() {
            return dragViewSpanX * dragViewSpanY;
        }
    }

    private class CellAndSpan {
        int x, y;
        int spanX, spanY;

        public CellAndSpan() {
        }

        public void copy(CellAndSpan copy) {
            copy.x = x;
            copy.y = y;
            copy.spanX = spanX;
            copy.spanY = spanY;
        }

        public CellAndSpan(int x, int y, int spanX, int spanY) {
            this.x = x;
            this.y = y;
            this.spanX = spanX;
            this.spanY = spanY;
        }

        public String toString() {
            return "(" + x + ", " + y + ": " + spanX + ", " + spanY + ")";
        }

    }

    /**
     * Find a starting cell position that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX     The X location at which you want to search for a vacant area.
     * @param pixelY     The Y location at which you want to search for a vacant area.
     * @param spanX      Horizontal span of the object.
     * @param spanY      Vertical span of the object.
     * @param result     Previously returned value to possibly recycle.
     * @return The X, Y cell of a vacant area that can contain this object,
     * nearest the requested location.
     */
    public int[] findNearestArea(int pixelX, int pixelY, int spanX, int spanY, int[] result) {
        return findNearestArea(pixelX, pixelY, spanX, spanY, spanX, spanY, false, result, null);
    }

    boolean existsEmptyCell() {
        return findCellForSpan(null, 1, 1);
    }

    /**
     * Finds the upper-left coordinate of the first rectangle in the grid that can
     * hold a cell of the specified dimensions. If intersectX and intersectY are not -1,
     * then this method will only return coordinates for rectangles that contain the cell
     * (intersectX, intersectY)
     *
     * @param cellXY The array that will contain the position of a vacant cell if such a cell
     *               can be found.
     * @param spanX  The horizontal span of the cell we want to find.
     * @param spanY  The vertical span of the cell we want to find.
     * @return True if a vacant cell of the specified dimension was found, false otherwise.
     */
    public boolean findCellForSpan(int[] cellXY, int spanX, int spanY) {
        boolean foundCell = false;
        final int endX = mCountX - (spanX - 1);
        final int endY = mCountY - (spanY - 1);

        for (int y = 0; y < endY && !foundCell; y++) {
            inner:
            for (int x = 0; x < endX; x++) {
                for (int i = 0; i < spanX; i++) {
                    for (int j = 0; j < spanY; j++) {
                        if (mOccupied[x + i][y + j]) {
                            // small optimization: we can skip to after the column we just found
                            // an occupied cell
                            x += i;
                            continue inner;
                        }
                    }
                }
                if (cellXY != null) {
                    cellXY[0] = x;
                    cellXY[1] = y;
                }
                foundCell = true;
                break;
            }
        }

        return foundCell;
    }

    /**
     * A drag event has begun over this layout.
     * It may have begun over this layout (in which case onDragChild is called first),
     * or it may have begun on another layout.
     */
    public void onDragEnter() {
        mDragging = true;
    }

    /**
     * Called when drag has left this CellLayout or has been completed (successfully or not)
     */
    public void onDragExit() {
        // This can actually be called when we aren't in a drag, e.g. when adding a new
        // item to this layout via the customize drawer.
        // Guard against that case.
        if (mDragging) {
            mDragging = false;
        }

        // Invalidate the drag data
        mDragCell[0] = mDragCell[1] = -1;
        mDragOutlineAnims[mDragOutlineCurrent].animateOut();
        mDragOutlineCurrent = (mDragOutlineCurrent + 1) % mDragOutlineAnims.length;
        revertTempState();
        setIsDragOverlapping(false);
    }

    /**
     * Mark a child as having been dropped.
     * At the beginning of the drag operation, the child may have been on another
     * screen, but it is re-parented before this method is called.
     *
     * @param child The child that is being dropped
     */
    void onDropChild(View child) {
        if (child != null) {
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.dropped = true;
            child.requestLayout();
        }
    }

    /**
     * Computes a bounding rectangle for a range of cells
     *
     * @param cellX      X coordinate of upper left corner expressed as a cell position
     * @param cellY      Y coordinate of upper left corner expressed as a cell position
     * @param cellHSpan  Width in cells
     * @param cellVSpan  Height in cells
     * @param resultRect Rect into which to put the results
     */
    public void cellToRect(int cellX, int cellY, int cellHSpan, int cellVSpan, Rect resultRect) {
        final int cellWidth = mCellWidth;
        final int cellHeight = mCellHeight;
        final int widthGap = mWidthGap;
        final int heightGap = mHeightGap;

        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();

        int width = cellHSpan * cellWidth + ((cellHSpan - 1) * widthGap);
        int height = cellVSpan * cellHeight + ((cellVSpan - 1) * heightGap);

        int x = hStartPadding + cellX * (cellWidth + widthGap);
        int y = vStartPadding + cellY * (cellHeight + heightGap);

        resultRect.set(x, y, x + width, y + height);
    }

    private void clearOccupiedCells() {
        for (int x = 0; x < mCountX; x++) {
            for (int y = 0; y < mCountY; y++) {
                mOccupied[x][y] = false;
            }
        }
    }

    public void markCellsAsOccupiedForView(View view) {
        if (view == null || view.getParent() != mShortcutsAndWidgets) return;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        markCellsForView(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, mOccupied, true);
    }

    public void markCellsAsUnoccupiedForView(View view) {
        if (view == null || view.getParent() != mShortcutsAndWidgets) return;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        markCellsForView(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, mOccupied, false);
    }

    private void markCellsForView(int cellX, int cellY, int spanX, int spanY, boolean[][] occupied,
                                  boolean value) {
        if (cellX < 0 || cellY < 0) return;
        for (int x = cellX; x < cellX + spanX && x < mCountX; x++) {
            for (int y = cellY; y < cellY + spanY && y < mCountY; y++) {
                occupied[x][y] = value;
            }
        }
    }

    public int getDesiredWidth() {
        return getPaddingLeft() + getPaddingRight() + (mCountX * mCellWidth) +
                (Math.max((mCountX - 1), 0) * mWidthGap);
    }

    public int getDesiredHeight() {
        return getPaddingTop() + getPaddingBottom() + (mCountY * mCellHeight) +
                (Math.max((mCountY - 1), 0) * mHeightGap);
    }

    public int getDesiredHeight(int i) {
        return getPaddingTop() + getPaddingBottom() + (this.mCellHeight * i) + (Math.max(i - 1, 0) * this.mHeightGap);
    }

    public boolean isOccupied(int x, int y) {
        if (x < mCountX && y < mCountY) {
            return mOccupied[x][y];
        } else {
            throw new RuntimeException("Position exceeds the bound of this CellLayout");
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new CellLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof CellLayout.LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new CellLayout.LayoutParams(p);
    }

    public boolean findVacantCell(int spanX, int spanY, int[] outXY) {
        return Utilities.findVacantCell(outXY, spanX, spanY, mCountX, mCountY, mOccupied);
    }

    public boolean isRegionVacant(int x, int y, int spanX, int spanY) {
        int x2 = x + spanX - 1;
        int y2 = y + spanY - 1;
        if (x < 0 || y < 0 || x2 >= mCountX || y2 >= mCountY) {
            return false;
        }
        for (int i = x; i <= x2; i++) {
            for (int j = y; j <= y2; j++) {
                if (mOccupied[i][j]) {
                    return false;
                }
            }
        }

        return true;
    }


    public void addAlignCell(View view) {
        boolean enableDesktopAlign = Settings.isDesktopAlignEnable(mLauncher);

        if (!enableDesktopAlign) {
            return;
        }

        final int[] cellXY = new int[2];
        if (view != null || view.getParent() == mShortcutsAndWidgets) {
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            cellXY[0] = lp.cellX;
            cellXY[1] = lp.cellY;

            // 小部件删除时，不做对齐
            if (lp.cellHSpan == 1 && lp.cellVSpan == 1) {
                mPendingAlignCells.add(cellXY);
            }
        }
    }

    public void completeAlign() {
        if (mPendingAlignCells.size() == 0) {
            return;
        }

        setUseTempCoords(false);
        for (int[] cell : mPendingAlignCells) {
            alignToCell(cell);
        }
        mPendingAlignCells.clear();
    }


    private void alignToCell(int[] cellXY) {
        if (getChildAt(cellXY[0], cellXY[1]) != null) {
            return;
        }

        int startPos = getRankForCell(cellXY) + 1;
        int endPos = mCountX * mCountY - 1;
        int moveDirection = -1;
        int delay = 0;
        for (int i = startPos; i <= endPos; i++) {
            int nextPos = i + moveDirection;
            View v = getChildAt(i % mCountX, i / mCountX);
            if (v == null) {
                continue;
            }

            LayoutParams lp = (LayoutParams) v.getLayoutParams();
            if (lp.cellHSpan > 1 || lp.cellVSpan > 1) {
                break;
            }

            if (animateChildToPosition(v, nextPos % mCountX, nextPos / mCountX,
                    ALIGN_ANIMATION_DURATION, delay, true, true)) {
                delay += ALIGN_VIEW_ANIM_DELAY;
            }
        }

        mLauncher.getWorkspace().updateItemLocationsInDatabase(this);
    }


    public void resort(boolean forwoad) {
        if (isEmpty()) {
            return;
        }

        int cellCount = mCountX * mCountY;
        List<int[]> candidateCells = new ArrayList<>();
        for (int i = 0; i < cellCount; i++) {
            int[] cell = new int[]{i % mCountX, i / mCountX};
            View v = getChildAt(cell[0], cell[1]);

            if (v == null) {
                candidateCells.add(cell);
                continue;
            }

            LayoutParams lp = (LayoutParams) v.getLayoutParams();
            if (lp.cellHSpan == 1 && lp.cellVSpan == 1) {
                candidateCells.add(cell);
            }
        }

        List<View> candidateViews = new ArrayList<>();
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.cellHSpan == 1 && lp.cellVSpan == 1) {
                candidateViews.add(child);
            }
        }

        int candiateCellCount = candidateCells.size();
        int candiateViewCount = candidateViews.size();
        if (candiateCellCount == 0 || candiateViewCount == 0) {
            return;
        }

        int delay = 0;
        int moveViewStart, moveViewEnd;
        int moveCellStart;
        int moveDirection;
        if (forwoad) {
            moveCellStart = moveViewStart = 0;
            moveViewEnd = candiateViewCount - 1;
            moveDirection = 1;
        } else {
            moveViewStart = candiateViewCount - 1;
            moveCellStart = candiateCellCount - 1;
            moveViewEnd = 0;
            moveDirection = -1;
        }

        int movePos = -1;
        do {
            View child = candidateViews.get(moveViewStart);
            int[] cell = candidateCells.get(moveCellStart);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.cellX != cell[0] || lp.cellY != cell[1]) {
                if (animateChildToPosition(child, cell[0], cell[1],
                        ALIGN_ANIMATION_DURATION, delay, true, true)) {
                    delay += ALIGN_VIEW_ANIM_DELAY;
                }
            }
            movePos = moveViewStart;
            moveCellStart += moveDirection;
            moveViewStart += moveDirection;
        } while (movePos != moveViewEnd);

        mLauncher.getWorkspace().updateItemLocationsInDatabase(this);
    }

    public int getRankForCell(int[] cellXY) {
        return cellXY[0] + mCountX * cellXY[1];
    }

    @SuppressLint("WrongConstant")
    public void beginShakeAnimations() {
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = mShortcutsAndWidgets.getChildAt(i);
            if (childAt instanceof IShakeInterface) {
                childAt.setLayerType(2, null);
                ((IShakeInterface) childAt).beginOrAdjustHintAnimations();
            }
        }

    }

    public void stopShakeAnimations() {
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = mShortcutsAndWidgets.getChildAt(i);
            if (childAt instanceof IShakeInterface) {
                childAt.setLayerType(View.LAYER_TYPE_NONE, null);
                ((IShakeInterface) childAt).completeAndClearReorderHintAnimations();
                childAt.invalidate();
            }
        }
    }

    public boolean isPrivatePage() {
        return false;
    }

    public void setGap(int i, int i2) {
        this.mOriginalWidthGap = i;
        this.mOriginalHeightGap = i2;
        requestLayout();
    }

    public void setCellDimension(int width, int height) {
        this.mCellWidth = width;
//        this.mOriginalCellWidth = i;
        this.mCellHeight = height;
//        this.mOriginalCellHeight = i2;
    }

    public int getPositionYForIndex(int i) {
        int i2 = this.mCellHeight;
        int i3 = this.mHeightGap;
        return (((i2 + i3) * (i / this.mCountX)) + getPaddingTop()) - getPaddingTop();
    }

    public boolean getVacantCell(int[] iArr, int i, int i2) {
        return findVacantCell(iArr, i, i2, this.mCountX, this.mCountY, this.mOccupied);
    }

    static boolean findVacantCell(int[] iArr, int i, int i2, int i3, int i4, boolean[][] zArr) {
        int i5 = 0;
        while (i5 < i4) {
            int i6 = 0;
            while (i6 < i3) {
                boolean z = !zArr[i6][i5];
                for (int i7 = i6; i7 < (i6 + i) - 1 && i6 < i3; i7++) {
                    for (int i8 = i5; i8 < (i5 + i2) - 1 && i5 < i4; i8++) {
                        z = z && !zArr[i7][i8];
                        if (!z) {
                            break;
                        }
                    }
                }
                if (z) {
                    iArr[0] = i6;
                    iArr[1] = i5;
                    return true;
                }
                i6++;
            }
            i5++;
        }
        return false;
    }

    public View getChildAtPosition(int i) {
        return getChildAt(i % this.mCountX, i / this.mCountX);
    }


    public void reSetDrawDel() {
    }

    public void updatePageListView() {
//        if (this.mLauncher != null && this.mLauncher.isPageListViewVisible()) {
//            this.mLauncher.getPageListView().updatePageThumb(this);
//        }
    }

    @Override
    public boolean updateAppInfo(AppInfo applicationInfo, boolean z) {
        boolean z2;
        Context context = (Context)this.mLauncher;
        final IconCache iconCache = LauncherAppState.getInstance().getIconCache();

        int childCount = getChildCount();
        boolean z3 = false;
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt instanceof BubbleTextView) {
                BubbleTextView bubbleTextView = (BubbleTextView) childAt;
                ShortcutInfo shortcutInfo = (ShortcutInfo) childAt.getTag();
                ComponentName component = shortcutInfo.intent.getComponent();
                if (component == null || !component.equals(applicationInfo.componentName)) {
                    z2 = z3;
                } else {
                    shortcutInfo.setIcon(applicationInfo.iconBitmap);
                    shortcutInfo.title = applicationInfo.title.toString();
                    shortcutInfo.intent = new Intent(applicationInfo.intent);
                    shortcutInfo.newInstalled = applicationInfo.newInstalled;
                    bubbleTextView.applyFromShortcutInfo(shortcutInfo, iconCache);
                    if (!z && this.mLauncher != null && this.mLauncher.isInBatchMode() && !bubbleTextView.isBatchSelected()) {
//                        bubbleTextView.setDarkEffect();
                    }
                    if (applicationInfo == null || applicationInfo.newInstalled == false) {
                        bubbleTextView.setText(applicationInfo.title);
                    } else {
                        SpannableString spannableString = new SpannableString(" " + ((Object) applicationInfo.title));
                        spannableString.setSpan(new ImageSpan(this.mNewInstallPrefix, 1), 0, 1, 17);
                        bubbleTextView.setText(spannableString);
                    }
                    LauncherModel.updateItemInDatabase(getContext(), shortcutInfo);
                    z2 = true;
                }
                z3 = z2;
            } else if (!z && (childAt instanceof FolderIcon)) {
                FolderIcon folderIcon = (FolderIcon) childAt;
                if (folderIcon.updateAppInfo(applicationInfo)) {
                    folderIcon.invalidate();
                    z3 = true;
                }
            }
        }
        return z3;
    }

    public void setScreenId(long screenId) {
        this.mScreenId = screenId;
    }

    public final long getScreenId() {
        return this.mScreenId;
    }

    public View getAppChildAt(int index) {
        return mShortcutsAndWidgets.getChildAt(index);
    }

    public boolean removeItemInfoAndUpdateDB(ItemInfo itemInfo, boolean z) {
        if (itemInfo == null) {
            return false;
        }
        ArrayList arrayList = new ArrayList();
        int childCount = getAppChildCount();

        for (int i = 0; i < childCount; i ++) {
            View childAt = mShortcutsAndWidgets.getChildAt(i);
            ItemInfo info = (ItemInfo) childAt.getTag();

            if (!(childAt instanceof BubbleTextView)) {
                if (!z && (childAt instanceof FolderIcon)) {
                    FolderInfo folderInfo = (FolderInfo) childAt.getTag();
                    if (folderInfo.contents.contains(itemInfo)) {
                        folderInfo.remove((ShortcutInfo) itemInfo);
                        LauncherModel.deleteItemFromDatabase(this.mLauncher, itemInfo);
                        break;
                    }
                }
            } else {
                if (info.id == itemInfo.id) {
                    arrayList.add(childAt);
                    break;
                }
            }
        }

        int size = arrayList.size();
        for (int i2 = 0; i2 < size; i2++) {
            View view = (View) arrayList.get(i2);
            this.addAlignCell(view);
            removeView(view);
            LauncherModel.deleteItemFromDatabase(this.mLauncher, (ItemInfo) view.getTag());
        }
        return true;
    }

    public boolean removeItem(ItemInfo itemInfo, boolean z) {
        if (itemInfo == null) {
            return false;
        }
        ArrayList arrayList = new ArrayList();
        int childCount = getAppChildCount();

        for (int i = 0; i < childCount; i ++) {
            View childAt = mShortcutsAndWidgets.getChildAt(i);
            ItemInfo info = (ItemInfo) childAt.getTag();
            if (info == null)
                continue;

            if (!(childAt instanceof BubbleTextView)) {
                if (!z && (childAt instanceof FolderIcon)) {
                    FolderInfo folderInfo = (FolderInfo) childAt.getTag();
                    for (ShortcutInfo s: folderInfo.contents) {
                        if (s.id == itemInfo.id) {
                            folderInfo.remove(s);
                            break;
                        }
                    }
                }
            } else {
                if (info.id == itemInfo.id) {
                    arrayList.add(childAt);
                    break;
                }
            }
        }

        int size = arrayList.size();
        for (int i2 = 0; i2 < size; i2++) {
            View view = (View) arrayList.get(i2);
            removeView(view);
        }
        return true;
    }
}

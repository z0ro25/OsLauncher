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
import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.TransitionDrawable;

import androidx.core.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.amz.ios.launcher.BaseFolderIcon.FolderRingAnimator;
import com.amz.ios.launcher.accessibility.DragAndDropAccessibilityDelegate;
import com.amz.ios.launcher.util.Thunk;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class BaseCellLayout extends ViewGroup {

    private static final String TAG = "BaseCellLayout";

    public static final int WORKSPACE_ACCESSIBILITY_DRAG = 2;
    public static final int FOLDER_ACCESSIBILITY_DRAG = 1;

    protected Launcher mLauncher;

    //in small screen, is add-page?
    protected boolean mIsNullScreen = false;

    @Thunk
    public int mCellWidth;
    @Thunk
    public int mCellHeight;

    protected int mFixedCellWidth;
    protected int mFixedCellHeight;

    @Thunk
    public int mCountX;
    @Thunk
    public int mCountY;

    protected int mOriginalWidthGap;
    protected int mOriginalHeightGap;
    @Thunk
    protected  int mWidthGap;
    @Thunk
    protected int mHeightGap;
    protected int mMaxGap;
    protected boolean mDropPending = false;
    protected boolean mIsDragTarget = true;

    // These are temporary variables to prevent having to allocate a new object just to
    // return an (x, y) value from helper functions. Do NOT use them to maintain other state.
    @Thunk
    protected final int[] mTmpPoint = new int[2];
    @Thunk
    protected final int[] mTempLocation = new int[2];

    protected boolean[][] mOccupied;
    protected boolean[][] mTmpOccupied;

    protected OnTouchListener mInterceptTouchListener;
    protected StylusEventHelper mStylusEventHelper;

    protected ArrayList<FolderRingAnimator> mFolderOuterRings = new ArrayList<FolderRingAnimator>();

    protected float mBackgroundAlpha;

    protected static final int BACKGROUND_ACTIVATE_DURATION = 120;
    protected TransitionDrawable mBackground;

    // These values allow a fixed measurement to be set on the CellLayout.
    protected int mFixedWidth = -1;
    protected int mFixedHeight = -1;

    // If we're actively dragging something over this screen, mIsDragOverlapping is true
    protected boolean mIsDragOverlapping = false;

    // These arrays are used to implement the drag visualization on x-large screens.
    // They are used as circular arrays, indexed by mDragOutlineCurrent.
    @Thunk
    protected Rect[] mDragOutlines = new Rect[4];
    @Thunk
    protected float[] mDragOutlineAlphas = new float[mDragOutlines.length];
    protected InterruptibleInOutAnimator[] mDragOutlineAnims =
            new InterruptibleInOutAnimator[mDragOutlines.length];

    // Used as an index into the above 3 arrays; indicates which is the most current value.
    protected int mDragOutlineCurrent = 0;
    protected final Paint mDragOutlinePaint = new Paint();

    protected ClickShadowView mTouchFeedbackView;

    @Thunk
    protected HashMap<BaseCellLayout.LayoutParams, Animator> mReorderAnimators = new HashMap<>();
    @Thunk
    protected HashMap<View, CellLayout.ReorderPreviewAnimation> mShakeAnimators = new HashMap<>();

    protected boolean mItemPlacementDirty = false;

    // When a drag operation is in progress, holds the nearest cell to the touch point
    protected final int[] mDragCell = new int[2];

    protected boolean mDragging = false;

    protected TimeInterpolator mEaseOutInterpolator;
    public ShortcutAndWidgetContainer mShortcutsAndWidgets;
    protected ImageView mAddMarkView;

    protected boolean mIsHotseat = false;
    protected float mHotseatScale = 1f;

    protected boolean mIsFolder = false;
    protected boolean mIsFull;

    public static final int MODE_SHOW_REORDER_HINT = 0;
    public static final int MODE_DRAG_OVER = 1;
    public static final int MODE_ON_DROP = 2;
    public static final int MODE_ON_DROP_EXTERNAL = 3;
    public static final int MODE_ACCEPT_DROP = 4;

    protected static final boolean DESTRUCTIVE_REORDER = false;
    protected static final boolean DEBUG_VISUALIZE_OCCUPIED = false;

    public static final int LANDSCAPE = 0;
    public static final int PORTRAIT = 1;

    protected static final float REORDER_PREVIEW_MAGNITUDE = 0.12f;
    protected static final int REORDER_ANIMATION_DURATION = 150;
    protected static final int ALIGN_ANIMATION_DURATION = 200;
    protected static final int ALIGN_VIEW_ANIM_DELAY = 20;
    @Thunk
    protected float mReorderPreviewAnimationMagnitude;

    protected ArrayList<View> mIntersectingViews = new ArrayList<View>();
    protected Rect mOccupiedRect = new Rect();
    protected int[] mDirectionVector = new int[2];
    protected int[] mPreviousReorderDirection = new int[2];
    protected static final int INVALID_DIRECTION = -100;

    protected final Rect mTempRect = new Rect();

    protected final static Paint sPaint = new Paint();

    // Related to accessible drag and drop
    protected DragAndDropAccessibilityDelegate mTouchHelper;
    protected boolean mUseTouchHelper = false;

    protected final ArrayList<int[]> mPendingAlignCells = new ArrayList<>();

    public BaseCellLayout(Context context) {
        super(context);
    }

    public BaseCellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseCellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    // This class stores info for two purposes:
    // 1. When dragging items (mDragInfo in Workspace), we store the View, its cellX & cellY,
    //    its spanX, spanY, and the screen it is on
    // 2. When long clicking on an empty cell in a CellLayout, we save information about the
    //    cellX and cellY coordinates and which page was clicked. We then set this as a tag on
    //    the CellLayout that was long clicked
    public static final class CellInfo {
        public View cell;
        public int cellX = -1;
        public int cellY = -1;
        public int spanX;
        public int spanY;
        public long screenId;
        public long container;

        public CellInfo(View v, ItemInfo info) {
            cell = v;
            cellX = info.cellX;
            cellY = info.cellY;
            spanX = info.spanX;
            spanY = info.spanY;
            screenId = info.screenId;
            container = info.container;
        }

        @Override
        public String toString() {
            return "Cell[view=" + (cell == null ? "null" : cell.getClass())
                    + ", x=" + cellX + ", y=" + cellY + "]";
        }
    }

    /*************************************************************
     * LayoutParams Class
     **************************************************************/
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        /**
         * Horizontal location of the item in the grid.
         */
        @ViewDebug.ExportedProperty
        public int cellX;

        /**
         * Vertical location of the item in the grid.
         */
        @ViewDebug.ExportedProperty
        public int cellY;

        /**
         * Temporary horizontal location of the item in the grid during reorder
         */
        public int tmpCellX;

        /**
         * Temporary vertical location of the item in the grid during reorder
         */
        public int tmpCellY;

        /**
         * Indicates that the temporary coordinates should be used to layout the items
         */
        public boolean useTmpCoords;

        /**
         * Number of cells spanned horizontally by the item.
         */
        @ViewDebug.ExportedProperty
        public int cellHSpan;

        /**
         * Number of cells spanned vertically by the item.
         */
        @ViewDebug.ExportedProperty
        public int cellVSpan;

        /**
         * Indicates whether the item will set its x, y, width and height parameters freely,
         * or whether these will be computed based on cellX, cellY, cellHSpan and cellVSpan.
         */
        public boolean isLockedToGrid = true;

        /**
         * Indicates that this item should use the full extents of its parent.
         */
        public boolean isFullscreen = false;

        /**
         * Indicates whether this item can be reordered. Always true except in the case of the
         * the AllApps button.
         */
        public boolean canReorder = true;

        // X coordinate of the view in the layout.
        @ViewDebug.ExportedProperty
        int x;
        // Y coordinate of the view in the layout.
        @ViewDebug.ExportedProperty
        int y;

        boolean dropped;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            cellHSpan = 1;
            cellVSpan = 1;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            cellHSpan = 1;
            cellVSpan = 1;
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            this.cellX = source.cellX;
            this.cellY = source.cellY;
            this.cellHSpan = source.cellHSpan;
            this.cellVSpan = source.cellVSpan;
        }

        public LayoutParams(int cellX, int cellY, int cellHSpan, int cellVSpan) {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            this.cellX = cellX;
            this.cellY = cellY;
            this.cellHSpan = cellHSpan;
            this.cellVSpan = cellVSpan;
        }

        public void setup(int cellWidth, int cellHeight, int widthGap, int heightGap,
                          boolean invertHorizontally, int colCount) {
            if (isLockedToGrid) {
                final int myCellHSpan = cellHSpan;
                final int myCellVSpan = cellVSpan;
                int myCellX = useTmpCoords ? tmpCellX : cellX;
                int myCellY = useTmpCoords ? tmpCellY : cellY;

                if (invertHorizontally) {
                    myCellX = colCount - myCellX - cellHSpan;
                }

                width = myCellHSpan * cellWidth + ((myCellHSpan - 1) * widthGap) -
                        leftMargin - rightMargin;
                height = myCellVSpan * cellHeight + ((myCellVSpan - 1) * heightGap) -
                        topMargin - bottomMargin;
                x = myCellX * (cellWidth + widthGap) + leftMargin;
                y = myCellY * (cellHeight + heightGap) + topMargin;
            }
        }

        public String toString() {
            return "(" + this.cellX + ", " + this.cellY + ")";
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getWidth() {
            return width;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getHeight() {
            return height;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getX() {
            return x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getY() {
            return y;
        }
    }

    /**
     * Given a point, return the cell that strictly encloses that point
     *
     * @param x      X coordinate of the point
     * @param y      Y coordinate of the point
     * @param result Array of 2 ints to hold the x and y coordinate of the cell
     */
    public void pointToCellExact(int x, int y, int[] result) {
        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();

        result[0] = (x - hStartPadding) / (mCellWidth + mWidthGap);
        result[1] = (y - vStartPadding) / (mCellHeight + mHeightGap);

        final int xAxis = mCountX;
        final int yAxis = mCountY;

        if (result[0] < 0) result[0] = 0;
        if (result[0] >= xAxis) result[0] = xAxis - 1;
        if (result[1] < 0) result[1] = 0;
        if (result[1] >= yAxis) result[1] = yAxis - 1;
    }

    /**
     * Given a point, return the cell that most closely encloses that point
     *
     * @param x      X coordinate of the point
     * @param y      Y coordinate of the point
     * @param result Array of 2 ints to hold the x and y coordinate of the cell
     */
    protected void pointToCellRounded(int x, int y, int[] result) {
        pointToCellExact(x + (mCellWidth / 2), y + (mCellHeight / 2), result);
    }

    /**
     * Given a cell coordinate, return the point that represents the upper left corner of that cell
     *
     * @param cellX  X coordinate of the cell
     * @param cellY  Y coordinate of the cell
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    protected void cellToPoint(int cellX, int cellY, int[] result) {
        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();

        result[0] = hStartPadding + cellX * (mCellWidth + mWidthGap);
        result[1] = vStartPadding + cellY * (mCellHeight + mHeightGap);
    }

    /**
     * Given a cell coordinate, return the point that represents the center of the cell
     *
     * @param cellX  X coordinate of the cell
     * @param cellY  Y coordinate of the cell
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    protected void cellToCenterPoint(int cellX, int cellY, int[] result) {
        regionToCenterPoint(cellX, cellY, 1, 1, result);
    }

    /**
     * Given a cell coordinate and span return the point that represents the center of the regio
     *
     * @param cellX  X coordinate of the cell
     * @param cellY  Y coordinate of the cell
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    protected void regionToCenterPoint(int cellX, int cellY, int spanX, int spanY, int[] result) {
        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();
        result[0] = hStartPadding + cellX * (mCellWidth + mWidthGap) +
                (spanX * mCellWidth + (spanX - 1) * mWidthGap) / 2;
        result[1] = vStartPadding + cellY * (mCellHeight + mHeightGap) +
                (spanY * mCellHeight + (spanY - 1) * mHeightGap) / 2;
    }

    /**
     * Given a cell coordinate and span fills out a corresponding pixel rect
     *
     * @param cellX  X coordinate of the cell
     * @param cellY  Y coordinate of the cell
     * @param result Rect in which to write the result
     */
    protected void regionToRect(int cellX, int cellY, int spanX, int spanY, Rect result) {
        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();
        final int left = hStartPadding + cellX * (mCellWidth + mWidthGap);
        final int top = vStartPadding + cellY * (mCellHeight + mHeightGap);
        result.set(left, top, left + (spanX * mCellWidth + (spanX - 1) * mWidthGap),
                top + (spanY * mCellHeight + (spanY - 1) * mHeightGap));
    }

    public float getDistanceFromCell(float x, float y, int[] cell) {
        cellToCenterPoint(cell[0], cell[1], mTmpPoint);
        return (float) Math.hypot(x - mTmpPoint[0], y - mTmpPoint[1]);
    }

    public int getCellWidth() {
        return mCellWidth;
    }

    protected int getCellHeight() {
        return mCellHeight;
    }

    protected int getWidthGap() {
        return mWidthGap;
    }

    protected int getHeightGap() {
        return mHeightGap;
    }

    public void setFixedSize(int width, int height) {
        mFixedWidth = width;
        mFixedHeight = height;
    }

    public boolean updateAppInfo(AppInfo applicationInfo, boolean z) {
        return false;
    }

    public void setNullScreen(boolean isNullScreen) {
        mIsNullScreen = isNullScreen;
    }

    public boolean isNullScreen() {
        return mIsNullScreen;
    }
}

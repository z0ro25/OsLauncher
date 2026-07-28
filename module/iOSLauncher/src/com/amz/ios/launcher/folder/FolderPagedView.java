/**
 * Copyright (C) 2015 The Android Open Source Project
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amz.ios.launcher.folder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.amz.ios.launcher.BubbleTextView;
import com.amz.ios.launcher.CellLayout;
import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.DragController;
import com.amz.ios.launcher.DragSource;
import com.amz.ios.launcher.DragView;
import com.amz.ios.launcher.DropTarget;
import com.amz.ios.launcher.FolderIcon;
import com.amz.ios.launcher.HolographicOutlineHelper;
import com.amz.ios.launcher.ItemInfo;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.PreloadIconDrawable;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.dragndrop.DragOptions;
import com.amz.ios.launcher.popup.PopupContainerWithArrow;
import com.amz.ios.launcher.views.CustomTextView;

import java.util.concurrent.atomic.AtomicInteger;

public class FolderPagedView extends BaseFolderPagedView {

    private static final String TAG = "FolderPagedView";

    private CellLayout mDragTargetLayout = null;
    Bitmap mDragOutline = null;
    float[] mDragViewVisualCenter = new float[2];
    int[] mTargetCell = new int[2];
    private final int[] mTempXY = new int[2];
    private CellLayout.CellInfo mDragInfo;
    public static final int DRAG_BITMAP_PADDING = 2;
    private final Canvas mCanvas = new Canvas();
    private DragController mDragController;
    private HolographicOutlineHelper mOutlineHelper;
    private static final Rect sTempRect = new Rect();

    public FolderPagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mOutlineHelper = HolographicOutlineHelper.obtain(context);
    }

    void setup(DragController dragController) {
        mDragController = dragController;
    }

    public void startDrag(CellLayout.CellInfo cellInfo, boolean accessible) {
        mDragInfo = cellInfo;
    }

    public void onDragEnter(DropTarget.DragObject d) {
        CellLayout layout = getCurrentDropLayout();
        setCurrentDropLayout(layout);
    }

    public void onDragOver(DropTarget.DragObject d, float center[]) {

        if (mDragTargetLayout == null)
            return;

        ItemInfo item = (ItemInfo) d.dragInfo;
        final View child = (mDragInfo == null) ? null : mDragInfo.cell;
        mDragViewVisualCenter[0] = center[0]; mDragViewVisualCenter[1] = center[1];

        int minSpanX = item.spanX;
        int minSpanY = item.spanY;
        if (item.minSpanX > 0 && item.minSpanY > 0) {
            minSpanX = item.minSpanX;
            minSpanY = item.minSpanY;
        }

        mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
                (int) mDragViewVisualCenter[1], minSpanX, minSpanY,
                mDragTargetLayout, mTargetCell);

        mDragTargetLayout.visualizeDropLocation(child, mDragOutline,
                (int) mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1],
                mTargetCell[0], mTargetCell[1], item.spanX, item.spanY, false,
                d.dragView.getDragVisualizeOffset(), d.dragView.getDragRegion());
    }

    void setCurrentDropLayout(CellLayout layout) {
        if (mDragTargetLayout != null) {
            mDragTargetLayout.revertTempState();
            mDragTargetLayout.onDragExit();
        }
        mDragTargetLayout = layout;
        if (mDragTargetLayout != null) {
            mDragTargetLayout.onDragEnter();
        }
    }

    public void onDragExit(DropTarget.DragObject d) {
        setCurrentDropLayout(null);
    }

    public void beginDragShared(View child, DragSource source, boolean accessible) {
        beginDragShared(child, new Point(), source, accessible, new DragOptions());
    }

    public DragView beginDragShared(View child, Point relativeTouchPos, DragSource source,
                                    boolean accessible, DragOptions dragOptions) {
        child.clearFocus();
        child.setPressed(false);

        AtomicInteger padding = null;
        Bitmap b = null;
        int bmpWidth;
        int bmpHeight;
        float scale;
        int dragLayerX;
        int dragLayerY;

        Launcher launcher = Launcher.getLauncher(getContext());

        if (dragOptions.previewProvider != null) {
            mDragOutline = dragOptions.previewProvider.createDragOutline(mCanvas);
            b = dragOptions.previewProvider.createDragBitmap(mCanvas);
            padding = new AtomicInteger(dragOptions.previewProvider.previewPadding);
            bmpWidth = b.getWidth();
            bmpHeight = b.getHeight();
            scale = dragOptions.previewProvider.getScaleAndPosition(b, mTempXY);
            dragLayerX = mTempXY[0];
            dragLayerY = mTempXY[1];
        } else {
            // The outline is used to visualize where the item will land if dropped
            mDragOutline = createDragOutline(child, DRAG_BITMAP_PADDING);

            // The drag bitmap follows the touch point around on the screen
            padding = new AtomicInteger(DRAG_BITMAP_PADDING);
            b = createDragBitmap(child, padding);
            bmpWidth = b.getWidth();
            bmpHeight = b.getHeight();

            scale = launcher.getDragLayer().getLocationInDragLayer(child, mTempXY);
            dragLayerX = Math.round(mTempXY[0] - (bmpWidth - scale * child.getWidth()) / 2);
            dragLayerY = Math.round(mTempXY[1] - (bmpHeight - scale * bmpHeight) / 2
                    - padding.get() / 2);
        }

        DeviceProfile grid = launcher.getDeviceProfile();
        Point dragVisualizeOffset = null;
        Rect dragRect = null;
        if (child instanceof BubbleTextView) {
            BubbleTextView icon = (BubbleTextView) child;
            int iconSize = grid.iconSizePx;
            int top = child.getPaddingTop();
            int left = (bmpWidth - iconSize) / 2;
            int right = left + iconSize;
            int bottom = top + iconSize;
            if (icon.isLayoutHorizontal()) {
                // If the layout is horizontal, then if we are just picking up the icon, then just
                // use the child position since the icon is top-left aligned.  Otherwise, offset
                // the drag layer position horizontally so that the icon is under the current
                // touch position.
                if (icon.getIcon().getBounds().contains(relativeTouchPos.x, relativeTouchPos.y)) {
                    dragLayerX = Math.round(mTempXY[0]);
                } else {
                    dragLayerX = Math.round(mTempXY[0] + relativeTouchPos.x - (bmpWidth / 2));
                }
            }
            dragLayerY += top;
            // Note: The drag region is used to calculate drag layer offsets, but the
            // dragVisualizeOffset in addition to the dragRect (the size) to position the outline.
            dragVisualizeOffset = new Point(-padding.get() / 2, padding.get() / 2);
            dragRect = new Rect(left, top, right, bottom);
        } else if (child instanceof FolderIcon) {
            int previewSize = grid.folderIconSizePx;
            dragVisualizeOffset = new Point(-padding.get() / 2,
                    padding.get() / 2 - child.getPaddingTop());
            dragRect = new Rect(0, child.getPaddingTop(), child.getWidth(), previewSize);
        } else if (dragOptions.previewProvider != null) {
            dragVisualizeOffset = new Point(- padding.get() / 2, padding.get() / 2);
        }

        // Clear the pressed state if necessary
        if (child instanceof BubbleTextView) {
            BubbleTextView icon = (BubbleTextView) child;
            icon.clearPressedBackground();
        }

        if (child.getTag() == null || !(child.getTag() instanceof ItemInfo)) {
            String msg = "Drag started with a view that has no tag set. This "
                    + "will cause a crash (issue 11627249) down the line. "
                    + "View: " + child + "  tag: " + child.getTag();
            throw new IllegalStateException(msg);
        }

        if (dragOptions.isAccessibleDrag && child instanceof BubbleTextView && !accessible && ((ItemInfo)child.getTag()).container < 0) {
            PopupContainerWithArrow popupContainer = PopupContainerWithArrow
                    .showForIcon((BubbleTextView) child);
            if (popupContainer != null) {
                dragOptions.preDragCondition = popupContainer.createPreDragCondition();
            }
        }

        DragView dv = mDragController.startDrag(b, dragLayerX, dragLayerY, source, child.getTag(),
                DragController.DRAG_ACTION_MOVE, dragVisualizeOffset, dragRect, scale, accessible, dragOptions);
        dv.setIntrinsicIconScaleFactor(source.getIntrinsicIconScaleFactor());

        b.recycle();

        return dv;
    }

    int[] findNearestArea(int pixelX, int pixelY,
                          int spanX, int spanY, CellLayout layout, int[] recycle) {
        return layout.findNearestArea(pixelX, pixelY, spanX, spanY, recycle);
    }

    /**
     * Returns a new bitmap to be used as the object outline, e.g. to visualize the drop location.
     * Responsibility for the bitmap is transferred to the caller.
     */
    private Bitmap createDragOutline(View v, int padding) {
        final int outlineColor = getResources().getColor(R.color.outline_color);
        final Bitmap b = Bitmap.createBitmap(
                v.getWidth() + padding, v.getHeight() + padding, Bitmap.Config.ARGB_8888);

        mCanvas.setBitmap(b);
        drawDragView(v, mCanvas, padding);
        mOutlineHelper.applyExpensiveOutlineWithBlur(b, mCanvas, outlineColor, outlineColor);
        mCanvas.setBitmap(null);
        return b;
    }

    /**
     * Returns a new bitmap to be used as the object outline, e.g. to visualize the drop location.
     * Responsibility for the bitmap is transferred to the caller.
     */
    protected Bitmap createDragOutline(Bitmap orig, int padding, int w, int h,
                                       boolean clipAlpha) {
        final int outlineColor = getResources().getColor(R.color.outline_color);
        final Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(b);

        Rect src = new Rect(0, 0, orig.getWidth(), orig.getHeight());
        float scaleFactor = Math.min((w - padding) / (float) orig.getWidth(),
                (h - padding) / (float) orig.getHeight());
        int scaledWidth = (int) (scaleFactor * orig.getWidth());
        int scaledHeight = (int) (scaleFactor * orig.getHeight());
        Rect dst = new Rect(0, 0, scaledWidth, scaledHeight);

        // center the image
        dst.offset((w - scaledWidth) / 2, (h - scaledHeight) / 2);

        mCanvas.drawBitmap(orig, src, dst, null);
        mOutlineHelper.applyExpensiveOutlineWithBlur(b, mCanvas, outlineColor, outlineColor,
                clipAlpha);
        mCanvas.setBitmap(null);

        return b;
    }

    /**
     * Returns a new bitmap to show when the given View is being dragged around.
     * Responsibility for the bitmap is transferred to the caller.
     *
     * @param expectedPadding padding to add to the drag view. If a different padding was used
     *                        its value will be changed
     */
    public Bitmap createDragBitmap(View v, AtomicInteger expectedPadding) {
        Bitmap b;

        int padding = expectedPadding.get();
        if (v instanceof CustomTextView) {
            Drawable d = getTextViewIcon((CustomTextView) v);
            Rect bounds = getDrawableBounds(d);
            b = Bitmap.createBitmap(bounds.width() + padding,
                    bounds.height() + padding, Bitmap.Config.ARGB_8888);
            expectedPadding.set(padding - bounds.left - bounds.top);
        } else {
            b = Bitmap.createBitmap(
                    v.getWidth() + padding, v.getHeight() + padding, Bitmap.Config.ARGB_8888);
        }

        mCanvas.setBitmap(b);
        drawDragView(v, mCanvas, padding);
        mCanvas.setBitmap(null);

        return b;
    }

    /**
     * Returns the drawable for the given text view.
     */
    public static Drawable getTextViewIcon(CustomTextView tv) {
        final Drawable[] drawables = tv.getCompoundDrawables();
        for (int i = 0; i < drawables.length; i++) {
            if (drawables[i] != null) {
                return drawables[i];
            }
        }
        return null;
    }

    /*
     *
     * We call these methods (onDragStartedWithItemSpans/onDragStartedWithSize) whenever we
     * start a drag in Launcher, regardless of whether the drag has ever entered the Workspace
     *
     * These methods mark the appropriate pages as accepting drops (which alters their visual
     * appearance).
     *
     */
    private static Rect getDrawableBounds(Drawable d) {
        Rect bounds = new Rect();
        d.copyBounds(bounds);
        if (bounds.width() == 0 || bounds.height() == 0) {
            bounds.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        } else {
            bounds.offsetTo(0, 0);
        }
        if (d instanceof PreloadIconDrawable) {
            int inset = -((PreloadIconDrawable) d).getOutset();
            bounds.inset(inset, inset);
        }
        return bounds;
    }

    /**
     * Draw the View v into the given Canvas.
     *
     * @param v          the view to draw
     * @param destCanvas the canvas to draw on
     * @param padding    the horizontal and vertical padding to use when drawing
     */
    private static void drawDragView(View v, Canvas destCanvas, int padding) {
        final Rect clipRect = sTempRect;
        v.getDrawingRect(clipRect);

        boolean textVisible = false;

        destCanvas.save();
        if (v instanceof CustomTextView) {
            Drawable d = getTextViewIcon((CustomTextView) v);
            Rect bounds = getDrawableBounds(d);
            clipRect.set(0, 0, bounds.width() + padding, bounds.height() + padding);
            destCanvas.translate(padding / 2 - bounds.left, padding / 2 - bounds.top);
            d.draw(destCanvas);
        } else {
            if (v instanceof FolderIcon) {
                // For FolderIcons the text can bleed into the icon area, and so we need to
                // hide the text completely (which can't be achieved by clipping).
                if (((FolderIcon) v).getTextVisible()) {
                    ((FolderIcon) v).setTextVisible(false);
                    textVisible = true;
                }
            }
            destCanvas.translate(-v.getScrollX() + padding / 2, -v.getScrollY() + padding / 2);
            destCanvas.clipRect(clipRect);
            v.draw(destCanvas);

            // Restore text visibility of FolderIcon if necessary
            if (textVisible) {
                ((FolderIcon) v).setTextVisible(true);
            }
        }
        destCanvas.restore();
    }
}

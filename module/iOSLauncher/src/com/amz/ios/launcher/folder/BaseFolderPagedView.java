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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.amz.ios.launcher.AppInfo;
import com.amz.ios.launcher.BaseCellLayout;
import com.amz.ios.launcher.BubbleTextView;
import com.amz.ios.launcher.CellLayout;
import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.DragController;
import com.amz.ios.launcher.DragLayer;
import com.amz.ios.launcher.FocusIndicatorView;
import com.amz.ios.launcher.FolderIcon;
import com.amz.ios.launcher.BubbleTextView;
import com.amz.ios.launcher.IconCache;
import com.amz.ios.launcher.ItemInfo;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherModel;
import com.amz.ios.launcher.PageIndicator;
import com.amz.ios.launcher.PagedView;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.ShortcutAndWidgetContainer;
import com.amz.ios.launcher.ShortcutInfo;
import com.amz.ios.launcher.Utilities;
import com.amz.ios.launcher.Workspace.ItemOperator;
import com.amz.ios.launcher.config.Settings;
import com.amz.ios.launcher.folder.Folder;
import com.amz.ios.launcher.util.Thunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BaseFolderPagedView extends PagedView {

    private static final String TAG = "BaseFolderPagedView";

    private static final int MAX_CELL_X_COUNT = 3;
    private static final int MAX_CELL_Y_COUNT = 3;

    private static final boolean ALLOW_FOLDER_SCROLL = true;

    private static final int REORDER_ANIMATION_DURATION = 230;
    private static final int START_VIEW_REORDER_DELAY = 30;
    private static final float VIEW_REORDER_DELAY_FACTOR = 0.9f;

    private static final int PAGE_INDICATOR_ANIMATION_START_DELAY = 300;
    private static final int PAGE_INDICATOR_ANIMATION_STAGGERED_DELAY = 150;
    private static final int PAGE_INDICATOR_ANIMATION_DURATION = 400;

    // This value approximately overshoots to 1.5 times the original size.
    private static final float PAGE_INDICATOR_OVERSHOOT_TENSION = 4.9f;

    /**
     * Fraction of the width to scroll when showing the next page hint.
     */
    private static final float SCROLL_HINT_FRACTION = 0.07f;

    private static final int[] sTempPosArray = new int[2];

    public final boolean mIsRtl;

    private final LayoutInflater mInflater;
    private final IconCache mIconCache;

    @Thunk
    final HashMap<View, Runnable> mPendingAnimations = new HashMap<>();
    private final int mMaxItemsPerPage;

    private int mAllocatedContentSize;
    private int mFolderCellWidthPx;
    private int mFolderCellHeightPx;
    private int mGridCountX;
    private int mGridCountY;

    private Folder mFolder;
    private FocusIndicatorView mFocusIndicatorView;

    public BaseFolderPagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LauncherAppState app = LauncherAppState.getInstance();

        DeviceProfile grid = ((Launcher) getContext()).getDeviceProfile();
        mGridCountX = MAX_CELL_X_COUNT;
        mGridCountY = MAX_CELL_Y_COUNT;

        mMaxItemsPerPage = mGridCountX * mGridCountY;
        mFolderCellWidthPx = grid.folderCellWidthPx;
        mFolderCellHeightPx = grid.folderCellHeightPx;

        mInflater = LayoutInflater.from(context);
        mIconCache = app.getIconCache();

        mIsRtl = Utilities.isRtl(getResources());
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setFolder(Folder folder) {
        mFolder = folder;
        mFocusIndicatorView = (FocusIndicatorView) folder.findViewById(R.id.focus_indicator);
    }

    private void setupContentDimensions(int count) {
        mAllocatedContentSize = count;
        // Update grid size
        for (int i = getPageCount() - 1; i >= 0; i--) {
            getPageAt(i).setGridSize(mGridCountX, mGridCountY);
        }
    }

    /**
     * Binds items to the layout.
     *
     * @return list of items that could not be bound, probably because we hit the max size limit.
     */
    public ArrayList<ShortcutInfo> bindItems(ArrayList<ShortcutInfo> items) {
        ArrayList<View> icons = new ArrayList<View>();
        ArrayList<ShortcutInfo> extra = new ArrayList<ShortcutInfo>();

        for (ShortcutInfo item : items) {
            if (!ALLOW_FOLDER_SCROLL && icons.size() >= mMaxItemsPerPage) {
                extra.add(item);
            } else {
                icons.add(createNewView(item));
            }
        }
        arrangeChildren(icons, icons.size(), false);
        return extra;
    }

    /**
     * Create space for a new item at the end, and returns the rank for that item.
     * Also sets the current page to the last page.
     */
    public int allocateRankForNewItem(ShortcutInfo info) {
        int rank = getItemCount();
        ArrayList<View> views = new ArrayList<View>(mFolder.getItemsInReadingOrder());
        if (rank > views.size()) {
            rank = views.size();
        }
        views.add(rank, null);
        arrangeChildren(views, views.size(), false);
        setCurrentPage(rank / mMaxItemsPerPage);
        return rank;
    }

    public View createAndAddViewForRank(ShortcutInfo item, int rank) {
        View icon = createNewView(item);
        addViewForRank(icon, item, rank);
        return icon;
    }

    /**
     * Adds the {@param view} to the layout based on {@param rank} and updated the position
     * related attributes. It assumes that {@param item} is already attached to the view.
     */
    public void addViewForRank(View view, ShortcutInfo item, int rank) {
        int pagePos = rank % mMaxItemsPerPage;
        int pageNo = rank / mMaxItemsPerPage;

        item.rank = rank;
        item.cellX = pagePos % mGridCountX;
        item.cellY = pagePos / mGridCountX;

        BaseCellLayout.LayoutParams lp = (BaseCellLayout.LayoutParams) view.getLayoutParams();
        lp.cellX = item.cellX;
        lp.cellY = item.cellY;

        CellLayout page = getPageAt(pageNo);
        if (page == null) {
            page = createAndAddNewPage();
        }

        page.addViewToCellLayout(view, -1, mFolder.mLauncher.getViewIdForItem(item), lp, true);
    }

    public void addViewFoInfo(View view, ShortcutInfo item) {
        int pagePos = item.rank % mMaxItemsPerPage;
        int pageNo = item.rank / mMaxItemsPerPage;

        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) view.getLayoutParams();
        lp.cellX = item.cellX;
        lp.cellY = item.cellY;

        CellLayout page = getPageAt(pageNo);
        if (page == null) {
            page = createAndAddNewPage();
        }

        page.addViewToCellLayout(view, -1, mFolder.mLauncher.getViewIdForItem(item), lp, true);
    }

    public View createAndAddShortcutWithInfo(ShortcutInfo item) {
        View icon = createNewView(item);
        addViewFoInfo(icon, item);
        return icon;
    }

    @SuppressLint("InflateParams")
    public View createNewView(ShortcutInfo item) {
        final BubbleTextView textView = (BubbleTextView) mInflater.inflate(R.layout.folder_shortcut, null, false);
        mFolder.getFolderIcon().updateFolderUnreadNum(item.intent.getComponent(), item.unreadNum);
        textView.applyFromShortcutInfo(item, mIconCache);
        textView.setOnLongClickListener(mFolder);
        textView.setOnFocusChangeListener(mFocusIndicatorView);
        textView.setLayoutParams(new CellLayout.LayoutParams(
                item.cellX, item.cellY, item.spanX, item.spanY));
        return textView;
    }

    @Override
    public CellLayout getPageAt(int index) {
        return (CellLayout) getChildAt(index);
    }

    public void removeCellLayoutView(View view) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            getPageAt(i).removeView(view);
        }
    }

    public CellLayout getCurrentCellLayout() {
        return getPageAt(getNextPage());
    }

    private CellLayout createAndAddNewPage() {
        DeviceProfile grid = ((Launcher) getContext()).getDeviceProfile();
        CellLayout page = new CellLayout(getContext());
        page.setIsFolder(true);
        page.setFixedSize(300, 500);
        page.getShortcutsAndWidgets().setMotionEventSplittingEnabled(false);
        page.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        page.setInvertIfRtl(true);
        page.setGridSize(mGridCountX, mGridCountY);

        addView(page, -1, generateDefaultLayoutParams());
        return page;
    }

    @Override
    protected int getChildGap() {
        return getPaddingLeft() + getPaddingRight();
    }

    public void setFixedSize(int width, int height) {
        width -= (getPaddingLeft() + getPaddingRight());
        height -= (getPaddingTop() + getPaddingBottom());
        for (int i = getChildCount() - 1; i >= 0; i--) {
            ((CellLayout) getChildAt(i)).setFixedSize(width, height);
        }
    }

    /**
     * Updates position and rank of all the children in the view.
     * It essentially removes all views from all the pages and then adds them again in appropriate
     * page.
     *
     * @param list      the ordered list of children.
     * @param itemCount if greater than the total children count, empty spaces are left
     *                  at the end, otherwise it is ignored.
     */
    public void arrangeChildren(ArrayList<View> list, int itemCount) {
        arrangeChildren(list, itemCount, true);
    }

    public void arrangeChildren(ArrayList<View> list, int itemCount, boolean saveChanges) {
        ArrayList<CellLayout> pages = new ArrayList<CellLayout>();
        for (int i = 0; i < getChildCount(); i++) {
            CellLayout page = (CellLayout) getChildAt(i);
            page.removeAllViews();
            pages.add(page);
        }
        setupContentDimensions(itemCount);

        Iterator<CellLayout> pageItr = pages.iterator();
        CellLayout currentPage = null;

        int position = 0;
        int newX, newY, rank;

        rank = 0;
        for (int i = 0; i < itemCount; i++) {
            View v = list.size() > i ? list.get(i) : null;
            if (currentPage == null || position >= mMaxItemsPerPage) {
                // Next page
                if (pageItr.hasNext()) {
                    currentPage = pageItr.next();
                } else {
                    currentPage = createAndAddNewPage();
                }
                position = 0;
            }

            if (v != null) {
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v.getLayoutParams();
                newX = position % mGridCountX;
                newY = position / mGridCountX;
                ItemInfo info = (ItemInfo) v.getTag();
                if (info.cellX != newX || info.cellY != newY || info.rank != rank) {
                    info.cellX = newX;
                    info.cellY = newY;
                    info.rank = rank;
                    if (saveChanges) {
                        LauncherModel.addOrMoveItemInDatabase(getContext(), info, mFolder.mInfo.id, 0, info.cellX, info.cellY);
                    }
                }
                lp.cellX = info.cellX;
                lp.cellY = info.cellY;
                currentPage.addViewToCellLayout(v, -1, mFolder.mLauncher.getViewIdForItem(info), lp, true);

                if (rank < FolderIcon.NUM_ITEMS_IN_PREVIEW && v instanceof BubbleTextView) {
                    ((BubbleTextView) v).verifyHighRes();
                }
            }

            rank++;
            position++;
        }

        // Remove extra views.
        boolean removed = false;
        while (pageItr.hasNext()) {
            removeView(pageItr.next());
            removed = true;
        }
        if (removed) {
            setCurrentPage(0);
        }

        setEnableOverscroll(getPageCount() > 1);

        // Update footer
        if (mPageIndicator != null)
            mPageIndicator.setVisibility(getPageCount() > 1 ? View.VISIBLE : View.GONE);
    }

    public int getDesiredWidth() {
        return getPageCount() > 0 ?
                (getPageAt(0).getDesiredWidth() + getPaddingLeft() + getPaddingRight()) : 0;
    }

    public int getDesiredHeight() {
        return getPageCount() > 0 ?
                (getPageAt(0).getDesiredHeight() + getPaddingTop() + getPaddingBottom()) : 0;
    }

    public int getItemCount() {
        int lastPageIndex = getChildCount() - 1;
        if (lastPageIndex < 0) {
            // If there are no pages, nothing has yet been added to the folder.
            return 0;
        }
        return getPageAt(lastPageIndex).getShortcutsAndWidgets().getChildCount()
                + lastPageIndex * mMaxItemsPerPage;
    }

    /**
     * @return the rank of the cell nearest to the provided pixel position.
     */
    public int findNearestArea(int pixelX, int pixelY) {
        int pageIndex = getNextPage();
        CellLayout page = getPageAt(pageIndex);
        page.findNearestArea(pixelX, pixelY, 1, 1, sTempPosArray);
        /*
        if (mFolder.isLayoutRtl()) {
            sTempPosArray[0] = page.getCountX() - sTempPosArray[0] - 1;
        }

         */

        Log.d(TAG, "findNearestArea pixelX = " + pixelX + " pixelY = " + pixelY);
        Log.d(TAG, "findNearestArea x = " + sTempPosArray[0] + " y = " + sTempPosArray[1]);
        return Math.min(mAllocatedContentSize - 1,
                pageIndex * mMaxItemsPerPage + sTempPosArray[1] * mGridCountX + sTempPosArray[0]);
    }

    public boolean isFull() {
        return !ALLOW_FOLDER_SCROLL && getItemCount() >= mMaxItemsPerPage;
    }

    public View getLastItem() {
        if (getChildCount() < 1) {
            return null;
        }
        ShortcutAndWidgetContainer lastContainer = getCurrentCellLayout().getShortcutsAndWidgets();
        int lastRank = lastContainer.getChildCount() - 1;
        if (mGridCountX > 0) {
            return lastContainer.getChildAt(lastRank % mGridCountX, lastRank / mGridCountX);
        } else {
            return lastContainer.getChildAt(lastRank);
        }
    }

    /**
     * Iterates over all its items in a reading order.
     *
     * @return the view for which the operator returned true.
     */
    public View iterateOverItems(ItemOperator op) {
        for (int k = 0; k < getChildCount(); k++) {
            CellLayout page = getPageAt(k);
            for (int j = 0; j < page.getCountY(); j++) {
                for (int i = 0; i < page.getCountX(); i++) {
                    View v = page.getChildAt(i, j);
                    if ((v != null) && op.evaluate((ItemInfo) v.getTag(), v, this)) {
                        return v;
                    }
                }
            }
        }
        return null;
    }

    public String getAccessibilityDescription() {
        return String.format(getContext().getString(R.string.folder_opened),
                mGridCountX, mGridCountY);
    }

    /**
     * Sets the focus on the first visible child.
     */
    public void setFocusOnFirstChild() {
        View firstChild = getCurrentCellLayout().getChildAt(0, 0);
        if (firstChild != null) {
            firstChild.requestFocus();
        }
    }

    @Override
    protected void notifyPageSwitchListener() {
        super.notifyPageSwitchListener();
        if (mFolder != null) {
            mFolder.updateTextViewFocus();
        }
    }

    /**
     * Scrolls the current view by a fraction
     */
    public void showScrollHint(int direction) {
        float fraction = (direction == DragController.SCROLL_LEFT) ^ mIsRtl
                ? -SCROLL_HINT_FRACTION : SCROLL_HINT_FRACTION;
        int hint = (int) (fraction * getWidth());
        int scroll = getScrollForPage(getNextPage()) + hint;
        int delta = scroll - getScrollX();
        if (delta != 0) {
            mScroller.setInterpolator(new DecelerateInterpolator());
            mScroller.startScroll(getScrollX(), 0, delta, 0, DragController.SCROLL_DELAY);
            invalidate();
        }
    }

    public void clearScrollHint() {
        if (getScrollX() != getScrollForPage(getNextPage())) {
            snapToPage(getNextPage());
        }
    }

    /**
     * Finish animation all the views which are animating across pages
     */
    public void completePendingPageChanges() {
        if (!mPendingAnimations.isEmpty()) {
            HashMap<View, Runnable> pendingViews = new HashMap<>(mPendingAnimations);
            for (Map.Entry<View, Runnable> e : pendingViews.entrySet()) {
                e.getKey().animate().cancel();
                e.getValue().run();
            }
        }
    }

    public boolean rankOnCurrentPage(int rank) {
        int p = rank / mMaxItemsPerPage;
        return p == getNextPage();
    }

    @Override
    protected void onPageBeginMoving() {
        super.onPageBeginMoving();
        getVisiblePages(sTempPosArray);
        for (int i = sTempPosArray[0]; i <= sTempPosArray[1]; i++) {
            verifyVisibleHighResIcons(i);
        }
    }

    /**
     * Ensures that all the icons on the given page are of high-res
     */
    public void verifyVisibleHighResIcons(int pageNo) {
        CellLayout page = getPageAt(pageNo);
        if (page != null) {
            ShortcutAndWidgetContainer parent = page.getShortcutsAndWidgets();
            for (int i = parent.getChildCount() - 1; i >= 0; i--) {
                ((BubbleTextView) parent.getChildAt(i)).verifyHighRes();
            }
        }
    }

    public int getAllocatedContentSize() {
        return mAllocatedContentSize;
    }

    /**
     * Reorders the items such that the {@param empty} spot moves to {@param target}
     */
    public void realTimeReorder(int empty, int target) {
        Log.d(TAG, "empty = " + empty + " target = " + target);
        completePendingPageChanges();
        int delay = 0;
        float delayAmount = START_VIEW_REORDER_DELAY;

        // Animation only happens on the current page.
        int pageToAnimate = getNextPage();

        int pageT = target / mMaxItemsPerPage;
        int pagePosT = target % mMaxItemsPerPage;

        if (pageT != pageToAnimate) {
            Log.e(TAG, "Cannot animate when the target cell is invisible");
        }
        int pagePosE = empty % mMaxItemsPerPage;
        int pageE = empty / mMaxItemsPerPage;

        int startPos, endPos;
        int moveStart, moveEnd;
        int direction;

        if (target == empty) {
            // No animation
            return;
        } else if (target > empty) {
            // Items will move backwards to make room for the empty cell.
            direction = 1;

            // If empty cell is in a different page, move them instantly.
            if (pageE < pageToAnimate) {
                moveStart = empty;
                // Instantly move the first item in the current page.
                moveEnd = pageToAnimate * mMaxItemsPerPage;
                // Animate the 2nd item in the current page, as the first item was already moved to
                // the last page.
                startPos = 0;
            } else {
                moveStart = moveEnd = -1;
                startPos = pagePosE;
            }

            endPos = pagePosT;
        } else {
            // The items will move forward.
            direction = -1;

            if (pageE > pageToAnimate) {
                // Move the items immediately.
                moveStart = empty;
                // Instantly move the last item in the current page.
                moveEnd = (pageToAnimate + 1) * mMaxItemsPerPage - 1;

                // Animations start with the second last item in the page
                startPos = mMaxItemsPerPage - 1;
            } else {
                moveStart = moveEnd = -1;
                startPos = pagePosE;
            }

            endPos = pagePosT;
        }

        // Instant moving views.
        while (moveStart != moveEnd) {
            int rankToMove = moveStart + direction;
            int p = rankToMove / mMaxItemsPerPage;
            int pagePos = rankToMove % mMaxItemsPerPage;
            int x = pagePos % mGridCountX;
            int y = pagePos / mGridCountX;

            final CellLayout page = getPageAt(p);
            final View v = page.getChildAt(x, y);
            if (v != null) {
                if (pageToAnimate != p) {
                    page.removeView(v);
                    addViewForRank(v, (ShortcutInfo) v.getTag(), moveStart);
                } else {
                    // Do a fake animation before removing it.
                    final int newRank = moveStart;
                    final float oldTranslateX = v.getTranslationX();

                    Runnable endAction = new Runnable() {

                        @Override
                        public void run() {
                            mPendingAnimations.remove(v);
                            v.setTranslationX(oldTranslateX);
                            ((CellLayout) v.getParent().getParent()).removeView(v);
                            addViewForRank(v, (ShortcutInfo) v.getTag(), newRank);
                        }
                    };
                    v.animate()
                            .translationXBy((direction > 0 ^ mIsRtl) ? -v.getWidth() : v.getWidth())
                            .setDuration(REORDER_ANIMATION_DURATION)
                            .setStartDelay(0)
                            .withEndAction(endAction);
                    mPendingAnimations.put(v, endAction);
                }
            }
            moveStart = rankToMove;
        }

        if ((endPos - startPos) * direction <= 0) {
            // No animation
            return;
        }

        CellLayout page = getPageAt(pageToAnimate);
        for (int i = startPos; i != endPos; i += direction) {
            int nextPos = i + direction;
            View v = page.getChildAt(nextPos % mGridCountX, nextPos / mGridCountX);
            if (v != null) {
                ((ItemInfo) v.getTag()).rank -= direction;
            }
            if (page.animateChildToPosition(v, i % mGridCountX, i / mGridCountX,
                    REORDER_ANIMATION_DURATION, delay, true, true)) {
                delay += delayAmount;
                delayAmount *= VIEW_REORDER_DELAY_FACTOR;
            }
        }
    }

    public void setMarkerScale(float scale) {
//        int count = mPageIndicator.getChildCount();
//        for (int i = 0; i < count; i++) {
//            View marker = mPageIndicator.getChildAt(i);
//            marker.animate().cancel();
//            marker.setScaleX(scale);
//            marker.setScaleY(scale);
//        }
    }

    public void animateMarkers() {
//        int count = mPageIndicator.getChildCount();
//        Interpolator interpolator = new OvershootInterpolator(PAGE_INDICATOR_OVERSHOOT_TENSION);
//        for (int i = 0; i < count; i++) {
//            mPageIndicator.getChildAt(i).animate().scaleX(1).scaleY(1)
//                    .setInterpolator(interpolator)
//                    .setDuration(PAGE_INDICATOR_ANIMATION_DURATION)
//                    .setStartDelay(PAGE_INDICATOR_ANIMATION_STAGGERED_DELAY * i
//                            + PAGE_INDICATOR_ANIMATION_START_DELAY);
//        }
    }

    public int itemsPerPage() {
        return mMaxItemsPerPage;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public final void smoothScrollToFirstPage() {
        snapToPage(0);
    }

    public void cancelDragScroll() {
    }

    public void setContentCellDimension() {}

    public void beginShakeAnimations() {
        int currentPage = getCurrentPage();
        int childCount = getChildCount();

        for (int i = 0; i < childCount; i++) {
            CellLayout cellLayout = (CellLayout) getChildAt(i);
            if (cellLayout != null) {
                if (i == currentPage)
                    cellLayout.beginShakeAnimations();
                else
                    cellLayout.stopShakeAnimations();
            }
        }
    }

    public void stopShakeAnimations() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            CellLayout cellLayout = (CellLayout) getChildAt(i);
            if (cellLayout != null) {
                cellLayout.stopShakeAnimations();
            }
        }
    }

    public BubbleTextView getShortcutViewAt(int index) {
        int childCount = getChildCount();
        int count = 0;

        for (int i = 0; i < childCount; i++) {
            CellLayout cellLayout = (CellLayout) getChildAt(i);
            if (cellLayout != null) {
                int iconCount = cellLayout.getAppChildCount();
                for (int j = 0; j < iconCount; j ++) {
                    View findView = cellLayout.getAppChildAt(j);
                    if (count == index)
                        return (BubbleTextView) findView;
                    count ++;
                }
            }
        }

        return null;
    }

    public boolean updateAppInfo(AppInfo applicationInfo, boolean z) {
        return false;
    }

    public int getAppChildCount() {
        return getItemCount();
    }

    public void setShortcutItemViews(ArrayList<ShortcutInfo> items) {
        ArrayList<View> views = new ArrayList<>();
        Iterator<ShortcutInfo> it = items.iterator();
        while (it.hasNext()) {
            ShortcutInfo next = it.next();
            View view = createAndAddShortcutWithInfo(next);
            views.add(view);
        }

        arrangeChildren(views, views.size(), true);
    }

    public void addTempAddFolderItem(ArrayList<View> originalViews, ArrayList<ShortcutInfo> addedItems) {
        arrangeChildren(originalViews, originalViews.size(), false);
    }

    public void removeAppFromPage(View view) {
        int childCount = getChildCount();

        for (int i = 0; i < childCount; i++) {
            CellLayout cellLayout = (CellLayout) getChildAt(i);
            if (cellLayout != null) {
                int iconCount = cellLayout.getAppChildCount();
                for (int j = 0; j < iconCount; j ++) {
                    View findView = cellLayout.getAppChildAt(j);
                    if (findView == view) {
                        cellLayout.addAlignCell(findView);
                        cellLayout.removeView(findView);
                        cellLayout.completeAlign();
                        return;
                    }
                }
            }
        }

        Log.i(TAG, "FolderPagedView has not FolderItemView.");
    }

    public void removeItem(View v) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            getPageAt(i).removeView(v);
        }
    }

    public void removeAllPages() {
        ArrayList<CellLayout> pages = new ArrayList<>();
        for (int i = 0; i < this.getPageCount(); i ++) {
            CellLayout page = this.getPageAt(i);
            pages.add(page);
        }

        for (int i = 1; i < pages.size(); i ++) {
            this.removeView(pages.get(i));
        }

        CellLayout page = this.getPageAt(0);
        if (page != null) {
            page.removeAllViews();
        }
    }

    public boolean addViewToCellLayout(View child, int index, int childId, BaseCellLayout.LayoutParams params,
                                       boolean markCells) {
        int currentPage = getCurrentPage();
        CellLayout pageView = (CellLayout)getChildAt(currentPage);
        if (pageView == null) {
            Log.i(TAG, "addViewToCellLayout did not add child.");
            return false;
        }

        pageView.addViewToCellLayout(child, index, childId, params, markCells);
        return true;
    }

    int[] findNearestArea(int pixelX, int pixelY,
                          int spanX, int spanY, int[] recycle) {
        CellLayout layout = getCurrentCellLayout();
        return layout.findNearestArea(
                pixelX, pixelY, spanX, spanY, recycle);
    }

    public int getCountX() {
        CellLayout layout = getCurrentCellLayout();
        if (layout == null)
            return 0;

        return layout.getCountX();
    }

    public int getCountY() {
        CellLayout layout = getCurrentCellLayout();
        if (layout == null)
            return 0;

        return layout.getCountY();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (isLoopEnable()) {
            int scrollX = getScrollX();
            if (scrollX < 0 || scrollX > mMaxScrollX) {
                int pageCount = getChildCount();
                int offset = pageCount * getViewportWidth();
                long drawingTime = getDrawingTime();
                canvas.save();
                Rect clipRect = new Rect(getScrollX(), getScrollY(),
                        getScrollX() + getRight() - getLeft(),getScrollY() + getBottom() - getTop());
                Log.d(TAG, "dispatchDraw left = " + clipRect.left + ", width = " + clipRect.width() + ", height = " + clipRect.height());
                canvas.clipRect(clipRect);
                if (getScrollX() > mMaxScrollX) {
                    canvas.translate(offset, 0);
                    drawChild(canvas, getPageAt(0), drawingTime);
                } else if (getScrollX() < 0) {
                    canvas.translate(-offset, 0);
                    drawChild(canvas, getPageAt(pageCount - 1), drawingTime);
                }
                canvas.restore();
            }
        }
    }

    @Override
    protected PageIndicator.PageMarkerResources getPageIndicatorMarker(int pageIndex) {
        return new PageIndicator.PageMarkerResources(R.drawable.ic_pageindicator_current,
                R.drawable.ic_pageindicator_default);
    }

    protected void onPageEndMoving() {
        super.onPageEndMoving();

        if (DragLayer.sTidyUping)
            beginShakeAnimations();
    }

    public CellLayout getCurrentDropLayout() {
        return (CellLayout) getChildAt(getNextPage());
    }

    public View getViewForInfo(ShortcutInfo shortcutInfo) {
        int childCount = getChildCount();

        for (int i = 0; i < childCount; i++) {
            CellLayout cellLayout = (CellLayout) getChildAt(i);
            if (cellLayout != null) {
                int iconCount = cellLayout.getAppChildCount();
                for (int j = 0; j < iconCount; j ++) {
                    View childAt = cellLayout.getAppChildAt(j);
                    if (childAt != null && childAt.getTag() == shortcutInfo) {
                        return childAt;
                    }
                }
            }
        }

        return null;
    }

    public int getFirstChildPaddingLeft() {
        return getPaddingLeft();
    }

    public int getFirstChildPaddingTop() {
        return getPaddingTop();
    }

    protected boolean isLoopEnable() {
        return Settings.sLoopEnable && getPageCount() > 1;
    }
}

package com.amz.ios.launcher.folder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.analytics.UMEventConstants;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.launcher.Alarm;
import com.amz.ios.launcher.BubbleTextView;
import com.amz.ios.launcher.CellLayout;
import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.DragController;
import com.amz.ios.launcher.DragSource;
import com.amz.ios.launcher.DropTarget;
import com.amz.ios.launcher.FolderIcon;
import com.amz.ios.launcher.FolderInfo;
import com.amz.ios.launcher.IconCache;
import com.amz.ios.launcher.ItemInfo;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherModel;
import com.amz.ios.launcher.LauncherSettings;
import com.amz.ios.launcher.OnAlarmListener;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.ShortcutAndWidgetContainer;
import com.amz.ios.launcher.ShortcutInfo;
import com.amz.ios.launcher.UninstallDropTarget;
import com.amz.ios.launcher.Workspace;
import com.amz.ios.launcher.dragndrop.DragOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FolderLayout extends LinearLayout implements DropTarget, DragSource,
        FolderInfo.FolderListener, View.OnClickListener, View.OnLongClickListener,
        UninstallDropTarget.UninstallSource {
    private static final String TAG = "Launcher.FolderLayout";
    private Launcher mLauncher;
    private DeviceProfile mDeviceProfile;
    private final LayoutInflater mInflater;
    private final IconCache mIconCache;

    FolderInfo mInfo;
    FolderIcon mFolderIcon;

    private CellLayout mContent;
    private int mAllocatedContentSize;
    private int mGridCountX;
    private int mGridCountY;
    boolean mItemsInvalidated = false;
    final ArrayList<View> mItemsInReadingOrder = new ArrayList<View>();
    private static final int REORDER_DELAY = 250;
    private static final int START_VIEW_REORDER_DELAY = 30;
    private static final int REORDER_ANIMATION_DURATION = 230;
    private static final float VIEW_REORDER_DELAY_FACTOR = 0.9f;
    private boolean mDragInProgress = false;
    private boolean mSuppressOnAdd = false;
    private ShortcutInfo mCurrentDragInfo;
    private View mCurrentDragView;
    int mTargetRank, mPrevTargetRank, mEmptyCellRank;
    private boolean mItemAddedBackToSelfViaIcon = false;

    private static final int[] sTempPosArray = new int[2];
    private final Alarm mReorderAlarm = new Alarm();
    private Runnable mDeferredAction;
    private boolean mDeferDropAfterUninstall;
    private boolean mUninstallSuccessful;


    public FolderLayout(Context context) {
        this(context, null);
    }

    public FolderLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FolderLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = (Launcher) context;
        mInflater = LayoutInflater.from(context);

        LauncherAppState app = LauncherAppState.getInstance();
        mDeviceProfile = mLauncher.getDeviceProfile();
        mIconCache = app.getIconCache();
        mGridCountX = mDeviceProfile.inv.numFolderColumns;
        mGridCountY = mDeviceProfile.inv.numRows;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContent = (CellLayout) findViewById(R.id.folder_content);
        mContent.setIsFolder(true);
        mContent.setOnClickListener(this);
    }

    CellLayout getCellLayout() {
        return mContent;
    }

    public FolderInfo getFolderInfo() {
        return mInfo;
    }

    public void bind(FolderInfo folderInfo, FolderIcon icon) {
        mInfo = folderInfo;
        mFolderIcon = icon;

        ArrayList<ShortcutInfo> children = folderInfo.contents;
        Collections.sort(children, ITEM_POS_COMPARATOR);
        bindItems(children);

        mItemsInvalidated = true;
        mInfo.addListener(this);
    }

    private void bindItems(ArrayList<ShortcutInfo> items) {
        ArrayList<View> icons = new ArrayList<View>();

        for (ShortcutInfo item : items) {
            icons.add(createNewView(item));
        }
        arrangeChildren(icons, icons.size(), true);
    }

    /**
     * Create space for a new item at the end, and returns the rank for that item.
     * Also sets the current page to the last page.
     */
    private int allocateRankForNewItem(ShortcutInfo info) {
        int rank = getItemCount();
        ArrayList<View> views = new ArrayList<View>(getItemsInReadingOrder());
        views.add(rank, null);
        arrangeChildren(views, views.size(), false);
        return rank;
    }

    @SuppressLint("InflateParams")
    private View createNewView(ShortcutInfo item) {
        final BubbleTextView textView = (BubbleTextView) mInflater.inflate(
                R.layout.folder_application, null, false);
        mFolderIcon.updateFolderUnreadNum(item.intent.getComponent(), item.unreadNum);
        textView.applyFromShortcutInfo(item, mIconCache);
        textView.setOnClickListener(this);
        textView.setOnLongClickListener(this);

        textView.setLayoutParams(new CellLayout.LayoutParams(
                item.cellX, item.cellY, item.spanX, item.spanY));
        return textView;
    }

    private View createAndAddViewForRank(ShortcutInfo item, int rank) {
        View icon = createNewView(item);
        addViewForRank(icon, item, rank);
        return icon;
    }

    /**
     * Adds the {@param view} to the layout based on {@param rank} and updated the position
     * related attributes. It assumes that {@param item} is already attached to the view.
     */
    private void addViewForRank(View view, ShortcutInfo item, int rank) {
        item.rank = rank;
        item.cellX = rank % mGridCountX;
        item.cellY = rank / mGridCountX;

        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) view.getLayoutParams();
        lp.cellX = item.cellX;
        lp.cellY = item.cellY;
        mContent.addViewToCellLayout(
                view, -1, mLauncher.getViewIdForItem(item), lp, true);
    }

    private void arrangeChildren(ArrayList<View> list, int itemCount, boolean saveChanges) {
        mContent.removeAllViews();
        setupContentDimensions(itemCount);

        int position = 0;
        int newX, newY, rank;
        rank = 0;
        for (int i = 0; i < itemCount; i++) {
            View v = list.size() > i ? list.get(i) : null;
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
                        LauncherModel.addOrMoveItemInDatabase(getContext(), info,
                                mInfo.id, 0, info.cellX, info.cellY);
                    }
                }
                lp.cellX = info.cellX;
                lp.cellY = info.cellY;
                mContent.addViewToCellLayout(
                        v, -1, mLauncher.getViewIdForItem(info), lp, true);

                if (rank < FolderIcon.NUM_ITEMS_IN_PREVIEW && v instanceof BubbleTextView) {
                    ((BubbleTextView) v).verifyHighRes();
                }
            }

            rank++;
            position++;
        }
    }

    public void rearrangeChildren() {
        ArrayList<View> views = getItemsInReadingOrder();
        arrangeChildren(views, views.size());
        mItemsInvalidated = true;
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

    public void notifyDrop() {
        if (mDragInProgress) {
            mItemAddedBackToSelfViaIcon = true;
        }
    }

    // This is used so the item doesn't immediately appear in the folder when added. In one case
    // we need to create the illusion that the item isn't added back to the folder yet, to
    // to correspond to the animation of the icon back into the folder. This is
    public void hideItem(ShortcutInfo info) {
        View v = getViewForInfo(info);
        if (v != null) {
            v.setVisibility(INVISIBLE);
        }
    }

    public void showItem(ShortcutInfo info) {
        View v = getViewForInfo(info);
        if (v != null) {
            v.setVisibility(VISIBLE);
        }
    }

    private View getViewForInfo(final ShortcutInfo item) {
        return iterateOverItems(new Workspace.ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo info, View view, View parent) {
                return info == item;
            }
        });
    }

    /**
     * Iterates over all its items in a reading order.
     *
     * @return the view for which the operator returned true.
     */
    public View iterateOverItems(Workspace.ItemOperator op) {
        for (int j = 0; j < mContent.getCountY(); j++) {
            for (int i = 0; i < mContent.getCountX(); i++) {
                View v = mContent.getChildAt(i, j);
                if ((v != null) && op.evaluate((ItemInfo) v.getTag(), v, this)) {
                    return v;
                }
            }
        }
        return null;
    }


    private void setupContentDimensions(int count) {
        mAllocatedContentSize = count;
        if ((count % mGridCountX) != 0 || count / mGridCountX == 0) {
            mGridCountY = (count / mGridCountX) + 1;
        } else {
            mGridCountY = count / mGridCountX;
        }


        // Celllayout 父布局为Scrollview,会拦截touch事件且无法响应click事件;
        // 临时处理方案:  celllayout 设置大小填充满父布局, 处理click事件;
        int caculateFolderMinCountY = mDeviceProfile.caculateFolderMinCountY() - 1;
        if (mGridCountY < caculateFolderMinCountY) {
            mGridCountY = caculateFolderMinCountY;
            mContent.setIsFull(false);
        } else {
            mContent.setIsFull(true);
        }
        mContent.setGridSize(mGridCountX, mGridCountY);
    }

    public int getItemCount() {
        return mContent.getShortcutsAndWidgets().getChildCount();
    }

    public ArrayList<View> getItemsInReadingOrder() {
        if (mItemsInvalidated) {
            mItemsInReadingOrder.clear();
            iterateOverItems(new Workspace.ItemOperator() {

                @Override
                public boolean evaluate(ItemInfo info, View view, View parent) {
                    mItemsInReadingOrder.add(view);
                    return false;
                }
            });
            mItemsInvalidated = false;
        }
        return mItemsInReadingOrder;
    }

    @Override
    public void onAdd(ShortcutInfo item) {
        // If the item was dropped onto this open folder, we have done the work associated
        // with adding the item to the folder, as indicated by mSuppressOnAdd being set
        DebugLog.w(TAG, "===============onAdd:" + item);
        AnalyticsDelegate.onSmartSortEvent(getContext(), UMEventConstants.DESKTOP_FOLDER_APPS_ADD);
        if (mSuppressOnAdd)
            return;
        createAndAddViewForRank(item, allocateRankForNewItem(item));
        mItemsInvalidated = true;
        LauncherModel.addOrMoveItemInDatabase(
                mLauncher, item, mInfo.id, 0, item.cellX, item.cellY);
    }

    @Override
    public void onRemove(ShortcutInfo item) {
        DebugLog.w(TAG, "===============onRemove:" + item);
        AnalyticsDelegate.onSmartSortEvent(getContext(), UMEventConstants.DESKTOP_FOLDER_APPS_REMOVE);
        mItemsInvalidated = true;
        // If this item is being dragged from this open folder, we have already handled
        // the work associated with removing the item, so we don't have to do anything here.
        if (item == mCurrentDragInfo)
            return;
        View v = getViewForInfo(item);
        mContent.removeView(v);
        rearrangeChildren();
        if (getItemCount() == 0) {
            removeFolder();
        }
    }

    @Override
    public void onTitleChanged(CharSequence title) {

    }

    @Override
    public void onItemsChanged() {

    }

    @Override
    public void onClick(View v) {
        Object tag = v.getTag();
        if (tag instanceof ShortcutInfo) {
            mLauncher.onClick(v);
        } else if (v instanceof CellLayout) {
            mLauncher.closeFolder();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        // Return if global dragging is not enabled
        if (!mLauncher.isDraggingEnabled())
            return true;
        return beginDrag(v, false);
    }

    private boolean beginDrag(View v, boolean accessible) {
        final Object tag = v.getTag();
        final DragController dragController = mLauncher.getDragController();
        if (!dragController.isDragging() && tag instanceof ShortcutInfo) {
            ShortcutInfo item = (ShortcutInfo) tag;
            if (!v.isInTouchMode()) {
                return false;
            }
            mLauncher.getWorkspace().beginDragShared(v, new Point(), this, accessible, new DragOptions());
            mCurrentDragInfo = item;
            mEmptyCellRank = item.rank;
            mCurrentDragView = v;

            mContent.removeView(mCurrentDragView);
            mInfo.remove(mCurrentDragInfo);
            mDragInProgress = true;
            mItemAddedBackToSelfViaIcon = false;
        }
        return true;
    }

    private void replaceFolderWithFinalItem() {
        // Add the last remaining child to the workspace in place of the folder
        Runnable onCompleteRunnable = new Runnable() {
            @Override
            public void run() {
                CellLayout cellLayout = mLauncher.getCellLayout(mInfo.container, mInfo.screenId);

                View child = null;
                // Move the item from the folder to the workspace, in the position of the folder
                if (getItemCount() == 1) {
                    ShortcutInfo finalItem = mInfo.contents.get(0);
                    child = mLauncher.createShortcut(cellLayout, finalItem);
                    LauncherModel.addOrMoveItemInDatabase(mLauncher, finalItem, mInfo.container,
                            mInfo.screenId, mInfo.cellX, mInfo.cellY);
                }
                if (getItemCount() <= 1) {
                    mContent.removeAllViews();
                    mItemsInvalidated = true;

                    // Remove the folder
                    LauncherModel.deleteItemFromDatabase(mLauncher, mInfo);
                    if (cellLayout != null) {
                        // b/12446428 -- sometimes the cell layout has already gone away?
                        cellLayout.removeView(mFolderIcon);
                    }
                    if (mFolderIcon instanceof DropTarget) {
                        final DragController dragController = mLauncher.getDragController();
                        dragController.removeDropTarget((DropTarget) mFolderIcon);
                    }
                }
                // We add the child after removing the folder to prevent both from existing at
                // the same time in the CellLayout.  We need to add the new item with addInScreenFromBind()
                // to ensure that hotseat items are placed correctly.
                if (child != null) {
                    mLauncher.getWorkspace().addInScreenFromBind(child, mInfo.container, mInfo.screenId,
                            mInfo.cellX, mInfo.cellY, mInfo.spanX, mInfo.spanY);
                }

                removeFolder();
            }
        };
        View finalChild = getLastItem();
        if (finalChild != null) {
            mFolderIcon.performDestroyAnimation(finalChild, onCompleteRunnable);
        } else {
            onCompleteRunnable.run();
        }
    }

    private View getLastItem() {
        if (getItemCount() < 1) {
            return null;
        }
        ShortcutAndWidgetContainer lastContainer = mContent.getShortcutsAndWidgets();
        int lastRank = lastContainer.getChildCount() - 1;
        if (mGridCountX > 0) {
            return lastContainer.getChildAt(lastRank % mGridCountX, lastRank / mGridCountX);
        } else {
            return lastContainer.getChildAt(lastRank);
        }
    }

    @Override
    public boolean acceptDrop(DragObject dragObject) {
        final ItemInfo item = (ItemInfo) dragObject.dragInfo;
        final int itemType = item.itemType;
        return (itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT ||
                itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT);
    }

    @Override
    public boolean isDropEnabled() {
        return true;
    }

    @Override
    public void onDrop(DragObject dragObject) {
        Runnable cleanUpRunnable = null;

        // If we are coming from All Apps space, we defer removing the extra empty screen
        // until the folder closes
        if (dragObject.dragSource != mLauncher.getWorkspace() && !(dragObject.dragSource instanceof FolderLayout)) {
            cleanUpRunnable = new Runnable() {
                @Override
                public void run() {
                    mLauncher.exitSpringLoadedDragModeDelayed(true,
                            Launcher.EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT,
                            null);
                }
            };
        }

        View currentDragView;
        ShortcutInfo si = mCurrentDragInfo;

        currentDragView = mCurrentDragView;
        addViewForRank(currentDragView, si, mEmptyCellRank);

        if (dragObject.dragView.hasDrawn()) {

            // Temporarily reset the scale such that the animation target gets calculated correctly.
            float scaleX = getScaleX();
            float scaleY = getScaleY();
            setScaleX(1.0f);
            setScaleY(1.0f);
            mLauncher.getDragLayer().animateViewIntoPosition(dragObject.dragView, currentDragView,
                    cleanUpRunnable, null);
            setScaleX(scaleX);
            setScaleY(scaleY);
        } else {
            dragObject.deferDragViewCleanupPostAnimation = false;
            currentDragView.setVisibility(VISIBLE);
        }

        mItemsInvalidated = true;
        rearrangeChildren();

        // Temporarily suppress the listener, as we did all the work already here.
        mSuppressOnAdd = true;
        mInfo.add(si);
        mSuppressOnAdd = false;
        // Clear the drag info, as it is no longer being dragged.
        mCurrentDragInfo = null;
        mDragInProgress = false;
    }

    @Override
    public void onDragEnter(DragObject dragObject) {
        mPrevTargetRank = -1;
    }

    @Override
    public void onDragOver(DragObject dragObject) {
        onDragOver(dragObject, REORDER_DELAY);
    }

    void onDragOver(DragObject d, int reorderDelay) {
        final float[] r = new float[2];
        mTargetRank = getTargetRank(d, r);
        if (mTargetRank != mPrevTargetRank) {
            mReorderAlarm.cancelAlarm();
            mReorderAlarm.setOnAlarmListener(mReorderAlarmListener);
            mReorderAlarm.setAlarm(reorderDelay);
            mPrevTargetRank = mTargetRank;
        }
    }


    private void clearDragInfo() {
        mCurrentDragInfo = null;
        mCurrentDragView = null;
        mSuppressOnAdd = false;
    }

    @Override
    public void onDragExit(DragObject dragObject) {
        mReorderAlarm.cancelAlarm();
    }

    @Override
    public void onFlingToDelete(DragObject dragObject, PointF vec) {
        // Do nothing
    }


    @Override
    public void prepareAccessibilityDrop() {

    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        mLauncher.getDragLayer().getDescendantRectRelativeToSelf(this, outRect);
    }

    @Override
    public void getLocationInDragLayer(int[] loc) {
        mLauncher.getDragLayer().getLocationInDragLayer(this, loc);
    }


    @Override
    public boolean supportsFlingToDelete() {
        return true;
    }

    @Override
    public boolean supportsAppInfoDropTarget() {
        return false;
    }

    @Override
    public boolean supportsDeleteDropTarget() {
        return true;
    }

    @Override
    public float getIntrinsicIconScaleFactor() {
        return 1f;
    }

    @Override
    public void onFlingToDeleteCompleted() {
        // Do nothing
    }

    @Override
    public void onDropCompleted(final View target, final DragObject d, final boolean isFlingToDelete, final boolean success) {
        if (mDeferDropAfterUninstall) {
            Log.d(TAG, "Deferred handling drop because waiting for uninstall.");
            mDeferredAction = new Runnable() {
                public void run() {
                    onDropCompleted(target, d, isFlingToDelete, success);
                    mDeferredAction = null;
                }
            };
            return;
        }

        boolean beingCalledAfterUninstall = mDeferredAction != null;
        boolean successfulDrop =
                success && (!beingCalledAfterUninstall || mUninstallSuccessful);

        if (successfulDrop) {
            if (!mItemAddedBackToSelfViaIcon && target != this && getItemCount() <= 1) {
                replaceFolderWithFinalItem();
            }
        } else {
            // The drag failed, we need to return the item to the folder
            ShortcutInfo info = (ShortcutInfo) d.dragInfo;
            View icon = (mCurrentDragView != null && mCurrentDragView.getTag() == info)
                    ? mCurrentDragView : createNewView(info);
            ArrayList<View> views = getItemsInReadingOrder();
            views.add(info.rank, icon);
            arrangeChildren(views, views.size());
            mItemsInvalidated = true;

            mSuppressOnAdd = true;
            mFolderIcon.onDrop(d);
            mSuppressOnAdd = false;
        }

        if (target != this) {
            rearrangeChildren();
            clearDragInfo();
        }

        mDragInProgress = false;
        mItemAddedBackToSelfViaIcon = false;
        mCurrentDragInfo = null;
        mCurrentDragView = null;
        mSuppressOnAdd = false;

        // Reordering may have occured, and we need to save the new item locations. We do this once
        // at the end to prevent unnecessary database operations.
        updateItemLocationsInDatabaseBatch();

        if (getItemCount() == 0) {
            removeFolder();
        }
    }

    void removeFolder() {
        CellLayout cellLayout = mLauncher.getCellLayout(mInfo.container, mInfo.screenId);
        if (cellLayout != null && mFolderIcon.getParent() != null) {
            cellLayout.addAlignCell(mFolderIcon);
            cellLayout.removeView(mFolderIcon);
            cellLayout.completeAlign();
        }
        LauncherModel.deleteItemFromDatabase(mLauncher, mInfo);
    }

    OnAlarmListener mReorderAlarmListener = new OnAlarmListener() {
        public void onAlarm(Alarm alarm) {
            realTimeReorder(mEmptyCellRank, mTargetRank);
            mEmptyCellRank = mTargetRank;
        }
    };

    /**
     * Reorders the items such that the {@param empty} spot moves to {@param target}
     */
    public void realTimeReorder(int empty, int target) {
        int delay = 0;
        float delayAmount = START_VIEW_REORDER_DELAY;

        int pagePosT = target;
        int pagePosE = empty;

        int startPos, endPos;
        int direction;

        if (target == empty) {
            // No animation
            return;
        } else if (target > empty) {
            // Items will move backwards to make room for the empty cell.
            direction = 1;

            startPos = pagePosE;
            endPos = pagePosT;
        } else {
            // The items will move forward.
            direction = -1;

            startPos = pagePosE;
            endPos = pagePosT;
        }

        if ((endPos - startPos) * direction <= 0) {
            // No animation
            return;
        }

        CellLayout page = mContent;
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

    @Override
    public void onUninstallActivityReturned(boolean success) {
        mDeferDropAfterUninstall = false;
        mUninstallSuccessful = success;
        if (mDeferredAction != null) {
            mDeferredAction.run();
        }
    }

    @Override
    public void deferCompleteDropAfterUninstallActivity() {
        mDeferDropAfterUninstall = true;
    }

    /**
     * Ensures that all the icons on the given page are of high-res
     */
    public void verifyVisibleHighResIcons() {
        if (mContent != null) {
            ShortcutAndWidgetContainer parent = mContent.getShortcutsAndWidgets();
            for (int i = parent.getChildCount() - 1; i >= 0; i--) {
                ((BubbleTextView) parent.getChildAt(i)).verifyHighRes();
            }
        }
    }

    private int getTargetRank(DragObject d, float[] recycle) {
        recycle = d.getVisualCenter(recycle);
        mContent.findNearestArea((int) recycle[0] - getPaddingLeft(), (int) recycle[1] - getPaddingTop(), 1, 1, sTempPosArray);
        return Math.min(mAllocatedContentSize - 1,
                sTempPosArray[1] * mGridCountX + sTempPosArray[0]);
    }

    private void updateItemLocationsInDatabaseBatch() {
        ArrayList<View> list = getItemsInReadingOrder();
        ArrayList<ItemInfo> items = new ArrayList<ItemInfo>();
        for (int i = 0; i < list.size(); i++) {
            View v = list.get(i);
            ItemInfo info = (ItemInfo) v.getTag();
            info.rank = i;
            items.add(info);
        }

        LauncherModel.moveItemsInDatabase(mLauncher, items, mInfo.id, 0);
    }

    // Compares item position based on rank and position giving priority to the rank.
    public static final Comparator<ItemInfo> ITEM_POS_COMPARATOR = new Comparator<ItemInfo>() {

        @Override
        public int compare(ItemInfo lhs, ItemInfo rhs) {
            if (lhs.rank != rhs.rank) {
                return lhs.rank - rhs.rank;
            } else if (lhs.cellY != rhs.cellY) {
                return lhs.cellY - rhs.cellY;
            } else {
                return lhs.cellX - rhs.cellX;
            }
        }
    };


}

package com.amz.ios.launcher.folder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.amz.ios.launcher.Alarm;
import com.amz.ios.launcher.AppInfo;
import com.amz.ios.launcher.BaseCellLayout;
import com.amz.ios.launcher.BubbleTextView;
import com.amz.ios.launcher.CellLayout;
import com.amz.ios.launcher.DragController;
import com.amz.ios.launcher.DragLayer;
import com.amz.ios.launcher.DropTarget;
import com.amz.ios.launcher.FolderIcon;
import com.amz.ios.launcher.FolderInfo;
import com.amz.ios.launcher.Hotseat;
import com.amz.ios.launcher.ItemInfo;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAnimatorUpdateListener;
import com.amz.ios.launcher.LauncherModel;
import com.amz.ios.launcher.LauncherSettings;
import com.amz.ios.launcher.OnAlarmListener;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.ShortcutInfo;
import com.amz.ios.launcher.Utilities;
import com.amz.ios.launcher.Workspace;
import com.amz.ios.launcher.animation.IOSAccelerateDecelerateInterpolator;
import com.amz.ios.launcher.dragndrop.DragOptions;
import com.amz.ios.launcher.folder.FolderEditText;
import com.amz.ios.launcher.accessibility.LauncherAccessibilityDelegate.AccessibilityDragSource;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class Folder extends BaseFolder implements FolderInfo.FolderListener, AccessibilityDragSource  {
    private static final String TAG = "Launcher.Folder";

    static final boolean DEBUG_DRAG = false;
    static final boolean DEBUG_ENABLE = false;
    static final boolean DEBUG_NORMAL = false;

    View mPopUpItemView = null;

    private static final int MSG_ADDITEM_FOLDER = 8001;
    public ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            return false;
        }

        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        public void onDestroyActionMode(ActionMode actionMode) {
        }

        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }
    };
    AlphaComparator mAlphaComparator = new AlphaComparator();
    private boolean mContentChanged = false;
    private ShortcutInfo mCurrentDragInfo;
    private int mFoldMaskHeight = getResources().getDimensionPixelSize(R.dimen.folder_mask_content);
    private int mFoldScrollPaddingBottom = getResources().getDimensionPixelSize(R.dimen.folder_scroll_paddbottom);
    private int mFoldScrollPaddingLeft = getResources().getDimensionPixelSize(R.dimen.folder_scroll_paddleft);
    private int mFoldScrollPaddingRight = getResources().getDimensionPixelSize(R.dimen.folder_scroll_paddright);
    private int mFoldScrollPaddingTop = getResources().getDimensionPixelSize(R.dimen.folder_scroll_paddtop);
    private Drawable mFolderContentDownMask = getResources().getDrawable(R.drawable.folder_content_downmask);
    private Drawable mFolderContentUpMask = getResources().getDrawable(R.drawable.folder_content_upmask);
    private FolderIcon mFolderIcon;
    protected float mFolderShrinkFactor = (((float) getResources().getInteger(R.integer.config_folderShrinkPercentage)) / 100.0f);
    private int mHasChoosedItem = 0;
    public FolderInfo mInfo;
    private boolean mIsClickAddOk = false;

    public OnAlarmListener mReorderAlarmListener = new OnAlarmListener() {
        @Override
        public void onAlarm(Alarm alarm) {
            Log.d(TAG, "Reorder animations: mEmptyCellRank"  + mEmptyCellRank  + "mTargetRank" + mTargetRank);
            mFolderContent.realTimeReorder(mEmptyCellRank, mTargetRank);
            mEmptyCellRank = mTargetRank;
        }
    };
    private float mScrollerBgAlpha = 0.0f;
    private HashMap<ShortcutInfo, Boolean> mSelectedStateChanged = new HashMap<>(12);
    private ArrayList<ShortcutInfo> mTempAddFolderItem = new ArrayList<>();
    private int mTotalHeight;
    private int mTotalItemSize = 0;
    private int mTargetRank, mPrevTargetRank, mEmptyCellRank;

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

    private boolean beginDrag(View v, boolean accessible) {
        Object tag = v.getTag();
        if (tag instanceof ShortcutInfo) {
            ShortcutInfo item = (ShortcutInfo) tag;
            if (!v.isInTouchMode()) {
                return false;
            }

            if (!DragLayer.sTidyUping) {
                DragLayer.sStartTidyUpInFolder = true;
                mLauncher.startTidyUp();
            }

            mFolderContent.beginDragShared(v, new Point(), this, accessible, new DragOptions());

            this.mIconDrawable = ((TextView) v).getCompoundDrawables()[1];
            this.mCurrentDragInfo = item;
            this.mEmptyCell[0] = item.cellX;
            this.mEmptyCell[1] = item.cellY;

            mEmptyCellRank = item.rank;
            mCurrentDragView = v;

            mFolderContent.removeItem(mCurrentDragView);

            mInfo.remove(mCurrentDragInfo);
            mDragInProgress = true;
            mItemAddedBackToSelfViaIcon = false;
        }
        return true;
    }

    public void startDrag(CellLayout.CellInfo cellInfo) {
        startDrag(cellInfo, false);
    }

    @Override
    public void startDrag(CellLayout.CellInfo cellInfo, boolean accessible) {
        beginDrag(cellInfo.cell, accessible);
        mFolderContent.startDrag(cellInfo, accessible);

        View child = cellInfo.cell;
        // Make sure the drag was started by a long press as opposed to a long click.
        if (!child.isInTouchMode()) {
            return;
        }
        CellLayout layout = (CellLayout) child.getParent().getParent();
        layout.prepareChildForDrag(child);
        mFolderContent.beginDragShared(child, this, accessible);

    }

    @Override
    public void enableAccessibleDrag(boolean enable) {
//        mLauncher.getSearchDropTargetBar().enableAccessibleDrag(enable);
        for (int i = 0; i < mFolderContent.getChildCount(); i++) {
            mFolderContent.getPageAt(i).enableAccessibleDrag(enable, CellLayout.FOLDER_ACCESSIBILITY_DRAG);
        }

        mFolderName.setImportantForAccessibility(enable ? IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS : IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        mLauncher.getWorkspace().setAddNewPageOnDrag(!enable);
    }

    public class AlphaComparator implements Comparator<ItemInfo> {
        private final Collator sCollator = Collator.getInstance();

        AlphaComparator() {}

        @Override
        public final int compare(ItemInfo shortcutInfo, ItemInfo shortcutInfo2) {
            return this.sCollator.compare(Folder.this.stripStart(shortcutInfo.title.toString()), Folder.this.stripStart(shortcutInfo2.title.toString()));
        }
    }

    public Folder(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    private void cleanTemItemToAdd() {
        this.mFolderContent.removeAllPages();
        ArrayList<ShortcutInfo> arrayList = this.mInfo.contents;
        mFolderContent.setShortcutItemViews(arrayList);
        setupContentForNumItems(arrayList.size());
        this.mSelectedStateChanged.clear();
    }

    public static Folder fromXml(Context context) {
        return (Folder) LayoutInflater.from(context).inflate(R.layout.user_folder, (ViewGroup) null);
    }

    private void updateItemLocationsInDatabaseBatch() {
        ArrayList<View> itemsInReadingOrder = getItemsInReadingOrder();
        ArrayList<ItemInfo> items = new ArrayList<ItemInfo>();
        for (int i = 0; i < itemsInReadingOrder.size(); i++) {
            ItemInfo info = (ItemInfo) itemsInReadingOrder.get(i).getTag();
            info.rank = i;
            items.add(info);
        }
        LauncherModel.moveItemsInDatabase(this.mLauncher, items, this.mInfo.id, (int)this.mInfo.screenId);
    }

    @Override
    public void animateClosed() {
        this.mTempAddFolderItem.clear();
        this.mTotalItemSize = 0;
        if (getParent() instanceof DragLayer) {
            this.mFolderName.setCursorVisible(false);
            AnimatorSet animatorSet = new AnimatorSet();
            ArrayList arrayList = new ArrayList();
            ObjectAnimator ofPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat("alpha", 0.1f));
            ofPropertyValuesHolder.setDuration((long) this.mFoldCloseAnimDuration);
            arrayList.add(ofPropertyValuesHolder);
            arrayList.add(drawZoomAnimation(false));
            animatorSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animator) {
                    Folder.this.mItemsInvalidated = true;
                    Folder.this.onCloseComplete();
                    Folder.this.mState = STATE_SMALL;
                }

                public void onAnimationStart(Animator animator) {
                    Folder.this.sendCustomAccessibilityEvent(32, Folder.this.getContext().getString(R.string.folder_closed));
                    Folder.this.mState = STATE_ANIMATING;
                    Folder.this.endTidyuping();
                    Folder.this.mFolderHeader.setVisibility(INVISIBLE);
                }
            });
            animatorSet.playTogether(arrayList);
            animatorSet.start();
            if (this.mOpenInDragMode) {
                this.mOpenInDragMode = false;
            }
        }

        mFolderIcon.invalidate();
    }

    @Override
    public void animateOpen() {
        this.mTempAddFolderItem.clear();
        this.mTotalItemSize = 0;
        if (this.mDragController.isDragging()) {
            this.mOpenInDragMode = true;
            this.mMoveInValidArea = false;
        } else {
            this.mOpenInDragMode = false;
        }

        mFolderContent.completePendingPageChanges();
        if (!mDragInProgress) {
            // Open on the first page.
            mFolderContent.snapToPageImmediately(0);
        }

        this.rearrangeChildren();
        this.mFolderContent.smoothScrollToFirstPage();
        positionAndSizeAsIcon();
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        measure(View.MeasureSpec.makeMeasureSpec(dragLayer.getWidth(), MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dragLayer.getHeight(), MeasureSpec.EXACTLY));
        layoutChildren();
        if (getParent() instanceof DragLayer) {
            AnimatorSet animatorSet = new AnimatorSet();
            ArrayList arrayList = new ArrayList();
            final View childAt = this.mLauncher.getWorkspace().getScreenWithId(this.mInfo.screenId);

            centerAboutIcon();
            ObjectAnimator ofPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat("alpha", 1.0f));
            ofPropertyValuesHolder.setDuration((long) this.mFoldAlphaAnimDuration);
            arrayList.add(ofPropertyValuesHolder);
            arrayList.add(drawZoomAnimation(true));

            final boolean updateAnimationFlag = !mDragInProgress;

            ValueAnimator duration = ValueAnimator.ofFloat(0.0f, 1.0f).setDuration(200L);
            duration.setInterpolator(new OvershootInterpolator(3.0f));
            duration.setStartDelay(100);
            duration.addListener(new Animator.AnimatorListener() {
                public void onAnimationCancel(Animator animator) {
                }

                public void onAnimationEnd(Animator animator) {
                    if (!Folder.this.mLauncher.isTidyUping()) {
                    }
                }

                public void onAnimationRepeat(Animator animator) {
                }

                public void onAnimationStart(Animator animator) {
                    if (!Folder.this.mLauncher.isTidyUping()) {
                    }
                }
            });
            duration.addUpdateListener(new LauncherAnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(float f, float f2) {
                }
            });
            arrayList.add(duration);
            animatorSet.addListener(new Animator.AnimatorListener() {
                public void onAnimationCancel(Animator animator) {}
                public void onAnimationRepeat(Animator animator) {}

                public void onAnimationEnd(Animator animator) {
                    if (Folder.this.getParent() == null) {
                        Folder.this.mState = STATE_SMALL;
                        if (childAt != null) {
                            childAt.setAlpha(1.0f);
                        }
                    } else {
                        Folder.this.mState = STATE_OPEN;
                        Folder.this.requestFocus();
                        Folder.this.startTidyuping();
                    }

                    Folder.this.mFolderHeader.setVisibility(VISIBLE);
                    mFolderContent.setFocusOnFirstChild();
                    mInfo.setOption(FolderInfo.FLAG_MULTI_PAGE_ANIMATION, true, mLauncher);
                    mFolderContent.animateMarkers();
                }

                public void onAnimationStart(Animator animator) {
                    Folder.this.mState = STATE_ANIMATING;
                    Folder.this.mFolderHeader.setVisibility(VISIBLE);
                    sendCustomAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                            mFolderContent.getAccessibilityDescription());
                    mState = STATE_ANIMATING;

                }
            });
            animatorSet.playTogether(arrayList);
            animatorSet.play(duration);
//            animatorSet.setStartDelay(100);
            animatorSet.start();

            // Footer animation
            if (mFolderContent.getPageCount() > 1 && !mInfo.hasOption(FolderInfo.FLAG_MULTI_PAGE_ANIMATION)) {
                mFolderContent.setMarkerScale(0);
            } else {
                mFolderContent.setMarkerScale(1);
            }

            // Make sure the folder picks up the last drag move even if the finger doesn't move.
            if (mDragController.isDragging()) {
                mDragController.forceTouchMove();
            }

            FolderPagedView pages = mFolderContent;
            pages.verifyVisibleHighResIcons(pages.getNextPage());

        }
    }

    @Override
    public void arrangeChildren(ArrayList<View> arrayList) {
        int[] iArr = new int[2];
        if (arrayList == null) {
            arrayList = getItemsInReadingOrder();
        }

        mFolderContent.arrangeChildren(arrayList, arrayList.size(), false);
        this.mItemsInvalidated = true;
    }

    public void bind(FolderInfo info) {
        mInfo = info;
        ArrayList<ShortcutInfo> children = info.contents;
        Collections.sort(children, ITEM_POS_COMPARATOR);
        ArrayList<ShortcutInfo> overflow = mFolderContent.bindItems(children);

        setupContentForNumItems(children.size());

        mItemsInvalidated = true;
        updateTextViewFocus();
        FolderInfo folderInfo = this.mInfo;
        folderInfo.addListener(this);
        if (!sDefaultFolderName.contentEquals(folderInfo.title)) {
            String title = Launcher.getRealSystemFolderTitle(mLauncher, mInfo.title.toString());
            mFoldShower.setText(title);
        } else {
            mFoldShower.setText(sDefaultFolderName);
        }

        updateItemLocationsInDatabaseBatch();
    }

    @Override
    public void centerAboutIcon() {
        CellLayout currentDropLayout = this.mLauncher.getWorkspace().getCurrentDropLayout();
        if (currentDropLayout != null) {
            DragLayer.LayoutParams layoutParams = (DragLayer.LayoutParams) getLayoutParams();
            int desiredWidth = this.mFolderContent.getDesiredWidth() + getPaddingLeft() + getPaddingRight();
            int paddingTop = this.mFolderNameHeight + getPaddingTop() + getPaddingBottom() + this.mFolderContent.getDesiredHeight();
            DragLayer dragLayer = (DragLayer) this.mLauncher.findViewById(R.id.drag_layer);
            dragLayer.getDescendantRectRelativeToSelf(this.mFolderIcon, this.mTempRect);
            int centerX = this.mTempRect.centerX() - (desiredWidth / 2);
            int centerY = this.mTempRect.centerY() - (paddingTop / 2);
            Rect rect = new Rect();
            dragLayer.getDescendantRectRelativeToSelf(currentDropLayout, rect);
            int min = Math.min(Math.max(rect.left, centerX), (rect.left + rect.width()) - desiredWidth);
            int min2 = Math.min(Math.max(rect.top, centerY), (rect.top + rect.height()) - paddingTop);
            if (desiredWidth >= rect.width()) {
                min = rect.left + ((rect.width() - desiredWidth) / 2);
            }
            if (paddingTop >= rect.height()) {
                min2 = rect.top + ((rect.height() - paddingTop) / 2);
            }
            setPivotX((float) ((centerX - min) + (desiredWidth / 2)));
            setPivotY((float) ((centerY - min2) + (paddingTop / 2)));
            layoutParams.width = desiredWidth;
            layoutParams.height = paddingTop;
            layoutParams.x = min;
            layoutParams.y = min2;
        }
    }

    @Override
    public void doneEditingFolderName(boolean z) {
        String trim = this.mFolderName.getText().toString().trim();
        String realSystemFolderTitle = Launcher.getRealSystemFolderTitle(this.mLauncher, this.mInfo.title.toString());
        this.mInfo.editDefaultFolder(this.mLauncher, trim);
        if (TextUtils.isEmpty(trim) || trim.equals(realSystemFolderTitle)) {
            this.mFoldShower.setText(realSystemFolderTitle);
        } else {
            this.mInfo.setTitle(trim);
            this.mFoldShower.setText(trim);
        }
        this.mFolderName.clearFocus();
        this.mFolderName.setVisibility(INVISIBLE);
        this.mFoldShower.setVisibility(VISIBLE);
        this.mFolderHeader.setBackgroundDrawable(null);
        LauncherModel.updateItemInDatabase(this.mLauncher, this.mInfo);
        if (z) {
            sendCustomAccessibilityEvent(32, String.format(getContext().getString(R.string.folder_renamed), trim));
            this.mFolderContent.setVisibility(VISIBLE);
        }
        requestFocus();
        this.mIsEditingName = false;
    }

    public void cancelEditingFolderName() {
        Folder.this.mInputMethodManager.hideSoftInputFromWindow(Folder.this.getWindowToken(), 0);
        this.mFolderName.clearFocus();
        this.mFolderName.setVisibility(INVISIBLE);
        this.mFoldShower.setVisibility(VISIBLE);
        this.mFolderHeader.setBackgroundDrawable(null);

        this.mFolderContent.setVisibility(VISIBLE);

        requestFocus();
        this.mIsEditingName = false;
    }

    public Animator drawZoomAnimation(final boolean z) {
        int[] locationOnScreen = new int[2];
        int[] localCenterForIndex = new int[2];
        int smallIconSize = this.mFolderIcon.getSmallIconSize();
        float iconWidth = (((float) smallIconSize) / ((float) Utilities.getIconWidth())) * this.mFolderShrinkFactor;
        float iconWidth2 = (((float) smallIconSize) / ((float) Utilities.getIconWidth())) * this.mFolderShrinkFactor;
        this.mFolderIcon.getLocationOnScreen(locationOnScreen);
        this.mFolderIcon.getLocalCenterForIndex(0, localCenterForIndex);
        this.mFolderIconCenter[0] = locationOnScreen[0] + localCenterForIndex[0];
        this.mFolderIconCenter[1] = locationOnScreen[1] + localCenterForIndex[1];
        if (z || getScaleX() >= 1.0f) {
            this.mFolderContent.getLocationOnScreen(this.mFolderScrollPos);
        } else {
            Log.w(TAG, " drawZoomAnimation -- -- getScaleX() = " + getScaleX());
        }
        View childAtPosition = this.mFolderContent.getShortcutViewAt(0);
        if (childAtPosition != null) {
            ((BubbleTextView) childAtPosition).getLocalIconCenter(this.mChildOffset);
        }

        this.mFirstChildCenter[0] = this.mFolderScrollPos[0] + this.mFolderContent.getFirstChildPaddingLeft() + this.mChildOffset[0];
        this.mFirstChildCenter[1] = this.mFolderScrollPos[1] + this.mFolderContent.getFirstChildPaddingTop() + this.mChildOffset[1];

        if (z) {
            this.mOldScaleX = iconWidth;
            this.mOldScaleY = iconWidth2;
            this.mOldTranslationX = (float) (this.mFolderIconCenter[0] - this.mFirstChildCenter[0]);
            this.mOldTranslationY = (float) (this.mFolderIconCenter[1] - this.mFirstChildCenter[1]);
            setPivotX((float) this.mFirstChildCenter[0]);
            setPivotY((float) this.mFirstChildCenter[1]);
            this.mOldAlpha = 0.1f;
            this.mNewTranslationX = 0.0f;
            this.mNewTranslationY = 0.0f;
            this.mNewScaleX = 1.0f;
            this.mNewScaleY = 1.0f;
            this.mNewAlpha = 1.0f;
        } else {
            this.mOldTranslationX = getTranslationX();
            this.mOldTranslationY = getTranslationY();
            this.mOldScaleX = getScaleX();
            this.mOldScaleY = getScaleY();
            this.mOldAlpha = getAlpha();
            this.mNewTranslationX = (float) (this.mFolderIconCenter[0] - this.mFirstChildCenter[0]);
            this.mNewTranslationY = (float) (this.mFolderIconCenter[1] - this.mFirstChildCenter[1]);
            this.mNewScaleX = iconWidth;
            this.mNewScaleY = iconWidth2;
            this.mNewAlpha = 0.2f;
            setPivotX((float) this.mFirstChildCenter[0]);
            setPivotY((float) this.mFirstChildCenter[1]);
        }
        ValueAnimator duration = ValueAnimator.ofFloat(0.0f, 1.0f).setDuration((long) (z ? SC_MULTIPLE_CHOICES : this.mLauncher.isResuming() ? SC_MULTIPLE_CHOICES : 0));
        duration.setInterpolator(z ? new IOSAccelerateDecelerateInterpolator() : new AccelerateDecelerateInterpolator());
        duration.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                Folder.this.setTranslationX(Folder.this.mNewTranslationX);
                Folder.this.setTranslationY(Folder.this.mNewTranslationY);
                Folder.this.setScaleX(Folder.this.mNewScaleX);
                Folder.this.setScaleY(Folder.this.mNewScaleY);
                if (!z) {
                    Folder.this.mFolderIcon.invalidate();
                }
            }

            public void onAnimationStart(Animator animator) {
                if (z) {
                    Folder.this.setTranslationX(Folder.this.mOldTranslationX);
                    Folder.this.setTranslationY(Folder.this.mOldTranslationY);
                    Folder.this.setScaleX(Folder.this.mOldScaleX);
                    Folder.this.setScaleY(Folder.this.mOldScaleY);
                }
                Folder.this.mFolderIcon.invalidate();
            }
        });
        duration.addUpdateListener(new LauncherAnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(float f, float f2) {
                Folder.this.setTranslationX((Folder.this.mOldTranslationX * f) + (Folder.this.mNewTranslationX * f2));
                Folder.this.setTranslationY((Folder.this.mOldTranslationY * f) + (Folder.this.mNewTranslationY * f2));
                Folder.this.setScaleX((Folder.this.mOldScaleX * f) + (Folder.this.mNewScaleX * f2));
                Folder.this.setScaleY((Folder.this.mOldScaleY * f) + (Folder.this.mNewScaleY * f2));
            }
        });
        return duration;
    }

    public void endTidyuping() {
        if (this.mFolderContent != null) {
            this.mFolderContent.stopShakeAnimations();
        }
    }
    @Override
    public View getEditTextRegion() {
        return this.mFoldShower;
    }

    @Override
    public FolderIcon getFolderIcon() {
        return this.mFolderIcon;
    }

    @Override
    public FolderInfo getInfo() {
        return this.mInfo;
    }

    @Override
    public View getItemAt(int i) {
        return this.mFolderContent.getShortcutViewAt(i);
    }

    @Override
    public int getItemCount() {
        int count = this.mFolderContent.getAppChildCount();
        return count;
    }

    @Override
    public ArrayList<View> getItemsInReadingOrder(boolean z) {
        if (mItemsInvalidated) {
            mItemsInReadingOrder.clear();
            mFolderContent.iterateOverItems(new Workspace.ItemOperator() {
                @Override
                public boolean evaluate(ItemInfo info, View view, View parent) {
                    mItemsInReadingOrder.add(view);
                    return false;
                }
            });

            if (mItemsInReadingOrder.size() <= 0) {
                Log.d(TAG, "mItemsInReadingOrder size = 0!!!");
            }
            mItemsInvalidated = false;
        }
        return mItemsInReadingOrder;
    }

    public void layoutChildren() {
        int sumPaddingTop = 0;
        int measuredWidth = (getMeasuredWidth() - getPaddingLeft()) - getPaddingRight();
        int paddingTop = (getPaddingTop() + ((getMeasuredHeight() - this.mTotalHeight) / 2)) - this.mFolderNameHeight;
        int childCount = getChildCount();

        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.getVisibility() != GONE) {
                int measuredWidth2 = childAt.getMeasuredWidth();
                int measuredHeight = childAt.getMeasuredHeight();
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) childAt.getLayoutParams();
                int i3 = paddingTop + layoutParams.topMargin;
                int paddingLeft = ((getPaddingLeft() + ((measuredWidth - measuredWidth2) / 2)) + layoutParams.leftMargin) - layoutParams.rightMargin;
                childAt.layout(paddingLeft, i3, measuredWidth2 + paddingLeft, i3 + measuredHeight);
                sumPaddingTop = layoutParams.bottomMargin + measuredHeight + i3;
            } else {
                sumPaddingTop = paddingTop;
            }
            paddingTop = sumPaddingTop;
        }
    }

    @Override
    public void onAdd(ShortcutInfo shortcutInfo) {
        this.mItemsInvalidated = true;
        if (!this.mSuppressOnAdd) {
            mFolderContent.createAndAddViewForRank(shortcutInfo, mFolderContent.allocateRankForNewItem(shortcutInfo));
            mItemsInvalidated = true;
            LauncherModel.addOrMoveItemInDatabase(mLauncher, shortcutInfo, mInfo.id, 0, shortcutInfo.cellX, shortcutInfo.cellY);

            if (this.mInfo.opened && this.mOpenInDragMode) {
                this.mOpenInDragMode = false;
            }
        }
    }

    public boolean onAddTempItem() {
        if (this.mSuppressOnAdd) {
            return false;
        }

        ArrayList<View> originalAppViews = getItemsInReadingOrder();
        this.mTotalItemSize = this.mTempAddFolderItem.size() + this.mHasChoosedItem;
        setupContentForNumItems(this.mTempAddFolderItem.size() + this.mHasChoosedItem);
        mFolderContent.addTempAddFolderItem(originalAppViews, this.mTempAddFolderItem);
        return true;
    }

    @Override
    public void onClick(View view) {
        Object tag = view.getTag();
        if ((tag instanceof CellLayout.CellInfo)) {
            this.mLauncher.closeFolder(this);
        }
    }

    @Override
    public void onCloseComplete() {
        DragLayer parent = (DragLayer) getParent();
        if (parent != null) {
            parent.removeView(this);
        }
        this.mDragController.removeDropTarget(this);
        clearFocus();
        this.mFolderIcon.requestFocus();
        this.mFolderContent.cancelDragScroll();

        this.mTempAddFolderItem.clear();
        this.mTotalItemSize = 0;
        this.mFoldShower.setVisibility(VISIBLE);
        this.mFoldShower.setEnabled(true);
        this.mFoldShower.setText(Launcher.getRealSystemFolderTitle(this.mLauncher, this.mInfo.title.toString()));
        this.mFolderHeader.setBackgroundDrawable(null);
        this.mFolderName.setVisibility(INVISIBLE);
        this.mFolderContent.setVisibility(VISIBLE);
        if (this.mRearrangeOnClose) {
            setupContentForNumItems(getItemCount());
            this.mRearrangeOnClose = false;
        }
        if (getItemCount() < 1) {
            if (!this.mDragInProgress && !this.mSuppressFolderDeletion) {
                replaceFolderWithFinalItem(true);
            } else if (this.mDragInProgress) {
                this.mDeleteFolderOnDropCompleted = true;
            }
        }
        this.mSuppressFolderDeletion = false;
        if (this.mContentChanged && !this.mLauncher.isTidyUping()) {
            this.mLauncher.getWorkspace().removeNullScreen(true);
        }
        this.mLauncher.commandCloseFolder();
        mFolderIcon.invalidate();
    }

    @Override
    public void onFlingToDelete(DragObject dragObject, PointF vec) {

    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        setClickable(true);
        setFocusable(true);
        this.mFolderHeader = (FrameLayout) findViewById(R.id.folder_header);
        this.mFolderQuickTitleContainer = (FrameLayout) findViewById(R.id.folder_quick_title_container);
        this.mFolderContent = (FolderPagedView) findViewById(R.id.folder_content);
        this.mFolderContent.setPadding(this.mFoldScrollPaddingLeft, this.mFoldScrollPaddingTop, this.mFoldScrollPaddingRight, this.mFoldScrollPaddingBottom);
        this.mFolderContent.setFolder(this);
        this.mFolderContent.setOnClickListener(this);
        this.mFolderName = (FolderEditText) findViewById(R.id.folder_name);
        this.mFolderName.setSelectAllOnFocus(true);
        this.mFolderName.addTextChangedListener(this.mTextWatcher);
        this.mFolderName.setFolder(this);
        this.mFolderName.setOnFocusChangeListener(this);
        this.mFolderHeader.measure(0, 0);
        this.mFolderNameHeight = this.mFolderHeader.getMeasuredHeight();
        this.mFolderName.setCustomSelectionActionModeCallback(this.mActionModeCallback);
        this.mFolderName.setSelectAllOnFocus(false);
        this.mFolderName.setInputType(this.mFolderName.getInputType() | 524288 | 8192);
        this.mFolderName.setVisibility(INVISIBLE);
        this.mFoldShower = (TextView) findViewById(R.id.folder_shower);
        this.mFoldShower.setVisibility(VISIBLE);
        this.mFoldShower.setOnClickListener(this.mNameClickListener);
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int i5;
        int paddingLeft = ((r - l) - getPaddingLeft()) - getPaddingRight();
        int paddingTop = (getPaddingTop() + (((getBottom() - getTop()) - this.mTotalHeight) / 2)) - this.mFolderNameHeight;
        int childCount = getChildCount();
        int paddingT = paddingTop;
        for (int i = 0; i < childCount; i ++) {
            View childAt = getChildAt(i);
            if (childAt.getVisibility() != GONE) {
                int measuredWidth = childAt.getMeasuredWidth();
                int measuredHeight = childAt.getMeasuredHeight();
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) childAt.getLayoutParams();
                int i8 = paddingT + layoutParams.topMargin;
                int paddingLeft2 = ((getPaddingLeft() + ((paddingLeft - measuredWidth) / 2)) + layoutParams.leftMargin) - layoutParams.rightMargin;
                childAt.layout(paddingLeft2, i8, measuredWidth + paddingLeft2, i8 + measuredHeight);
                i5 = layoutParams.bottomMargin + measuredHeight + i8;
            } else {
                i5 = paddingT;
            }

            paddingT = i5;
        }
        int left = this.mFolderQuickTitleContainer.getLeft() + this.mShadowWidth;
        int right = this.mFolderQuickTitleContainer.getRight() - this.mShadowWidth;
        this.mFolderContentUpMask.setBounds(left, this.mFolderQuickTitleContainer.getTop(), right, this.mFolderQuickTitleContainer.getTop() + this.mFoldMaskHeight);
        this.mFolderContentDownMask.setBounds(left, (this.mFolderQuickTitleContainer.getBottom() - this.mFolderContent.getPaddingBottom()) - this.mFoldMaskHeight, right, this.mFolderQuickTitleContainer.getBottom() - this.mFolderContent.getPaddingBottom());
    }

    @Override
    public boolean onLongClick(View view) {

//        if (!mLauncher.isShaking()){
//            if (!mLauncher.showingFloatingMenu && (view instanceof BubbleTextView)){
//                mPopUpItemView = view;
//                mLauncher.openFloatingMenu(view);
//                BaseCellLayout.CellInfo longClickCellInfo = null;
//                if (view.getTag() instanceof ItemInfo) {
//                    ItemInfo info = (ItemInfo) view.getTag();
//                    longClickCellInfo = new CellLayout.CellInfo(view, info);
//                }
//                if (longClickCellInfo != null)
//                    mLauncher.getWorkspace().showInfo(longClickCellInfo);
//                return true;
//            }
//        }

        if (!mLauncher.isDraggingEnabled())
            return true;

        return beginDrag(view, false);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = 0;
        this.mTotalHeight = 0;
        this.mFolderContent.setContentCellDimension();
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        int width = displayMetrics.widthPixels;

        if (this.mLauncher != null) {
            height = this.mLauncher.getWorkspaceHeight();
        }
        if (height <= 0) {
            height = displayMetrics.heightPixels;
        }

        int scrollDesiredWidth = this.mFolderContent.getDesiredWidth();
        int scrollDesiredHeight = this.mFolderContent.getDesiredHeight();
        int makeMeasureSpec = View.MeasureSpec.makeMeasureSpec(scrollDesiredWidth, MeasureSpec.EXACTLY);
        int makeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(scrollDesiredHeight, MeasureSpec.EXACTLY);
        this.mFolderContent.measure(makeMeasureSpec, makeMeasureSpec2);
        this.mFolderQuickTitleContainer.measure(makeMeasureSpec, makeMeasureSpec2);
        this.mTotalHeight = scrollDesiredHeight + this.mTotalHeight;
        this.mFolderHeader.measure(
                View.MeasureSpec.makeMeasureSpec(scrollDesiredWidth, MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(this.mFolderNameHeight, MeasureSpec.EXACTLY));
        setMeasuredDimension(width, height);
    }

    @Override
    public void onRemove(ShortcutInfo shortcutInfo) {
        View viewForInfo;
        this.mItemsInvalidated = true;
        if (shortcutInfo != this.mCurrentDragInfo && (viewForInfo = mFolderContent.getViewForInfo(shortcutInfo)) != null) {
            this.mFolderContent.removeAppFromPage(viewForInfo);
            if (this.mState == STATE_ANIMATING) {
                this.mRearrangeOnClose = true;
            } else {
                setupContentForNumItems(getItemCount());
            }
            if (getItemCount() < 1) {
                this.mLauncher.closeFolder();
            }
            if (!this.mInfo.opened && getItemCount() <= 1) {
                replaceFolderWithFinalItem(false);
            }

            if (!this.mInfo.opened && mInfo.contents.size() <= 0) {
                replaceFolderWithFinalItemNotUpdateDataBase();
            }
        }
    }

    @Override
    public void positionAndSizeAsIcon() {
        if (getParent() instanceof DragLayer) {
            setScaleX(1.0f);
            setScaleY(1.0f);
            setAlpha(0.0f);
            setTranslationX(0.0f);
            setTranslationY(0.0f);
            this.mState = STATE_SMALL;
        }
    }

    public void reAddeFolderIcon() {
        if (this.mInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
//            DockBar dockBar = this.mLauncher.getDockBar();
//            if (dockBar != null) {
//                dockBar.addView(this.mFolderIcon, this.mInfo.cellX);
//                return;
//            }
            return;
        }
        CellLayout cellLayout = this.mLauncher.getCellLayout(this.mInfo.container, this.mLauncher.getModel().getScreen(this.mInfo.screenId));
        if (cellLayout != null) {
//            cellLayout.addViewToCellLayout(this.mFolderIcon,
//                    -1,
//                    LauncherModel.getCellLayoutChildIdFromScreenId(
//                            this.mInfo.container,
//                            this.mInfo.screenId,
//                            this.mInfo.cellX,
//                            this.mInfo.cellY,
//                            this.mInfo.spanX,
//                            this.mInfo.spanY),
//                    (BaseCellLayout.LayoutParams) this.mFolderIcon.getLayoutParams(),
//                    true,
//                    false);
        }
    }

    @Override
    public void replaceFolderWithFinalItem(boolean animated) {
        Runnable onCompleteRunnable = new Runnable() {
            public void run() {
                long screenId;
                CellLayout cellLayout = null;
                if (Folder.this.mInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    screenId = -1;
                    cellLayout = Folder.this.mLauncher.getHotseat().getLayout();
                } else {
                    screenId = Folder.this.mInfo.screenId;
                    cellLayout = Folder.this.mLauncher.getCellLayout(Folder.this.mInfo.container, screenId);
                }

                View view = null;
                if (Folder.this.getItemCount() == 1) {
                    ShortcutInfo shortcutInfo = Folder.this.mInfo.contents.get(0);
                    View createShortcut = Folder.this.mLauncher.createShortcut(cellLayout, shortcutInfo);
                    LauncherModel.addOrMoveItemInDatabase(Folder.this.mLauncher, shortcutInfo, Folder.this.mInfo.container, Folder.this.mInfo.screenId, Folder.this.mInfo.cellX, Folder.this.mInfo.cellY);
                    view = createShortcut;
                }
                if (Folder.this.getItemCount() <= 1) {
                    LauncherModel.deleteItemFromDatabase(Folder.this.mLauncher, Folder.this.mInfo);
                    cellLayout.removeView(Folder.this.mFolderIcon);
                    if (Folder.this.mFolderIcon instanceof DropTarget) {
                        Folder.this.mDragController.removeDropTarget((DropTarget) Folder.this.mFolderIcon);
                    }
                    Folder.this.mLauncher.removeFolder(Folder.this.mInfo);
                    Folder.this.mFolderContent.removeAllViews();
                    Folder.this.mItemsInReadingOrder.clear();
                }
                if (view != null) {
                    Folder.this.mLauncher.getWorkspace().addInScreen(view, Folder.this.mInfo.container, screenId, Folder.this.mInfo.cellX, Folder.this.mInfo.cellY, Folder.this.mInfo.spanX, Folder.this.mInfo.spanY);
                    if (DragLayer.sTidyUping && Folder.this.mFolderContent != null) {
                        Folder.this.mLauncher.getWorkspace().beginShakeAnimations(true);
                    }
                }
            }
        };
        View itemAt = getItemAt(0);
        if (!animated || itemAt == null) {
            onCompleteRunnable.run();
        } else {
            onCompleteRunnable.run();
//            this.mFolderIcon.performDestroyAnimation(itemAt, onCompleteRunnable);
        }
    }

    public void replaceFolderWithFinalItemNotUpdateDataBase() {
        CellLayout cellLayout = null;
        if (this.mInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            Hotseat dockBar = this.mLauncher.getHotseat();
            if (dockBar != null) {
                cellLayout = Folder.this.mLauncher.getHotseat().getLayout();
            }
        } else {
            cellLayout = this.mLauncher.getCellLayout(this.mInfo.container, this.mInfo.screenId);
        }

        if (cellLayout == null) {
            Log.e(TAG, "replaceFolderWithFinalItem -- null == cellLayout");
        } else {
            cellLayout.removeView(this.mFolderIcon);
        }

//        LauncherModel.deleteItemFromDatabase(this.mLauncher, this.mInfo);
    }

    public void setFolderIcon(FolderIcon folderIcon) {
        this.mFolderIcon = folderIcon;
    }
    @Override
    public void startEditingFolderName() {
        this.mFoldShower.setVisibility(INVISIBLE);
        this.mFoldShower.clearFocus();
        this.mFolderName.setVisibility(VISIBLE);
        this.mFolderName.setCursorVisible(true);
        CharSequence text = this.mFoldShower.getText();
        this.mFolderName.setText(text);
        this.mFolderName.requestFocus();
        this.mIsEditingName = true;
    }

    public void startTidyuping() {
        if (DragLayer.sTidyUping && this.mFolderContent != null) {
            this.mFolderContent.beginShakeAnimations();
        }
    }

    public String stripStart(String str) {
        return (!TextUtils.isEmpty(str) && str.charAt(0) == 160) ? str.substring(1) : str;
    }

    public boolean updateAppInfo(AppInfo applicationInfo) {
        return this.mFolderContent.updateAppInfo(applicationInfo, true);
    }

    @Override
    public void updateItemLocationsInDatabase() {
        ArrayList<View> items = getItemsInReadingOrder();
        Iterator<View> it = items.iterator();
        while (it.hasNext()) {
            ItemInfo itemInfo = (ItemInfo) it.next().getTag();
            LauncherModel.moveItemInDatabase(this.mLauncher, itemInfo, this.mInfo.id, this.mInfo.screenId, itemInfo.cellX, itemInfo.cellY);
        }
    }
    private final int SC_MULTIPLE_CHOICES = 100;

    public float getPivotXForIconAnimation() {
        return getPivotX();
    }

    public float getPivotYForIconAnimation() {
        return getPivotY();
    }

    /********************************************************************
     *          DragSource Implement                                    *
     ********************************************************************/
    @Override
    public boolean isDropEnabled() {
        return true;
    }

    @Override
    public void onDrop(DragObject d) {
        Runnable cleanUpRunnable = null;

        // If we are coming from All Apps space, we defer removing the extra empty screen
        // until the folder closes
        if (d.dragSource != mLauncher.getWorkspace() && !(d.dragSource instanceof Folder)) {
            cleanUpRunnable = new Runnable() {
                @Override
                public void run() {
                    mLauncher.exitSpringLoadedDragModeDelayed(true,
                            Launcher.EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT,
                            null);
                }
            };
        }

        // If the icon was dropped while the page was being scrolled, we need to compute
        // the target location again such that the icon is placed of the final page.
        if (!mFolderContent.rankOnCurrentPage(mEmptyCellRank)) {
            // Reorder again.
            mTargetRank = getTargetRank(d, null);

            // Rearrange items immediately.
            mReorderAlarmListener.onAlarm(mReorderAlarm);

            mOnScrollHintAlarm.cancelAlarm();
            mScrollPauseAlarm.cancelAlarm();
        }
        mFolderContent.completePendingPageChanges();

        View currentDragView;
        ShortcutInfo si = mCurrentDragInfo;
        if (mIsExternalDrag) {
            currentDragView = mFolderContent.createAndAddViewForRank(si, mEmptyCellRank);
            // Actually move the item in the database if it was an external drag. Call this
            // before creating the view, so that ShortcutInfo is updated appropriately.
            LauncherModel.addOrMoveItemInDatabase(mLauncher, si, mInfo.id, 0, si.cellX, si.cellY);

            // We only need to update the locations if it doesn't get handled in #onDropCompleted.
            if (d.dragSource != this) {
                updateItemLocationsInDatabaseBatch();
            }
            mIsExternalDrag = false;
        } else {
            currentDragView = mCurrentDragView;
            mFolderContent.addViewForRank(currentDragView, si, mEmptyCellRank);

            if (mLauncher.isTidyUping() && currentDragView != null) {
                mFolderContent.beginShakeAnimations();
            }
        }

        if (d.dragView.hasDrawn()) {
            // Temporarily reset the scale such that the animation target gets calculated correctly.
            float scaleX = getScaleX();
            float scaleY = getScaleY();
            setScaleX(1.0f);
            setScaleY(1.0f);
            mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, currentDragView,
                    cleanUpRunnable, null);
            setScaleX(scaleX);
            setScaleY(scaleY);
        } else {
            d.deferDragViewCleanupPostAnimation = false;
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

        if (mFolderContent.getPageCount() > 1) {
            // The animation has already been shown while opening the folder.
            mInfo.setOption(FolderInfo.FLAG_MULTI_PAGE_ANIMATION, true, mLauncher);
        }
//        ShortcutInfo shortcutInfo = (ShortcutInfo) dragObject.dragInfo;
//        if (shortcutInfo == this.mCurrentDragInfo) {
//            CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) this.mCurrentDragView.getLayoutParams();
//            layoutParams.cellX = this.mEmptyCell[0];
//            layoutParams.cellY = this.mEmptyCell[1];
//            Runnable runnable = new Runnable() {
//                public void run() {
//                    if (Folder.this.mCurrentDragView != null) {
//                        Folder.this.mCurrentDragView.setVisibility(VISIBLE);
//                    }
//                }
//            };
//
//            this.mFolderContent.addViewToCellLayout(this.mCurrentDragView, -1, (int) shortcutInfo.id, layoutParams, true);
//            this.mCurrentDragView.setVisibility(INVISIBLE);
//            if (dragObject.dragView.hasDrawn()) {
//                this.mLauncher.getDragLayer().animateViewIntoPosition(dragObject.dragView, this.mCurrentDragView, runnable);
//            } else {
//                dragObject.deferDragViewCleanupPostAnimation = false;
//                this.mCurrentDragView.setVisibility(VISIBLE);
//            }
//            this.mItemsInvalidated = true;
//            setupContentDimensions(getItemCount());
//            this.mSuppressOnAdd = true;
//        } else {
//            if (dragObject.dragView != null) {
//                dragObject.deferDragViewCleanupPostAnimation = false;
//            }
//            showFolderScrollerBackground(false);
//        }
//        this.mInfo.add(shortcutInfo);
    }

    @Override
    public void onDragEnter(DragObject d) {

        Log.e("Event is ","Drag Enter");
        mPrevTargetRank = -1;
        // Get the area offset such that the folder only closes if half the drag icon width
        // is outside the folder area
        mScrollAreaOffset = d.dragView.getDragRegionWidth() / 2 - d.xOffset;
        Log.d(TAG, "mScrollAreaOffset = " + mScrollAreaOffset);
        mFolderContent.onDragEnter(d);
    }

    public void getContentHitRect(Rect rect) {
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        if (dragLayer != null) {
            dragLayer.getDescendantRectRelativeToSelf(mFolderContent, rect);
        }
    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        getHitRect(outRect);
        outRect.left -= mScrollAreaOffset;
        outRect.right += mScrollAreaOffset;

        Log.d(TAG, "onDragEnter = getHitRectRelativeToDragLayer = " + outRect);
    }

    private int getTargetRank(DragObject d, float[] recycle) {
        recycle = d.getVisualCenter(recycle);
        Rect rectContent = new Rect();
        getContentHitRect(rectContent);
        int nContentLeft = rectContent.left + mFolderContent.getPaddingLeft();
        int nContentTop = rectContent.top + mFolderContent.getPaddingTop();
        recycle[0] -= nContentLeft; recycle[1] -= nContentTop;

        return mFolderContent.findNearestArea((int) recycle[0] - getPaddingLeft(), (int) recycle[1] - getPaddingTop());
    }

    @Override
    public void onDragOver(DragObject d) {

        Log.e("Event is ","Drag Over");
        onDragOver(d, REORDER_DELAY);

        float recycle[] = new float[2];
        recycle = d.getVisualCenter(recycle);
        Rect rectContent = new Rect();
        getContentHitRect(rectContent);
        int nContentLeft = rectContent.left + mFolderContent.getPaddingLeft();
        int nContentTop = rectContent.top + mFolderContent.getPaddingTop();
        recycle[0] -= nContentLeft; recycle[1] -= nContentTop;

        mFolderContent.onDragOver(d, recycle);
    }

    public void onDragOver(DropTarget.DragObject d, int reorderDelay) {
        if (mScrollPauseAlarm.alarmPending()) {
            return;
        }

        Log.d(TAG, "Enter onDragOver");

        final float[] r = new float[2];
        mTargetRank = getTargetRank(d, r);

        if (mTargetRank != mPrevTargetRank) {
            mReorderAlarm.cancelAlarm();
            mReorderAlarm.setOnAlarmListener(mReorderAlarmListener);
            mReorderAlarm.setAlarm(REORDER_DELAY);
            mPrevTargetRank = mTargetRank;
        }

        float x = r[0];
        int currentPage = mFolderContent.getNextPage();
        float cellOverlap = mFolderContent.getCurrentCellLayout().getCellWidth() * ICON_OVERSCROLL_WIDTH_FACTOR;

        //Log.d(TAG, "onDragOver Scroll x = " + x + " cellOverlap = " + cellOverlap);
        Log.d(TAG, "onDragOver Scroll width = " + mFolderContent.getWidth() + ", cellOverlap = " + cellOverlap + ", x"  + x);

        boolean isOutsideLeftEdge = x < cellOverlap;
        boolean isOutsideRightEdge = x > (mFolderContent.getWidth() - cellOverlap);

        if (currentPage > 0 && (mFolderContent.mIsRtl ? isOutsideRightEdge : isOutsideLeftEdge)) {
            showScrollHint(DragController.SCROLL_LEFT, d);
        } else if (currentPage < (mFolderContent.getPageCount() - 1)
                && (mFolderContent.mIsRtl ? isOutsideLeftEdge : isOutsideRightEdge)) {
            showScrollHint(DragController.SCROLL_RIGHT, d);
        } else {
            mOnScrollHintAlarm.cancelAlarm();
            if (mScrollHintDir != DragController.SCROLL_NONE) {
                mFolderContent.clearScrollHint();
                mScrollHintDir = DragController.SCROLL_NONE;
            }
        }
    }

    @Override
    public void onDragExit(DropTarget.DragObject dragObject) {

        Log.e("Event is ","Drag Exit");
        mReorderAlarm.cancelAlarm();
        mOnScrollHintAlarm.cancelAlarm();
        mScrollPauseAlarm.cancelAlarm();
        mFolderContent.onDragExit(dragObject);

        if (mScrollHintDir != DragController.SCROLL_NONE) {
            mFolderContent.clearScrollHint();
            mScrollHintDir = DragController.SCROLL_NONE;
        }

        if (!this.mOpenInDragMode || !folderIsAnimating()) {
            if (!dragObject.dragComplete) {
                this.mOnExitAlarm.setOnAlarmListener(this.mOnExitAlarmListener);
                this.mOnExitAlarm.setAlarm(100);
            }
            this.mReorderAlarm.cancelAlarm();
        }
        Log.e(TAG, "onDragExit --folderIsAnimating return!");
    }

    @Override
    public boolean acceptDrop(DropTarget.DragObject dragObject) {
//        if (mPopUpItemView != null) return false;
        final ItemInfo item = (ItemInfo) dragObject.dragInfo;
        final int itemType = item.itemType;
        return ((itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT ||
                itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT) &&
                !isFull());
    }

    @Override
    public void completeDragExit() {
        super.completeDragExit();

        this.mLauncher.closeFolder();
        this.mCurrentDragInfo = null;
        this.mCurrentDragView = null;
        this.mSuppressOnAdd = false;
        this.mRearrangeOnClose = true;

        mLauncher.enterSpringLoadedDragMode();
    }

    @Override
    public void prepareAccessibilityDrop() {
        if (mReorderAlarm.alarmPending()) {
            mReorderAlarm.cancelAlarm();
            mReorderAlarmListener.onAlarm(mReorderAlarm);
        }
    }

    @Override
    public void getLocationInDragLayer(int[] loc) {
        this.mLauncher.getDragLayer().getLocationInDragLayer(this, loc);
    }

    /***************************************************
     * DragSource Implement
     **************************************************/

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
        return 1;
    }

    @Override
    public void onFlingToDeleteCompleted() {
    }

    @Override
    public void onDropCompleted(View view, DragObject dragObject, boolean isFlingToDelete, boolean success) {
        boolean hasNoParent;
        boolean successed = false;
        if (success)
        {
            if (view instanceof Hotseat) {
                Hotseat dockBar = (Hotseat) view;
                if (dockBar.getChangeViewFlag()) {
                    ItemInfo switchInfo = dockBar.getSwitchInfo();
                    if (switchInfo instanceof ShortcutInfo) {
                        switchInfo.screenId = this.mInfo.screenId;
                        switchInfo.container = this.mInfo.id;
                        notifyDrop();
                        this.mInfo.add((ShortcutInfo) switchInfo);
                        if (this.mFolderIcon.getParent() == null) {
                            reAddeFolderIcon();
                            successed = false;
                            if (this.mDeleteFolderOnDropCompleted && !this.mItemAddedBackToSelfViaIcon) {
                                replaceFolderWithFinalItem(true);
                            }
                        }
                    } else {
                        return;
                    }
                }
            }
            successed = true;
            replaceFolderWithFinalItem(true);
        } else {
            if (this.mFolderIcon.getParent() == null) {
                reAddeFolderIcon();
                hasNoParent = true;
            } else {
                hasNoParent = false;
            }
            if (view == null || hasNoParent) {
                this.mFolderIcon.dropNoAnimation(dragObject);
            } else {
                this.mFolderIcon.onDrop(dragObject);
                mLauncher.endTidyUp();
            }
            if (this.mOnExitAlarm.alarmPending() || this.mState == STATE_ANIMATING) {
                this.mSuppressFolderDeletion = true;
            }
            dragObject.deferDragViewCleanupPostAnimation = false;
            successed = true;
        }
        if (view != this && this.mOnExitAlarm.alarmPending()) {
            this.mOnExitAlarm.cancelAlarm();
            completeDragExit();
        }

        this.mDeleteFolderOnDropCompleted = false;
        this.mDragInProgress = false;
        this.mItemAddedBackToSelfViaIcon = false;
        this.mCurrentDragInfo = null;
        this.mCurrentDragView = null;
        this.mSuppressOnAdd = false;

        if (successed) {
            rearrangeChildren();
            updateItemLocationsInDatabase();
        }
    }

    private void showScrollHint(int direction, DragObject d) {
        // Show scroll hint on the right
        if (mScrollHintDir != direction) {
            mFolderContent.showScrollHint(direction);
            mScrollHintDir = direction;
        }

        // Set alarm for when the hint is complete
        if (!mOnScrollHintAlarm.alarmPending() || mCurrentScrollDir != direction) {
            mCurrentScrollDir = direction;
            mOnScrollHintAlarm.cancelAlarm();
            mOnScrollHintAlarm.setOnAlarmListener(new OnScrollHintListener(d));
            mOnScrollHintAlarm.setAlarm(SCROLL_HINT_DURATION);

            mReorderAlarm.cancelAlarm();
            mTargetRank = mEmptyCellRank;
        }
    }

    /**
     * Rearranges the children based on their rank.
     */
    public void rearrangeChildren() {
        rearrangeChildren(-1);
    }

    /**
     * Rearranges the children based on their rank.
     *
     * @param itemCount if greater than the total children count, empty spaces are left at the end,
     *                  otherwise it is ignored.
     */
    public void rearrangeChildren(int itemCount) {
        ArrayList<View> views = getItemsInReadingOrder();
        mFolderContent.arrangeChildren(views, Math.max(itemCount, views.size()));
        mItemsInvalidated = true;
    }

    /**
     * Fraction of icon width which behave as scroll region.
     */
    private static final float ICON_OVERSCROLL_WIDTH_FACTOR = 0.45f;

    /**
     * Time for which the scroll hint is shown before automatically changing page.
     */
    public static final int SCROLL_HINT_DURATION = DragController.SCROLL_DELAY;

    int mScrollHintDir = DragController.SCROLL_NONE;
    int mCurrentScrollDir = DragController.SCROLL_NONE;
    private boolean mIsExternalDrag;
    // Folder scrolling
    private int mScrollAreaOffset;

    private class OnScrollHintListener implements OnAlarmListener {

        private final DragObject mDragObject;

        OnScrollHintListener(DragObject object) {
            mDragObject = object;
        }

        /**
         * Scroll hint has been shown long enough. Now scroll to appropriate page.
         */
        @Override
        public void onAlarm(Alarm alarm) {
            if (mCurrentScrollDir == DragController.SCROLL_LEFT) {
                mFolderContent.scrollLeft();
                mFolderContent.onDragEnter(null);
                mScrollHintDir = DragController.SCROLL_NONE;
            } else if (mCurrentScrollDir == DragController.SCROLL_RIGHT) {
                mFolderContent.scrollRight();
                mFolderContent.onDragEnter(null);
                mScrollHintDir = DragController.SCROLL_NONE;
            } else {
                // This should not happen
                return;
            }
            mCurrentScrollDir = DragController.SCROLL_NONE;

            // Pause drag event until the scrolling is finished
            mScrollPauseAlarm.setOnAlarmListener(new OnScrollFinishedListener(mDragObject));
            mScrollPauseAlarm.setAlarm(DragController.RESCROLL_DELAY);
        }
    }

    public void showPopUpItemAfterClose(){
        if (mPopUpItemView != null){
            mPopUpItemView.setVisibility(View.VISIBLE);
            mPopUpItemView = null;
        }
    }

    private class OnScrollFinishedListener implements OnAlarmListener {

        private final DragObject mDragObject;

        OnScrollFinishedListener(DragObject object) {
            mDragObject = object;
        }

        /**
         * Page scroll is complete.
         */
        @Override
        public void onAlarm(Alarm alarm) {
            // Reorder immediately on page change.
            onDragOver(mDragObject, 1);
        }
    }
}

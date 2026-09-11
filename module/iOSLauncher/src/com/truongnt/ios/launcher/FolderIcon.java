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

package com.truongnt.ios.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.truongnt.ios.launcher.DropTarget.DragObject;
import com.truongnt.ios.launcher.FolderInfo.FolderListener;
import com.truongnt.ios.launcher.awareness.UnreadLoaderCompact;
import com.truongnt.ios.launcher.badge.BadgeRenderer;
import com.truongnt.ios.launcher.badge.FolderBadgeInfo;
import com.truongnt.ios.launcher.folder.Folder;
import com.truongnt.ios.launcher.graphics.IconPalette;
import com.truongnt.ios.launcher.theme.ThemeManager;
import com.truongnt.ios.launcher.util.Thunk;
import com.truongnt.ios.launcher.views.CustomTextView;
import com.truongnt.ios.launcher.BubbleTextView.ReorderHintAnimation;

import java.util.ArrayList;

/**
 * An icon that can appear on in the workspace representing an UserFolder.
 */
public class FolderIcon extends BaseFolderIcon implements FolderListener, IShakeInterface {
    private static final String TAG = "FolderIcon";

    Folder mFolder;
    private FolderInfo mInfo;

    private CheckLongPressHelper mLongPressHelper;
    private StylusEventHelper mStylusEventHelper;

    @Thunk
    BubbleTextView mFolderName;

    FolderRingAnimator mFolderRingAnimator = null;

    private PreviewItemDrawingParams mParams = new PreviewItemDrawingParams(0, 0, 0, 0);
    @Thunk
    PreviewItemDrawingParams mAnimParams = new PreviewItemDrawingParams(0, 0, 0, 0);
    @Thunk
    ArrayList<ShortcutInfo> mHiddenItems = new ArrayList<ShortcutInfo>();

    @Thunk
    ItemInfo mDragInfo;

    private boolean mContentVisible = true;

    private FolderBadgeInfo mBadgeInfo = new FolderBadgeInfo();
    private BadgeRenderer mBadgeRenderer;
    private float mBadgeScale;
    private Point mTempSpaceForBadgeOffset = new Point();
    private Rect mTempBounds = new Rect();
    private Paint mLeftPaint;

    private boolean mPressedDelIcon = false;
    /** Toạ độ điểm chạm ở ACTION_DOWN — để đo quãng di chuyển, biết lúc nào bắt đầu kéo folder. */
    private float mTouchDownX;
    private float mTouchDownY;
    private static final Property<FolderIcon, Float> BADGE_SCALE_PROPERTY = new Property<FolderIcon, Float>(Float.TYPE, "badgeScale") {
        @Override
        public Float get(FolderIcon folderIcon) {
            return folderIcon.mBadgeScale;
        }

        @Override
        public void set(FolderIcon folderIcon, Float value) {
            folderIcon.mBadgeScale = value;
            folderIcon.invalidate();
        }
    };

    public FolderIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FolderIcon(Context context) {
        super(context);
        init();
    }

    private void init() {
        mLongPressHelper = new CheckLongPressHelper(this);
        mStylusEventHelper = new StylusEventHelper(this);
        setAccessibilityDelegate(LauncherAppState.getInstance().getAccessibilityDelegate());
        this.mLeftPaint = new Paint();
        this.mLeftPaint.setFilterBitmap(true);
    }

    public boolean isDropEnabled() {
        final ViewGroup cellLayoutChildren = (ViewGroup) getParent();
        final ViewGroup cellLayout = (ViewGroup) cellLayoutChildren.getParent();
        final Workspace workspace = (Workspace) cellLayout.getParent();
        return !workspace.workspaceInModalState();
    }

    static FolderIcon fromXml(int resId, Launcher launcher, ViewGroup group,
                              FolderInfo folderInfo, IconCache iconCache) {
        @SuppressWarnings("all") // suppress dead code warning
        final boolean error = INITIAL_ITEM_ANIMATION_DURATION >= DROP_IN_ANIMATION_DURATION;
        if (error) {
            throw new IllegalStateException("DROP_IN_ANIMATION_DURATION must be greater than " +
                    "INITIAL_ITEM_ANIMATION_DURATION, as sequencing of adding first two items " +
                    "is dependent on this");
        }

        DeviceProfile grid = launcher.getDeviceProfile();

        FolderIcon icon = (FolderIcon) LayoutInflater.from(launcher).inflate(resId, group, false);
        icon.setClipToPadding(false);
        icon.mFolderName = (BubbleTextView) icon.findViewById(R.id.folder_icon_name);
        icon.mFolderName.setText(folderInfo.title);
        icon.mFolderName.setCompoundDrawablePadding(0);
        icon.mFolderName.setIsFolderName(true);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) icon.mFolderName.getLayoutParams();
        lp.topMargin = grid.iconSizePx + grid.iconDrawablePaddingPx;

        // Offset the preview background to center this view accordingly
        icon.mPreviewBackground = (ImageView) icon.findViewById(R.id.preview_background);

        ThemeManager themeManager = launcher.getThemeManager();
        Drawable background = themeManager.readFolderBgDrawable();
        icon.mPreviewBackground.setImageDrawable(background);

        lp = (FrameLayout.LayoutParams) icon.mPreviewBackground.getLayoutParams();
        lp.topMargin = grid.folderBackgroundOffset;
        lp.width = grid.folderIconSizePx;
        lp.height = grid.folderIconSizePx;

        icon.setTag(folderInfo);
        icon.setOnClickListener(launcher);
        icon.mInfo = folderInfo;
        icon.mLauncher = launcher;
        icon.mBadgeRenderer = launcher.getDeviceProfile().mBadgeRenderer;
        icon.setContentDescription(String.format(launcher.getString(R.string.folder_name_format),
                folderInfo.title));

        Folder fromXml = Folder.fromXml(launcher);
        fromXml.setDragController(launcher.getDragController());
        fromXml.setFolderIcon(icon);
        fromXml.bind(folderInfo);
        icon.mFolder = fromXml;

//        FolderRootLayout folderRootLayout = launcher.getRootFolderLayout();
//        folderRootLayout.bind(icon, folderInfo);
//        icon.mFolderLayout = folderRootLayout.getFolderLayout(folderInfo);

        icon.mFolderRingAnimator = new FolderRingAnimator(launcher, icon);
        folderInfo.addListener(icon);

        icon.setOnFocusChangeListener(launcher.mFocusHandler);
        return icon;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        sStaticValuesDirty = true;
        return super.onSaveInstanceState();
    }

    public Folder getFolder() {
        return mFolder;
    }

    public FolderInfo getFolderInfo() {
        return mInfo;
    }

    private boolean willAcceptItem(ItemInfo item) {
        final int itemType = item.itemType;
        return ((itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT ||
                itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT) &&
                item != mInfo);
    }

    public boolean acceptDrop(Object dragInfo) {
        final ItemInfo item = (ItemInfo) dragInfo;
        return willAcceptItem(item);
    }

    public void addItem(ShortcutInfo item) {
        mInfo.add(item);
    }

    public void onDragEnter(Object dragInfo) {
        if (!willAcceptItem((ItemInfo) dragInfo)) return;
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) getLayoutParams();
        CellLayout layout = (CellLayout) getParent().getParent();
        mFolderRingAnimator.setCell(lp.cellX, lp.cellY);
        mFolderRingAnimator.setCellLayout(layout);
        mFolderRingAnimator.animateToAcceptState();
        layout.showFolderAccept(mFolderRingAnimator);
        mDragInfo = (ItemInfo) dragInfo;
    }

    public void onDragOver(Object dragInfo) {
    }

    OnAlarmListener mOnOpenListener = new OnAlarmListener() {
        public void onAlarm(Alarm alarm) {
            ShortcutInfo item;
            if (mDragInfo instanceof AppInfo) {
                // Came from all apps -- make a copy.
                item = ((AppInfo) mDragInfo).makeShortcut();
                item.spanX = 1;
                item.spanY = 1;
            } else {
                // ShortcutInfo
                item = (ShortcutInfo) mDragInfo;
            }
            mLauncher.openFolder(FolderIcon.this);
        }
    };

    public void performCreateAnimation(final ShortcutInfo destInfo, final View destView,
                                       final ShortcutInfo srcInfo, final DragView srcView, Rect dstRect,
                                       float scaleRelativeToDragLayer, Runnable postAnimationRunnable) {

        // These correspond two the drawable and view that the icon was dropped _onto_
        Drawable animateDrawable = getTopDrawable((CustomTextView) destView);
        computePreviewDrawingParams(animateDrawable.getIntrinsicWidth(),
                destView.getMeasuredWidth());

        // This will animate the first item from it's position as an icon into its
        // position as the first item in the preview
        animateFirstItem(animateDrawable, INITIAL_ITEM_ANIMATION_DURATION, false, null);
        addItem(destInfo);

        // This will animate the dragView (srcView) into the new folder
        onDrop(srcInfo, srcView, dstRect, scaleRelativeToDragLayer, 1, postAnimationRunnable, null);
    }

    public void performDestroyAnimation(final View finalView, Runnable onCompleteRunnable) {
        Drawable animateDrawable = getTopDrawable((CustomTextView) finalView);
        computePreviewDrawingParams(animateDrawable.getIntrinsicWidth(), finalView.getMeasuredWidth());

        // This will animate the first item from it's position as an icon into its position as the first item in the preview
        animateFirstItem(animateDrawable, FINAL_ITEM_ANIMATION_DURATION, true, onCompleteRunnable);
    }

    public void onDragExit(Object dragInfo) {
        onDragExit();
    }

    public void onDragExit() {
        mFolderRingAnimator.animateToNaturalState();
    }

    private void onDrop(final ShortcutInfo item, DragView animateView, Rect finalRect,
                        float scaleRelativeToDragLayer, int index, Runnable postAnimationRunnable,
                        DragObject d) {
        item.cellX = -1;
        item.cellY = -1;

        // Typically, the animateView corresponds to the DragView; however, if this is being done
        // after a configuration activity (ie. for a Shortcut being dragged from AllApps) we
        // will not have a view to animate
        if (animateView != null) {
            DragLayer dragLayer = mLauncher.getDragLayer();
            Rect from = new Rect();
            dragLayer.getViewRectRelativeToSelf(animateView, from);
            Rect to = finalRect;
            if (to == null) {
                to = new Rect();
                Workspace workspace = mLauncher.getWorkspace();
                // Set cellLayout and this to it's final state to compute final animation locations
                workspace.setFinalTransitionTransform((CellLayout) getParent().getParent());
                float scaleX = getScaleX();
                float scaleY = getScaleY();
                setScaleX(1.0f);
                setScaleY(1.0f);
                scaleRelativeToDragLayer = dragLayer.getDescendantRectRelativeToSelf(this, to);
                // Finished computing final animation locations, restore current state
                setScaleX(scaleX);
                setScaleY(scaleY);
                workspace.resetTransitionTransform((CellLayout) getParent().getParent());
            }

            int[] center = new int[2];
            float scale = getLocalCenterForIndex(index, center);
            center[0] = Math.round(scaleRelativeToDragLayer * center[0]);
            center[1] = Math.round(scaleRelativeToDragLayer * center[1]);

            to.offset(center[0] - animateView.getMeasuredWidth() / 2,
                    center[1] - animateView.getMeasuredHeight() / 2);

            float finalAlpha = index < NUM_ITEMS_IN_PREVIEW ? 0.5f : 0f;

            float finalScale = scale * scaleRelativeToDragLayer;
            dragLayer.animateView(animateView, from, to, finalAlpha,
                    1, 1, finalScale, finalScale, DROP_IN_ANIMATION_DURATION,
                    new DecelerateInterpolator(2), new AccelerateInterpolator(2),
                    postAnimationRunnable, DragLayer.ANIMATION_END_DISAPPEAR, null);
            addItem(item);
            mHiddenItems.add(item);
//            mFolderLayout.hideItem(item);
            postDelayed(new Runnable() {
                public void run() {
                    mHiddenItems.remove(item);
//                    mFolderLayout.showItem(item);
                    invalidate();
                }
            }, DROP_IN_ANIMATION_DURATION);
        } else {
            addItem(item);
        }
    }

    public void onDrop(DragObject d) {
        ShortcutInfo item;
        if (d.dragInfo instanceof AppInfo) {
            // Came from all apps -- make a copy
            item = (AppInfo) d.dragInfo;
        } else {
            item = (ShortcutInfo) d.dragInfo;
        }
//        mFolderLayout.notifyDrop();
        onDrop(item, d.dragView, null, 1.0f, mInfo.contents.size(), d.postAnimationRunnable, d);
    }

    private void computePreviewDrawingParams(Drawable d) {
        computePreviewDrawingParams(d.getIntrinsicWidth(), getMeasuredWidth());
    }

    public void setBadgeInfo(FolderBadgeInfo badgeInfo) {
        updateBadgeScale(mBadgeInfo.hasBadge(), badgeInfo.hasBadge());
        mBadgeInfo = badgeInfo;
    }

    /**
     * Sets mBadgeScale to 1 or 0, animating if wasBadged or isBadged is false
     * (the badge is being added or removed).
     */
    private void updateBadgeScale(boolean wasBadged, boolean isBadged) {
        float newBadgeScale = isBadged ? 1f : 0f;
        // Animate when a badge is first added or when it is removed.
        if ((wasBadged ^ isBadged) && isShown()) {
            ObjectAnimator.ofFloat(this, BADGE_SCALE_PROPERTY, newBadgeScale).start();
        } else {
            mBadgeScale = newBadgeScale;
            invalidate();
        }
    }

    /**
     * [BỎ DẤU TRỪ FOLDER] Luôn trả false: folder KHÔNG còn nút xoá ở góc trên-trái.
     *
     * Dấu trừ đã bỏ khỏi {@code onDraw}; nếu vẫn giữ vùng chạm cũ thì góc đó thành nút VÔ HÌNH —
     * chạm nhầm là xoá cả folder. Giữ lại hàm (thay vì gỡ chỗ gọi) để luồng onTouchEvent không
     * đổi cấu trúc, chỉ rẽ nhánh "không phải dấu trừ" -> mở folder / long-press như bình thường.
     */
    private boolean checkUninstallPressed(int x, int y) {
        return false;
    }
    public float getLocalCenterForIndex(int index, int[] center) {
        mParams = computePreviewItemDrawingParams(Math.min(NUM_ITEMS_IN_PREVIEW - 1, index), mParams);

        mParams.transX += mPreviewOffsetX;
        mParams.transY += mPreviewOffsetY;
        float offsetX = mParams.transX + (mParams.scale * mIntrinsicIconSize) / 2;
        float offsetY = mParams.transY + (mParams.scale * mIntrinsicIconSize) / 2;

        center[0] = Math.round(offsetX);
        center[1] = Math.round(offsetY);
        return mParams.scale;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mFolder == null || !mContentVisible) return;
        if (mFolder.getItemCount() == 0 && !mAnimating) return;

        ArrayList<View> items = mFolder.getItemsInReadingOrder();
        if (items.size() <= 0)
            return;

        Drawable d;
        CustomTextView v;

        // Update our drawing parameters if necessary
        if (mAnimating) {
            computePreviewDrawingParams(mAnimParams.drawable);
        } else {
            v = (CustomTextView) items.get(0);
            d = getTopDrawable(v);
            computePreviewDrawingParams(d);
        }

        int nItemsInPreview = Math.min(items.size(), NUM_ITEMS_IN_PREVIEW);
        if (!mAnimating) {
            for (int i = nItemsInPreview - 1; i >= 0; i--) {
                v = (CustomTextView) items.get(i);
                if (!mHiddenItems.contains(v.getTag())) {
                    d = getTopDrawable(v);
                    mParams = computePreviewItemDrawingParams(i, mParams);
                    mParams.drawable = d;
                    drawPreviewItem(canvas, mParams);
                }
            }
        } else {
            drawPreviewItem(canvas, mAnimParams);
        }

        UnreadLoaderCompact.drawUnreadEventIfNeed(mLauncher, canvas, this);
        drawBadge(canvas);
        // [BỎ DẤU TRỪ FOLDER] Không vẽ dấu trừ khi bật edit mode nữa (theo yêu cầu): folder chỉ
        // xoá bằng cách kéo hết app ra ngoài. Vùng chạm cũng đã bỏ ở checkUninstallPressed() để
        // không còn nút vô hình. Giữ nguyên dấu trừ của APP và WIDGET.
    }

    public void drawBadge(Canvas canvas) {
        if ((mBadgeInfo != null && mBadgeInfo.hasBadge()) || mBadgeScale > 0) {
            getIconBounds(mTempBounds);
            mTempSpaceForBadgeOffset.set(mTempBounds.left, mTempBounds.top);
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            canvas.translate(scrollX, scrollY);
            IconPalette badgePalette = IconPalette.getFolderBadgePalette(getResources());
            mBadgeRenderer.drawFolderBadge(canvas, badgePalette, mBadgeInfo, mTempBounds, mBadgeScale, mTempSpaceForBadgeOffset);
            canvas.translate(-scrollX, -scrollY);
        }
    }

    public void drawDelIcon(Canvas canvas) {
        int scrollX = getScrollX();
        int scrollY = getScrollY();
        float scale = DelIconAnim.getScale();
        boolean isRefresh = DelIconAnim.shouldRefresh();
        if (isRefresh) {
            canvas.save();
            canvas.translate((float) scrollX, (float) scrollY);

            // Đồng bộ cỡ với app/widget (45% icon) và neo tâm tại GÓC TRÊN-TRÁI của nền folder (face).
            // Trước đây hằng 60px + gốc (0,0) = mép view -> folder lệch cỡ và lệch vị trí.
            int delSize = (int) (mLauncher.getDeviceProfile().iconSizePx * 0.45f);
            int half = (int) (delSize * scale * .5f);
            int left = folderFaceLeft() - half;
            int top = folderFaceTop() - half;

            Rect rect = new Rect(
                    left,
                    top,
                    (int) (((float) left) + (((float) delSize) * scale)),
                    (int) (((float) top) + (((float) delSize) * scale)));

            this.mLeftPaint.setAlpha((int) (scale * 255.0f));
            VectorDrawable drawable = (VectorDrawable)getContext().getResources().getDrawable(R.drawable.delete_button);
            drawable.setBounds(rect);
            drawable.draw(canvas);
            canvas.translate((float) (-scrollX), (float) (-scrollY));
            canvas.restore();
        }
    }

    /** X trái của nền folder (face) trong view; fallback về icon bounds nếu chưa có. */
    private int folderFaceLeft() {
        if (mPreviewBackground != null && mPreviewBackground.getWidth() > 0) {
            return mPreviewBackground.getLeft();
        }
        Rect b = new Rect();
        getIconBounds(b);
        return b.left;
    }

    /** Y trên của nền folder (face) trong view; fallback về icon bounds nếu chưa có. */
    private int folderFaceTop() {
        if (mPreviewBackground != null && mPreviewBackground.getHeight() > 0) {
            return mPreviewBackground.getTop();
        }
        Rect b = new Rect();
        getIconBounds(b);
        return b.top;
    }
    public void getIconBounds(Rect outBounds) {
        int iconSize = mLauncher.getDeviceProfile().iconSizePx;
        int top = getPaddingTop();
        int left = (getWidth() - iconSize) / 2;
        int right = left + iconSize;
        int bottom = top + iconSize;
        outBounds.set(left, top, right, bottom);
    }

    public void showContent(boolean visible) {
        mContentVisible = visible;
        invalidate();
    }

    private Drawable getTopDrawable(CustomTextView v) {
        Drawable d = v.getCompoundDrawables()[1];
        return (d instanceof PreloadIconDrawable) ? ((PreloadIconDrawable) d).mIcon : d;
    }

    private void animateFirstItem(final Drawable d, int duration, final boolean reverse,
                                  final Runnable onCompleteRunnable) {
        final PreviewItemDrawingParams finalParams = computePreviewItemDrawingParams(0, null);

        float iconSize = mLauncher.getDeviceProfile().iconSizePx;
        final float scale0 = iconSize / d.getIntrinsicWidth();
        final float transX0 = (mAvailableSpaceInPreview - iconSize) / 2;
        final float transY0 = (mAvailableSpaceInPreview - iconSize) / 2 + getPaddingTop();
        mAnimParams.drawable = d;

        ValueAnimator va = LauncherAnimUtils.ofFloat(this, 0f, 1.0f);
        va.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float progress = (Float) animation.getAnimatedValue();
                if (reverse) {
                    progress = 1 - progress;
                    mPreviewBackground.setAlpha(progress);
                }

                mAnimParams.transX = transX0 + progress * (finalParams.transX - transX0);
                mAnimParams.transY = transY0 + progress * (finalParams.transY - transY0);
                mAnimParams.scale = scale0 + progress * (finalParams.scale - scale0);
                invalidate();
            }
        });
        va.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimating = false;
                if (onCompleteRunnable != null) {
                    onCompleteRunnable.run();
                }
            }
        });
        va.setDuration(duration);
        va.start();
    }

    public void setTextVisible(boolean visible) {
        if (visible) {
            mFolderName.setVisibility(VISIBLE);
        } else {
            mFolderName.setVisibility(INVISIBLE);
        }
    }

    public void setTextColor(int color) {
        mFolderName.setTextColor(color);
    }

    public void setTextSizePx(float size) {
        mFolderName.setTextSizePx(size);
    }

    public void scaleIconSize(float scale) {
        mFolderName.scaleIconSize(scale);
    }

    public boolean getTextVisible() {
        return mFolderName.getVisibility() == VISIBLE;
    }

    public void onItemsChanged() {
        invalidate();
        requestLayout();
    }

    public void onAdd(ShortcutInfo item) {
        final ComponentName componentName = item.intent.getComponent();
        updateFolderUnreadNum(componentName, item.unreadNum);

        boolean wasBadged = mBadgeInfo.hasBadge();
        mBadgeInfo.addBadgeInfo(mLauncher.getPopupDataProvider().getBadgeInfoForItem(item));
        boolean isBadged = mBadgeInfo.hasBadge();
        updateBadgeScale(wasBadged, isBadged);

        invalidate();
        requestLayout();
    }

    public void onRemove(ShortcutInfo item) {
        final ComponentName componentName = item.intent.getComponent();
        updateFolderUnreadNum(componentName, item.unreadNum);

        boolean wasBadged = mBadgeInfo.hasBadge();
        mBadgeInfo.subtractBadgeInfo(mLauncher.getPopupDataProvider().getBadgeInfoForItem(item));
        boolean isBadged = mBadgeInfo.hasBadge();
        updateBadgeScale(wasBadged, isBadged);

        invalidate();
        requestLayout();
    }

    public void onTitleChanged(CharSequence title) {
        mFolderName.setText(title);
        setContentDescription(String.format(getContext().getString(R.string.folder_name_format),
                title));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Call the superclass onTouchEvent first, because sometimes it changes the state to
        // isPressed() on an ACTION_UP
        boolean result = super.onTouchEvent(event);

        // Check for a stylus button press, if it occurs cancel any long press checks.
        if (mStylusEventHelper.checkAndPerformStylusEvent(event)) {
            mLongPressHelper.cancelLongPress();
            return true;
        }

        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Mốc đo quãng di chuyển để biết khi nào bắt đầu kéo (xem ACTION_MOVE).
                mTouchDownX = event.getX();
                mTouchDownY = event.getY();
                if (!checkUninstallPressed(x, y)) {
                    mLongPressHelper.postCheckForLongPress();
                    this.mPressedDelIcon = false;
                } else {
                    this.mPressedDelIcon = true;
                    cancelLongPress();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (this.mPressedDelIcon) {
                    post(new Runnable() {
                        public void run() {
                            FolderIcon.this.mLauncher.removeFolder(FolderIcon.this.mInfo);
                        }
                    });
                } else {
                    this.mPressedDelIcon = false;
                    // Nhấc tay mà long-press CHƯA nổ và ngón còn trong view -> đây là một cú CHẠM:
                    // tự gọi click để mở folder. Cần vì onTouchEvent nay trả true (giữ chuỗi touch
                    // cho long-press/kéo thả), khiến super không còn tự phát click nữa.
                    if (!mLongPressHelper.hasPerformedLongPress()
                            && Utilities.pointInView(this, event.getX(), event.getY(), mSlop)) {
                        performClick();
                    }
                }

                mLongPressHelper.cancelLongPress();
                break;
            case MotionEvent.ACTION_MOVE:
                // [FIX] "Giữ liền rồi kéo folder" — giống thao tác với icon app.
                //
                // Bắt đầu kéo NGAY khi ngón di quá touchSlop, KHÔNG cần bật edit mode trước.
                // Nếu chưa ở edit thì tự bật (startTidyUp) rồi nhấc folder lên kéo luôn.
                //
                // Vì sao không trông chờ long-press: ACTION_MOVE gọi cancelLongPress() ngay khi
                // ngón nhích ra ngoài view — mà kéo thì đương nhiên đi ra ngoài — nên long-press
                // bị huỷ trước khi kịp nổ.
                // Nhấc folder lên kéo khi ngón di quá touchSlop — CHỈ trong edit mode.
                //
                // NGOÀI edit mode, giữ liền phải MỞ POPUP 3 mục (Delete Folder / Edit Home Screen
                // / Rename) giống app, nên ở đó để long-press nổ bình thường; muốn kéo thì giữ cho
                // popup hiện rồi kéo tiếp — DragLayer lo qua startDragFromContextPopup().
                if (DragLayer.sTidyUping
                        && mLauncher != null
                        && !mLauncher.isFolderOpen()          // folder đang mở -> không nhấc kéo
                        && (getTag() instanceof ItemInfo)
                        && mLauncher.getDragController() != null
                        && !mLauncher.getDragController().isDragging()
                        && (Math.abs(event.getX() - mTouchDownX) > mSlop
                            || Math.abs(event.getY() - mTouchDownY) > mSlop)) {
                    mLongPressHelper.cancelLongPress();
                    mLauncher.closeFloatingMenu();
                    // Chỉ gọi startDrag (KHÔNG kèm showInfo): hai hàm này làm trùng việc nhau
                    // (cùng set mDragInfo, ẩn child, prepareChildForDrag) — gọi cả hai sẽ ẩn
                    // child hai lần và ghi đè trạng thái drag.
                    CellLayout.CellInfo cellInfo =
                            new CellLayout.CellInfo(this, (ItemInfo) getTag());
                    mLauncher.getWorkspace().startDrag(cellInfo, false);
                    break;
                }
                if (!Utilities.pointInView(this, event.getX(), event.getY(), mSlop)) {
                    mLongPressHelper.cancelLongPress();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                this.mLongPressHelper.cancelLongPress();
                this.mPressedDelIcon = false;
                break;
        }
        // [FIX] Không kéo được folder để đổi vị trí.
        //
        // super.onTouchEvent() trả FALSE khi view không ở trạng thái clickable — trả thẳng giá trị
        // đó ra thì FolderIcon "buông" chuỗi touch ngay sau ACTION_DOWN, nên ACTION_MOVE/UP không
        // về nữa và long-press (mốc bắt đầu kéo) không bao giờ nổ.
        //
        // Trước đây nhánh dấu trừ đôi lúc trả true nên che lấp vấn đề; sau khi bỏ dấu trừ folder
        // thì lộ ra. Có listener click/long-click thì coi như đã xử lý — giống cách BubbleTextView
        // (icon app) đang làm, nhờ vậy kéo thả folder hoạt động như kéo app.
        if (isClickable() || isLongClickable()) {
            return true;
        }
        return result;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mLongPressHelper.cancelLongPress();
    }

    public void setFolderUnreadNum(int unreadNum) {
        if (unreadNum <= 0) {
            mInfo.unreadNum = 0;
        } else {
            mInfo.unreadNum = unreadNum;
        }
    }

    /**
     * M: Update unread number of the folder, the number is the total unread number
     * of all shortcuts in folder, duplicate shortcut will be only count once.
     */
    public void updateFolderUnreadNum() {
        final ArrayList<ShortcutInfo> contents = mInfo.contents;
        final int contentsCount = contents.size();
        int unreadNumTotal = 0;
        final ArrayList<ComponentName> components = new ArrayList<ComponentName>();
        ShortcutInfo shortcutInfo = null;
        ComponentName componentName = null;
        int unreadNum = 0;
        for (int i = 0; i < contentsCount; i++) {
            shortcutInfo = contents.get(i);
            componentName = shortcutInfo.intent.getComponent();
            unreadNum = UnreadLoaderCompact.getUnreadNumberOfComponent(componentName);
            if (unreadNum > 0) {
                shortcutInfo.unreadNum = unreadNum;
                int j = 0;
                for (j = 0; j < components.size(); j++) {
                    if (componentName != null && componentName.equals(components.get(j))) {
                        break;
                    }
                }

                if (j >= components.size()) {
                    components.add(componentName);
                    unreadNumTotal += unreadNum;
                }
            }
        }
        setFolderUnreadNum(unreadNumTotal);
    }

    /**
     * M: Update the unread message of the shortcut with the given information.
     *
     * @param unreadNum the number of the unread message.
     */
    public void updateFolderUnreadNum(ComponentName component, int unreadNum) {
        final ArrayList<ShortcutInfo> contents = mInfo.contents;
        final int contentsCount = contents.size();
        int unreadNumTotal = 0;
        ShortcutInfo appInfo = null;
        ComponentName name = null;
        final ArrayList<ComponentName> components = new ArrayList<ComponentName>();
        for (int i = 0; i < contentsCount; i++) {
            appInfo = contents.get(i);
            name = appInfo.intent.getComponent();
            if (name != null && name.equals(component)) {
                appInfo.unreadNum = unreadNum;
            }
            if (appInfo.unreadNum > 0) {
                int j = 0;
                for (j = 0; j < components.size(); j++) {
                    if (name != null && name.equals(components.get(j))) {
                        break;
                    }
                }
                if (j >= components.size()) {
                    components.add(name);
                    unreadNumTotal += appInfo.unreadNum;
                }
            }
        }
        setFolderUnreadNum(unreadNumTotal);
    }

    public int getSmallIconSize() {
//        if (this.mBaselineIconScale == 0.0f) {
//            computePreviewDrawingParams(this.mPreviewBackground.getWidth(), getMeasuredWidth(), getMeasuredHeight());
//        }
        return this.mBaselineIconSize;
    }

    private ReorderHintAnimation mShakeAnimators = null;
    @Override
    public void beginOrAdjustHintAnimations() {
        if (this.mShakeAnimators == null) {
            this.mShakeAnimators = new ReorderHintAnimation(this);
            this.mShakeAnimators.animate();
        }
    }

    @Override
    public void beginOrAdjustHintAnimations(int i) {
        if (this.mShakeAnimators == null) {
            this.mShakeAnimators = new ReorderHintAnimation(this);
            this.mShakeAnimators.animate(i);
        }
    }

    @Override
    public void completeAndClearReorderHintAnimations() {
        if (this.mShakeAnimators != null) {
            this.mShakeAnimators.completeAnimationImmediately();
            this.mShakeAnimators = null;
        }
    }

    @Override
    public void joinAnimations(View view) {
        if (this.mShakeAnimators != null && (view instanceof IShakeInterface)) {
            view.clearAnimation();
            view.setScaleX(1.0f);
            view.setScaleY(1.0f);
            view.setTranslationX(0.0f);
            view.setTranslationY(0.0f);
            this.mShakeAnimators.join(view);
        }
    }

    public void dropNoAnimation(DropTarget.DragObject dragObject) {
        this.mFolder.notifyDrop();
//        onDrop((ShortcutInfo) dragObject.dragInfo, (DragView) null, (Rect) null, 1.0f, this.mInfo.contents.size(), dragObject.postAnimationRunnable);
    }

    public boolean updateAppInfo(AppInfo applicationInfo) {
        if (!this.mInfo.opened) {
            return this.mFolder.updateAppInfo(applicationInfo);
        }
        Log.w(TAG, "app.screen != mInfo.screenId, return false");
        return false;
    }
}

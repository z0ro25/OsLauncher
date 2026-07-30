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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;

import com.amz.ios.ioslite.common.Partner;

import java.util.ArrayList;

public class Hotseat extends FrameLayout implements Stats.LaunchSourceProvider {
    private static final String TAG = "Hotseat";
    private IconCache mIconCache;
    public CellLayout mContent;
    private Launcher mLauncher;
    private final boolean mHasVerticalHotseat;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLauncher = (Launcher) context;
        mIconCache = LauncherAppState.getInstance().getIconCache();
        mHasVerticalHotseat = mLauncher.getDeviceProfile().isVerticalBarLayout();
        mChangeView = false;
        this.mSwitchItemInfo = null;

    }

    public CellLayout getLayout() {
        return mContent;
    }

    /**
     * Returns whether there are other icons than the all apps button in the hotseat.
     */
    public boolean hasIcons() {
        return mContent.getShortcutsAndWidgets().getChildCount() > 1;
    }

    /**
     * Registers the specified listener on the cell layout of the hotseat.
     */
    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        mContent.setOnLongClickListener(l);
    }

    /* Get the orientation invariant order of the item in the hotseat for persistence. */
    int getOrderInHotseat(int x, int y) {
        return mHasVerticalHotseat ? (mContent.getCountY() - y - 1) : x;
    }

    /* Get the orientation specific coordinates given an invariant order in the hotseat. */
    int getCellXFromOrder(int rank) {
        return mHasVerticalHotseat ? 0 : rank;
    }

    int getCellYFromOrder(int rank) {
        return mHasVerticalHotseat ? (mContent.getCountY() - (rank + 1)) : 0;
    }

    private View mDockGlassBg;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        DeviceProfile grid = mLauncher.getDeviceProfile();
        mContent = (CellLayout) findViewById(R.id.layout);
        mDockGlassBg = findViewById(R.id.dock_glass_bg);

        mContent.setIsHotseat(true);

        resetLayout();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        positionDockGlass();
    }

    /**
     * CellLayout luôn đo full-width nên khung glass không thể "wrap" hàng icon
     * bằng XML. Ở đây tính bounding-box THẬT của các icon dock (union các cell
     * có child) rồi đặt khung glass ôm sát khối icon, cộng thêm padding.
     */
    private void positionDockGlass() {
        if (mDockGlassBg == null || mContent == null) return;

        ShortcutAndWidgetContainer container = mContent.getShortcutsAndWidgets();
        int childCount = container.getChildCount();
        if (childCount == 0) {
            mDockGlassBg.setVisibility(GONE);
            return;
        }

        // Union bounds của các icon, quy về hệ toạ độ của Hotseat (RelativeLayout parent).
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        boolean any = false;
        for (int i = 0; i < childCount; i++) {
            View child = container.getChildAt(i);
            if (child == null || child.getVisibility() == GONE) continue;
            any = true;
            int cl = container.getLeft() + mContent.getLeft() + child.getLeft();
            int ct = container.getTop() + mContent.getTop() + child.getTop();
            minX = Math.min(minX, cl);
            minY = Math.min(minY, ct);
            maxX = Math.max(maxX, cl + child.getWidth());
            maxY = Math.max(maxY, ct + child.getHeight());
        }
        if (!any) {
            mDockGlassBg.setVisibility(GONE);
            return;
        }

        int padH = getResources().getDimensionPixelSize(R.dimen.dock_glass_padding_horizontal);
        int padV = getResources().getDimensionPixelSize(R.dimen.dock_glass_padding_vertical);
        int gl = minX - padH;
        int gt = minY - padV;
        int gr = maxX + padH;
        int gb = maxY + padV;

        View parent = (View) mDockGlassBg.getParent();
        gl = Math.max(gl, 0);
        gt = Math.max(gt, 0);
        if (parent != null) {
            gr = Math.min(gr, parent.getWidth());
            gb = Math.min(gb, parent.getHeight());
        }

        // Khung glass là sibling của @id/layout trong RelativeLayout; toạ độ trên
        // đã tính theo hệ Hotseat, mà RelativeLayout khớp full Hotseat nên dùng luôn.
        int newW = gr - gl;
        int newH = gb - gt;

        android.view.ViewGroup.MarginLayoutParams lp =
                (android.view.ViewGroup.MarginLayoutParams) mDockGlassBg.getLayoutParams();
        // Guard: chỉ setLayoutParams khi giá trị đổi -> tránh vòng lặp requestLayout.
        boolean changed = lp.width != newW || lp.height != newH
                || lp.leftMargin != gl || lp.topMargin != gt;
        if (changed) {
            lp.width = newW;
            lp.height = newH;
            if (lp instanceof android.widget.RelativeLayout.LayoutParams) {
                android.widget.RelativeLayout.LayoutParams rlp =
                        (android.widget.RelativeLayout.LayoutParams) lp;
                rlp.removeRule(android.widget.RelativeLayout.ALIGN_TOP);
                rlp.removeRule(android.widget.RelativeLayout.ALIGN_BOTTOM);
                rlp.removeRule(android.widget.RelativeLayout.ALIGN_START);
                rlp.removeRule(android.widget.RelativeLayout.ALIGN_LEFT);
            }
            lp.leftMargin = gl;
            lp.topMargin = gt;
            mDockGlassBg.setLayoutParams(lp);
        }
        if (mDockGlassBg.getVisibility() != VISIBLE) {
            mDockGlassBg.setVisibility(VISIBLE);
        }
    }

    void resetLayout() {

        mContent.removeAllViewsInLayout();

        DeviceProfile grid = mLauncher.getDeviceProfile();

        if (grid.isLandscape && !grid.isLargeTablet) {
            mContent.setGridSize(1, (int) grid.inv.numHotseatIcons);
        } else {
            mContent.setGridSize((int) grid.inv.numHotseatIcons, 1);
        }

        /*
        boolean allAppsEnable = LauncherAppState.isAllAppsEnable();
        if (allAppsEnable) {
            // Add the Apps button
            Context context = getContext();

            LayoutInflater inflater = LayoutInflater.from(context);
            BubbleTextView allAppsButton = (BubbleTextView)
                    inflater.inflate(R.layout.app_icon, mContent, false);

            Drawable d = mIconCache.getAllAppsDrawable();

            mLauncher.resizeIconDrawable(d);
            allAppsButton.setCompoundDrawables(null, d, null, null);

            allAppsButton.setContentDescription(context.getString(R.string.all_apps_button_label));
            if (mLauncher != null) {
                mLauncher.setAllAppsButton(allAppsButton);
                allAppsButton.setOnTouchListener(mLauncher.getHapticFeedbackTouchListener());
                allAppsButton.setOnClickListener(mLauncher);
                allAppsButton.setOnLongClickListener(mLauncher);
                allAppsButton.setOnFocusChangeListener(mLauncher.mFocusHandler);
            }

            // Note: We do this to ensure that the hotseat is always laid out in the orientation of
            // the hotseat in order regardless of which orientation they were added
            int x = getCellXFromOrder(mAllAppsButtonRank);
            int y = getCellYFromOrder(mAllAppsButtonRank);
            CellLayout.LayoutParams lp = new CellLayout.LayoutParams(x, y, 1, 1);
            lp.canReorder = false;

            mContent.addViewToCellLayout(allAppsButton, -1, allAppsButton.getId(), lp, true);

        }

         */
    }

    public final void d() {
        ViewPropertyAnimator translationY;
        int i10 = 0;
        int i11;
        float f10;

        int count = mContent.getAppChildCount();
        if (count == 0) {
            return;
        }

        InvariantDeviceProfile profile = LauncherAppState.getInstance().getInvariantDeviceProfile();
        int iconSize = profile.iconBitmapSize;

        int i12 = getResources().getDisplayMetrics().widthPixels;

        mContent.getAppChildAt(0);
        float f11 = i12;
        int i13 = (int) ((23.0f * f11) / 100.0f);
        int size = (int) (((((f11 * 24.8f) / 100.0f) - iconSize / 2.0f) - 0));
        if (iconSize != 1) {
            if (iconSize == 2) {
                float f12 = size;
                i10 = i12 / 2;
                this.mContent.getAppChildAt(0).animate().translationY(f12).translationX(i10 - i13).setDuration(260L).start();
                translationY = this.mContent.getAppChildAt(1).animate().translationY(f12);
            } else if (iconSize == 3) {
                float f13 = size;
                this.mContent.getAppChildAt(0).animate().translationY(f13).translationX((i12 / 2) - ((i13 * 3) / 2)).setDuration(260L).start();
                this.mContent.getAppChildAt(1).animate().translationY(f13).translationX((i12 - i13) / 2.0f).setDuration(260L).start();
                translationY = this.mContent.getAppChildAt(2).animate().translationY(f13);
                i11 = i12 + i13;
            } else {
                float f14 = size;
                int i14 = i12 / 2;
                mContent.getAppChildAt(0).animate().translationY(f14).translationX(i14 - (i13 * 2)).setDuration(260L).start();
                mContent.getAppChildAt(1).animate().translationY(f14).translationX(i14 - i13).setDuration(260L).start();
                mContent.getAppChildAt(2).animate().translationY(f14).translationX(i14).setDuration(260L).start();
                translationY = mContent.getAppChildAt(3).animate().translationY(f14);
                i10 = i14 + i13;
            }
            f10 = i10;
            translationY.translationX(f10).setDuration(260L).start();
        }
        translationY = this.mContent.getAppChildAt(0).animate().translationY(size);
        i11 = i12 - i13;
        f10 = i11 / 2.0f;
        translationY.translationX(f10).setDuration(260L).start();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // We don't want any clicks to go through to the hotseat unless the workspace is in
        // the normal state.
        return mLauncher.getWorkspace().workspaceInModalState();
    }

    @Override
    public void fillInLaunchSourceData(Bundle sourceData) {
        sourceData.putString(Stats.SOURCE_EXTRA_CONTAINER, Stats.CONTAINER_HOTSEAT);
    }

    void changeIconTextColor(int color) {
        boolean visible = Partner.getBoolean(mLauncher, Partner.DEF_HOTSEAT_LABEL_SHOW_ENABLE);

        if (visible) {
            int count = mContent.getShortcutsAndWidgets().getChildCount();
            for (int childIndex = 0; childIndex < count; childIndex++) {
                View v = mContent.getShortcutsAndWidgets().getChildAt(childIndex);

                if (v instanceof FolderIcon) {
                    FolderIcon fi = (FolderIcon) v;
                    fi.setTextColor(color);
                }

                if (v instanceof BubbleTextView) {
                    BubbleTextView bb = (BubbleTextView) v;
                    bb.setTextColor(color);
                }
            }
        }
    }

    public void removeView(ItemInfo itemInfo) {
        boolean success = mContent.removeItem((ShortcutInfo) itemInfo, true);

        if (!success)
            Log.i(TAG, "removeView info not exists or not a bubbleTextView.");
    }

    public void removeViewForAllAppMode(AppInfo appInfo) {
        boolean success = mContent.removeItem((ShortcutInfo) appInfo, true);

        if (!success)
            Log.i(TAG, "removeView info not exists or not a bubbleTextView.");
    }



    public void removeView(FolderInfo folderInfo, ItemInfo itemInfo) {
        for (int childCount = mContent.getAppChildCount() - 1; childCount >= 0; childCount--) {
            View childAt = mContent.getAppChildAt(childCount);
            if ((childAt instanceof FolderIcon) && childAt.getTag() == folderInfo) {
                folderInfo.remove((ShortcutInfo) itemInfo);

                if (folderInfo.contents.size() <= 0) {
                    mContent.removeView(childAt);
                }
                break;
            }
        }
    }

    public void removeShortcut(ShortcutInfo shortcutInfo) {
        if (shortcutInfo != null) {
            ArrayList arrayList = new ArrayList();
            int childCount = mContent.getAppChildCount();
            int i = 0;
            while (true) {
                if (i >= childCount) {
                    break;
                }
                View childAt = mContent.getAppChildAt(i);
                ItemInfo itemInfo = (ItemInfo) childAt.getTag();
                if (!(itemInfo instanceof FolderInfo)) {
                    if ((itemInfo instanceof ShortcutInfo) && ((ShortcutInfo) itemInfo) == shortcutInfo) {
                        arrayList.add(childAt);
                        break;
                    }
                } else {
                    FolderInfo folderInfo = (FolderInfo) itemInfo;
                    if (folderInfo.contents.contains(shortcutInfo)) {
                        folderInfo.remove(shortcutInfo);
                        LauncherModel.deleteItemFromDatabase(this.mLauncher, shortcutInfo);
                        break;
                    }
                }
                i++;
            }

            int size = arrayList.size();
            for (int i2 = 0; i2 < size; i2++) {
                View view = (View) arrayList.get(i2);
                ItemInfo itemInfo2 = (ItemInfo) view.getTag();
                removeView(view);
                LauncherModel.deleteItemFromDatabase(this.mLauncher, itemInfo2);
            }
        }
    }

    public void beginShakeAnimations() {
        if (mContent != null) {
            mContent.beginShakeAnimations();
        }
    }

    public void stopShakeAnimations() {
        if (mContent != null) {
            mContent.stopShakeAnimations();
        }
    }


    public boolean getChangeViewFlag() {
        return this.mChangeView;
    }

    public ItemInfo getSwitchInfo() {
        return this.mSwitchItemInfo;
    }

    private ItemInfo mSwitchItemInfo;
    private boolean mChangeView;
}

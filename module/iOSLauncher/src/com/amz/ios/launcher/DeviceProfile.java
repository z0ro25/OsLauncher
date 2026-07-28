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

import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.setting.IOSSettings;
import com.amz.ios.ioslite.common.util.DeviceInfoUtil;
import com.amz.ios.ioslite.common.util.DisplayUtil;
import com.amz.ios.launcher.assembly.SearchWidgetUtil;
import com.amz.ios.launcher.badge.BadgeRenderer;
import com.amz.ios.launcher.config.Settings;

public class DeviceProfile {

    private Context mContext;
    public final InvariantDeviceProfile inv;

    public final int hotSeatBarMarginHeight = 20;

    // Device properties
    public final boolean isTablet;
    public final boolean isLargeTablet;
    public final boolean isPhone;
    public final boolean transposeLayoutWithOrientation;

    // Device properties in current orientation
    public final boolean isLandscape;
    public final int widthPx;
    public final int heightPx;
    public final int availableWidthPx;
    public final int availableHeightPx;

    // Overview mode
    public int panelBotomMenuHeightPx;
    private int panelTopMenuHeightPx;

    // Workspace
    public int defaultHomeButtonHeightPx;
    private int desiredWorkspaceLeftRightMarginPx;
    public final int edgeMarginPx;
    public final int edgeMarginTop;
    public final Rect defaultWidgetPadding;
    private final int pageIndicatorHeightPx;
    private final int defaultPageSpacingPx;
    private float dragViewScale;

    // Workspace icons
    public int originalIconSizePx;
    public int iconSizePx;
    public int originalIconTextSizePx;
    public int iconTextSizePx;
    public int iconDrawablePaddingPx;
    public int iconDrawablePaddingOriginalPx;

    public int cellWidthPx;
    public int cellHeightPx;

    public int workspaceIconTextColor;
    public float workspaceIconTextSizeScale;
    public float workspaceIconSizeScale;
    public int workspaceIconTextFont;

    // Folder
    public int folderBackgroundOffset;
    public int folderIconSizePx;
    public int folderCellWidthPx;
    public int folderCellHeightPx;

    // Hotseat
    public int hotseatCellWidthPx;
    public int hotseatCellHeightPx;
    public int hotseatIconSizePx;
    public int hotseatBarHeightPx;

    // All apps
    public int allAppsNumCols;
    public int allAppsNumPredictiveCols;
    public final int allAppsIconSizePx;
    public final int allAppsIconTextSizePx;

    // QSB
    public boolean searchBarVisible;
    private int searchBarSpaceWidthPx;
    private int searchBarSpaceHeightPx;
    private int defaultSearchBarHeightPx;
    private int defaultDropTargetBarHeightPx;

    // Widgets
    public int widgetPreviewImageSizePx;

    public int labelHeight;

    // QSB
    public int statusBarHeightPx;
    public int navigationBarHeightPx;

    // Icon badges
    public BadgeRenderer mBadgeRenderer;

    public DeviceProfile(Context context, InvariantDeviceProfile inv,
                         Point minSize, Point maxSize,
                         int width, int height, boolean isLandscape) {

        mContext = context;
        this.inv = inv;
        this.isLandscape = isLandscape;

        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();

        // Constants from resources
        isTablet = DeviceInfoUtil.isTablet(context);
        isLargeTablet = DeviceInfoUtil.isLargeTablet(context);
        isPhone = !isTablet && !isLargeTablet;

        // Some more constants
        transposeLayoutWithOrientation = res.getBoolean(R.bool.hotseat_transpose_layout_with_orientation);
        statusBarHeightPx = res.getDimensionPixelSize(R.dimen.launcher_status_bar_height);
        navigationBarHeightPx = res.getDimensionPixelSize(R.dimen.launcher_navigation_bar_height);
        ComponentName cn = new ComponentName(context.getPackageName(),
                this.getClass().getName());
        defaultWidgetPadding = AppWidgetHostView.getDefaultPaddingForWidget(context, cn, null);
//        if (LauncherAppState.isAllAppsEnable()) {
//            edgeMarginPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin_allapps_enable);
//        } else {
//            edgeMarginPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin_normal);
//        }
        edgeMarginPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin_normal);
        edgeMarginTop = res.getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin_top);
        defaultHomeButtonHeightPx = res.getDimensionPixelSize(R.dimen.workspace_default_home_button_height);
        desiredWorkspaceLeftRightMarginPx = edgeMarginPx;
        pageIndicatorHeightPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_page_indicator_height);
        defaultPageSpacingPx =  res.getDimensionPixelSize(R.dimen.dynamic_grid_workspace_page_spacing);
        panelBotomMenuHeightPx = res.getDimensionPixelSize(R.dimen.overview_panel_bottom_menu_height);
        panelTopMenuHeightPx = res.getDimensionPixelSize(R.dimen.overview_panel_top_menu_height);
        iconDrawablePaddingOriginalPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_icon_drawable_padding);
        workspaceIconTextColor = Settings.getWorkspaceLabelColor(context);
        workspaceIconTextSizeScale = Settings.getWorkspaceTextSizeScale(context);
        workspaceIconSizeScale = Settings.getWorkspaceIconSizeScale(context);
        workspaceIconTextFont = Settings.getWorkspaceTextFont(context);

        // AllApps uses the original non-scaled icon text size
        allAppsIconTextSizePx = Utilities.pxFromDp(inv.iconTextSize, dm);

        // AllApps uses the original non-scaled icon size
        allAppsIconSizePx = Utilities.pxFromDp(inv.iconSize, dm) * 9 / 10;

        widgetPreviewImageSizePx = res.getDimensionPixelSize(R.dimen.widget_preview_img_size);

        // Determine sizes.
        widthPx = width;
        heightPx = height;
        if (isLandscape) {
            availableWidthPx = maxSize.x;
            availableHeightPx = minSize.y;
        } else {
            availableWidthPx = minSize.x;
            availableHeightPx = maxSize.y;
        }

        searchBarVisible = isSearchBarVisible(context);
        // Calculate the remaining vars
        updateAvailableDimensions(dm, res);

        // Save device profile params to settingprovider;
        // Icon style widget will use to layout;
        saveDeviceProfileToIOSSettings(context);

        // This is done last, after iconSizePx is calculated above.
        mBadgeRenderer = new BadgeRenderer(context, iconSizePx);
    }

    private void updateAvailableDimensions(DisplayMetrics dm, Resources res) {
        // Check to see if the icons fit in the new available height.  If not, then we need to
        // shrink the icon size.
        float scaleHorizontal = 1f;
        float vertical = 1f;
        int drawablePadding = iconDrawablePaddingOriginalPx;
        updateIconSize(1f, drawablePadding, res, dm);
        float usedHeight = (cellHeightPx * inv.numRows);
        float usedWidth = (cellWidthPx * inv.numHotseatIcons);

        // We only care about the top and bottom workspace padding, which is not affected by RTL.
        Rect workspacePadding = getWorkspacePadding(false /* isLayoutRtl */);
        int maxHeight = (availableHeightPx - workspacePadding.top - workspacePadding.bottom);
        if (usedHeight > maxHeight) {
            vertical = maxHeight / usedHeight;
            drawablePadding = 0;
        }

        int maxWidth = (availableWidthPx - workspacePadding.left - workspacePadding.right);
        if (usedWidth > maxWidth) {
            scaleHorizontal = maxWidth / usedWidth;
        }

        float scale = Math.min(vertical, scaleHorizontal);
        updateIconSize(scale, drawablePadding, res, dm);
    }

    private void updateIconSize(float scale, int drawablePadding, Resources res, DisplayMetrics dm) {
        /*
        * inv.iconSize,inv.iconTextSize：根据屏幕尺寸匹配得到，参考类InvariantDeviceProfile
        * */
        originalIconSizePx = (int) (Utilities.pxFromDp(inv.iconSize, dm) * scale);
        //workspaceIconSizeScale,workspaceIconTextSizeScale:读取SP文件获得，默认是１，不进行缩放
        iconSizePx = (int) (originalIconSizePx * workspaceIconSizeScale);
        originalIconTextSizePx = (int) (Utilities.pxFromDp(inv.iconTextSize, dm) * scale);
        iconTextSizePx = (int) (originalIconTextSizePx * workspaceIconTextSizeScale);
        iconDrawablePaddingPx = drawablePadding;
        hotseatIconSizePx = (int) (Utilities.pxFromDp(inv.hotseatIconSize, dm) * scale * workspaceIconSizeScale);

        // Search Bar
        searchBarSpaceWidthPx = Math.min(widthPx,
                res.getDimensionPixelSize(R.dimen.dynamic_grid_search_bar_max_width));

        defaultSearchBarHeightPx = res.getDimensionPixelSize(R.dimen.search_drop_target_bar_height);
        defaultDropTargetBarHeightPx = res.getDimensionPixelOffset(R.dimen.drop_target_bar_height);
        searchBarSpaceHeightPx = searchBarVisible || Partner.getBoolean(mContext, Partner.FEATURE_DISPLAY_CUTOUT_MODE) ?
                defaultSearchBarHeightPx : defaultDropTargetBarHeightPx;

        // Calculate the actual text height
        Paint textPaint = new Paint();
        textPaint.setTextSize(iconTextSizePx);
        FontMetrics fm = textPaint.getFontMetrics();
        labelHeight = (int) Math.ceil(fm.bottom - fm.top);
        cellWidthPx = iconSizePx + 2 * edgeMarginPx;
        cellHeightPx = iconSizePx + iconDrawablePaddingPx + labelHeight;
//        cellHeightPx = cellWidthPx;

        final float scaleDps = res.getDimensionPixelSize(R.dimen.dragViewScale);
        dragViewScale = (iconSizePx + scaleDps) / iconSizePx;

        // Hotseat
        if (Partner.getBoolean(mContext,Partner.DEF_HOTSEAT_LABEL_FORCED_SHOW)){
            hotseatBarHeightPx = cellHeightPx;
            hotseatCellWidthPx = cellWidthPx;
        }else {
            if (Partner.getBoolean(mContext, Partner.DEF_HOTSEAT_LABEL_SHOW_ENABLE)) {
                hotseatBarHeightPx = cellHeightPx + res.getDimensionPixelSize(R.dimen.dynamic_grid_hotseatbar_compensation_height);
                hotseatCellWidthPx = cellWidthPx;
            } else {
                hotseatBarHeightPx = iconSizePx + res.getDimensionPixelSize(R.dimen.dynamic_grid_hotseatbar_compensation_height);
                hotseatCellWidthPx = iconSizePx;
            }
        }
        //*/ios.xiaomin.liao,20180725,change hotseat height.
        hotseatBarHeightPx = hotseatBarHeightPx + res.getDimensionPixelSize(R.dimen.dynamic_grid_hotseatbar_add_height);
        //*/
//        hotseatCellHeightPx = hotseatBarHeightPx;
        hotseatCellHeightPx = iconSizePx;
        // Folder
        folderCellWidthPx = cellWidthPx + 3 * edgeMarginPx;
        folderCellHeightPx = cellHeightPx + 2 * edgeMarginPx;
        folderBackgroundOffset = 0;
        folderIconSizePx = iconSizePx;
    }

    /**
     * @param recyclerViewWidth the available width of the AllAppsRecyclerView
     */
    public void updateAppsViewNumCols(Resources res, int recyclerViewWidth) {
        int appsViewLeftMarginPx = res.getDimensionPixelSize(R.dimen.all_apps_grid_view_start_margin);
        int allAppsCellWidthGap = res.getDimensionPixelSize(R.dimen.all_apps_icon_width_gap);
        int availableAppsWidthPx = (recyclerViewWidth > 0) ? recyclerViewWidth : availableWidthPx;
        int numAppsCols = (availableAppsWidthPx - appsViewLeftMarginPx) /
                (allAppsIconSizePx + allAppsCellWidthGap);
        int numPredictiveAppCols = Math.max(inv.minAllAppsPredictionColumns, numAppsCols);
        allAppsNumCols = numAppsCols;
        allAppsNumPredictiveCols = numPredictiveAppCols;
    }

    /**
     * Returns the search bar top offset
     */
    private int getSearchBarTopOffset() {
        if (isTablet && !isVerticalBarLayout()) {
            return 4 * edgeMarginPx;
        } else {
            return 0;
        }
    }

    /**
     * Returns the search bar bounds in the current orientation
     */
    public Rect getSearchBarBounds(boolean isLayoutRtl) {
        Rect bounds = new Rect();
        if (isLandscape && transposeLayoutWithOrientation) {
            if (isLayoutRtl) {
                bounds.set(availableWidthPx - searchBarSpaceHeightPx, edgeMarginPx,
                        availableWidthPx, availableHeightPx - edgeMarginPx);
            } else {
                bounds.set(0, edgeMarginPx, searchBarSpaceHeightPx,
                        availableHeightPx - edgeMarginPx);
            }
        } else {
            if (isTablet) {
                // Pad the left and right of the workspace to ensure consistent spacing
                // between all icons
                int width = getCurrentWidth();
                // XXX: If the icon size changes across orientations, we will have to take
                //      that into account here too.
                int gap = (width - 2 * edgeMarginPx -
                        (inv.numColumns * cellWidthPx)) / (2 * (inv.numColumns + 1));
                bounds.set(edgeMarginPx + gap, getSearchBarTopOffset(),
                        availableWidthPx - (edgeMarginPx + gap),
                        searchBarSpaceHeightPx);
            } else {
                bounds.set(desiredWorkspaceLeftRightMarginPx - defaultWidgetPadding.left,
                        getSearchBarTopOffset(),
                        availableWidthPx - (desiredWorkspaceLeftRightMarginPx -
                                defaultWidgetPadding.right),
                        searchBarVisible ? searchBarSpaceHeightPx - statusBarHeightPx : 0);
            }
        }
        return bounds;
    }

    /**
     * Returns the workspace padding in the specified orientation
     */
    Rect getWorkspacePadding(boolean isLayoutRtl) {
        Rect searchBarBounds = getSearchBarBounds(isLayoutRtl);
        Rect padding = new Rect();
        if (isLandscape && transposeLayoutWithOrientation) {
            // Pad the left and right of the workspace with search/hotseat bar sizes
            if (isLayoutRtl) {
                padding.set(hotseatBarHeightPx, edgeMarginTop,
                        searchBarBounds.width(), edgeMarginTop);
            } else {
                padding.set(searchBarBounds.width(), edgeMarginTop,
                        hotseatBarHeightPx, edgeMarginTop);
            }
        } else {
            if (isTablet) {
                // Pad the left and right of the workspace to ensure consistent spacing
                // between all icons
                float gapScale = 1f + (dragViewScale - 1f) / 2f;
                int width = getCurrentWidth();
                int height = getCurrentHeight();
                int paddingTop = searchBarBounds.bottom;
                int paddingBottom = hotseatBarHeightPx + pageIndicatorHeightPx;
                int availableWidth = Math.max(0, width - (int) ((inv.numColumns * cellWidthPx) +
                        (inv.numColumns * gapScale * cellWidthPx)));
                int availableHeight = Math.max(0, height - paddingTop - paddingBottom
                        - 2 * inv.numRows * cellHeightPx);
                padding.set(availableWidth / 2, paddingTop + availableHeight / 2,
                        availableWidth / 2, paddingBottom + availableHeight / 2);
            } else {
                // Pad the top and bottom of the workspace with search/hotseat bar sizes

                padding.set(desiredWorkspaceLeftRightMarginPx
//                                - mContext.getResources().getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin_normal)
                                ,
                        searchBarBounds.bottom,
                        desiredWorkspaceLeftRightMarginPx
//                                - mContext.getResources().getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin_normal)
                        ,
                        hotseatBarHeightPx + pageIndicatorHeightPx + mContext.getResources().getDimensionPixelSize(R.dimen.dynamic_grid_hotseatbar_add_height));
            }
        }
        return padding;
    }

    private int getWorkspacePageSpacing(boolean isLayoutRtl) {
        if ((isLandscape && transposeLayoutWithOrientation) || isLargeTablet) {
            // In landscape mode the page spacing is set to the default.
            return defaultPageSpacingPx;
        } else {
            // In portrait, we want the pages spaced such that there is no
            // overhang of the previous / next page into the current page viewport.
            // We assume symmetrical padding in portrait mode.
            return Math.max(defaultPageSpacingPx, 2 * getWorkspacePadding(isLayoutRtl).left);
        }
    }

    // The rect returned will be extended to below the system ui that covers the workspace
    Rect getHotseatRect() {
        if (isVerticalBarLayout()) {
            return new Rect(availableWidthPx - hotseatBarHeightPx, 0,
                    Integer.MAX_VALUE, availableHeightPx);
        } else {
            return new Rect(0, availableHeightPx - hotseatBarHeightPx + DisplayUtil.getNavigationbarHeight(mContext),
                    availableWidthPx, Integer.MAX_VALUE);
        }
    }

    public static int calculateCellWidth(int width, int countX) {
        return width / countX;
    }

    public static int calculateCellHeight(int height, int countY) {
        return height / countY;
    }

    public int caculateFolderCellHeight() {
        return folderCellHeightPx;
    }

    public int caculateFolderMinCountY() {
        return availableHeightPx/folderCellHeightPx;
    }

    /**
     * When {@code true}, hotseat is on the bottom row when in landscape mode.
     * If {@code false}, hotseat is on the right column when in landscape mode.
     */
    public boolean isVerticalBarLayout() {
        return isLandscape && transposeLayoutWithOrientation;
    }

    boolean shouldFadeAdjacentWorkspaceScreens() {
        return isVerticalBarLayout() || isLargeTablet;
    }

    private int getVisibleChildCount(ViewGroup parent) {
        int visibleChildren = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i).getVisibility() != View.GONE) {
                visibleChildren++;
            }
        }
        return visibleChildren;
    }

    public void layout(Launcher launcher) {
        FrameLayout.LayoutParams lp;
        boolean hasVerticalBarLayout = isVerticalBarLayout();
        final boolean isLayoutRtl = Utilities.isRtl(launcher.getResources());

        // Layout the search bar space
        /*
        View searchBar = launcher.getSearchDropTargetBar();
        lp = (FrameLayout.LayoutParams) searchBar.getLayoutParams();
        FrameLayout targets = (FrameLayout) searchBar.findViewById(R.id.drag_target_bar);
        if (hasVerticalBarLayout) {
            // Vertical search bar space -- The search bar is fixed in the layout to be on the left
            //                              of the screen regardless of RTL
            lp.gravity = Gravity.LEFT;
            lp.width = searchBarSpaceHeightPx;

            FrameLayout.LayoutParams targetsLp = (FrameLayout.LayoutParams) targets.getLayoutParams();
            targetsLp.gravity = Gravity.TOP;
            targetsLp.height = LayoutParams.WRAP_CONTENT;
        } else {
            // Horizontal search bar space
            lp.gravity = Gravity.TOP;
            lp.height = searchBarSpaceHeightPx;
            FrameLayout.LayoutParams targetsLp = (FrameLayout.LayoutParams) targets.getLayoutParams();
            targets.getLayoutParams().width = searchBarSpaceWidthPx;
            if (Partner.getBoolean(mContext,Partner.FEATURE_DISPLAY_CUTOUT_MODE)){
                targetsLp.topMargin = DisplayUtil.getStatusHeight(mContext);
            }
            targets.setLayoutParams(targetsLp);
        }
        searchBar.setLayoutParams(lp);
         */

        // Layout the workspace
        PagedView workspace = (PagedView) launcher.findViewById(R.id.workspace);

        lp = (FrameLayout.LayoutParams) workspace.getLayoutParams();
        lp.gravity = Gravity.CENTER;
        Rect padding = getWorkspacePadding(isLayoutRtl);
        workspace.setLayoutParams(lp);

        int extraPadding = (int) (availableHeightPx - hotSeatBarMarginHeight - hotseatBarHeightPx - pageIndicatorHeightPx - cellWidthPx * inv.numRows) / 2;

        workspace.setPadding(
                padding.left,
                padding.top,
                padding.right,
                padding.bottom + extraPadding);

        workspace.setPageSpacing(getWorkspacePageSpacing(isLayoutRtl));

        // Layout the hotseat
        Hotseat hotseat = launcher.findViewById(R.id.hotseat);
        lp = (FrameLayout.LayoutParams) hotseat.getLayoutParams();
        if (hasVerticalBarLayout) {
            // Vertical hotseat -- The hotseat is fixed in the layout to be on the right of the
            //                     screen regardless of RTL
            lp.gravity = Gravity.RIGHT;
            lp.width = hotseatBarHeightPx;
            lp.height = LayoutParams.MATCH_PARENT + 40;
            hotseat.findViewById(R.id.layout).setPadding(0, 2 * edgeMarginPx, 0, 2 * edgeMarginPx);
        } else if (isTablet) {
            // Pad the hotseat with the workspace padding calculated above
            lp.gravity = Gravity.BOTTOM;
            lp.width = LayoutParams.MATCH_PARENT;
            lp.height = hotseatBarHeightPx + 40;
            hotseat.setPadding(edgeMarginPx + padding.left, 0,
                    edgeMarginPx + padding.right,
                    2 * edgeMarginPx);
        } else {
            // For phones, layout the hotseat without any bottom margin
            // to ensure that we have space for the folders
            lp.gravity = Gravity.BOTTOM;
            lp.width = LayoutParams.MATCH_PARENT;
            lp.height = hotseatBarHeightPx;
//            hotseat.findViewById(R.id.layout).setPadding(padding.left, 0,
//                    padding.right, 0)
            hotseat.setPadding(
                    edgeMarginPx,
                    0,
                    edgeMarginPx,
                    mContext.getResources().getDimensionPixelSize(R.dimen.dynamic_grid_hotseatbar_add_height)
            );
        }
        hotseat.setLayoutParams(lp);

        // Layout the page indicators
        View pageIndicator = launcher.findViewById(R.id.page_indicator);
        if (pageIndicator != null) {
            if (hasVerticalBarLayout) {
                // Hide the page indicators when we have vertical search/hotseat
                pageIndicator.setVisibility(View.GONE);
            } else {
                // Put the page indicators above the hotseat
                lp = (FrameLayout.LayoutParams) pageIndicator.getLayoutParams();
                lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
                lp.width = LayoutParams.WRAP_CONTENT;
                lp.height = pageIndicatorHeightPx;
                //*/ios.xiaomin.liao,20180725,change pageIndicator height.
                lp.bottomMargin = hotseatBarHeightPx + mContext.getResources().getDimensionPixelSize(R.dimen.dynamic_grid_page_indicator_add_height);
                lp.leftMargin = 50;
                lp.rightMargin = 50;
                /*/
                lp.bottomMargin = hotseatBarHeightPx;
                //*/
                pageIndicator.setLayoutParams(lp);
            }
        }
    }

    public int getCurrentWidth() {
        return isLandscape
                ? Math.max(widthPx, heightPx)
                : Math.min(widthPx, heightPx);
    }

    public int getCurrentHeight() {
        return isLandscape
                ? Math.min(widthPx, heightPx)
                : Math.max(widthPx, heightPx);
    }

    private boolean isSearchBarVisible(Context context) {
        return SearchWidgetUtil.shouldShow(context);
    }


    private void saveDeviceProfileToIOSSettings(Context context) {
        ContentResolver resolver = context.getContentResolver();
        IOSSettings.putInt(resolver, IOSSettings.Launcher.LAUNCHER_APP_ICON_SIZE_PX, iconSizePx);
        IOSSettings.putInt(resolver, IOSSettings.Launcher.LAUNCHER_APP_ICON_LABEL_PADDING_PX, iconDrawablePaddingPx);
        IOSSettings.putInt(resolver, IOSSettings.Launcher.LAUNCHER_APP_LABEL_SIZE_PX, iconTextSizePx);
    }

    public void scaleIconTextSize(Context context, float scale) {
        workspaceIconTextSizeScale = scale;
        iconTextSizePx = (int) (originalIconTextSizePx * scale);
        ContentResolver resolver = context.getContentResolver();
        IOSSettings.putInt(resolver, IOSSettings.Launcher.LAUNCHER_APP_LABEL_SIZE_PX, iconTextSizePx);
    }

    /**
     * Callback when a component changes the DeviceProfile associated with it, as a result of
     * configuration change
     */
    public interface OnDeviceProfileChangeListener {

        /**
         * Called when the device profile is reassigned. Note that for layout and measurements, it
         * is sufficient to listen for inset changes. Use this callback when you need to perform
         * a one time operation.
         */
        void onDeviceProfileChanged(DeviceProfile dp);
    }

    public final int b() {
        return this.isLandscape ? Math.max(this.widthPx, this.heightPx) : Math.min(this.widthPx, this.heightPx);
    }
}

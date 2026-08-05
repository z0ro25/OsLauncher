/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.ioslite.common.util.DeviceInfoUtil;
import com.amz.ios.launcher.config.GridConfig;
import com.amz.ios.launcher.config.Settings;
import com.amz.ios.launcher.util.Thunk;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.ui.AppUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class InvariantDeviceProfile {

    private Context mContext;

    private static final String TAG = "InvariantDeviceProfile";
    // This is a static that we use for the default icon size on a 4/5-inch phone
    private static float DEFAULT_ICON_SIZE_DP = 48;

    private static final float ICON_SIZE_DEFINED_IN_APP_DP = 48;

    // Constants that affects the interpolation curve between statically defined device profile
    // buckets.
    private static float KNEARESTNEIGHBOR = 3;
    private static float WEIGHT_POWER = 5;

    // used to offset float not being able to express extremely small weights in extreme cases.
    private static float WEIGHT_EFFICIENT = 100000f;


    private boolean mIsTablet;
    private boolean mIsPhone;

    // Profile-defining invariant properties
    String name;
    float minWidthDps;
    float minHeightDps;
    public int screenWidthPx;
    public int screenHeightPx;
    public int textHeight;

    /**
     * Number of icons per row and column in the workspace.
     */
    public int numRows;
    public int numColumns;

    /**
     * The minimum number of predicted apps in all apps.
     */
    int minAllAppsPredictionColumns;

    /**
     * Number of icons per row and column in the folder.
     */
    public int numFolderRows;
    public int numFolderColumns;
    float iconSize;
    public int iconBitmapSize;
    public int fillResIconDpi;
    float iconTextSize;

    /**
     * Number of icons inside the hotseat area.
     */
    float numHotseatIcons;
    float hotseatIconSize;
    int defaultLayoutId;

    // Derived invariant properties
    public DeviceProfile landscapeProfile;
    public DeviceProfile portraitProfile;


    InvariantDeviceProfile() {
    }

    public InvariantDeviceProfile(InvariantDeviceProfile p) {
        this(p.name, p.minWidthDps, p.minHeightDps, p.numRows, p.numColumns,
                p.numFolderRows, p.numFolderColumns, p.minAllAppsPredictionColumns,
                p.iconSize, p.iconTextSize, p.numHotseatIcons, p.hotseatIconSize,
                p.defaultLayoutId);
    }

    InvariantDeviceProfile(String n, float w, float h, int r, int c, int fr, int fc, int maapc,
                           float is, float its, float hs, float his, int dlId) {
        name = n;
        minWidthDps = w;
        minHeightDps = h;
        numRows = r;
        numColumns = c;
        minAllAppsPredictionColumns = maapc;
        iconSize = is;
        iconTextSize = its;
        numHotseatIcons = hs;
        hotseatIconSize = his;
        defaultLayoutId = dlId;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    InvariantDeviceProfile(Context context) {

        mContext = context;

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);

        Point smallestSize = new Point();
        Point largestSize = new Point();
        display.getCurrentSizeRange(smallestSize, largestSize);

        // This guarantees that width < height
        minWidthDps = Utilities.dpiFromPx(Math.min(smallestSize.x, smallestSize.y), dm);
        minHeightDps = Utilities.dpiFromPx(Math.min(largestSize.x, largestSize.y), dm);

        mIsTablet = DeviceInfoUtil.isTablet(context);
        mIsPhone = !mIsTablet;

        ArrayList<InvariantDeviceProfile> closestProfiles =
                findClosestDeviceProfiles(minWidthDps, minHeightDps, getPredefinedDeviceProfiles());
        DebugLog.w(TAG,"=========================closestProfiles:"+closestProfiles.get(0).toString());
        InvariantDeviceProfile interpolatedDeviceProfileOut =
                invDistWeightedInterpolate(minWidthDps, minHeightDps, closestProfiles);

        InvariantDeviceProfile closestProfile = closestProfiles.get(0);

        Point realSize = new Point();
        display.getRealSize(realSize);

        int smallSide = Math.min(realSize.x, realSize.y);
        int largeSide = Math.max(realSize.x, realSize.y);

        /*
        if (BuildUtil.isCustomerBuild() && Partner.getBoolean(context, Partner.DEF_GRID_ICON_CUSTOM_ENABLE)) {
            applyCustomGridIconSize(context);
        }*/

        screenHeightPx = largeSide;
        screenWidthPx = smallSide;

        int[] gridInfo = new int[3];
        getDeviceGrid(screenWidthPx, screenHeightPx,gridInfo);
        /*
        numRows = closestProfile.numRows;
        numColumns = closestProfile.numColumns;
        defaultLayoutId = closestProfile.defaultLayoutId;


         */
        numRows = gridInfo[0];
        numColumns = gridInfo[1];
        defaultLayoutId = gridInfo[2];
        numFolderRows = numRows;
        numFolderColumns = numColumns;

//        minAllAppsPredictionColumns = closestProfile.minAllAppsPredictionColumns;

//        iconTextSize = interpolatedDeviceProfileOut.iconTextSize;
        /*
        iconSize = interpolatedDeviceProfileOut.iconSize;
        hotseatIconSize = interpolatedDeviceProfileOut.hotseatIconSize;
         */

        // The real size never changes. smallSide and largeSide will remain the
        // same in any orientation.

        int size = (int) ((Math.min(smallSide, largeSide) / numColumns) * 0.6f);
        iconBitmapSize = size;
        hotseatIconSize = iconSize = Utilities.dpiFromPx(size,dm);
        iconTextSize = iconSize / 4.5f;
        Rect rect = new Rect();

        fillResIconDpi = dm.densityDpi;

        /*
        int customGrid = Settings.getDesktopGrid(context);
        if (customGrid > -1) {
            GridConfig.GridInfo info = GridConfig.PRE_DESKTOP_GRID.get(customGrid);
            applyCustomGridConfig(info);
        } else {
            saveCurrentGridConfig(context);
        }*/

        // Dock lấp đầy chiều ngang màn thay vì cố định 4 cell (màn dài trước đây
        // dock ngắn nhìn xấu). Ước lượng số cell vừa bề rộng: bề rộng / bề ngang 1
        // cell (icon + 2 lề). Sàn = numColumns/hồ sơ gần nhất để không nhỏ hơn cũ.
        int hotseatEdgeMarginPx =
                context.getResources().getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin_normal);
        int hotseatCellWidthPx = size + 2 * hotseatEdgeMarginPx;
        int fitHotseat = hotseatCellWidthPx > 0 ? (smallSide / hotseatCellWidthPx) : numColumns;
        numHotseatIcons = Math.max(
                Math.max(numColumns, closestProfile.numHotseatIcons), fitHotseat);
        landscapeProfile = new DeviceProfile(context, this, smallestSize, largestSize,
                largeSide, smallSide, true /* isLandscape */);
        portraitProfile = new DeviceProfile(context, this, smallestSize, largestSize,
                smallSide, largeSide, false /* isLandscape */);
    }

    ArrayList<InvariantDeviceProfile> getPredefinedDeviceProfiles() {
        ArrayList<InvariantDeviceProfile> predefinedDeviceProfiles = new ArrayList<>();

        int smallerGridSize = AppUtils.getInstance(mContext).getGridSize();
        if (smallerGridSize == 0) smallerGridSize = 5;
        int biggerGridSize = AppUtils.getInstance(mContext).getGridSize();
        if (biggerGridSize == 0) biggerGridSize = 6;

        boolean isT = b(minWidthDps,minHeightDps);
        boolean isSamSung = isSamSungPhone();

        int biggerXml = isSamSung ? R.xml.default_workspace_6x4_samsung : R.xml.default_workspace_6x4;
        int smallerXml = isSamSung ? R.xml.default_workspace_5x4_samsung : R.xml.default_workspace_5x4;
        int defineXml = isSamSung ? isT ? R.xml.default_workspace_6x4_samsung : R.xml.default_workspace_5x4_samsung : isT ? R.xml.default_workspace_6x4 : R.xml.default_workspace_5x4;

        int defineGridSize = smallerGridSize == 0 ? isT ? 6 : 5 : smallerGridSize;
        // width, height, #rows, #columns, #folder rows, #folder columns,
        // iconSize, iconTextSize, #hotseat, #hotseatIconSize, defaultLayoutId.
        if (mIsPhone) {

            predefinedDeviceProfiles.add(new InvariantDeviceProfile("Nexus S",
                    296, 491.33f, /*smallerGridSize*/ 6, 4, 3, 3, 4, 58, 11, 4, 58, biggerXml));

            predefinedDeviceProfiles.add(new InvariantDeviceProfile(
                    "Nexus 4", 359.0f, 567.0f, /*smallerGridSize*/ 6, 4, 3, 3, 4, 58.0f, 11.0f, 4, 58.0f, /*smallerXml*/biggerXml
            ));
            predefinedDeviceProfiles.add(
                    new InvariantDeviceProfile(
                            "Nexus 5", 335, 567, /*defineGridSize*/6, 4, 3, 3, 4, 58.0f, 11.0f, 4, 58.0f, /*defineXml*/biggerXml
                    )
            );

            predefinedDeviceProfiles.add(
                    new InvariantDeviceProfile(
                            "Nexus 5", 335, 567, /*defineGridSize*/6, 4, 3, 3, 4, 58.0f, 11.0f, 4, 58.0f, defineXml
                    )
            );

            predefinedDeviceProfiles.add(
                    new InvariantDeviceProfile(
                        "Large Phone", 406.0f, 694.0f, /*biggerGridSize*/6, 4, 3, 3, 4, 60.0f, 10.0f, 4, 60.0f, isSamSung ? R.xml.default_workspace_6x4_samsung : R.xml.default_workspace_6x4
                    )
            );

            predefinedDeviceProfiles.add(
                    new InvariantDeviceProfile(
                            "Tablet", 575.0f, 904.0f, /*smallerGridSize*/6, 4, 3, 3, 4, 60.0f, 10.0f, 4, 60.0f, isSamSung ? R.xml.default_workspace_5x4_samsung : R.xml.default_workspace_5x4
                    )
            );

            /*
            predefinedDeviceProfiles.add(new InvariantDeviceProfile("Huawei Mate8",
                    336, 580, defineGridSize, 4, 4, 4, 4, 50, 12, hasAllApps ? 5 : 4, 50, defineXml));
            predefinedDeviceProfiles.add(new InvariantDeviceProfile("Samsung Note3",
                    336, 615, biggerGridSize, 4, 4, 4, 4, 60, 12, hasAllApps ? 5 : 4, 60, biggerXml));
            predefinedDeviceProfiles.add(new InvariantDeviceProfile("HongMi Note",
                    372, 678, smallerGridSize, 4, 4, 4, 4, 56, 10, hasAllApps ? 5 : 4, 56, smallerXml));
            predefinedDeviceProfiles.add(new InvariantDeviceProfile("IOSOs 8.1",
                    336, 648, smallerGridSize, 4, 4, 4, 4, 60, 12, hasAllApps ? 5 : 4, 60, smallerGridSize));
            predefinedDeviceProfiles.add(new InvariantDeviceProfile("Large Phone",
                    406, 694, defineGridSize, 4, 4, 4, 4, 56, 13f, hasAllApps ? 5 : 4, 56, defineGridSize));


             */
        }

        if (mIsTablet) {
            // The tablet profile is odd in that the landscape orientation
            // also includes the nav bar on the side
            predefinedDeviceProfiles.add(new InvariantDeviceProfile("Nexus 7",
                    575, 904, 5, 6, 4, 5, 4, 72, 14f, 6, 60, R.xml.default_workspace_5x6));
            // Larger tablet profiles always have system bars on the top & bottom
            predefinedDeviceProfiles.add(new InvariantDeviceProfile("Nexus 10",
                    727, 1207, 5, 6, 4, 5, 4, 76, 14f, 6, 64, R.xml.default_workspace_5x6));
            predefinedDeviceProfiles.add(new InvariantDeviceProfile("20-inch Tablet",
                    1527, 2527, 7, 7, 6, 6, 4, 100, 18, 6, 72, R.xml.default_workspace_4x4));
        }

        return predefinedDeviceProfiles;
    }

    private int getLauncherIconDensity(int requiredSize) {
        // Densities typically defined by an app.
        int[] densityBuckets = new int[]{
                DisplayMetrics.DENSITY_LOW,
                DisplayMetrics.DENSITY_MEDIUM,
                DisplayMetrics.DENSITY_TV,
                DisplayMetrics.DENSITY_HIGH,
                DisplayMetrics.DENSITY_XHIGH,
                DisplayMetrics.DENSITY_XXHIGH,
                DisplayMetrics.DENSITY_XXXHIGH
        };

        int density = DisplayMetrics.DENSITY_XXXHIGH;
        for (int i = densityBuckets.length - 1; i >= 0; i--) {
            float expectedSize = ICON_SIZE_DEFINED_IN_APP_DP * densityBuckets[i]
                    / DisplayMetrics.DENSITY_DEFAULT;
            if (expectedSize >= requiredSize) {
                density = densityBuckets[i];
            }
        }

        return density;
    }


    private void applyCustomGridConfig(GridConfig.GridInfo info) {
        if (info.rows > 0 && info.columns > 0) {
            this.numRows = info.rows;
            this.numColumns = info.columns;
            this.defaultLayoutId = info.layoutId;
        }
    }

    private void applyCustomGridIconSize(Context context) {
        int cusIconSizeDp = Partner.getDimensionDpSize(context, Partner.DEF_GRID_ICON_SIZE);
        int cusLabelSizeDp = Partner.getDimensionDpSize(context, Partner.DEF_GRID_LABEL_SIZE);

        if (cusIconSizeDp > 0) {
            hotseatIconSize = iconSize = cusIconSizeDp;
        }

        if (cusLabelSizeDp > 0) {
            iconTextSize = cusLabelSizeDp;
        }
    }

    private void saveCurrentGridConfig(Context context) {
        GridConfig.GridInfo info = new GridConfig.GridInfo(numRows, numColumns);
        for (Map.Entry<Integer, GridConfig.GridInfo> entry : GridConfig.PRE_DESKTOP_GRID.entrySet()) {
            if (entry.getValue().equals(info)) {
                Settings.setDesktopGrid(context, entry.getKey());
            }
        }
    }

    @Thunk
    float dist(float x0, float y0, float x1, float y1) {
        return (float) Math.hypot(x1 - x0, y1 - y0);
    }

    /**
     * Returns the closest device profiles ordered by closeness to the specified width and height
     */
    // Package private visibility for testing.
    ArrayList<InvariantDeviceProfile> findClosestDeviceProfiles(
            final float width, final float height, ArrayList<InvariantDeviceProfile> points) {

        // Sort the profiles by their closeness to the dimensions
        ArrayList<InvariantDeviceProfile> pointsByNearness = points;
        Collections.sort(pointsByNearness, new Comparator<InvariantDeviceProfile>() {
            public int compare(InvariantDeviceProfile a, InvariantDeviceProfile b) {
                return (int) (dist(width, height, a.minWidthDps, a.minHeightDps)
                        - dist(width, height, b.minWidthDps, b.minHeightDps));
            }
        });

        return pointsByNearness;
    }

    // Package private visibility for testing.
    InvariantDeviceProfile invDistWeightedInterpolate(float width, float height,
                                                      ArrayList<InvariantDeviceProfile> points) {
        float weights = 0;

        InvariantDeviceProfile p = points.get(0);
        if (dist(width, height, p.minWidthDps, p.minHeightDps) == 0) {
            return p;
        }

        InvariantDeviceProfile out = new InvariantDeviceProfile();
        for (int i = 0; i < points.size() && i < KNEARESTNEIGHBOR; ++i) {
            p = new InvariantDeviceProfile(points.get(i));
            float w = weight(width, height, p.minWidthDps, p.minHeightDps, WEIGHT_POWER);
            weights += w;
            out.add(p.multiply(w));
        }
        return out.multiply(1.0f / weights);
    }

    private void add(InvariantDeviceProfile p) {
        iconSize += p.iconSize;
        iconTextSize += p.iconTextSize;
        hotseatIconSize += p.hotseatIconSize;
    }

    private InvariantDeviceProfile multiply(float w) {
        iconSize *= w;
        iconTextSize *= w;
        hotseatIconSize *= w;
        return this;
    }

    private float weight(float x0, float y0, float x1, float y1, float pow) {
        float d = dist(x0, y0, x1, y1);
        if (Float.compare(d, 0f) == 0) {
            return Float.POSITIVE_INFINITY;
        }
        return (float) (WEIGHT_EFFICIENT / Math.pow(d, pow));
    }

    @Override
    public String toString() {
        return "InvariantDeviceProfile{" +
                "mIsTablet=" + mIsTablet +
                ", mIsPhone=" + mIsPhone +
                ", name='" + name + '\'' +
                ", minWidthDps=" + minWidthDps +
                ", minHeightDps=" + minHeightDps +
                ", screenWidthPx=" + screenWidthPx +
                ", screenHeightPx=" + screenHeightPx +
                ", numRows=" + numRows +
                ", numColumns=" + numColumns +
                ", minAllAppsPredictionColumns=" + minAllAppsPredictionColumns +
                ", numFolderRows=" + numFolderRows +
                ", numFolderColumns=" + numFolderColumns +
                ", iconSize=" + iconSize +
                ", iconBitmapSize=" + iconBitmapSize +
                ", fillResIconDpi=" + fillResIconDpi +
                ", iconTextSize=" + iconTextSize +
                ", numHotseatIcons=" + numHotseatIcons +
                ", hotseatIconSize=" + hotseatIconSize +
                ", defaultLayoutId=" + defaultLayoutId +
                ", landscapeProfile=" + landscapeProfile +
                ", portraitProfile=" + portraitProfile +
                '}';
    }

    public boolean isSamSungPhone(){
        try {
            return Build.MANUFACTURER.toLowerCase().contains("samsung");
        } catch (Throwable unused) {
            return false;
        }
    }

    public void getDeviceGrid(int width, int height, int[] gridInfo){
        if (gridInfo == null) return;
        Resources resources = mContext.getResources();
        int workSpaceHeight = height -
                resources.getDimensionPixelSize(R.dimen.dynamic_grid_hotseatbar_add_height)
                - resources.getDimensionPixelSize(R.dimen.dynamic_grid_hotseatbar_compensation_height)
                - resources.getDimensionPixelSize(R.dimen.dynamic_grid_page_indicator_height)
                - resources.getDimensionPixelSize(R.dimen.dynamic_grid_page_indicator_add_height)
                - resources.getDimensionPixelSize(R.dimen.dynamic_grid_page_indicator_margin);
        float ratio = workSpaceHeight * 1.0f / width;

        int row = 5;
        int col = 5;
        int gridXml = R.xml.default_workspace_5x5;

        if (ratio > 1.78f){
            row = 6;
            col = 4;
            gridXml = R.xml.default_workspace_6x4;
        }
        else if (ratio > 1.57f){
            col = 4;
            gridXml = R.xml.default_workspace_5x4;
        }
        else if (ratio > 1.40f){
            row = 4;
            col = 4;
            gridXml = R.xml.default_workspace_4x4;
        }

        gridInfo[0] = row;
        gridInfo[1] = col;
        gridInfo[2] = gridXml;

        /**
         *  1.78f 4,7 (4x6)
         *  1.57f 4,6 (4x5)
         *  1.44f 5,7 (5x6)
         *  1.28f 4,5 (4x4)
         *  1.23f 5,6 (5*5)
         */
    }

    public final boolean b(float f, float f2) {
        return f2 / f > 1.7777778f || f / f2 > 1.7777778f;
    }

}
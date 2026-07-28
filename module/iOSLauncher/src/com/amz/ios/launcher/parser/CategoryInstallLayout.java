package com.amz.ios.launcher.parser;

import android.appwidget.AppWidgetHost;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.amz.ios.ioslite.common.CommonSdk;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.util.CommonUtilities;
import com.amz.ios.launcher.InvariantDeviceProfile;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherSettings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class CategoryInstallLayout extends AutoInstallsLayout {
    private static final String TAG = "CategoryInstallLayout";
    private long mScreenId;
    private HashMap<Integer, CategoryFolder> mCachedFolders = new HashMap<>();
    private int mFolderCount;

    private int numRows;
    private int numColumns;

    public CategoryInstallLayout(Context context, AppWidgetHost appWidgetHost, LayoutParserCallback callback, Resources res, int layoutId) {
        super(context, appWidgetHost, callback, res, 0);

        InvariantDeviceProfile inv = LauncherAppState.getInstance().getInvariantDeviceProfile();
        numRows = inv.numRows;
        numColumns = inv.numColumns;
    }

    public int loadLayout(SQLiteDatabase db, long screenId, List<ComponentName> componentNames) {
        mAllLauncherComps = componentNames;
        mScreenId = screenId;
        mDb = db;

        AppCategoryProvider categoryProvider = AppCategoryProvider.getInstance();
        TreeMap<CategoryFolder, ArrayList<ComponentName>> folderMap = new TreeMap<>(getCatFolderComparator());

        int type;
        CategoryFolder folder;
        for (ComponentName cn : mAllLauncherComps) {
            type = categoryProvider.getCatTypeForComp(cn);
            folder = getAndUpdateCachedFolder(type);

            ArrayList<ComponentName> sectionApps = folderMap.get(folder);
            if (sectionApps == null) {
                sectionApps = new ArrayList<>();
                folderMap.put(folder, sectionApps);
            }
            sectionApps.add(cn);
        }

        try {
            return parseLayout(folderMap);
        } catch (Exception e) {
            Log.w(TAG, "Got exception parsing layout.", e);
            return -1;
        }
    }

    private int parseLayout(Map<CategoryFolder, ArrayList<ComponentName>> map) {
        mFolderCount = 0;
        for (Map.Entry<CategoryFolder, ArrayList<ComponentName>> entry : map.entrySet()) {
            mFolderCount += parseAndAddEntry(entry.getKey(), entry.getValue());
        }

        return mFolderCount;
    }

    private int parseAndAddEntry(CategoryFolder folderInfo, ArrayList<ComponentName> componentNames) {
        DebugLog.w(TAG, folderInfo.toString());
        long folderId = addFolderForType(folderInfo);
        if (folderId == -1) {
            Log.e(TAG, "parseAndAddNode fail , can not get folder id");
            return 0;
        }

        ActivityInfo info;
        long id;
        int contentCount = 0;
        for (ComponentName cn : componentNames) {
            mValues.clear();
            mValues.put(LauncherSettings.Favorites.CONTAINER, folderId);
            mValues.put(LauncherSettings.Favorites.SCREEN, mScreenId);
            mValues.put(LauncherSettings.Favorites.RANK, contentCount);

            try {

                try {
                    info = mPackageManager.getActivityInfo(cn, 0);
                } catch (PackageManager.NameNotFoundException nnfe) {
                    String[] packages = mPackageManager.currentToCanonicalPackageNames(
                            new String[]{cn.getPackageName()});
                    cn = new ComponentName(packages[0], cn.getClassName());
                    info = mPackageManager.getActivityInfo(cn, 0);
                }
                final Intent intent = new Intent(Intent.ACTION_MAIN, null)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(cn)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                id = addShortcut(info.loadLabel(mPackageManager).toString(),
                        intent, LauncherSettings.Favorites.ITEM_TYPE_APPLICATION);
                DebugLog.w(TAG, info.loadLabel(mPackageManager).toString() + " : " + cn);
                if (id > 0) {
                    contentCount += 1;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Unable to add favorite: " + cn, e);
            }
        }

        return 1;
    }

    private long addFolderForType(CategoryFolder folder) {
        long[] out = new long[2];
        parseFolderCellXY(out, mFolderCount);

        mValues.clear();
        mValues.put(LauncherSettings.Favorites.CONTAINER, LauncherSettings.Favorites.CONTAINER_DESKTOP);
        mValues.put(LauncherSettings.Favorites.SCREEN, mScreenId);
        mValues.put(LauncherSettings.Favorites.CELLX, out[0]);
        mValues.put(LauncherSettings.Favorites.CELLY, out[1]);

        mValues.put(LauncherSettings.Favorites.TITLE, folder.getFolderName());
        mValues.put(LauncherSettings.Favorites.FOLDER_CATEGORY_TYPE, folder.getFolderType());
        mValues.put(LauncherSettings.Favorites.ITEM_TYPE, LauncherSettings.Favorites.ITEM_TYPE_FOLDER);
        mValues.put(LauncherSettings.Favorites.SPANX, 1);
        mValues.put(LauncherSettings.Favorites.SPANY, 1);
        mValues.put(LauncherSettings.Favorites._ID, mCallback.generateNewItemId());
        long folderId = mCallback.insertAndCheck(mDb, mValues);

        if (folderId < 0) {
            Log.e(TAG, "Unable to add folder");
            return -1;
        }
        return folderId;
    }


    private void parseFolderCellXY(long[] out, int count) {
        out[0] = count % numColumns;
        out[1] = count / numColumns;
    }

    private Comparator<CategoryFolder> getCatFolderComparator() {
        return new Comparator<CategoryFolder>() {
            @Override
            public int compare(CategoryFolder o1, CategoryFolder o2) {
                return CommonUtilities.compare(o1.getFolderType(), o2.getFolderType());
            }
        };
    }

    private CategoryFolder getAndUpdateCachedFolder(int type) {
        CategoryFolder folder = mCachedFolders.get(type);
        if (folder == null) {
            folder = new CategoryFolder(mContext, type);
            mCachedFolders.put(type, folder);
        }
        return folder;
    }


}

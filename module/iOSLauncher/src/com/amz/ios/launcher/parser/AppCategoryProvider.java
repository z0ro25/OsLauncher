package com.amz.ios.launcher.parser;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.amz.ios.ioslite.common.CommonSdk;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.util.PackageUtil;
import com.amz.ios.launcher.parser.AppCategoryModel.Category;


public class AppCategoryProvider implements AppCategoryModel.Callback {
    private static final String TAG = "CategoryAppProvider";
    private static AppCategoryProvider sCatFolderProvider;
    private AppCategoryModel mCategoryModel;
    private Context mContext;
    private PackageManager mPackageManager;
    private SQLiteDatabase mDatabase;

    private static final String IOS_OS_OTA_PACKAGE = "com.zhuoyi.security.service";

    public static AppCategoryProvider getInstance() {
        if (sCatFolderProvider == null) {
            synchronized (AppCategoryProvider.class) {
                Context context = CommonSdk.getApplicationContext();
                final AppCategoryModel model = AppCategoryModel.getInstance();
                sCatFolderProvider = new AppCategoryProvider(context, model);
            }
        }
        return sCatFolderProvider;
    }


    private AppCategoryProvider(Context context, AppCategoryModel model) {
        mContext = context;
        mPackageManager = context.getPackageManager();
        mCategoryModel = model;
        mCategoryModel.setCallback(this);
        init(context);
    }

    private void init(Context context) {
        if (mCategoryModel.hasCompleteInitalize()) {
            openDatabase(mContext);
        } else {
            mCategoryModel.initalize(context);
        }
    }


    @Override
    public void onDatabaseUpdate() {
        openDatabase(mContext);
    }

    @Override
    public void startUpdateDatabase() {
        closeDatabase();
    }

    /**
     * Parse  folder type from a app componentnew
     */
    public int getCatTypeForComp(ComponentName cn) {
        int type = -1;

        if (cn == null) {
            return CategoryFolder.CATEGORY_FOLDER_DEFAULT;
        }

        if (cn.getPackageName().equals(IOS_OS_OTA_PACKAGE)) {
            type = getTypeForOtaComponent(cn);
        } else {
            if (mDatabase != null) {
                Cursor cursor = null;
                try {
                    cursor = mDatabase.query(Category.TABLE_NAME,
                            new String[]{Category.CATEGORY_VALUE},
                            Category.PACKAGE_NAME + "=?",
                            new String[]{cn.getPackageName()},
                            null, null, null);
                    if (cursor != null && cursor.moveToNext()) {
                        type = cursor.getInt(0);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "getCatTypeForComp fail for cn : " + cn, e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }

        if (type == -1) {
            if (PackageUtil.isSystemApp(mPackageManager, cn.getPackageName())) {
                type = CategoryFolder.CATEGORY_FOLDER_SYSTEM;
                DebugLog.d(TAG, "system: " + cn);
            } else {
                type = CategoryFolder.CATEGORY_FOLDER_OTHER;
                DebugLog.d(TAG, "other: " + cn);
            }
        }
        return type;
    }


    private int getTypeForOtaComponent(ComponentName cn) {
        String clsName = cn.getClassName();
        if (clsName.equals("com.zhuoyi.security.service.newsarticle")) {
            return CategoryFolder.CATEGORY_FOLDER_NEWS_BOOKS;
        } else if (clsName.equals("com.zhuoyi.security.service.meituan")) {
            return CategoryFolder.CATEGORY_FOLDER_LIFESTYPE;
        } else if (clsName.equals("com.zhuoyi.security.service.ttvideo")) {
            return CategoryFolder.CATEGORY_FOLDER_MEDIA;
        }
        return -1;
    }


    private SQLiteDatabase openDatabase(Context context) {
        if (mDatabase == null) {
            try {
                String databasePath = context.getDatabasePath(Category.DATABASE_NAME).getPath();
                mDatabase = SQLiteDatabase.openDatabase(databasePath,
                        null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            } catch (Exception e) {
                Log.e(TAG, "open database fail", e);
                mDatabase = null;
            }
        }
        return mDatabase;
    }

    private void closeDatabase() {
        if (mDatabase != null) {
            mDatabase.close();
        }
    }

}

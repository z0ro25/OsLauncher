package com.amz.ios.themeclub.dao;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.config.ThemeConfig;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.bean.ThemeInfo;
import com.amz.ios.themeclub.util.ThemeUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by GL on 16-12-5.
 */

public final class DataBaseOpenHelper extends SQLiteOpenHelper {
    public static final String TAG = DataBaseOpenHelper.class.getSimpleName();
    public static final String DB_NAME = "themeclub.db";
    public static final String DOWNLOAD_THEME_TABLE = "download_theme";
    public static final int DB_VERSION = 2;

    private Context mContext = null;

    public DataBaseOpenHelper(final Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        onUpgrade(db, 0, DB_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int version = oldVersion + 1; version <= newVersion; version++) {
            upgradeTo(db, version);
        }
    }

    private void upgradeTo(SQLiteDatabase db, int version) {
        db.beginTransaction();
        switch (version) {
            case 1:
                createThemeTable(db);
                break;
            case 2:
                setPackageNamePrimary(db);
                break;
            default:
                throw new IllegalStateException("Don't know how to upgrade to " + version);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    private void createThemeTable(SQLiteDatabase db) {
        Log.d(TAG, "createThemeTable: ");
        try {
            db.execSQL("DROP TABLE IF EXISTS " + DOWNLOAD_THEME_TABLE);
            db.execSQL("CREATE TABLE " + DOWNLOAD_THEME_TABLE + "(" +
                    ThemeInfo.Impl._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    ThemeInfo.Impl.THEME_PACKAGE_NAME + " TEXT, " +
                    ThemeInfo.Impl.THEME_PATH + " TEXT, " +
                    ThemeInfo.Impl.THEME_TYPE + " TEXT, " +
                    ThemeInfo.Impl.THEME_TITLE + " TEXT, " +
                    ThemeInfo.Impl.THEME_FONT + " TEXT, " +
                    ThemeInfo.Impl.THEME_AUTHOR + " TEXT, " +
                    ThemeInfo.Impl.THEME_DESCRIPTION + " TEXT, " +
                    ThemeInfo.Impl.THEME_VERSION + " TEXT, " +
                    ThemeInfo.Impl.THEME_THUMB + " BINARY, " +
                    ThemeInfo.Impl.THEME_ICONTHUMB + " BINARY, " +
                    ThemeInfo.Impl.THEME_LASTUPDATETIME + " INTEGER);"
            );
            String packageName =mContext.getPackageName();
            if (ThemeConfig.getDefaultThemePkg().equals(packageName)) {
                addDefaultTheme(db);
            }
            initThemeTable(db);
        } catch (SQLException ex) {
            Log.e(TAG, "couldn't create table in themeclub database");
            throw ex;
        }
    }

    private void setPackageNamePrimary(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + "temp" + "(" +
                ThemeInfo.Impl._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ThemeInfo.Impl.THEME_PACKAGE_NAME + " UNIQUE NOT NULL ON CONFLICT Ignore, " +
                ThemeInfo.Impl.THEME_PATH + " TEXT, " +
                ThemeInfo.Impl.THEME_TYPE + " TEXT, " +
                ThemeInfo.Impl.THEME_TITLE + " TEXT, " +
                ThemeInfo.Impl.THEME_FONT + " TEXT, " +
                ThemeInfo.Impl.THEME_AUTHOR + " TEXT, " +
                ThemeInfo.Impl.THEME_DESCRIPTION + " TEXT, " +
                ThemeInfo.Impl.THEME_VERSION + " TEXT, " +
                ThemeInfo.Impl.THEME_THUMB + " BINARY, " +
                ThemeInfo.Impl.THEME_ICONTHUMB + " BINARY, " +
                ThemeInfo.Impl.THEME_LASTUPDATETIME + " INTEGER);"
        );
        db.execSQL("REPLACE INTO TEMP SELECT * FROM " + DOWNLOAD_THEME_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DOWNLOAD_THEME_TABLE);
        db.execSQL("Alter Table temp Rename To " + DOWNLOAD_THEME_TABLE);
    }

    private void addDefaultTheme(SQLiteDatabase db) {
        ContentValues defaultCv = new ContentValues();
        defaultCv.put(ThemeInfo.Impl.THEME_PACKAGE_NAME, mContext.getPackageName());
        defaultCv.put(ThemeInfo.Impl.THEME_LASTUPDATETIME, Long.MAX_VALUE);
        try {
            ByteArrayOutputStream osThumb = new ByteArrayOutputStream();
//            Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
//                    R.drawable.default_wallpaper);
            Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                    com.ios.theme.def.R.drawable.theme_preview_op0);
            bitmap.compress(Bitmap.CompressFormat.JPEG,
                    ThemeUtils.THEME_THUMB_QUALITY, osThumb);
            defaultCv.put(ThemeInfo.Impl.THEME_THUMB, osThumb.toByteArray());
            db.replace(DataBaseOpenHelper.DOWNLOAD_THEME_TABLE, null, defaultCv);
            osThumb.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void initThemeTable(SQLiteDatabase db) {
        List<PackageInfo> allAppList;
        ThemeInfo themeInfo;
        PackageManager pm = mContext.getPackageManager();
        allAppList = pm.getInstalledPackages(0);
        for (PackageInfo info : allAppList) {
            if (info.packageName.startsWith(ThemeUtils.PEFIX_OF_THEME_PACKAGE_NAME)) {
                themeInfo = ThemeUtils.getThemeInfo(mContext, info.applicationInfo.sourceDir, info.packageName);
                if (themeInfo != null) {
                    ContentValues cv = new ContentValues();
                    cv.put(ThemeInfo.Impl.THEME_PACKAGE_NAME, themeInfo.packageName);
                    cv.put(ThemeInfo.Impl.THEME_PATH, themeInfo.themePath);
                    cv.put(ThemeInfo.Impl.THEME_TYPE, themeInfo.themeType);
                    cv.put(ThemeInfo.Impl.THEME_TITLE, themeInfo.title);
                    cv.put(ThemeInfo.Impl.THEME_FONT, themeInfo.font);
                    cv.put(ThemeInfo.Impl.THEME_AUTHOR, themeInfo.author);
                    cv.put(ThemeInfo.Impl.THEME_DESCRIPTION, themeInfo.description);
                    cv.put(ThemeInfo.Impl.THEME_VERSION, themeInfo.version);
                    try {
                        ByteArrayOutputStream osThumb = new ByteArrayOutputStream();
                        themeInfo.thumb.compress(Bitmap.CompressFormat.JPEG, ThemeUtils.THEME_THUMB_QUALITY, osThumb);
                        cv.put(ThemeInfo.Impl.THEME_THUMB, osThumb.toByteArray());
                        osThumb.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    cv.put(ThemeInfo.Impl.THEME_LASTUPDATETIME, info.lastUpdateTime);
                    db.replace(DataBaseOpenHelper.DOWNLOAD_THEME_TABLE, null, cv);
                }
            }
        }
    }


}

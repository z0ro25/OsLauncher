package com.amz.ios.themeclub;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;

import com.amz.ios.themeclub.bean.ThemeInfo;
import com.amz.ios.themeclub.dao.DataBaseOpenHelper;
import com.amz.ios.themeclub.util.AppUtils;
import com.amz.ios.themeclub.util.PreferencesUtils;
import com.amz.ios.themeclub.util.ThemeUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by GL on 16-12-6.
 */

public class PackageBroadcastReceiver extends BroadcastReceiver{
    private static final String TAG = PackageBroadcastReceiver.class.getSimpleName();
    private static final String PREFIX_PACKAGE = "package:";

    private PackageManager pm;
    private PackageInfo packageInfo;
    private ThemeInfo themeInfo;
    private DataBaseOpenHelper dataBaseOpenHelper;
    private SQLiteDatabase db;

    public PackageBroadcastReceiver() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String packageName = null;
        String action = null;
        if(intent != null
                && intent.getDataString()!=null
                && intent.getDataString().startsWith(PREFIX_PACKAGE) ){
            action = intent.getAction();
            packageName = intent.getDataString().split(PREFIX_PACKAGE)[1];
        }else {
            return;
        }

        if (packageName !=null && packageName.startsWith(ThemeInfo.PREFIX_OF_THEME_NAME)) {
            try{
                pm = context.getPackageManager();
                dataBaseOpenHelper = new DataBaseOpenHelper(context);
                db = dataBaseOpenHelper.getWritableDatabase();

                if( action.equals(Intent.ACTION_PACKAGE_ADDED) ){
                    packageInfo = pm.getPackageInfo(packageName, 0);
                    themeInfo = ThemeUtils.getThemeInfo(context, packageInfo.applicationInfo.sourceDir, packageName);
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
                        ByteArrayOutputStream osThumb = new ByteArrayOutputStream();
                        themeInfo.thumb.compress(Bitmap.CompressFormat.JPEG, ThemeUtils.THEME_THUMB_QUALITY, osThumb);
                        cv.put(ThemeInfo.Impl.THEME_THUMB, osThumb.toByteArray());
                        osThumb.close();
                        cv.put(ThemeInfo.Impl.THEME_LASTUPDATETIME, packageInfo.lastUpdateTime);
                        db.replace(DataBaseOpenHelper.DOWNLOAD_THEME_TABLE, null, cv);

                    }

                    String path = PreferencesUtils.getString(context,"installapk");
                    AppUtils.deleteApkFile(path);

                }else if( action.equals(Intent.ACTION_PACKAGE_REPLACED) ){
                    packageInfo = pm.getPackageInfo(packageName, 0);
                    themeInfo = ThemeUtils.getThemeInfo(context, packageInfo.applicationInfo.sourceDir, packageName);
                    ContentValues cv = new ContentValues();
                    cv.put(ThemeInfo.Impl.THEME_LASTUPDATETIME, packageInfo.lastUpdateTime);
                    db.update(DataBaseOpenHelper.DOWNLOAD_THEME_TABLE, cv, ThemeInfo.Impl.THEME_PACKAGE_NAME+"=?", new String[]{packageName});

                }else if( action.equals(Intent.ACTION_PACKAGE_REMOVED) ){
                    db.delete(DataBaseOpenHelper.DOWNLOAD_THEME_TABLE, ThemeInfo.Impl.THEME_PACKAGE_NAME+"=?", new String[]{packageName});

                }
                context.sendBroadcast(new Intent(ThemeUtils.BROADCAST_THEME_NEED_UPDATE));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if (db != null) {
                    db.close();
                }
            }
        }
    }
}

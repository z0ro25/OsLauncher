package com.amz.ios.launcher.expdev;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.amz.ios.launcher.AppInfo;
import com.amz.ios.launcher.LauncherSettings;

import java.util.ArrayList;

public class NewInstallAppHandler {
    public static final String TAG = "NewInstallAppHandler";

//    public static final String TABLE_NEWINSTALLEEVENT = "newinstall";
//
//    public static final class NewInstallEvent implements LauncherSettings.BaseLauncherColumns {
//        public static final String NEWINSTALLFLAG = "newinstallflag";
//        public static final String PACKAGE_NAME = "packagename";
//        //public static final Uri CONTENT_URI = Uri.parse("content://" + ProviderConfig.AUTHORITY + "/newinstall?notify=true");
//        public static final Uri CONTENT_URI_NO_NOTIFICATION = Uri.parse("content://" + ProviderConfig.AUTHORITY + "/newinstall");
//
//        NewInstallEvent() {
//        }
//    }

    public static ArrayList<String> loadNewFlagApps(Context context) {
        ArrayList<String> newApps = new ArrayList();

        ContentResolver resolver = context.getContentResolver();
        Uri localUri = LauncherSettings.NewInstall.CONTENT_URI;

        String[] projection = new String[1];
        projection[0] = LauncherSettings.NewInstall.PACKAGENAME;

        String[] selectionArgs = new String[1];
        selectionArgs[0] = "0";

        try {
            Cursor cursor = resolver.query(localUri, projection, LauncherSettings.NewInstall.NEW_INSTALL_FLAG + " = ?", selectionArgs, null);
            if (cursor != null) {
                while(cursor.moveToNext()) {
                    int packageNameColumnIndex = cursor.getColumnIndexOrThrow(LauncherSettings.NewInstall.PACKAGENAME);
                    String packageName = cursor.getString(packageNameColumnIndex);
                    newApps.add(packageName);
                }

                cursor.close();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

        return newApps;
    }


    public static void updateNewFlagInDatabase(Context context, AppInfo appInfo)
    {
        int newInstalled = appInfo.newInstalled ? 1 : 0;

        String str = appInfo.componentName.getPackageName();
        ContentResolver resolver = context.getContentResolver();
        String[] selectionArgs = new String[1];
        selectionArgs[0] = str;
        try
        {
            ContentValues localContentValues = new ContentValues();
            localContentValues.put(LauncherSettings.NewInstall.NEW_INSTALL_FLAG, Integer.valueOf(newInstalled));
            localContentValues.put(LauncherSettings.NewInstall.PACKAGENAME, str);
            localContentValues.put(LauncherSettings.NewInstall.MODIFIED, 1);

            Uri uri = LauncherSettings.NewInstall.CONTENT_URI;
            String[] projection = new String[1];
            projection[0] = LauncherSettings.NewInstall.NEW_INSTALL_FLAG;

            Cursor cursor = resolver.query(uri, projection, LauncherSettings.NewInstall.PACKAGENAME + " = ?", selectionArgs, null);

            if (cursor != null) {
                if (cursor.getCount() > 0)
                {
                    resolver.update(LauncherSettings.NewInstall.CONTENT_URI, localContentValues, LauncherSettings.NewInstall.PACKAGENAME + "= ?", selectionArgs);
                } else {
                    localContentValues.put(LauncherSettings.NewInstall._ID, Long.valueOf(getMaxId(context)));
                    resolver.insert(LauncherSettings.NewInstall.CONTENT_URI, localContentValues);
                }

                if (cursor != null)
                    cursor.close();
            }
        }
        catch (Exception e) {}
    }

    private static long getMaxId(final Context context) {
        final ContentResolver contentResolver = context.getContentResolver();
        Cursor c = contentResolver.query(LauncherSettings.NewInstall.CONTENT_URI,
                new String[] { LauncherSettings.NewInstall._ID },
                null,
                null,
                LauncherSettings.NewInstall._ID + " DESC LIMIT 1");

        long maxId = 1;
        if (c != null && c.moveToNext()) {
            maxId = c.getLong(0) + 1;
        }

        Log.d(TAG, "generated Max ID = " + maxId);
        return maxId;
    }

    public static void updateNewFlagItems(Context context, AppInfo appInfo)
    {
        String str = appInfo.componentName.getPackageName();
        ContentResolver resolver = context.getContentResolver();
        String[] selectionArgs = new String[1];
        selectionArgs[0] = str;
        int newInstalled = appInfo.newInstalled ? 1 : 0;

        try
        {
            Uri uri = LauncherSettings.NewInstall.CONTENT_URI;
            String[] projections = new String[1];
            projections[0] = LauncherSettings.NewInstall.NEW_INSTALL_FLAG;
            Cursor cursor = resolver.query(uri, projections, LauncherSettings.NewInstall.PACKAGENAME + " = ?", selectionArgs, null);

            int count = cursor.getCount();
            ContentValues localContentValues = new ContentValues();
            localContentValues.put(LauncherSettings.NewInstall.NEW_INSTALL_FLAG, Integer.valueOf(newInstalled));

            if (count == 0)
            {
                localContentValues.put(LauncherSettings.NewInstall._ID, Long.valueOf(appInfo.id));
                localContentValues.put(LauncherSettings.NewInstall.PACKAGENAME, str);
                resolver.insert(LauncherSettings.NewInstall.CONTENT_URI, localContentValues);
                if (cursor != null)
                    cursor.close();
                return;
            }

            while (cursor.moveToNext())
            {
                localContentValues.put(LauncherSettings.NewInstall.NEW_INSTALL_FLAG, Integer.valueOf(newInstalled));
                localContentValues.put(LauncherSettings.NewInstall.PACKAGENAME, str);

                resolver.update(LauncherSettings.NewInstall.CONTENT_URI, localContentValues, LauncherSettings.NewInstall.PACKAGENAME +"= ?", selectionArgs);
            }
        }
        catch (Exception e) {}
    }
}

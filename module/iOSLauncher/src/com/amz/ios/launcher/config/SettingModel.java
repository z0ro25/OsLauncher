package com.amz.ios.launcher.config;


import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.amz.ios.ioslite.common.launcher.LauncherSettingSubject;
import com.amz.ios.ioslite.common.provider.AppUsagesProvider;
import com.amz.ios.ioslite.common.util.CommonUtilities;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherModel;
import com.amz.ios.launcher.LauncherSettings;
import com.amz.ios.launcher.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class SettingModel {
    private static final String TAG = "SettingModel";

    private static long getMaxId(final Context context) {
        final ContentResolver contentResolver = context.getContentResolver();
        Cursor c = contentResolver.query(LauncherSettings.BackupSettings.CONTENT_URI,
                new String[] { LauncherSettings.BackupSettings._ID },
                null,
                null,
                LauncherSettings.BackupSettings._ID + " DESC LIMIT 1");

        long maxId = 1;
        if (c != null && c.moveToNext()) {
            maxId = c.getLong(0) + 1;
        }

        Log.d(TAG, "generated Max ID = " + maxId);
        return maxId;
    }

    private static void updateDatabase(Context context, String key, String value) {
        final ContentResolver contentResolver = context.getContentResolver();
        final ContentValues values = new ContentValues();

        long recordId = getFieldId(context, key);

        if (recordId == -1) {
            long newId = getMaxId(context);
            values.put(LauncherSettings.BackupSettings._ID, newId);
            values.put(LauncherSettings.BackupSettings.SETTING_NAME, key);
            values.put(LauncherSettings.BackupSettings.SETTING_VALUE, value);
            values.put(LauncherSettings.BackupSettings.MODIFIED, 1);
            try {
                Uri newUri = contentResolver.insert(LauncherSettings.BackupSettings.CONTENT_URI, values);
                Log.d(TAG, "updateDatabase = " + newUri);
            }catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            final Uri uri = LauncherSettings.BackupSettings.getContentUri(recordId);
            values.put(LauncherSettings.BackupSettings.SETTING_VALUE, value);
            try {
                int updateRows = contentResolver.update(uri, values, null, null);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void printDB(final Context context) {
        final ContentResolver contentResolver = context.getContentResolver();
        final Uri contentUri = LauncherSettings.BackupSettings.CONTENT_URI;
        Cursor cursor = null;

        try {
            cursor = contentResolver.query(contentUri, null, null, null, null);
            final int idIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BackupSettings._ID);
            final int nameFieldIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BackupSettings.SETTING_NAME);
            final int valueFieldIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BackupSettings.SETTING_VALUE);

            Log.e(TAG, "printDB loading start");
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIndex);
                String settingName = cursor.getString(nameFieldIndex);
                String settingValue = cursor.getString(valueFieldIndex);

                Log.d(TAG, "id" + id +  ":name = " + settingName + ":value = " + settingValue);
            }
        } catch (Exception e) {
            Log.e(TAG, "loading interrupted", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }

            Log.e(TAG, "printDB loading end");
        }
    }

    private static String getFieldValue(final Context context, final String key) {
        printDB(context);
        ArrayList<String> values = new ArrayList<String>();

        final ContentResolver contentResolver = context.getContentResolver();
        final Uri contentUri = LauncherSettings.BackupSettings.CONTENT_URI;
        Cursor cursor = null;

        try {

            String[] selectionArgs = new String[]{ "%"+key+"%" };
            cursor = contentResolver.query(contentUri, null, LauncherSettings.BackupSettings.SETTING_NAME+" like ? ", selectionArgs, null);
            final int idIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BackupSettings._ID);
            final int nameFieldIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BackupSettings.SETTING_NAME);
            final int valueFieldIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BackupSettings.SETTING_VALUE);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIndex);
                String settingName = cursor.getString(nameFieldIndex);
                String settingValue = cursor.getString(valueFieldIndex);

                if (settingName.equals(key)) {
                    values.add(settingValue);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "loading interrupted", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (values.size() >= 1) {
            return values.get(0);
        }
        return "";
    }

    private static long getFieldId(Context context, String key) {
        long recordID = -1;
        final ContentResolver contentResolver = context.getContentResolver();
        final Uri contentUri = LauncherSettings.BackupSettings.CONTENT_URI;

        String[] selectionArgs = new String[]{ "%"+key+"%" };
        final Cursor cursor = contentResolver.query(contentUri, null, LauncherSettings.BackupSettings.SETTING_NAME+" like ? ", selectionArgs, null);
        try {
            final int idIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BackupSettings._ID);
            final int nameFieldIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BackupSettings.SETTING_NAME);
            final int valueFieldIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BackupSettings.SETTING_VALUE);

            while (cursor.moveToNext()) {
                recordID = cursor.getLong(idIndex);
                String settingName = cursor.getString(nameFieldIndex);
                if (settingName.equals(key)) {
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "loading interrupted", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return recordID;
    }

    public static void putBoolean(Context context, String key, boolean value) {
        updateDatabase(context, key, String.valueOf(value));
        notifyChange(key);
    }

    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        String value = getFieldValue(context, key);
        if (!value.isEmpty()) {
            return Boolean.valueOf(value);
        }
        return defaultValue;
    }

    public static void putInt(Context context, String key, int value) {
        updateDatabase(context, key, String.valueOf(value));
        notifyChange(key);
    }

    public static int getInt(Context context, String key, int defaultValue) {
        String value = getFieldValue(context, key);
        if (!value.isEmpty()) {
            try {
                return Integer.valueOf(value);
            } catch(Exception e) {
                Log.d(TAG, "key = " + key + e.getMessage());
            }
        }

        return defaultValue;
    }

    public static void putFloat(Context context, String key, float value) {
        updateDatabase(context, key, String.valueOf(value));
        notifyChange(key);
    }

    public static float getFloat(Context context, String key, float defaultValue) {
        String value = getFieldValue(context, key);
        if (!value.isEmpty()) {
            try {
                return Float.valueOf(value);
            }catch(Exception e) {
                Log.d(TAG, "key = " + key + " message = " + e.getMessage());
            }
        }

        return defaultValue;
    }

    public static void putString(Context context, String key, String value) {
        updateDatabase(context, key, value);
        notifyChange(key);
    }

    public static String getString(Context context, String key, String defaultValue) {
        String value = getFieldValue(context, key);
        if (!value.isEmpty()) {
            return value;
        }

        return defaultValue;
    }

    public static void notifyChange(String key) {
        LauncherSettingSubject.handleLauncherSettingChanged(key);
    }
}

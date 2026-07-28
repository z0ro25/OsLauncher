package com.amz.ios.ioslite.common.setting;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import com.amz.ios.ioslite.common.Partner;

public class SettingProvider extends ContentProvider {
    private static final String TAG = "SettingProvider";
    private static int SETTING_PROVIDER_VERSION = 5;

    private static final String KEY_SP_VERSION = "setting_provider_version";
    private static final String PREFS_FILE_NAME = "settingprovider";

    private SharedPreferences mSharePrefs;
    private Context mContext;

    @Override
    public boolean onCreate() {
        mContext = getContext();
        SETTING_PROVIDER_VERSION = Partner.getInteger(mContext, Partner.DEF_SETTING_DB_VERSION, SETTING_PROVIDER_VERSION);
        mSharePrefs = mContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
        loadDefSettingsIfNecessary();
        return true;
    }


    private void loadDefSettingsIfNecessary() {
        int oldVersion = mSharePrefs.getInt(KEY_SP_VERSION, 0);
        if (oldVersion == SETTING_PROVIDER_VERSION) {
            return;
        }

        SharedPreferences.Editor editor = mSharePrefs.edit();
        editor.clear();
        // Set defautlt settings on first load;
        if (oldVersion < 2) {
            editor.putInt(IOSSettings.Launcher.LAUNCHER_WORKSPACE_ICON_LABEL_COLOR, Partner.getColor(mContext, Partner.DEF_WORKSPACE_LABEL_COLOR, Color.WHITE));
        }
        editor.putInt(KEY_SP_VERSION, SETTING_PROVIDER_VERSION);
        editor.commit();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (Binder.getCallingUid() != Process.myUid()) {
            Log.w(TAG, "not current uid call , return;");
            return null;
        }
        Log.w(TAG, "call method : " + method + ", arg : " + arg);

        switch (method) {
            case IOSSettings.METHOD_GET_BOOLEAN: {
                boolean defaultValue = extras.getBoolean(IOSSettings.EXTRA_DEFAULT_VALUE);

                Bundle result = new Bundle();
                result.putBoolean(IOSSettings.EXTRA_VALUE, getBoolean(arg, defaultValue));
                return result;
            }
            case IOSSettings.METHOD_SET_BOOLEAN: {
                boolean oldValue = getBoolean(arg, false);
                boolean newValue = extras.getBoolean(IOSSettings.EXTRA_VALUE);
                putBoolean(arg, newValue);

                Bundle result = new Bundle();
                result.putBoolean(IOSSettings.EXTRA_VALUE, newValue);

                if (oldValue != newValue) {
                    notifyChangeFor(arg);
                }
                return result;

            }
            case IOSSettings.METHOD_GET_STRING: {
                String defaultValue = extras.getString(IOSSettings.EXTRA_DEFAULT_VALUE);

                Bundle result = new Bundle();
                result.putString(IOSSettings.EXTRA_VALUE, getString(arg, defaultValue));
                return result;
            }
            case IOSSettings.METHOD_SET_STRING: {
                String oldValue = getString(arg, "null");
                String newValue = extras.getString(IOSSettings.EXTRA_VALUE);
                putString(arg, newValue);

                Bundle result = new Bundle();
                result.putString(IOSSettings.EXTRA_VALUE, newValue);

                if (!oldValue.equals(newValue)) {
                    notifyChangeFor(arg);
                }
                return result;

            }
            case IOSSettings.METHOD_GET_INTEGER: {
                int defaultValue = extras.getInt(IOSSettings.EXTRA_DEFAULT_VALUE);

                Bundle result = new Bundle();
                result.putInt(IOSSettings.EXTRA_VALUE, getInt(arg, defaultValue));
                return result;
            }
            case IOSSettings.METHOD_SET_INTEGER: {
                int oldValue = getInt(arg, -1);
                int newValue = extras.getInt(IOSSettings.EXTRA_VALUE);
                putInt(arg, newValue);

                Bundle result = new Bundle();
                result.putInt(IOSSettings.EXTRA_VALUE, newValue);

                if (oldValue != newValue) {
                    notifyChangeFor(arg);
                }

                return result;
            }
            case IOSSettings.METHOD_GET_FLOAT: {
                float defaultValue = extras.getFloat(IOSSettings.EXTRA_DEFAULT_VALUE);

                Bundle result = new Bundle();
                result.putFloat(IOSSettings.EXTRA_VALUE, getFloat(arg, defaultValue));
                return result;
            }
            case IOSSettings.METHOD_SET_FLOAT: {
                float oldValue = getFloat(arg, 0);
                float newValue = extras.getFloat(IOSSettings.EXTRA_VALUE);
                putFloat(arg, newValue);

                Bundle result = new Bundle();
                result.putFloat(IOSSettings.EXTRA_VALUE, newValue);
                if (oldValue != newValue) {
                    notifyChangeFor(arg);
                }

                return result;

            }
        }
        return null;
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        return mSharePrefs.getBoolean(key, defaultValue);
    }

    private void putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = mSharePrefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private String getString(String key, String defaultValue) {
        return mSharePrefs.getString(key, defaultValue);
    }

    private void putString(String key, String value) {
        SharedPreferences.Editor editor = mSharePrefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private int getInt(String key, int defaultValue) {
        return mSharePrefs.getInt(key, defaultValue);
    }

    private void putInt(String key, int value) {
        SharedPreferences.Editor editor = mSharePrefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private float getFloat(String key, float defaultValue) {
        return mSharePrefs.getFloat(key, defaultValue);
    }

    private void putFloat(String key, float value) {
        SharedPreferences.Editor editor = mSharePrefs.edit();
        editor.putFloat(key, value);
        editor.apply();
    }


    private void notifyChangeFor(String arg) {
        getContext().getContentResolver().notifyChange(IOSSettings.getUriFor(arg), null);
    }

}

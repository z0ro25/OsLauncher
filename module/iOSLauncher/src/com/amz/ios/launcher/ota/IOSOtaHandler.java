package com.amz.ios.launcher.ota;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;


public class IOSOtaHandler {

    public static final String OTA2_UPDATE_HIDE = "com.zhuoyi.security.service.UpdateHide";
    public static final String OTA2_UPDATE_HIDE_ENTRY = "com.zhuoyi.security.service.UpdateHideEntry";
    public static final String TAG = "ota2";
    public static boolean ENABLE_OTA_2_FEATURE;
    public static final String OTA2_PACKAGE_NAME = "com.zhuoyi.security.service";
    public static final String ACTION_UPDATE_LABEL = "com.amz.ios.launcher.ACTION_UPDATE_LABEL";
    public static final String ACTION_UPDATE_CATEGORY = "com.amz.ios.launcher.ACTION_UPDATE_CATEGORY";
    public static final String ACTION_RELOAD_ICON = "com.amz.ios.launcher.ACTION_RELOAD_ICON";
    public static final String EXTRA_ICON_ID = "com.amz.ios.launcher.EXTRA_ICON_ID";
    public static final String EXTRA_ICON_PATH = "com.amz.ios.launcher.EXTRA_ICON_PATH";
    public static final String EXTRA_COMPONENT_NAME = "com.amz.ios.launcher.EXTRA_COMPONENT_NAME";
    public static final String EXTRA_APP_LABEL = "com.amz.ios.launcher.EXTRA_APP_LABEL";
    public static final String EXTRA_APP_CATEGORY = "com.amz.ios.launcher.EXTRA_APP_CATEGORY";

    private static List<String> mExternalHideList;
    private static HashMap<String, String> mExternalHideEntryMap;

    public static boolean isAppHide(Context context, String pkg) {
        if (ENABLE_OTA_2_FEATURE) {
            if (mExternalHideList == null) {
                updateExternalHideList(context);
            }

            for (String pkgName : mExternalHideList) {
                if (pkgName.equals(pkg)) {
                    Log.i(TAG, "external hide : " + pkg);
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isAppEntryHide(Context context, String packageName, String className) {
        if (ENABLE_OTA_2_FEATURE) {
            if (mExternalHideEntryMap == null) {
                updateExternalHideEntryMap(context);
            }

            Log.i(TAG, "isAppEntryHide -- " + packageName + ", " + className);

            if (!packageName.equals(OTA2_PACKAGE_NAME) || !mExternalHideEntryMap.containsKey(className)) {
                return false;
            }

            String entry = mExternalHideEntryMap.get(className);

            try {
                context.getPackageManager().getApplicationInfo(entry, PackageManager.GET_ACTIVITIES);
            } catch (Exception e) {
                Log.i(TAG, "external entry hide : " + entry);
                return true;
            }
        }

        return false;
    }

    public static ComponentName getAppEntryRepalced(Context context, String pkgname) {
        if (ENABLE_OTA_2_FEATURE) {
            if (mExternalHideEntryMap == null) {
                updateExternalHideEntryMap(context);
            }
            Iterator<Map.Entry<String, String>> itr = mExternalHideEntryMap.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, String> entry = itr.next();

                if (entry.getValue().equals(pkgname)) {
                    return new ComponentName(OTA2_PACKAGE_NAME, entry.getKey());
                }
            }
        }

        return null;
    }

    public static String getHideAppPackageName(Context context, String key) {
        if (ENABLE_OTA_2_FEATURE) {
            if (mExternalHideEntryMap == null) {
                updateExternalHideEntryMap(context);
            }
            return mExternalHideEntryMap.get(key);
        }

        return null;
    }

    public static ComponentName getHideAppComponentName(Context context, String className) {
        ComponentName componentName = null;

        if (IOSOtaHandler.ENABLE_OTA_2_FEATURE) {
            String pkgName = getHideAppPackageName(context, className);
            if (pkgName != null) {
                Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkgName);
                if (intent != null) {
                    List<ResolveInfo> apps = context.getPackageManager().queryIntentActivities(intent, 0);
                    if (apps != null && apps.size() > 0) {
                        componentName = new ComponentName(apps.get(0).activityInfo.applicationInfo.packageName, apps.get(0).activityInfo.name);
                    }
                }
            }
        }

        return componentName;
    }

    public static void updateExternalHideList(Context context) {
        mExternalHideList = getExternalHideList(context);
    }

    public static List<String> getExternalHideList() {
        return mExternalHideList;
    }

    private static List<String> getExternalHideList(Context context) {
        Log.i(TAG, "get external hide app list");

        List<String> list = new ArrayList<String>();
        Cursor cursor = null;

        try {
            Uri uri = Uri.parse("content://com.zhuoyi.security.service.provider.HideAppProvider/hide_table");
            cursor = context.getContentResolver().query(uri, null, null, null, null);

            if (cursor != null) {
                Log.i(TAG, "count = " + cursor.getCount());

                while (cursor.moveToNext()) {
                    String pkg = cursor.getString(1);

                    Log.i(TAG, "pkg name = " + pkg);
                    list.add(pkg);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "get external hide list error!");
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return list;
    }

    public static void updateExternalHideEntryMap(Context context) {
        mExternalHideEntryMap = getExternalHideEntryMap(context);
    }

    private static HashMap<String, String> getExternalHideEntryMap(Context context) {
        Log.i(TAG, "get external hide entry map");

        HashMap<String, String> map = new HashMap<String, String>();
        Cursor cursor = null;

        try {
            Uri uri = Uri.parse("content://com.zhuoyi.security.service.provider.HideAppProvider/entry_table");
            cursor = context.getContentResolver().query(uri, null, null, null, null);

            if (cursor != null) {
                Log.i(TAG, "count = " + cursor.getCount());

                while (cursor.moveToNext()) {
                    String key = OTA2_PACKAGE_NAME + "." + cursor.getString(1);
                    String value = cursor.getString(2);

                    Log.i(TAG, "key = " + key + ", value = " + value);
                    map.put(key, value);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "get external hide entry map error!");
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return map;
    }
}

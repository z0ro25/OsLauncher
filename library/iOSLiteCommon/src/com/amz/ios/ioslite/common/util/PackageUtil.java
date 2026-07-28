package com.amz.ios.ioslite.common.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Convenience function:
 * <p/>
 * 1. Check if app of package is a system app;
 * 2. Get package versionCode, versionName;
 * 3. Check if app of package is installed;
 * 4. Check if app of package is google app;
 */
public class PackageUtil {
    // Google packages;
    private static final String GOOGLE_PACKAGE_PREFIX = "com.google.android";
    private static final List<String> GOOGLE_PACKAGE_LIST = Arrays.asList("com.android.vending", "com.google.android", "com.android.chrome");

    /**
     * Check  if componentName is a system app ;
     */
    public static boolean isSystemApp(Context context, ComponentName componentName) {
        final String packageName = componentName.getPackageName();
        final PackageManager pm = context.getPackageManager();
        return isSystemApp(pm, packageName);
    }


    public static boolean isSystemApp(Context context, String packageName) {
        final PackageManager pm = context.getPackageManager();
        return isSystemApp(pm, packageName);
    }


    public static boolean isSystemApp(PackageManager pm, String packageName) {
        if (packageName != null) {
            try {
                PackageInfo info = pm.getPackageInfo(packageName, 0);
                return (info != null) && (info.applicationInfo != null) &&
                        ((info.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0);
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        } else {
            return false;
        }
    }


    /**
     * Check the package for this packageName is installed;
     */
    public static boolean isAppInstalled(Context context, String packageName) {
        if (!TextUtils.isEmpty(packageName)) {
            try {
                final PackageManager pm = context.getPackageManager();
                PackageInfo info = pm.getPackageInfo(packageName, 0);
                return info != null ? true : false;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    static PackageInfo getPackageInfoForCfg(Context context) {
        PackageInfo pi = null;

        try {
            PackageManager pm = context.getPackageManager();
            pi = pm.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_CONFIGURATIONS);

            return pi;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return pi;
    }


    /**
     * Check the component is belong to a google package;
     */
    public static boolean isGoogleApp(ComponentName component) {
        return component.getPackageName().startsWith(GOOGLE_PACKAGE_PREFIX)
                || GOOGLE_PACKAGE_LIST.contains(component.getPackageName());
    }
}

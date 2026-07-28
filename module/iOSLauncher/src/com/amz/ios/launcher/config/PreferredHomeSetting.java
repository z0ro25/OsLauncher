package com.amz.ios.launcher.config;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;

import java.lang.reflect.Method;

/**
 * PreferredHomeSettings guide user to set IOS as user's default Launcher;
 * Author : huangshuai
 * Modified Date : 20160813
 */

public class PreferredHomeSetting {
    private static final boolean DEBUG = false;
    private static final String TAG = "PreferredHomeSetting";

    private static boolean startPreferredSettings(Context context) {
        DebugLog.d(TAG, "startPreferredSettings()");
        try {
            DebugLog.d(TAG, "start PreferredSettingsActivity");
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);
            intentFilter.addCategory(Intent.CATEGORY_HOME);

            Intent appIntent = new Intent(Intent.ACTION_MAIN);
            appIntent.addCategory(Intent.CATEGORY_HOME);

            Intent intent = new Intent();
            if (!(context instanceof Activity)) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            intent.setClassName("com.android.settings", "com.android.settings.Settings$PreferredSettingsActivity");
            intent.putExtra("preferred_app_" +
                    "package_name", context.getPackageName());
            intent.putExtra("is_user_confirmed", true);
            intent.putExtra("preferred_app_intent", appIntent);
            intent.putExtra("preferred_app_intent_filter", intentFilter);
            intent.putExtra("preferred_app_label", context.getString(R.string.settings_default_launcher));
            context.startActivity(intent);
            return true;
        } catch (Exception exception) {
            Log.w(TAG, "start PreferredSettingsActivity fail", exception);
            try {
                Intent intent = new Intent("com.android.settings.PREFERRED_SETTINGS");
                if (!(context instanceof Activity)) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                context.startActivity(intent);
                return true;
            } catch (Exception ex) {
                DebugLog.e(TAG, "start PREFERRED_SETTINGS fail", ex);
            }
        }

        return false;

    }

    private static boolean startHomeSettinsActivity(Context context) {
        if (!BuildUtil.ATLEAST_LOLLIPOP) {
            return false;
        }
        DebugLog.d(TAG, "startHomeSettinsActivity()");
        try {
            Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
            if (!(context instanceof Activity)) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
            return true;
        } catch (Exception exception) {
            DebugLog.e(TAG, "startHomeSettinsActivity fail", exception);
        }

        return false;

    }


    private static boolean startLauncherChooser(Context context) {
        return startPreferredSettings(context) && startHomeSettinsActivity(context);
    }

    public static void clearDefaultHomeApp(Context context) {
        DebugLog.d(TAG, "clearDefaultHomeApp()");
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.MAIN");
            intentFilter.addCategory("android.intent.category.HOME");
            intentFilter.addCategory("android.intent.category.DEFAULT");
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.HOME");
            intent.setComponent(new ComponentName(context.getPackageName(), Launcher.class.getName()));

            Object iPackageManager;
            Class AppGlobals = Class.forName("android.app.AppGlobals");
            Class IPackageManager = Class.forName("android.content.pm.IPackageManager");
            Method getPackageManager = AppGlobals.getMethod("getPackageManager");
            Method setLastChosenActivity = IPackageManager.getMethod("setLastChosenActivity", Intent.class, String.class, Integer.TYPE, IntentFilter.class, Integer.TYPE, ComponentName.class);
            iPackageManager = getPackageManager.invoke(AppGlobals);
            setLastChosenActivity.invoke(iPackageManager, intent, intent.resolveTypeIfNeeded(context.getContentResolver()), PackageManager.MATCH_DEFAULT_ONLY, intentFilter, IntentFilter.MATCH_CATEGORY_EMPTY | IntentFilter.MATCH_ADJUSTMENT_NORMAL, intent.getComponent());
        } catch (Exception exceptoin) {
            DebugLog.e(TAG, "clearDefaultHomeApp fail", exceptoin);
        }
    }

    public static void setDefaultHomeApp(Context context) {
        if (isDefautHomeApp(context)) {
            return;
        }

        if (!startLauncherChooser(context)) {
            clearDefaultHomeApp(context);

            startResolverActivity(context);
        }
    }

    public static boolean isDefautHomeApp(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo info = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return info.activityInfo.packageName.equals(context.getPackageName()) ? true : false;
    }

    private static boolean isResolverActivityExit(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("android", "com.android.internal.app.ResolverActivity"));
        try {
            return intent.resolveActivity(context.getPackageManager()) != null;
        } catch (Exception paramContext) {
            DebugLog.e(TAG, "com.android.internal.app.ResolverActivity not exit ");
        }
        return false;
    }

    private static void startResolverActivity(Context context) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");

        if (Build.BRAND.equals("lge")) {
            intent.setComponent(new ComponentName("android", "com.android.internal.app.ResolverActivityEx"));
        } else {
            intent.setComponent(new ComponentName("android", "com.android.internal.app.ResolverActivity"));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        } catch (Exception ex) {
            DebugLog.e(TAG, "startResolverActivity fail", ex);
        }
    }

}

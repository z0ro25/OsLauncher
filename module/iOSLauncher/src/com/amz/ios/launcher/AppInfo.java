/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amz.ios.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import com.amz.ios.ioslite.common.CommonSdk;
import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.launcher.compat.LauncherActivityInfoCompat;
import com.amz.ios.launcher.compat.UserHandleCompat;
import com.amz.ios.launcher.compat.UserManagerCompat;
import com.amz.ios.launcher.config.Settings;
import com.amz.ios.launcher.util.ComponentKey;
import com.amz.ios.launcher.util.PackageManagerHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

/**
 * Represents an app in AllAppsView.
 */
public class AppInfo extends ShortcutInfo {
    private static final String TAG = "Launcher3.AppInfo";

    static final int DOWNLOADED_FLAG = 1;
    static final int UPDATED_SYSTEM_APP_FLAG = 2;

    private static final long NEW_INSTALL_PERIOD = 1000 * 60 * 60;
    public ComponentName componentName;
    public ApplicationInfo mApplicationInfo;

    public AppInfo() {
        itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT;
    }

    /**
     * Must not hold the Context.
     */
    public AppInfo(Context context, LauncherActivityInfoCompat info, UserHandleCompat user,
            IconCache iconCache) {
        this(context, info, user, iconCache,
                UserManagerCompat.getInstance(context).isQuietModeEnabled(user));
    }

    public AppInfo(Context context, LauncherActivityInfoCompat info, UserHandleCompat user,
            IconCache iconCache, boolean quietModeEnabled) {

        this.itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION;
        this.componentName = info.getComponentName();
        this.container = ItemInfo.NO_ID;
        mApplicationInfo = info.getApplicationInfo();
        appFlags = info.getApplicationInfo().flags;
        flags = initFlags(context, info);
        if (flags != 0) {
            uninstallable = true;
        }
        firstInstallTime = info.getFirstInstallTime();

        if (PackageManagerHelper.isAppSuspended(info.getApplicationInfo())) {
            isDisabled |= ShortcutInfo.FLAG_DISABLED_SUSPENDED;
        }
        if (quietModeEnabled) {
            isDisabled |= ShortcutInfo.FLAG_DISABLED_QUIET_USER;
        }
        iconCache.getTitleAndIcon(this, info, false /* useLowResIcon */);
        intent = makeLaunchIntent(context, info, user);
        this.user = user;
    }

    public AppInfo(AppInfo info) {
        super(info);
        mApplicationInfo = info.mApplicationInfo;
        componentName = info.componentName;
        title = Utilities.trim(info.title);
        intent = new Intent(info.intent);
        flags = info.flags;
        firstInstallTime = info.firstInstallTime;
        isDisabled = info.isDisabled;
        iconBitmap = info.iconBitmap;
        newInstalled = info.newInstalled;
        uninstallable = info.uninstallable;
        calledNum = info.calledNum;
        lastCalledTime = info.lastCalledTime;
        isHidden = info.isHidden;
        url = info.url;
    }

    public AppInfo(ShortcutInfo shortcutInfo) {
        super(shortcutInfo);
        this.componentName = shortcutInfo.intent.getComponent();
        this.title = String.valueOf(shortcutInfo.title);
    }

    public static int initFlags(Context context, LauncherActivityInfoCompat info) {
        int appFlags = info.getApplicationInfo().flags;
        int flags = 0;
        if ((appFlags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
            flags |= DOWNLOADED_FLAG;

            if ((appFlags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                flags |= UPDATED_SYSTEM_APP_FLAG;
            }
        }

        if (isNotUninstallApp(info.getComponentName().getPackageName())) {
            flags = 0;
        }

        return flags;
    }


    public Intent getIntent() {
        return intent;
    }

    protected Intent getRestoredIntent() {
        return null;
    }

    private static String[] mNotUninstallApps;

    public static boolean isNotUninstallApp(String packageName) {
        if (!TextUtils.isEmpty(packageName)) {
            if (mNotUninstallApps == null) {
                mNotUninstallApps = getNotUninstallList();
            }

            if (mNotUninstallApps == null) {
                return false;
            }

            for (int i = 0; i < mNotUninstallApps.length; i++) {
                if (packageName.equals(mNotUninstallApps[i])) {
                    return true;
                }
            }
        }
        return false;
    }


    public static void refreshNotUninstallApp() {
        mNotUninstallApps = getNotUninstallList();
    }

    private static String[] getNotUninstallList() {
        Context context = CommonSdk.getApplicationContext();
        String[] notUninstallApps = Settings.getNotUninstallAppList(context);
        if (notUninstallApps == null) {
            notUninstallApps = Partner.getStringArray(context, Partner.DEF_NOT_UNINSTALL_APP_LIST);
        }
        return notUninstallApps;
    }

    public int getFlags(){
        int flags = 0;
        if ((appFlags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
            flags |= DOWNLOADED_FLAG;

            if ((appFlags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                flags |= UPDATED_SYSTEM_APP_FLAG;
            }
        }

        if (isNotUninstallApp(componentName.getPackageName())) {
            flags = 0;
        }

        this.flags = flags;

        return this.flags;
    }

    public String getPackageName() {
        if (componentName != null)
            return componentName.getPackageName();
        return "";
    }

    @Override
    public String toString() {
        return "ApplicationInfo(title=" + title + " id=" + this.id
                + " type=" + this.itemType + " container=" + this.container
                + " screen=" + screenId + " cellX=" + cellX + " cellY=" + cellY
                + " spanX=" + spanX + " spanY=" + spanY + " dropPos=" + Arrays.toString(dropPos)
                + " user=" + user + ")";
    }

    /**
     * Helper method used for debugging.
     */
    public static void dumpApplicationInfoList(String tag, String label, ArrayList<AppInfo> list) {
        Log.d(tag, label + " size=" + list.size());
        for (AppInfo info : list) {
            Log.d(tag, "   title=\"" + info.title
                    + " componentName=" + info.componentName);
        }
    }

    public ShortcutInfo makeShortcut() {
        return new ShortcutInfo(this);
    }

    public ComponentKey toComponentKey() {
        return new ComponentKey(componentName, user);
    }

    public static Intent makeLaunchIntent(Context context, LauncherActivityInfoCompat info,
                                          UserHandleCompat user) {
        long serialNumber = UserManagerCompat.getInstance(context).getSerialNumberForUser(user);
        return new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(info.getComponentName())
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                .putExtra(EXTRA_PROFILE, serialNumber);
    }

    @Override
    public boolean isDisabled() {
        return isDisabled != 0;
    }
}

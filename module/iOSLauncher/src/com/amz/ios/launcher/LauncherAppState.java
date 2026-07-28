/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static android.content.Context.RECEIVER_EXPORTED;

import android.app.Application;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.amz.ios.ioslite.common.ContextHelper;
import com.amz.ios.ioslite.common.debug.DebugUtil;
import com.amz.ios.ioslite.common.launcher.LauncherSettingSubject;
import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.ioslite.common.util.DeviceInfoUtil;
import com.amz.ios.ioslite.common.util.PackageUtil;
import com.amz.ios.launcher.accessibility.LauncherAccessibilityDelegate;
import com.amz.ios.launcher.awareness.AppUsagesModel;
import com.amz.ios.launcher.compat.LauncherAppsCompat;
import com.amz.ios.launcher.compat.PackageInstallerCompat;
import com.amz.ios.launcher.compat.UserManagerCompat;
import com.amz.ios.launcher.config.Settings;
import com.amz.ios.launcher.graphics.IconShapeOverride;
import com.amz.ios.launcher.notification.NotificationListener;
import com.amz.ios.launcher.ota.IOSOtaHandler;
import com.amz.ios.launcher.parser.AppCategoryProvider;
import com.amz.ios.launcher.theme.ThemeManager;
import com.amz.ios.launcher.util.ConfigMonitor;
import com.amz.ios.launcher.util.SettingsObserver;
import com.amz.ios.launcher.util.Thunk;

import java.lang.ref.WeakReference;

import static com.amz.ios.launcher.notification.NotificationListener.NOTIFICATION_BADGING;

public class LauncherAppState {
    private static final String TAG = "LauncherAppState";
    private final AppFilter mAppFilter;
    private final BuildInfo mBuildInfo;
    @Thunk
    final LauncherModel mModel;
    private final IconCache mIconCache;
    private final ThemeManager mThemeManager;
    private final WidgetPreviewLoader mWidgetCache;
    final AppUsagesModel mAppUsagesModel;

    private boolean mWallpaperChangedSinceLastCheck;

    private static WeakReference<LauncherProvider> sLauncherProvider;
    private static Context sContext;

    private static LauncherAppState INSTANCE;

    private InvariantDeviceProfile mInvariantDeviceProfile;

    private LauncherAccessibilityDelegate mAccessibilityDelegate;

    private final SettingsObserver mNotificationBadgingObserver;

    private static boolean mNewAppsCategory;

    public static LauncherAppState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LauncherAppState();
        }
        return INSTANCE;
    }

    public static LauncherAppState getInstance(Context context) {
        if (sContext == null) {
            setApplicationContext(context);
        }

        if (INSTANCE == null) {
            INSTANCE = new LauncherAppState();
        }
        return INSTANCE;
    }

    public static LauncherAppState getInstanceNoCreate() {
        return INSTANCE;
    }

    public Context getContext() {
        return sContext;
    }

    public static void initalize(Application context) {
        setApplicationContext(context);
    }

    public static void setApplicationContext(Context context) {
        if (sContext != null) {
            Log.w(Launcher.TAG, "setApplicationContext called twice! old=" + sContext + " new=" + context);
        }
        sContext = context.getApplicationContext();
    }

    private LauncherAppState() {
        if (sContext == null) {
            throw new IllegalStateException("LauncherAppState inited before app context set");
        }

        Log.v(Launcher.TAG, "LauncherAppState inited");
        
        IconShapeOverride.apply(sContext);

        // initialize launcher settings;
        mNewAppsCategory = Settings.isNewAppsCategoryEnable(sContext);
        AppCategoryProvider.getInstance();

        mInvariantDeviceProfile = new InvariantDeviceProfile(sContext);
        mIconCache = new IconCache(sContext, mInvariantDeviceProfile);
        mThemeManager = new ThemeManager(sContext, mInvariantDeviceProfile,mIconCache);
        mWidgetCache = new WidgetPreviewLoader(sContext, mIconCache);

        mAppFilter = AppFilter.loadByName(sContext.getString(R.string.app_filter_class));
        mBuildInfo = BuildInfo.loadByName(sContext.getString(R.string.build_info_class));
        mModel = new LauncherModel(this, mIconCache, mAppFilter);
        mAppUsagesModel = new AppUsagesModel(this);

        LauncherAppsCompat.getInstance(sContext).addOnAppsChangedCallback(mModel);

        // Register intent receivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED);
        // For handling managed profiles
        filter.addAction(LauncherAppsCompat.ACTION_MANAGED_PROFILE_ADDED);
        filter.addAction(LauncherAppsCompat.ACTION_MANAGED_PROFILE_REMOVED);
        filter.addAction(LauncherAppsCompat.ACTION_MANAGED_PROFILE_AVAILABLE);
        filter.addAction(LauncherAppsCompat.ACTION_MANAGED_PROFILE_UNAVAILABLE);

        // For live calendar icon user;
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(IOSOtaHandler.ACTION_RELOAD_ICON);
        filter.addAction(IOSOtaHandler.ACTION_UPDATE_CATEGORY);
        filter.addAction(IOSOtaHandler.ACTION_UPDATE_LABEL);

        if (BuildUtil.isCustomerBuild()
                && !"off".equalsIgnoreCase(DeviceInfoUtil.getSystemProperty("ro.build.iosos_securityicon", "on"))) {
            IOSOtaHandler.ENABLE_OTA_2_FEATURE = true;
            filter.addAction(IOSOtaHandler.OTA2_UPDATE_HIDE);
            filter.addAction(IOSOtaHandler.OTA2_UPDATE_HIDE_ENTRY);
            Log.v(IOSOtaHandler.TAG, "ota2 is open");
        }

        ContextHelper.registerReceiver(sContext, mModel, filter);

        UserManagerCompat.getInstance(sContext).enableAndResetCache();
        new ConfigMonitor(sContext).register();


        if (!sContext.getResources().getBoolean(R.bool.notification_badging_enabled)) {
            mNotificationBadgingObserver = null;
        } else {
            // Register an observer to rebind the notification listener when badging is re-enabled.
            mNotificationBadgingObserver = new SettingsObserver.Secure(
                    sContext.getContentResolver()) {
                @Override
                public void onSettingChanged(boolean isNotificationBadgingEnabled) {
                    if (isNotificationBadgingEnabled) {
                        NotificationListener.requestRebind(new ComponentName(
                                sContext, NotificationListener.class));
                    }
                }
            };
            mNotificationBadgingObserver.register(NOTIFICATION_BADGING);
        }
    }

    /**
     * Call from Application.onTerminate(), which is not guaranteed to ever be called.
     */
    public void onTerminate() {
        sContext.unregisterReceiver(mModel);
        final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(sContext);
        launcherApps.removeOnAppsChangedCallback(mModel);
        PackageInstallerCompat.getInstance(sContext).onStop();

        if (mNotificationBadgingObserver != null) {
            mNotificationBadgingObserver.unregister();
        }
    }

    /**
     * Reloads the workspace items from the DB and re-binds the workspace. This should generally
     * not be called as DB updates are automatically followed by UI update
     */
    public void reloadWorkspace() {
        mModel.resetLoadedState(false, true);
        mModel.startLoaderFromBackground();
    }

    public void recreateWidgetPreviewDb() {
        mWidgetCache.recreateWidgetPreviewDb();
    }

    LauncherModel setLauncher(Launcher launcher) {
        LauncherSettingSubject.register(launcher);
        getLauncherProvider().setLauncherProviderChangeListener(launcher);
        mModel.initialize(launcher);
        mAccessibilityDelegate = ((launcher != null) && Utilities.ATLEAST_LOLLIPOP) ?
                new LauncherAccessibilityDelegate(launcher) : null;
        return mModel;
    }

    public LauncherAccessibilityDelegate getAccessibilityDelegate() {
        return mAccessibilityDelegate;
    }

    public IconCache getIconCache() {
        return mIconCache;
    }

    public ThemeManager getThemeManager() {
        return mThemeManager;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    public AppUsagesModel getAppUsagesModel() {
        return mAppUsagesModel;
    }

    static void setLauncherProvider(LauncherProvider provider) {
        sLauncherProvider = new WeakReference<LauncherProvider>(provider);
    }

    public static LauncherProvider getLauncherProvider() {
        return sLauncherProvider.get();
    }

    public static String getSharedPreferencesKey() {
        return LauncherFiles.SHARED_PREFERENCES_KEY;
    }

    public WidgetPreviewLoader getWidgetCache() {
        return mWidgetCache;
    }

    public void onWallpaperChanged() {
        mWallpaperChangedSinceLastCheck = true;
        mModel.createGaussWallpaperBitmap(true);
    }

    public boolean hasWallpaperChangedSinceLastCheck() {
        boolean result = mWallpaperChangedSinceLastCheck;
        mWallpaperChangedSinceLastCheck = false;
        return result;
    }

    public boolean applyNewTheme(String packageName) {
        if (TextUtils.isEmpty(packageName) || !PackageUtil.isAppInstalled(sContext, packageName)) {
            DebugUtil.debugTheme(TAG, "applyNewTheme: theme package not install:" + packageName);
            return false;
        }

        String curThemePkgName = Settings.getThemePackageName(sContext);
        DebugUtil.debugTheme(TAG, "curThemePkgName : " + curThemePkgName);
        DebugUtil.debugTheme(TAG, "newThemePkgName : " + packageName);
        if (!curThemePkgName.equals(packageName)) {
            LauncherAppState app = LauncherAppState.getInstanceNoCreate();
            getThemeManager().applyThemePkg(packageName, true/* changeWallpaper */);
            clearAppIconCache();
            clearWidgetPreviewCache();
            app.recreateWidgetPreviewDb();
            app.getIconCache().flush();
            app.getModel().forceReload();
            DebugUtil.debugTheme(TAG, "success applyNewTheme : " + packageName);
            return true;
        }

        return false;
    }

    void clearWidgetPreviewCache() {
        sContext.deleteDatabase(LauncherFiles.WIDGET_PREVIEWS_DB);
    }

    void clearAppIconCache() {
        sContext.deleteDatabase(LauncherFiles.APP_ICONS_DB);
    }

    public void initInvariantDeviceProfile() {
        mInvariantDeviceProfile = new InvariantDeviceProfile(sContext);
    }

    public InvariantDeviceProfile getInvariantDeviceProfile() {
        return mInvariantDeviceProfile;
    }

    public static boolean isDogfoodBuild() {
        return getInstance().mBuildInfo.isDogfoodBuild();
    }


    public static boolean isNewAppsCategotyEnable() {
        return mNewAppsCategory;
    }

    public static void setNewAppsCategotyEnable(boolean value) {
        mNewAppsCategory = value;
    }

    public static InvariantDeviceProfile getIDP(Context context) {
        return LauncherAppState.getInstance(context).getInvariantDeviceProfile();
    }
}

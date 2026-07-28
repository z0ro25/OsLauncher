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

import android.app.SearchManager;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Parcelable;
import android.os.Process;
import android.os.SystemClock;
import android.os.TransactionTooLargeException;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.MutableInt;
import android.util.Pair;
import android.util.Xml;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.Router;
import com.amz.ios.ioslite.common.config.ThemeConfig;
import com.amz.ios.ioslite.common.debug.DebugUtil;
import com.amz.ios.ioslite.common.debug.ExceptionHandler;
import com.amz.ios.ioslite.common.util.CommonUtilities;
import com.amz.ios.ioslite.common.util.FileUtil;
import com.amz.ios.ioslite.common.util.GaussBlurUtil;
import com.amz.ios.ioslite.common.util.PermissionUtil;
import com.amz.ios.launcher.assembly.LeftCustomContentUtil;
import com.amz.ios.launcher.compat.AppWidgetManagerCompat;
import com.amz.ios.launcher.compat.LauncherActivityInfoCompat;
import com.amz.ios.launcher.compat.LauncherAppsCompat;
import com.amz.ios.launcher.compat.PackageInstallerCompat;
import com.amz.ios.launcher.compat.PackageInstallerCompat.PackageInstallInfo;
import com.amz.ios.launcher.compat.UserHandleCompat;
import com.amz.ios.launcher.compat.UserManagerCompat;
import com.amz.ios.launcher.config.GestureEventModel;
import com.amz.ios.launcher.config.GridConfig;
import com.amz.ios.launcher.config.Settings;
import com.amz.ios.launcher.expdev.NewInstallAppHandler;
import com.amz.ios.launcher.folder.Folder;
import com.amz.ios.launcher.graphics.LauncherIcons;
import com.amz.ios.launcher.model.BgDataModel;
import com.amz.ios.launcher.model.WidgetsModel;
//import com.amz.ios.launcher.model.nano.LauncherDumpProto;
import com.amz.ios.launcher.ota.IOSOtaHandler;
import com.amz.ios.launcher.parser.AppCategoryProvider;
import com.amz.ios.launcher.parser.CategoryFolder;
import com.amz.ios.launcher.provider.AppTypeProvider;
import com.amz.ios.launcher.shortcuts.DeepShortcutManager;
import com.amz.ios.launcher.shortcuts.ShortcutInfoCompat;
import com.amz.ios.launcher.shortcuts.ShortcutKey;
import com.amz.ios.launcher.util.ComponentKey;
import com.amz.ios.launcher.util.CursorIconInfo;
import com.amz.ios.launcher.util.FlagOp;
import com.amz.ios.launcher.util.ItemInfoMatcher;
import com.amz.ios.launcher.util.LongArrayMap;
import com.amz.ios.launcher.util.ManagedProfileHeuristic;
import com.amz.ios.launcher.util.MultiHashMap;
import com.amz.ios.launcher.util.PackageManagerHelper;
import com.amz.ios.launcher.util.Provider;
import com.amz.ios.launcher.util.StringFilter;
import com.amz.ios.launcher.util.Thunk;
//import com.google.protobuf.nano.MessageNano;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Maintains in-memory state of the Launcher. It is expected that there should be only one
 * LauncherModel object held in a static. Also provide APIs for updating the database state
 * for the Launcher.
 */
public class LauncherModel extends BroadcastReceiver
        implements LauncherAppsCompat.OnAppsChangedCallbackCompat {

    static final String TAG = "Launcher.Model";

    static final boolean DEBUG_LOADERS = true;
    private static final boolean DEBUG_RECEIVER = false;
    private static final boolean REMOVE_UNRESTORED_ICONS = true;

    public static final int LOADER_FLAG_NONE = 0;
    public static final int LOADER_FLAG_CLEAR_WORKSPACE = 1 << 0;
    public static final int LOADER_FLAG_MIGRATE_SHORTCUTS = 1 << 1;

    private static final int ITEMS_CHUNK = 6; // batch size for the workspace icons
    private static final long INVALID_SCREEN_ID = -1L;

    @Thunk
    final boolean mAppsCanBeOnRemoveableStorage;

    @Thunk
    final LauncherAppState mApp;
    @Thunk
    final Object mLock = new Object();
    @Thunk
    DeferredHandler mHandler = new DeferredHandler();
    @Thunk
    LoaderTask mLoaderTask;
    @Thunk
    boolean mIsLoaderTaskRunning;
    @Thunk
    boolean mHasLoaderCompletedOnce;

    private static final String MIGRATE_AUTHORITY = "com.android.launcher2.settings";

    @Thunk
    public static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");

    static {
        sWorkerThread.start();
    }

    @Thunk
    static final Handler sWorker = new Handler(sWorkerThread.getLooper());

    // We start off with everything not loaded.  After that, we assume that
    // our monitoring of the package manager provides all updfates and we never
    // need to do a requery.  These are only ever touched from the loader thread.
    @Thunk
    public boolean mWorkspaceLoaded;
    @Thunk
    boolean mAllAppsLoaded;

    // When we are loading pages synchronously, we can't just post the binding of items on the side
    // pages as this delays the rotation process.  Instead, we wait for a callback from the first
    // draw (in Workspace) to initiate the binding of the remaining side pages.  Any time we start
    // a normal load, we also clear this set of Runnables.
    static final ArrayList<Runnable> mDeferredBindRunnables = new ArrayList<Runnable>();

    /**
     * Set of runnables to be called on the background thread after the workspace binding
     * is complete.
     */
    static final ArrayList<Runnable> mBindCompleteRunnables = new ArrayList<Runnable>();

    @Thunk
    WeakReference<Callbacks> mCallbacks;

    static final boolean ENABLE_GAUSS_WALLPAPER_BG = true;

    BitmapDrawable mGaussWallpaperDrawable;

    // < only access in worker thread >
    AllAppsList mBgAllAppsList;

    // Entire list of widgets.
    WidgetsModel mBgWidgetsModel;

    // The lock that must be acquired before referencing any static bg data structures.  Unlike
    // other locks, this one can generally be held long-term because we never expect any of these
    // static data structures to be referenced outside of the worker thread except on the first
    // load after configuration change.
    static final Object sBgLock = new Object();

    // sBgItemsIdMap maps *all* the ItemInfos (shortcuts, folders, and widgets) created by
    // LauncherModel to their ids
    static final LongArrayMap<ItemInfo> sBgItemsIdMap = new LongArrayMap<>();

    // sBgWorkspaceItems is passed to bindItems, which expects a list of all folders and shortcuts
    //       created by LauncherModel that are directly on the home screen (however, no widgets or
    //       shortcuts within folders).
    static final ArrayList<ItemInfo> sBgWorkspaceItems = new ArrayList<ItemInfo>();

    // sBgAppWidgets is all LauncherAppWidgetInfo created by LauncherModel. Passed to bindAppWidget()
    static final ArrayList<LauncherAppWidgetInfo> sBgAppWidgets = new ArrayList<LauncherAppWidgetInfo>();

    // sBgFolders is all FolderInfos created by LauncherModel. Passed to bindFolders()
    static final LongArrayMap<FolderInfo> sBgFolders = new LongArrayMap<>();

    // sBgWorkspaceScreens is the ordered set of workspace screens.
    static final ArrayList<Long> sBgWorkspaceScreens = new ArrayList<Long>();

    //Hidden App list
    public static final HashMap<Long, ItemInfo> sHdItemsIdMap = new HashMap<>();

    // sBgWidgetProviders is the set of widget providers including custom internal widgets
    public static HashMap<ComponentKey, LauncherAppWidgetProviderInfo> sBgWidgetProviders;

    // sPendingPackages is a set of packages which could be on sdcard and are not available yet
    static final HashMap<UserHandleCompat, HashSet<String>> sPendingPackages =
            new HashMap<UserHandleCompat, HashSet<String>>();

    static final ArrayList<ResolveInfo> sExportIOSShortcuts = new ArrayList<ResolveInfo>();

    static final Map<ShortcutKey, MutableInt> sPinnedShortcutCounts = new HashMap<>();

    private boolean mHasShortcutHostPermission;
    // Runnable to check if the shortcuts permission has changed.
    private final Runnable mShortcutPermissionCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (mWorkspaceLoaded) {
                boolean hasShortcutHostPermission =
                        DeepShortcutManager.getInstance(mApp.getContext()).hasHostPermission();
                if (hasShortcutHostPermission != mHasShortcutHostPermission) {
                    forceReload();
                }
            }
        }
    };

    static final BgDataModel sBgDataModel = new BgDataModel();

    // </ only access in worker thread >

    public boolean isModelLoaded() {
        synchronized (mLock) {
            return mWorkspaceLoaded && mLoaderTask == null;
        }
    }

    @Thunk
    IconCache mIconCache;

    @Thunk
    final LauncherAppsCompat mLauncherApps;
    @Thunk
    final UserManagerCompat mUserManager;

    //When installed new app on phone, registered in this.
    ArrayList<String> mNewFlagPackageNames = new ArrayList<>();

    public interface Callbacks {
        void onLoadStart(boolean hasLoad);

        boolean setLoadOnResume();

        int getCurrentWorkspaceScreen();

        void startBinding();

        void bindItems(ArrayList<ItemInfo> shortcuts, int start, int end,
                       boolean forceAnimateIcons);

        void bindScreens(ArrayList<Long> orderedScreenIds);

        void bindAddScreens(ArrayList<Long> orderedScreenIds);

        void bindFolders(LongArrayMap<FolderInfo> folders);

        void finishBindingItems();

        void bindAppWidget(LauncherAppWidgetInfo info);

        void bindAllApplications(ArrayList<AppInfo> apps);

        void bindAppsAdded(ArrayList<Long> newScreens,
                           ArrayList<ItemInfo> addNotAnimated,
                           ArrayList<ItemInfo> addAnimated,
                           ArrayList<AppInfo> addedApps);

        void bindAppsUpdated(ArrayList<AppInfo> apps);

        void bindShortcutsChanged(ArrayList<ShortcutInfo> updated,
                                  ArrayList<ShortcutInfo> removed, UserHandleCompat user);

        void bindWidgetsRestored(ArrayList<LauncherAppWidgetInfo> widgets);

        void bindRestoreItemsChange(HashSet<ItemInfo> updates);

        void bindComponentsRemoved(ArrayList<String> packageNames,
                                   ArrayList<AppInfo> appInfos, UserHandleCompat user, int reason);

        void bindAllPackages(WidgetsModel model);

        void addAndbindCategoryApps(Map<ShortcutInfo, FolderInfo> categoryMapMap);

        void bindSearchProviderChanged();

        void onPageBoundSynchronously(int page);

        void dumpLogsToLocalData();

        void applyGaussWallpaperBackground();

        void onLoadComplete();

        void removeShortcutById(long id);

        void bindDeepShortcutMap(MultiHashMap<ComponentKey, String> deepShortcutMap);

        void bindWorkspaceComponentsRemoved(ItemInfoMatcher matcher);

        void notifyWidgetProvidersChanged();
    }

    public interface ItemInfoFilter {
        boolean filterItem(ItemInfo parent, ItemInfo info, ComponentName cn);
    }

    LauncherModel(LauncherAppState app, IconCache iconCache, AppFilter appFilter) {
        Context context = app.getContext();

        mAppsCanBeOnRemoveableStorage = Environment.isExternalStorageRemovable();
        mApp = app;
        mBgAllAppsList = new AllAppsList(iconCache, appFilter);
        mBgWidgetsModel = new WidgetsModel(context, iconCache, appFilter);
        mIconCache = iconCache;

        mLauncherApps = LauncherAppsCompat.getInstance(context);
        mUserManager = UserManagerCompat.getInstance(context);
    }

    /**
     * Runs the specified runnable immediately if called from the main thread, otherwise it is
     * posted on the main thread handler.
     */
    @Thunk
    public void runOnMainThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            // If we are on the worker thread, post onto the main handler
            mHandler.post(r);
        } else {
            r.run();
        }
    }

    /**
     * Runs the specified runnable immediately if called from the worker thread, otherwise it is
     * posted on the worker thread handler.
     */
    @Thunk
    static void runOnWorkerThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            // If we are not on the worker thread, then post to the worker handler
            sWorker.post(r);
        }
    }

    public void setPackageState(final PackageInstallInfo installInfo) {
        Runnable updateRunnable = new Runnable() {

            @Override
            public void run() {
                synchronized (sBgLock) {
                    final HashSet<ItemInfo> updates = new HashSet<>();

                    if (installInfo.state == PackageInstallerCompat.STATUS_INSTALLED) {
                        // Ignore install success events as they are handled by Package add events.
                        return;
                    }

                    for (ItemInfo info : sBgItemsIdMap) {
                        if (info instanceof ShortcutInfo) {
                            ShortcutInfo si = (ShortcutInfo) info;
                            ComponentName cn = si.getTargetComponent();
                            if (si.isPromise() && (cn != null)
                                    && installInfo.packageName.equals(cn.getPackageName())) {
                                si.setInstallProgress(installInfo.progress);

                                if (installInfo.state == PackageInstallerCompat.STATUS_FAILED) {
                                    // Mark this info as broken.
                                    si.status &= ~ShortcutInfo.FLAG_INSTALL_SESSION_ACTIVE;
                                }
                                updates.add(si);
                            }
                        }
                    }

                    for (LauncherAppWidgetInfo widget : sBgAppWidgets) {
                        if (widget.providerName.getPackageName().equals(installInfo.packageName)) {
                            widget.installProgress = installInfo.progress;
                            updates.add(widget);
                        }
                    }

                    if (!updates.isEmpty()) {
                        // Push changes to the callback.
                        Runnable r = new Runnable() {
                            public void run() {
                                Callbacks callbacks = getCallback();
                                if (callbacks != null) {
                                    callbacks.bindRestoreItemsChange(updates);
                                }
                            }
                        };
                        mHandler.post(r);
                    }
                }
            }
        };
        runOnWorkerThread(updateRunnable);
    }

    /**
     * Updates the icons and label of all pending icons for the provided package name.
     */
    public void updateSessionDisplayInfo(final String packageName) {
        Runnable updateRunnable = new Runnable() {

            @Override
            public void run() {
                synchronized (sBgLock) {
                    final ArrayList<ShortcutInfo> updates = new ArrayList<>();
                    final UserHandleCompat user = UserHandleCompat.myUserHandle();

                    for (ItemInfo info : sBgItemsIdMap) {
                        if (info instanceof ShortcutInfo) {
                            ShortcutInfo si = (ShortcutInfo) info;
                            ComponentName cn = si.getTargetComponent();
                            if (si.isPromise() && (cn != null)
                                    && packageName.equals(cn.getPackageName())) {
                                if (si.hasStatusFlag(ShortcutInfo.FLAG_AUTOINTALL_ICON)) {
                                    // For auto install apps update the icon as well as label.
                                    mIconCache.getTitleAndIcon(si,
                                            si.promisedIntent, user,
                                            si.shouldUseLowResIcon());
                                } else {
                                    // Only update the icon for restored apps.
                                    si.updateIcon(mIconCache);
                                }
                                updates.add(si);
                            }
                        }
                    }

                    if (!updates.isEmpty()) {
                        // Push changes to the callback.
                        Runnable r = new Runnable() {
                            public void run() {
                                Callbacks callbacks = getCallback();
                                if (callbacks != null) {
                                    callbacks.bindShortcutsChanged(updates,
                                            new ArrayList<ShortcutInfo>(), user);
                                }
                            }
                        };
                        mHandler.post(r);
                    }
                }
            }
        };
        runOnWorkerThread(updateRunnable);
    }

    public void addAppsToAllApps(final Context ctx, final ArrayList<AppInfo> allAppsApps) {
        final Callbacks callbacks = getCallback();

        if (allAppsApps == null) {
            throw new RuntimeException("allAppsApps must not be null");
        }
        if (allAppsApps.isEmpty()) {
            return;
        }

        // Process the newly added applications and add them to the database first
        Runnable r = new Runnable() {
            public void run() {
                runOnMainThread(new Runnable() {
                    public void run() {
                        Callbacks cb = getCallback();
                        if (callbacks == cb && cb != null) {
                            Log.e("adadadada", "run: 1" );
                            callbacks.bindAppsAdded(null, null, null, allAppsApps);
                        }
                    }
                });
            }
        };
        runOnWorkerThread(r);
    }

    public void removeAppShortcut(final Context context, final long id) {
        final Callbacks callbacks = getCallback();

        ItemInfo info = new ItemInfo();
        info.id = id;
        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
        deleteItemFromDatabase(context, info);

        Runnable r = new Runnable() {
            public void run() {
                runOnMainThread(new Runnable() {
                    public void run() {
                        Callbacks cb = getCallback();
                        if (callbacks == cb && cb != null) {
                            callbacks.removeShortcutById(id);
                        }
                    }
                });
            }
        };
        runOnWorkerThread(r);
    }

    private static boolean findNextAvailableIconSpaceInScreen(ArrayList<ItemInfo> occupiedPos,
                                                              int[] xy, int spanX, int spanY) {
        LauncherAppState app = LauncherAppState.getInstance();
        InvariantDeviceProfile profile = app.getInvariantDeviceProfile();
        final int xCount = profile.numColumns;
        final int yCount = profile.numRows;
        boolean[][] occupied = new boolean[xCount][yCount];
        if (occupiedPos != null) {
            for (ItemInfo r : occupiedPos) {
                int right = r.cellX + r.spanX;
                int bottom = r.cellY + r.spanY;
                for (int x = r.cellX; 0 <= x && x < right && x < xCount; x++) {
                    for (int y = r.cellY; 0 <= y && y < bottom && y < yCount; y++) {
                        occupied[x][y] = true;
                    }
                }
            }
        }
        return Utilities.findVacantCell(xy, spanX, spanY, xCount, yCount, occupied);
    }

    /**
     * Find a position on the screen for the given size or adds a new screen.
     *
     * @return screenId and the coordinates for the item.
     */
    @Thunk
    Pair<Long, int[]> findSpaceForItem(
            Context context,
            ArrayList<Long> workspaceScreens,
            ArrayList<Long> addedWorkspaceScreensFinal,
            int spanX, int spanY, long preScreenId) {
        LongSparseArray<ArrayList<ItemInfo>> screenItems = new LongSparseArray<>();

        // Use sBgItemsIdMap as all the items are already loaded.
        assertWorkspaceLoaded();
        synchronized (sBgLock) {
            for (ItemInfo info : sBgItemsIdMap) {
                if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    ArrayList<ItemInfo> items = screenItems.get(info.screenId);
                    if (items == null) {
                        items = new ArrayList<>();
                        screenItems.put(info.screenId, items);
                    }
                    items.add(info);
                }
            }
        }

        // Find appropriate space for the item.
        long screenId = 0;
        int[] cordinates = new int[2];
        boolean found = false;


        if (preScreenId != -1) {
            screenId = preScreenId;
            found = findNextAvailableIconSpaceInScreen(
                    screenItems.get(screenId), cordinates, spanX, spanY);
        }

        int screenCount = workspaceScreens.size();
        if (!found) {
            // First check the preferred screen.
            // Bắt đầu từ page đầu tiên (index 0) để dồn/lấp kín các ô trống ở những
            // page đầu, thay vì bỏ qua 2 page đầu khiến app dồn hết ra page sau.
            int preferredScreenIndex = 0;
            if (preferredScreenIndex < screenCount) {
                screenId = workspaceScreens.get(preferredScreenIndex);
                found = findNextAvailableIconSpaceInScreen(
                        screenItems.get(screenId), cordinates, spanX, spanY);
            }
        }


        if (!found) {
            // Search on any of the screens starting from the first screen.
            for (int screen = 0; screen < screenCount; screen++) {
                screenId = workspaceScreens.get(screen);
                if (findNextAvailableIconSpaceInScreen(
                        screenItems.get(screenId), cordinates, spanX, spanY)) {
                    // We found a space for it
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            // Still no position found. Add a new screen to the end.
            screenId = LauncherAppState.getLauncherProvider().generateNewScreenId();

            // Save the screen id for binding in the workspace
            workspaceScreens.add(screenId);
            addedWorkspaceScreensFinal.add(screenId);
            // If we still can't find an empty space, then God help us all!!!
            if (!findNextAvailableIconSpaceInScreen(
                    screenItems.get(screenId), cordinates, spanX, spanY)) {
                throw new RuntimeException("Can't find space to add the item");
            }
        }
        return Pair.create(screenId, cordinates);
    }


    public void addAndBindAddedWorkspaceItems(final Context context,
                                              final ArrayList<? extends ItemInfo> workspaceApps, boolean category, boolean prompt) {
        if (workspaceApps.size() == 0) {
            return;
        }

        if (category && LauncherAppState.isNewAppsCategotyEnable()) {
            categoryAndBindAddedWorkspaceItems(context, workspaceApps, prompt);
        } else {
            addAndBindAddedWorkspaceItems(context, workspaceApps);

        }
    }


    public void categoryAndBindAddedWorkspaceItems(final Context context,
                                                   final ArrayList<? extends ItemInfo> workspaceApps, final boolean prompt) {
        DebugUtil.debugCategory(TAG, "categoryAndBindAddedWorkspaceItems");
        final Callbacks callbacks = getCallback();
        if (workspaceApps.isEmpty()) {
            return;
        }

        final AppCategoryProvider categoryProvider = AppCategoryProvider.getInstance();
        ShortcutInfo shortcutInfo = null;
        final HashMap<ShortcutInfo, FolderInfo> categoryMap = new HashMap<ShortcutInfo, FolderInfo>(3);

        for (ItemInfo item : workspaceApps) {
            if (item instanceof AppInfo) {
                shortcutInfo = ((AppInfo) item).makeShortcut();
            } else if (item instanceof ShortcutInfo) {
                shortcutInfo = (ShortcutInfo) item;
            }

            if (shortcutInfo == null) {
                continue;
            }

            ComponentName cn = shortcutInfo.getTargetComponent();
            final int categoryType = categoryProvider.getCatTypeForComp(cn);
            addToCategoryFolder(context, categoryType, shortcutInfo, categoryMap);
        }

        runOnMainThread(new Runnable() {
            public void run() {
                Callbacks cb = getCallback();
                if (callbacks == cb && cb != null && prompt) {
                    callbacks.addAndbindCategoryApps(categoryMap);
                }
            }
        });
    }

    /**
     * Adds the provided items to the workspace.
     */
    public void addAndBindAddedWorkspaceItems(final Context context,
                                              final ArrayList<? extends ItemInfo> workspaceApps) {
        final Callbacks callbacks = getCallback();
        if (workspaceApps.isEmpty()) {
            return;
        }
        // Process the newly added applications and add them to the database first
        Runnable r = new Runnable() {
            public void run() {
                final ArrayList<ItemInfo> addedShortcutsFinal = new ArrayList<ItemInfo>();
                final ArrayList<Long> addedWorkspaceScreensFinal = new ArrayList<Long>();

                // Get the list of workspace screens.  We need to append to this list and
                // can not use sBgWorkspaceScreens because loadWorkspace() may not have been
                // called.
                ArrayList<Long> workspaceScreens = loadWorkspaceScreensDb(context);
                synchronized (sBgLock) {
                    for (ItemInfo item : workspaceApps) {
                        if (item instanceof ShortcutInfo || item instanceof AppInfo) {
                            // Short-circuit this logic if the icon exists somewhere on the workspace
                            if (shortcutExists(context, item.getIntent(), item.user)) {
                                continue;
                            }
                        }

                        // Find appropriate space for the item.
                        Pair<Long, int[]> coords = findSpaceForItem(context,
                                workspaceScreens, addedWorkspaceScreensFinal,
                                1, 1, -1);
                        long screenId = coords.first;
                        int[] cordinates = coords.second;

                        ItemInfo itemInfo;
                        if (item instanceof ShortcutInfo || item instanceof FolderInfo) {
                            itemInfo = item;
                        } else if (item instanceof AppInfo) {
                            itemInfo = ((AppInfo) item).makeShortcut();
                        } else {
                            throw new RuntimeException("Unexpected info type");
                        }

                        // Add the shortcut to the db
                        //If NO_ID, Set container id with CONTAINER_DESKTOP
                        //if HIDDEN APP, set container id with CONTAINER_HIDDENFOLDER
                        if (itemInfo.container == -1) {
                            itemInfo.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                        }
                        addItemToDatabase(context, itemInfo,
                                itemInfo.container,
                                screenId, cordinates[0], cordinates[1]);
                        // Save the ShortcutInfo for binding in the workspace
                        addedShortcutsFinal.add(itemInfo);

                        // sometimes icon overlap, optimize modify
                        sBgItemsIdMap.put(itemInfo.id, itemInfo);
                    }
                }

                // Update the workspace screens
                updateWorkspaceScreenOrder(context, workspaceScreens);

                if (!addedShortcutsFinal.isEmpty()) {
                    runOnMainThread(new Runnable() {
                        public void run() {
                            Callbacks cb = getCallback();
                            if (callbacks == cb && cb != null) {
                                final ArrayList<ItemInfo> addAnimated = new ArrayList<ItemInfo>();
                                final ArrayList<ItemInfo> addNotAnimated = new ArrayList<ItemInfo>();
                                if (!addedShortcutsFinal.isEmpty()) {
                                    ItemInfo info = addedShortcutsFinal.get(addedShortcutsFinal.size() - 1);
                                    long lastScreenId = info.screenId;
                                    for (ItemInfo i : addedShortcutsFinal) {
                                        if (i.screenId == lastScreenId) {
                                            addAnimated.add(i);
                                        } else {
                                            addNotAnimated.add(i);
                                        }
                                    }
                                }
                                Log.e("adadadada", "run: 2" );
                                callbacks.bindAppsAdded(addedWorkspaceScreensFinal,
                                        addNotAnimated, addAnimated, null);
                            }
                        }
                    });
                }
            }
        };
        runOnWorkerThread(r);
    }

    @Thunk
    void addToCategoryFolder(Context context, int categotyType, final ShortcutInfo info, Map<ShortcutInfo, FolderInfo> categoryMap) {
        synchronized (sBgLock) {
            boolean addToExitFolder = false;
            FolderInfo folderInfo = null;
            for (ItemInfo item : sBgItemsIdMap) {
                if (item instanceof FolderInfo) {
                    final FolderInfo fi = (FolderInfo) item;
                    if (fi.folderCategoryType == categotyType) {
                        runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                fi.add(info);
                            }
                        });
                        folderInfo = fi;
                        addToExitFolder = true;
                    }
                }
            }

            if (!addToExitFolder) {
                folderInfo = new FolderInfo();
                folderInfo.title = CategoryFolder.getFolerNameForType(context, categotyType);
                folderInfo.add(info);
                folderInfo.folderCategoryType = categotyType;
                ArrayList<ItemInfo> itemList = new ArrayList<ItemInfo>(1);
                itemList.add(folderInfo);
                addAndBindAddedWorkspaceItems(context, itemList);
            }

            categoryMap.put(info, folderInfo);
            DebugUtil.debugCategory(TAG, info.title + " category to " + folderInfo.title);
        }
    }


    public void addWorkspaceItemFromFolder(final Context context,
                                           final ItemInfo itemInfo, final long preScreenId) {
        final Callbacks callbacks = getCallback();

        // Process the newly added applications and add them to the database first
        Runnable r = new Runnable() {
            public void run() {
                final ArrayList<Long> addedWorkspaceScreensFinal = new ArrayList<Long>();

                // Get the list of workspace screens.  We need to append to this list and
                // can not use sBgWorkspaceScreens because loadWorkspace() may not have been
                // called.
                ArrayList<Long> workspaceScreens = loadWorkspaceScreensDb(context);
                synchronized (sBgLock) {
                    // Find appropriate space for the item.
                    Pair<Long, int[]> coords = findSpaceForItem(context,
                            workspaceScreens, addedWorkspaceScreensFinal,
                            1, 1, preScreenId);
                    long screenId = coords.first;
                    int[] cordinates = coords.second;


                    itemInfo.screenId = screenId;
                    itemInfo.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                    itemInfo.cellX = cordinates[0];
                    itemInfo.cellY = cordinates[1];

                    updateItemInDatabase(context, itemInfo);
                }

                // Update the workspace screens
                updateWorkspaceScreenOrder(context, workspaceScreens);


                runOnMainThread(new Runnable() {
                    public void run() {
                        Callbacks cb = getCallback();
                        if (callbacks == cb && cb != null) {
                            final ArrayList<ItemInfo> addAnimated = new ArrayList<ItemInfo>();
                            final ArrayList<ItemInfo> addNotAnimated = new ArrayList<ItemInfo>();
                            addAnimated.add(itemInfo);
                            Log.e("adadadada", "run: 3" );
                            callbacks.bindAppsAdded(addedWorkspaceScreensFinal,
                                    addNotAnimated, addAnimated, null);
                        }
                    }
                });

            }
        };
        runOnWorkerThread(r);
    }

    private void unbindItemInfosAndClearQueuedBindRunnables() {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            throw new RuntimeException("Expected unbindLauncherItemInfos() to be called from the " +
                    "main thread");
        }

        // Clear any deferred bind runnables
        synchronized (mDeferredBindRunnables) {
            mDeferredBindRunnables.clear();
        }

        // Remove any queued UI runnables
        mHandler.cancelAll();
        // Unbind all the workspace items
        unbindWorkspaceItemsOnMainThread();
    }

    /**
     * Unbinds all the sBgWorkspaceItems and sBgAppWidgets on the main thread
     */
    void unbindWorkspaceItemsOnMainThread() {
        // Ensure that we don't use the same workspace items data structure on the main thread
        // by making a copy of workspace items first.
        final ArrayList<ItemInfo> tmpItems = new ArrayList<ItemInfo>();
        synchronized (sBgLock) {
            tmpItems.addAll(sBgWorkspaceItems);
            tmpItems.addAll(sBgAppWidgets);
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for (ItemInfo item : tmpItems) {
                    item.unbind();
                }
            }
        };
        runOnMainThread(r);
    }

    /**
     * Adds an item to the DB if it was not created previously, or move it to a new
     * <container, screen, cellX, cellY>
     */
    public static void addOrMoveItemInDatabase(Context context, ItemInfo item, long container,
                                               long screenId, int cellX, int cellY) {
        if (item.container == ItemInfo.NO_ID) {
            // From all apps
            addItemToDatabase(context, item, container, screenId, cellX, cellY);
        } else {
            // From somewhere else
            moveItemInDatabase(context, item, container, screenId, cellX, cellY);
        }
    }

    static void checkItemInfoLocked(
        final long itemId, final ItemInfo item, StackTraceElement[] stackTrace) {
        ItemInfo modelItem = sBgItemsIdMap.get(itemId);
        if (modelItem != null && item != modelItem) {
            // check all the data is consistent
            if (modelItem instanceof ShortcutInfo && item instanceof ShortcutInfo) {
                ShortcutInfo modelShortcut = (ShortcutInfo) modelItem;
                ShortcutInfo shortcut = (ShortcutInfo) item;
                if (modelShortcut.title.toString().equals(shortcut.title.toString()) &&
                        modelShortcut.intent.filterEquals(shortcut.intent) &&
                        modelShortcut.id == shortcut.id &&
                        modelShortcut.itemType == shortcut.itemType &&
                        modelShortcut.container == shortcut.container &&
                        modelShortcut.screenId == shortcut.screenId &&
                        modelShortcut.cellX == shortcut.cellX &&
                        modelShortcut.cellY == shortcut.cellY &&
                        modelShortcut.spanX == shortcut.spanX &&
                        modelShortcut.spanY == shortcut.spanY &&
                        ((modelShortcut.dropPos == null && shortcut.dropPos == null) ||
                                (modelShortcut.dropPos != null &&
                                        shortcut.dropPos != null &&
                                        modelShortcut.dropPos[0] == shortcut.dropPos[0] &&
                                        modelShortcut.dropPos[1] == shortcut.dropPos[1]))) {
                    // For all intents and purposes, this is the same object
                    return;
                }
            }

            // the modelItem needs to match up perfectly with item if our model is
            // to be consistent with the database-- for now, just require
            // modelItem == item or the equality check above
            String msg = "item: " + ((item != null) ? item.toString() : "null") +
                    "modelItem: " +
                    ((modelItem != null) ? modelItem.toString() : "null") +
                    "Error: ItemInfo passed to checkItemInfo doesn't match original";
//            RuntimeException e = new RuntimeException(msg);
//            if (stackTrace != null) {
//                e.setStackTrace(stackTrace);
//            }
//            throw e;
        }
    }

    static void checkItemInfo(final ItemInfo item) {
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        final long itemId = item.id;
        Runnable r = new Runnable() {
            public void run() {
                synchronized (sBgLock) {
                    checkItemInfoLocked(itemId, item, stackTrace);
                }
            }
        };
        runOnWorkerThread(r);
    }

    public static void updateItemInDatabaseHelper(Context context, final ContentValues values, ItemInfo item, final String callingFunction) {
        final ItemInfo itemInfo = item;
        final long itemId = itemInfo.id;
        final Uri uri = LauncherSettings.Favorites.getContentUri(itemId);
        final ContentResolver cr = context.getContentResolver();

        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        Runnable r = new Runnable() {
            public void run() {
                int result = cr.update(uri, values, null, null);
                if (result > 0)
                    updateItemArrays(itemInfo, itemId, stackTrace);
            }
        };
        runOnWorkerThread(r);
    }

    static void updateItemsInDatabaseHelper(Context context, final ArrayList<ContentValues> valuesList,
                                            final ArrayList<ItemInfo> items, final String callingFunction) {
        final ContentResolver cr = context.getContentResolver();

        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        Runnable r = new Runnable() {
            public void run() {
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                int count = items.size();
                for (int i = 0; i < count; i++) {
                    ItemInfo item = items.get(i);
                    final long itemId = item.id;
                    final Uri uri = LauncherSettings.Favorites.getContentUri(itemId);
                    ContentValues values = valuesList.get(i);

                    ops.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
                    updateItemArrays(item, itemId, stackTrace);

                }
                try {
                    ContentProviderResult[] result = cr.applyBatch(LauncherProvider.AUTHORITY, ops);
                    Log.d(TAG, "updateItemsInDatabaseHelper = result " + result.length);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        runOnWorkerThread(r);
    }

    static void updateItemArraysForHiddenFolder(ItemInfo item, long itemId, StackTraceElement[] stackTrace) {
        // Lock on mBgLock *after* the db operation
        synchronized (sBgLock) {
            checkItemInfoLocked(itemId, item, stackTrace);

            if (item.container != LauncherSettings.Favorites.CONTAINER_DESKTOP &&
                    item.container != LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                // Item is in a folder, make sure this folder exists
                if (!sBgFolders.containsKey(item.container)) {
                    // An items container is being set to a that of an item which is not in the list of Folders.
                    String msg = "item: " + item + " container being set to: " + item.container + ", not in the list of folders";
                    Log.e(TAG, msg);
                }
            }

            // Items are added/removed from the corresponding FolderInfo elsewhere, such
            // as in Workspace.onDrop. Here, we just add/remove them from the list of items
            // that are on the desktop, as appropriate

            if (item != null &&
                    (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP || item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT)) {
                switch (item.itemType) {
                    case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                    case LauncherSettings.Favorites.ITEM_TYPE_VIRTUAL_APP:
                    case LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT:
                    case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                        if (sBgItemsIdMap.get(itemId) == null) {
                            sBgItemsIdMap.put(item.id, item);
                        }

                        if (!sBgWorkspaceItems.contains(item)) {
                            sBgWorkspaceItems.add(item);
                        }
                        break;
                    default:
                        break;
                }
            } else {

                if (item != null) {
                    sBgWorkspaceItems.remove(item);
                }
            }
        }
    }

    static void updateItemArrays(ItemInfo item, long itemId, StackTraceElement[] stackTrace) {
        // Lock on mBgLock *after* the db operation
        synchronized (sBgLock) {
            checkItemInfoLocked(itemId, item, stackTrace);

            if (item.container != LauncherSettings.Favorites.CONTAINER_DESKTOP &&
                    item.container != LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                // Item is in a folder, make sure this folder exists
                if (!sBgFolders.containsKey(item.container)) {
                    // An items container is being set to a that of an item which is not in the list of Folders.
                    String msg = "item: " + item + " container being set to: " + item.container + ", not in the list of folders";
                    Log.e(TAG, msg);
                }
            }

            // Items are added/removed from the corresponding FolderInfo elsewhere, such
            // as in Workspace.onDrop. Here, we just add/remove them from the list of items
            // that are on the desktop, as appropriate
            ItemInfo modelItem = sBgItemsIdMap.get(itemId);

            if (modelItem != null &&
                    (modelItem.container == LauncherSettings.Favorites.CONTAINER_DESKTOP || modelItem.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT)) {
                switch (modelItem.itemType) {
                    case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                    case LauncherSettings.Favorites.ITEM_TYPE_VIRTUAL_APP:
                    case LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT:
                    case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                        if (!sBgWorkspaceItems.contains(modelItem)) {
                            sBgWorkspaceItems.add(modelItem);
                        }
                        break;
                    default:
                        break;
                }
            } else {
                if (modelItem != null) {
                    sBgWorkspaceItems.remove(modelItem);
                }
            }
        }
    }

    /**
     * Move an item in the DB to a new <container, screen, cellX, cellY>
     */
    public static void moveItemInDatabase(Context context,
                                          final ItemInfo item,
                                          final long container,
                                          final long screenId,
                                          final int cellX,
                                          final int cellY) {
        item.container = container;
        item.cellX = cellX;
        item.cellY = cellY;

        // We store hotseat items in canonical form which is this orientation invariant position in the hotseat
        if (context instanceof Launcher && screenId < 0 && container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            item.screenId = ((Launcher) context).getHotseat().getOrderInHotseat(cellX, cellY);
        } else {
            item.screenId = screenId;
        }

        final ContentValues values = new ContentValues();
        values.put(LauncherSettings.Favorites.CONTAINER, item.container);
        values.put(LauncherSettings.Favorites.CELLX, item.cellX);
        values.put(LauncherSettings.Favorites.CELLY, item.cellY);
        values.put(LauncherSettings.Favorites.RANK, item.rank);
        values.put(LauncherSettings.Favorites.SCREEN, item.screenId);

        updateItemInDatabaseHelper(context, values, item, "moveItemInDatabase");
    }

    /**
     * Move items in the DB to a new <container, screen, cellX, cellY>. We assume that the
     * cellX, cellY have already been updated on the ItemInfos.
     */
    public static void moveItemsInDatabase(Context context,
                                           final ArrayList<ItemInfo> items,
                                           final long container,
                                           final int screen) {

        ArrayList<ContentValues> contentValues = new ArrayList<ContentValues>();
        int count = items.size();

        for (int i = 0; i < count; i++) {
            ItemInfo item = items.get(i);
            item.container = container;

            // We store hotseat items in canonical form which is this orientation invariant position in the hotseat
            if (context instanceof Launcher && screen < 0 &&
                    container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                item.screenId = ((Launcher) context).getHotseat().getOrderInHotseat(item.cellX, item.cellY);
            } else {
                item.screenId = screen;
            }

            final ContentValues values = new ContentValues();
            values.put(LauncherSettings.Favorites.CONTAINER, item.container);
            values.put(LauncherSettings.Favorites.CELLX, item.cellX);
            values.put(LauncherSettings.Favorites.CELLY, item.cellY);
            values.put(LauncherSettings.Favorites.RANK, item.rank);
            values.put(LauncherSettings.Favorites.SCREEN, item.screenId);

            contentValues.add(values);
        }
        updateItemsInDatabaseHelper(context, contentValues, items, "moveItemInDatabase");
    }

    /**
     * Move and/or resize item in the DB to a new <container, screen, cellX, cellY, spanX, spanY>
     */
    static void modifyItemInDatabase(Context context, final ItemInfo item, final long container,
                                     final long screenId, final int cellX, final int cellY, final int spanX, final int spanY) {
        item.container = container;
        item.cellX = cellX;
        item.cellY = cellY;
        item.spanX = spanX;
        item.spanY = spanY;

        // We store hotseat items in canonical form which is this orientation invariant position
        // in the hotseat
        if (context instanceof Launcher && screenId < 0 &&
                container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            item.screenId = ((Launcher) context).getHotseat().getOrderInHotseat(cellX, cellY);
        } else {
            item.screenId = screenId;
        }

        final ContentValues values = new ContentValues();
        values.put(LauncherSettings.Favorites.CONTAINER, item.container);
        values.put(LauncherSettings.Favorites.CELLX, item.cellX);
        values.put(LauncherSettings.Favorites.CELLY, item.cellY);
        values.put(LauncherSettings.Favorites.RANK, item.rank);
        values.put(LauncherSettings.Favorites.SPANX, item.spanX);
        values.put(LauncherSettings.Favorites.SPANY, item.spanY);
        values.put(LauncherSettings.Favorites.SCREEN, item.screenId);

        updateItemInDatabaseHelper(context, values, item, "modifyItemInDatabase");
    }

    /**
     * Update an item to the database in a specified container.
     */
    public static void updateItemInDatabase(Context context, final ItemInfo item) {
        final ContentValues values = new ContentValues();
        item.onAddToDatabase(context, values);
        updateItemInDatabaseHelper(context, values, item, "updateItemInDatabase");
    }

    private void assertWorkspaceLoaded() {
        if (LauncherAppState.isDogfoodBuild()) {
            synchronized (mLock) {
                if (!mHasLoaderCompletedOnce ||
                        (mLoaderTask != null && mLoaderTask.mIsLoadingAndBindingWorkspace)) {
                    throw new RuntimeException("Trying to add shortcut while loader is running");
                }
            }
        }
    }

    /**
     * Returns true if the shortcuts already exists on the workspace. This must be called after
     * the workspace has been loaded. We identify a shortcut by its intent.
     */
    @Thunk
    boolean shortcutExists(Context context, Intent intent, UserHandleCompat user) {
        assertWorkspaceLoaded();
        final String intentWithPkg, intentWithoutPkg;
        if (intent.getComponent() != null) {
            // If component is not null, an intent with null package will produce
            // the same result and should also be a match.
            String packageName = intent.getComponent().getPackageName();
            if (intent.getPackage() != null) {
                intentWithPkg = intent.toUri(0);
                intentWithoutPkg = new Intent(intent).setPackage(null).toUri(0);
            } else {
                intentWithPkg = new Intent(intent).setPackage(packageName).toUri(0);
                intentWithoutPkg = intent.toUri(0);
            }
        } else {
            intentWithPkg = intent.toUri(0);
            intentWithoutPkg = intent.toUri(0);
        }

        synchronized (sBgLock) {
            for (ItemInfo item : sBgItemsIdMap) {
                if (item instanceof ShortcutInfo) {
                    ShortcutInfo info = (ShortcutInfo) item;
                    Intent targetIntent = info.promisedIntent == null
                            ? info.intent : info.promisedIntent;
                    if (targetIntent != null && info.user.equals(user)) {
                        String s = targetIntent.toUri(0);
                        if (intentWithPkg.equals(s) || intentWithoutPkg.equals(s)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }


    /**
     * Find a folder in the db, creating the FolderInfo if necessary, and adding it to folderList.
     */
    FolderInfo getFolderById(Context context, LongArrayMap<FolderInfo> folderList, long id) {
        final ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI, null,
                "_id=? and (itemType=? or itemType=?)",
                new String[]{String.valueOf(id),
                        String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_FOLDER)}, null);

        try {
            if (c.moveToFirst()) {
                final int itemTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
                final int titleIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);
                final int containerIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
                final int screenIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
                final int cellXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
                final int cellYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
                final int optionsIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.OPTIONS);

                FolderInfo folderInfo = null;
                switch (c.getInt(itemTypeIndex)) {
                    case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                        folderInfo = findOrMakeFolder(folderList, id);
                        break;
                }

                // Do not trim the folder label, as is was set by the user.
                folderInfo.title = c.getString(titleIndex);
                folderInfo.id = id;
                folderInfo.container = c.getInt(containerIndex);
                folderInfo.screenId = c.getInt(screenIndex);
                folderInfo.cellX = c.getInt(cellXIndex);
                folderInfo.cellY = c.getInt(cellYIndex);
                folderInfo.options = c.getInt(optionsIndex);

                return folderInfo;
            }
        } finally {
            c.close();
        }

        return null;
    }

    /**
     * Add an item to the database in a specified container. Sets the container, screen, cellX and
     * cellY fields of the item. Also assigns an ID to the item.
     */
    public static void addItemToDatabase(final Context context, final ItemInfo item, final long container,
                                         final long screenId, final int cellX, final int cellY) {
        item.container = container;
        item.cellX = cellX;
        item.cellY = cellY;

        // We store hotseat items in canonical form which is this orientation invariant position in the hotseat
        if (context instanceof Launcher && screenId < 0 &&
                container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            item.screenId = ((Launcher) context).getHotseat().getOrderInHotseat(cellX, cellY);
        } else {
            item.screenId = screenId;
        }

        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();
        item.onAddToDatabase(context, values);

        item.id = LauncherAppState.getLauncherProvider().generateNewItemId();
        values.put(LauncherSettings.Favorites._ID, item.id);

        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        Runnable r = new Runnable() {
            public void run() {
                Uri rowURI = cr.insert(LauncherSettings.Favorites.CONTENT_URI, values);

                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    checkItemInfoLocked(item.id, item, stackTrace);
                    sBgItemsIdMap.put(item.id, item);
                    switch (item.itemType) {
                        case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                            sBgFolders.put(item.id, (FolderInfo) item);
                            sBgWorkspaceItems.add(item);
                            break;
                        case LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT: {
                            // Increment the count for the given shortcut
                            ShortcutKey pinnedShortcut = ShortcutKey.fromItemInfo(item);
                            MutableInt count = sPinnedShortcutCounts.get(pinnedShortcut);
                            if (count == null) {
                                count = new MutableInt(1);
                                sPinnedShortcutCounts.put(pinnedShortcut, count);
                            } else {
                                count.value++;
                            }

                            // Since this is a new item, pin the shortcut in the system server.
                            if (count.value == 1) {
                                DeepShortcutManager.getInstance(context).pinShortcut(pinnedShortcut);
                            }
                            // Fall through
                        }
                        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                        case LauncherSettings.Favorites.ITEM_TYPE_VIRTUAL_APP:
                            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP ||
                                    item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                                sBgWorkspaceItems.add(item);
                            } else {
                                if (!sBgFolders.containsKey(item.container)) {
                                    // Adding an item to a folder that doesn't exist.
                                    String msg = "adding item: " + item + " to a folder that " +
                                            " doesn't exist";
                                    Log.e(TAG, msg);
                                }
                            }
                            break;
                        case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                            sBgAppWidgets.add((LauncherAppWidgetInfo) item);
                            break;
                    }
                }
            }
        };
        runOnWorkerThread(r);
    }

    /**
     * Creates a new unique child id, for a given cell span across all layouts.
     */
    static int getCellLayoutChildId(
            long container, long screen, int localCellX, int localCellY, int spanX, int spanY) {
        return (((int) container & 0xFF) << 24)
                | ((int) screen & 0xFF) << 16 | (localCellX & 0xFF) << 8 | (localCellY & 0xFF);
    }

    private static ArrayList<ItemInfo> getItemsByPackageName(
            final String pn, final UserHandleCompat user) {
        ItemInfoFilter filter = new ItemInfoFilter() {
            @Override
            public boolean filterItem(ItemInfo parent, ItemInfo info, ComponentName cn) {
                return cn.getPackageName().equals(pn) && info.user.equals(user);
            }
        };
        return filterItemInfos(sBgItemsIdMap, filter);
    }

    /**
     * Removes all the items from the database corresponding to the specified package.
     */
    static void deletePackageFromDatabase(Context context, final String pn, final UserHandleCompat user) {
        Log.d(TAG, "deletePackageFromDatabase(Context context, final String pn, final UserHandleCompat user)");

        deleteItemsFromDatabase(context, getItemsByPackageName(pn, user));
    }

    /**
     * Removes the specified item from the database
     *
     * @param context
     * @param item
     */
    public static void deleteItemFromDatabase(Context context, final ItemInfo item) {
        Log.d(TAG, "deleteItemFromDatabase(Context context, final ItemInfo item)");

        ArrayList<ItemInfo> items = new ArrayList<ItemInfo>();
        items.add(item);
        deleteItemsFromDatabase(context, items);
    }

    /**
     * Removes the specified items from the database
     *
     * @param context
     * @param items
     */
    static void deleteItemsFromDatabase(final Context context, final ArrayList<? extends ItemInfo> items) {
        Log.d(TAG, "deleteItemsFromDatabase(final Context context, final ArrayList<? extends ItemInfo> items)");

        final ContentResolver cr = context.getContentResolver();
        Runnable r = new Runnable() {
            public void run() {
                for (ItemInfo item : items) {
                    final Uri uri = LauncherSettings.Favorites.getContentUri(item.id);
                    cr.delete(uri, null, null);

                    // Lock on mBgLock *after* the db operation
                    synchronized (sBgLock) {
                        switch (item.itemType) {
                            case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                                sBgFolders.remove(item.id);
                                for (ItemInfo info : sBgItemsIdMap) {
                                    if (info.container == item.id) {
                                        // We are deleting a folder which still contains items that think they are contained by that folder.
                                        String msg = "deleting a folder (" + item + ") which still " + "contains items (" + info + ")";
                                        Log.e(TAG, msg);
                                    }
                                }
                                sBgWorkspaceItems.remove(item);
                                break;
                            case LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT: {
                                // Decrement pinned shortcut count
                                ShortcutKey pinnedShortcut = ShortcutKey.fromItemInfo(item);
                                MutableInt count = sPinnedShortcutCounts.get(pinnedShortcut);
                                if ((count == null || --count.value == 0)
                                        && !InstallShortcutReceiver.getPendingShortcuts(context)
                                        .contains(pinnedShortcut)) {
                                    DeepShortcutManager.getInstance(context).unpinShortcut(pinnedShortcut);
                                }
                                // Fall through.
                            }
                            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                            case LauncherSettings.Favorites.ITEM_TYPE_VIRTUAL_APP:
                                sBgWorkspaceItems.remove(item);
                                break;
                            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                                sBgAppWidgets.remove(item);
                                break;
                        }
                        sBgItemsIdMap.remove(item.id);
                    }
                }
            }
        };
        runOnWorkerThread(r);
    }

    /**
     * Removes all the items from the database matching {@param matcher}.
     */
    public void deleteItemsFromDatabase(ItemInfoMatcher matcher) {
        Log.d(TAG, "deleteItemsFromDatabase(ItemInfoMatcher matcher)");

        ArrayList<ItemInfo> items = new ArrayList<ItemInfo>();
        Iterable<? extends ItemInfo> matchItems = matcher.filterItemInfos(sBgItemsIdMap);
        for (ItemInfo item : matchItems) {
            items.add(item);
        }
        deleteItemsFromDatabase(mApp.getContext(), items);
    }

    /**
     * Update the order of the workspace screens in the database. The array list contains
     * a list of screen ids in the order that they should appear.
     */
    public void updateWorkspaceScreenOrder(Context context, final ArrayList<Long> screens) {
        final ArrayList<Long> screensCopy = new ArrayList<Long>(screens);
        final ContentResolver cr = context.getContentResolver();
        final Uri uri = LauncherSettings.WorkspaceScreens.CONTENT_URI;

        // Remove any negative screen ids -- these aren't persisted
        Iterator<Long> iter = screensCopy.iterator();
        while (iter.hasNext()) {
            long id = iter.next();
            if (id < 0) {
                iter.remove();
            }
        }

        Runnable r = new Runnable() {
            @Override
            public void run() {
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                // Clear the table
                ops.add(ContentProviderOperation.newDelete(uri).build());
                int count = screensCopy.size();
                for (int i = 0; i < count; i++) {
                    ContentValues v = new ContentValues();
                    long screenId = screensCopy.get(i);
                    v.put(LauncherSettings.WorkspaceScreens._ID, screenId);
                    v.put(LauncherSettings.WorkspaceScreens.SCREEN_RANK, i);
                    ops.add(ContentProviderOperation.newInsert(uri).withValues(v).build());
                }

                try {
                    cr.applyBatch(LauncherProvider.AUTHORITY, ops);
                } catch (Exception ex) {
                    ExceptionHandler.addDumpLog(TAG, "updateWorkspaceScreenOrder error", ex);
                }

                synchronized (sBgLock) {
                    sBgWorkspaceScreens.clear();
                    sBgWorkspaceScreens.addAll(screensCopy);
                }
            }
        };
        runOnWorkerThread(r);
    }

    /**
     * Remove the contents of the specified folder from the database
     */
    public static void deleteFolderContentsFromDatabase(Context context, final FolderInfo info) {
        Log.d(TAG, "deleteFolderContentsFromDatabase");
        final ContentResolver cr = context.getContentResolver();

        Runnable r = new Runnable() {
            public void run() {
                cr.delete(LauncherSettings.Favorites.getContentUri(info.id), null, null);
                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    sBgItemsIdMap.remove(info.id);
                    sBgFolders.remove(info.id);
                    sBgWorkspaceItems.remove(info);
                }

                cr.delete(LauncherSettings.Favorites.CONTENT_URI, LauncherSettings.Favorites.CONTAINER + "=" + info.id, null);
                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    for (ItemInfo childInfo : info.contents) {
                        sBgItemsIdMap.remove(childInfo.id);
                    }
                }
            }
        };
        runOnWorkerThread(r);
    }

    /**
     * Set this as the current Launcher activity object for the loader.
     */
    public void initialize(Callbacks callbacks) {
        synchronized (mLock) {
            // Disconnect any of the callbacks and drawables associated with ItemInfos on the
            // workspace to prevent leaking Launcher activities on orientation change.
            unbindItemInfosAndClearQueuedBindRunnables();
            mCallbacks = new WeakReference<Callbacks>(callbacks);
        }
    }

    @Override
    public void onPackageChanged(String packageName, UserHandleCompat user) {
        int op = PackageUpdatedTask.OP_UPDATE;
        enqueuePackageUpdated(new PackageUpdatedTask(op, new String[]{packageName},
                user));
    }

    @Override
    public void onPackageRemoved(String packageName, UserHandleCompat user) {
        int op = PackageUpdatedTask.OP_REMOVE;
        enqueuePackageUpdated(new PackageUpdatedTask(op, new String[]{packageName},
                user));
    }

    @Override
    public void onPackageAdded(String packageName, UserHandleCompat user) {
        int op = PackageUpdatedTask.OP_ADD;
        enqueuePackageUpdated(new PackageUpdatedTask(op, new String[]{packageName},
                user));
    }

    @Override
    public void onPackagesAvailable(String[] packageNames, UserHandleCompat user,
                                    boolean replacing) {
        if (!replacing) {
            enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_ADD, packageNames,
                    user));
            if (mAppsCanBeOnRemoveableStorage) {
                // Only rebind if we support removable storage. It catches the
                // case where
                // apps on the external sd card need to be reloaded
                startLoaderFromBackground();
            }
        } else {
            // If we are replacing then just update the packages in the list
            enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_UPDATE,
                    packageNames, user));
        }
    }

    @Override
    public void onPackagesUnavailable(String[] packageNames, UserHandleCompat user,
                                      boolean replacing) {
        if (!replacing) {
            enqueuePackageUpdated(new PackageUpdatedTask(
                    PackageUpdatedTask.OP_UNAVAILABLE, packageNames,
                    user));
        }
    }

    @Override
    public void onPackagesSuspended(String[] packageNames, UserHandleCompat user) {
        enqueuePackageUpdated(new PackageUpdatedTask(
                PackageUpdatedTask.OP_SUSPEND, packageNames,
                user));
    }

    @Override
    public void onPackagesUnsuspended(String[] packageNames, UserHandleCompat user) {
        enqueuePackageUpdated(new PackageUpdatedTask(
                PackageUpdatedTask.OP_UNSUSPEND, packageNames,
                user));
    }

    @Override
    public void onShortcutsChanged(String packageName, List<ShortcutInfoCompat> shortcuts, UserHandleCompat user) {
        enqueueModelUpdateTask(new ShortcutsChangedTask(packageName, shortcuts, user, true));
    }

    private int mDate;

    /**
     * Call from the handler for ACTION_PACKAGE_ADDED, ACTION_PACKAGE_REMOVED and
     * ACTION_PACKAGE_CHANGED.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG_RECEIVER) Log.d(TAG, "onReceive intent=" + intent);

        final String action = intent.getAction();
        if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            // If we have changed locale we need to clear out the labels in all apps/workspace.
            forceReload();
        } else if (SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED.equals(action)) {
            Callbacks callbacks = getCallback();
            if (callbacks != null) {
                callbacks.bindSearchProviderChanged();
            }
        } else if (LauncherAppsCompat.ACTION_MANAGED_PROFILE_ADDED.equals(action)
                || LauncherAppsCompat.ACTION_MANAGED_PROFILE_REMOVED.equals(action)) {
            UserManagerCompat.getInstance(context).enableAndResetCache();
            forceReload();
        } else if (Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_TIME_TICK.equals(action)) {
            int date = Calendar.getInstance().get(Calendar.DATE);
            ComponentName clockComp = AppTypeProvider.getAppTypeProvider().getClockComp();
            if (clockComp != null) {
//                onPackageChanged(clockComp.getPackageName(), UserHandleCompat.myUserHandle());
            }

            if (Intent.ACTION_TIME_TICK.equals(action)) {
                if (mDate != date) {
                    mDate = date;
                } else {
                    return;
                }
            }

            ComponentName calendarComp = AppTypeProvider.getAppTypeProvider().getCalendarComp();
            if (calendarComp != null) {
                onPackageChanged(calendarComp.getPackageName(), UserHandleCompat.myUserHandle());
            }
        } else if (LauncherAppsCompat.ACTION_MANAGED_PROFILE_AVAILABLE.equals(action) ||
                LauncherAppsCompat.ACTION_MANAGED_PROFILE_UNAVAILABLE.equals(action)) {
            UserHandleCompat user = UserHandleCompat.fromIntent(intent);
            forceReload();
            if (user != null) {
                enqueuePackageUpdated(new PackageUpdatedTask(
                        PackageUpdatedTask.OP_USER_AVAILABILITY_CHANGE,
                        new String[0], user));
            }
        } else if (IOSOtaHandler.OTA2_UPDATE_HIDE.equals(action)) {
            Log.i(IOSOtaHandler.TAG, "hide list changed");
            IOSOtaHandler.updateExternalHideList(mApp.getContext());

            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String pkgAdded = bundle.getString("packageadd");
                String pkgHided = bundle.getString("packagehide");

                Log.i(IOSOtaHandler.TAG, "added pkg : " + pkgAdded);
                Log.i(IOSOtaHandler.TAG, "hided pkg : " + pkgHided);

                if (pkgAdded != null && !pkgAdded.isEmpty()) {
                    enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_ADD,
                            new String[]{pkgAdded}, UserHandleCompat.myUserHandle()));
                }

                if (pkgHided != null && !pkgHided.isEmpty()) {
                    enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_REMOVE,
                            new String[]{pkgHided}, UserHandleCompat.myUserHandle()));
                }
            }
        } else if (IOSOtaHandler.OTA2_UPDATE_HIDE_ENTRY.equals(action)) {
            Log.i(IOSOtaHandler.TAG, "hide entry map changed");
            IOSOtaHandler.updateExternalHideEntryMap(mApp.getContext());

            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String pkgAdded = bundle.getString("packageadd");
                String pkgHided = bundle.getString("packagehide");

                Log.i(IOSOtaHandler.TAG, "added pkg entry: " + pkgAdded);
                Log.i(IOSOtaHandler.TAG, "hided pkg entry: " + pkgHided);

                if (pkgAdded != null && !pkgAdded.isEmpty()) {
                    enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_ADD_ENTRY,
                            new String[]{pkgAdded}, UserHandleCompat.myUserHandle()));
                }

                if (pkgHided != null && !pkgHided.isEmpty()) {
                    enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_REMOVE_ENTRY,
                            new String[]{pkgHided}, UserHandleCompat.myUserHandle()));
                }
            }
        }
        else if (IOSOtaHandler.ACTION_RELOAD_ICON.equals(action)){
            String componentName = intent.getStringExtra(IOSOtaHandler.EXTRA_COMPONENT_NAME);
            int iconId = intent.getIntExtra(IOSOtaHandler.EXTRA_ICON_ID,0);
            String iconPath = intent.getStringExtra(IOSOtaHandler.EXTRA_ICON_PATH);
            if (TextUtils.isEmpty(componentName)) return;
            ComponentName cmpName = ComponentName.unflattenFromString(componentName);
            if (cmpName == null) return;
            enqueuePackageUpdated(
                    new PackageUpdatedTask(
                            new String[]{cmpName.getPackageName()},
                            componentName,
                            UserHandleCompat.myUserHandle(),
                            iconId,
                            iconPath,
                            TextUtils.isEmpty(iconPath) ? 1 : 2
                    )
            );
        }
        else if (IOSOtaHandler.ACTION_UPDATE_LABEL.equals(action)){
            String componentName = intent.getStringExtra(IOSOtaHandler.EXTRA_COMPONENT_NAME);
            String appLabel = intent.getStringExtra(IOSOtaHandler.EXTRA_APP_LABEL);
            if (TextUtils.isEmpty(componentName) || TextUtils.isEmpty(appLabel)) return;
            ComponentName cmpName = ComponentName.unflattenFromString(componentName);
            if (cmpName == null) return;
            enqueuePackageUpdated(
                    new PackageUpdatedTask(
                            new String[]{cmpName.getPackageName()},
                            componentName,
                            UserHandleCompat.myUserHandle(),
                            appLabel
                    )
            );
        }
        else if (IOSOtaHandler.ACTION_UPDATE_CATEGORY.equals(action)){
            String componentName = intent.getStringExtra(IOSOtaHandler.EXTRA_COMPONENT_NAME);
            int appCategory = intent.getIntExtra(IOSOtaHandler.EXTRA_APP_CATEGORY,0);
            if (TextUtils.isEmpty(componentName) || appCategory < 0) return;
            ComponentName cmpName = ComponentName.unflattenFromString(componentName);
            if (cmpName == null) return;
            enqueuePackageUpdated(
                    new PackageUpdatedTask(
                            new String[]{cmpName.getPackageName()},
                            componentName,
                            UserHandleCompat.myUserHandle(),
                            appCategory
                    )
            );
        }
    }

    public void forceReload() {
        resetLoadedState(true, true);

        // Do this here because if the launcher activity is running it will be restarted.
        // If it's not running startLoaderFromBackground will merely tell it that it needs
        // to reload.
        startLoaderFromBackground();
    }

    public void resetLoadedState(boolean resetAllAppsLoaded, boolean resetWorkspaceLoaded) {
        synchronized (mLock) {
            // Stop any existing loaders first, so they don't set mAllAppsLoaded or
            // mWorkspaceLoaded to true later
            stopLoaderLocked();
            if (resetAllAppsLoaded) mAllAppsLoaded = false;
            if (resetWorkspaceLoaded) mWorkspaceLoaded = false;
        }
    }

    /**
     * When the launcher is in the background, it's possible for it to miss paired
     * configuration changes.  So whenever we trigger the loader from the background
     * tell the launcher that it needs to re-run the loader when it comes back instead
     * of doing it now.
     */
    public void startLoaderFromBackground() {
        boolean runLoader = false;
        Callbacks callbacks = getCallback();
        if (callbacks != null) {
            // Only actually run the loader if they're not paused.
            if (!callbacks.setLoadOnResume()) {
                runLoader = true;
            }
        }
        if (runLoader) {
            startLoader(PagedView.INVALID_RESTORE_PAGE);
        }
    }

    /**
     * If there is already a loader task running, tell it to stop.
     */
    private void stopLoaderLocked() {
        LoaderTask oldTask = mLoaderTask;
        if (oldTask != null) {
            oldTask.stopLocked();
        }
    }

    public boolean isCurrentCallbacks(Callbacks callbacks) {
        return (mCallbacks != null && mCallbacks.get() == callbacks);
    }

    public void startLoader(int synchronousBindPage) {
        startLoader(synchronousBindPage, LOADER_FLAG_NONE);
    }

    public void startLoader(int synchronousBindPage, int loadFlags) {
        // Enable queue before starting loader. It will get disabled in Launcher#finishBindingItems
        InstallShortcutReceiver.enableInstallQueue();
        UninstallShortcutReceiver.enableUninstallQueue();
        synchronized (mLock) {
            // Clear any deferred bind-runnables from the synchronized load process
            // We must do this before any loading/binding is scheduled below.
            synchronized (mDeferredBindRunnables) {
                mDeferredBindRunnables.clear();
            }

            // Don't bother to start the thread if we know it's not going to do anything
            if (mCallbacks != null && mCallbacks.get() != null) {
                // If there is already one running, tell it to stop.
                stopLoaderLocked();
                mLoaderTask = new LoaderTask(mApp.getContext(), loadFlags);
                if (synchronousBindPage != PagedView.INVALID_RESTORE_PAGE
                        && mAllAppsLoaded && mWorkspaceLoaded && !mIsLoaderTaskRunning) {
                    mLoaderTask.runBindSynchronousPage(synchronousBindPage);
                } else {
                    sWorkerThread.setPriority(Thread.NORM_PRIORITY);
                    sWorker.post(mLoaderTask);
                }
            }
        }
    }

    void bindRemainingSynchronousPages() {
        // Post the remaining side pages to be loaded
        if (!mDeferredBindRunnables.isEmpty()) {
            Runnable[] deferredBindRunnables = null;
            synchronized (mDeferredBindRunnables) {
                deferredBindRunnables = mDeferredBindRunnables.toArray(
                        new Runnable[mDeferredBindRunnables.size()]);
                mDeferredBindRunnables.clear();
            }
            for (final Runnable r : deferredBindRunnables) {
                mHandler.post(r);
            }
        }
    }

    public void stopLoader() {
        synchronized (mLock) {
            if (mLoaderTask != null) {
                mLoaderTask.stopLocked();
            }
        }
    }

    /**
     * Loads the workspace screen ids in an ordered list.
     */
    public static ArrayList<Long> loadWorkspaceScreensDb(Context context) {
        ArrayList<Long> emptyScreens = loadEmptyWorkspaceScreensDb(context);

        final ContentResolver contentResolver = context.getContentResolver();
        final Uri screensUri = LauncherSettings.WorkspaceScreens.CONTENT_URI;

        // Get screens ordered by rank.
        final Cursor sc = contentResolver.query(screensUri, null, null, null, LauncherSettings.WorkspaceScreens.SCREEN_RANK);
        ArrayList<Long> screenIds = new ArrayList<Long>();
        try {
            final int idIndex = sc.getColumnIndexOrThrow(LauncherSettings.WorkspaceScreens._ID);
            while (sc.moveToNext()) {
                try {
                    long screenId = sc.getLong(idIndex);
                    if (!isEmptyScreen(screenId, emptyScreens))
                        screenIds.add(screenId);
                } catch (Exception e) {
                    Launcher.addDumpLog(TAG, "Desktop items loading interrupted" + " - invalid screens: " + e, true);
                }
            }
        } finally {
            sc.close();
        }
        return screenIds;
    }

    public static boolean isEmptyScreen(long screenId, ArrayList<Long> emptyScreens) {
        Iterator<Long> emptyIterator = emptyScreens.iterator();
        while (emptyIterator.hasNext()) {
            long emptyId = emptyIterator.next();
            if (screenId == emptyId)
                return true;
        }

        return false;
    }
    /**
     * Loads the empty workspace screen ids in an ordered list.
     * Maybe user pretend new pages on workspace.
     * If add no pages, that pages will be stored in database
     */
    public static ArrayList<Long> loadEmptyWorkspaceScreensDb(Context context) {
        final ContentResolver contentResolver = context.getContentResolver();
        final Uri screensUri = LauncherSettings.EmptyWorkspaceScreens.CONTENT_URI;

        // Get screens ordered by rank.
        final Cursor sc = contentResolver.query(screensUri, null, null, null, LauncherSettings.EmptyWorkspaceScreens.SCREEN_RANK);
        ArrayList<Long> emptyScreenIds = new ArrayList<Long>();
        try {
            final int idIndex = sc.getColumnIndexOrThrow(LauncherSettings.EmptyWorkspaceScreens._ID);
            while (sc.moveToNext()) {
                try {
                    emptyScreenIds.add(sc.getLong(idIndex));
                } catch (Exception e) {
                    Launcher.addDumpLog(TAG, "Desktop empty items loading interrupted" + " - invalid screens: " + e, true);
                }
            }
        } finally {
            sc.close();
        }
        return emptyScreenIds;
    }

    /**
     * Update the order of the workspace screens in the database. The array list contains
     * a list of screen ids in the order that they should appear.
     */
    public static void updateEmptyWorkspaceScreen(Context context, final ArrayList<Long> screens) {
        final ArrayList<Long> screensCopy = new ArrayList<Long>(screens);
        final ContentResolver cr = context.getContentResolver();
        final Uri uri = LauncherSettings.EmptyWorkspaceScreens.CONTENT_URI;

        // Remove any negative screen ids -- these aren't persisted
        Iterator<Long> iter = screensCopy.iterator();
        while (iter.hasNext()) {
            long id = iter.next();
            if (id < 0) {
                iter.remove();
            }
        }

        Runnable r = new Runnable() {
            @Override
            public void run() {
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                // Clear the table
                ops.add(ContentProviderOperation.newDelete(uri).build());
                int count = screensCopy.size();
                for (int i = 0; i < count; i++) {
                    ContentValues v = new ContentValues();
                    long screenId = screensCopy.get(i);
                    v.put(LauncherSettings.EmptyWorkspaceScreens._ID, screenId);
                    v.put(LauncherSettings.EmptyWorkspaceScreens.SCREEN_RANK, i);
                    ops.add(ContentProviderOperation.newInsert(uri).withValues(v).build());
                }

                try {
                    cr.applyBatch(LauncherProvider.AUTHORITY, ops);
                } catch (Exception ex) {
                    ExceptionHandler.addDumpLog(TAG, "updateWorkspaceScreenOrder error", ex);
                }

                synchronized (sBgLock) {
//                    sBgWorkspaceScreens.clear();
//                    sBgWorkspaceScreens.addAll(screensCopy);
                }
            }
        };
        runOnWorkerThread(r);
    }

    /**
     * Remove the contents of the empty workspace table from the database
     */
    public static void cleanEmptyWorkspacesFromDatabase(Context context) {
        final ContentResolver cr = context.getContentResolver();
        final Uri uri = LauncherSettings.EmptyWorkspaceScreens.CONTENT_URI;

        Runnable r = new Runnable() {
            public void run() {
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                ops.add(ContentProviderOperation.newDelete(uri).build());

                try {
                    cr.applyBatch(LauncherProvider.AUTHORITY, ops);
                } catch (Exception ex) {
                    ExceptionHandler.addDumpLog(TAG, "cleanEmptyWorkspacesFromDatabase error", ex);
                }
            }
        };
        runOnWorkerThread(r);
    }


    public boolean isAllAppsLoaded() {
        return mAllAppsLoaded;
    }

    /**
     * Runnable for the thread that loads the contents of the launcher:
     * - workspace icons
     * - widgets
     * - all apps icons
     */
    private class LoaderTask implements Runnable {
        private Context mContext;
        @Thunk
        boolean mIsLoadingAndBindingWorkspace;
        private boolean mStopped;
        @Thunk
        boolean mLoadAndBindStepFinished;
        private int mFlags;
        private boolean mInitWorkspace;

        LoaderTask(Context context, int flags) {
            mContext = context;
            mFlags = flags;
        }

        public void loadAndBindWorkspace() {
            mIsLoadingAndBindingWorkspace = true;

            // Load the workspace
            if (DEBUG_LOADERS) {
                Log.d(TAG, "loadAndBindWorkspace mWorkspaceLoaded=" + mWorkspaceLoaded);
            }

            if (!mWorkspaceLoaded) {
                loadWorkspace();
                synchronized (LoaderTask.this) {
                    if (mStopped) {
                        return;
                    }
                    mWorkspaceLoaded = true;
                }
            }

            // Bind the workspace
            bindWorkspace(-1);
        }

        private void waitForIdle() {
            // Wait until the either we're stopped or the other threads are done.
            // This way we don't start loading all apps until the workspace has settled
            // down.
            synchronized (LoaderTask.this) {
                final long workspaceWaitTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

                mHandler.postIdle(new Runnable() {
                    public void run() {
                        synchronized (LoaderTask.this) {
                            mLoadAndBindStepFinished = true;
                            DebugUtil.debugLaunch(TAG, "done with previous binding step");
                            LoaderTask.this.notify();
                        }
                    }
                });

                while (!mStopped && !mLoadAndBindStepFinished) {
                    try {
                        // Just in case mFlushingWorkerThread changes but we aren't woken up,
                        // wait no longer than 1sec at a time
                        this.wait(1000);
                    } catch (InterruptedException ex) {
                        // Ignore
                    }
                }
                DebugUtil.debugLaunch(TAG, "waited "
                        + (SystemClock.uptimeMillis() - workspaceWaitTime)
                        + "ms for previous step to finish binding");
            }
        }

        void runBindSynchronousPage(int synchronousBindPage) {
            if (synchronousBindPage == PagedView.INVALID_RESTORE_PAGE) {
                // Ensure that we have a valid page index to load synchronously
                throw new RuntimeException("Should not call runBindSynchronousPage() without " +
                        "valid page index");
            }
            if (!mAllAppsLoaded || !mWorkspaceLoaded) {
                // Ensure that we don't try and bind a specified page when the pages have not been
                // loaded already (we should load everything asynchronously in that case)
                throw new RuntimeException("Expecting AllApps and Workspace to be loaded");
            }
            synchronized (mLock) {
                if (mIsLoaderTaskRunning) {
                    // Ensure that we are never running the background loading at this point since
                    // we also touch the background collections
                    throw new RuntimeException("Error! Background loading is already running");
                }
            }

            // XXX: Throw an exception if we are already loading (since we touch the worker thread
            //      data structures, we can't allow any other thread to touch that data, but because
            //      this call is synchronous, we can get away with not locking).

            // The LauncherModel is static in the LauncherAppState and mHandler may have queued
            // operations from the previous activity.  We need to ensure that all queued operations
            // are executed before any synchronous binding work is done.
            mHandler.flush();

            // Divide the set of loaded items into those that we are binding synchronously, and
            // everything else that is to be bound normally (asynchronously).
            bindWorkspace(synchronousBindPage);
            // XXX: For now, continue posting the binding of AllApps as there are other issues that
            //      arise from that.
            onlyBindAllApps();

            bindDeepShortcuts();
        }

        public void run() {
            synchronized (mLock) {
                if (mStopped) {
                    return;
                }
                mIsLoaderTaskRunning = true;
            }

            DebugUtil.debugLaunch(TAG, "step 0: loadStart");
            loadStart();
            // Optimize for end-user experience: if the Launcher is up and // running with the
            // All Apps interface in the foreground, load All Apps first. Otherwise, load the
            // workspace first (default).
            keep_running:
            {
                DebugUtil.debugLaunch(TAG, "step 1: loading workspace");
                loadAndBindWorkspace();

                if (mStopped) {
                    break keep_running;
                }

                waitForIdle();

                // second step
                DebugUtil.debugLaunch(TAG, "step 2: loading all apps");
                loadAndBindAllApps();
                verifyApplication();
                waitForIdle();
                loadDeepShortcuts();
                bindDeepShortcuts();

                //third step
                createGaussWallpaperBitmap(false);

                DebugUtil.debugLaunch(TAG, "step 3: loadComplete");
                loadComplete();

                mApp.getAppUsagesModel().loadUsages();
                GestureEventModel.reset();
                GestureEventModel.getInstance(mContext);

            }
            // Clear out this reference, otherwise we end up hoding it until all of the
            // callback runnables are done.
            mContext = null;

            synchronized (mLock) {
                // If we are still the last one to be scheduled, remove ourselves.
                if (mLoaderTask == this) {
                    mLoaderTask = null;
                }
                mIsLoaderTaskRunning = false;
                mHasLoaderCompletedOnce = true;
                mInitWorkspace = false;
            }
        }

        public void stopLocked() {
            synchronized (LoaderTask.this) {
                mStopped = true;
                this.notify();
            }
        }

        /**
         * Gets the callbacks object.  If we've been stopped, or if the launcher object
         * has somehow been garbage collected, return null instead.  Pass in the Callbacks
         * object that was around when the deferred message was scheduled, and if there's
         * a new Callbacks object around then also return null.  This will save us from
         * calling onto it with data that will be ignored.
         */
        Callbacks tryGetCallbacks(Callbacks oldCallbacks) {
            synchronized (mLock) {
                if (mStopped) {
                    return null;
                }

                if (mCallbacks == null) {
                    return null;
                }

                final Callbacks callbacks = mCallbacks.get();
                if (callbacks != oldCallbacks) {
                    return null;
                }
                if (callbacks == null) {
                    Log.w(TAG, "no mCallbacks");
                    return null;
                }

                return callbacks;
            }
        }

        // check & update map of what's occupied; used to discard overlapping/invalid items
        private boolean checkItemPlacement(LongArrayMap<ItemInfo[][]> occupied, ItemInfo item,
                                           ArrayList<Long> workspaceScreens) {
            LauncherAppState app = LauncherAppState.getInstance();
            InvariantDeviceProfile profile = app.getInvariantDeviceProfile();
            final int countX = profile.numColumns;
            final int countY = profile.numRows;

            long containerIndex = item.screenId;
            if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                // Return early if we detect that an item is under the hotseat button
                if (mCallbacks == null) {
                    Log.e(TAG, "Error loading shortcut into hotseat " + item
                            + " into position (" + item.screenId + ":" + item.cellX + ","
                            + item.cellY + ") occupied by all apps");
                    return false;
                }

                final ItemInfo[][] hotseatItems =
                        occupied.get((long) LauncherSettings.Favorites.CONTAINER_HOTSEAT);

                if (item.screenId >= profile.numHotseatIcons) {
                    Log.e(TAG, "Error loading shortcut " + item
                            + " into hotseat position " + item.screenId
                            + ", position out of bounds: (0 to " + (profile.numHotseatIcons - 1)
                            + ")");
                    return false;
                }

                if (hotseatItems != null) {
                    if (hotseatItems[(int) item.screenId][0] != null) {
                        Log.e(TAG, "Error loading shortcut into hotseat " + item
                                + " into position (" + item.screenId + ":" + item.cellX + ","
                                + item.cellY + ") occupied by "
                                + occupied.get(LauncherSettings.Favorites.CONTAINER_HOTSEAT)
                                [(int) item.screenId][0]);
                        return false;
                    } else {
                        hotseatItems[(int) item.screenId][0] = item;
                        return true;
                    }
                } else {
                    final ItemInfo[][] items = new ItemInfo[(int) profile.numHotseatIcons][1];
                    items[(int) item.screenId][0] = item;
                    occupied.put((long) LauncherSettings.Favorites.CONTAINER_HOTSEAT, items);
                    return true;
                }
            } else if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                if (!workspaceScreens.contains((Long) item.screenId)) {
                    // The item has an invalid screen id.
                    return false;
                }
            } else {
                // Skip further checking if it is not the hotseat or workspace container
                return true;
            }

            if (!occupied.containsKey(item.screenId)) {
                ItemInfo[][] items = new ItemInfo[countX + 1][countY + 1];
                occupied.put(item.screenId, items);
            }

            final ItemInfo[][] screens = occupied.get(item.screenId);
            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP &&
                    item.cellX < 0 || item.cellY < 0 ||
                    (item.cellX + item.spanX) > countX ||
                    (item.cellY + item.spanY) > countY) {
                Log.e(TAG, "Error loading shortcut " + item
                        + " into cell (" + containerIndex + "-" + item.screenId + ":"
                        + item.cellX + "," + item.cellY
                        + ") out of screen bounds ( " + countX + "x" + countY + ")");
                return false;
            }

            // Check if any workspace icons overlap with each other
            for (int x = item.cellX; x < (item.cellX + item.spanX); x++) {
                for (int y = item.cellY; y < (item.cellY + item.spanY); y++) {
                    if (screens[x][y] != null) {
                        Log.e(TAG, "Error loading shortcut " + item
                                + " into cell (" + containerIndex + "-" + item.screenId + ":"
                                + x + "," + y
                                + ") occupied by "
                                + screens[x][y]);
                        return false;
                    }
                }
            }
            for (int x = item.cellX; x < (item.cellX + item.spanX); x++) {
                for (int y = item.cellY; y < (item.cellY + item.spanY); y++) {
                    screens[x][y] = item;
                }
            }

            return true;
        }

        /**
         * Clears all the sBg data structures
         */
        private void clearSBgDataStructures() {
            synchronized (sBgLock) {
                sBgWorkspaceItems.clear();
                sBgAppWidgets.clear();
                sBgFolders.clear();
                sBgItemsIdMap.clear();
                sHdItemsIdMap.clear();
                sBgWorkspaceScreens.clear();
                sPinnedShortcutCounts.clear();
            }
        }

        /**
         * Load Applications, Widgets, Shortcuts from Database
         */

        public void loadWorkspace() {
            DebugUtil.debugLaunch(TAG, "loadWorkspace: start");
            Log.d(TAG, "loadWorkspace: start");

            final Context context = mContext;
            final ContentResolver contentResolver = context.getContentResolver();
            final PackageManager manager = context.getPackageManager();
            final boolean isSafeMode = manager.isSafeMode();
            final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
            final DeepShortcutManager shortcutManager = DeepShortcutManager.getInstance(context);

            /**
             final boolean isSdCardReady = context.registerReceiver(null,
             new IntentFilter(StartupReceiver.SYSTEM_READY)) != null;
             */
            final boolean isSdCardReady = true;

            LauncherAppState app = LauncherAppState.getInstance();
            InvariantDeviceProfile profile = app.getInvariantDeviceProfile();
            int countX = profile.numColumns;
            int countY = profile.numRows;

            AppTypeProvider.getAppTypeProvider().loadAppTypeFromDb();

            if ((mFlags & LOADER_FLAG_CLEAR_WORKSPACE) != 0) {
                DebugUtil.debugLaunch(TAG, "loadWorkspace: resetting launcher database");
                LauncherAppState.getLauncherProvider().deleteDatabase();
            }

            // Make sure the default workspace is loaded
            DebugUtil.debugLaunch(TAG, "loadWorkspace: loading default favorites");
            mInitWorkspace = LauncherAppState.getLauncherProvider().loadDefaultFavoritesIfNecessary();
            DebugUtil.debugLaunch(TAG, "loadWorkspace: load database");
            synchronized (sBgLock) {
                clearSBgDataStructures();
                final HashMap<String, Integer> installingPkgs = PackageInstallerCompat
                        .getInstance(mContext).updateAndGetActiveSessionCache();
                mNewFlagPackageNames = NewInstallAppHandler.loadNewFlagApps(mContext);
                sBgWorkspaceScreens.addAll(loadWorkspaceScreensDb(mContext));

                final ArrayList<ItemInfo> itemsToRelocate = new ArrayList<ItemInfo>();
                final ArrayList<Long> itemsToRemove = new ArrayList<Long>();
                final ArrayList<Long> restoredRows = new ArrayList<Long>();
                final Uri contentUri = LauncherSettings.Favorites.CONTENT_URI;
                if (DEBUG_LOADERS) Log.d(TAG, "loading model from " + contentUri);
                final Cursor cursor = contentResolver.query(contentUri, null, null, null, null);

                // +1 for the hotseat (it can be larger than the workspace)
                // Load workspace in reverse order to ensure that latest items are loaded first (and
                // before any earlier duplicates)
                final LongArrayMap<ItemInfo[][]> occupied = new LongArrayMap<>();

                Map<ShortcutKey, ShortcutInfoCompat> shortcutKeyToPinnedShortcuts = new HashMap<>();

                try {
                    final int idIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
                    final int intentIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);
                    final int titleIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);
                    final int containerIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
                    final int itemTypeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
                    final int folderCatTypeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.FOLDER_CATEGORY_TYPE);
                    final int appWidgetIdIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.APPWIDGET_ID);
                    final int appWidgetProviderIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.APPWIDGET_PROVIDER);
                    final int screenIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
                    final int cellXIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
                    final int cellYIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
                    final int spanXIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANX);
                    final int spanYIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANY);
                    final int rankIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.RANK);
                    final int restoredIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.RESTORED);
                    final int profileIdIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.PROFILE_ID);
                    final int optionsIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.OPTIONS);
                    final int callNumIndex = cursor.getColumnIndexOrThrow("called_num");
                    final int lastCalledTimeIndex = cursor.getColumnIndexOrThrow("last_called_time");
                    final int urlIndex = cursor.getColumnIndexOrThrow("url");
                    final int hiddenIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.IS_HIDDEN);

                    final CursorIconInfo cursorIconInfo = new CursorIconInfo(cursor);

                    final LongSparseArray<UserHandleCompat> allUsers = new LongSparseArray<>();
                    final LongSparseArray<Boolean> quietMode = new LongSparseArray<>();
                    final LongSparseArray<Boolean> unlockedUsers = new LongSparseArray<>();
                    for (UserHandleCompat user : mUserManager.getUserProfiles()) {
                        long serialNo = mUserManager.getSerialNumberForUser(user);
                        allUsers.put(serialNo, user);
                        quietMode.put(serialNo, mUserManager.isQuietModeEnabled(user));

                        boolean userUnlocked = mUserManager.isUserUnlocked(user);

                        // We can only query for shortcuts when the user is unlocked.
                        if (userUnlocked) {
                            List<ShortcutInfoCompat> pinnedShortcuts =
                                    shortcutManager.queryForPinnedShortcuts(null, user.getUser());
                            if (shortcutManager.wasLastCallSuccess()) {
                                for (ShortcutInfoCompat shortcut : pinnedShortcuts) {
                                    shortcutKeyToPinnedShortcuts.put(ShortcutKey.fromInfo(shortcut),
                                            shortcut);
                                }
                            } else {
                                // Shortcut manager can fail due to some race condition when the
                                // lock state changes too frequently. For the purpose of the loading
                                // shortcuts, consider the user is still locked.
                                userUnlocked = false;
                            }
                        }
                        unlockedUsers.put(serialNo, userUnlocked);
                    }

                    ShortcutInfo info;
                    String intentDescription;
                    LauncherAppWidgetInfo appWidgetInfo;
                    int container;
                    long id;
                    long serialNumber;
                    Intent intent;
                    UserHandleCompat user;
                    String targetPackage;

                    while (!mStopped && cursor.moveToNext()) {
                        try {
                            int itemType = cursor.getInt(itemTypeIndex);
                            boolean restored = 0 != cursor.getInt(restoredIndex);
                            boolean allowMissingTarget = false;
                            container = cursor.getInt(containerIndex);

                            switch (itemType) {
                                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                                case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                                case LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT:
                                case LauncherSettings.Favorites.ITEM_TYPE_VIRTUAL_APP:
                                    id = cursor.getLong(idIndex);
                                    intentDescription = cursor.getString(intentIndex);
                                    serialNumber = cursor.getInt(profileIdIndex);
                                    user = allUsers.get(serialNumber);
                                    int promiseType = cursor.getInt(restoredIndex);
                                    int disabledState = 0;
                                    boolean itemReplaced = false;
                                    targetPackage = null;
                                    if (user == null) {
                                        // User has been deleted remove the item.
                                        itemsToRemove.add(id);
                                        continue;
                                    }
                                    try {
                                        intent = Intent.parseUri(intentDescription, 0);
                                        ComponentName cn = intent.getComponent();
                                        if (cn != null && cn.getPackageName() != null) {

                                            if (itemType != LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT) {
                                                if (IOSOtaHandler.isAppHide(context, cn.getPackageName())) {
                                                    try {
                                                        context.getPackageManager().getApplicationInfo(cn.getPackageName(), PackageManager.GET_ACTIVITIES);
                                                    } catch (Exception e) {
                                                        itemsToRemove.add(id);
                                                        continue;
                                                    }
                                                    ComponentName appCN = IOSOtaHandler.getAppEntryRepalced(context, cn.getPackageName());
                                                    if (appCN != null) {
                                                        cn = appCN;
                                                        intent = new Intent(Intent.ACTION_MAIN, null)
                                                                .addCategory(Intent.CATEGORY_LAUNCHER)
                                                                .setComponent(cn)
                                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                                                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                                    }
                                                }

                                                if (IOSOtaHandler.isAppEntryHide(context, cn.getPackageName(), cn.getClassName())) {
                                                    itemsToRemove.add(id);
                                                    continue;
                                                }
                                            }

                                            boolean validPkg = launcherApps.isPackageEnabledForProfile(cn.getPackageName(), user);
                                            boolean validComponent = validPkg &&
                                                    launcherApps.isActivityEnabledForProfile(cn, user);
                                            if (validPkg) {
                                                targetPackage = cn.getPackageName();
                                            }

                                            if (validComponent) {
                                                if (restored) {
                                                    // no special handling necessary for this item
                                                    restoredRows.add(id);
                                                    restored = false;
                                                }
                                                if (quietMode.get(serialNumber)) {
                                                    disabledState = ShortcutInfo.FLAG_DISABLED_QUIET_USER;
                                                }
                                            } else if (validPkg) {
                                                intent = null;
                                                if ((promiseType & ShortcutInfo.FLAG_AUTOINTALL_ICON) != 0) {
                                                    // We allow auto install apps to have their intent
                                                    // updated after an install.
                                                    intent = manager.getLaunchIntentForPackage(cn.getPackageName());
                                                    if (intent != null) {
                                                        ContentValues values = new ContentValues();
                                                        values.put(LauncherSettings.Favorites.INTENT, intent.toUri(0));
                                                        updateItem(id, values);
                                                    }
                                                }

                                                if (intent == null) {
                                                    // The app is installed but the component is no longer available.
                                                    Launcher.addDumpLog(TAG, "Invalid component removed: " + cn, true);
                                                    itemsToRemove.add(id);
                                                    continue;
                                                } else {
                                                    // no special handling necessary for this item
                                                    restoredRows.add(id);
                                                    restored = false;
                                                }
                                            } else if (restored) {
                                                // Package is not yet available but might be installed later.
                                                Launcher.addDumpLog(TAG, "package not yet restored: " + cn, true);

                                                if ((promiseType & ShortcutInfo.FLAG_RESTORE_STARTED) != 0) {
                                                    // Restore has started once.
                                                } else if (installingPkgs.containsKey(cn.getPackageName())) {
                                                    // App restore has started. Update the flag
                                                    promiseType |= ShortcutInfo.FLAG_RESTORE_STARTED;
                                                    ContentValues values = new ContentValues();
                                                    values.put(LauncherSettings.Favorites.RESTORED, promiseType);
                                                    updateItem(id, values);
                                                } else if (REMOVE_UNRESTORED_ICONS) {
                                                    Launcher.addDumpLog(TAG, "Unrestored package removed: " + cn, true);
                                                    itemsToRemove.add(id);
                                                    continue;
                                                }
                                            /* uninstalled app show default icon modify
                                            } else if (PackageManagerHelper.isAppEnabled(
                                                    manager, cn.getPackageName(),
                                                    PackageManager.GET_UNINSTALLED_PACKAGES)) {
                                                // Package is present but not available.
                                                allowMissingTarget = true;
                                                disabledState = ShortcutInfo.FLAG_DISABLED_NOT_AVAILABLE;
                                            */
                                            } else if (!isSdCardReady) {
                                                // SdCard is not ready yet. Package might get available,
                                                // once it is ready.
                                                Launcher.addDumpLog(TAG, "Invalid package: " + cn
                                                        + " (check again later)", true);
                                                HashSet<String> pkgs = sPendingPackages.get(user);
                                                if (pkgs == null) {
                                                    pkgs = new HashSet<String>();
                                                    sPendingPackages.put(user, pkgs);
                                                }
                                                pkgs.add(cn.getPackageName());
                                                allowMissingTarget = true;
                                                // Add the icon on the workspace anyway.
                                            } else {
                                                // Do not wait for external media load anymore.
                                                // Log the invalid package, and remove it
                                                Launcher.addDumpLog(TAG, "Invalid package removed: " + cn, true);
                                                itemsToRemove.add(id);
                                                continue;
                                            }
                                        } else if (cn == null) {
                                            // For shortcuts with no component, keep them as they are
                                            restoredRows.add(id);
                                            restored = false;
                                        }
                                    } catch (URISyntaxException e) {
                                        Launcher.addDumpLog(TAG, "Invalid uri: " + intentDescription, true);
                                        itemsToRemove.add(id);
                                        continue;
                                    }

                                    boolean useLowResIcon = false/*container >= 0 && c.getInt(rankIndex) >= FolderIcon.NUM_ITEMS_IN_PREVIEW*/;

                                    if (itemReplaced) {
                                        if (user.equals(UserHandleCompat.myUserHandle())) {
                                            info = getAppShortcutInfo(manager, intent, user, context, null, cursorIconInfo.iconIndex, titleIndex, false, useLowResIcon);
                                        } else {
                                            // Don't replace items for other profiles.
                                            itemsToRemove.add(id);
                                            continue;
                                        }
                                    } else if (restored) {
                                        if (user.equals(UserHandleCompat.myUserHandle())) {
                                            Launcher.addDumpLog(TAG,
                                                    "constructing info for partially restored package",
                                                    true);
                                            info = getRestoredItemInfo(cursor, titleIndex, intent, promiseType, itemType, cursorIconInfo, context);
                                            intent = getRestoredItemIntent(cursor, context, intent);
                                        } else {
                                            // Don't restore items for other profiles.
                                            itemsToRemove.add(id);
                                            continue;
                                        }
                                    } else if (itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                                        info = getAppShortcutInfo(manager, intent, user, context, cursor, cursorIconInfo.iconIndex, titleIndex, allowMissingTarget, useLowResIcon);
                                    } else if (itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT) {
                                        ShortcutKey key = ShortcutKey.fromIntent(intent, user.getUser());
                                        if (unlockedUsers.get(serialNumber)) {
                                            ShortcutInfoCompat pinnedShortcut = shortcutKeyToPinnedShortcuts.get(key);
                                            if (pinnedShortcut == null) {
                                                // The shortcut is no longer valid.
                                                itemsToRemove.add(id);
                                                continue;
                                            }

                                            info = new ShortcutInfo(pinnedShortcut, context);
                                            info.setIcon(LauncherIcons.createShortcutIcon(pinnedShortcut, context));
                                            if (PackageManagerHelper.isAppSuspended(manager, pinnedShortcut.getPackage())) {
                                                info.isDisabled |= ShortcutInfo.FLAG_DISABLED_SUSPENDED;
                                            }
                                            intent = info.intent;
                                        } else {
                                            // Create a shortcut info in disabled mode for now.
                                            info = getShortcutInfo(intent, cursor, context, titleIndex, cursorIconInfo);
                                            info.isDisabled |= ShortcutInfo.FLAG_DISABLED_LOCKED_USER;
                                        }
                                    } else if (itemType == LauncherSettings.Favorites.ITEM_TYPE_VIRTUAL_APP) {
                                        info = getVirtualAppInfo(intent, cursor, context, titleIndex, cursorIconInfo);

                                        // Shortcuts are only available on the primary profile
                                        if (PackageManagerHelper.isAppSuspended(manager, targetPackage)) {
                                            disabledState |= ShortcutInfo.FLAG_DISABLED_SUSPENDED;
                                        }
                                        // App shortcuts that used to be automatically added to Launcher
                                        // didn't always have the correct intent flags set, so do that
                                        // here
                                        if (intent.getAction() != null &&
                                                intent.getCategories() != null &&
                                                intent.getAction().equals(Intent.ACTION_MAIN) &&
                                                intent.getCategories().contains(Intent.CATEGORY_LAUNCHER)) {
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                        }
                                    } else{
                                        info = getShortcutInfo(intent, cursor, context, titleIndex, cursorIconInfo);

                                        // Shortcuts are only available on the primary profile
                                        if (PackageManagerHelper.isAppSuspended(manager, targetPackage)) {
                                            disabledState |= ShortcutInfo.FLAG_DISABLED_SUSPENDED;
                                        }
                                        // App shortcuts that used to be automatically added to Launcher
                                        // didn't always have the correct intent flags set, so do that
                                        // here
                                        if (intent.getAction() != null &&
                                                intent.getCategories() != null &&
                                                intent.getAction().equals(Intent.ACTION_MAIN) &&
                                                intent.getCategories().contains(Intent.CATEGORY_LAUNCHER)) {
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                        }
                                    }

                                    if (info != null) {
                                        info.id = id;
                                        info.intent = intent;
                                        info.container = container;
                                        info.screenId = cursor.getInt(screenIndex);
                                        info.cellX = cursor.getInt(cellXIndex);
                                        info.cellY = cursor.getInt(cellYIndex);
                                        info.rank = cursor.getInt(rankIndex);
                                        info.spanX = 1;
                                        info.spanY = 1;
                                        info.calledNum = cursor.getInt(callNumIndex);
                                        info.lastCalledTime = cursor.getInt(lastCalledTimeIndex);
                                        info.url = cursor.getString(urlIndex );

                                        int hiddenFlag = cursor.getInt(hiddenIndex);
//                                        info.isHidden = (hiddenFlag > 0) ? true: false;
                                        info.intent.putExtra(ItemInfo.EXTRA_PROFILE, serialNumber);
                                        if (info.promisedIntent != null) {
                                            info.promisedIntent.putExtra(ItemInfo.EXTRA_PROFILE, serialNumber);
                                        }
                                        info.isDisabled = disabledState;
                                        if (isSafeMode && !Utilities.isSystemApp(context, intent)) {
                                            info.isDisabled |= ShortcutInfo.FLAG_DISABLED_SAFEMODE;
                                        }

                                        // check & update map of what's occupied
                                        if (!checkItemPlacement(occupied, info, sBgWorkspaceScreens)) {
                                            itemsToRemove.add(id);
                                            break;
                                        }

                                        if (restored) {
                                            ComponentName cn = info.getTargetComponent();
                                            if (cn != null) {
                                                Integer progress = installingPkgs.get(cn.getPackageName());
                                                if (progress != null) {
                                                    info.setInstallProgress(progress);
                                                } else {
                                                    info.status &= ~ShortcutInfo.FLAG_INSTALL_SESSION_ACTIVE;
                                                }
                                            }
                                        }

                                        switch (container) {
                                            case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                                            case LauncherSettings.Favorites.CONTAINER_HOTSEAT:
                                                sBgWorkspaceItems.add(info);
                                                break;
                                            default: // Item is in a user folder
                                                if (info.isHidden) {
                                                    sHdItemsIdMap.put(Long.valueOf(info.id), info);
                                                } else {
                                                    FolderInfo folderInfo = findOrMakeFolder(sBgFolders, container);
                                                    folderInfo.add(info);
                                                }
                                                break;
                                        }
                                        sBgItemsIdMap.put(info.id, info);
                                    } else {
                                        throw new RuntimeException("Unexpected null ShortcutInfo");
                                    }
                                    break;

                                case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                                    id = cursor.getLong(idIndex);
                                    FolderInfo folderInfo = findOrMakeFolder(sBgFolders, id);

                                    // Do not trim the folder label, as is was set by the user.
                                    final int categoryType = cursor.getInt(folderCatTypeIndex);
                                    if (categoryType > -1) {
                                        folderInfo.title = CategoryFolder.getFolerNameForType(mContext, categoryType);
                                    } else {
                                        folderInfo.title = cursor.getString(titleIndex);
                                    }
                                    folderInfo.folderCategoryType = categoryType;
                                    folderInfo.id = id;
                                    folderInfo.container = container;
                                    folderInfo.screenId = cursor.getInt(screenIndex);
                                    folderInfo.cellX = cursor.getInt(cellXIndex);
                                    folderInfo.cellY = cursor.getInt(cellYIndex);
                                    folderInfo.spanX = 1;
                                    folderInfo.spanY = 1;
                                    folderInfo.options = cursor.getInt(optionsIndex);

                                    // check & update map of what's occupied
                                    if (!checkItemPlacement(occupied, folderInfo, sBgWorkspaceScreens)) {
                                        itemsToRemove.add(id);
                                        break;
                                    }

                                    switch (container) {
                                        case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                                        case LauncherSettings.Favorites.CONTAINER_HOTSEAT:
                                            sBgWorkspaceItems.add(folderInfo);
                                            break;
                                    }

                                    if (restored) {
                                        // no special handling required for restored folders
                                        restoredRows.add(id);
                                    }

                                    sBgItemsIdMap.put(folderInfo.id, folderInfo);
                                    sBgFolders.put(folderInfo.id, folderInfo);
                                    break;

                                case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                                case LauncherSettings.Favorites.ITEM_TYPE_IOS_APPWIDGET:
                                    // Read all Launcher-specific widget details
                                    boolean customWidget = itemType == LauncherSettings.Favorites.ITEM_TYPE_IOS_APPWIDGET;

                                    int appWidgetId = cursor.getInt(appWidgetIdIndex);
                                    serialNumber = cursor.getLong(profileIdIndex);
                                    String savedProvider = cursor.getString(appWidgetProviderIndex);
                                    id = cursor.getLong(idIndex);
                                    user = allUsers.get(serialNumber);
                                    if (user == null) {
                                        itemsToRemove.add(id);
                                        continue;
                                    }

                                    final ComponentName component = ComponentName.unflattenFromString(savedProvider);

                                    final int restoreStatus = cursor.getInt(restoredIndex);
                                    final boolean isIdValid = (restoreStatus & LauncherAppWidgetInfo.FLAG_ID_NOT_VALID) == 0;
                                    final boolean wasProviderReady = (restoreStatus & LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY) == 0;

                                    final LauncherAppWidgetProviderInfo provider = LauncherModel.getProviderInfo(context, ComponentName.unflattenFromString(savedProvider),
                                                    user);

                                    final boolean isProviderReady = isValidProvider(provider);
                                    if (!isSafeMode && !customWidget && wasProviderReady && !isProviderReady) {
                                        String log = "Deleting widget that isn't installed anymore: " + "id=" + id + " appWidgetId=" + appWidgetId;

                                        Log.e(TAG, log);
                                        Launcher.addDumpLog(TAG, log, false);
                                        itemsToRemove.add(id);
                                    } else {
                                        if (isProviderReady) {
                                            appWidgetInfo = new LauncherAppWidgetInfo(appWidgetId, provider.provider);
                                            // The provider is available. So the widget is either
                                            // available or not available. We do not need to track
                                            // any future restore updates.
                                            int status = restoreStatus & ~LauncherAppWidgetInfo.FLAG_RESTORE_STARTED;
                                            if (!wasProviderReady) {
                                                // If provider was not previously ready, update the status and UI flag.
                                                // Id would be valid only if the widget restore broadcast was received.
                                                if (isIdValid) {
                                                    status = LauncherAppWidgetInfo.FLAG_UI_NOT_READY;
                                                } else {
                                                    status &= ~LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY;
                                                }
                                            }
                                            appWidgetInfo.restoreStatus = status;
                                        } else {
                                            Log.v(TAG, "Widget restore pending id=" + id + " appWidgetId=" + appWidgetId + " status =" + restoreStatus);
                                            appWidgetInfo = new LauncherAppWidgetInfo(appWidgetId, component);
                                            appWidgetInfo.restoreStatus = restoreStatus;
                                            Integer installProgress = installingPkgs.get(component.getPackageName());

                                            if ((restoreStatus & LauncherAppWidgetInfo.FLAG_RESTORE_STARTED) != 0) {
                                                // Restore has started once.
                                            } else if (installProgress != null) {
                                                // App restore has started. Update the flag
                                                appWidgetInfo.restoreStatus |= LauncherAppWidgetInfo.FLAG_RESTORE_STARTED;
                                            } else if (REMOVE_UNRESTORED_ICONS && !isSafeMode) {
                                                Launcher.addDumpLog(TAG, "Unrestored widget removed: " + component, true);
                                                itemsToRemove.add(id);
                                                continue;
                                            }

                                            appWidgetInfo.installProgress = installProgress == null ? 0 : installProgress;
                                        }

                                        appWidgetInfo.id = id;
                                        appWidgetInfo.screenId = cursor.getInt(screenIndex);
                                        appWidgetInfo.cellX = cursor.getInt(cellXIndex);
                                        appWidgetInfo.cellY = cursor.getInt(cellYIndex);
                                        appWidgetInfo.spanX = cursor.getInt(spanXIndex);
                                        appWidgetInfo.spanY = cursor.getInt(spanYIndex);
                                        appWidgetInfo.user = user;

                                        if (container != LauncherSettings.Favorites.CONTAINER_DESKTOP && container != LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                                            Log.e(TAG, "Widget found where container != " + "CONTAINER_DESKTOP nor CONTAINER_HOTSEAT - ignoring!");
                                            itemsToRemove.add(id);
                                            continue;
                                        }

                                        appWidgetInfo.container = container;
                                        // check & update map of what's occupied
                                        if (!checkItemPlacement(occupied, appWidgetInfo, sBgWorkspaceScreens)) {
                                            itemsToRemove.add(id);
                                            break;
                                        }

                                        if (!customWidget) {
                                            String providerName = appWidgetInfo.providerName.flattenToString();
                                            if (!providerName.equals(savedProvider) || (appWidgetInfo.restoreStatus != restoreStatus)) {
                                                ContentValues values = new ContentValues();
                                                values.put(LauncherSettings.Favorites.APPWIDGET_PROVIDER, providerName);
                                                values.put(LauncherSettings.Favorites.RESTORED, appWidgetInfo.restoreStatus);
                                                updateItem(id, values);
                                            }
                                        }
                                        sBgItemsIdMap.put(appWidgetInfo.id, appWidgetInfo);
                                        sBgAppWidgets.add(appWidgetInfo);
                                    }
                                    break;
                            }
                        } catch (Exception e) {
                            Launcher.addDumpLog(TAG, "Desktop items loading interrupted", e, true);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }

                // Break early if we've stopped loading
                if (mStopped) {
                    clearSBgDataStructures();
                    return;
                }

                if (itemsToRemove.size() > 0) {
                    // Remove dead items
                    contentResolver.delete(LauncherSettings.Favorites.CONTENT_URI, Utilities.createDbSelectionQuery(LauncherSettings.Favorites._ID, itemsToRemove), null);
                    if (DEBUG_LOADERS)
                    {
                        Log.d(TAG, "Removed = " + Utilities.createDbSelectionQuery(LauncherSettings.Favorites._ID, itemsToRemove));
                    }

                    // Remove any empty folder
                    for (long folderId : LauncherAppState.getLauncherProvider().deleteEmptyFolders()) {
                        sBgWorkspaceItems.remove(sBgFolders.get(folderId));
                        sBgFolders.remove(folderId);
                        sBgItemsIdMap.remove(folderId);
                    }
                }

                if (itemsToRelocate.size() > 0) {
                    addAndBindAddedWorkspaceItems(mContext, itemsToRelocate);
                }

                // Sort all the folder items and make sure the first 3 items are high resolution.
                ArrayList<Long> foldersToRemove = new ArrayList<Long>();

                for (int i  = 0; i < sBgFolders.size(); i ++) {
                    FolderInfo folder = sBgFolders.valueAt(i);
                    if (folder.contents.size() <= 0)
                        foldersToRemove.add(folder.id);

                    Collections.sort(folder.contents, Folder.ITEM_POS_COMPARATOR);
                    int pos = 0;
                    for (ShortcutInfo info : folder.contents) {
                        if (info.usingLowResIcon) {
                            info.updateIcon(mIconCache, false);
                        }
                        pos++;
                        if (pos >= FolderIcon.NUM_ITEMS_IN_PREVIEW) {
                            break;
                        }
                    }
                }

                for (Long index : foldersToRemove) {
                    long folderId = index.longValue();
                    sBgWorkspaceItems.remove(sBgFolders.get(folderId));
                    sBgFolders.remove(folderId);
                    sBgItemsIdMap.remove(folderId);
                }

                if (restoredRows.size() > 0) {
                    // Update restored items that no longer require special handling
                    ContentValues values = new ContentValues();
                    values.put(LauncherSettings.Favorites.RESTORED, 0);
                    contentResolver.update(LauncherSettings.Favorites.CONTENT_URI, values,
                            Utilities.createDbSelectionQuery(
                                    LauncherSettings.Favorites._ID, restoredRows), null);
                }

                if (!isSdCardReady && !sPendingPackages.isEmpty()) {
                    context.registerReceiver(new AppsAvailabilityCheck(),
                            new IntentFilter(StartupReceiver.SYSTEM_READY),
                            null, sWorker);
                }

                // Add any need screens
//                ArrayList<Long> needScreens = new ArrayList<Long>();
//                int needSize = occupied.size();
//                for (int i = 0; i < needSize; i++) {
//                    long screenId = occupied.keyAt(i);
//                    if (screenId > 0 && !sBgWorkspaceScreens.contains(screenId)) {
//                        needScreens.add(screenId);
//                    }
//                }
//
//                // Remove any empty screens
//                ArrayList<Long> unusedScreens = new ArrayList<Long>(sBgWorkspaceScreens);
//                for (ItemInfo item : sBgItemsIdMap) {
//                    long screenId = item.screenId;
//                    if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP && unusedScreens.contains(screenId)) {
//                        unusedScreens.remove(screenId);
//                    }
//                }
//
//                // If there are any empty screens remove them, and update.
//                if (unusedScreens.size() != 0 || needScreens.size() != 0) {
//                    sBgWorkspaceScreens.addAll(needScreens);
//                    sBgWorkspaceScreens.removeAll(unusedScreens);
//                    updateWorkspaceScreenOrder(context, sBgWorkspaceScreens);
//                }


                DebugUtil.debugLaunch(TAG, "loadWorkspace end");
                if (DEBUG_LOADERS) {
                    Log.d(TAG, "workspace layout: ");
                    int nScreens = occupied.size();
                    for (int y = 0; y < countY; y++) {
                        String line = "";

                        for (int i = 0; i < nScreens; i++) {
                            long screenId = occupied.keyAt(i);
                            if (screenId > 0) {
                                line += " | ";
                            }
                            ItemInfo[][] screen = occupied.valueAt(i);
                            for (int x = 0; x < countX; x++) {
                                if (x < screen.length && y < screen[x].length) {
                                    line += (screen[x][y] != null) ? "#" : ".";
                                } else {
                                    line += "!";
                                }
                            }
                        }
                        Log.d(TAG, "[ " + line + " ]");
                    }
                }
            }
        }

        /**
         * Partially updates the item without any notification. Must be called on the worker thread.
         */
        private void updateItem(long itemId, ContentValues update) {
            mContext.getContentResolver().update(
                    LauncherSettings.Favorites.CONTENT_URI,
                    update,
                    BaseColumns._ID + "= ?",
                    new String[]{Long.toString(itemId)});
        }

        /**
         * Filters the set of items who are directly or indirectly (via another container) on the
         * specified screen.
         */
        private void filterCurrentWorkspaceItems(long currentScreenId,
                                                 ArrayList<ItemInfo> allWorkspaceItems,
                                                 ArrayList<ItemInfo> currentScreenItems,
                                                 ArrayList<ItemInfo> otherScreenItems) {
            // Purge any null ItemInfos
            Iterator<ItemInfo> iter = allWorkspaceItems.iterator();
            while (iter.hasNext()) {
                ItemInfo i = iter.next();
                if (i == null) {
                    iter.remove();
                }
            }

            // Order the set of items by their containers first, this allows use to walk through the
            // list sequentially, build up a list of containers that are in the specified screen,
            // as well as all items in those containers.
            Set<Long> itemsOnScreen = new HashSet<Long>();
            Collections.sort(allWorkspaceItems, new Comparator<ItemInfo>() {
                @Override
                public int compare(ItemInfo lhs, ItemInfo rhs) {
                    return (int) (lhs.container - rhs.container);
                }
            });
            for (ItemInfo info : allWorkspaceItems) {
                if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    if (info.screenId == currentScreenId) {
                        currentScreenItems.add(info);
                        itemsOnScreen.add(info.id);
                    } else {
                        otherScreenItems.add(info);
                    }
                } else if (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    currentScreenItems.add(info);
                    itemsOnScreen.add(info.id);
                } else {
                    if (itemsOnScreen.contains(info.container)) {
                        currentScreenItems.add(info);
                        itemsOnScreen.add(info.id);
                    } else {
                        otherScreenItems.add(info);
                    }
                }
            }
        }

        /**
         * Filters the set of widgets which are on the specified screen.
         */
        private void filterCurrentAppWidgets(long currentScreenId,
                                             ArrayList<LauncherAppWidgetInfo> appWidgets,
                                             ArrayList<LauncherAppWidgetInfo> currentScreenWidgets,
                                             ArrayList<LauncherAppWidgetInfo> otherScreenWidgets) {

            for (LauncherAppWidgetInfo widget : appWidgets) {
                if (widget == null) continue;
                if (widget.container == LauncherSettings.Favorites.CONTAINER_DESKTOP &&
                        widget.screenId == currentScreenId) {
                    currentScreenWidgets.add(widget);
                } else {
                    otherScreenWidgets.add(widget);
                }
            }
        }

        /**
         * Filters the set of folders which are on the specified screen.
         */
        private void filterCurrentFolders(long currentScreenId,
                                          LongArrayMap<ItemInfo> itemsIdMap,
                                          LongArrayMap<FolderInfo> folders,
                                          LongArrayMap<FolderInfo> currentScreenFolders,
                                          LongArrayMap<FolderInfo> otherScreenFolders) {

            int total = folders.size();
            for (int i = 0; i < total; i++) {
                long id = folders.keyAt(i);
                FolderInfo folder = folders.valueAt(i);

                ItemInfo info = itemsIdMap.get(id);
                if (info == null || folder == null) continue;
                if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP &&
                        info.screenId == currentScreenId) {
                    currentScreenFolders.put(id, folder);
                } else {
                    otherScreenFolders.put(id, folder);
                }
            }
        }

        /**
         * Sorts the set of items by hotseat, workspace (spatially from top to bottom, left to
         * right)
         */
        private void sortWorkspaceItemsSpatially(ArrayList<ItemInfo> workspaceItems) {
            final LauncherAppState app = LauncherAppState.getInstance();
            final InvariantDeviceProfile profile = app.getInvariantDeviceProfile();
            // XXX: review this
            Collections.sort(workspaceItems, new Comparator<ItemInfo>() {
                @Override
                public int compare(ItemInfo lhs, ItemInfo rhs) {
                    int cellCountX = profile.numColumns;
                    int cellCountY = profile.numRows;
                    int screenOffset = cellCountX * cellCountY;
                    int containerOffset = screenOffset * (Launcher.SCREEN_COUNT + 1); // +1 hotseat
                    long lr = (lhs.container * containerOffset + lhs.screenId * screenOffset +
                            lhs.cellY * cellCountX + lhs.cellX);
                    long rr = (rhs.container * containerOffset + rhs.screenId * screenOffset +
                            rhs.cellY * cellCountX + rhs.cellX);
                    return (int) (lr - rr);
                }
            });
        }

        private void bindWorkspaceScreens(final Callbacks oldCallbacks,
                                          final ArrayList<Long> orderedScreens) {
            final Runnable r = new Runnable() {
                @Override
                public void run() {
                    Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindScreens(orderedScreens);
                    }
                }
            };
            runOnMainThread(r);
        }

        private void bindWorkspaceItems(final Callbacks oldCallbacks,
                                        final ArrayList<ItemInfo> workspaceItems,
                                        final ArrayList<LauncherAppWidgetInfo> appWidgets,
                                        final LongArrayMap<FolderInfo> folders,
                                        ArrayList<Runnable> deferredBindRunnables) {

            final boolean postOnMainThread = (deferredBindRunnables != null);

            // Bind the workspace items
            int N = workspaceItems.size();
            for (int i = 0; i < N; i += ITEMS_CHUNK) {
                final int start = i;
                final int chunkSize = (i + ITEMS_CHUNK <= N) ? ITEMS_CHUNK : (N - i);
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                        if (callbacks != null) {
                            callbacks.bindItems(workspaceItems, start, start + chunkSize,
                                    false);
                        }
                    }
                };
                if (postOnMainThread) {
                    synchronized (deferredBindRunnables) {
                        deferredBindRunnables.add(r);
                    }
                } else {
                    runOnMainThread(r);
                }
            }

            // Bind the folders
            if (!folders.isEmpty()) {
                final Runnable r = new Runnable() {
                    public void run() {
                        Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                        if (callbacks != null) {
                            callbacks.bindFolders(folders);
                        }
                    }
                };
                if (postOnMainThread) {
                    synchronized (deferredBindRunnables) {
                        deferredBindRunnables.add(r);
                    }
                } else {
                    runOnMainThread(r);
                }
            }

            // Bind the widgets, one at a time
            N = appWidgets.size();
            for (int i = 0; i < N; i++) {
                final LauncherAppWidgetInfo widget = appWidgets.get(i);
                final Runnable r = new Runnable() {
                    public void run() {
                        Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                        if (callbacks != null) {
                            callbacks.bindAppWidget(widget);
                        }
                    }
                };
                if (postOnMainThread) {
                    deferredBindRunnables.add(r);
                } else {
                    runOnMainThread(r);
                }
            }
        }

        /**
         * Binds all loaded data to actual views on the main thread.
         */
        private void bindWorkspace(int synchronizeBindPage) {
            DebugUtil.debugLaunch(TAG, "bindWorkspace start");
            Runnable r;

            // Don't use these two variables in any of the callback runnables.
            // Otherwise we hold a reference to them.
            final Callbacks oldCallbacks = mCallbacks.get();
            if (oldCallbacks == null) {
                // This launcher has exited and nobody bothered to tell us.  Just bail.
                Log.w(TAG, "LoaderTask running with no launcher");
                return;
            }

            // Save a copy of all the bg-thread collections
            ArrayList<ItemInfo> workspaceItems = new ArrayList<ItemInfo>();
            ArrayList<LauncherAppWidgetInfo> appWidgets = new ArrayList<LauncherAppWidgetInfo>();
            ArrayList<Long> orderedScreenIds = new ArrayList<Long>();

            final LongArrayMap<FolderInfo> folders;
            final LongArrayMap<ItemInfo> itemsIdMap;

            synchronized (sBgLock) {
                workspaceItems.addAll(sBgWorkspaceItems);
                appWidgets.addAll(sBgAppWidgets);
                orderedScreenIds.addAll(sBgWorkspaceScreens);

                folders = sBgFolders.clone();
                itemsIdMap = sBgItemsIdMap.clone();
            }

            final boolean isLoadingSynchronously = synchronizeBindPage != PagedView.INVALID_RESTORE_PAGE;
            int currScreen = isLoadingSynchronously ? synchronizeBindPage : oldCallbacks.getCurrentWorkspaceScreen();
            if (currScreen >= orderedScreenIds.size()) {
                // There may be no workspace screens (just hotseat items and an empty page).
                currScreen = PagedView.INVALID_RESTORE_PAGE;
            }
            final int currentScreen = currScreen;
            final long currentScreenId = currentScreen < 0 ? INVALID_SCREEN_ID : orderedScreenIds.get(currentScreen);

            // Load all the items that are on the current page first (and in the process, unbind
            // all the existing workspace items before we call startBinding() below.
            unbindWorkspaceItemsOnMainThread();

            // Separate the items that are on the current screen, and all the other remaining items
            ArrayList<ItemInfo> currentWorkspaceItems = new ArrayList<ItemInfo>();
            ArrayList<ItemInfo> otherWorkspaceItems = new ArrayList<ItemInfo>();
            ArrayList<LauncherAppWidgetInfo> currentAppWidgets = new ArrayList<LauncherAppWidgetInfo>();
            ArrayList<LauncherAppWidgetInfo> otherAppWidgets = new ArrayList<LauncherAppWidgetInfo>();
            LongArrayMap<FolderInfo> currentFolders = new LongArrayMap<>();
            LongArrayMap<FolderInfo> otherFolders = new LongArrayMap<>();

            filterCurrentWorkspaceItems(currentScreenId, workspaceItems, currentWorkspaceItems, otherWorkspaceItems);
            filterCurrentAppWidgets(currentScreenId, appWidgets, currentAppWidgets, otherAppWidgets);
            filterCurrentFolders(currentScreenId, itemsIdMap, folders, currentFolders, otherFolders);
            sortWorkspaceItemsSpatially(currentWorkspaceItems);
            sortWorkspaceItemsSpatially(otherWorkspaceItems);

            // Tell the workspace that we're about to start binding items
            r = new Runnable() {
                public void run() {
                    Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.startBinding();
                    }
                }
            };
            runOnMainThread(r);

            bindWorkspaceScreens(oldCallbacks, orderedScreenIds);

            // Load items on the current page
            bindWorkspaceItems(oldCallbacks, currentWorkspaceItems, currentAppWidgets,
                    currentFolders, null);

            if (isLoadingSynchronously) {
                r = new Runnable() {
                    public void run() {
                        Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                        if (callbacks != null && currentScreen != PagedView.INVALID_RESTORE_PAGE) {
                            DebugUtil.debugLaunch(TAG, "onPageBoundSynchronously");
                            callbacks.onPageBoundSynchronously(currentScreen);
                        }
                    }
                };
                runOnMainThread(r);
            }

            // Load all the remaining pages (if we are loading synchronously, we want to defer this
            // work until after the first render)
            synchronized (mDeferredBindRunnables) {
                mDeferredBindRunnables.clear();
            }
            bindWorkspaceItems(oldCallbacks, otherWorkspaceItems, otherAppWidgets, otherFolders,
                    (isLoadingSynchronously ? mDeferredBindRunnables : null));

            // Tell the workspace that we're done binding items
            r = new Runnable() {
                public void run() {
                    Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        DebugUtil.debugLaunch(TAG, "finishBindingItems");
                        callbacks.finishBindingItems();
                    }

                    mIsLoadingAndBindingWorkspace = false;

                    // Run all the bind complete runnables after workspace is bound.
                    if (!mBindCompleteRunnables.isEmpty()) {
                        synchronized (mBindCompleteRunnables) {
                            for (final Runnable r : mBindCompleteRunnables) {
                                runOnWorkerThread(r);
                            }
                            mBindCompleteRunnables.clear();
                        }
                    }

                }
            };
            if (isLoadingSynchronously) {
                synchronized (mDeferredBindRunnables) {
                    mDeferredBindRunnables.add(r);
                }
            } else {
                runOnMainThread(r);
            }
        }

        private void loadAndBindAllApps() {
            if (DEBUG_LOADERS) {
                Log.d(TAG, "loadAndBindAllApps mAllAppsLoaded=" + mAllAppsLoaded);
            }
            if (!mAllAppsLoaded) {
                loadAllApps();
                synchronized (LoaderTask.this) {
                    if (mStopped) {
                        return;
                    }
                }
                updateIconCache();
                synchronized (LoaderTask.this) {
                    if (mStopped) {
                        return;
                    }
                    mAllAppsLoaded = true;
                }
            } else {
                onlyBindAllApps();
            }
        }

        private void updateIconCache() {
            // Ignore packages which have a promise icon.
            HashSet<String> packagesToIgnore = new HashSet<>();
            synchronized (sBgLock) {
                for (ItemInfo info : sBgItemsIdMap) {
                    if (info instanceof ShortcutInfo) {
                        ShortcutInfo si = (ShortcutInfo) info;
                        if (si.isPromise() && si.getTargetComponent() != null) {
                            packagesToIgnore.add(si.getTargetComponent().getPackageName());
                        }
                    } else if (info instanceof LauncherAppWidgetInfo) {
                        LauncherAppWidgetInfo lawi = (LauncherAppWidgetInfo) info;
                        if (lawi.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY)) {
                            packagesToIgnore.add(lawi.providerName.getPackageName());
                        }
                    }
                }
            }
            mIconCache.updateDbIcons(packagesToIgnore);
        }

        //todo load app
        private void onlyBindAllApps() {
            final Callbacks oldCallbacks = mCallbacks.get();
            if (oldCallbacks == null) {
                // This launcher has exited and nobody bothered to tell us.  Just bail.
                Log.w(TAG, "LoaderTask running with no launcher (onlyBindAllApps)");
                return;
            }

            // shallow copy
            @SuppressWarnings("unchecked")
            final ArrayList<AppInfo> list
                    = (ArrayList<AppInfo>) mBgAllAppsList.data.clone();
            final WidgetsModel widgetList = mBgWidgetsModel.clone();

            Runnable r = new Runnable() {
                public void run() {
                    final long t = SystemClock.uptimeMillis();
                    final Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindAllApplications(list);
                        callbacks.bindAllPackages(widgetList);
                    }
                    if (DEBUG_LOADERS) {
                        Log.d(TAG, "bound all " + list.size() + " apps from cache in "
                                + (SystemClock.uptimeMillis() - t) + "ms");
                    }
                }
            };

            runOnMainThread(r);
        }

        //todo load app
        private void loadAllApps() {
            final long loadTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

            final Callbacks oldCallbacks = mCallbacks.get();
            if (oldCallbacks == null) {
                // This launcher has exited and nobody bothered to tell us.  Just bail.
                Log.w(TAG, "LoaderTask running with no launcher (loadAllApps)");
                return;
            }

            final List<UserHandleCompat> profiles = mUserManager.getUserProfiles();

            String[] hideApps = Partner.getStringArray(mContext, Partner.DEF_HIDE_APP_LIST);

            // Clear the list of apps
            mBgAllAppsList.clear();
            for (UserHandleCompat user : profiles) {
                // Query for the set of apps0
                final long qiaTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
                final List<LauncherActivityInfoCompat> apps = mLauncherApps.getActivityList(null, user);
                if (DEBUG_LOADERS) {
                    Log.d(TAG, "getActivityList took " + (SystemClock.uptimeMillis() - qiaTime) + "ms for user " + user);
                    Log.d(TAG, "getActivityList got " + apps.size() + " apps for user " + user);
                }
                // Fail if we don't have any apps
                // TODO: Fix this. Only fail for the current user.
                if (apps == null || apps.isEmpty()) {
                    return;
                }
                boolean quietMode = mUserManager.isQuietModeEnabled(user);
                // Create the ApplicationInfos
                for (int i = 0; i < apps.size(); i++) {
                    LauncherActivityInfoCompat app = apps.get(i);
                    if (AppFilter.filterLoadApp(mContext, app.getComponentName())) {
                        continue;
                    }

                    if (isHideApp(app.getComponentName().getPackageName(), hideApps)) {
                        continue;
                    }

                    if (IOSOtaHandler.isAppHide(mContext, app.getComponentName().getPackageName())
                            || IOSOtaHandler.isAppEntryHide(mContext, app.getComponentName().getPackageName(), app.getComponentName().getClassName())) {
                        continue;
                    }
                    // This builds the icon bitmaps.
                    AppInfo appInfo = new AppInfo(mContext, app, user, mIconCache, quietMode);
                    String packageName = appInfo.componentName.getPackageName();
                    appInfo.newInstalled = LauncherModel.this.getAppNewFlag(packageName);
                    mBgAllAppsList.add(appInfo);
                }

                final List<LauncherActivityInfoCompat> homeApp = mLauncherApps.getActivityList("com.oslauncher.applauncher.themelauncher", user);
                for (LauncherActivityInfoCompat app: homeApp) {
                    AppInfo appInfo = new AppInfo(mContext, app, user, mIconCache, quietMode);
                    String packageName = appInfo.componentName.getPackageName();
                    appInfo.newInstalled = LauncherModel.this.getAppNewFlag(packageName);
                    mBgAllAppsList.add(appInfo);
                }


                final ManagedProfileHeuristic heuristic = ManagedProfileHeuristic.get(mContext, user);
                if (heuristic != null) {
                    final Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            heuristic.processUserApps(apps);
                        }
                    };
                    runOnMainThread(new Runnable() {

                        @Override
                        public void run() {
                            // Check isLoadingWorkspace on the UI thread, as it is updated on
                            // the UI thread.
                            if (mIsLoadingAndBindingWorkspace) {
                                synchronized (mBindCompleteRunnables) {
                                    mBindCompleteRunnables.add(r);
                                }
                            } else {
                                runOnWorkerThread(r);
                            }
                        }
                    });
                }
            }
            // Huh? Shouldn't this be inside the Runnable below?
            final ArrayList<AppInfo> added = mBgAllAppsList.added;
            mBgAllAppsList.added = new ArrayList<AppInfo>();

            // Post callback on main thread
            mHandler.post(new Runnable() {
                public void run() {

                    final long bindTime = SystemClock.uptimeMillis();
                    final Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindAllApplications(added);
                        if (DEBUG_LOADERS) {
                            Log.d(TAG, "bound " + added.size() + " apps in "
                                    + (SystemClock.uptimeMillis() - bindTime) + "ms");
                        }
                    } else {
                        Log.i(TAG, "not binding apps: no Launcher activity");
                    }
                }
            });
            // Cleanup any data stored for a deleted user.
            ManagedProfileHeuristic.processAllUsers(profiles, mContext);

            loadAndBindWidgetsAndShortcuts(tryGetCallbacks(oldCallbacks), true /* refresh */);
            if (DEBUG_LOADERS) {
                Log.d(TAG, "Icons processed in "
                        + (SystemClock.uptimeMillis() - loadTime) + "ms");
            }
        }

        private void loadStart() {
            final boolean hasLoad = mWorkspaceLoaded;
            Runnable r = new Runnable() {
                public void run() {
                    final Callbacks callbacks = mCallbacks.get();
                    if (callbacks != null) {
                        callbacks.onLoadStart(hasLoad);
                    }
                }
            };
            runOnMainThread(r);
        }


        private void loadComplete() {
            Runnable r = new Runnable() {
                public void run() {
                    final Callbacks callbacks = mCallbacks.get();
                    if (callbacks != null) {
                        callbacks.onLoadComplete();
                    }
                }
            };

            runOnMainThread(r);
        }

        private void verifyApplication() {
            final Context context = mApp.getContext();
            ArrayList<ItemInfo> tmpInfos;
            ArrayList<ItemInfo> added = new ArrayList<ItemInfo>();

            synchronized (sBgLock) {
                for (AppInfo app : mBgAllAppsList.data) {
                    tmpInfos = getItemInfoForComponentName(app.componentName, app.user);
                    if (tmpInfos.isEmpty()) {
                        app.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                        added.add(app);
                        Log.d(TAG, "Missing Application on load : " + app);
                    }
                }
            }

            if (!added.isEmpty()) {
                boolean prompt = !mInitWorkspace;
                addAndBindAddedWorkspaceItems(context, added, true, prompt);
            }
        }

        private void loadDeepShortcuts() {
            sBgDataModel.deepShortcutMap.clear();
            DeepShortcutManager shortcutManager = DeepShortcutManager.getInstance(mContext);
            mHasShortcutHostPermission = shortcutManager.hasHostPermission();

            if (mHasShortcutHostPermission) {
                for (UserHandleCompat user : mUserManager.getUserProfiles()) {
                    if (mUserManager.isUserUnlocked(user)) {
                        List<ShortcutInfoCompat> shortcuts =
                                shortcutManager.queryForAllShortcuts(user.getUser());

                        sBgDataModel.updateDeepShortcutMap(null, user.getUser(), shortcuts);
                    }
                }
            }
        }

        public void dumpState() {
            synchronized (sBgLock) {
                Log.d(TAG, "mLoaderTask.mContext=" + mContext);
                Log.d(TAG, "mLoaderTask.mStopped=" + mStopped);
                Log.d(TAG, "mLoaderTask.mLoadAndBindStepFinished=" + mLoadAndBindStepFinished);
                Log.d(TAG, "mItems size=" + sBgWorkspaceItems.size());
            }
        }
    }

    public void bindDeepShortcuts() {
        final MultiHashMap<ComponentKey, String> shortcutMapCopy =
                sBgDataModel.deepShortcutMap.clone();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Callbacks callbacks = getCallback();
                if (callbacks != null) {
                    callbacks.bindDeepShortcutMap(shortcutMapCopy);
                }
            }
        };
        runOnMainThread(r);
    }

    /**
     * Refreshes the cached shortcuts if the shortcut permission has changed.
     * Current implementation simply reloads the workspace, but it can be optimized to
     * use partial updates similar to {@link UserManagerCompat}
     */
    public void refreshShortcutsIfRequired() {
        if (Utilities.ATLEAST_NOUGAT_MR1) {
            sWorker.removeCallbacks(mShortcutPermissionCheckRunnable);
            sWorker.post(mShortcutPermissionCheckRunnable);
        }
    }

    /**
     * Called when the icons for packages have been updated in the icon cache.
     */
    public void onPackageIconsUpdated(HashSet<String> updatedPackages, UserHandleCompat user) {
        final Callbacks callbacks = getCallback();
        final ArrayList<AppInfo> updatedApps = new ArrayList<>();
        final ArrayList<ShortcutInfo> updatedShortcuts = new ArrayList<>();

        // If any package icon has changed (app was updated while launcher was dead),
        // update the corresponding shortcuts.
        synchronized (sBgLock) {
            for (ItemInfo info : sBgItemsIdMap) {
                if (info instanceof ShortcutInfo && user.equals(info.user)
                        && info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                    ShortcutInfo si = (ShortcutInfo) info;
                    ComponentName cn = si.getTargetComponent();
                    if (cn != null && updatedPackages.contains(cn.getPackageName())) {
                        si.updateIcon(mIconCache);
                        updatedShortcuts.add(si);
                    }
                }
            }
            mBgAllAppsList.updateIconsAndLabels(updatedPackages, user, updatedApps);
        }

        if (!updatedShortcuts.isEmpty()) {
            final UserHandleCompat userFinal = user;
            mHandler.post(new Runnable() {

                public void run() {
                    Callbacks cb = getCallback();
                    if (cb != null && callbacks == cb) {
                        cb.bindShortcutsChanged(updatedShortcuts,
                                new ArrayList<ShortcutInfo>(), userFinal);
                    }
                }
            });
        }

        if (!updatedApps.isEmpty()) {
            mHandler.post(new Runnable() {

                public void run() {
                    Callbacks cb = getCallback();
                    if (cb != null && callbacks == cb) {
                        cb.bindAppsUpdated(updatedApps);
                    }
                }
            });
        }

        // Reload widget list. No need to refresh, as we only want to update the icons and labels.
        loadAndBindWidgetsAndShortcuts(callbacks, false);
    }

    void enqueuePackageUpdated(PackageUpdatedTask task) {
        sWorker.post(task);
    }

    @Thunk
    class AppsAvailabilityCheck extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (sBgLock) {
                final LauncherAppsCompat launcherApps = LauncherAppsCompat
                        .getInstance(mApp.getContext());
                final PackageManager manager = context.getPackageManager();
                final ArrayList<String> packagesRemoved = new ArrayList<String>();
                final ArrayList<String> packagesUnavailable = new ArrayList<String>();
                for (Entry<UserHandleCompat, HashSet<String>> entry : sPendingPackages.entrySet()) {
                    UserHandleCompat user = entry.getKey();
                    packagesRemoved.clear();
                    packagesUnavailable.clear();
                    for (String pkg : entry.getValue()) {
                        if (!launcherApps.isPackageEnabledForProfile(pkg, user)) {
                            boolean packageOnSdcard = PackageManagerHelper.isAppEnabled(
                                    manager, pkg, PackageManager.GET_UNINSTALLED_PACKAGES);
                            if (packageOnSdcard) {
                                Launcher.addDumpLog(TAG, "Package found on sd-card: " + pkg, true);
                                packagesUnavailable.add(pkg);
                            } else {
                                Launcher.addDumpLog(TAG, "Package not found: " + pkg, true);
                                packagesRemoved.add(pkg);
                            }
                        }
                    }
                    if (!packagesRemoved.isEmpty()) {
                        enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_REMOVE,
                                packagesRemoved.toArray(new String[packagesRemoved.size()]), user));
                    }
                    if (!packagesUnavailable.isEmpty()) {
                        enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_UNAVAILABLE,
                                packagesUnavailable.toArray(new String[packagesUnavailable.size()]), user));
                    }
                }
                sPendingPackages.clear();
            }
        }
    }

    private class PackageUpdatedTask implements Runnable {

        int mOp;
        String[] mPackages;
        String mAppLabel;
        int mCategory;
        int mIconId;
        String mIconPath;
        String mComponentName;
        UserHandleCompat mUser;
        int mIconType;

        public static final int OP_NONE = 0;
        public static final int OP_ADD = 1;
        public static final int OP_UPDATE = 2;
        public static final int OP_REMOVE = 3; // uninstlled
        public static final int OP_UNAVAILABLE = 4; // external media unmounted
        public static final int OP_ADD_ENTRY = 5;
        public static final int OP_REMOVE_ENTRY = 6;
        public static final int OP_SUSPEND = 7; // package suspended
        public static final int OP_UNSUSPEND = 8; // package unsuspended
        public static final int OP_USER_AVAILABILITY_CHANGE = 9; // user available/unavailable
        public static final int OP_USER_UPDATE_ICON = 10;
        public static final int OP_USER_UPDATE_LABEL = 11;
        public static final int OP_USER_UPDATE_CATEGORY = 12;

        public PackageUpdatedTask(int op, String[] packages, UserHandleCompat user) {
            mOp = op;
            mPackages = packages;
            mUser = user;
        }

        public PackageUpdatedTask(String[] packages, String componentName, UserHandleCompat user, int iconId, String iconPath, int type) {
            this.mOp = OP_USER_UPDATE_ICON;
            this.mPackages = packages;
            this.mIconId = iconId;
            this.mIconPath = iconPath;
            this.mUser = user;
            this.mIconType = type;
            this.mComponentName = componentName;
        }

        public PackageUpdatedTask(String[] packages, String componentName, UserHandleCompat user, String appLabel) {
            this.mOp = OP_USER_UPDATE_LABEL;
            this.mPackages = packages;
            this.mAppLabel = appLabel;
            this.mUser = user;
            this.mComponentName = componentName;
        }

        public PackageUpdatedTask(String[] packages, String componentName, UserHandleCompat user, int category) {
            this.mOp = OP_USER_UPDATE_CATEGORY;
            this.mPackages = packages;
            this.mComponentName = componentName;
            this.mUser = user;
            this.mCategory = category;
        }

        @Override
        public void run() {
            if (!mHasLoaderCompletedOnce) {
                // Loader has not yet run.
                return;
            }
            final Context context = mApp.getContext();

            final String[] packages = mPackages;
            final int N = packages.length;

            // 当前主题包删除,恢复至默认主题;
            if (packages[0].equals(Settings.getThemePackageName(context)) && mOp == OP_REMOVE) {
                mApp.applyNewTheme(ThemeConfig.getDefaultThemePkg());
                return;
            } else if (packages[0].equals(LeftCustomContentUtil.getCustomPkg())) {
                LeftCustomContentUtil.hasCustomContentToLeft(context, true);
            }

            String[] hideApps = Partner.getStringArray(context, Partner.DEF_HIDE_APP_LIST);

            FlagOp flagOp = FlagOp.NO_OP;
            StringFilter pkgFilter = StringFilter.of(new HashSet<>(Arrays.asList(packages)));
            switch (mOp) {
                case OP_ADD: {
                    for (int i = 0; i < N; i++) {
                        if (DEBUG_LOADERS) Log.d(TAG, "mAllAppsList.addPackage " + packages[i]);

                        if (LeftCustomContentUtil.NEWS_PAGE_PACKAGE_NAME.equals(packages[i])) {
                            LeftCustomContentUtil.hasCustomContentToLeft(context, true);
                            LeftCustomContentUtil.isNewsPageInstall = true;
                        }

                        if (isHideApp(packages[i], hideApps)) {
                            continue;
                        }

                        mIconCache.updateIconsForPkg(packages[i], mUser);
                        mBgAllAppsList.addPackage(context, packages[i], mUser);
                    }

                    ManagedProfileHeuristic heuristic = ManagedProfileHeuristic.get(context, mUser);
                    if (heuristic != null) {
                        heuristic.processPackageAdd(mPackages);
                    }
                    break;
                }
                case OP_UPDATE:
                    for (int i = 0; i < N; i++) {
                        if (isHideApp(packages[i], hideApps)) {
                            continue;
                        }

                        if (DEBUG_LOADERS) Log.d(TAG, "mAllAppsList.updatePackage " + packages[i]);
                        mIconCache.updateIconsForPkg(packages[i], mUser);
                        mBgAllAppsList.updatePackage(context, packages[i], mUser);
                        mApp.getWidgetCache().removePackage(packages[i], mUser);
                    }
                    // Since package was just updated, the target must be available now.
                    flagOp = FlagOp.removeFlag(ShortcutInfo.FLAG_DISABLED_NOT_AVAILABLE);

                    if (IOSOtaHandler.ENABLE_OTA_2_FEATURE) {
                        for (int i = 0; i < N; i++) {
                            if (IOSOtaHandler.OTA2_PACKAGE_NAME.equals(packages[i])) {
                                IOSOtaHandler.updateExternalHideList(mApp.getContext());

                                List<String> hideAppList = IOSOtaHandler.getExternalHideList();
                                enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_REMOVE,
                                        hideAppList.toArray(new String[hideAppList.size()]), mUser));
                                break;
                            }
                        }
                    }
                    break;
                case OP_REMOVE: {
                    ManagedProfileHeuristic heuristic = ManagedProfileHeuristic.get(context, mUser);
                    if (heuristic != null) {
                        heuristic.processPackageRemoved(mPackages);
                    }
                    for (int i = 0; i < N; i++) {
                        if (DEBUG_LOADERS) Log.d(TAG, "mAllAppsList.removePackage " + packages[i]);
                        mIconCache.removeIconsForPkg(packages[i], mUser);
                    }
                    // Fall through
                }
                case OP_UNAVAILABLE:
                    for (int i = 0; i < N; i++) {
                        if (DEBUG_LOADERS) Log.d(TAG, "mAllAppsList.removePackage " + packages[i]);
                        mBgAllAppsList.removePackage(packages[i], mUser);
                        mApp.getWidgetCache().removePackage(packages[i], mUser);
                    }
                    flagOp = FlagOp.addFlag(ShortcutInfo.FLAG_DISABLED_NOT_AVAILABLE);
                    break;
                case OP_SUSPEND:
                case OP_UNSUSPEND:
                    flagOp = mOp == OP_SUSPEND ?
                            FlagOp.addFlag(ShortcutInfo.FLAG_DISABLED_SUSPENDED) :
                            FlagOp.removeFlag(ShortcutInfo.FLAG_DISABLED_SUSPENDED);
                    if (DEBUG_LOADERS) Log.d(TAG, "mAllAppsList.(un)suspend " + N);
                    mBgAllAppsList.updatePackageFlags(pkgFilter, mUser, flagOp);
                    break;
                case OP_USER_AVAILABILITY_CHANGE:
                    //Log.e("zwb"," OP_USER_AVAILABILITY_CHANGE " +  UserManagerCompat.getInstance(context).isQuietModeEnabled(mUser));
                    flagOp = UserManagerCompat.getInstance(context).isQuietModeEnabled(mUser)
                            ? FlagOp.addFlag(ShortcutInfo.FLAG_DISABLED_QUIET_USER)
                            : FlagOp.removeFlag(ShortcutInfo.FLAG_DISABLED_QUIET_USER);
                    // We want to update all packages for this user.
                    pkgFilter = StringFilter.matchesAll();
                    mBgAllAppsList.updatePackageFlags(pkgFilter, mUser, flagOp);
                    break;
                case OP_ADD_ENTRY:
                    for (int i = 0; i < N; i++) {
                        if (DEBUG_LOADERS)
                            Log.d(TAG, "mAllAppsList.addPackageEntry " + packages[i]);
                        mIconCache.updateIconsForPkg(packages[i], mUser);
                        mBgAllAppsList.addPackageEntry(context, packages[i], mUser);
                    }

                    ManagedProfileHeuristic heuristicAdd = ManagedProfileHeuristic.get(context, mUser);
                    if (heuristicAdd != null) {
                        heuristicAdd.processPackageAdd(mPackages);
                    }
                    break;
                case OP_REMOVE_ENTRY:
                    ManagedProfileHeuristic heuristicRemove = ManagedProfileHeuristic.get(context, mUser);
                    if (heuristicRemove != null) {
                        heuristicRemove.processPackageRemoved(mPackages);
                    }
                    for (int i = 0; i < N; i++) {
                        if (DEBUG_LOADERS)
                            Log.d(TAG, "mAllAppsList.removePackageEntry " + packages[i]);
                        mBgAllAppsList.removePackageEntry(packages[i], mUser);
                    }
                    break;
                case OP_USER_UPDATE_ICON:
                    for (String pkg : packages){
                        synchronized (IconCache.class){
                            mIconCache.removeIconsForPkg(mComponentName,mUser);
                            ComponentName componentName = null;
                            try{
                                componentName = ComponentName.unflattenFromString(mComponentName);
                            }
                            catch (Exception e){

                            }
                            if (componentName == null) continue;
                            try {
                                String pkgName = componentName.getPackageName();
                                PackageInfo packageInfo = mIconCache.mPackageManager.getPackageInfo(pkgName, 8192);
                                long userSerial = mUserManager.getSerialNumberForUser(mUser);
                                for (LauncherActivityInfoCompat infoCompat : mIconCache.mLauncherApps.getActivityList(pkgName,mUser)){
                                    if (infoCompat != null  && infoCompat.getComponentName() != null && mComponentName.equals(infoCompat.getComponentName().flattenToString())){
                                        mIconCache.setCustomAppIcon(
                                                infoCompat,
                                                packageInfo,
                                                userSerial,
                                                mIconId,
                                                mIconPath,
                                                mIconType
                                        );
                                    }
                                }
                            } catch (PackageManager.NameNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                        mBgAllAppsList.addPackage(context,pkg,mUser);
                    }
                    break;
                case OP_USER_UPDATE_CATEGORY:
                    synchronized (IconCache.class){
                        long userSerial = mUserManager.getSerialNumberForUser(mUser);
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(IconCache.IconDB.COLUMN_DATA2,String.valueOf(mCategory));
                        mIconCache.updateIconDB(
                                contentValues,
                                new String[]{mComponentName,String.valueOf(userSerial)}
                        );
                        mIconCache.setCustomizeCategory(
                                mComponentName,
                                mCategory
                        );
                    }
                    break;
                case OP_USER_UPDATE_LABEL:
                    for (String pkg : packages){
                        synchronized (IconCache.class){
                            long userSerial = mUserManager.getSerialNumberForUser(mUser);
                            ContentValues contentValues = new ContentValues();
                            contentValues.put("label", mAppLabel);
                            mIconCache.updateIconDB(
                                    contentValues,
                                    new String[]{mComponentName,String.valueOf(userSerial)}
                            );
                            mIconCache.setCustomAppLabel(
                                    mComponentName,
                                    mAppLabel
                            );
                        }
                    }
                    break;
                default:
            }

            ArrayList<AppInfo> added = null;
            ArrayList<AppInfo> modified = null;
            final ArrayList<AppInfo> removedApps = new ArrayList<AppInfo>();

            if (mBgAllAppsList.added.size() > 0) {
                added = new ArrayList<AppInfo>(mBgAllAppsList.added);
                mBgAllAppsList.added.clear();
            }
            if (mBgAllAppsList.modified.size() > 0) {
                modified = new ArrayList<AppInfo>(mBgAllAppsList.modified);
                mBgAllAppsList.modified.clear();
            }
            if (mBgAllAppsList.removed.size() > 0) {
                removedApps.addAll(mBgAllAppsList.removed);
                mBgAllAppsList.removed.clear();
            }

            final Callbacks callbacks = getCallback();
            if (callbacks == null) {
                Log.w(TAG, "Nobody to tell about the new app.  Launcher is probably loading.");
                return;
            }

            final HashMap<ComponentName, AppInfo> addedOrUpdatedApps =
                    new HashMap<ComponentName, AppInfo>();

            if (added != null) {
                // ensure that we add all the workspace applications to the db
                addAndBindAddedWorkspaceItems(context, added, true, true);
                addAppsToAllApps(context, added);
                for (AppInfo ai : added) {
                    addedOrUpdatedApps.put(ai.componentName, ai);
                }
            }

            if (modified != null) {
                final ArrayList<AppInfo> modifiedFinal = modified;
                for (AppInfo ai : modified) {
                    addedOrUpdatedApps.put(ai.componentName, ai);
                }

                mHandler.post(new Runnable() {
                    public void run() {
                        Callbacks cb = getCallback();
                        if (callbacks == cb && cb != null) {
                            callbacks.bindAppsUpdated(modifiedFinal);
                        }
                    }
                });
            }

            // Update shortcut infos
            if (mOp == OP_ADD || mOp == OP_UPDATE || flagOp != FlagOp.NO_OP) {
                final ArrayList<ShortcutInfo> updatedShortcuts = new ArrayList<ShortcutInfo>();
                final ArrayList<ShortcutInfo> removedShortcuts = new ArrayList<ShortcutInfo>();
                final ArrayList<LauncherAppWidgetInfo> widgets = new ArrayList<LauncherAppWidgetInfo>();

                HashSet<String> packageSet = new HashSet<String>(Arrays.asList(packages));
                synchronized (sBgLock) {
                    for (ItemInfo info : sBgItemsIdMap) {
                        if (info instanceof ShortcutInfo && mUser.equals(info.user)) {
                            ShortcutInfo si = (ShortcutInfo) info;
                            boolean infoUpdated = false;
                            boolean shortcutUpdated = false;

                            // Update shortcuts which use iconResource.
                            if ((si.iconResource != null)
                                    && packageSet.contains(si.iconResource.packageName)) {
                                Bitmap icon = Utilities.createIconBitmap(
                                        si.iconResource.packageName,
                                        si.iconResource.resourceName, context);
                                if (icon != null) {
                                    si.setIcon(icon);
                                    si.usingFallbackIcon = false;
                                    infoUpdated = true;
                                }
                            }

                            ComponentName cn = si.getTargetComponent();
                            if (cn != null && packageSet.contains(cn.getPackageName())) {
                                AppInfo appInfo = addedOrUpdatedApps.get(cn);

                                if (si.isPromise()) {
                                    if (si.hasStatusFlag(ShortcutInfo.FLAG_AUTOINTALL_ICON)) {
                                        // Auto install icon
                                        PackageManager pm = context.getPackageManager();
                                        ResolveInfo matched = pm.resolveActivity(
                                                new Intent(Intent.ACTION_MAIN)
                                                        .setComponent(cn).addCategory(Intent.CATEGORY_LAUNCHER),
                                                PackageManager.MATCH_DEFAULT_ONLY);
                                        if (matched == null) {
                                            // Try to find the best match activity.
                                            Intent intent = pm.getLaunchIntentForPackage(
                                                    cn.getPackageName());
                                            if (intent != null) {
                                                cn = intent.getComponent();
                                                appInfo = addedOrUpdatedApps.get(cn);
                                            }

                                            if ((intent == null) || (appInfo == null)) {
                                                removedShortcuts.add(si);
                                                continue;
                                            }
                                            si.promisedIntent = intent;
                                        }
                                    }

                                    // Restore the shortcut.
                                    if (appInfo != null) {
                                        si.flags = appInfo.flags;
                                    }

                                    si.intent = si.promisedIntent;
                                    si.promisedIntent = null;
                                    si.status = ShortcutInfo.DEFAULT;
                                    infoUpdated = true;
                                    si.updateIcon(mIconCache);
                                }

                                if (appInfo != null && Intent.ACTION_MAIN.equals(si.intent.getAction())
                                        && si.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                                    si.updateIcon(mIconCache);
                                    si.title = Utilities.trim(appInfo.title);
                                    si.contentDescription = appInfo.contentDescription;
                                    infoUpdated = true;
                                }

                                int oldDisabledFlags = si.isDisabled;
                                si.isDisabled = flagOp.apply(si.isDisabled);
                                if (si.isDisabled != oldDisabledFlags) {
                                    shortcutUpdated = true;
                                }
                            }

                            if (infoUpdated || shortcutUpdated) {
                                updatedShortcuts.add(si);
                            }
                            if (infoUpdated) {
                                updateItemInDatabase(context, si);
                            }
                        } else if (info instanceof LauncherAppWidgetInfo) {
                            LauncherAppWidgetInfo widgetInfo = (LauncherAppWidgetInfo) info;
                            if (mUser.equals(widgetInfo.user)
                                    && widgetInfo.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY)
                                    && packageSet.contains(widgetInfo.providerName.getPackageName())) {
                                widgetInfo.restoreStatus &=
                                        ~LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY &
                                                ~LauncherAppWidgetInfo.FLAG_RESTORE_STARTED;

                                // adding this flag ensures that launcher shows 'click to setup'
                                // if the widget has a config activity. In case there is no config
                                // activity, it will be marked as 'restored' during bind.
                                widgetInfo.restoreStatus |= LauncherAppWidgetInfo.FLAG_UI_NOT_READY;

                                widgets.add(widgetInfo);
                                updateItemInDatabase(context, widgetInfo);
                            }
                        }
                    }
                }

                if (!updatedShortcuts.isEmpty() || !removedShortcuts.isEmpty()) {
                    mHandler.post(new Runnable() {

                        public void run() {
                            Callbacks cb = getCallback();
                            if (callbacks == cb && cb != null) {
                                callbacks.bindShortcutsChanged(
                                        updatedShortcuts, removedShortcuts, mUser);
                            }
                        }
                    });
                    if (!removedShortcuts.isEmpty()) {
                        deleteItemsFromDatabase(context, removedShortcuts);
                    }
                }
                if (!widgets.isEmpty()) {
                    mHandler.post(new Runnable() {
                        public void run() {
                            Callbacks cb = getCallback();
                            if (callbacks == cb && cb != null) {
                                callbacks.bindWidgetsRestored(widgets);
                            }
                        }
                    });
                }
            }

            final ArrayList<String> removedPackageNames =
                    new ArrayList<String>();
            if (mOp == OP_REMOVE || mOp == OP_UNAVAILABLE) {
                // Mark all packages in the broadcast to be removed
                removedPackageNames.addAll(Arrays.asList(packages));
            } else if (mOp == OP_UPDATE) {
                // Mark disabled packages in the broadcast to be removed
                for (int i = 0; i < N; i++) {
                    if (isPackageDisabled(context, packages[i], mUser)) {
                        removedPackageNames.add(packages[i]);
                    }
                }
            }

            if (!removedPackageNames.isEmpty() || !removedApps.isEmpty()) {
                final int removeReason;
                if (mOp == OP_UNAVAILABLE) {
                    removeReason = ShortcutInfo.FLAG_DISABLED_NOT_AVAILABLE;
                } else {
                    // Remove all the components associated with this package
                    for (String pn : removedPackageNames) {
                        deletePackageFromDatabase(context, pn, mUser);
                    }
                    // Remove all the specific components
                    for (AppInfo a : removedApps) {
                        ArrayList<ItemInfo> infos = getItemInfoForComponentName(a.componentName, mUser);
                        deleteItemsFromDatabase(context, infos);
                    }
                    removeReason = 0;
                }

                // Remove any queued items from the install queue
                InstallShortcutReceiver.removeFromInstallQueue(context, removedPackageNames, mUser);
                // Call the components-removed callback
                mHandler.post(new Runnable() {
                    public void run() {
                        Callbacks cb = getCallback();
                        if (callbacks == cb && cb != null) {
                            callbacks.bindComponentsRemoved(
                                    removedPackageNames, removedApps, mUser, removeReason);
                        }
                    }
                });
            }

            // Update widgets
            if (mOp == OP_ADD || mOp == OP_REMOVE || mOp == OP_UPDATE) {
                // Always refresh for a package event on secondary user
                boolean needToRefresh = !mUser.equals(UserHandleCompat.myUserHandle());

                // Refresh widget list, if the package already had a widget.
                synchronized (sBgLock) {
                    if (sBgWidgetProviders != null) {
                        HashSet<String> pkgSet = new HashSet<>();
                        Collections.addAll(pkgSet, mPackages);

                        for (ComponentKey key : sBgWidgetProviders.keySet()) {
                            needToRefresh |= key.user.equals(mUser) &&
                                    pkgSet.contains(key.componentName.getPackageName());
                        }
                    }
                }

                if (!needToRefresh && mOp != OP_REMOVE) {
                    // Refresh widget list, if there is any newly added widget
                    PackageManager pm = context.getPackageManager();
                    List<ResolveInfo> widgetUpdateReceiver = null;
                    for (String pkg : mPackages) {
                        widgetUpdateReceiver = pm.queryBroadcastReceivers(
                                new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                                        .setPackage(pkg), 0);
                        if (widgetUpdateReceiver != null) {
                            needToRefresh |= !widgetUpdateReceiver.isEmpty();
                        }
                    }
                }

                loadAndBindWidgetsAndShortcuts(callbacks, needToRefresh);
            }

            // Write all the logs to disk
            mHandler.post(new Runnable() {
                public void run() {
                    Callbacks cb = getCallback();
                    if (callbacks == cb && cb != null) {
                        callbacks.dumpLogsToLocalData();
                    }
                }
            });
        }
    }

    public static List<LauncherAppWidgetProviderInfo> getWidgetProviders(Context context,
                                                                         boolean refresh) {
        ArrayList<LauncherAppWidgetProviderInfo> results =
                new ArrayList<LauncherAppWidgetProviderInfo>();
        WidgetsModel model = null;
        try {
            synchronized (sBgLock) {
                if (sBgWidgetProviders == null || refresh) {
                    HashMap<ComponentKey, LauncherAppWidgetProviderInfo> tmpWidgetProviders
                            = new HashMap<>();
                    AppWidgetManagerCompat wm = AppWidgetManagerCompat.getInstance(context);
                    LauncherAppWidgetProviderInfo info;

                    Collection<IOSAppWidget> customWidgets = Launcher.getIOSAppWidgets().values();
                    for (IOSAppWidget widget : customWidgets) {
                        info = new LauncherAppWidgetProviderInfo(context, widget);
                        UserHandleCompat user = wm.getUser(info);
                        tmpWidgetProviders.put(new ComponentKey(info.provider, user), info);
                    }

                    List<AppWidgetProviderInfo> widgets = wm.getAllProviders();
                    for (AppWidgetProviderInfo pInfo : widgets) {
                        info = LauncherAppWidgetProviderInfo.fromProviderInfo(context, pInfo);
                        UserHandleCompat user = wm.getUser(info);
                        tmpWidgetProviders.put(new ComponentKey(info.provider, user), info);
                    }


                    // Retrieve system broadcastreceivers,get all ios widget providers.
                    // Whick has a intent fileter with action : android.appwidget.action.IOS_WIDGET
                    PackageManager packageManager = context.getPackageManager();
                    List<ResolveInfo> queryInfos = packageManager.queryBroadcastReceivers(new Intent(
                            IOSAppWidget.IOS_APP_WIDGET_ACTION), PackageManager.GET_META_DATA);
                    ActivityInfo activityInfo;
                    boolean enableTorchWidget = Partner.getBoolean(context, Partner.FEATURE_TORCH_ENABLE);
                    for (int i = 0; i < queryInfos.size(); i = i + 1) {
                        activityInfo = queryInfos.get(i).activityInfo;
                        if (!enableTorchWidget && activityInfo.name.equals(Router.WIDGET_PROVIDER_TORCH)) {
                            continue;
                        }
                        try {
                            info = LauncherAppWidgetProviderInfo.fromIOSWidgetComponent(context, new ComponentName(activityInfo.packageName, activityInfo.name));
                            UserHandleCompat user = wm.getUser(info);
                            tmpWidgetProviders.put(new ComponentKey(info.provider, user), info);
                        } catch (Exception e) {
                            Log.e(TAG, "fromIOSWidgetComponent activityInfo fail : " + activityInfo, e);
                            //e.printStackTrace();
                        }
                    }

                    // Replace the global list at the very end, so that if there is an exception,
                    // previously loaded provider list is used.
                    sBgWidgetProviders = tmpWidgetProviders;
                }
                results.addAll(sBgWidgetProviders.values());
                return results;
            }
        } catch (Exception e) {
            if (e.getCause() instanceof TransactionTooLargeException) {
                // the returned value may be incomplete and will not be refreshed until the next
                // time Launcher starts.
                // TODO: after figuring out a repro step, introduce a dirty bit to check when
                // onResume is called to refresh the widget provider list.
                synchronized (sBgLock) {
                    if (sBgWidgetProviders != null) {
                        results.addAll(sBgWidgetProviders.values());
                    }
                    return results;
                }
            } else {
                throw e;
            }
        }
    }


    public static LauncherAppWidgetProviderInfo getProviderInfo(Context ctx, ComponentName name,
                                                                UserHandleCompat user) {
        synchronized (sBgLock) {
            if (sBgWidgetProviders == null) {
                getWidgetProviders(ctx, false /* refresh */);
            }
            return sBgWidgetProviders.get(new ComponentKey(name, user));
        }
    }

    public void loadAndBindWidgetsAndShortcuts(final Callbacks callbacks, final boolean refresh) {

        runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                updateWidgetsModel(refresh);
                final WidgetsModel model = mBgWidgetsModel.clone();

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Callbacks cb = getCallback();
                        if (callbacks == cb && cb != null) {
                            callbacks.bindAllPackages(model);
                        }
                    }
                });
                // update the Widget entries inside DB on the worker thread.
                LauncherAppState.getInstance().getWidgetCache().removeObsoletePreviews(
                        model.getRawList());

            }
        });
    }


    public List<AppInfo> getAllAppInfo() {
        return (ArrayList<AppInfo>) mBgAllAppsList.data.clone();
    }

    public List<ResolveInfo> getIOSShortcut() {
        return (ArrayList<ResolveInfo>) sExportIOSShortcuts.clone();
    }

    /**
     * Returns a list of ResolveInfos/AppWidgetInfos.
     *
     * @see #loadAndBindWidgetsAndShortcuts
     */
    @Thunk
    void updateWidgetsModel(boolean refresh) {
        PackageManager packageManager = mApp.getContext().getPackageManager();

        final ArrayList<Object> widgetsAndShortcuts = new ArrayList<Object>();
        widgetsAndShortcuts.addAll(getWidgetProviders(mApp.getContext(), refresh));

        Intent shortcutsIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        widgetsAndShortcuts.addAll(packageManager.queryIntentActivities(shortcutsIntent, 0));

        /*
        Intent iosShortcutsIntent = new Intent(IOSShortcut.ACTION_CREATE_IOS_SHORTCUT);
        final List<ResolveInfo> iosShortcuts = packageManager.queryIntentActivities(iosShortcutsIntent, 0);

        boolean enableBatterySaveShortcut = Partner.getBoolean(mApp.getContext(), Partner.FEATURE_BATTERY_SAVE_ENABLE);
        boolean enableQuickSettingsShortcut = Partner.getBoolean(mApp.getContext(), Partner.FEATURE_QUICK_SETTINGS_ENABLE, true);
        Iterator<ResolveInfo> resolverInfoIter = iosShortcuts.iterator();
        while (resolverInfoIter.hasNext()) {
            ResolveInfo info = resolverInfoIter.next();
            if (info.activityInfo.name.equals(Router.SHORTCUT_BATTERY_SAVE) && !enableBatterySaveShortcut) {
                resolverInfoIter.remove();
            }
            if (info.activityInfo.name.equals(Router.SHORTCUT_QUICK_SETTINGS) && !enableQuickSettingsShortcut) {
                resolverInfoIter.remove();
            }
        }


        sExportIOSShortcuts.addAll(iosShortcuts);
         */

        sExportIOSShortcuts.clear();
//        widgetsAndShortcuts.addAll(iosWidgets);

        mBgWidgetsModel.setWidgetsAndShortcuts(widgetsAndShortcuts);

    }

    @Thunk
    static boolean isPackageDisabled(Context context, String packageName,
                                     UserHandleCompat user) {
        final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
        return !launcherApps.isPackageEnabledForProfile(packageName, user);
    }

    public static boolean isValidPackageActivity(Context context, ComponentName cn,
                                                 UserHandleCompat user) {
        if (cn == null) {
            return false;
        }
        final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
        if (!launcherApps.isPackageEnabledForProfile(cn.getPackageName(), user)) {
            return false;
        }
        return launcherApps.isActivityEnabledForProfile(cn, user);
    }

    public static boolean isValidPackage(Context context, String packageName,
                                         UserHandleCompat user) {
        if (packageName == null) {
            return false;
        }
        final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
        return launcherApps.isPackageEnabledForProfile(packageName, user);
    }

    public static boolean isHideApp(String packageName, String[] hideApps) {
        if (!TextUtils.isEmpty(packageName) && hideApps != null) {
            for (int i = 0; i < hideApps.length; i++) {
                if (packageName.equals(hideApps[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isInHiddenFolder(String packageName, ArrayList<String> hiddenApps) {
        if (hiddenApps == null)
            return false;

        for (int i = 0; i < hiddenApps.size(); i ++) {
            if (packageName.equals(hiddenApps.get(i))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Make an ShortcutInfo object for a restored application or shortcut item that points
     * to a package that is not yet installed on the system.
     */
    public ShortcutInfo getRestoredItemInfo(Cursor c, int titleIndex, Intent intent,
                                            int promiseType, int itemType, CursorIconInfo iconInfo, Context context) {
        final ShortcutInfo info = new ShortcutInfo();
        info.user = UserHandleCompat.myUserHandle();

        Bitmap icon = iconInfo.loadIcon(c, info, context);
        // the fallback icon
        if (icon == null) {
            mIconCache.getTitleAndIcon(info, intent, info.user, false /* useLowResIcon */);
        } else {
            info.setIcon(icon);
        }

        if ((promiseType & ShortcutInfo.FLAG_RESTORED_ICON) != 0) {
            String title = (c != null) ? c.getString(titleIndex) : null;
            if (!TextUtils.isEmpty(title)) {
                info.title = Utilities.trim(title);
            }
        } else if ((promiseType & ShortcutInfo.FLAG_AUTOINTALL_ICON) != 0) {
            if (TextUtils.isEmpty(info.title)) {
                info.title = (c != null) ? Utilities.trim(c.getString(titleIndex)) : "";
            }
        } else {
            throw new InvalidParameterException("Invalid restoreType " + promiseType);
        }

        info.contentDescription = mUserManager.getBadgedLabelForUser(info.title, info.user);
        info.itemType = itemType;
        info.promisedIntent = intent;
        info.status = promiseType;
        return info;
    }

    /**
     * Make an Intent object for a restored application or shortcut item that points
     * to the market page for the item.
     */
    @Thunk
    Intent getRestoredItemIntent(Cursor c, Context context, Intent intent) {
        ComponentName componentName = intent.getComponent();
        return getMarketIntent(componentName.getPackageName());
    }

    static Intent getMarketIntent(String packageName) {
        return new Intent(Intent.ACTION_VIEW)
                .setData(new Uri.Builder()
                        .scheme("market")
                        .authority("details")
                        .appendQueryParameter("id", packageName)
                        .build());
    }

    /**
     * Make an ShortcutInfo object for a shortcut that is an application.
     * <p/>
     * If c is not null, then it will be used to fill in missing data like the title and icon.
     */
    public ShortcutInfo getAppShortcutInfo(PackageManager packageManager, Intent intent,
                                           UserHandleCompat user, Context context, Cursor c, int iconIndex, int titleIndex,
                                           boolean allowMissingTarget, boolean useLowResIcon) {
        if (user == null) {
            Log.d(TAG, "Null user found in getShortcutInfo");
            return null;
        }

        ComponentName componentName = intent.getComponent();
        if (componentName == null) {
            Log.d(TAG, "Missing component found in getShortcutInfo: " + componentName);
            return null;
        }

        Intent newIntent = new Intent(intent.getAction(), null);
        newIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        newIntent.setComponent(componentName);
        LauncherActivityInfoCompat lai = mLauncherApps.resolveActivity(newIntent, user);
        if ((lai == null) && !allowMissingTarget) {
            Log.d(TAG, "Missing activity found in getShortcutInfo: " + componentName);
            return null;
        }

        final AppInfo info = new AppInfo();
        info.componentName = componentName;
        mIconCache.getTitleAndIcon(info, componentName, lai, user, false, useLowResIcon);
        if (mIconCache.isDefaultIcon(info.getIcon(mIconCache), user) && c != null) {
            Bitmap icon = Utilities.createIconBitmap(c, iconIndex, context);
            info.setIcon(icon == null ? mIconCache.getDefaultIcon(user) : icon);
        }
        // from the db
        if (lai != null) {
            info.title = lai.getLabel();
        }

        if (lai != null && PackageManagerHelper.isAppSuspended(lai.getApplicationInfo())) {
            info.isDisabled = ShortcutInfo.FLAG_DISABLED_SUSPENDED;
        }

        // fall back to the class name of the activity
        if (info.title == null) {
            info.title = componentName.getClassName();
        }

        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
        info.user = user;
        info.contentDescription = mUserManager.getBadgedLabelForUser(info.title, info.user);
        info.uninstallable = true;
        if (lai != null) {
            info.flags = AppInfo.initFlags(context, lai);
            info.appFlags = lai.getApplicationInfo().flags;
            if (info.flags == 0) {
                info.uninstallable = false;
            }
        }

        return info;
    }

    static ArrayList<ItemInfo> filterItemInfos(Iterable<ItemInfo> infos,
                                               ItemInfoFilter f) {
        HashSet<ItemInfo> filtered = new HashSet<ItemInfo>();
        for (ItemInfo i : infos) {
            if (i instanceof ShortcutInfo) {
                ShortcutInfo info = (ShortcutInfo) i;
                ComponentName cn = info.getTargetComponent();
                if (cn != null && f.filterItem(null, info, cn)) {
                    filtered.add(info);
                }
            } else if (i instanceof FolderInfo) {
                FolderInfo info = (FolderInfo) i;
                for (ShortcutInfo s : info.contents) {
                    ComponentName cn = s.getTargetComponent();
                    if (cn != null && f.filterItem(info, s, cn)) {
                        filtered.add(s);
                    }
                }
            } else if (i instanceof LauncherAppWidgetInfo) {
                LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) i;
                ComponentName cn = info.providerName;
                if (cn != null && f.filterItem(null, info, cn)) {
                    filtered.add(info);
                }
            }
        }
        return new ArrayList<ItemInfo>(filtered);
    }

    @Thunk
    ArrayList<ItemInfo> getItemInfoForComponentName(final ComponentName cname,
                                                    final UserHandleCompat user) {
        ItemInfoFilter filter = new ItemInfoFilter() {
            @Override
            public boolean filterItem(ItemInfo parent, ItemInfo info, ComponentName cn) {
                if (info.user == null) {
                    return cn.equals(cname);
                } else {
                    return cn.equals(cname) && info.user.equals(user);
                }
            }
        };
        return filterItemInfos(sBgItemsIdMap, filter);
    }

    /**
     * Make an ShortcutInfo object for a shortcut that isn't an application.
     */
    @Thunk
    ShortcutInfo getShortcutInfo(Intent intent, Cursor c, Context context,
                                 int titleIndex, CursorIconInfo iconInfo) {
        UserHandleCompat user = UserHandleCompat.myUserHandle();
        final ShortcutInfo info = new ShortcutInfo();
        // TODO: If there's an explicit component and we can't install that, delete it.
        ComponentName componentName = intent.getComponent();
        if (componentName != null && componentName.getPackageName().equals(mApp.getContext().getPackageName())) {
            LauncherActivityInfoCompat lai = mLauncherApps.resolveActivity(intent, UserHandleCompat.myUserHandle());
            if (lai == null) {
                Log.d(TAG, "Missing activity found in getShortcutInfo: " + componentName);
                return null;
            }
            mIconCache.getTitleAndIconForIOSShortcut(info, componentName, lai, user, false);
        } else {
            Bitmap icon = iconInfo.loadIcon(c, info, context);
            // the fallback icon
            if (icon == null) {
                icon = mIconCache.getDefaultIcon(info.user);
                info.usingFallbackIcon = true;
            }
            info.setIcon(icon);
            info.title = Utilities.trim(c.getString(titleIndex));
        }

        // Non-app shortcuts are only supported for current user.
        info.uninstallable = true;
        info.user = user;
        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
        info.contentDescription = mUserManager.getBadgedLabelForUser(info.title, info.user);
        return info;
    }

    /**
     * Make an Virutal App object for a shortcut that isn't an application.
     */
    ShortcutInfo getVirtualAppInfo(Intent intent, Cursor c, Context context,
                                 int titleIndex, CursorIconInfo iconInfo) {
        UserHandleCompat user = UserHandleCompat.myUserHandle();
        final ShortcutInfo info = new ShortcutInfo();
        // TODO: If there's an explicit component and we can't install that, delete it.
        int shortcutId = intent.getIntExtra(Shortcuts.SHORTCUT_ID, -1);
        String iconName = Shortcuts.getShortcutResName(shortcutId);

        Bitmap icon = null;
        Drawable drawableIcon = null;

        if (drawableIcon == null) {
            icon = Utilities.createIconBitmap(R.drawable.lite_rom_hide_app, context);
        } else {
            icon = Utilities.createIconBitmap(drawableIcon, context);
        }

        // the fallback icon
        if (icon == null) {
            icon = mIconCache.getDefaultIcon(info.user);
            info.usingFallbackIcon = true;
        }

        info.setIcon(icon);
        info.title = Utilities.trim(c.getString(titleIndex));

        // Non-app shortcuts are only supported for current user.
        info.uninstallable = false;
        info.user = user;
        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_VIRTUAL_APP;
        info.contentDescription = mUserManager.getBadgedLabelForUser(info.title, info.user);
        return info;
    }

    ShortcutInfo infoFromIOSShortcutIntent(Context context, Intent intent) {
        ComponentName componentName = intent.getComponent();
        LauncherActivityInfoCompat lai = mLauncherApps.resolveActivity(intent, UserHandleCompat.myUserHandle());
        if (lai == null) {
            Log.d(TAG, "Missing activity found in getShortcutInfo: " + componentName);
            return null;
        }

        final ShortcutInfo info = new ShortcutInfo();
        UserHandleCompat user = UserHandleCompat.myUserHandle();
        mIconCache.getTitleAndIconForIOSShortcut(info, componentName, lai, user, false);

        Intent launchIntent = new Intent().setComponent(componentName);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        info.intent = launchIntent;
        info.user = user;
        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
        info.contentDescription = mUserManager.getBadgedLabelForUser(info.title, info.user);
        return info;
    }

    ShortcutInfo infoFromShortcutIntent(Context context, Intent data) {
        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Parcelable bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);

        if (intent == null) {
            // If the intent is null, we can't construct a valid ShortcutInfo, so we return null
            Log.e(TAG, "Can't construct ShorcutInfo with null intent");
            return null;
        }

        Bitmap icon = null;
        boolean customIcon = false;
        ShortcutIconResource iconResource = null;

        if (bitmap instanceof Bitmap) {
            icon = Utilities.createIconBitmap((Bitmap) bitmap, context);
            customIcon = true;
        } else {
            Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (extra instanceof ShortcutIconResource) {
                iconResource = (ShortcutIconResource) extra;
                icon = Utilities.createIconBitmap(iconResource.packageName,
                        iconResource.resourceName, context);
            }
        }

        final ShortcutInfo info = new ShortcutInfo();

        // Only support intents for current user for now. Intents sent from other
        // users wouldn't get here without intent forwarding anyway.
        info.user = UserHandleCompat.myUserHandle();
        if (icon == null) {
            icon = mIconCache.getDefaultIcon(info.user);
            info.usingFallbackIcon = true;
        }
        info.setIcon(icon);

        info.title = Utilities.trim(name);
        info.contentDescription = mUserManager.getBadgedLabelForUser(info.title, info.user);
        info.intent = intent;
        info.customIcon = customIcon;
        info.iconResource = iconResource;

        return info;
    }

    /**
     * Return an existing FolderInfo object if we have encountered this ID previously,
     * or make a new one.
     */
    @Thunk
    static FolderInfo findOrMakeFolder(LongArrayMap<FolderInfo> folders, long id) {
        // See if a placeholder was created for us already
        FolderInfo folderInfo = folders.get(id);
        if (folderInfo == null) {
            // No placeholder -- create a new instance
            folderInfo = new FolderInfo();
            folders.put(id, folderInfo);
        }
        return folderInfo;
    }

    void enqueueModelUpdateTask(BaseModelUpdateTask task) {
        if (!mWorkspaceLoaded && mLoaderTask == null) {
            if (DEBUG_LOADERS) {
                Log.d(TAG, "enqueueModelUpdateTask Ignoring task since loader is pending=" + task);
            }
            return;
        }
        task.init(this);
        runOnWorkerThread(task);
    }

    /**
     * A task to be executed on the current callbacks on the UI thread.
     * If there is no current callbacks, the task is ignored.
     */
    public interface CallbackTask {

        void execute(Callbacks callbacks);
    }

    /**
     * A runnable which changes/updates the data model of the launcher based on certain events.
     */
    public interface ModelUpdateTask extends Runnable {

        /**
         * Called before the task is posted to initialize the internal state.
         */
        void init(LauncherAppState app, LauncherModel model,
                  BgDataModel dataModel, AllAppsList allAppsList, Executor uiExecutor);

    }

    /**
     * A runnable which changes/updates the data model of the launcher based on certain events.
     */
    public static abstract class BaseModelUpdateTask implements Runnable {

        protected LauncherModel mModel;
        protected DeferredHandler mUiHandler;

        /* package private */
        void init(LauncherModel model) {
            mModel = model;
            mUiHandler = mModel.mHandler;
        }

        @Override
        public void run() {
            if (!mModel.mHasLoaderCompletedOnce) {
                // Loader has not yet run.
                return;
            }
            execute(mModel.mApp, sBgDataModel, mModel.mBgAllAppsList);
        }

        /**
         * Execute the actual task. Called on the worker thread.
         */
        public abstract void execute(
                LauncherAppState app, BgDataModel dataModel, AllAppsList apps);

        /**
         * Schedules a {@param task} to be executed on the current callbacks.
         */
        public final void scheduleCallbackTask(final CallbackTask task) {
            final Callbacks callbacks = mModel.getCallback();
            mUiHandler.post(new Runnable() {
                public void run() {
                    Callbacks cb = mModel.getCallback();
                    if (callbacks == cb && cb != null) {
                        task.execute(callbacks);
                    }
                }
            });
        }

        public void bindUpdatedShortcuts(
                ArrayList<ShortcutInfo> updatedShortcuts, UserHandleCompat user) {
            bindUpdatedShortcuts(updatedShortcuts, new ArrayList<ShortcutInfo>(), user);
        }

        public void bindUpdatedShortcuts(
                final ArrayList<ShortcutInfo> updatedShortcuts,
                final ArrayList<ShortcutInfo> removedShortcuts,
                final UserHandleCompat user) {
            if (!updatedShortcuts.isEmpty() || !removedShortcuts.isEmpty()) {
                scheduleCallbackTask(new CallbackTask() {
                    @Override
                    public void execute(Callbacks callbacks) {
                        callbacks.bindShortcutsChanged(updatedShortcuts, removedShortcuts, user);
                    }
                });
            }
        }

        public void bindDeepShortcuts(BgDataModel dataModel) {
            final MultiHashMap<ComponentKey, String> shortcutMapCopy = dataModel.deepShortcutMap.clone();
            scheduleCallbackTask(new CallbackTask() {
                @Override
                public void execute(Callbacks callbacks) {
                    callbacks.bindDeepShortcutMap(shortcutMapCopy);
                }
            });
        }

        public void deleteAndBindComponentsRemoved(final ItemInfoMatcher matcher) {
            mModel.deleteItemsFromDatabase(matcher);

            // Call the components-removed callback
            scheduleCallbackTask(new CallbackTask() {
                @Override
                public void execute(Callbacks callbacks) {
                    callbacks.bindWorkspaceComponentsRemoved(matcher);
                }
            });
        }
    }

    public class ShortcutsChangedTask extends BaseModelUpdateTask {

        private final String mPackageName;
        private final List<ShortcutInfoCompat> mShortcuts;
        private final UserHandleCompat mUser;
        private final boolean mUpdateIdMap;

        public ShortcutsChangedTask(String packageName, List<ShortcutInfoCompat> shortcuts,
                                    UserHandleCompat user, boolean updateIdMap) {
            mPackageName = packageName;
            mShortcuts = shortcuts;
            mUser = user;
            mUpdateIdMap = updateIdMap;
        }

        @Override
        public void execute(LauncherAppState app, BgDataModel dataModel, AllAppsList apps) {
            final Context context = app.getContext();
            DeepShortcutManager deepShortcutManager = DeepShortcutManager.getInstance(context);
            deepShortcutManager.onShortcutsChanged(mShortcuts);

            // Find ShortcutInfo's that have changed on the workspace.
            HashSet<ShortcutKey> removedKeys = new HashSet<>();
            MultiHashMap<ShortcutKey, ShortcutInfo> keyToShortcutInfo = new MultiHashMap<>();
            HashSet<String> allIds = new HashSet<>();

            for (ItemInfo itemInfo : sBgItemsIdMap) {
                if (itemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT) {
                    ShortcutInfo si = (ShortcutInfo) itemInfo;
                    if (si.getIntent().getPackage().equals(mPackageName) && si.user.equals(mUser)) {
                        keyToShortcutInfo.addToList(ShortcutKey.fromItemInfo(si), si);
                        allIds.add(si.getDeepShortcutId());
                    }
                }
            }

            final ArrayList<ShortcutInfo> updatedShortcutInfos = new ArrayList<>();
            if (!keyToShortcutInfo.isEmpty()) {
                // Update the workspace to reflect the changes to updated shortcuts residing on it.
                List<ShortcutInfoCompat> shortcuts = deepShortcutManager.queryForFullDetails(
                        mPackageName, new ArrayList<>(allIds), mUser.getUser());
                for (ShortcutInfoCompat fullDetails : shortcuts) {
                    ShortcutKey key = ShortcutKey.fromInfo(fullDetails);
                    List<ShortcutInfo> shortcutInfos = keyToShortcutInfo.remove(key);
                    if (!fullDetails.isPinned()) {
                        // The shortcut was previously pinned but is no longer, so remove it from
                        // the workspace and our pinned shortcut counts.
                        // Note that we put this check here, after querying for full details,
                        // because there's a possible race condition between pinning and
                        // receiving this callback.
                        removedKeys.add(key);
                        continue;
                    }
                    for (final ShortcutInfo shortcutInfo : shortcutInfos) {
                        shortcutInfo.updateFromDeepShortcutInfo(fullDetails, context);
                        shortcutInfo.setIcon(LauncherIcons.createShortcutIcon(fullDetails, context));
                        updatedShortcutInfos.add(shortcutInfo);
                    }
                }
            }

            // If there are still entries in keyToShortcutInfo, that means that
            // the corresponding shortcuts weren't passed in onShortcutsChanged(). This
            // means they were cleared, so we remove and unpin them now.
            removedKeys.addAll(keyToShortcutInfo.keySet());

            bindUpdatedShortcuts(updatedShortcutInfos, mUser);
            if (!keyToShortcutInfo.isEmpty()) {
                deleteAndBindComponentsRemoved(ItemInfoMatcher.ofShortcutKeys(removedKeys));
            }

            if (mUpdateIdMap) {
                // Update the deep shortcut map if the list of ids has changed for an activity.
                dataModel.updateDeepShortcutMap(mPackageName, mUser.getUser(), mShortcuts);
                bindDeepShortcuts(dataModel);
            }
        }
    }

    public void updateAndBindShortcutInfo(final ShortcutInfo si, final ShortcutInfoCompat info) {
        updateAndBindShortcutInfo(new Provider<ShortcutInfo>() {
            @Override
            public ShortcutInfo get() {
                si.updateFromDeepShortcutInfo(info, mApp.getContext());
                si.setIcon(LauncherIcons.createShortcutIcon(info, mApp.getContext()));
                return si;
            }
        });
    }

    /**
     * Utility method to update a shortcut on the background thread.
     */
    public void updateAndBindShortcutInfo(final Provider<ShortcutInfo> shortcutProvider) {
        enqueueModelUpdateTask(new BaseModelUpdateTask() {
            @Override
            public void execute(LauncherAppState app, BgDataModel dataModel, AllAppsList apps) {
                ShortcutInfo info = shortcutProvider.get();
                ArrayList<ShortcutInfo> update = new ArrayList<>();
                update.add(info);
                bindUpdatedShortcuts(update, info.user);
            }
        });
    }

    static boolean isValidProvider(AppWidgetProviderInfo provider) {
        return (provider != null) && (provider.provider != null)
                && (provider.provider.getPackageName() != null);
    }

    public synchronized void dump(String prefix, FileDescriptor fd, PrintWriter writer,
                                  String[] args) {
        if (args.length > 0 && TextUtils.equals(args[0], "--proto")) {
            dumpProto(prefix, fd, writer, args);
            return;
        }
        writer.println(prefix + "Data Model:");
        writer.print(prefix + " ---- workspace screens: ");
        for (int i = 0; i < sBgWorkspaceScreens.size(); i++) {
            writer.print(" " + sBgWorkspaceScreens.get(i).toString());
        }
        writer.println();
        writer.println(prefix + " ---- workspace items ");
        for (int i = 0; i < sBgWorkspaceItems.size(); i++) {
            writer.println(prefix + '\t' + sBgWorkspaceItems.get(i).toString());
        }
        writer.println(prefix + " ---- appwidget items ");
        for (int i = 0; i < sBgAppWidgets.size(); i++) {
            writer.println(prefix + '\t' + sBgAppWidgets.get(i).toString());
        }
        writer.println(prefix + " ---- folder items ");
        for (int i = 0; i< sBgFolders.size(); i++) {
            writer.println(prefix + '\t' + sBgFolders.valueAt(i).toString());
        }
        writer.println(prefix + " ---- items id map ");
        for (int i = 0; i< sBgItemsIdMap.size(); i++) {
            writer.println(prefix + '\t' + sBgItemsIdMap.valueAt(i).toString());
        }

        if (args.length > 0 && TextUtils.equals(args[0], "--all")) {
            writer.println(prefix + "shortcuts");
            for (ArrayList<String> map : sBgDataModel.deepShortcutMap.values()) {
                writer.print(prefix + "  ");
                for (String str : map) {
                    writer.print(str + ", ");
                }
                writer.println();
            }
        }
    }

    private synchronized void dumpProto(String prefix, FileDescriptor fd, PrintWriter writer,
                                        String[] args) {

        // Add top parent nodes. (L1)
//        DumpTargetWrapper hotseat = new DumpTargetWrapper(LauncherDumpProto.ContainerType.HOTSEAT, 0);
//        LongArrayMap<DumpTargetWrapper> workspaces = new LongArrayMap<>();
//        for (int i = 0; i < sBgWorkspaceScreens.size(); i++) {
//            workspaces.put(sBgWorkspaceScreens.get(i),
//                    new DumpTargetWrapper(LauncherDumpProto.ContainerType.WORKSPACE, i));
//        }
//        DumpTargetWrapper dtw;
//        // Add non leaf / non top nodes (L2)
//        for (int i = 0; i < sBgFolders.size(); i++) {
//            FolderInfo fInfo = sBgFolders.valueAt(i);
//            dtw = new DumpTargetWrapper(LauncherDumpProto.ContainerType.FOLDER, sBgFolders.size());
//            dtw.writeToDumpTarget(fInfo);
//            for(ShortcutInfo sInfo: fInfo.contents) {
//                DumpTargetWrapper child = new DumpTargetWrapper(sInfo);
//                child.writeToDumpTarget(sInfo);
//                dtw.add(child);
//            }
//            if (fInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
//                hotseat.add(dtw);
//            } else if (fInfo.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
//                workspaces.get(fInfo.screenId).add(dtw);
//            }
//        }
//        // Add leaf nodes (L3): *Info
//        for (int i = 0; i < sBgWorkspaceItems.size(); i++) {
//            ItemInfo info = sBgWorkspaceItems.get(i);
//            if (info instanceof FolderInfo) {
//                continue;
//            }
//            dtw = new DumpTargetWrapper(info);
//            dtw.writeToDumpTarget(info);
//            if (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
//                hotseat.add(dtw);
//            } else if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
//                workspaces.get(info.screenId).add(dtw);
//            }
//        }
//        for (int i = 0; i < sBgAppWidgets.size(); i++) {
//            ItemInfo info = sBgAppWidgets.get(i);
//            dtw = new DumpTargetWrapper(info);
//            dtw.writeToDumpTarget(info);
//            if (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
//                hotseat.add(dtw);
//            } else if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
//                workspaces.get(info.screenId).add(dtw);
//            }
//        }
//
//
//        // Traverse target wrapper
//        ArrayList<LauncherDumpProto.DumpTarget> targetList = new ArrayList<>();
//        targetList.addAll(hotseat.getFlattenedList());
//        for (int i = 0; i < workspaces.size(); i++) {
//            targetList.addAll(workspaces.valueAt(i).getFlattenedList());
//        }
//
//        if (args.length > 1 && TextUtils.equals(args[1], "--debug")) {
//            for (int i = 0; i < targetList.size(); i++) {
//                writer.println(prefix + DumpTargetWrapper.getDumpTargetStr(targetList.get(i)));
//            }
//            return;
//        } else {
//            LauncherDumpProto.LauncherImpression proto = new LauncherDumpProto.LauncherImpression();
//            proto.targets = new LauncherDumpProto.DumpTarget[targetList.size()];
//            for (int i = 0; i < targetList.size(); i++) {
//                proto.targets[i] = targetList.get(i);
//            }
//            FileOutputStream fos = new FileOutputStream(fd);
//            try {
//
//                fos.write(MessageNano.toByteArray(proto));
//                Log.d(TAG, MessageNano.toByteArray(proto).length + "Bytes");
//            } catch (IOException e) {
//                Log.e(TAG, "Exception writing dumpsys --proto", e);
//            }
//        }
    }

    public void dumpState(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        if (args.length > 0 && TextUtils.equals(args[0], "--all")) {
            writer.println(prefix + "All apps list: size=" + mBgAllAppsList.data.size());
            for (AppInfo info : mBgAllAppsList.data) {
                writer.println(prefix + "   title=\"" + info.title + "\" iconBitmap=" + info.iconBitmap
                        + " componentName=" + info.componentName.getPackageName());
            }
        }
        dump(prefix, fd, writer, args);
    }

    public void dumpState() {
        Log.d(TAG, "mCallbacks=" + mCallbacks);
        AppInfo.dumpApplicationInfoList(TAG, "mAllAppsList.data", mBgAllAppsList.data);
        AppInfo.dumpApplicationInfoList(TAG, "mAllAppsList.added", mBgAllAppsList.added);
        AppInfo.dumpApplicationInfoList(TAG, "mAllAppsList.removed", mBgAllAppsList.removed);
        AppInfo.dumpApplicationInfoList(TAG, "mAllAppsList.modified", mBgAllAppsList.modified);
        if (mLoaderTask != null) {
            mLoaderTask.dumpState();
        } else {
            Log.d(TAG, "mLoaderTask=null");
        }
    }

    public Callbacks getCallback() {
        return mCallbacks != null ? mCallbacks.get() : null;
    }

    /**
     * @return {@link FolderInfo} if its already loaded.
     */
    public FolderInfo findFolderById(Long folderId) {
        synchronized (sBgLock) {
            return sBgFolders.get(folderId);
        }
    }

    public BitmapDrawable getGaussWallpaperDrawable() {
        try {
            if (mGaussWallpaperDrawable == null) {
                createGaussWallpaperBitmap(true);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return mGaussWallpaperDrawable;
    }

    public void createGaussWallpaperBitmap(boolean forceUpdate) {
        if (!ENABLE_GAUSS_WALLPAPER_BG) {
            return;
        }

        if (!forceUpdate && mGaussWallpaperDrawable != null) {
            return;
        }

        new AsyncTask<Void, Void, Boolean>() {
            public Boolean doInBackground(Void... args) {
                Context context = mApp.getContext();
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                Drawable wallpaper;
                if (PermissionUtil.hasPermissions(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    wallpaper = wallpaperManager.getDrawable();
                } else {
                    return false;
                }
                if (wallpaper == null) {
                    return false;
                }

                Bitmap wallpaperBitmap = ((BitmapDrawable) wallpaper).getBitmap();
                Bitmap result = null;
                if (!wallpaperBitmap.isRecycled()) {
                    Bitmap source = CommonUtilities.cropWallpaperBitmap(context, wallpaperBitmap);
                    try {
                        result = GaussBlurUtil.createBlurBitmap(source);
                    } catch (Exception ex) {
                        result = null;
                    }

                    if (result != null) {
                        BitmapDrawable drawable = new BitmapDrawable(result);
                        drawable.setColorFilter(context.getResources().getColor(R.color.wallpaper_bg_dim_color), PorterDuff.Mode.MULTIPLY);
                        mGaussWallpaperDrawable = drawable;
                    }
                }

                return mGaussWallpaperDrawable != null;
            }

            @Override
            protected void onPostExecute(Boolean bool) {
                if (bool) {
                    if (getCallback() != null) {
                        getCallback().applyGaussWallpaperBackground();
                    }
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
    }

    /**
     * @return the looper for the worker thread which can be used to start background tasks.
     */
    public static Looper getWorkerLooper() {
        return sWorkerThread.getLooper();
    }

    public void dumpWorkspace() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                synchronized (sBgLock) {
                    try {
                        final Context context = mApp.getContext();
                        String itemTag;
                        ComponentName componentName;
                        ComponentName hideAppCN;
                        File exportDir = new File(Environment.getExternalStorageDirectory(), "launcher");
                        if (!exportDir.exists()) {
                            exportDir.mkdirs();
                        }
                        File xmlFile = new File(exportDir, "workspace.xml");
                        if (xmlFile.exists()) {
                            xmlFile.delete();
                        }
                        Log.e(TAG, "create workspace path --- " + xmlFile.getPath());

                        String XML_NAMESPACE = "http://schemas.android.com/apk/res-auto/com.ios.launcher";
                        XmlSerializer xmlSerializer = Xml.newSerializer();
                        FileOutputStream outXml = new FileOutputStream(new File(exportDir, GridConfig.getGridWorkspaceXmlName(context)));
                        xmlSerializer.setOutput(outXml, "utf-8");
                        xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                        xmlSerializer.startDocument("utf-8", null);
                        xmlSerializer.setPrefix("launcher", XML_NAMESPACE);
                        xmlSerializer.startTag(null, "favorites");

                        for (ItemInfo info : sBgWorkspaceItems) {
                            if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
                                itemTag = "shortcut";
                                xmlSerializer.startTag(null, itemTag);
                            } else if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                                itemTag = "favorite";
                                xmlSerializer.startTag(null, itemTag);
                            } else {
                                continue;
                            }
                            if (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                                xmlSerializer.attribute(XML_NAMESPACE, "container", "-101");
                            }
                            if (info instanceof ShortcutInfo) {
                                componentName = ((ShortcutInfo) info).intent.getComponent();
                                if (componentName != null) {
                                    hideAppCN = IOSOtaHandler.getHideAppComponentName(mApp.getContext(), componentName.getClassName());
                                    if (hideAppCN != null) {
                                        componentName = hideAppCN;
                                    }
                                    xmlSerializer.attribute(XML_NAMESPACE, "className", componentName.getClassName());
                                    xmlSerializer.attribute(XML_NAMESPACE, "packageName", componentName.getPackageName());
                                }
                            }
                            xmlSerializer.attribute(XML_NAMESPACE, "screen", "" + info.screenId);
                            xmlSerializer.attribute(XML_NAMESPACE, "x", "" + info.cellX);
                            xmlSerializer.attribute(XML_NAMESPACE, "y", "" + info.cellY);
                            xmlSerializer.endTag(null, itemTag);
                        }

                        for (LauncherAppWidgetInfo widgetInfo : sBgAppWidgets) {
                            xmlSerializer.startTag(null, "appwidget");
                            xmlSerializer.attribute(XML_NAMESPACE, "className", widgetInfo.providerName.getClassName());
                            xmlSerializer.attribute(XML_NAMESPACE, "packageName", widgetInfo.providerName.getPackageName());
                            xmlSerializer.attribute(XML_NAMESPACE, "screen", "" + widgetInfo.screenId);
                            xmlSerializer.attribute(XML_NAMESPACE, "spanX", "" + widgetInfo.spanX);
                            xmlSerializer.attribute(XML_NAMESPACE, "spanY", "" + widgetInfo.spanY);
                            xmlSerializer.attribute(XML_NAMESPACE, "x", "" + widgetInfo.cellX);
                            xmlSerializer.attribute(XML_NAMESPACE, "y", "" + widgetInfo.cellY);
                            xmlSerializer.endTag(null, "appwidget");
                        }

                        for (FolderInfo folder : sBgFolders) {
                            xmlSerializer.startTag(null, "folder");
                            xmlSerializer.attribute(XML_NAMESPACE, "favoriteTitle", "" + folder.title);
                            if (folder.folderCategoryType != -1) {
                                xmlSerializer.attribute(XML_NAMESPACE, "folderCategoryType", "" + folder.folderCategoryType);
                            }
                            xmlSerializer.attribute(XML_NAMESPACE, "screen", "" + folder.screenId);
                            xmlSerializer.attribute(XML_NAMESPACE, "x", "" + folder.cellX);
                            xmlSerializer.attribute(XML_NAMESPACE, "y", "" + folder.cellY);

                            for (ShortcutInfo shortcutInfo : folder.contents) {
                                if (shortcutInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
                                    itemTag = "shortcut";
                                    xmlSerializer.startTag(null, itemTag);
                                } else {
                                    itemTag = "favorite";
                                    xmlSerializer.startTag(null, itemTag);
                                }
                                componentName = shortcutInfo.intent.getComponent();
                                if (componentName != null) {
                                    hideAppCN = IOSOtaHandler.getHideAppComponentName(mApp.getContext(), componentName.getClassName());
                                    if (hideAppCN != null) {
                                        componentName = hideAppCN;
                                    }
                                    xmlSerializer.attribute(XML_NAMESPACE, "className", componentName.getClassName());
                                    xmlSerializer.attribute(XML_NAMESPACE, "packageName", componentName.getPackageName());
                                }
                                xmlSerializer.attribute(XML_NAMESPACE, "screen", "" + shortcutInfo.screenId);
                                xmlSerializer.attribute(XML_NAMESPACE, "x", "" + shortcutInfo.cellX);
                                xmlSerializer.attribute(XML_NAMESPACE, "y", "" + shortcutInfo.cellY);
                                xmlSerializer.endTag(null, itemTag);
                            }

                            xmlSerializer.endTag(null, "folder");
                        }

                        xmlSerializer.endTag(null, "favorites");
                        xmlSerializer.endDocument();
                        outXml.close();
                        Log.e(TAG, "create workspace.xml success");
                    } catch (Exception e) {
                        Log.e(TAG, "createWorkspaceXml exception --- " + e);
                    }
                }
            }
        };
        runOnWorkerThread(runnable);
    }


    public void dumpAllAppsToLocalData() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final List<AppInfo> appInfos = getAllAppInfo();
                FileOutputStream fos = null;
                File outFile = null;
                try {
                    outFile = new File(FileUtil.getRootFilesDir(), "allapps.txt");
                    if (outFile.exists()) {
                        outFile.delete();
                    }

                    outFile.createNewFile();
                    fos = new FileOutputStream(outFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (fos != null) {
                    PrintWriter writer = new PrintWriter(fos);
                    for (int i = 0; i < appInfos.size(); i++) {
                        AppInfo info = appInfos.get(i);
                        writer.println("" + (i + 1) + ":" + info.title);
                        writer.println(info.componentName.getPackageName());
                        writer.println(info.componentName.getClassName());
                    }
                    writer.close();
                }
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        runOnWorkerThread(runnable);
    }

    public static AppInfo loadAppFromScreenData(long screenId, ComponentName componentName) {
        String packageName = componentName.getPackageName();

        Iterator<ItemInfo> it = sBgWorkspaceItems.iterator();
        while (it.hasNext()) {
            ItemInfo next = it.next();
            if (next instanceof AppInfo) {
                AppInfo appInfo = (AppInfo) next;
                if (appInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION &&
                        appInfo.screenId == screenId) {
                    String appPackageName = appInfo.componentName.getPackageName();
                    if (packageName.contentEquals(appPackageName))
                        return appInfo;
                }
            } else if (next instanceof FolderInfo) {
                Iterator<ShortcutInfo> it3 = ((FolderInfo) next).contents.iterator();
                while (it3.hasNext()) {
                    ShortcutInfo next2 = it3.next();
                    if (next2 instanceof AppInfo) {
                        AppInfo appInfo = (AppInfo) next2;
                        if (appInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION &&
                                appInfo.screenId == screenId) {
                            String appPackageName = appInfo.componentName.getPackageName();
                            if (packageName.contentEquals(appPackageName))
                                return appInfo;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static FolderInfo loadFolderInfoFromScreenData(ShortcutInfo shortcutInfo) {
        Iterator<FolderInfo> it = sBgFolders.iterator();
        while (it.hasNext()) {
            FolderInfo next = it.next();

            Iterator<ShortcutInfo> infoIterator = next.contents.iterator();
            while (infoIterator.hasNext()) {
                ShortcutInfo info = infoIterator.next();
                if (shortcutInfo.id == info.id) {
                    return next;
                }
            }
        }

        return null;
    }
    /**
     * description: get current Application list from sBgWorkspaceItems
     * @return AppInfo lists
     */
    protected static int sCellCountX;
    protected static int sCellCountY;

    public static ShortcutInfo findAppFromWorkspaceItems(ShortcutInfo shortcutInfo) {
        synchronized (sBgLock) {
            Iterator<ItemInfo> it = sBgWorkspaceItems.iterator();
            while (it.hasNext()) {
                ItemInfo next = it.next();
                if (next instanceof ShortcutInfo) {
                    ShortcutInfo appInfo = (ShortcutInfo) next;
                    if (appInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                        if (appInfo.id == shortcutInfo.id) {
                            return appInfo;
                        }
                    }
                }

                if (next instanceof FolderInfo) {
                    Iterator<ShortcutInfo> shortcut = ((FolderInfo) next).contents.iterator();
                    while (shortcut.hasNext()) {
                        ShortcutInfo info = shortcut.next();
                        if (info instanceof ShortcutInfo) {
                            ShortcutInfo applicationInfo = (ShortcutInfo) info;
                            if (applicationInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                                if (applicationInfo.id == shortcutInfo.id) {
                                    return applicationInfo;
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public static ShortcutInfo findAppFromHiddenItems(ShortcutInfo shortcutInfo) {
        synchronized (sBgLock) {
            Iterator<ItemInfo> it = sHdItemsIdMap.values().iterator();
            while (it.hasNext()) {
                ItemInfo next = it.next();
                if (next instanceof ShortcutInfo) {
                    ShortcutInfo appInfo = (ShortcutInfo) next;
                    if (appInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                        if (appInfo.id == shortcutInfo.id) {
                            return appInfo;
                        }
                    }
                }
            }
        }

        return null;
    }
    public ArrayList<ShortcutInfo> loadAppsFromFolderId(long folderId) {
        ArrayList<ShortcutInfo> appList = new ArrayList<>();

        Iterator<ItemInfo> it = sBgWorkspaceItems.iterator();
        while (it.hasNext()) {
            ItemInfo next = it.next();
            if (next instanceof ShortcutInfo) {
                ShortcutInfo appInfo = (ShortcutInfo)next;
                if (appInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                    if (appInfo.container != folderId) {
                        AppInfo cloneApp = new AppInfo(appInfo);
                        appList.add(cloneApp);
                    }
                }
            }

            if (next instanceof FolderInfo) {
                Iterator<ShortcutInfo> shortcut = ((FolderInfo) next).contents.iterator();
                while (shortcut.hasNext()) {
                    ShortcutInfo info = shortcut.next();
                    if (info instanceof ShortcutInfo) {
                        ShortcutInfo applicationInfo = (ShortcutInfo) info;
                        if (applicationInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                            if (applicationInfo.container != folderId) {
                                AppInfo cloneApp = new AppInfo(applicationInfo);
                                appList.add(cloneApp);
                            }
                        }
                    }
                }
            }
        }

        return appList;
    }

    /**
     * @description: update called time and call count of application in database
     * @param context
     * @param itemInfo
     */
    public static void updateCalledTimeAndCountItemInDatabase(Context context, ItemInfo itemInfo) {
        itemInfo.lastCalledTime = (int) System.currentTimeMillis();
        itemInfo.calledNum++;
        ContentValues contentValues = new ContentValues();
        contentValues.put("last_called_time", Long.valueOf(itemInfo.lastCalledTime));
        contentValues.put("called_num", Long.valueOf(itemInfo.calledNum));
        updateItemInDatabaseHelper(context, contentValues, itemInfo, "updateCalledTimeAndCountItemInDatabase");
    }

    public static int getCellCountX() {
        return sCellCountX;
    }

    public static int getCellCountY() {
        return sCellCountY;
    }

    static void updateWorkspaceLayoutCells(int xCount, int yCount) {
        sCellCountX = xCount;
        sCellCountY = yCount;
    }

    private static boolean mExternalAppAvailable = false;
    public static boolean isExternalAppAvailable() {
        return mExternalAppAvailable;
    }

    private ArrayList<ItemInfo> getItemInfoForPackageName(final String packageName) {
        return filterItemInfos(LauncherModel.sBgItemsIdMap, new ItemInfoFilter() {
            @Override
            public boolean filterItem(ItemInfo itemInfo, ItemInfo itemInfo2, ComponentName componentName) {
                return componentName.getPackageName().equals(packageName);
            }
        });
    }

    public boolean checkApplicationEnabled(Context context, String packageName) {
        if (packageName == null || "".equals(packageName)) {
            return false;
        }
        try {
            context.getPackageManager().getApplicationInfo(packageName, 1);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public long getScreen(long screenId) {
        long findScreenId;
        synchronized (sBgWorkspaceScreens) {
            int size = sBgWorkspaceScreens.size();
            int i3 = 0;
            while (true) {
                if (i3 >= size) {
                    findScreenId = -1;
                    break;
                } else if (screenId == sBgWorkspaceScreens.get(i3)) {
                    findScreenId = i3;
                    break;
                } else {
                    i3++;
                }
            }

            if (findScreenId == -1) {
                return -1;//queryScreenNum(i);
            }
            return findScreenId;// + sPrivatePageScreens.size();
        }
    }

    public static int getCellLayoutChildIdFromScreenId(long containerId, long screenId, int cellX, int cellY, int spanX, int spanY) {
        return ((((int) containerId) & 255) << 24) | ((((int)screenId) & 255) << 16) | ((cellX & 255) << 8) | (cellY & 255);
    }

    public boolean getAppNewFlag(String str) {
        return this.mNewFlagPackageNames.contains(str) ? true : false;
    }

    public boolean isWorkSpaceLoaded() {
        return this.mWorkspaceLoaded;
    }


    public void addItem(ItemInfo itemInfo) {
        Log.d(TAG, "addItem,item = " + itemInfo);
//        synchronized (sWorkspaceScreens) {
//            Iterator<WorkspaceScreenData> it = sWorkspaceScreens.iterator();
//            while (true) {
//                if (!it.hasNext()) {
//                    break;
//                }
//                WorkspaceScreenData next = it.next();
//                if (itemInfo.screenId == next.getScreenId()) {
//                    Log.d(TAG, "addItem ws = " + next.getScreenId() + ",result = " + next.addItem(itemInfo));
//                    break;
//                }
//            }
//        }
    }

    public void removeItem(ItemInfo itemInfo) {
        Log.d(TAG, "removeItem, item = " + itemInfo);
        synchronized (sBgLock) {
            Iterator<ItemInfo> it = sBgWorkspaceItems.iterator();
            while (it.hasNext()) {
                ItemInfo next = it.next();
                if (next instanceof ShortcutInfo) {
                    ShortcutInfo appInfo = (ShortcutInfo)next;
                    if (appInfo.id == itemInfo.id) {
                        sBgWorkspaceItems.remove(appInfo);
                        return;
                    }
                }

                if (next instanceof FolderInfo) {
                    Iterator<ShortcutInfo> shortcut = ((FolderInfo) next).contents.iterator();
                    while (shortcut.hasNext()) {
                        ShortcutInfo info = shortcut.next();
                        if (info instanceof AppInfo) {
                            ShortcutInfo appInfo = (ShortcutInfo) info;
                            if (appInfo.id == itemInfo.id) {
                                sBgWorkspaceItems.remove(appInfo);
                                return;
                            }
                        }
                    }
                }
            }
//            Log.d(TAG, "removeItem ws = " + next.getScreenId() + ",result = " + next.removeItem(itemInfo));
        }
    }

    public static void findAndRemoveAppInFolders(ShortcutInfo appInfo, FolderInfo excludeFolder) {
        if (sBgFolders == null)
            return;

        for (FolderInfo folderInfo : sBgFolders) {
            ArrayList<ShortcutInfo> shortcutList = folderInfo.contents;
            Iterator<ShortcutInfo> it = shortcutList.iterator();
            while (it.hasNext()) {
                ShortcutInfo shortcut = (ShortcutInfo) it.next();
                if (shortcut.id == appInfo.id && excludeFolder.id != folderInfo.id) {
                    folderInfo.remove(appInfo);
                    return;
                }
            }
        }
    }

    public static ArrayList<ShortcutInfo> searchAllAppsInWorkspaceWithAppInfo(AppInfo keyAppInfo) {

        ArrayList<ShortcutInfo> removeApps = new ArrayList<ShortcutInfo>();

        String keyAppPackageName = keyAppInfo.getPackageName();
        Iterator<ItemInfo> it = sBgWorkspaceItems.iterator();
        while (it.hasNext()) {
            ItemInfo next = it.next();

            if (next instanceof ShortcutInfo) {
                ShortcutInfo appInfo = (ShortcutInfo) next;
                if (keyAppPackageName.equals(appInfo.getPackageName())) {
                    removeApps.add(appInfo);
                }
            }

            if (next instanceof FolderInfo) {
                Iterator<ShortcutInfo> shortcut = ((FolderInfo) next).contents.iterator();
                while (shortcut.hasNext()) {
                    ShortcutInfo info = shortcut.next();
                    if (keyAppPackageName.equals(info.getPackageName())) {
                        removeApps.add(info);
                    }
                }
            }
        }

        return removeApps;
    }
}

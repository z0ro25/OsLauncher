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

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.launcher.appoverride.AppOverrideStore;
import com.amz.ios.launcher.compat.LauncherActivityInfoCompat;
import com.amz.ios.launcher.compat.LauncherAppsCompat;
import com.amz.ios.launcher.compat.UserHandleCompat;
import com.amz.ios.launcher.compat.UserManagerCompat;
import com.amz.ios.launcher.config.Settings;
import com.amz.ios.launcher.leftpage.drawables.ClockDrawable;
import com.amz.ios.launcher.model.PackageItemInfo;
import com.amz.ios.launcher.provider.AppTypeProvider;
import com.amz.ios.launcher.theme.ThemeManager;
import com.amz.ios.launcher.util.ComponentKey;
import com.amz.ios.launcher.util.Thunk;
import com.amz.ios.launcher.views.CustomTextView;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;

/**
 * Cache of application icons.  Icons can be made from any thread.
 */
public class IconCache {

    private static final String TAG = "Launcher.IconCache";

    private static final int INITIAL_ICON_CACHE_CAPACITY = 50;

    // Empty class name is used for storing package default entry.
    private static final String EMPTY_CLASS_NAME = ".";

    private static final boolean DEBUG = false;

    private static final int LOW_RES_SCALE_FACTOR = 5;

    private static final String CUSTOM_LABEL_PREFIX = "custom_label_";

    @Thunk
    static final Object ICON_UPDATE_TOKEN = new Object();

    @Thunk
    static class CacheEntry {
        public Bitmap icon;
        public CharSequence title = "";
        public CharSequence contentDescription = "";
        public boolean isLowResIcon;
    }

    @Thunk
    static class IconData {
        public int mType = 1;
        public int mIconRes = 0;
        public String mIconPath = null;

        public IconData(){

        }

        public IconData(int iconRes, String iconPath, int type) {
            this.mType = type;
            this.mIconRes = iconRes;
            this.mIconPath = iconPath;
        }
    }

    private final HashMap<UserHandleCompat, Bitmap> mDefaultIcons = new HashMap<>();
    private final HashMap<String, Integer> mTempIcons = new HashMap<>();

    @Thunk
    final MainThreadExecutor mMainThreadExecutor = new MainThreadExecutor();

    private final Context mContext;
    private ThemeManager mThemeManager;
    public final PackageManager mPackageManager;
    @Thunk
    final UserManagerCompat mUserManager;
    public final LauncherAppsCompat mLauncherApps;
    private final HashMap<ComponentKey, CacheEntry> mCache =
            new HashMap<ComponentKey, CacheEntry>(INITIAL_ICON_CACHE_CAPACITY);
    private final int mIconDpi;
    @Thunk
    IconDB mIconDb;

    @Thunk
    final Handler mWorkerHandler;

    // The background color used for activity icons. Since these icons are displayed in all-apps
    // and folders, this would be same as the light quantum panel background. This color
    // is used to convert icons to RGB_565.
    private final int mActivityBgColor;
    // The background color used for package icons. These are displayed in widget tray, which
    // has a dark quantum panel background.
    private final int mPackageBgColor;
    private final BitmapFactory.Options mLowResOptions;
    private SharedPreferences mSharedPreferences;
    private String mSystemState;
    private Bitmap mLowResBitmap;
    private Canvas mLowResCanvas;
    private Paint mLowResPaint;

    public IconCache(Context context, InvariantDeviceProfile inv) {
        mContext = context;
        mPackageManager = context.getPackageManager();
        mUserManager = UserManagerCompat.getInstance(mContext);
        mLauncherApps = LauncherAppsCompat.getInstance(mContext);
        mIconDpi = inv.fillResIconDpi;
        mSharedPreferences = mContext.getSharedPreferences(
                Launcher.PREF_KEY,
                Context.MODE_PRIVATE
        );
//        this.mDefaultIcon = makeDefaultIcon();
        IconDB.DB_VERSION = Partner.getInteger(context, Partner.DEF_ICONCACHE_DB_VERSION, IconDB.DB_VERSION);
        mIconDb = new IconDB(context);

        mWorkerHandler = new Handler(LauncherModel.getWorkerLooper());

        mActivityBgColor = context.getResources().getColor(R.color.quantum_panel_bg_color);
        mPackageBgColor = context.getResources().getColor(R.color.quantum_panel_bg_color_dark);
        mLowResOptions = new BitmapFactory.Options();
        // Always prefer RGB_565 config for low res. If the bitmap has transparency, it will
        // automatically be loaded as ALPHA_8888.
        mLowResOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        updateSystemStateString();
    }

    public void setThemeManager(ThemeManager themeManager) {
        mThemeManager = themeManager;
    }


    private Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(), android.R.mipmap.sym_def_app_icon);
    }

    private Drawable getFullResIcon(Resources resources, int iconId) {
        Drawable d;
        try {
            d = resources.getDrawableForDensity(iconId, mIconDpi);
        } catch (Resources.NotFoundException e) {
            d = null;
        }

        return (d != null) ? d : getFullResDefaultActivityIcon();
    }


    public void applyIOSShortcut(ResolveInfo info, ImageView imageView, CustomTextView textView) {
        CharSequence label = info.loadLabel(mPackageManager);
        Drawable drawable = getFullResIcon(info.activityInfo);
        imageView.setImageDrawable(drawable);
        textView.setText(label);
    }

    public Drawable getFullResIcon(LauncherActivityInfo info) {
        return info.getIcon(mIconDpi);
    }

    public Drawable getFullResIcon(String packageName, int iconId) {
        Resources resources;
        try {
            resources = mPackageManager.getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }
        return getFullResDefaultActivityIcon();
    }

    public Drawable getFullResIcon(ActivityInfo info) {
        if (info.packageName.equals(mContext.getPackageName())) {
            Drawable drawable = getIOSShortcutIcon(new ComponentName(info.packageName, info.name));

            if (drawable != null) {
                return drawable;
            }
        }

        Resources resources;
        try {
            resources = mPackageManager.getResourcesForApplication(
                    info.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            int iconId = info.getIconResource();
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }

        return getFullResDefaultActivityIcon();
    }

    private Bitmap makeDefaultIcon(UserHandleCompat user) {
        Drawable unbadged = getFullResDefaultActivityIcon();
        return Utilities.createBadgedIconBitmap(unbadged, user, mContext);
    }

    /**
     * Remove any records for the supplied ComponentName.
     */
    public synchronized void remove(ComponentName componentName, UserHandleCompat user) {
        mCache.remove(new ComponentKey(componentName, user));
        if (componentName != null){
            mTempIcons.remove(componentName.getPackageName());
        }
    }


    /**
     * Empty out the cache.
     */
    public synchronized void flush() {
        mCache.clear();
        if (mIconDb != null) {
            mIconDb.close();
        }
        mIconDb = new IconDB(mContext);
    }

    /**
     * Remove any records for the supplied package name from memory.
     */
    private void removeFromMemCacheLocked(String packageName, UserHandleCompat user) {
        HashSet<ComponentKey> forDeletion = new HashSet<ComponentKey>();
        for (ComponentKey key : mCache.keySet()) {
            if (key.componentName.getPackageName().equals(packageName)
                    && key.user.equals(user)) {
                forDeletion.add(key);
            }
        }
        for (ComponentKey componentKey : forDeletion) {
            mCache.remove(componentKey);
            if (componentKey.componentName != null){
                mTempIcons.remove(componentKey.componentName.getPackageName());
            }
        }
    }

    private void removeFromMemCacheLockedByComponentName(String componentName, UserHandleCompat user) {
        HashSet<ComponentKey> forDeletion = new HashSet<ComponentKey>();
        for (ComponentKey key : mCache.keySet()) {
            if (key.componentName.flattenToShortString().equals(componentName)
                    && key.user.equals(user)) {
                forDeletion.add(key);
            }
        }
        for (ComponentKey componentKey : forDeletion) {
            mCache.remove(componentKey);
            if (componentKey.componentName != null){
                mTempIcons.remove(componentKey.componentName.getPackageName());
            }
        }
    }

    /**
     * Updates the entries related to the given package in memory and persistent DB.
     */
    public synchronized void updateIconsForPkg(String packageName, UserHandleCompat user) {
        removeIconsForPkg(packageName, user);
        try {
            PackageInfo info = mPackageManager.getPackageInfo(packageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES);
            long userSerial = mUserManager.getSerialNumberForUser(user);
            for (LauncherActivityInfoCompat app : mLauncherApps.getActivityList(packageName, user)) {
                addIconToDBAndMemCache(app, info, userSerial);
            }
        } catch (NameNotFoundException e) {
            Log.d(TAG, "Package not found", e);
            return;
        }
    }

    /**
     * Removes the entries related to the given package in memory and persistent DB.
     */
    public synchronized void removeIconsForPkg(String packageName, UserHandleCompat user) {
        removeFromMemCacheLocked(packageName, user);
        long userSerial = mUserManager.getSerialNumberForUser(user);
        mIconDb.getWritableDatabase().delete(IconDB.TABLE_NAME,
                IconDB.COLUMN_COMPONENT + " LIKE ? AND " + IconDB.COLUMN_USER + " = ?",
                new String[]{packageName + "/%", Long.toString(userSerial)});
    }

    public void updateDbIcons(Set<String> ignorePackagesForMainUser) {
        // Remove all active icon update tasks.
        mWorkerHandler.removeCallbacksAndMessages(ICON_UPDATE_TOKEN);

        updateSystemStateString();
        for (UserHandleCompat user : mUserManager.getUserProfiles()) {
            // Query for the set of apps
            final List<LauncherActivityInfoCompat> apps = mLauncherApps.getActivityList(null, user);
            // Fail if we don't have any apps
            // TODO: Fix this. Only fail for the current user.
            if (apps == null || apps.isEmpty()) {
                return;
            }

            // Update icon cache. This happens in segments and {@link #onPackageIconsUpdated}
            // is called by the icon cache when the job is complete.
            updateDBIcons(user, apps, UserHandleCompat.myUserHandle().equals(user)
                    ? ignorePackagesForMainUser : Collections.<String>emptySet());
        }
    }

    /**
     * Updates the persistent DB, such that only entries corresponding to {@param apps} remain in
     * the DB and are updated.
     *
     * @return The set of packages for which icons have updated.
     */
    private void updateDBIcons(UserHandleCompat user, List<LauncherActivityInfoCompat> apps,
                               Set<String> ignorePackages) {
        long userSerial = mUserManager.getSerialNumberForUser(user);
        PackageManager pm = mContext.getPackageManager();
        HashMap<String, PackageInfo> pkgInfoMap = new HashMap<String, PackageInfo>();
        for (PackageInfo info : pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES)) {
            pkgInfoMap.put(info.packageName, info);
        }

        HashMap<ComponentName, LauncherActivityInfoCompat> componentMap = new HashMap<>();
        for (LauncherActivityInfoCompat app : apps) {
            componentMap.put(app.getComponentName(), app);
        }

        Cursor c = mIconDb.getReadableDatabase().query(IconDB.TABLE_NAME,
                new String[]{IconDB.COLUMN_ROWID, IconDB.COLUMN_COMPONENT,
                        IconDB.COLUMN_LAST_UPDATED, IconDB.COLUMN_VERSION,
                        IconDB.COLUMN_SYSTEM_STATE},
                IconDB.COLUMN_USER + " = ? ",
                new String[]{Long.toString(userSerial)},
                null, null, null);

        final int indexComponent = c.getColumnIndex(IconDB.COLUMN_COMPONENT);
        final int indexLastUpdate = c.getColumnIndex(IconDB.COLUMN_LAST_UPDATED);
        final int indexVersion = c.getColumnIndex(IconDB.COLUMN_VERSION);
        final int rowIndex = c.getColumnIndex(IconDB.COLUMN_ROWID);
        final int systemStateIndex = c.getColumnIndex(IconDB.COLUMN_SYSTEM_STATE);

        HashSet<Integer> itemsToRemove = new HashSet<Integer>();
        Stack<LauncherActivityInfoCompat> appsToUpdate = new Stack<>();

        while (c.moveToNext()) {
            String cn = c.getString(indexComponent);
            ComponentName component = ComponentName.unflattenFromString(cn);
            PackageInfo info = pkgInfoMap.get(component.getPackageName());
            if (info == null) {
                if (!ignorePackages.contains(component.getPackageName())) {
                    remove(component, user);
                    itemsToRemove.add(c.getInt(rowIndex));
                }
                continue;
            }
            if ((info.applicationInfo.flags & ApplicationInfo.FLAG_IS_DATA_ONLY) != 0) {
                // Application is not present
                continue;
            }

            long updateTime = c.getLong(indexLastUpdate);
            int version = c.getInt(indexVersion);
            LauncherActivityInfoCompat app = componentMap.remove(component);
            if (version == info.versionCode && updateTime == info.lastUpdateTime &&
                    TextUtils.equals(mSystemState, c.getString(systemStateIndex))) {
                continue;
            }
            if (app == null) {
                remove(component, user);
                itemsToRemove.add(c.getInt(rowIndex));
            } else {
                appsToUpdate.add(app);
            }
        }
        c.close();
        if (!itemsToRemove.isEmpty()) {
            mIconDb.getWritableDatabase().delete(IconDB.TABLE_NAME,
                    Utilities.createDbSelectionQuery(IconDB.COLUMN_ROWID, itemsToRemove),
                    null);
        }

        // Insert remaining apps.
        if (!componentMap.isEmpty() || !appsToUpdate.isEmpty()) {
            Stack<LauncherActivityInfoCompat> appsToAdd = new Stack<>();
            appsToAdd.addAll(componentMap.values());
            new SerializedIconUpdateTask(userSerial, pkgInfoMap,
                    appsToAdd, appsToUpdate).scheduleNext();
        }
    }

    @Thunk
    void addIconToDBAndMemCache(LauncherActivityInfoCompat app, PackageInfo info,
                                long userSerial) {
        // Reuse the existing entry if it already exists in the DB. This ensures that we do not
        // create bitmap if it was already created during loader.
        ContentValues values = updateCacheAndGetContentValues(app, false);
        addIconToDB(values, app.getComponentName(), info, userSerial);
    }

    /**
     * Updates {@param values} to contain versoning information and adds it to the DB.
     *
     * @param values {@link ContentValues} containing icon & title
     */
    private void addIconToDB(ContentValues values, ComponentName key,
                             PackageInfo info, long userSerial) {
        values.put(IconDB.COLUMN_COMPONENT, key.flattenToString());
        values.put(IconDB.COLUMN_USER, userSerial);
        values.put(IconDB.COLUMN_LAST_UPDATED, info.lastUpdateTime);
        values.put(IconDB.COLUMN_VERSION, info.versionCode);
        try {
            mIconDb.getWritableDatabase().insertWithOnConflict(IconDB.TABLE_NAME, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            Log.e(TAG, "addIconToDB fail", e);
        }

    }

    @Thunk
    ContentValues updateCacheAndGetContentValues(LauncherActivityInfoCompat app,
                                                 boolean replaceExisting) {
        final ComponentKey key = new ComponentKey(app.getComponentName(), app.getUser());
        CacheEntry entry = null;
        if (!replaceExisting) {
            entry = mCache.get(key);
            // We can't reuse the entry if the high-res icon is not present.
            if (entry == null || entry.isLowResIcon || entry.icon == null) {
                entry = null;
            }
        }
        if (entry == null) {
            entry = new CacheEntry();
            Drawable drawable = getThemeIconForAppComp(app.getComponentName(), app, app.getUser());
//            IconData iconData = createIconDataFromCacheEntry(key);
//            if (iconData.mType == 1)
//                iconData.mIconRes = getIconRes(key,iconData.mIconRes);
//            Drawable drawable = getDrawableFromIconData(iconData);

            if (drawable == null)
                drawable = app.getIcon(mIconDpi);

            entry.icon = Utilities.createBadgedIconBitmap(drawable, app.getUser(), mContext);
        }
        else {
            entry.icon = Utilities.createBadgedIconBitmap(getRoundDrawable(new FastBitmapDrawable(entry.icon)), app.getUser(), mContext);
        }

        entry.title = getCustomLabel(app.getComponentName());
        entry.contentDescription = mUserManager.getBadgedLabelForUser(entry.title, app.getUser());
        if (TextUtils.isEmpty(entry.title))
            entry.title = app.getLabel();
        mCache.put(new ComponentKey(app.getComponentName(), app.getUser()), entry);

        return newContentValues(entry.icon, entry.title.toString(), mActivityBgColor,0,0,null,0);
    }

    public Drawable getRoundDrawable(Drawable drawable){

        // Nếu drawable đã là ảnh tạo hình sẵn (logo iOS: 4 góc trong suốt) thì trả NGUYÊN XI —
        // không nhét lại vào squircle/bo góc của launcher (sẽ cắt/lệch góc thật của ảnh).
        Bitmap raw = null;
        if (drawable instanceof BitmapDrawable) {
            raw = ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof FastBitmapDrawable) {
            raw = ((FastBitmapDrawable) drawable).getBitmap();
        }
        if (Utilities.hasTransparentCorners(raw)) {
            return drawable;
        }

        Bitmap bmp = drawableToBitmap(drawable);
        Bitmap bitmap = Utilities.createIconBitmap(bmp,mContext);

        Bitmap newBitmap = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(newBitmap);
        RectF rectF = new RectF(0,0,bitmap.getWidth(),bitmap.getHeight());
        float radius = mContext.getResources().getDimension(R.dimen.icon_round_corner);
        Path path = new Path();
        path.addRoundRect(rectF,radius,radius, Path.Direction.CW);
        canvas.clipPath(path);
        Paint paint = new Paint(1);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);

        int count = ClockDrawable.drawBgAndReturnSave(
                canvas,
                bitmap,
                bitmap.getWidth(),
                bitmap.getHeight(),
                paint
        );

//        canvas.drawBitmap(bitmap,0,0,new Paint(Paint.ANTI_ALIAS_FLAG));

        return new FastBitmapDrawable(newBitmap);
    }

    /**
     * Fetches high-res icon for the provided ItemInfo and updates the caller when done.
     *
     * @return a request ID that can be used to cancel the request.
     */
    public IconLoadRequest updateIconInBackground(final BubbleTextView caller, final ItemInfo info) {
        Runnable request = new Runnable() {

            @Override
            public void run() {
                Log.e("Info Class is ", info.getClass().getSimpleName());
                if (info instanceof AppInfo) {
                    getTitleAndIcon((AppInfo) info, null, false);
                } else if (info instanceof ShortcutInfo) {
                    ShortcutInfo st = (ShortcutInfo) info;
                    getTitleAndIcon(st,
                            st.promisedIntent != null ? st.promisedIntent : st.intent,
                            st.user, false);
                } else if (info instanceof PackageItemInfo) {
                    PackageItemInfo pti = (PackageItemInfo) info;
                    getTitleAndIconForApp(pti.packageName, pti.user, false, pti);
                }
                mMainThreadExecutor.execute(new Runnable() {

                    @Override
                    public void run() {
                        caller.reapplyItemInfo(info);
                    }
                });
            }
        };
        mWorkerHandler.post(request);
        return new IconLoadRequest(request, mWorkerHandler);
    }

    private Bitmap getNonNullIcon(CacheEntry entry, UserHandleCompat user) {
        return entry.icon == null ? getDefaultIcon(user) : entry.icon;
    }

    /**
     * Fill in "application" with the icon and label for "info."
     */
    public synchronized void getTitleAndIcon(AppInfo application,
                                             LauncherActivityInfoCompat info, boolean useLowResIcon) {
        UserHandleCompat user = info == null ? application.user : info.getUser();
        CacheEntry entry = cacheLocked(application.componentName, info, user,
                false, useLowResIcon);
        application.title = Utilities.trim(entry.title);
        application.iconBitmap = getNonNullIcon(entry, user);
        application.contentDescription = entry.contentDescription;
        application.usingLowResIcon = entry.isLowResIcon;
    }

    /**
     * Updates {@param application} only if a valid entry is found.
     */


    public synchronized void updateTitleAndIcon(AppInfo application) {
        CacheEntry entry = cacheLocked(application.componentName, null, application.user,
                false, application.usingLowResIcon);
        if (entry.icon != null && !isDefaultIcon(entry.icon, application.user)) {
            application.title = Utilities.trim(entry.title);
            application.iconBitmap = entry.icon;
            application.contentDescription = entry.contentDescription;
            application.usingLowResIcon = entry.isLowResIcon;
        }
    }

    /**
     * Returns a high res icon for the given intent and user
     */
    public synchronized Bitmap getIcon(Intent intent, UserHandleCompat user) {
        ComponentName component = intent.getComponent();
        // null info means not installed, but if we have a component from the intent then
        // we should still look in the cache for restored app icons.
        if (component == null) {
            return getDefaultIcon(user);
        }

        LauncherActivityInfoCompat launcherActInfo = mLauncherApps.resolveActivity(intent, user);
        CacheEntry entry = cacheLocked(component, launcherActInfo, user, true, false /* useLowRes */);
        return entry.icon;
    }

    /**
     * Fill in {@param shortcutInfo} with the icon and label for {@param intent}. If the
     * corresponding activity is not found, it reverts to the package icon.
     */
    public synchronized void getTitleAndIcon(ShortcutInfo shortcutInfo, Intent intent,
                                             UserHandleCompat user, boolean useLowResIcon) {
        ComponentName component = intent.getComponent();
        // null info means not installed, but if we have a component from the intent then
        // we should still look in the cache for restored app icons.
        if (component == null) {
            shortcutInfo.setIcon(getDefaultIcon(user));
            shortcutInfo.title = "";
            shortcutInfo.usingFallbackIcon = true;
            shortcutInfo.usingLowResIcon = false;
        } else {
            LauncherActivityInfoCompat info = mLauncherApps.resolveActivity(intent, user);
            getTitleAndIcon(shortcutInfo, component, info, user, true, useLowResIcon);
        }
    }

    /**
     * Fill in {@param shortcutInfo} with the icon and label for {@param info}
     */
    public synchronized void getTitleAndIcon(
            ShortcutInfo shortcutInfo, ComponentName component, LauncherActivityInfoCompat info,
            UserHandleCompat user, boolean usePkgIcon, boolean useLowResIcon) {
        CacheEntry entry = cacheLocked(component, info, user, usePkgIcon, useLowResIcon);
        shortcutInfo.setIcon(getNonNullIcon(entry, user));
        shortcutInfo.title = Utilities.trim(entry.title);
        shortcutInfo.usingFallbackIcon = isDefaultIcon(entry.icon, user);
        shortcutInfo.usingLowResIcon = entry.isLowResIcon;
    }

    /**
     * Fill in {@param appInfo} with the icon and label for {@param packageName}
     */
    public synchronized void getTitleAndIconForApp(
            String packageName, UserHandleCompat user, boolean useLowResIcon,
            PackageItemInfo infoOut) {
        CacheEntry entry = getEntryForPackageLocked(packageName, user, useLowResIcon);
        infoOut.iconBitmap = getNonNullIcon(entry, user);
        infoOut.title = Utilities.trim(entry.title);
        infoOut.usingLowResIcon = entry.isLowResIcon;
        infoOut.contentDescription = entry.contentDescription;
    }


    /**
     * Fill in {@param appInfo} with the icon and label for {@param packageName}
     */
    public synchronized void getTitleAndIconForIOSShortcut(ShortcutInfo shortcutInfo,
                                                              ComponentName componentName, LauncherActivityInfoCompat info, UserHandleCompat user, boolean useLowResIcon) {
        CacheEntry entry = cacheIOSShortcutLocked(componentName, info, user, useLowResIcon);
        shortcutInfo.setIcon(getNonNullIcon(entry, user));
        shortcutInfo.title = Utilities.trim(entry.title);
        shortcutInfo.usingFallbackIcon = isDefaultIcon(entry.icon, user);
        shortcutInfo.usingLowResIcon = entry.isLowResIcon;
    }

    public synchronized Drawable getIOSShortcutIcon(ComponentName componentName) {
        UserHandleCompat user = UserHandleCompat.myUserHandle();
        LauncherActivityInfoCompat info = mLauncherApps.resolveActivity(new Intent().setComponent(componentName), user);
        CacheEntry entry = cacheIOSShortcutLocked(componentName, info, user, false);
        Bitmap icon = getNonNullIcon(entry, user);
        return new BitmapDrawable(mContext.getResources(), icon);
    }
    public synchronized Bitmap getDefaultIcon(UserHandleCompat user) {
        if (!mDefaultIcons.containsKey(user)) {
            mDefaultIcons.put(user, makeDefaultIcon(user));
        }
        return mDefaultIcons.get(user);
    }

    public boolean isDefaultIcon(Bitmap icon, UserHandleCompat user) {
        return mDefaultIcons.get(user) == icon;
    }

    private static String[] mCtsApps;

    private boolean isCtsApp(Context context, String packageName) {
        if (!TextUtils.isEmpty(packageName)) {
            if (mCtsApps == null) {
                mCtsApps = Partner.getStringArray(context, Partner.DEF_CTS_APP_LIST);
            }

            if (mCtsApps == null) {
                return false;
            }

            for (int i = 0; i < mCtsApps.length; i++) {
                if (packageName.contains(mCtsApps[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    private Drawable getThemeIconForAppComp(ComponentName componentName, LauncherActivityInfoCompat info,
                                            UserHandleCompat user) {
        if (isCtsApp(mContext, componentName.getPackageName())) {
            return info.getIcon(mIconDpi);
        }

        AppTypeProvider appTypeProvider = AppTypeProvider.getAppTypeProvider();
        if (componentName.equals(appTypeProvider.getClockComp()) || componentName.equals(appTypeProvider.getCalendarComp())) {
            Drawable icon = mThemeManager.readCustomizedIconForAppComp(componentName);
            if (icon != null) {
                return icon;
            }
        }

        if (mThemeManager.isDefaultTheme() && Settings.isUseIconInApkEnable(mContext)) {
            return info.getIcon(mIconDpi);
        }

        Drawable icon = mThemeManager.readCustomizedIconForAppComp(componentName);
        info.getIcon(mIconDpi);

        if (icon == null) {
            icon = mThemeManager.applyWithThemeBg(info.getIcon(mIconDpi));
        }
        else {

        }
        return icon;
    }

    public Bitmap getThemeIconForComponent(ComponentName componentName, UserHandleCompat user) {
        if (isCtsApp(mContext, componentName.getPackageName())) {
            return null;
        }

        if (mThemeManager.isDefaultTheme() && Settings.isUseIconInApkEnable(mContext)) {
            return null;
        }

        Drawable icon = mThemeManager.readCustomizedIconForAppComp(componentName);

        if (icon != null) {
            return Utilities.createBadgedIconBitmap(icon, user, mContext);
        }

        return null;
    }

    public Drawable getThemeIconForApp(ApplicationInfo appInfo) {
        if (isCtsApp(mContext, appInfo.packageName)) {
            return appInfo.loadIcon(mPackageManager);
        }

        if (mThemeManager.isDefaultTheme() && Settings.isUseIconInApkEnable(mContext)) {
            return appInfo.loadIcon(mPackageManager);
        }

        return mThemeManager.applyWithThemeBg(
                getRoundIcon(
                        appInfo,
                        mPackageManager
                )
        );
    }

    private Drawable getThemeIconForShortcutComp(ComponentName componentName, LauncherActivityInfoCompat info,
                                                 UserHandleCompat user) {
        Drawable icon;

        icon = mThemeManager.readCustomizedIconForShortcutComp(componentName);

        if (icon == null && info != null) {
            try {
                Drawable iconDrawable = getRoundIcon(info.getApplicationInfo(),mPackageManager);
                if (iconDrawable == null)
                    return null;
                icon = mThemeManager.applyWithThemeBg(iconDrawable);
            }catch(Exception e) {
                e.printStackTrace();
            }
        }
        return icon;
    }

    public Drawable getRoundIcon(ApplicationInfo applicationInfo, PackageManager packageManager) {
        int eventType = -1;
        String name = "";
        int i = 0;
        /*
        try {
            Resources resource = packageManager.getResourcesForApplication(applicationInfo);
            XmlResourceParser resourceParser = resource.getAssets().openXmlResourceParser("AndroidManifest.xml");

            while (eventType != XmlResourceParser.END_DOCUMENT) {
                eventType = resourceParser.getEventType();
                Log.e("APP NAME is ", applicationInfo.packageName);
                if (eventType == XmlResourceParser.START_TAG) {
                    if (resourceParser.getName().equals("application")) {
                        for (i = 0; i < resourceParser.getAttributeCount(); i++)
                            name = resourceParser.getAttributeName(i);
                            Log.e("Attr Name is ", name);
                            if (resourceParser.getAttributeName(i).equals("roundIcon")) {

                                return resource.getDrawable(Integer.parseInt(resourceParser.getAttributeValue(i).substring(1)));
                            }
                    }
                }

                eventType = resourceParser.next();
            }
        } catch (Exception e) {
            Log.e(TAG, "getRoundIcon: ", e);
        }


         */
        return packageManager.getApplicationIcon(applicationInfo);
    }

    /**
     * Retrieves the entry from the cache. If the entry is not present, it creates a new entry.
     * This method is not thread safe, it must be called from a synchronized method.
     */
    private CacheEntry cacheLocked(ComponentName componentName, LauncherActivityInfoCompat info,
                                   UserHandleCompat user, boolean usePackageIcon, boolean useLowResIcon) {
        ComponentKey cacheKey = new ComponentKey(componentName, user);
        CacheEntry entry = mCache.get(cacheKey);
        if (entry == null || (entry.isLowResIcon && !useLowResIcon)) {
            entry = new CacheEntry();
            mCache.put(cacheKey, entry);

            // Check the DB first.
            if (!getEntryFromDB(cacheKey, entry, useLowResIcon)) {
                if (info != null) {
//                    IconData iconData = createIconDataFromCacheEntry(cacheKey);
//                    if (iconData.mType == 1){
//                        iconData.mIconRes = getIconRes(cacheKey, iconData.mIconRes);
//                    }
//                    Drawable drawable = getDrawableFromIconData(iconData);
                    Drawable drawable = getThemeIconForAppComp(componentName, info, user);
                    if (drawable == null)
                        drawable = info.getIcon(mIconDpi);
                    entry.icon = Utilities.createBadgedIconBitmap(
                            drawable,
                            info.getUser(),
                            mContext
                    );
                    entry.icon = Utilities.createBadgedIconBitmap(drawable, info.getUser(), mContext);

                } else {
                    if (usePackageIcon) {
                        CacheEntry packageEntry = getEntryForPackageLocked(
                                componentName.getPackageName(), user, false);
                        if (packageEntry != null) {
                            if (DEBUG) Log.d(TAG, "using package default icon for " +
                                    componentName.toShortString());
                            entry.icon = packageEntry.icon;
                            entry.title = packageEntry.title;
                            entry.contentDescription = packageEntry.contentDescription;
                        }
                    }
                    if (entry.icon == null) {
                        if (DEBUG) Log.d(TAG, "using default icon for " +
                                componentName.toShortString());
                        entry.icon = getDefaultIcon(user);
                    }
                }
            }
            else {

            }

//            if (!useLowResIcon) {
//                cachePkgWithLauncherIcon(componentName.getPackageName(), user, entry.icon);
//            }
        }

        if (info != null) {
            entry.title = info.getLabel();
            entry.contentDescription = mUserManager.getBadgedLabelForUser(entry.title, user);
        }

        String label = getCustomLabel(componentName);
        if (label != null){
            entry.title = label;
        }
        // TODO: 2023.12.03 Check Label From DB

        // Logo custom (nguồn sự thật: app_override.db qua AppOverrideStore). Đặt CUỐI để luôn
        // re-assert đè lên mọi icon dựng phía trên; bitmap memoize theo path nên rẻ.
        try {
            AppOverrideStore.INSTANCE.init(mContext);
            Bitmap overrideLogo = AppOverrideStore.INSTANCE.getCurrentLogoBitmap(componentName.flattenToString());
            if (overrideLogo != null) {
                entry.icon = Utilities.createBadgedIconBitmap(
                        new BitmapDrawable(mContext.getResources(), overrideLogo), user, mContext);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return entry;
    }

    public String getCustomLabel(ComponentName componentName){
        // Ưu tiên tên current từ app_override.db (nguồn sự thật duy nhất). Fallback prefs cũ.
        try {
            AppOverrideStore.INSTANCE.init(mContext);
            String current = AppOverrideStore.INSTANCE.getCurrentName(componentName.flattenToString());
            if (current != null) return current;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        try {
            SharedPreferences sharedPreferences = this.mSharedPreferences;
            return sharedPreferences.getString(CUSTOM_LABEL_PREFIX + componentName.flattenToString(), null);
        } catch (Throwable th){
            th.getMessage();
            return null;
        }
    }


    /**
     * Retrieves the entry from the cache. If the entry is not present, it creates a new entry.
     * This method is not thread safe, it must be called from a synchronized method.
     */
    private CacheEntry cacheIOSShortcutLocked(ComponentName componentName, LauncherActivityInfoCompat info,
                                                 UserHandleCompat user, boolean useLowResIcon) {

        ComponentKey cacheKey = new ComponentKey(componentName, user);

        CacheEntry entry = mCache.get(cacheKey);
        if (entry == null || (entry.isLowResIcon && !useLowResIcon)) {
            entry = new CacheEntry();
            mCache.put(cacheKey, entry);
            // Check the DB first.
            if (!getEntryFromDB(cacheKey, entry, useLowResIcon)) {
                Drawable themeIconDrawble;
                themeIconDrawble = getThemeIconForShortcutComp(componentName, info, user);
                if (themeIconDrawble != null)
                    entry.icon = Utilities.createIconBitmap(themeIconDrawble, mContext);
            }

            entry.contentDescription = mUserManager.getBadgedLabelForUser(entry.title, user);
        }

        if (info != null) {
            entry.title = info.getLabel();
        }
        return entry;
    }


    /**
     * Adds a default package entry in the cache. This entry is not persisted and will be removed
     * when the cache is flushed.
     */
    public synchronized void cachePackageInstallInfo(String packageName, UserHandleCompat user,
                                                     Bitmap icon, CharSequence title) {
        removeFromMemCacheLocked(packageName, user);

        ComponentKey cacheKey = getPackageKey(packageName, user);
        CacheEntry entry = mCache.get(cacheKey);

        // For icon caching, do not go through DB. Just update the in-memory entry.
        if (entry == null) {
            entry = new CacheEntry();
            mCache.put(cacheKey, entry);
        }
        if (!TextUtils.isEmpty(title)) {
            entry.title = title;
        }
        if (icon != null) {
            entry.icon = Utilities.createIconBitmap(icon, mContext);
        }
    }

    private static ComponentKey getPackageKey(String packageName, UserHandleCompat user) {
        ComponentName cn = new ComponentName(packageName, packageName + EMPTY_CLASS_NAME);
        return new ComponentKey(cn, user);
    }

    /**
     * Gets an entry for the package, which can be used as a fallback entry for various components.
     * This method is not thread safe, it must be called from a synchronized method.
     */
    private CacheEntry getEntryForPackageLocked(String packageName, UserHandleCompat user,
                                                boolean useLowResIcon) {
        ComponentKey cacheKey = getPackageKey(packageName, user);
        CacheEntry entry = mCache.get(cacheKey);

        if (entry == null || (entry.isLowResIcon && !useLowResIcon)) {
            entry = new CacheEntry();
            boolean entryUpdated = true;

            // Check the DB first.
            if (!getEntryFromDB(cacheKey, entry, useLowResIcon)) {
                try {
                    int flags = UserHandleCompat.myUserHandle().equals(user) ? 0 :
                            PackageManager.GET_UNINSTALLED_PACKAGES;
                    PackageInfo info = mPackageManager.getPackageInfo(packageName, flags);
                    ApplicationInfo appInfo = info.applicationInfo;
                    if (appInfo == null) {
                        throw new NameNotFoundException("ApplicationInfo is null");
                    }

                    Drawable drawable;
                    int customFlag = 0;

                    IconData iconData = createIconDataFromCacheEntry(cacheKey);
                    if (iconData.mType == 1){
                        iconData.mIconRes = getIconRes(cacheKey,iconData.mIconRes);
                    }

                    drawable = getDrawableFromIconData(iconData);
                    if (drawable != null){
                        customFlag = 1;
                    }
                    else {
                        drawable = appInfo.loadIcon(mPackageManager);
                    }

                    entry.icon = Utilities.createBadgedIconBitmap(
                            drawable, user, mContext);
                    entry.title = appInfo.loadLabel(mPackageManager);
                    entry.contentDescription = mUserManager.getBadgedLabelForUser(entry.title, user);
                    entry.isLowResIcon = false;

                    String customLabel = getCustomLabel(cacheKey.componentName);
                    if (customLabel != null)
                        entry.title = customLabel;

                    // Add the icon in the DB here, since these do not get written during
                    // package updates.
                    ContentValues values =
                            newContentValues(
                                    entry.icon,
                                    entry.title.toString(),
                                    mPackageBgColor,
                                    customFlag,
                                    iconData.mIconRes,
                                    iconData.mIconPath,
                                    iconData.mType
                            );
                    addIconToDB(values, cacheKey.componentName, info,
                            mUserManager.getSerialNumberForUser(user));

                } catch (NameNotFoundException e) {
                    if (DEBUG) Log.d(TAG, "Application not installed " + packageName);
                    entryUpdated = false;
                }
            }

            // Only add a filled-out entry to the cache
            if (entryUpdated) {
                mCache.put(cacheKey, entry);
            }
        }
        return entry;
    }

    public Bitmap drawableToBitmap (Drawable drawable) {

        if (drawable == null) return null;

        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);

        /*
        RectF rectF = new RectF(0,0,bitmap.getWidth(),bitmap.getHeight());
        float radius = mContext.getResources().getDimension(R.dimen.icon_round_corner);
        Path path = new Path();
        path.addRoundRect(rectF,radius,radius, Path.Direction.CW);
        canvas.clipPath(path);
        canvas.drawColor(Color.WHITE);
        drawable.draw(canvas);
         */

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);

        ClockDrawable.drawBgAndReturnSave(
                canvas,
                drawable,
                canvas.getWidth(),
                canvas.getHeight(),
                paint
        );

        return bitmap;
    }

    private void cachePkgWithLauncherIcon(String packageName, UserHandleCompat user, Bitmap launcherIconBitmap) {
        ComponentKey cacheKey = getPackageKey(packageName, user);
        CacheEntry entry = mCache.get(cacheKey);


        if (entry == null) {
            boolean entryUpdated = true;
            entry = new CacheEntry();

            try {
                int flags = UserHandleCompat.myUserHandle().equals(user) ? 0 :
                        PackageManager.GET_UNINSTALLED_PACKAGES;
                PackageInfo info = mPackageManager.getPackageInfo(packageName, flags);
                ApplicationInfo appInfo = info.applicationInfo;
                if (appInfo == null) {
                    throw new NameNotFoundException("ApplicationInfo is null");
                }
                entry.icon = launcherIconBitmap;
                entry.title = appInfo.loadLabel(mPackageManager);
                entry.contentDescription = mUserManager.getBadgedLabelForUser(entry.title, user);
                entry.isLowResIcon = false;

                // Add the icon in the DB here, since these do not get written during
                // package updates.
                ContentValues values =
                        newContentValues(
                                entry.icon,
                                entry.title.toString(),
                                mPackageBgColor,
                                0,
                                0,
                                null,
                                0
                        );
                addIconToDB(values, cacheKey.componentName, info,
                        mUserManager.getSerialNumberForUser(user));

            } catch (NameNotFoundException e) {
                entryUpdated = false;
            }


            // Only add a filled-out entry to the cache
            if (entryUpdated) {
                mCache.put(cacheKey, entry);
            }
        }
    }

    /**
     * Pre-load an icon into the persistent cache.
     * <p/>
     * <P>Queries for a component that does not exist in the package manager
     * will be answered by the persistent cache.
     *
     * @param componentName the icon should be returned for this component
     * @param icon          the icon to be persisted
     * @param dpi           the native density of the icon
     */
    public void preloadIcon(ComponentName componentName, Bitmap icon, int dpi, String label,
                            long userSerial, InvariantDeviceProfile idp) {
        // TODO rescale to the correct native DPI
        try {
            PackageManager packageManager = mContext.getPackageManager();
            packageManager.getActivityIcon(componentName);
            // component is present on the system already, do nothing
            return;
        } catch (PackageManager.NameNotFoundException e) {
            // pass
        }

        ContentValues values = newContentValues(
                Bitmap.createScaledBitmap(icon, idp.iconBitmapSize, idp.iconBitmapSize, true),
                label,
                Color.TRANSPARENT,
                0,
                0,
                null,
                0
        );
        values.put(IconDB.COLUMN_COMPONENT, componentName.flattenToString());
        values.put(IconDB.COLUMN_USER, userSerial);
        mIconDb.getWritableDatabase().insertWithOnConflict(IconDB.TABLE_NAME, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    private boolean getEntryFromDB(ComponentKey cacheKey, CacheEntry entry, boolean lowRes) {
        Cursor c = mIconDb.getReadableDatabase().query(IconDB.TABLE_NAME,
                new String[]{lowRes ? IconDB.COLUMN_ICON_LOW_RES : IconDB.COLUMN_ICON,
                        IconDB.COLUMN_LABEL},
                IconDB.COLUMN_COMPONENT + " = ? AND " + IconDB.COLUMN_USER + " = ?",
                new String[]{cacheKey.componentName.flattenToString(),
                        Long.toString(mUserManager.getSerialNumberForUser(cacheKey.user))},
                null, null, null);
        try {
            if (c.moveToNext()) {
                entry.icon = loadIconNoResize(c, 0, lowRes ? mLowResOptions : null);
                entry.isLowResIcon = lowRes;
                entry.title = c.getString(1);
                if (entry.title == null) {
                    entry.title = "";
                    entry.contentDescription = "";
                } else {
                    entry.contentDescription = mUserManager.getBadgedLabelForUser(
                            entry.title, cacheKey.user);
                }
                return true;
            }
        } finally {
            c.close();
        }
        return false;
    }

    public static class IconLoadRequest {
        private final Runnable mRunnable;
        private final Handler mHandler;

        IconLoadRequest(Runnable runnable, Handler handler) {
            mRunnable = runnable;
            mHandler = handler;
        }

        public void cancel() {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    /**
     * A runnable that updates invalid icons and adds missing icons in the DB for the provided
     * LauncherActivityInfoCompat list. Items are updated/added one at a time, so that the
     * worker thread doesn't get blocked.
     */
    @Thunk
    class SerializedIconUpdateTask implements Runnable {
        private final long mUserSerial;
        private final HashMap<String, PackageInfo> mPkgInfoMap;
        private final Stack<LauncherActivityInfoCompat> mAppsToAdd;
        private final Stack<LauncherActivityInfoCompat> mAppsToUpdate;
        private final HashSet<String> mUpdatedPackages = new HashSet<String>();

        @Thunk
        SerializedIconUpdateTask(long userSerial, HashMap<String, PackageInfo> pkgInfoMap,
                                 Stack<LauncherActivityInfoCompat> appsToAdd,
                                 Stack<LauncherActivityInfoCompat> appsToUpdate) {
            mUserSerial = userSerial;
            mPkgInfoMap = pkgInfoMap;
            mAppsToAdd = appsToAdd;
            mAppsToUpdate = appsToUpdate;
        }

        @Override
        public void run() {
            if (!mAppsToUpdate.isEmpty()) {
                LauncherActivityInfoCompat app = mAppsToUpdate.pop();
                String cn = app.getComponentName().flattenToString();
                ContentValues values = updateCacheAndGetContentValues(app, true);
                mIconDb.getWritableDatabase().update(IconDB.TABLE_NAME, values,
                        IconDB.COLUMN_COMPONENT + " = ? AND " + IconDB.COLUMN_USER + " = ?",
                        new String[]{cn, Long.toString(mUserSerial)});
                mUpdatedPackages.add(app.getComponentName().getPackageName());

                if (mAppsToUpdate.isEmpty() && !mUpdatedPackages.isEmpty()) {
                    // No more app to update. Notify model.
                    LauncherAppState.getInstance().getModel().onPackageIconsUpdated(
                            mUpdatedPackages, mUserManager.getUserForSerialNumber(mUserSerial));
                }

                // Let it run one more time.
                scheduleNext();
            } else if (!mAppsToAdd.isEmpty()) {
                LauncherActivityInfoCompat app = mAppsToAdd.pop();
                PackageInfo info = mPkgInfoMap.get(app.getComponentName().getPackageName());
                if (info != null) {
                    synchronized (IconCache.this) {
                        addIconToDBAndMemCache(app, info, mUserSerial);
                    }
                }

                if (!mAppsToAdd.isEmpty()) {
                    scheduleNext();
                }
            }
        }

        public void scheduleNext() {
            mWorkerHandler.postAtTime(this, ICON_UPDATE_TOKEN, SystemClock.uptimeMillis() + 1);
        }
    }

    private void updateSystemStateString() {
        mSystemState = Locale.getDefault().toString();
    }

    public static final class IconDB extends SQLiteOpenHelper {
        // v12: iOS system-app icons (ic_app_browser/gallery/market/mms/note/settings/
        // calendar/camera/clock/music/phone swapped to iOS art). Bump to invalidate the
        // cached bitmaps so the new icons show on existing installs.
        // v13: re-rendered the iOS icons from 256px sources, full-bleed & centered.
        // v14: pre-shaped iOS icons (đã bo góc sẵn, góc trong suốt) được render NGUYÊN XI —
        // bỏ bước mask/clip bo góc của launcher đè lên (làm cắt/lệch góc). Bump để dựng lại cache.
        // v15: nhận diện pre-shaped lấy mẫu góc sâu hơn (Utilities.hasTransparentCorners) để
        // icon squircle thụt vào (Music/Phone) không bị tô nền trắng -> hết VÒNG TRẮNG ở mép.
        // v16: thêm 6 logo iOS cho app Google (Gmail/Maps/News/TV/Wallet/Meet). Bump để xoá
        // bitmap cache cũ (logo gốc) -> vẽ lại bằng logo iOS mới.
        // v17: gỡ gm/apps.maps/videos/apps.tachyon khỏi def_cts_app_list (CTS chặn re-skin).
        // Bump để xoá blob logo gốc của Gmail/Maps/Google TV/Meet -> vẽ lại bằng logo iOS.
        // v18: gỡ apps.photos khỏi CTS + thêm Keep vào app_note. Bump để xoá blob Photos gốc
        // (chong chóng Google) -> vẽ lại ic_app_gallery; và dựng logo ic_app_note cho Keep.
        public static int DB_VERSION = 18;

        public final static String TABLE_NAME = "icons";
        public final static String COLUMN_ROWID = "rowid";
        public final static String COLUMN_COMPONENT = "componentName";
        public final static String COLUMN_USER = "profileId";
        public final static String COLUMN_LAST_UPDATED = "lastUpdated";
        public final static String COLUMN_VERSION = "version";
        public final static String COLUMN_ICON = "icon";
        public final static String COLUMN_ICON_LOW_RES = "icon_low_res";
        public final static String COLUMN_LABEL = "label";
        public final static String COLUMN_SYSTEM_STATE = "system_state";
        public final static String COLUMN_HISTORY = "history";
        public final static String COLUMN_CUSTOM_ICON = "customIcon";
        public final static String COLUMN_ICON_NAME = "iconName";
        public final static String COLUMN_DATA1 = "data1";
        public final static String COLUMN_DATA2 = "data2";
        public final static String COLUMN_DATA3 = "data3";
        public final static String COLUMN_DATA4 = "data4";
        public final static String COLUMN_DATA5 = "data5";
        public final static String COLUMN_DATA6 = "data6";

        public IconDB(Context context) {
            super(context, LauncherFiles.APP_ICONS_DB, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    COLUMN_COMPONENT + " TEXT NOT NULL, " +
                    COLUMN_USER + " INTEGER NOT NULL, " +
                    COLUMN_LAST_UPDATED + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_VERSION + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_ICON + " BLOB, " +
                    COLUMN_ICON_LOW_RES + " BLOB, " +
                    COLUMN_LABEL + " TEXT, " +
                    COLUMN_SYSTEM_STATE + " TEXT, " +
                    COLUMN_HISTORY + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_CUSTOM_ICON + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_ICON_NAME + " TEXT, " +
                    COLUMN_DATA1 + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_DATA2 + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_DATA3 + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_DATA4 + " TEXT, " +
                    COLUMN_DATA5 + " TEXT, " +
                    COLUMN_DATA6 + " TEXT, " +
                    "PRIMARY KEY (" + COLUMN_COMPONENT + ", " + COLUMN_USER + ") " +
                    ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion != newVersion) {
                clearDB(db);
            }
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion != newVersion) {
                clearDB(db);
            }
        }

        private void clearDB(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }

        public final Cursor query(String[] columns, String selection, String[] selectionArgs) {
            return getReadableDatabase().query(
                    TABLE_NAME,
                    columns,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
            );
        }

    }

    private ContentValues newContentValues(Bitmap icon, String label, int lowResBackgroundColor, int customFlag, int iconRes, String iconPath, int type) {
        ContentValues values = new ContentValues();
        values.put(IconDB.COLUMN_ICON, Utilities.flattenBitmap(icon));
        values.put(IconDB.COLUMN_HISTORY,System.currentTimeMillis());
        values.put(IconDB.COLUMN_LABEL, label);
        values.put(IconDB.COLUMN_SYSTEM_STATE, mSystemState);

        if (customFlag > 0){
            try {
                if (type == 1 && iconRes > 0){
                    values.put(IconDB.COLUMN_DATA1,0);
                    values.put(IconDB.COLUMN_ICON_NAME,mContext.getResources().getResourceEntryName(iconRes));
                }
                else if (type == 2){
                    values.put(IconDB.COLUMN_DATA1,1);
                    values.put(IconDB.COLUMN_ICON_NAME,iconPath);
                }
            }
            catch (Throwable th){
                th.getMessage();
            }
        }

        if (lowResBackgroundColor == Color.TRANSPARENT) {
            values.put(IconDB.COLUMN_ICON_LOW_RES, Utilities.flattenBitmap(
                    Bitmap.createScaledBitmap(icon,
                            icon.getWidth() / LOW_RES_SCALE_FACTOR,
                            icon.getHeight() / LOW_RES_SCALE_FACTOR, true)));
        } else {
            synchronized (this) {
                if (mLowResBitmap == null) {
                    mLowResBitmap = Bitmap.createBitmap(icon.getWidth() / LOW_RES_SCALE_FACTOR,
                            icon.getHeight() / LOW_RES_SCALE_FACTOR, Bitmap.Config.RGB_565);
                    mLowResCanvas = new Canvas(mLowResBitmap);
                    mLowResPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
                }
                mLowResCanvas.drawColor(lowResBackgroundColor);
                mLowResCanvas.drawBitmap(icon, new Rect(0, 0, icon.getWidth(), icon.getHeight()),
                        new Rect(0, 0, mLowResBitmap.getWidth(), mLowResBitmap.getHeight()),
                        mLowResPaint);
                values.put(IconDB.COLUMN_ICON_LOW_RES, Utilities.flattenBitmap(mLowResBitmap));
            }
        }
        return values;
    }

    private static Bitmap loadIconNoResize(Cursor c, int iconIndex, BitmapFactory.Options options) {
        byte[] data = c.getBlob(iconIndex);
        try {
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } catch (Exception e) {
            return null;
        }
    }

    public Bitmap getIconDrawable(ComponentName componentName){
        ComponentKey cacheKey = new ComponentKey(componentName,  UserHandleCompat.myUserHandle());
//        ComponentKey cacheKey = getPackageKey(packageName, UserHandleCompat.myUserHandle());
        CacheEntry entry = mCache.get(cacheKey);
        if (entry != null)
        {
            return entry.icon;
        }
        return null;
    }

    public int getIconRes(ComponentKey componentKey, int i){
        if (i == -1) return -1;
        if (i != 0) return Math.max(i, 0);
        String label = "";
        try {
            String cmpName = componentKey.componentName.flattenToString();
            String pkgName = componentKey.componentName.getPackageName();
            try {
                ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(pkgName, 128);
                label = applicationInfo != null ? this.mPackageManager.getApplicationLabel(applicationInfo).toString() : "";
            } catch (Throwable unused) {
            }
            if (isSystemApp(pkgName)) {
                if (!cmpName.contains("dialer") && !cmpName.contains("dialtacts")) {
                    if (cmpName.contains("gmail") || !(cmpName.contains("mms") || cmpName.contains("messaging") || cmpName.contains("conversations"))) {
                        if (!cmpName.contains("contacts") && !cmpName.contains("socialphonebook")) {
                            if (cmpName.contains("google") || !cmpName.contains("settings")) {
                                if (!cmpName.contains("email")) {
                                    if (!cmpName.contains("gallery") && !cmpName.contains("album")) {
                                        if (cmpName.contains("vending")) {
                                            if (hasNoTemp(pkgName, R.drawable.new_store_icon)) {
                                                return R.drawable.new_store_icon;
                                            }
                                        } else if (!cmpName.contains("browser") || cmpName.contains("vending") || cmpName.contains("com.sonyericsson.video") || cmpName.contains("com.mobisystems.files") || cmpName.contains("com.dropbox.android") || cmpName.contains("com.miui.player") || cmpName.contains("com.lge.music") || label.contains("music") || label.contains("file")) {
                                            if (cmpName.contains("video") || !cmpName.contains("health")) {
                                                if (cmpName.contains("music")) {
                                                    if (hasNoTemp(pkgName, R.drawable.new_music_icon)) {
                                                        return R.drawable.new_music_icon;
                                                    }
                                                } else if (cmpName.contains("calculator")) {
                                                    if (hasNoTemp(pkgName, R.drawable.new_calculator_icon)) {
                                                        return R.drawable.new_calculator_icon;
                                                    }
                                                } else if (cmpName.contains("camera")) {
                                                    if (hasNoTemp(pkgName, R.drawable.new_camera_icon)) {
                                                        return R.drawable.new_camera_icon;
                                                    }
                                                } else if (cmpName.contains("voice") || (!(cmpName.contains("memo") || cmpName.contains("note")) || cmpName.contains("com.sony.playmemories.mobile"))) {
                                                     if ((cmpName.contains("voicenote") || cmpName.contains("record")) && !cmpName.contains("screenrecorder")) {
                                                        if (hasNoTemp(pkgName, R.drawable.new_voice_memos_icon)) {
                                                            return R.drawable.new_voice_memos_icon;
                                                        }
                                                    } else if ((!cmpName.contains("clock") && !cmpName.contains("organizer")) || cmpName.contains("wearable") || cmpName.contains("smsorganizer")) {
                                                        if (cmpName.contains("calendar")) {
                                                            if (hasNoTemp(pkgName, R.drawable.calendar_icon)) {
                                                                return R.drawable.calendar_icon;
                                                            }
                                                        } else if (cmpName.contains("compass") && hasNoTemp(pkgName, R.drawable.compass_icon)) {
                                                            return R.drawable.compass_icon;
                                                        }
                                                    } else if (hasNoTemp(pkgName, R.drawable.clock_icon)) {
                                                        return R.drawable.clock_icon;
                                                    }
                                                } else if (hasNoTemp(pkgName, R.drawable.new_notes_icon)) {
                                                    return R.drawable.new_notes_icon;
                                                }
                                            } else if (hasNoTemp(pkgName, R.drawable.health_icon)) {
                                                return R.drawable.health_icon;
                                            }
                                        } else if (hasNoTemp(pkgName, R.drawable.new_safari_icon)) {
                                            return R.drawable.new_safari_icon;
                                        }
                                    }
                                    else if (hasNoTemp(pkgName, R.drawable.new_photos_icon)) {
                                        return R.drawable.new_photos_icon;
                                    }
                                } else if (hasNoTemp(pkgName, R.drawable.new_email_icon)) {
                                    return R.drawable.new_email_icon;
                                }
                            } else if (hasNoTemp(pkgName, R.drawable.new_settings_icon)) {
                                return R.drawable.new_settings_icon;
                            }
                        }
                        else if (hasNoTemp(pkgName,R.drawable.new_contact_icon))
                            return R.drawable.new_contact_icon;
                    } else if (hasNoTemp(pkgName, R.drawable.new_message_icon)) {
                        return R.drawable.new_message_icon;
                    }
                }
                else if (hasNoTemp(pkgName,R.drawable.new_call_icon))
                return R.drawable.new_call_icon;
            }
            if (cmpName.contains("com.samsung.android.email")) {
                if (hasNoTemp(pkgName, R.drawable.new_email_icon)) {
                    return R.drawable.new_email_icon;
                }
            } else if (cmpName.contains("video") || !cmpName.contains("health")) {
                if ((cmpName.contains("radio") || cmpName.contains("com.sec.android.app.fm")) && hasNoTemp(pkgName, R.drawable.ic_radio)) {
                    return R.drawable.ic_radio;
                }
            } else if (hasNoTemp(pkgName, R.drawable.health_icon)) {
                return R.drawable.health_icon;
            }
            if (cmpName.contains("com.google.android.googlequicksearchbox")) {
                if (hasNoTemp(pkgName, R.drawable.google_icon)) {
                    return R.drawable.google_icon;
                }
            } else if (cmpName.contains("com.android.chrome")) {
                if (hasNoTemp(pkgName, R.drawable.chrome_icon)) {
                    return R.drawable.chrome_icon;
                }
            } else if (cmpName.contains("com.google.android.apps.translate")) {
                if (hasNoTemp(pkgName, R.drawable.translate_icon)) {
                    return R.drawable.translate_icon;
                }
            } else if (cmpName.contains("com.google.android.play.games")) {
                if (hasNoTemp(pkgName, R.drawable.game_icon)) {
                    return R.drawable.game_icon;
                }
            } else if (cmpName.contains("com.google.android.apps.photos")) {
                if (hasNoTemp(pkgName, R.drawable.google_photos_icon)) {
                    return R.drawable.google_photos_icon;
                }
            } else if (pkgName.equals("com.google.android.apps.docs")) {
                if (hasNoTemp(pkgName, R.drawable.drive_icon)) {
                    return R.drawable.drive_icon;
                }
            } else if (cmpName.contains("com.google.android.keep")) {
                if (hasNoTemp(pkgName, R.drawable.keep_icon)) {
                    return R.drawable.keep_icon;
                }
            } else if (cmpName.contains("com.google.android.gm")) {
                if (hasNoTemp(pkgName, R.drawable.gmail_icon)) {
                    return R.drawable.gmail_icon;
                }
            } else if (cmpName.contains("com.google.android.youtube") && !cmpName.contains("videos") && hasNoTemp(pkgName, R.drawable.youtube_icon)) {
                return R.drawable.youtube_icon;
            }
            if (cmpName.contains("com.sec.android.app.music")) {
                if (hasNoTemp(pkgName, R.drawable.new_music_icon)) {
                    return R.drawable.new_music_icon;
                }
            } else if (cmpName.contains("com.sec.android.app.sbrowser")) {
                if (hasNoTemp(pkgName, R.drawable.new_safari_icon)) {
                    return R.drawable.new_safari_icon;
                }
            } else if (cmpName.contains("com.samsung.android.app.notes")) {
                if (hasNoTemp(pkgName, R.drawable.new_notes_icon)) {
                    return R.drawable.new_notes_icon;
                }
            } else if (cmpName.contains("com.sec.android.app.popupcalculator")) {
                if (hasNoTemp(pkgName, R.drawable.new_calculator_icon)) {
                    return R.drawable.new_calculator_icon;
                }
            } else if (cmpName.contains("com.sec.android.app.voicenote")) {
                if (hasNoTemp(pkgName, R.drawable.new_voice_memos_icon)) {
                    return R.drawable.new_voice_memos_icon;
                }
            } else if (cmpName.contains("com.google.android.apps.maps") && hasNoTemp(pkgName, R.drawable.google_maps_icon)) {
                return R.drawable.google_maps_icon;
            }
            if (!label.equals("facebook") && !cmpName.contains("com.facebook.katana") && !cmpName.contains("com.facebook.lite")) {
                if (!label.equals("whatsapp") && !cmpName.contains("com.whatsapp")) {
                    if (!label.equals("instagram") && !cmpName.contains("com.instagram.android")) {
                        if (!label.equals("twitter") && !cmpName.contains("com.twitter.android")) {
                            if (!label.equals("snapchat") && !cmpName.contains("com.snapchat.android")) {
                                if (!label.equals("dropbox") && !cmpName.contains("com.dropbox.android")) {
                                    if (!label.equals("line") && !cmpName.contains("jp.naver.line.android")) {
                                        if (!label.equals("viber") && !cmpName.contains("com.viber.voip")) {
                                            if (!label.equals("flipboard") && !cmpName.contains("flipboard.app")) {
                                                if (!label.equals("spotify") && !cmpName.contains("com.spotify.music")) {
                                                    if (!label.equals("uber") && !cmpName.contains("com.ubercab")) {
                                                        if (cmpName.contains("com.google.android.apps.translate")) {
                                                            return R.drawable.translate_icon;
                                                        }
                                                        if (!cmpName.contains("com.samsung.android.game.gamehome") && !cmpName.contains("com.google.android.play.games")) {
                                                            if (cmpName.contains("com.google.android.keep")) {
                                                                return R.drawable.keep_icon;
                                                            }
                                                            if (cmpName.contains("com.google.android.apps.tachyon")) {
                                                                return R.drawable.new_face_time_icon;
                                                            }
                                                            if (!cmpName.contains("com.google.android.apps.nbu.files") && !cmpName.contains("com.sec.android.app.myfiles") && !cmpName.contains("com.mi.android.globalFileexplorer") && !cmpName.contains("com.asus.filemanager")) {
                                                                if (cmpName.contains("com.samsung.android.video")) {
                                                                    return R.drawable.ic_videos;
                                                                }
                                                                if (cmpName.contains("com.netflix.mediaclient")) {
                                                                    return R.drawable.ic_netflix;
                                                                }
                                                                if (cmpName.contains("com.xiaomi.hm.health")) {
                                                                    return R.drawable.ic_mifit;
                                                                }
                                                                if (cmpName.contains("com.google.android.apps.podcasts")) {
                                                                    return R.drawable.ic_podcasts;
                                                                }
                                                                if (!cmpName.contains("com.google.android.apps.magazines") && !cmpName.contains("com.microsoft.amp.apps.bingnews")) {
                                                                    if (!cmpName.contains("com.facebook.orca") && !cmpName.contains("com.facebook.mlite")) {
                                                                        if (cmpName.contains("com.google.android.apps.chromecast.app")) {
                                                                            return R.drawable.ic_home;
                                                                        }
                                                                        if (cmpName.contains("com.discord")) {
                                                                            return R.drawable.ic_discord;
                                                                        }
                                                                        if (!cmpName.contains("deezer.android.app") && !cmpName.contains("deezer.android.tv")) {
                                                                            if (cmpName.contains("com.pinterest")) {
                                                                                return R.drawable.ic_pinterest;
                                                                            }
                                                                            if (!cmpName.contains("com.ss.android.ugc.trill") && !cmpName.contains("com.ss.android.ugc.trill.go")) {
                                                                                if (!cmpName.contains("com.shazam.android") && !cmpName.contains("com.shazam.android.lite")) {
                                                                                    if (cmpName.contains("com.fusionmedia.investing")) {
                                                                                        return R.drawable.ic_stocks;
                                                                                    }
                                                                                    if (cmpName.contains("com.google.android.apps.books")) {
                                                                                        return R.drawable.ic_book;
                                                                                    }
                                                                                    if (cmpName.contains("org.telegram.messenger")) {
                                                                                        return R.drawable.ic_telegram;
                                                                                    }
                                                                                    return 0;
                                                                                }
                                                                                return R.drawable.ic_shazam;
                                                                            }
                                                                            return R.drawable.ic_tik_tok;
                                                                        }
                                                                        return R.drawable.ic_deezer;
                                                                    }
                                                                    return R.drawable.ic_messenger;
                                                                }
                                                                return R.drawable.new_news_icon;
                                                            }
                                                            return R.drawable.new_file_icon;
                                                        }
                                                        return R.drawable.game_icon;
                                                    }
                                                    else return R.drawable.uber_icon;
                                                }
                                                else return R.drawable.spotify_icon;
                                            }
                                            else return R.drawable.flipboard_icon;
                                        }
                                        else return R.drawable.viber_icon;
                                    }
                                    else return R.drawable.line_icon;
                                }
                                else return R.drawable.dropbox_icon;
                            }
                            else return R.drawable.snapchat_icon;
                        }
                        else return R.drawable.twitter_icon;
                    }
                    else return R.drawable.instagram_icon;
                }
                else return R.drawable.whatsapp_icon;
            }
            else return R.drawable.facebook_icon;
        } catch (Throwable th) {
            th.getMessage();
            return 0;
        }
    }

    public int getCustomizedCategory(ComponentName componentName){
        try {
            return this.mSharedPreferences.getInt(componentName.flattenToString(), 0);
        } catch (Throwable unused) {
            return 0;
        }
    }

    public void setCustomizeCategory(String componentName, int category){
        SharedPreferences.Editor editor = this.mSharedPreferences.edit();
        editor.putInt(componentName,category);
        if (!editor.commit()) editor.apply();
    }

    public void setCustomAppIcon(LauncherActivityInfoCompat infoCompat, PackageInfo packageInfo, long userSerial, int iconRes, String iconPath, int type){
        ComponentKey componentKey = new ComponentKey(infoCompat.getComponentName(),infoCompat.getUser());
        CacheEntry cacheEntry = new CacheEntry();
        Drawable drawable = null;
        CharSequence label = null;
        int resType = 0;
        if (type == 1){
            int icon = getIconRes(componentKey, iconRes);
            if (icon > 0){
                drawable = ContextCompat.getDrawable(mContext,icon);
            }
            if (drawable != null){
                resType = 1;
            }
            else {
                drawable = infoCompat.getIcon(mIconDpi);
                resType = 0;
            }
        }
        else if (type == 2){
            if (!TextUtils.isEmpty(iconPath)){
                drawable = Drawable.createFromPath(iconPath);
            }
            if (drawable == null){
                drawable = infoCompat.getIcon(mIconDpi);
            }
        }

        cacheEntry.icon = drawableToBitmap(drawable);
        cacheEntry.title = getCustomLabel(componentKey.componentName);
        if (TextUtils.isEmpty(cacheEntry.title)){
            cacheEntry.title = infoCompat.getLabel();
        }
        cacheEntry.contentDescription = mUserManager.getBadgedLabelForUser(cacheEntry.title,infoCompat.getUser());

        this.mCache.put(
                componentKey,
                cacheEntry
        );

        addIconToDB(
                newContentValues(
                        cacheEntry.icon,
                        cacheEntry.title.toString(),
                        mActivityBgColor,
                        resType,
                        iconRes,
                        iconPath,
                        type
                ),
                infoCompat.getComponentName(),
                packageInfo,
                userSerial
        );

    }

    public void setCustomAppLabel(String componentName, String customAppLabel){
        SharedPreferences sharedPreferences = this.mSharedPreferences;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(CUSTOM_LABEL_PREFIX + componentName, customAppLabel);
        if (!editor.commit()) editor.apply();
    }

    public void updateIconDB(ContentValues contentValues, String[] values) {
        String where = IconDB.COLUMN_COMPONENT + " = ? AND " + IconDB.COLUMN_USER + " =  ?";
        mIconDb.getWritableDatabase().update(
                IconDB.TABLE_NAME,
                contentValues,
                where,
                values
        );
    }

    @SuppressLint("Range")
    public IconData createIconDataFromCacheEntry(ComponentKey componentKey){
        IconData iconData = new IconData();
        Cursor cursor;
        try {
            cursor = mIconDb.query(
                        new String[]{IconDB.COLUMN_ICON_NAME, IconDB.COLUMN_DATA1, IconDB.COLUMN_DATA4},
                        "componentName = ? AND profileId = ?",
                        new String[]{
                                componentKey.componentName.flattenToString(),
                                String.valueOf(mUserManager.getSerialNumberForUser(componentKey.user))
                        }
                    );
            if (cursor.moveToNext()) {
                String iconName = cursor.getString(cursor.getColumnIndex(IconDB.COLUMN_ICON_NAME));
                int data1 = cursor.getInt(cursor.getColumnIndex(IconDB.COLUMN_DATA1));
                if (data1 == 2 && !TextUtils.isEmpty(iconName)) {
                    cursor.close();
                    return new IconData(0, iconName, 2);
                }
                if (data1 == 1 && !TextUtils.isEmpty(iconName)) {
                    int res = 0;
                    try {
                        res = mContext.getResources().getIdentifier(iconName, "drawable", mContext.getPackageName());
                    } catch (Throwable th) {
                    }
                    cursor.close();
                    return new IconData(res, null, 1);
                }
            }
        }
        catch (Throwable th){
            cursor = null;
        }

        return iconData;
    }

    public Drawable getDrawableFromIconData(IconData iconData){
        if (iconData == null) return null;
        try {
            if (iconData.mType != 2){
                if (iconData.mType == 1 && iconData.mIconRes > 0){
                    return ContextCompat.getDrawable(
                            mContext,
                            iconData.mIconRes
                    );
                }
                return null;
            }
            try {
                if (TextUtils.isEmpty(iconData.mIconPath)) return null;
                return Drawable.createFromPath(iconData.mIconPath);
            }
            catch (Throwable th){
                return null;
            }
        }
        catch (Throwable th){
            return null;
        }
    }

    public final boolean hasNoTemp(String pkgName, int res){
        if (this.mTempIcons.containsKey(res))
            return false;
        this.mTempIcons.put(pkgName, res);
        return true;
    }

    public final boolean isSystemApp(String pkgName) {
        ApplicationInfo applicationInfo;
        if (pkgName != null) {
            try {
                PackageInfo packageInfo = this.mPackageManager.getPackageInfo(pkgName, 0);
                if (packageInfo == null || (applicationInfo = packageInfo.applicationInfo) == null) {
                    return false;
                }
                return (applicationInfo.flags & 1) != 0;
            } catch (Throwable unused) {
                return false;
            }
        }
        return false;
    }


}

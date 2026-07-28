package com.amz.ios.launcher.awareness;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;


import com.amz.ios.ioslite.common.provider.AppUsagesProvider;
import com.amz.ios.ioslite.common.util.CommonUtilities;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherModel;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class AppUsagesModel {
    private static final boolean DEBUG = false;
    private static final String TAG = "AppUsagesModel";

    private final LauncherAppState mApp;
    private final Context mContext;

    final Handler mWorkerHandler;
    final Handler mHandler = new Handler();
    private boolean mHasLoaderCompletedOnce;
    private boolean mLoading;

    static final Object sLock = new Object();
    public static final HashMap<ComponentName, UsageInfo> sComponentUsageMap = new HashMap<>();
    static final ArrayList<ComponentName> sRecentComps = new ArrayList<>();

    private WeakReference<Callbacks> mCallbacks;

    public interface Callbacks {
        void onRecentChange(ArrayList<ComponentName> sortComps);
    }

    public AppUsagesModel(LauncherAppState app) {
        mApp = app;
        mContext = app.getContext();
        mWorkerHandler = new Handler(LauncherModel.getWorkerLooper());
    }

    public void initialize(Callbacks callbacks) {
        mCallbacks = new WeakReference<Callbacks>(callbacks);
    }

    private Callbacks getCallback() {
        return mCallbacks != null ? mCallbacks.get() : null;
    }

    public void onLaunch(final Context context, final ComponentName cn) {
        if (!mHasLoaderCompletedOnce || cn == null) {
            return;
        }

        Runnable request = new Runnable() {
            @Override
            public void run() {
                synchronized (sLock) {
                    UsageInfo usage = sComponentUsageMap.get(cn);
                    if (usage == null) {
                        usage = new UsageInfo(cn);
                        sComponentUsageMap.put(cn, usage);
                        usage.onLaunch(context, true /*newAppUsage*/);
                    } else {
                        usage.onLaunch(context, false /*newAppUsage*/);
                    }

                    sortByLaunchTime();
                }
            }
        };
        mWorkerHandler.post(request);
    }


    public static void addItemInDatabase(Context context, UsageInfo item) {
        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();

        long id = generateNewItemId();
        item.setItemId(id);

        item.onAddToDatabase(values);
        cr.insert(AppUsagesProvider.Usages.CONTENT_URI, values);
        cr.notifyChange(AppUsagesProvider.Usages.CONTENT_URI, null);
    }

    public static void updateItemInDatabase(Context context, UsageInfo item) {
        final ContentValues values = new ContentValues();
        item.onAddToDatabase(values);

        final long itemId = item.id;
        final Uri uri = AppUsagesProvider.Usages.getContentUri(itemId);
        final ContentResolver cr = context.getContentResolver();
        cr.update(uri, values, null, null);
        cr.notifyChange(uri, null);
    }

    public static long generateNewItemId() {
        long maxItemId = 0;
        for (UsageInfo info : sComponentUsageMap.values()) {
            if (maxItemId < info.id) {
                maxItemId = info.id;
            }
        }
        return maxItemId + 1;
    }

    public void loadUsages() {
        if (mHasLoaderCompletedOnce || mLoading) {
            return;
        }
        mLoading = true;
        Runnable load = new Runnable() {
            @Override
            public void run() {
                synchronized (sLock) {
                    loadUsagesFromDb();
                    sortByLaunchTime();
                    mLoading = false;
                    mHasLoaderCompletedOnce = true;
                }
            }
        };

        mWorkerHandler.post(load);
    }

    private void sortByLaunchTime() {
        synchronized (sLock) {
            final ArrayList<ComponentName> launchComps = new ArrayList<>();
            launchComps.addAll(sComponentUsageMap.keySet());
            Collections.sort(launchComps, new Comparator<ComponentName>() {
                @Override
                public int compare(ComponentName o1, ComponentName o2) {
                    return CommonUtilities.compare(sComponentUsageMap.get(o2).getLatestLaunchTime(), sComponentUsageMap.get(o1).getLatestLaunchTime());
                }
            });

            if (sRecentComps.isEmpty() || sRecentComps.get(0) != launchComps.get(0)) {
                sRecentComps.clear();
                sRecentComps.addAll(launchComps);
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Callbacks cb = getCallback();
                        if (cb != null) {
                            cb.onRecentChange((ArrayList<ComponentName>) sRecentComps.clone());
                        }
                    }
                };
                mHandler.postDelayed(runnable, 1500);
            }
        }
    }

    private void loadUsagesFromDb() {
        sComponentUsageMap.clear();
        final Context context = mContext;
        final ContentResolver contentResolver = context.getContentResolver();
        final Uri contentUri = AppUsagesProvider.Usages.CONTENT_URI;
        if (DEBUG) Log.d(TAG, "loading  from " + contentUri);
        final Cursor cursor = contentResolver.query(contentUri, null, null, null, null);
        try {
            final int idIndex = cursor.getColumnIndexOrThrow(AppUsagesProvider.Usages._ID);
            final int componentNameIndex = cursor.getColumnIndexOrThrow
                    (AppUsagesProvider.Usages.APP_COMPONENTNAME);
            final int launchTimesIndex = cursor.getColumnIndexOrThrow
                    (AppUsagesProvider.Usages.APP_LAUNCH_TIMES);
            final int launchLatestTimeIndex = cursor.getColumnIndexOrThrow
                    (AppUsagesProvider.Usages.APP_LAUNCH_LATEST_TIME);

            String componentNameString;
            long launchTimes;
            long launchLastestTime;
            ComponentName componentName;
            UsageInfo usageInfo;
            long id;
            while (cursor.moveToNext()) {
                componentNameString = cursor.getString(componentNameIndex);
                launchTimes = cursor.getLong(launchTimesIndex);
                launchLastestTime = cursor.getLong(launchLatestTimeIndex);
                id = cursor.getLong(idIndex);

                componentName = ComponentName.unflattenFromString(componentNameString);
                usageInfo = new UsageInfo(componentName);
                usageInfo.setLaunchTimes(launchTimes);
                usageInfo.setLaunchLastestTime(launchLastestTime);
                usageInfo.setItemId(id);

                sComponentUsageMap.put(componentName, usageInfo);
            }
        } catch (Exception e) {
            Log.e(TAG, "loading interrupted", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}

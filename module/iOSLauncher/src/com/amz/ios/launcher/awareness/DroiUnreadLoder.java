package com.amz.ios.launcher.awareness;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.amz.ios.ioslite.common.ContextHelper;
import com.amz.ios.ioslite.common.debug.DebugUtil;
import com.amz.ios.launcher.Launcher;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;


public class DroiUnreadLoder extends UnreadLoaderCompact {
    private static final String TAG = "DroiUnreadLoder";
    private static final int INITIAL_BADGE_CAPACITY = 10;
    private static final Map<ComponentName, Integer> UNREAD_SUPPORT_SHORTCUTS = new HashMap<>(INITIAL_BADGE_CAPACITY);

    private static final String ACTION_UNREAD_CHANGED = "com.ios.intent.action.BADGE_COUNT_DASHBOARD";
    private static Uri AUTHORITY_URI = Uri.parse("content://com.ios.badge");
    private static Uri BADGE_URI
            = Uri.withAppendedPath(AUTHORITY_URI, "apps");
    private static Uri BADGE_INTERNAL_URI
            = Uri.withAppendedPath(AUTHORITY_URI, "internal");
    private static final String[] BADGE_COLUMNS = {
            "package",
            "class",
            "badgecount"
    };

    private static final int INDEX_PACKAGE = 0;
    private static final int INDEX_CLASS = 1;
    private static final int INDEX_COUNT = 2;

    private Context mContext;
    private WeakReference<UnreadCallbacks> mCallbacks;
    private boolean isInited;

    private ContentObserver mObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange, Uri uri) {
            Log.d(TAG, "onChange: " + uri);
            initUnreadNumberFromSystem(true);
        }
    };


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: " + action);
            if (ACTION_UNREAD_CHANGED.equals(action)) {
                initUnreadNumberFromSystem(true);
            }
        }
    };

    public DroiUnreadLoder(Context context) {
        mContext = context;
    }

    public void initInitFlag() {
        isInited = false;
    }

    @Override
    public void initialize(Launcher launcher, UnreadCallbacks callbacks) {
        if (!isInited) {
            initialBadgeAuthority();

            mContext.getContentResolver().registerContentObserver(BADGE_URI, true, mObserver);
            mContext.getContentResolver().registerContentObserver(BADGE_INTERNAL_URI, true, mObserver);
            mCallbacks = new WeakReference<UnreadCallbacks>(callbacks);

            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_UNREAD_CHANGED);
            ContextHelper.registerReceiver(launcher, mReceiver, filter);
            isInited = true;
        }
    }

    @Override
    public void loadAndInitUnreadShortcuts() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                initUnreadNumberFromSystem(false);
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                if (mCallbacks != null) {
                    UnreadCallbacks callbacks = mCallbacks.get();
                    if (callbacks != null) {
                        callbacks.bindUnreadInfoIfNeeded();
                    }
                }
            }
        }.execute();
    }

    @Override
    public void onCancel(Launcher launcher) {
        if (isInited) {
            mCallbacks = new WeakReference<UnreadCallbacks>(null);
            mContext.getContentResolver().unregisterContentObserver(mObserver);
            launcher.unregisterReceiver(mReceiver);

            isInited = false;
        }
    }

    @Override
    synchronized int getUnreadNumberOfComp(ComponentName component) {
        if (UNREAD_SUPPORT_SHORTCUTS.get(component) != null) {
            return UNREAD_SUPPORT_SHORTCUTS.get(component);
        }
        return 0;
    }

    synchronized void putUnreadNumberOfComp(ComponentName component, int count) {
        UNREAD_SUPPORT_SHORTCUTS.put(component, count);
    }

    private void initUnreadNumberFromSystem(boolean notifyChange) {
        Log.d(TAG, "initUnreadNumberFromSystem: " + notifyChange);
        Cursor c = mContext.getContentResolver().query(BADGE_URI, BADGE_COLUMNS, null, null, null);
        if (c != null) try {
            while (c.moveToNext()) {
                String packageName = c.getString(INDEX_PACKAGE);
                String className = c.getString(INDEX_CLASS);
                int count = c.getInt(INDEX_COUNT);
                if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(className)) {
                    ComponentName cn = new ComponentName(packageName, className);
                    if (notifyChange) {
                        Log.d(TAG, cn.toShortString() + " : " + count);
                        final UnreadCallbacks callbacks = mCallbacks.get();
                        if (callbacks != null && getUnreadNumberOfComp(cn) != count) {
                            callbacks.bindComponentUnreadChanged(cn, count);
                        }
                    }
                    putUnreadNumberOfComp(cn, count);
                }
            }
        } finally {
            c.close();
        }
    }

    public static void initialBadgeAuthority() {
        String authority = getDroiUnreadAuthority();
        if (!TextUtils.isEmpty(authority)) {
            AUTHORITY_URI = Uri.parse("content://" + authority);
            BADGE_URI = Uri.withAppendedPath(AUTHORITY_URI, "apps");
            BADGE_INTERNAL_URI = Uri.withAppendedPath(AUTHORITY_URI, "internal");

            Log.d(TAG, "AUTHORITY_URI = " + AUTHORITY_URI);
        }
    }


    public static boolean isAvaliable(Context context) {
        initialBadgeAuthority();
        Cursor cursor = context.getContentResolver().query(BADGE_URI, BADGE_COLUMNS, null, null, null);
        if (cursor == null) {
            Log.d(TAG,"droi unread loader not avaliable");
            return false;
        }
        cursor.close();
        Log.d(TAG,"droi unread loader is avaliable");
        return true;
    }

    public static String getDroiUnreadAuthority() {
        String auth = null;
        try {
            Class clsBN = Class.forName("com.amz.ios.launcher.BadgeNotification");
            Field field = clsBN.getDeclaredField("AUTHORITY");

            return String.valueOf(field.get(clsBN));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return auth;
    }
}

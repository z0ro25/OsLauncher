package com.amz.ios.launcher.config;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.amz.ios.ioslite.common.provider.AppUsagesProvider;
import com.amz.ios.ioslite.common.util.CommonUtilities;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherModel;
import com.amz.ios.launcher.LauncherSettings;
import com.amz.ios.launcher.R;

import java.io.IOException;
import java.util.HashMap;

public class GestureEventModel {
    private static final String TAG = "GestureEventModel";

    public static final String REQUEST_GESTURE_DES = "gesture_des";
    public static final String GESTURE_ACTION_DES = "gesture_action_des";
    public static final String GESTURE_ACTION_URI = "gesture_action_uri";

    public static final String GESTURE_ACTION_URI_NO = "gesture_action_uri_no";
    public static final String GESTURE_ACTION_URI_ALARM = "gesture_action_uri_alarm";
    public static final String GESTURE_ACTION_URI_HIDDENFOLDER = "gesture_action_uri_hidden_folder";
    public static final String GESTURE_ACTION_URI_SEARCH = "gesture_action_uri_search";

    public static final int DIRECTION_UP = 0;
    public static final int DIRECTION_DOWN = 1;
    public static final int DIRECTION_LEFT = 2;
    public static final int DIRECTION_RIGHT = 3;
    public static final int DOUBLE_CLICK = 4;

    public static final int GESTURE_SWIPE_UP = 1;
    public static final int GESTURE_SWIPE_DOWN = 2;
    public static final int GESTURE_SWIPE_OBLIQUELY = 3;
    public static final int GESTURE_DOUBLE_TAP = 4;

    final Handler mWorkerHandler;
    private boolean mHasLoaderCompletedOnce;
    private boolean mLoading;
    private static Context sContext;
    private static GestureEventModel mGestureEventModel;

    static final HashMap<Integer, GestureInfo> sGestureEventMap = new HashMap<>();


    private GestureEventModel(Context context) {
        sContext = context;
        mWorkerHandler = new Handler(LauncherModel.getWorkerLooper());
    }

    public static GestureEventModel getInstance(Context context) {
        if (mGestureEventModel == null) {
            mGestureEventModel = new GestureEventModel(context.getApplicationContext());
            mGestureEventModel.loadGestureEvent();
        }

        return mGestureEventModel;
    }

    public static void reset() {
        mGestureEventModel = null;
    }

    public static void triggerGestureEvent(Launcher launcher, int event, int direction) {
        if (mGestureEventModel == null) {
            getInstance(launcher);
            return;
        }
        if (event == GestureEventModel.GESTURE_DOUBLE_TAP) {
            /*
            if (Settings.isSleepModeEnabled(launcher)) {
                PowerManager pm = (PowerManager) sContext.getSystemService(Context.POWER_SERVICE);
                try {
                    pm.goToSleep(SystemClock.uptimeMillis());
                }catch (SecurityException e){
                    Log.d("TAG", "was unable to Call PowerManager.goToSleep");
                }
            }

             */
        }
        else
            triggerGestureAction(launcher, sGestureEventMap.get(event), direction);
    }

    private static void triggerGestureAction(Launcher launcher, GestureInfo info, int direction) {
        if (info != null) {
            launcher.handleGestureInfo(info, direction);
        }
    }

    public static void updateGestureEvent(int event, String uri, String desc) {
        if (mGestureEventModel != null) {
            mGestureEventModel.modifyGestureEventInDb(event, uri, desc);
        }
    }

    public static String getGestureActionDes(Context context, int event) {
        String desc = null;
        if (sGestureEventMap.get(event) != null) {
            desc = sGestureEventMap.get(event).getActionDesc();
        }
        return TextUtils.isEmpty(desc) ? context.getString(R.string.setting_gesture_none) : desc;
    }

    public static void setDefaultGestureInfos(Context context) {
        AlarmShowItemInfo alarmInfo = new AlarmShowItemInfo(context);
        updateGestureEvent(GESTURE_SWIPE_DOWN, alarmInfo.getURI(), alarmInfo.getDescription());

        SearchShowItemInfo searchInfo = new SearchShowItemInfo(context);
        updateGestureEvent(GESTURE_SWIPE_UP, searchInfo.getURI(), searchInfo.getDescription());
    }

    private void modifyGestureEventInDb(final int event, final String uri, final String desc) {
        if (!mHasLoaderCompletedOnce) {
            return;
        }

        Runnable request = new Runnable() {
            @Override
            public void run() {
                GestureInfo gestureInfo = sGestureEventMap.get(event);
                if (gestureInfo == null) {
                    gestureInfo = new GestureInfo(event, uri, desc);
                    sGestureEventMap.put(event, gestureInfo);
                    updateGestureInfoInDb(gestureInfo, true);
                } else {
                    gestureInfo.setActionUri(uri);
                    gestureInfo.setActionDesc(desc);
                    updateGestureInfoInDb(gestureInfo, false);
                }

            }
        };
        mWorkerHandler.post(request);
    }


    private void updateGestureInfoInDb(GestureInfo gestureInfo, boolean newGesture) {
        if (newGesture) {
            addItemInDatabase(sContext, gestureInfo);
        } else {
            updateItemInDatabase(sContext, gestureInfo);
        }
    }

    private static void addItemInDatabase(Context context, GestureInfo item) {
        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();

        long id = generateNewItemId();
        item.setItemId(id);

        item.onAddToDatabase(values);
        cr.insert(GestureSettings.Gesture.CONTENT_URI, values);
    }

    private static void updateItemInDatabase(Context context, GestureInfo item) {
        final ContentValues values = new ContentValues();
        item.onAddToDatabase(values);

        final long itemId = item.id;
        final Uri uri = GestureSettings.Gesture.getContentUri(itemId);
        final ContentResolver cr = context.getContentResolver();
        cr.update(uri, values, null, null);
    }

    private static long generateNewItemId() {
        long maxItemId = 0;
        for (GestureInfo info : sGestureEventMap.values()) {
            if (maxItemId < info.id) {
                maxItemId = info.id;
            }
        }
        return maxItemId + 1;
    }


    private void loadGestureEvent() {
        if (mHasLoaderCompletedOnce || mLoading) {
            return;
        }
        mLoading = true;
        Runnable load = new Runnable() {
            @Override
            public void run() {
                loadUsagesFromDb();
                mLoading = false;
                mHasLoaderCompletedOnce = true;

                if (sGestureEventMap.size() <= 0) {
                    setDefaultGestureInfos(sContext);
                }
            }
        };

        mWorkerHandler.post(load);
    }

    private void loadUsagesFromDb() {
        sGestureEventMap.clear();
        final Context context = sContext;
        final ContentResolver contentResolver = context.getContentResolver();
        final Uri contentUri = GestureSettings.Gesture.CONTENT_URI;
        final Cursor cursor = contentResolver.query(contentUri, null, null, null, null);
        try {
            final int idIndex = cursor.getColumnIndexOrThrow(GestureSettings.Gesture._ID);
            final int eventIndex = cursor.getColumnIndexOrThrow(GestureSettings.Gesture.GESTURE_EVENT);
            final int actionIntentIndex = cursor.getColumnIndexOrThrow(GestureSettings.Gesture.ACTION_INTENT);
            final int actionDescIndex = cursor.getColumnIndexOrThrow(GestureSettings.Gesture.ACTION_DES);

            int gestureEvent;
            String actionUri;
            String actionDes;
            GestureInfo gestureInfo;
            long id;
            while (cursor.moveToNext()) {
                gestureEvent = cursor.getInt(eventIndex);
                actionUri = cursor.getString(actionIntentIndex);
                actionDes = cursor.getString(actionDescIndex);
                id = cursor.getLong(idIndex);

                gestureInfo = new GestureInfo(gestureEvent, actionUri, actionDes);
                gestureInfo.setItemId(id);

                if (actionUri.contains(GESTURE_ACTION_URI) == false) {
                    PackageManager pm = context.getPackageManager();
                    ResolveInfo matched = pm.resolveActivity(gestureInfo.getActionIntent(), PackageManager.MATCH_DEFAULT_ONLY);
                    if (matched != null) {
                        gestureInfo.setActionDesc(matched.loadLabel(pm).toString());
                    }
                }

                sGestureEventMap.put(gestureEvent, gestureInfo);
            }
        } catch (Exception e) {
            Log.e(TAG, "loading interrupted", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


    public static class GestureInfo {

        public static final int NO_ID = -1;

        //The id in the settings database for this item
        public long id = NO_ID;

        private int mGestureEvent;
        private String mActionUri;
        private String mActionDesc;
        private Intent mActionIntent;


        public GestureInfo(int event, String uri, String desc) {
            mGestureEvent = event;
            mActionDesc = desc;
            mActionUri = uri;
            mActionIntent = CommonUtilities.parseUri(mActionUri);
        }

        public int getGestureEvent() {
            return mGestureEvent;
        }

        public Intent getActionIntent() {
            return mActionIntent;
        }

        public String getActionDesc() {
            return mActionDesc;
        }

        public String getActionUri() { return mActionUri; }

        void setItemId(long id) {
            this.id = id;
        }

        void setActionUri(String uri) {
            this.mActionUri = uri;
            this.mActionIntent = CommonUtilities.parseUri(mActionUri);
        }

        void setActionDesc(String desc) {
            this.mActionDesc = desc;
        }

        void onAddToDatabase(ContentValues values) {
            values.put(GestureSettings.Gesture._ID, id);
            values.put(GestureSettings.Gesture.GESTURE_EVENT, mGestureEvent);
            values.put(GestureSettings.Gesture.ACTION_INTENT, mActionUri);
            values.put(GestureSettings.Gesture.ACTION_DES, mActionDesc);
        }
    }

    public static class BlankItemInfo {
        protected Context mContext;

        public BlankItemInfo(Context context) {
            mContext = context;
        }

        public String getDescription() {
            return mContext.getString(R.string.setting_gesture_none);
        }

        public Drawable getDrawable() {
            return null;//mContext.getResources().getDrawable(R.drawable.ic_gesture_none);
        }

        public String getURI() {
            return GestureEventModel.GESTURE_ACTION_URI_NO;
        }
    }

    public static class AlarmShowItemInfo extends BlankItemInfo {
        public AlarmShowItemInfo(Context context) {
            super(context);
        }

        public String getDescription() {
            return mContext.getString(R.string.setting_gesture_shortcut_show_alarm);
        }

        public Drawable getDrawable() {
            return null;
        }

        public String getURI() {
            return GestureEventModel.GESTURE_ACTION_URI_ALARM;
        }
    }

    public static class HiddenFolderShowItemInfo extends BlankItemInfo {
        public HiddenFolderShowItemInfo(Context context) {
            super(context);
        }

        public String getDescription() {
            return mContext.getString(R.string.setting_gesture_shortcut_show_hidden_folder);
        }

        public Drawable getDrawable() {
            return null;
        }

        public String getURI() {
            return GestureEventModel.GESTURE_ACTION_URI_HIDDENFOLDER;
        }
    }

    public static class SearchShowItemInfo extends BlankItemInfo {
        public SearchShowItemInfo(Context context) {
            super(context);
        }

        public String getDescription() {
            return mContext.getString(R.string.setting_gesture_shortcut_show_search);
        }

        public Drawable getDrawable() {
            return null;
        }

        public String getURI() {
            return GestureEventModel.GESTURE_ACTION_URI_SEARCH;
        }
    }
}

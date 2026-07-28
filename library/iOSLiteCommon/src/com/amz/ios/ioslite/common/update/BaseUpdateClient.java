package com.amz.ios.ioslite.common.update;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.amz.ios.ioslite.common.util.BuildUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


/**
 * IOS version check and update;
 */
public abstract class BaseUpdateClient {
    private static final String TAG = "BaseUpdateClient";
    private static final String SHARED_PREFS_FILE = "ios_version_updates";

    private static final String KEY_LAST_CHECK_TIME = "last_check_update_time";
    private static final String KEY_LAST_CHECK_VERSION_NAME = "last_check_version_name";

    protected Context mContext;
    protected String mLocaleVersionName;
    protected List<Callback> mCallbacks;
    protected boolean mIsChecking;

    protected static final long MINUTE_OF_MILLISECONDS = 60 * 1000;
    protected static final long HOUR_OF_MILLISECONDS = 60 * MINUTE_OF_MILLISECONDS;
    protected static final long DAY_OF_MILLISECONDS = 24 * HOUR_OF_MILLISECONDS;

    /**
     * Callback after check task complete;
     */
    public interface Callback {

        /**
         * @param result if new version checked;
         *               the change.
         */

        void onNewVersionChecked(boolean result);
    }

    public static void init(Application application) {

    }

    public BaseUpdateClient(Context context) {
        mContext = context;
        mLocaleVersionName = BuildUtil.getIOSVersionName(context);
        mCallbacks = new ArrayList<>();
    }


    /**
     * Register a Callback .The callback will be called after check on server;
     */
    public void registerCallback(Callback callback) {
        mCallbacks.add(callback);
    }


    /**
     * Unregister a previously registered Callback.
     */
    public void unregisterCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    /**
     * Check lastest verion on server;
     *
     * @param force if check the time period  before check work start;
     *              false: check time period {@link #isTimeToCheckUpdate} ;
     *              true : do check ignore time period;
     */
    public void checkUpdate(boolean force) {
        if (!mIsChecking) {
            mIsChecking = true;

            if (checkUpdateFromRecord()) {
                notifyCallback(true /*newVersionChecked*/);
                return;
            }

            if (force || isTimeToCheckUpdate()) {
                doCheckTask();
            }
            mIsChecking = false;
        }
    }


    /**
     * Do actual update work : Download app or open a  url;
     * subclass need to implemnt this;
     */
    public abstract void updateApp();

    /**
     * Do actual check work : request the version on server, compare with local;
     * subclass need to implemnt this;
     */
    protected abstract void doCheckTask();

    /**
     * Return the timeinterval for processCheckUpdate() ;
     */
    protected abstract long getCheckIntervalMillis();


    private void notifyCallback(boolean newVersionChecked) {
        mIsChecking = false;
        for (Callback callback : mCallbacks) {
            callback.onNewVersionChecked(newVersionChecked);
        }
    }

    /**
     * Called after check task complete;
     *
     * @param newVersionChecked if new version is checked;
     * @param serverVersionName the versionName of newversion; this will be saved local;
     *                          In next check task, this will be compared first, then check with server;
     */
    protected void onNewVersionChecked(boolean newVersionChecked, String serverVersionName) {
        notifyCallback(newVersionChecked);
        recordLastCheckTime();
        recordLastCheckVersionName(serverVersionName);
    }


    protected boolean isTimeToCheckUpdate() {
        SharedPreferences sp = mContext.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        final long lastCheckTimeMills = sp.getLong(KEY_LAST_CHECK_TIME, 0);
        return Calendar.getInstance().getTimeInMillis() - lastCheckTimeMills > getCheckIntervalMillis();
    }

    protected void recordLastCheckTime() {
        SharedPreferences sp = mContext.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(KEY_LAST_CHECK_TIME, Calendar.getInstance().getTimeInMillis());
        editor.commit();
    }

    protected void recordLastCheckVersionName(String versionName) {
        SharedPreferences sp = mContext.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_LAST_CHECK_VERSION_NAME, versionName);
        editor.commit();
    }


    protected boolean checkUpdateFromRecord() {
        SharedPreferences sp = mContext.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        String recordVersionName = sp.getString(KEY_LAST_CHECK_VERSION_NAME, "0");
        return compareVersionName(recordVersionName, mLocaleVersionName);
    }

    /**
     * return true if new version checked on server; othersize return false;
     */
    protected boolean compareVersionName(String serverVersion, String localVersion) {
        try {
            String[] server = serverVersion.split("\\.");
            String[] local = localVersion.split("\\.");
            for (int i = 0; i < server.length; i++) {
                int serverUnit = Integer.valueOf(server[i]);
                int localUnit = Integer.valueOf(local[i]);
                if (serverUnit > localUnit) {
                    return true;
                }

                if (serverUnit < localUnit) {
                    return false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "compareVersionName fail,  " + serverVersion + " : " + localVersion, e);
            return false;
        }

        return false;
    }
}

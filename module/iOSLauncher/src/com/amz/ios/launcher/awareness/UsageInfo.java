package com.amz.ios.launcher.awareness;


import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;

import com.amz.ios.ioslite.common.provider.AppUsagesProvider;

import java.util.Calendar;

public class UsageInfo {

    public static final int NO_ID = -1;

    /**
     * The id in the settings database for this item
     */
    public long id = NO_ID;

    private String mComponentName;
    private long mLaunchTimes;
    private long mLaunchLastestTime;


    public UsageInfo(ComponentName componentName) {
        mComponentName = componentName.flattenToShortString();
    }

    /**
     * Called when the app of mComponentName be launched ;
     * Store the launch event in database;
     */
    public void onLaunch(Context context, boolean newAppUsage) {
        mLaunchLastestTime = Calendar.getInstance().getTimeInMillis();

        mLaunchTimes += 1;
        if (newAppUsage) {
            AppUsagesModel.addItemInDatabase(context, this);
        } else {
            AppUsagesModel.updateItemInDatabase(context, this);
        }
    }

    public long getFrequency() {
        return mLaunchTimes;
    }

    public long getLatestLaunchTime(){
        return mLaunchLastestTime;
    }


    void onAddToDatabase(ContentValues values) {
        values.put(AppUsagesProvider.Usages._ID, id);
        values.put(AppUsagesProvider.Usages.APP_COMPONENTNAME, mComponentName);
        values.put(AppUsagesProvider.Usages.APP_LAUNCH_TIMES, mLaunchTimes);
        values.put(AppUsagesProvider.Usages.APP_LAUNCH_LATEST_TIME, mLaunchLastestTime);
    }

    /**
     * Load user app launch info to integer array form string stored in database;
     */
    void setLaunchTimes(long value) {
        mLaunchTimes = value;
    }


    void setLaunchLastestTime(long time) {
        mLaunchLastestTime = time;
    }

    void setItemId(long id) {
        this.id = id;
    }

}

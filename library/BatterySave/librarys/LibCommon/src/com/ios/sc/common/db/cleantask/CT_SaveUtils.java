package com.ios.sc.common.db.cleantask;

import java.util.Calendar;
import java.util.List;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.os.Build;

import com.ios.sc.common.logs.CT_Log;

public class CT_SaveUtils {

    @TargetApi(21)
    public static String getTopPackageName(Context mContext) {
        String topPackageName = "";
        if (isAndroidSdk_api_21_plus()) {
            topPackageName = getTopPackageNameFor21(mContext);
        } else {
            ActivityManager mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            List<RunningTaskInfo> tasksInfo = mActivityManager.getRunningTasks(1);
            if (null != tasksInfo && tasksInfo.size() > 0) {
                topPackageName = tasksInfo.get(0).topActivity.getPackageName();
            }
        }
        CT_Log.logD("topPackageName="+topPackageName);
        return topPackageName;
    }

    @TargetApi(21)
    public static String getTopPackageNameFor21(Context mContext) {
        String topPackageName = "";
        android.app.usage.UsageStatsManager usm = (android.app.usage.UsageStatsManager) mContext.getSystemService("usagestats");
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        long startTime = calendar.getTimeInMillis() - 60*1000;

        //CT_Log.logD(" Range start:" + dateFormat.format(startTime) );
        //CT_Log.logD(" Range   end:" + dateFormat.format(endTime));
        android.app.usage.UsageEvents uEvents = usm.queryEvents(startTime,endTime);
        
        while (uEvents.hasNextEvent()){
            android.app.usage.UsageEvents.Event e = new android.app.usage.UsageEvents.Event();
            uEvents.getNextEvent(e);
            if (e != null){
                 //CT_Log.logE(" Event: " + dateFormat.format(e.getTimeStamp()) + "::" +  e.getPackageName());
                 topPackageName = e.getPackageName();
            }
        }
        return topPackageName;
    }
    
    /**
     * For L+ interface.
     * @return  Is this android 5.0 L version ?
     */
    @TargetApi(4)
    public static boolean isAndroidSdk_api_21_plus() {
        return Build.VERSION.SDK_INT >= 21;/*Build.VERSION_CODES.LOLLIPOP*/
    }

}

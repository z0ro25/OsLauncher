package com.amz.ios.ioslite.common.util;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

/**
 * Created by server on 16-11-7.
 */
public class ProcessUtil {
    /**
     *  get process name from process id;
     */
    public static String getProcessNameFromId(Context cxt, int pid) {
        ActivityManager am = (ActivityManager) cxt.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
            if (procInfo.pid == pid) {
                return procInfo.processName;
            }
        }
        return null;
    }
}

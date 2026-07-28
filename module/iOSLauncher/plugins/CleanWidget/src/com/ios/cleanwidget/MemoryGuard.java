package com.ios.cleanwidget;


import android.app.ActivityManager;
import android.content.Context;

import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.util.PackageUtil;
import com.amz.ios.ioslite.common.util.TimeUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MemoryGuard {
    private static final String TAG = "MemoryGuard";

    private static final long BYTE_OF_MB = 1024 * 1024;

    private static final long CLEAN_TASK_PERIOD = 3 * TimeUtil.MINUTE_OF_MILLISECONDS;
    private static long mLastCleanTime;


    /**
     * 清理內存
     *
     * @return 清理内存大小(Mb)
     */
    public static long killBackgroundApps(Context context) {
        long preMem = getAvailMemory(context);
        final long currentTime = System.currentTimeMillis();
        if (currentTime - mLastCleanTime < CLEAN_TASK_PERIOD) {
            DebugLog.d(TAG, "clean background app is too frequency");
            return 0;
        }

        mLastCleanTime = currentTime;
        List<String> list = getRunningPkgs(context);
        for (String pkgName : list) {
            DebugLog.d(TAG, "killBackgroundApps: " + pkgName);
            if (inWhiteList(context, pkgName)) {
                DebugLog.d(TAG, "whitelist pkg: " + pkgName);
                continue;
            }

            DebugLog.d(TAG, "killBackgroundApps kill: " + pkgName);
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            am.killBackgroundProcesses(pkgName);
        }

        long reduce = getAvailMemory(context) - preMem;
        DebugLog.d(TAG, "reduce memory :" + reduce);
        return reduce;
    }


    /**
     * 获取占用内存比例
     */
    public static float getUsedMemPercent(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);

        long totalMem = mi.totalMem;
        long availMem = mi.availMem;

        DebugLog.d(TAG, String.format("totalMem : %s , availMem : %s", totalMem, availMem));
        float percent = 1.0f * (totalMem - availMem) / totalMem;
        return percent;
    }


    /**
     * 获取可用內存大小 (Mb)
     */
    private static long getAvailMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long result = mi.availMem / BYTE_OF_MB;
        return result;
    }

    private static List<String> getRunningPkgs(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> infoList = am.getRunningAppProcesses();

        final List<String> runningPkgs = new ArrayList<>();
        for (ActivityManager.RunningAppProcessInfo info : infoList) {
            runningPkgs.addAll(Arrays.asList(info.pkgList));
        }

        return runningPkgs;
    }


    private static final boolean inWhiteList(Context context, String pkgName) {
        return context.getPackageName().equals(pkgName)
                || PackageUtil.isSystemApp(context, pkgName);
    }
}

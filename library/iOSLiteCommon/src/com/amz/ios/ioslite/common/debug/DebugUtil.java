package com.amz.ios.ioslite.common.debug;


import android.os.SystemClock;
import android.util.Log;

public class DebugUtil {
    //adb shell setprop log.tag.PROPERTY_NAME [VERBOSE | SUPPRESS]

    private static final String TAG_LAUNCH = "Launcher.Launch";
    private static final boolean DEBUG_LAUNCH = DebugUtil.isPropertyEnabled(TAG_LAUNCH);
    private static long sLastTime;

    private static final String TAG_THEME = "Theme.Apply";
    private static final boolean DEBUG_THEME = DebugUtil.isPropertyEnabled(TAG_THEME);

    private static final String TAG_CATEGORY = "Launcher.Category";
    private static final boolean DEBUG_CATEGORY = DebugUtil.isPropertyEnabled(TAG_CATEGORY);

    private static final String TAG_UNREAD = "Launcher.Unread";
    private static final boolean DEBUG_UNREAD = true;//DebugUtil.isPropertyEnabled(TAG_UNREAD);

    /**
     * 程序启动耗时日志
     */
    public static void debugLaunch(String tag, String msg) {
        if (DEBUG_LAUNCH) {
            long spendTime = SystemClock.uptimeMillis() - sLastTime;
            sLastTime = SystemClock.uptimeMillis();
            Log.d(TAG_LAUNCH, "Now:" + sLastTime + ", Spend:" + spendTime + "  >>  " + tag + ", " + msg);
        }
    }

    /**
     * 主题应用切换
     */
    public static void debugTheme(String tag, String msg) {
        if (DEBUG_THEME) {
            Log.d(TAG_THEME, tag + ": >> " + msg);
        }
    }

    /**
     * 应用分类
     */
    public static void debugCategory(String tag, String msg) {
        if (DEBUG_CATEGORY) {
            Log.d(TAG_CATEGORY, tag + ": >> " + msg);
        }
    }

    /**
     * 未读信息
     */
    public static void debugUnread(String tag, String msg) {
        if (DEBUG_UNREAD) {
            Log.d(TAG_UNREAD, tag + ": >> " + msg);
        }
    }

    public static boolean isPropertyEnabled(String propertyName) {
        return Log.isLoggable(propertyName, Log.VERBOSE);
    }
}

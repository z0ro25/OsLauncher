package com.amz.ios.ioslite.common.debug;

import android.util.Log;

import com.amz.ios.ioslite.common.util.BuildUtil;

public class DebugLog {
    private static final String TAG = "DebugLog";
    private static final boolean ENABLE_DEBUG_LOG = DebugUtil.isPropertyEnabled(TAG);

    private static boolean enableLog() {
        return BuildUtil.DEBUG || ENABLE_DEBUG_LOG;
    }

    public static void v(String tag, String msg) {
        if (enableLog()) {
            Log.v(tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (enableLog()) {
            Log.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (enableLog()) {
            Log.i(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (enableLog()) {
            Log.w(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (enableLog()) {
            Log.e(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (enableLog()) {
            Log.e(tag, msg, tr);
        }
    }

}

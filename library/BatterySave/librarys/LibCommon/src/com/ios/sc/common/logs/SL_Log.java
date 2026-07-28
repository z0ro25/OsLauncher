package com.ios.sc.common.logs;

public class SL_Log extends C_I_Log {

    protected static String TAG = "SoftLock";

    public static void logV(String msg) {
        logv(TAG, msg);
    }

    public static void logI(String msg) {
        logi(TAG, msg);
    }

    public static void logD(String msg) {
        logd(TAG, msg);
    }

    public static void logW(String msg) {
        logw(TAG, msg);
    }

    public static void logE(String msg) {
        loge(TAG, msg);
    }

    public static void logII(String msg) {
        logii(TAG, msg);
    }
}

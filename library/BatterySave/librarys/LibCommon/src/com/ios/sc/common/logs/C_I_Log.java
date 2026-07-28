package com.ios.sc.common.logs;

import android.util.Log;


class C_I_Log {

    static void logv(String TAG, String msg) {
            Log.v(TAG, msg);
    }

    static void logd(String TAG, String msg) {
            Log.d(TAG, msg);
    }

    static void logw(String TAG, String msg) {
            Log.w(TAG, msg);
    }

    static void logi(String TAG, String msg) {
            Log.i(TAG, msg);
    }

    static void logvv(String TAG, String msg) {
        Log.v(TAG, msg);
    }

    static void logii(String TAG, String msg) {
        Log.i(TAG, msg);
    }

    static void logdd(String TAG, String msg) {
        Log.d(TAG, msg);
    }

    static void logww(String TAG, String msg) {
        Log.w(TAG, msg);
    }

    static void loge(String TAG, String msg) {
        Log.e(TAG, msg);
    }

}

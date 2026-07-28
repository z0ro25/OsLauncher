package com.zhuoyi.security.batterysave.util;

import android.util.Log;

/**
 * Created by zengrui on 2016/9/1.
 */
public class BS_LOG {
    private static final String TAG = "BS_LOG";

    public static void logV(String msg) {
        Log.v(TAG,msg);
    }

    public static void logI(String msg){
        Log.i(TAG,msg);
    }

    public static void logD(String msg) {
        Log.d(TAG,msg);
    }

    public static void logE(String msg) {
        Log.e(TAG,msg);
    }
}

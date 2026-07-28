package com.amz.ios.ioslite.common;


import android.app.Application;
import android.content.Context;

import com.amz.ios.ioslite.common.http.VolleyUtil;

/**
 * Common 模块维护全局变量；
 * <p/>
 * 1. 持有应用context；
 * 2. 初始化Library；
 */
public class CommonSdk {
    //  应用Context
    private static Context sAppContext;

    public static void initalize(Application context) {
        setApplicationContext(context);
        VolleyUtil.initalize(context);
    }

    /**
     * 持有应用Context；
     */
    private static void setApplicationContext(Context context) {
        sAppContext = context.getApplicationContext();
    }

    /**
     * 获取应用Context；
     */
    public static Context getApplicationContext() {
        return sAppContext;
    }
}

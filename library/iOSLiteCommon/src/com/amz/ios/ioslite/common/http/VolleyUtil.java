package com.amz.ios.ioslite.common.http;

import android.app.Application;
import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Volley 工具类；
 * 维护全局请求队列；
 */
public class VolleyUtil {
    private static RequestQueue sRequestQue;
    private static Context sContext;

    public static void initalize(Application context) {
        sContext = context;
    }

    public static RequestQueue getRequestQue() {
        if (sRequestQue == null) {
            sRequestQue = Volley.newRequestQueue(sContext);
        }
        return sRequestQue;
    }
}

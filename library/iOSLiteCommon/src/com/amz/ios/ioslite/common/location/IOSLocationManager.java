package com.amz.ios.ioslite.common.location;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.amz.ios.ioslite.common.util.BuildUtil;

public abstract class IOSLocationManager {
    private static final String TAG = "IOSLocManager";
    private static IOSLocationManager sInstance;
    private static Object sInstanceLock = new Object();

    public static void initalize(Application context) {
        getInstance().init(context);
    }

    public static void onTerminate() {
        getInstance().destory();
    }

    public static IOSLocationManager getInstance() {
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                Class clazz;
                try {
                    if (BuildUtil.isCNBuild()) {
                        clazz = Class.forName("com.ios.dulocation.IOSDuLocationManager");
                        sInstance = (IOSLocationManager) clazz.newInstance();
                    } else {
                        clazz = Class.forName("com.ios.iosnativelocation.IOSNativeLocationManager");
                        sInstance = (IOSLocationManager) clazz.newInstance();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "===========get location manager insta fail ", e);
                }

                if (sInstance == null) {
                    sInstance = new DefaultLocationManager();
                }
            }
            return sInstance;
        }
    }

    public abstract void init(Context context);

    public abstract void destory();
}

package com.amz.ios.ioslite.common.analytics;


import android.app.Activity;
import android.app.Application;
import android.content.Context;

/**
 * DroiBass
 */
public class DroiAnalytics extends AbsAnalytics {
    @Override
    protected void initalize(Application application) {
//        com.droi.sdk.analytics.DroiAnalytics.initialize(application);
//        com.droi.sdk.analytics.DroiAnalytics.enableActivityLifecycleCallbacks(application);
    }
    
    @Override
    protected void onCreate(Activity activity) {

    }

    @Override
    protected void onResume(Activity activity) {

    }

    @Override
    protected void onPause(Activity activity) {

    }

    @Override
    protected void onEvent(Context context, String event, String type) {

    }
}

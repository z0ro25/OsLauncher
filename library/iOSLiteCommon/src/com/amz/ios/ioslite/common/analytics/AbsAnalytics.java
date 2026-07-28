package com.amz.ios.ioslite.common.analytics;

import android.app.Activity;
import android.app.Application;
import android.content.Context;


public abstract class AbsAnalytics {
    protected abstract void initalize(Application application);

    protected abstract void onCreate(Activity activity);

    protected abstract void onResume(Activity activity);

    protected abstract void onPause(Activity activity);

    protected abstract void onEvent(Context context, String event, String type);

}
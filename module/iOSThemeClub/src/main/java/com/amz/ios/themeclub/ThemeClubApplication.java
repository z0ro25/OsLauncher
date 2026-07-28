package com.amz.ios.themeclub;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.amz.ios.ioslite.common.ContextHelper;

public class ThemeClubApplication {
    private static Context sContext;
    private static PackageBroadcastReceiver sPackageBroadcastReceiver;
    private static final String PACKAGE_BROADCAST_DATA_SCHEME = "package";

    public static void initalize(Application context) {
        sContext = context;

        sPackageBroadcastReceiver = new PackageBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme(PACKAGE_BROADCAST_DATA_SCHEME);
        ContextHelper.registerReceiver(context, sPackageBroadcastReceiver, filter);
    }

    public static void release(Application context) {
        context.unregisterReceiver(sPackageBroadcastReceiver);
    }

    public static Context getContext() {
        return sContext;
    }
}

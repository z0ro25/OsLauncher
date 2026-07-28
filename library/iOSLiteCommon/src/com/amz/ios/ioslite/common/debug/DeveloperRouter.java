package com.amz.ios.ioslite.common.debug;

import android.content.Context;
import android.content.Intent;

import com.amz.ios.ioslite.common.util.CommonUtilities;


public class DeveloperRouter {

    public static final String ADVERTISE_DEBUG_ACTIVITY = "com.ios.admob.AdDeveloperActivity";

    public static final String ACTION_ADVERTISE_SOURCE = "ioslite.intent.action.ADVERTISE_SOURCE";


    public static void startAdControllActivity(Context context) {
        Intent intent = new Intent();
        intent.setClassName(context, ADVERTISE_DEBUG_ACTIVITY);
        CommonUtilities.startActivitySafely(context, intent);
    }

    public static void startAdSourceActivity(Context context) {
        Intent intent = new Intent(ACTION_ADVERTISE_SOURCE);
        intent.setPackage(context.getPackageName());
        CommonUtilities.startActivitySafely(context, intent);
    }

}

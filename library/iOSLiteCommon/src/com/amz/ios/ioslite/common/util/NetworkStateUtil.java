package com.amz.ios.ioslite.common.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkStateUtil {
    public static final int STATE_NONE = -1;
    public static final int STATE_WIFI = 0;
    public static final int STATE_MOBILE = 1;

    public static int getNetworkState(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return STATE_WIFI;
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                return STATE_MOBILE;
            }
        }
        return STATE_NONE;
    }


    public static boolean isNetworkConnected(Context context) {
        return getNetworkState(context) != STATE_NONE;
    }

    public static boolean isWifiConnected(Context context) {
        return getNetworkState(context) == STATE_WIFI;
    }

    public static boolean isMobileConnected(Context context) {
        return getNetworkState(context) == STATE_MOBILE;
    }
}

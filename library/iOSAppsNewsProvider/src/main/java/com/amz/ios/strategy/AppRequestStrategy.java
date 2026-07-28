package com.amz.ios.strategy;

import android.content.Context;

import com.amz.ios.ioslite.common.util.PreferencesUtil;

/**
 * Created by liaozhongjun on 2017/1/17.
 */

public class AppRequestStrategy {
    public int from;
    public int to;//count

    private static final String KEY_APP_REQUEST_FROM = "app_request_from";
    private static final String KEY_APP_REQUEST_RESET = "app_request_reset";
    private static final int SHOW_COUNT = 8;
    int num;

    public static AppRequestStrategy newAppRequestStrategy(Context context) {
        return new AppRequestStrategy(context, SHOW_COUNT);
    }

    public static AppRequestStrategy newAppRequestStrategy(Context context, int count) {
        return new AppRequestStrategy(context, count);
    }

    private AppRequestStrategy(Context context, int count) {
        from = PreferencesUtil.getInt(context, KEY_APP_REQUEST_FROM, 1);
        to = count;
        num = count;
    }


    public void updateStrategy(Context context, int count) {
        if (from + count <= num) {
            return;
        }
        boolean reset = PreferencesUtil.getBoolean(context, KEY_APP_REQUEST_RESET, true);
        if (count < num) {
            from = from + count - num;
            PreferencesUtil.putBoolean(context, KEY_APP_REQUEST_RESET, true);
        } else {
            if (reset) {
                from = 1;
                PreferencesUtil.putBoolean(context, KEY_APP_REQUEST_RESET, false);
            } else {
                from = from + count;
            }
        }
        PreferencesUtil.putInt(context, KEY_APP_REQUEST_FROM, from);
    }


}

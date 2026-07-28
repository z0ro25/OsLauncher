package com.amz.ios.ioslite.common.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by server on 16-10-24.
 */
public class Sputil {
    public static void saveTorchState(Context context, boolean isLightOpen, String key,String fileName) {
        SharedPreferences perf = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = perf.edit();
        editor.putBoolean(key, isLightOpen);
        editor.commit();
    }

    public static boolean getTorchState(Context context, String key,String fileName) {
        SharedPreferences perf = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        boolean torchState = perf.getBoolean(key, false);
        return torchState;
    }
}

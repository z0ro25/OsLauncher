package com.ios.cleanwidget;

import android.content.Context;


public class CleanMaster {
    private static final String TAG = "CleanMaster";
    private static final float FAKE_USED_MEMORY = 0.70f;

    public static long processClean(Context context) {
        long result = MemoryGuard.killBackgroundApps(context);
        return result;
    }

    public static float getUsedMemoryPercent(Context context) {
        float usedMemoryPercent;
        try {
            usedMemoryPercent = MemoryGuard.getUsedMemPercent(context);
        } catch (Exception e) {
            usedMemoryPercent = FAKE_USED_MEMORY;
        }

        if (usedMemoryPercent > 1.0 || usedMemoryPercent < 0) {
            usedMemoryPercent = FAKE_USED_MEMORY;
        }
        return usedMemoryPercent;
    }

}

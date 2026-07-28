package com.amz.ios.launcher.util;

import android.os.Build;

public class VersionManager {
    public static final int FROYO_SDK_START = 8;
    public static final int GINGERBREAD_SDK_START = 9;
    public static final int ICS_SDK_START = 14;
    public static final int JB_SDK_MR1 = 17;
    public static final int JB_SDK_MR2 = 18;
    public static final int JB_SDK_START = 16;
    public static final int KITKAT_R1 = 19;
    private static final int SUPPORT_SDK_END = 100;
    private static final int SUPPORT_SDK_START = 8;

    public static int getCurrentSdkVersion() {
        return Build.VERSION.SDK_INT;
    }

    public static final boolean isKitkatOrLater() {
        return Build.VERSION.SDK_INT >= KITKAT_R1;
    }

    public static boolean isLaterThanHoneycombMR2() {
        return getCurrentSdkVersion() >= ICS_SDK_START;
    }
}

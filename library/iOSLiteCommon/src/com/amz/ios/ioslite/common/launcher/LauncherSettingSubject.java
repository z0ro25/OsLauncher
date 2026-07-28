package com.amz.ios.ioslite.common.launcher;

import java.lang.ref.WeakReference;

public class LauncherSettingSubject {
    private static WeakReference<LauncherSettingCallback> sCallbackWeakReference = null;

    public static void register(LauncherSettingCallback callback) {
        if (callback != null) {
            sCallbackWeakReference = new WeakReference<LauncherSettingCallback>(callback);
        }
    }

    public static void unRegister() {
        sCallbackWeakReference.clear();
    }

    public static void handleLauncherSettingChanged(String key) {
        if (sCallbackWeakReference != null) {
            LauncherSettingCallback callback = sCallbackWeakReference.get();
            if (callback != null){
                callback.onLauncherSettingChanged(key);
            }
        }
    }
}

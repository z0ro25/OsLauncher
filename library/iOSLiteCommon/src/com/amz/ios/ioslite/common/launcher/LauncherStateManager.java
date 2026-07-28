package com.amz.ios.ioslite.common.launcher;

import java.util.ArrayList;
import java.util.List;

/**
 *   Launcher notify state change;
 */
public class LauncherStateManager{

    // Hold all registed callbacks;
    private static final List<LauncherStateCallback> mCallbacks = new ArrayList<>();

    /**
     * Register a LauncherStateCallback .The callback will be called by launcher;
     */
    public static void registerCallback(LauncherStateCallback callback){
        mCallbacks.add(callback);
    }

    /**
     * Unregister a previously registered LauncherStateCallback.
     */
    public static void unregisterCallback(LauncherStateCallback callback){
        mCallbacks.remove(callback);
    }

    /**
     *   Called by Launcher, notify listeners that launcher workspace scroll pages,
     */
    public static void notifyPageSwitch(){
        for(LauncherStateCallback callback : mCallbacks){
            callback.onPageSwitch();
        }
    }

    /**
     *   Called by Launcher, notify listeners that left custom page of workspace hide;
     */
    public static void notifyLeftCustomContentHide(){
        for(LauncherStateCallback callback : mCallbacks){
            callback.onLeftCustomContentHide();
        }
    }

    /**
     *   Called by Launcher, notify listeners that left custom page of workspace show;
     */
    public static void notifyLeftCustomContentShow(){
        for(LauncherStateCallback callback : mCallbacks){
            callback.onLeftCustomContentShow();
        }
    }
}

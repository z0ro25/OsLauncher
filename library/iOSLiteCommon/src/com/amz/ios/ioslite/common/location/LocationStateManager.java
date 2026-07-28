package com.amz.ios.ioslite.common.location;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by server on 17-3-22.
 */
public class LocationStateManager {
    private static List<OnLocationStateListener> sListeners = new ArrayList<>();

    public static void registerListener(OnLocationStateListener onLocationStateListener){
        sListeners.add(onLocationStateListener);
    }

    public static void unRegisterListener(OnLocationStateListener onLocationStateListener){
        sListeners.remove(onLocationStateListener);
    }

    public static void startPositioning(Context context,String provider, boolean isLauncher){
        for (OnLocationStateListener listener : sListeners) {
            listener.startPositioning(context,provider,isLauncher);
        }
    }

    public static void stopPositioning(){
        for (OnLocationStateListener listener : sListeners) {
            listener.stopPositioning();
        }
    }
}

package com.amz.ios.ioslite.common.location;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by server on 17-3-22.
 */
public class GpsResultManager {
    private static List<OnGpsResulListener> mListeners = new ArrayList<>();

    public static void registerListener(OnGpsResulListener onGpsResulListener){
        mListeners.add(onGpsResulListener);
    }

    public static void unRegisterListener(OnGpsResulListener onGpsResulListener){
        mListeners.remove(onGpsResulListener);
    }

    //handle the situation that gps fail
    public static void handleGpsFailSituation(){
        for (OnGpsResulListener listener : mListeners) {
            listener.onGpsFail();
        }
    }

    //handle the situation that gps successful
    public static void handleGpsSuccessfulSituation(){
        for (OnGpsResulListener listener : mListeners) {
            listener.onGpsSuccessful();
        }
    }
}

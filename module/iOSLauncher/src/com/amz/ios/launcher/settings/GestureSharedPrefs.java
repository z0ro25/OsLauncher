//package com.amz.ios.launcher.settings;
//
//import android.content.Context;
//import android.content.SharedPreferences;
//
//public class GestureSharedPrefs {
//    public static final String GESTURE_DOUBLE_CLICK_KEY = "gesture_doubleclick";
//    public static final String GESTURE_DOWN_PREFS_KEY = "gesture_down";
//e_
//    public static final int DIRECTION_UP = 0;
//    public static final int DIRECTION_DOWN = 1;
//    public static final int DIRECTION_LEFT = 2;
//    public static final int DIRECTION_RIGHT = 3;
//    public static final int DOUBLE_CLICK = 4;
//
//    public static final int GESTURE_FUNCTION_CLOSE = 0;
//    public static final int GESTURE_HIDE_APP = 1;
//    public static final int GESTURE_QUICK_ACCESS = 2;
//    public static final int GESTURE_POP_NOTIFICATION = 3;
//    public static final int GESTURE_OPEN_SEARCH = 4;
//    public static final int GESTURE_EDIT_LAUNCHER = 5;
//    public static final int GESTURE_ONE_LOCKER = 6;
//
//    public static final String GESTURE_OBLIQUELY_PREFS_KEY = "gesture_obliquely";
//    public static final String GESTURE_SHARED_PREFS = "com.ios.ioslite.gesture_prefs";
//    public static final String GESTURE_UP_PREFS_KEY = "gesture_up";
//
//    public static int getGestureFunction(Context context, int direction) {
//        SharedPreferences gesturePrefs = getGesturePrefs(context);
//        if (direction == DIRECTION_UP)
//            return gesturePrefs.getInt(GESTURE_UP_PREFS_KEY, GESTURE_OPEN_SEARCH);
//        if (direction == DIRECTION_DOWN)
//            return gesturePrefs.getInt(GESTURE_DOWN_PREFS_KEY, GESTURE_POP_NOTIFICATION);
//        if (direction == DIRECTION_LEFT || direction == DIRECTION_RIGHT)
//            return gesturePrefs.getInt(GESTURE_OBLIQUELY_PREFS_KEY, GESTURE_QUICK_ACCESS);
//
//        return gesturePrefs.getInt(GESTURE_DOUBLE_CLICK_KEY, GESTURE_ONE_LOCKER);
//    }
//
//    public static SharedPreferences getGesturePrefs(Context context) {
//        return getGesturePrefs(context, 0);
//    }
//
//    public static SharedPreferences getGesturePrefs(Context context, int i) {
//        return context.getSharedPreferences(GESTURE_SHARED_PREFS, i);
//    }
//
//    public static void updateGesturePrefs(Context context, String str, int i) {
//        SharedPreferences.Editor edit = getGesturePrefs(context).edit();
//        edit.putInt(str, i);
//        edit.commit();
//    }
//}

package com.amz.ios.themeclub.util;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.widget.Toast;

import com.amz.ios.themeclub.ThemeClubApplication;

/**
 * Created by lideqian on 16-11-29.
 */
public class CustomUtils {

    public static final int LENGTH_SHORT = 1000;
    private static Toast mToast;
    private static Handler mHandler = new Handler();
    private static Runnable r = new Runnable() {
        public void run() {
            mToast.cancel();
        }
    };

    public static void showToast(Context mContext, String text, int duration) {

        if (mContext == null) {
            return;
        }
        mHandler.removeCallbacks(r);
        if (mToast != null)
            mToast.setText(text);
        else
            mToast = Toast.makeText(mContext, text, Toast.LENGTH_SHORT);
        mHandler.postDelayed(r, duration);

        mToast.show();
    }

    public static void showToast(Context mContext, int resId, int duration) {
        if(mContext == null){
            return;
        }
        Resources resource = mContext.getResources();
        if(resource != null) {
            showToast(mContext, mContext.getResources().getString(resId), duration);
        }
    }

    public static void shortToast(String s) {
        showToast(ThemeClubApplication.getContext(),s,LENGTH_SHORT);
    }

    public static void showToast(String s,int duration) {
        showToast(ThemeClubApplication.getContext(),s,duration);
    }


}

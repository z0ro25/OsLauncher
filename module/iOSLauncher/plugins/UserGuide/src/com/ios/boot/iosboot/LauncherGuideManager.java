package com.ios.boot.iosboot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.amz.ios.launcher.LauncherAppState;

/**
 * 用户向导管理
 */
public class LauncherGuideManager {
    private static final String LAUNCHER_GUIDE_COMPLETE = "launcher.guide_complete";
    private static final String FIRST_RUN_ACTIVITY_DISPLAYED = "launcher.first_run_activity_displayed";
    private static final String SPEED_GUIDE_ACTIVITY_DISPLAYED = "launcher.speed_guide_activity_displayed";
    private static final String NEW_VERSION_INFO_ACTIVITY_DISPLAYED = "launcher.new_version_activity_displayed";
    private static final String ALL_APPS_GUIDE_DISMISSED_KEY = "launcher.all_apps_guide_dismissed";

    private SharedPreferences mSharedPrefs;

    private static LauncherGuideManager INSTANCE;

    public static LauncherGuideManager getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new LauncherGuideManager(context);
        }
        return INSTANCE;
    }

    private LauncherGuideManager(Context context) {
        mSharedPrefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(),
                Context.MODE_PRIVATE);
    }

    public boolean hasCompleteGuide() {
        return mSharedPrefs.getBoolean(LAUNCHER_GUIDE_COMPLETE, false);
    }

    public void markGuideComplete() {
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putBoolean(LAUNCHER_GUIDE_COMPLETE, true);
        editor.apply();
    }


    public boolean hasRunFirstRunActivity() {
        return mSharedPrefs.getBoolean(FIRST_RUN_ACTIVITY_DISPLAYED, true); //xiaopeng remove ios welcome true
    }

    public void markFirstRunActivityShown() {
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putBoolean(FIRST_RUN_ACTIVITY_DISPLAYED, true);
        editor.apply();
    }


    public void showFirstRunActivity(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, FirstRunActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }


    public boolean hasRunSpeedGuideActivity() {
        return mSharedPrefs.getBoolean(SPEED_GUIDE_ACTIVITY_DISPLAYED, false);
    }

    public void markSpeedGuideActivityShown() {
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putBoolean(SPEED_GUIDE_ACTIVITY_DISPLAYED, true);
        editor.apply();
    }

    public void showSpeedGuideActivity(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, SpeedUpActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    public boolean hasRunNewVersionInfoActivity() {
        return mSharedPrefs.getBoolean(NEW_VERSION_INFO_ACTIVITY_DISPLAYED, false);
    }

    public void markNewVersionInfoActivityShown() {
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putBoolean(NEW_VERSION_INFO_ACTIVITY_DISPLAYED, true);
        editor.apply();
    }

    public void showNewVersionInfoActivity(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, UpdateInfoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    public boolean hasRunAllAppsGuide() {
        return mSharedPrefs.getBoolean(ALL_APPS_GUIDE_DISMISSED_KEY, false);
    }

    public void markAllAppsGuideShown() {
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putBoolean(ALL_APPS_GUIDE_DISMISSED_KEY, true);
        editor.apply();
    }


}

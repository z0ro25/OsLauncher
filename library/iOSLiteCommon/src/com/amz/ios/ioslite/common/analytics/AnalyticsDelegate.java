package com.amz.ios.ioslite.common.analytics;

import android.app.Activity;
import android.app.Application;
import android.content.Context;


public class AnalyticsDelegate {
    // 友盟数据统计
//    private static AbsAnalytics sUmAnalytics;
    // DroiBass
//    private static AbsAnalytics sDroiAnalytics;

    private AnalyticsDelegate() {
    }

    public static void initalize(Application application) {
//        sUmAnalytics = new UmAnalytics();
//        sUmAnalytics.initalize(application);
//
//        sDroiAnalytics = new DroiAnalytics();
//        sDroiAnalytics.initalize(application);
    }

    public static void onCreate(Activity activity) {
//        if (sUmAnalytics != null) {
//            sUmAnalytics.onCreate(activity);
//        }
    }

    public static void onResume(Activity activity) {
//        if (sUmAnalytics != null) {
//            sUmAnalytics.onResume(activity);
//        }
    }

    public static void onPause(Activity activity) {
//        if (sUmAnalytics != null) {
//            sUmAnalytics.onPause(activity);
//        }
    }

    public static void onLauncherEvent(Context context, String... args) {
//        if (sUmAnalytics != null && args.length > 1) {
//            sUmAnalytics.onEvent(context, args[0], args[1]);
//        }
    }

    public static void onEvent(Context context, String event, String type) {
//        if (sUmAnalytics != null) {
//            sUmAnalytics.onEvent(context, event, type);
//        }
    }

    public static void onSearchEvent(Context context,String event){
//        onLauncherEvent(context,UMEventConstants.SEARCHBOX_EVENT,event);
    }

    public static void onCleanWidgetEvent(Context context,String event){
//        onLauncherEvent(context,UMEventConstants.IOS_CLEANWIDGET,event);
    }

    public static void onThemeEvent(Context context,String event){
//        onLauncherEvent(context,UMEventConstants.IOS_THEME,event);
    }

    public static void onWallpaperEvent(Context context,String event){
//        onLauncherEvent(context,UMEventConstants.IOS_WALLPAPER, event);
    }

    public static void onAllAppsEvent(Context context,String event){
//        onLauncherEvent(context,UMEventConstants.IOS_ALLAPPS,event);
    }

    public static void onWeatherEvent(Context context,String event){
//        onLauncherEvent(context,UMEventConstants.IOS_WEATHER_EVENT,event);
    }

    public static void onSettingsEvent(Context context,String event){
//        onLauncherEvent(context,UMEventConstants.IOS_SETTINGS_EVENT,event);
    }

    public static void onLauncherLongClickEvent(Context context,String event){
//        onLauncherEvent(context,UMEventConstants.IOS_LONGCLICK_EVENT,event);
    }

    public static void onLauncherSettingsEvent(Context context,String event){
//        onLauncherEvent(context,UMEventConstants.IOS_LAUNCHERSETTINGS_EVENT,event);
    }

    public static void onStackWidgetEvent(Context context,String event){
//        onLauncherEvent(context,UMEventConstants.STACK_WIDGET_EVENT,event);
    }

    public static void onDiscoveryEvent(Context context,String event){
//        onLauncherEvent(context,UMEventConstants.DISCOVERY_EVENT,event);
    }

    public static void onSmartSortEvent(Context context,String event){
//        onLauncherEvent(context,UMEventConstants.SMART_SORT_EVENT,event);
    }

    public static void onTwAdsEvent(Context context,String adsId,String event){
//        onLauncherEvent(context,UMEventConstants.ADS_EVNET,UMEventConstants.TW_ADS_Event,adsId,event);
    }

    public static void onDroiAdsEvent(Context context,String adsId,String event){
//        onLauncherEvent(context,UMEventConstants.ADS_EVNET,UMEventConstants.DROI_ADS_Event,adsId,event);
    }

}


package com.amz.ios.ioslite.common;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.amz.ios.ioslite.common.util.CommonUtilities;


public class Router {
    private static final String TAG = "Router";

    // 美华中心
    private static final String ACTIVITY_THEMECLUB = "com.amz.ios.themeclub.MainActivity";
    private static final String THEMECLUB_ACTION = "ios.intent.action.ThemeClubActivity";
    private static final String EXTRA_KEY_THEMECLUB = "themeclubtype";
    public static final int EXTRA_VALUE_WALLPAPER = 0;
    public static final int EXTRA_VALUE_THEME = 1;
    public static final int EXTRA_VALUE_MINE = 2;

    // 应用市场
    private static final String ACTIVITY_APP_MARKET = "com.ios.widget.newspage.module.moduleappmarket.AppMarketActivity";

    // 探索
    private static final String ACTIVITY_DISCOVERY = "com.ios.discovery.ui.activity.MainActivity";

    // 小部件，快捷方式
    public static final String WIDGET_PROVIDER_TORCH = "com.ios.iossettings.TorchWidgetProvider";
    public static final String SHORTCUT_BATTERY_SAVE = "com.amz.ios.launcher.shortcut.BatterySave";

    public static final String SHORTCUT_BATTERY_WIDGET = "com.amz.ios.launcher.shortcut.BatterySave";
    public static final String SHORTCUT_PICTURE_WIDGET = "com.amz.ios.launcher.shortcut.BatterySave";
    //快捷设置
    public static final String SHORTCUT_QUICK_SETTINGS = "com.ios.iossettings.SettingsActivity";

    //搜索
    private static final String KEY_SEARCH_KEYWORD = "keyword";
    private static final String KEY_SEARCH_TYPE = "type";
    private static final String DEFAULT_SEARCH_KEYWORD = "";
    private static final int DEFAULT_SEARCH_TYPE = 3;
    private static final String URL_SEARCH_PREFIX = "iosSearch://droi.com:200/";

    //天气
    private static final String ACTIVITY_WEATHER = "com.ios.weather.ui.WeatherDetailActivity";

    // 省电模式
    private static final String ACTIVITY_BATTERY_SAVE = "com.zhuoyi.security.batterysave.BS_MainActivity";

    // 省电模式
    private static final String LAUNCHER = "com.amz.ios.launcher.Launcher";

    /**
     * 启动美华中心
     */
    public static void startThemeClubActivity(Context context) {
        startThemeClubActivity(context, EXTRA_VALUE_THEME);
    }

    /**
     * 美华中心壁纸页面
     */
    public static void startWallpaperActivity(Context context) {
        startThemeClubActivity(context, EXTRA_VALUE_WALLPAPER);
    }

    /**
     * 美华中心壁纸页面
     */
    public static void startMineActivity(Context context) {
        startThemeClubActivity(context, EXTRA_VALUE_MINE);
    }

    /**
     * 美华中心主题页面
     */
    private static void startThemeClubActivity(Context context, int type) {
        Intent intent = new Intent();
        intent.setClassName(context, ACTIVITY_THEMECLUB);
        intent.setAction(THEMECLUB_ACTION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_KEY_THEMECLUB, type);
        CommonUtilities.startActivitySafely(context, intent);
    }


    /**
     * 打开探索 Discovery
     */
    public static void startDiscoveryActivity(Context context) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setClassName(context, ACTIVITY_DISCOVERY);
        CommonUtilities.startActivitySafely(context, intent);
    }


    /**
     * 打开应用市场
     */

    public static void startAppMarketActivity(Context context) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setComponent(new ComponentName(context.getPackageName(), ACTIVITY_APP_MARKET));
        CommonUtilities.startActivitySafely(context, intent);
    }

    /**
     * 打开搜索进入默认页
     */
    public static void startSearchActivity(Context context) {
        startSearchActivity(context, null);
    }

    public static void startSearchActivityForResult(Activity activity, int requestCod3) {
        startSearchActivityForResult(activity, null,requestCod3);
    }

    public static void startSearchActivityForResult(Activity activity, Bundle extra, int requestCode) {

        try {
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setComponent(new ComponentName("com.ios.ioslite.global", "com.amz.ios.search.SearchActivity"));
            activity.startActivityForResult(intent,requestCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开搜索跳转链接
     */
    public static void startSearchActivity(Context context, Bundle extra) {

        try {
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setComponent(new ComponentName("com.ios.ioslite.global", "com.amz.ios.search.SearchActivity"));
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        Intent intent = new Intent(LiteAction.ACTION_SEARCH_ACTIVITY);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//
//        String keyword = DEFAULT_SEARCH_KEYWORD;
//        int type = DEFAULT_SEARCH_TYPE;
//        if (extra != null) {
//            keyword = extra.getString(KEY_SEARCH_KEYWORD, DEFAULT_SEARCH_KEYWORD);
//            type = extra.getInt(KEY_SEARCH_TYPE, DEFAULT_SEARCH_TYPE);
//        }
//        StringBuilder sb = new StringBuilder();
//        sb.append(URL_SEARCH_PREFIX).append(type).append("/").append(keyword);
//        intent.setData(Uri.parse(sb.toString()));
//        CommonUtilities.startActivitySafely(context, intent);
    }

    /**
     * 打开一键加速
     */
    public static void startCleanUp(Context context) {
        Intent intent = new Intent(LiteAction.ACTION_CLEAN_WIDGET);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    /**
     * 打开天气
     */
    public static void startWeatherActivity(final Context context) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setComponent(new ComponentName(context.getPackageName(), ACTIVITY_WEATHER));
        CommonUtilities.startActivitySafely(context, intent);
    }

    /**
     * 启动省电模式
     */
    public static void startBatterySaveActivity(final Context context) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setComponent(new ComponentName(context.getPackageName(), ACTIVITY_BATTERY_SAVE));
        CommonUtilities.startActivitySafely(context, intent);
    }
}



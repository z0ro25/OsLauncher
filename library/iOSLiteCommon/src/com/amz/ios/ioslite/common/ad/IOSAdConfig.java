package com.amz.ios.ioslite.common.ad;


import android.content.Context;

import com.amz.ios.ioslite.common.R;

public class IOSAdConfig {
    // 一键加速小部件
    public static final int ID_CLEAN_WIDGET = 1;
    // AllApp上方广告
    public static final int ID_ALL_APPS = 2;
    // 主题详情
    public static final int ID_THEME_DETAIL = 3;
    // 壁纸详情
    public static final int ID_WALLPAPER_DETAIL = 4;
    // 搜索页面
    public static final int ID_SEARCH = 5;
    // 天气页面
    public static final int ID_WEATHER = 6;
    // IOSKnow
    public static final int ID_IOS_KNOW = 7;
    // 主题列表
    public static final int ID_LIST_THEME = 10;
    // 壁纸列表
    public static final int ID_LIST_WALLPAPER = 11;
    // 极简主题
    public static final int ID_LIST_MIN_THEME = 12;
    // 广告卡片
    public static final int ID_LIST_STACK_WIDGET = 13;
    // 新闻页应用推荐
    public static final int ID_APP_RECOMMEND_NEWSPAGE = 14;
    // 文件夹应用推荐
    public static final int ID_APP_RECOMMEND_FOLDER = 15;
    // 应用市场
    public static final int ID_APP_RECOMMEND_MARKET = 16;
    // Discovery
    public static final int ID_APP_RECOMMEND_DISCOVERY = 17;
    // Interstitial khi mở app
    public static final int ID_INTERSTITIAL_OPEN_APP = 18;
    // App-open khi quay lại desktop
    public static final int ID_APP_OPEN = 19;

    public static String getAdDescription(Context cotext, int id) {
        int resId;
        switch (id) {
            case ID_ALL_APPS:
                resId = R.string.ad_des_all_apps;
                break;
            case ID_THEME_DETAIL:
                resId = R.string.ad_des_theme_detail;
                break;
            case ID_WALLPAPER_DETAIL:
                resId = R.string.ad_des_wallpaper_detail;
                break;
            case ID_CLEAN_WIDGET:
                resId = R.string.ad_des_clean_widget;
                break;
            case ID_SEARCH:
                resId = R.string.ad_des_search;
                break;
            case ID_WEATHER:
                resId = R.string.ad_des_weather;
                break;
            case ID_IOS_KNOW:
                resId = R.string.ad_des_ioskonw;
                break;
            case ID_LIST_THEME:
                resId = R.string.ad_des_theme_list;
                break;
            case ID_LIST_WALLPAPER:
                resId = R.string.ad_des_wallpaper_list;
                break;
            case ID_LIST_MIN_THEME:
                resId = R.string.ad_des_theme_min;
                break;
            case ID_LIST_STACK_WIDGET:
                resId = R.string.ad_des_stackwidget_list;
                break;
            case ID_APP_RECOMMEND_NEWSPAGE:
                resId = R.string.ad_des_app_recommend_newspage;
                break;
            case ID_APP_RECOMMEND_FOLDER:
                resId = R.string.ad_des_app_recommend_folder;
                break;
            case ID_APP_RECOMMEND_MARKET:
                resId = R.string.ad_des_app_recommend_market;
                break;
            case ID_APP_RECOMMEND_DISCOVERY:
                resId = R.string.ad_des_app_recommend_discovery;
                break;
            default:
                resId = R.string.ad_des_default;
                break;
        }
        return cotext.getString(resId);
    }
}

package com.amz.ios.ioslite.common;


import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.Log;

import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.ioslite.common.util.DisplayUtil;

public class Partner {
    private static final String TAG = "Partner";
    public static final String CUSTOM_PACKAGE = "com.oslauncher.applauncher.themelauncher.custom";
    public static final String TEST_PACKAGE = "com.oslauncher.applauncher.themelauncher.test";
    private static Resources sPartnerRes;
    private static String sPartnerPkgName;
    private static Context sPartnerContext;

    // 功能开关；
    // IOSKnow 是否启用
    public static final String FEATURE_IOS_KNOW_ENABLE = "feature_ios_know_enable";
    //探索是否启用
    public static final String FEATURE_IOS_DISCOVERY_ENABLE = "feature_ios_discovery_enable";
    // App Manager 是否启用
    public static final String FEATURE_APP_MANAGER_ENABLE = "feature_app_manager_enable";
    // Local Push 是否启用
    public static final String FEATURE_LOCAL_PUSH_ENABLE = "feature_local_push_enable";
    // 桌面搜索框 是否启用
    public static final String FEATURE_SEARCH_BOX_ENABLE = "feature_search_box_enable";
    // 负一屏新闻页 是否启用
    public static final String FEATURE_LEFT_NEWS_PAGE_ENABLE = "feature_left_news_page_enable";
    // AllApp 最近打开应用
    public static final String FEATURE_ALL_APP_RECENTLY_APPS_ENABLE = "feature_allapp_recently_apps_enable";
    // 手电筒
    public static final String FEATURE_TORCH_ENABLE = "feature_torch_enable";
    //自动亮度
    public static final String FEATURE_AUTO_LIGHT_ENABLE = "feature_auto_light_enable";
    // 省电模式
    public static final String FEATURE_BATTERY_SAVE_ENABLE = "feature_battery_save_enable";
    //热点
    public static final String FEATURE_WIFI_AP_ENABLE = "feature_wifi_ap_enable";
    //快捷设置
    public static final String FEATURE_QUICK_SETTINGS_ENABLE = "feature_quick_settings_enable";
    // 美华中心 动态壁纸
    public static final String FEATURE_LIVE_WALLPAPER_ENABLE = "feature_live_wallpaper_enable";
	//美化中心是否显示锁屏
    public static final String FEATURE_LOCK_SCREEN_ENABLE = "feature_lock_screen_enable";
    // set as default theme
    public static final String FEATURE_SET_AS_DEFAULT_THEME_ENABLE = "feature_set_as_default_theme_enable";
    // 桌面未读消息角标是否显示
    public static final String FEATURE_ICON_UNREAD_SUPPORT = "feature_icon_unread_support";
    // 软件分享
    public static final String FEATURE_SOFTWARE_SHARE_SUPPORT = "feature_sw_share_support";
    // 检查更新
    public static final String FEATURE_CHECK_UPDATE_SUPPORT = "feature_check_update_support";
    // shortcut
    public static final String FEATURE_INSTALL_SHORTCUT_ENABLE = "feature_install_shortcut_enable";
    // 应用信息选项
    public static final String FEATURE_DROP_INFO_ENABLE = "feature_drop_info_enable";
    // 卸载选项
    public static final String FEATURE_DROP_UNINSTALL_ENABLE = "feature_drop_uninstall_enable";
    // unavailable
    public static final String FEATURE_DROP_UNAVAILABLE_ENABLE = "feature_drop_unavailable_enable";
    // 桌面风格切换
    public static final String FEATURE_DESKTOP_STYLE_CHANGE_ENABLE = "feature_desktop_style_change_enable";
    // 桌面布局行列切换
    public static final String FEATURE_DESKTOP_GRID_CHANGE_ENABLE = "feature_desktop_grid_change_enable";
    // 桌面手势开关
    public static final String FEATURE_DESKTOP_GESTURE_ENABLE = "feature_desktop_gesture_enable";
    //桌面打开应用动画
    public static final String FEATURE_DESKTOP_TRANS_ANIM_ENABLE = "feature_desktop_trans_anim_enable";
    // 图标整理
    public static final String FEATURE_SORT_ICON_ENABLE = "feature_sort_icon_enable";
    // new install icon
    public static final String FEATURE_NEW_INSTALL_ICON_ENABLE = "feature_new_install_icon_enable";
    // deffect setting
    public static final String FEATURE_DESKTOP_EFFECT_ENABLE = "feature_desktop_effect_enable";
    // delete effect
    public static final String FEATURE_DELETE_EFFECT_ENABLE = "feature_delete_effect_enable";
    // category feature
    public static final String FEATURE_CATEGORY_NEW_APPS_ENABLE = "feature_category_new_apps_enable";

    // 功能默认配置；
    // IOSKnow 默认状态
    public static final String DEF_IOS_KNOW_ENABLE = "def_ios_konw_enable";
    // 桌面默认字体颜色
    public static final String DEF_WORKSPACE_LABEL_COLOR = "def_workspace_label_color";
    // 默认应用主题包
    public static final String DEF_APPLY_THEME_PACKAGE = "def_apply_theme_package";
    // Hotseat是否显示应用名称
    public static final String DEF_HOTSEAT_LABEL_SHOW_ENABLE = "def_hotseat_label_show_enable";
    // AllApp默认显示样式
    public static final String DEF_ALLAPP_DISPLAY_MODE = "def_allapp_display_mode";
    // AllApp默认状态
    public static final String DEF_ALLAPP_ENABLE = "def_allapp_enable";
    // 负一屏默认状态
    public static final String DEF_LEFT_NEWS_PAGE_ENABLE = "def_left_news_page_enable";
    // 桌面搜索框默认状态
    public static final String DEF_SEARCH_BOX_ENABLE = "def_search_box_enable";
    // App Manager默认状态
    public static final String DEF_APP_MANAGER_ENABLE = "def_app_manager_enable";
    // 桌面网格布局
    public static final String DEF_GRID_LAYOUT = "def_grid_rows_columns";
    // 桌面启动应用动画
    public static final String DEF_PENDING_ANIM_STYLE = "def_pending_anim_style";
    // 是否自定义图标文字等；（只在标准版生效）
    public static final String DEF_GRID_ICON_CUSTOM_ENABLE = "def_grid_icon_custom_enable";
    // 自定义桌面图标大小
    public static final String DEF_GRID_ICON_SIZE = "def_grid_icon_size";
    // 自定义桌面名称大小
    public static final String DEF_GRID_LABEL_SIZE = "def_grid_label_size";
    // 桌面布局自动分类
    public static final String DEF_CATEGORY_DEFAULT_LAYOUT = "def_category_default_layout";
    // 桌面新安装应用自动分类
    public static final String DEF_CATEGORY_NEW_APPS = "def_category_new_apps";

    // 用户向导开启
    public static final String DEF_USER_GUIDE_ENABLE = "def_user_guide_enable";
    // shortcut
    public static final String DEF_SHORTCUT_BLACK_LIST = "def_shortcut_black_list";
    // widgets
    public static final String DEF_WIDGET_BLACK_LIST = "def_widget_black_list";
    // fixed widgets
    public static final String DEF_WIDGET_FIXED_LIST = "def_widget_fixed_list";
    // 桌面默认滑动效果
    public static final String DEF_WORKSPACE_SCROLL_EFFECT = "def_workspace_scroll_effect";
    // 桌面默认滑动效果
    public static final String DEF_WORKSPACE_MAGIC_THUMB = "def_workspace_magic_thumb";
    // 桌面加载动画
    public static final String DEF_LAUNCHER_LOADING_VIEW_ENABLE = "def_launcher_loading_view_enable";
    // 一键加速是否显示百分比
    public static final String DEF_CLEAN_WIDGET_SHOW_PERCENT = "def_clean_widget_show_percent";
    // 使用系统默认壁纸作为默认主题壁纸
    public static final String DEF_USE_SYSTEM_DEFAULT_WALLPAPER = "def_use_system_default_wallpaper";
    // cts
    public static final String DEF_CTS_APP_LIST = "def_cts_app_list";
    // hide app
    public static final String DEF_HIDE_APP_LIST = "def_hide_app_list";
    // not uninstall app
    public static final String DEF_NOT_UNINSTALL_APP_LIST = "def_not_uninstall_app_list";
    //流量开关是否自动定位
    public static final String FEATURE_START_LOCATION_WHEN_MOBILE_DATA_OPEN = "feature_start_location_when_mobile_data_open";
    //删除应用图标自动对齐
    public static final String DEF_DESKTOP_AUTO_ALIGN_ENABLE = "def_desktop_auto_align_enable";
    //壁纸滑动是否开启
    public static final String DEF_WALLPAPER_SCROLL_ENABLED = "def_wallpaper_scroll_enabled";
    public static final String DEF_NEWS_PAGE_SWITCH_ENABLED = "def_news_page_switch_enabled";
    // default launcher database version
    public static final String DEF_LAUNCHER_DB_VERSION = "def_launcher_db_version";
    // default launcher iconcahe version
    public static final String DEF_ICONCACHE_DB_VERSION = "def_iconcache_db_version";
    // default launcher setting version
    public static final String DEF_SETTING_DB_VERSION = "def_setting_db_version";
    // default launcher weather version
    public static final String DEF_WEATHER_DB_VERSION = "def_weather_db_version";
    //桌面滑动阀值
    public static final String DEF_LAUNCHER_SIGNIFICANT_MOVE_THRESHOLD = "def_launcher_significant_move_threshold";
    // desktop loop
    public static final String FEATURE_DESKTOP_LOOP_ENABLE = "feature_desktop_loop_enable";
    public static final String DEF_DESKTOP_LOOP_ENABLE = "def_desktop_loop_enable";
    // sleep mode
    public static final String DEF_SLEEP_MODE_ENABLE = "def_sleep_mode_enable";
    //是否懒加载新闻页
    public static final String DEF_DESKTOP_LAZY_LOAD_NEWSPAGER_ENABLE = "def_desktop_lazy_load_newspager_enable";
    //手势默认设置
    public static final String DEF_GESTURE_SETTING = "def_gesture_setting";
    // 产品定义
    public static final String PRODUCT_NAME = "product_name";
    public static final String PRODUCT_OFFICAL_WEBSITE = "product_offical_website";
    public static final String PRODUCT_FACEBOOK_WEBSITE = "product_facebook_website";
    public static final String PRODUCT_EMAIL = "product_email";
    //锁屏请求资源版本号
    public static final String FEATURE_LOCK_SCREEN_VERSION = "feature_lock_screen_version";
    //是否使用主题apk背景
    public static final String DEF_USE_ICON_IN_APK_ENABLE = "def_use_icon_in_apk_enable";
    //是否是刘海屏
    public static final String FEATURE_DISPLAY_CUTOUT_MODE = "feature_display_cutout_mode";
    //是否强制显示hotseat区应用名称
    public static final String DEF_HOTSEAT_LABEL_FORCED_SHOW = "def_hotseat_label_forced_show";

    /**
     * 客户版本 ： com.oslauncher.applauncher.themelauncher.custom 存在则取其中配置，不存在则应用内配置生效
     * 公开版本 ： 只取应用内配置；
     */
    private static synchronized void initalize(Context ctx) {
        if (sPartnerRes == null) {
            Context context = ctx.getApplicationContext();
            // 如果是客户版本，判断配置apk是否存在；读取配置
            if (BuildUtil.isCustomerBuild()) {
                PackageManager pm = context.getPackageManager();

                try {
                    sPartnerPkgName = TEST_PACKAGE;
                    sPartnerRes = pm.getResourcesForApplication(TEST_PACKAGE);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Failed to find resources for " + TEST_PACKAGE);
                }

                if (sPartnerRes == null) {
                    try {
                        sPartnerPkgName = CUSTOM_PACKAGE;
                        sPartnerRes = pm.getResourcesForApplication(CUSTOM_PACKAGE);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e(TAG, "Failed to find resources for " + CUSTOM_PACKAGE);
                    }
                }
            }

            // 客户配置apk不存在，读取自身配置；
            if (sPartnerRes == null) {
                sPartnerRes = context.getResources();
                sPartnerPkgName = context.getPackageName();
            }
        }
    }

    public static String getPackageName(Context context) {
        if (sPartnerRes == null) {
            initalize(context);
        }
        return sPartnerPkgName;
    }

    public static Resources getResources(Context context) {
        if (sPartnerRes == null) {
            initalize(context);
        }

        return sPartnerRes;
    }

    public static Context getPartnerContext(Context context) {
        if (sPartnerContext == null) {
            try {
                sPartnerContext = context.createPackageContext(Partner.TEST_PACKAGE, Context.CONTEXT_IGNORE_SECURITY);
            } catch (PackageManager.NameNotFoundException ex) {

            }

            if (sPartnerContext == null) {
                try {
                    sPartnerContext = context.createPackageContext(Partner.CUSTOM_PACKAGE, Context.CONTEXT_IGNORE_SECURITY);
                } catch (PackageManager.NameNotFoundException ex) {

                }
            }
        }
        return sPartnerContext;
    }

    public static int getInteger(Context context, String key) {
        return getInteger(context, key , 0);
    }

    public static int getInteger(Context context, String key, int def) {
        if (sPartnerRes == null) {
            initalize(context);
        }
        int resId = sPartnerRes.getIdentifier(key, "integer",
                sPartnerPkgName);

        return (resId != 0) ? sPartnerRes.getInteger(resId) : def;
    }


    public static boolean getBoolean(Context context, String key) {
        return getBoolean(context, key, false);
    }

    public static boolean getBoolean(Context context, String key, boolean def) {
        if (sPartnerRes == null) {
            initalize(context);
        }
        int resId = sPartnerRes.getIdentifier(key, "bool",
                sPartnerPkgName);

        return (resId != 0) ? sPartnerRes.getBoolean(resId) : def;
    }

    public static String getString(Context context, String key) {
        if (sPartnerRes == null) {
            initalize(context);
        }
        int resId = sPartnerRes.getIdentifier(key, "string",
                sPartnerPkgName);

        return (resId != 0) ? sPartnerRes.getString(resId) : "";
    }

    public static String[] getStringArray(Context context, String key) {
        if (sPartnerRes == null) {
            initalize(context);
        }
        int resId = sPartnerRes.getIdentifier(key, "array",
                sPartnerPkgName);

        return (resId != 0) ? sPartnerRes.getStringArray(resId) : null;
    }

    public static int getColor(Context context, String key, int def) {
        if (sPartnerRes == null) {
            initalize(context);
        }
        int resId = sPartnerRes.getIdentifier(key, "color",
                sPartnerPkgName);

        return (resId != 0) ? sPartnerRes.getColor(resId) : def;
    }


    public static int getDimensionDpSize(Context context, String key) {
        if (sPartnerRes == null) {
            initalize(context);
        }
        int resId = sPartnerRes.getIdentifier(key, "dimen",
                sPartnerPkgName);

        int dimPx = (resId != 0) ? sPartnerRes.getDimensionPixelSize(resId) : 0;
        return DisplayUtil.px2dip(context, dimPx);
    }


    public static int getXmlResId(Context context, String key) {
        if (sPartnerRes == null) {
            initalize(context);
        }
        int defaultLayout = sPartnerRes.getIdentifier(key,
                "xml", sPartnerPkgName);
        return defaultLayout;
    }


}

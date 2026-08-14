package com.amz.ios.launcher.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.launcher.LauncherSettingSubject;
import com.amz.ios.ioslite.common.setting.IOSSettings;
import com.amz.ios.launcher.AppInfo;
import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.InstallShortcutReceiver;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherSettings;


public class Settings {
    private static final String SHARED_PREFS_FILE = "launcher_settings";

    public static final String PREFER_WORKSPACE_TEXT_FONT = "launcher_workspace_text_font";

    // 桌面壁纸是否滚动
    public static final String PREFER_WALLPAPER_SCROLL_ENABLE = "launcher_wallpaper_scroll_enabled";

    // 桌面搜索小部件是否打开
    public static final String PREFER_SEARCH_BAR_ENABLE = "launcher_search_bar_enabled";

    // 桌面负一屏否打开
    public static final String PREFER_LEFT_PAGE_ENABLE = "launcher_left_page_enabled";

    // 桌面悬浮空间IOSKnow是否打开
    public static final String PREFER_SWING_VIEW_ENABLE = "launcher_swing_view_enabled";

    public static final String PREFER_SLEEP_MODE_ENABLE = "launcher_swing_view_enabled";


    // AllApps内应用管理空间是否打开
    public static final String PREFER_APPMGR_SHOW = "launcher_appmgr_show";

    // 桌面启动应用动画
    public static final String PREFER_APP_ANIMATION_STYLE = "launcher_app_animation_style";

    // AllApp 显示样式
    public static final String PREFER_ALL_APPS_DISPLAY_MODE = "launcher_all_apps_display_mode";

    // 桌面滑动特效
    public static final String PREFER_WORKSPACE_SCROLL_EFFECT = "launcher_workspace_scroll_effect";

    // 桌面滑动特效
    public static final String PREFER_WORKSPACE_MAGIC_THUMB = "launcher_workspace_magic_thumb";

    // 桌面图标缩放比例
    public static final String PREFER_WORKSPACE_ICON_SIZE_SCALE = "launcher_workspace_icon_size_scale";

    // 桌面图标文字缩放比例
    public static final String PREFER_WORKSPACE_TEXT_SIZE_SCALE = "launcher_workspace_text_size_scale";

    // 桌面图标文字颜色
    public static final String PREFER_WORKSPACE_ICON_LABEL_COLOR = "launcher_workspace_icon_label_color";

    // 桌面默认主页
    public static final String PREFER_WORKSPACE_DEFAULT_PAGE = "launcher_workspace_default_page";

    // 桌面删除部件释放动画
    public static final String PREFER_DROP_TARGET_ANIM_STYLE = "launcher_drop_target_anim_style";

    // 魔法手指
    public static final String PREFER_MAGIC_FINGER_STYLE = "launcher_magic_finger_style";

    // AllApp是否启用
    public static final String PREFER_ALLAPPS_ENABLE = "launcher_allapps_enable";

    // 桌面行列布局
    public static final String PREFER_DESKTOP_GRID = "launcher_desktop_grid";

    // 桌面重置
    public static final String PREFER_DESKTOP_RESET = "launcher_desktop_reset";

    // 开发这模式是否启用
    public static final String PREFER_DEVELOPER_ENABLE = "launcher_developer_enable";

    //负一屏幕开关是否显示
    public static final String PREFER_NEWS_PAGE_SWITCH_ENABLED = "launcher_news_page_switch_enable";

    //桌面文件夹底部探索是否显示
    public static final String PREFER_FOLDER_DISCOVERY_USER_ENABLED = "launcher_discovery_user_enable";
    public static final String PREFER_FOLDER_DISCOVERY_FEATURE_ENABLED = "launcher_discovery_feature_enable";
    public static final String IOS_NETWORK_FOLDER_DISCOVERY_LIST = "launcher_network_folder_discovery_list";

    //桌面是否显示卸载入口
    public static final String PREFER_NOT_UNINSTALL_APP_LIST = "not_uninstall_app_list";

    //桌面网络控制快捷方式黑名单是否生效
    public static final String PREFER_NETWORK_SHORTCUT_BLACKLIST_ENABLED = "launcher_network_shortcut_blacklist_enabled";

    //桌面快捷方式黑名单
    public static final String PREFER_NETWORK_SHORTCUT_BLACK_LIST = "prefer_shortcut_black_list";

    // 桌面内存检测窗口
    public static final String PREFER_MEMORY_WATCHER_ENABLE = "launcher_memory_wather_enable";

    // 应用智能整理
    public static final String PREFER_CATEGORY_NEW_APPS_ENABLE = "launcher_category_new_apps";

    // 应用自动对齐
    public static final String PREFER_DESKTOP_AUTO_ALIGN_ENABLE = "launcher_desktop_auto_align_enable";

    // Màn General: bật/tắt hiệu ứng blur nền desktop (dock, widget đồng hồ, app library). Default true.
    public static final String PREFER_DESKTOP_BLUR_ENABLE = "launcher_desktop_blur_enable";

    // Màn General: ẩn tên app (ON = ẩn) ở workspace + App Library. Default false = hiện tên.
    public static final String PREFER_HIDE_APP_LABEL = "launcher_hide_app_label";

    // 循环滑动
    public static final String PREFER_DESKTOP_LOOP_ENABLE = "launcher_desktop_loop_enable";

    //是否使用apk中icon背景
    public static final String PREFER_USE_ICON_IN_APK_ENABLE = "launcher_use_icon_in_apk_enabled";

    //navigationbar弹出时，隐藏hotseat的应用名称;navigationbar隐藏时，显示hotseat的应用名称
    public static final String PREFER_HOTSEAT_APP_NAME_VISIBILITY = "launcher_hotseat_app_name_visibility";

    //是否重置系统默认壁纸
    public static final String PREFER_RESET_DEFAULT_WALLPAPER = "launcher_reset_default_wallpaper";

    //是否重置系统默认壁纸
    public static final String PREFER_HIDDENAPP_PASSWORD = "launcher_hidden_app_password";

    public static void notifyChange(String key) {
        LauncherSettingSubject.handleLauncherSettingChanged(key);
    }


    private static boolean getBoolean(Context context, String key, boolean defaultValue) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        return sp.getBoolean(key, defaultValue);
    }

    private static boolean getBoolean(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        return sp.getBoolean(key, false);
    }

    private static void putBoolean(Context context, String key, boolean value) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(key, value);
        editor.commit();

        notifyChange(key);
    }

    private static String getString(Context context, String key, String defValue) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        return sp.getString(key, defValue);
    }

    private static void putString(Context context, String key, String value) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.apply();

        notifyChange(key);
    }

    private static int getInt(Context context, String key, int defaultVale) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        return sp.getInt(key, defaultVale);
    }

    private static int getInt(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        return sp.getInt(key, 0);
    }

    private static void putInt(Context context, String key, int value) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(key, value);
        editor.apply();
        notifyChange(key);
    }

    private static float getFloat(Context context, String key, float defValue) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        return sp.getFloat(key, defValue);
    }

    private static void putFloat(Context context, String key, float value) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(key, value);
        editor.apply();
        notifyChange(key);
    }

    public static boolean isSearchBarEnabled(Context context) {
        return getBoolean(context, PREFER_SEARCH_BAR_ENABLE, Partner.getBoolean(context, Partner.DEF_SEARCH_BOX_ENABLE,true));
    }

    public static void setSearchBarEnabled(Context context, boolean value) {
        putBoolean(context, PREFER_SEARCH_BAR_ENABLE, value);
    }


    public static boolean isLeftPageEnabled(Context context) {
        return getBoolean(context, PREFER_LEFT_PAGE_ENABLE, Partner.getBoolean(context, Partner.DEF_LEFT_NEWS_PAGE_ENABLE, true));
    }

    public static void setLeftPageEnabled(Context context, boolean value) {
        putBoolean(context, PREFER_LEFT_PAGE_ENABLE, value);
    }

    public static boolean isSwingViewEnabled(Context context) {
        return getBoolean(context, PREFER_SWING_VIEW_ENABLE, Partner.getBoolean(context, Partner.DEF_IOS_KNOW_ENABLE));
    }

    public static void setSwingViewEnabled(Context context, boolean value) {
        putBoolean(context, PREFER_SWING_VIEW_ENABLE, value);
    }

    public static boolean isSleepModeEnabled(Context context) {
        return getBoolean(context, PREFER_SLEEP_MODE_ENABLE, Partner.getBoolean(context, Partner.DEF_SLEEP_MODE_ENABLE));
    }

    public static void setSleepModeEnabled(Context context, boolean value) {
        putBoolean(context, PREFER_SLEEP_MODE_ENABLE, value);
    }

    public static void setAppmgrShow(Context context, boolean value) {
        putBoolean(context, PREFER_APPMGR_SHOW, value);
    }

    public static boolean isAppmgrShow(Context context) {
        return getBoolean(context, PREFER_APPMGR_SHOW,
                Partner.getBoolean(context, Partner.FEATURE_APP_MANAGER_ENABLE) && Partner.getBoolean(context, Partner.DEF_APP_MANAGER_ENABLE)
        );
    }


    public static String getThemePackageName(Context context) {
        return IOSSettings.getString(context.getContentResolver(), IOSSettings.Launcher.LAUNCHER_THEME_PACKAGE);
    }

    public static void saveThemePackageName(Context context, String value) {
        IOSSettings.putString(context.getContentResolver(), IOSSettings.Launcher.LAUNCHER_THEME_PACKAGE, value);
    }


    public static int getAppAnimationStyle(Context context) {
        return getInt(context, PREFER_APP_ANIMATION_STYLE, Partner.getInteger(context, Partner.DEF_PENDING_ANIM_STYLE));
    }

    public static void setAppAnimationStyle(Context context, int value) {
        putInt(context, PREFER_APP_ANIMATION_STYLE, value);
    }

    public static boolean isWallpaperScrollEnabled(Context context) {
        return IOSSettings.getBoolean(context.getContentResolver(), IOSSettings.Launcher.LAUNCHER_WALLPAPER_SCROLL_ENABLE, Partner.getBoolean(context, Partner.DEF_WALLPAPER_SCROLL_ENABLED));
    }

    public static void setWallpaperScrollEnabled(Context context, boolean value) {
        IOSSettings.putBoolean(context.getContentResolver(), IOSSettings.Launcher.LAUNCHER_WALLPAPER_SCROLL_ENABLE, value);
        notifyChange(PREFER_WALLPAPER_SCROLL_ENABLE);
    }

    public static int getAllAppsDisplayMode(Context context) {
        return getInt(context, PREFER_ALL_APPS_DISPLAY_MODE, Partner.getInteger(context, Partner.DEF_ALLAPP_DISPLAY_MODE));
    }


    public static void setAllAppsDisplayMode(Context context, int mode) {
        putInt(context, PREFER_ALL_APPS_DISPLAY_MODE, mode);
    }

    public static String getWorkspaceScrollEffect(Context context) {
        return getString(context, PREFER_WORKSPACE_SCROLL_EFFECT, Partner.getString(context, Partner.DEF_WORKSPACE_SCROLL_EFFECT));
    }

    public static void setWorkspaceScrollEffect(Context context, String value) {
        putString(context, PREFER_WORKSPACE_SCROLL_EFFECT, value);
    }

    public static String getWorkspaceMagicThumb(Context context) {
        return getString(context, PREFER_WORKSPACE_MAGIC_THUMB, Partner.getString(context, Partner.DEF_WORKSPACE_MAGIC_THUMB));
    }

    public static void setWorkspaceMagicThumb(Context context, String value) {
        putString(context, PREFER_WORKSPACE_MAGIC_THUMB, value);
    }

    public static int getWorkspaceLabelColor(Context context) {
        return getInt(context, IOSSettings.Launcher.LAUNCHER_WORKSPACE_ICON_LABEL_COLOR, 0xFFFFFFFF);
//        return IOSSettings.getInt(context.getContentResolver(), IOSSettings.Launcher.LAUNCHER_WORKSPACE_ICON_LABEL_COLOR);
    }

    public static void setWorkspaceLabelColor(Context context, int value) {
        putInt(context, IOSSettings.Launcher.LAUNCHER_WORKSPACE_ICON_LABEL_COLOR, value);
//        IOSSettings.putInt(context.getContentResolver(), IOSSettings.Launcher.LAUNCHER_WORKSPACE_ICON_LABEL_COLOR, value);
        notifyChange(PREFER_WORKSPACE_ICON_LABEL_COLOR);
    }

    public static float getWorkspaceIconSizeScale(Context context) {
        return getFloat(context, PREFER_WORKSPACE_ICON_SIZE_SCALE, 1.0f);
    }

    public static void setWorkspaceIconSizeScale(Context context, float scale) {
        putFloat(context, PREFER_WORKSPACE_ICON_SIZE_SCALE, scale);
    }

    public static float getWorkspaceTextSizeScale(Context context) {
        return getFloat(context, PREFER_WORKSPACE_TEXT_SIZE_SCALE, 1.0f);
    }

    public static void setWorkspaceTextSizeScale(Context context, float scale) {
        putFloat(context, PREFER_WORKSPACE_TEXT_SIZE_SCALE, scale);
    }

    public static int getWorkspaceDefaultPage(Context context) {
        return getInt(context, PREFER_WORKSPACE_DEFAULT_PAGE, 0);
    }

    public static void setWorkspaceDefaultPage(Context context, int value) {
        putInt(context, PREFER_WORKSPACE_DEFAULT_PAGE, value);
    }


    public static int getDropTargetAnimStyle(Context context) {
        return getInt(context, PREFER_DROP_TARGET_ANIM_STYLE, 0);
    }

    public static void setDropTargetAnimStyle(Context context, int value) {
        putInt(context, PREFER_DROP_TARGET_ANIM_STYLE, value);
    }

    public static int getMagicFingerStyle(Context context) {
        return getInt(context, PREFER_MAGIC_FINGER_STYLE, 0);
    }

    public static void setMagicFingerStyle(Context context, int value) {
        putInt(context, PREFER_MAGIC_FINGER_STYLE, value);
    }

    public static int getDesktopGrid(Context context) {
        return getInt(context, PREFER_DESKTOP_GRID, Partner.getInteger(context, Partner.DEF_GRID_LAYOUT));
    }

    public static void setDesktopGrid(Context context, int value) {
        putInt(context, PREFER_DESKTOP_GRID, value);
    }

    /**
     * User đã chủ động chọn grid (màn Screen Grid) hay chưa. Dùng để chỉ áp custom grid khi có
     * lựa chọn thật, giữ nguyên hành vi mặc định (tính theo tỉ lệ màn hình) cho user chưa chọn.
     */
    public static boolean hasDesktopGrid(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        return sp.contains(PREFER_DESKTOP_GRID);
    }

    public static boolean isDeveloperEnabled(Context context) {
        return getBoolean(context, PREFER_DEVELOPER_ENABLE, false);
    }

    public static void setDeveloperEnabled(Context context, boolean value) {
        putBoolean(context, PREFER_DEVELOPER_ENABLE, value);
    }

    public static boolean isNewsPageSwitchEnable(Context context) {
        return getBoolean(context, PREFER_NEWS_PAGE_SWITCH_ENABLED, Partner.getBoolean(context, Partner.DEF_NEWS_PAGE_SWITCH_ENABLED, false));
    }

    public static void setNewsPageSwitchEnable(Context context, boolean value) {
        putBoolean(context, PREFER_NEWS_PAGE_SWITCH_ENABLED, value);
    }

    public static boolean isFolderDiscoveryEnable(Context context) {
        return isFolderDiscoveryFeatureEnable(context) && isFolderDiscoveryUserEnable(context);
    }

    public static boolean isFolderDiscoveryFeatureEnable(Context context) {
        return getBoolean(context, PREFER_FOLDER_DISCOVERY_FEATURE_ENABLED, Partner.getBoolean(context, Partner.FEATURE_IOS_DISCOVERY_ENABLE, false));
    }

    public static boolean isFolderDiscoveryUserEnable(Context context) {
        return getBoolean(context, PREFER_FOLDER_DISCOVERY_USER_ENABLED, true);
    }

    public static void setFolderDiscoveryFeatureEnable(Context context, boolean value) {
        putBoolean(context, PREFER_FOLDER_DISCOVERY_FEATURE_ENABLED, value);
    }

    public static void setNetworkFolderDiscoveryListStr(Context context, String listStr) {
        putString(context, IOS_NETWORK_FOLDER_DISCOVERY_LIST, listStr);
    }

    public static String[] getNetworkFolderDiscoveryList(Context context) {
        String[] networkFolderDiscoveryList = null;
        String networkFolderDiscoveryListStr = getString(context, IOS_NETWORK_FOLDER_DISCOVERY_LIST, null);
        if (!TextUtils.isEmpty(networkFolderDiscoveryListStr)) {
            try {
                networkFolderDiscoveryList = networkFolderDiscoveryListStr.split(",");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return networkFolderDiscoveryList;
    }

    public static void setFolderDiscoveryUserEnable(Context context, boolean value) {
        putBoolean(context, PREFER_FOLDER_DISCOVERY_USER_ENABLED, value);
    }

    public static void setNetworkShortcutBlacklistEnabled(Context context, boolean value) {
        putBoolean(context, PREFER_NETWORK_SHORTCUT_BLACKLIST_ENABLED, value);
    }

    public static boolean isNetworkShortcutBlacklistEnabled(Context context) {
        return getBoolean(context, PREFER_NETWORK_SHORTCUT_BLACKLIST_ENABLED, false);
    }


    public static boolean isMemWatcherEnabled(Context context) {
        return getBoolean(context, PREFER_MEMORY_WATCHER_ENABLE, false);
    }

    public static void setMemWatcherEnabled(Context context, boolean value) {
        putBoolean(context, PREFER_MEMORY_WATCHER_ENABLE, value);
    }

    public static boolean isFingerprintEnable(Context context) {
        return android.provider.Settings.System.getInt(context.getContentResolver(), "ios_fp_home_control", 0) != 0;
    }

    public static boolean isNewAppsCategoryEnable(Context context) {
        return getBoolean(context, PREFER_CATEGORY_NEW_APPS_ENABLE, Partner.getBoolean(context, Partner.DEF_CATEGORY_NEW_APPS));
    }

    public static void setNewAppsCategoryEnable(Context context, boolean value) {
        putBoolean(context, PREFER_CATEGORY_NEW_APPS_ENABLE, value);
    }

    public static boolean isDesktopAlignEnable(Context context) {
        return getBoolean(context, PREFER_DESKTOP_AUTO_ALIGN_ENABLE, Partner.getBoolean(context, Partner.DEF_DESKTOP_AUTO_ALIGN_ENABLE));
    }

    public static void setDesktopAlignEnable(Context context, boolean value) {
        putBoolean(context, PREFER_DESKTOP_AUTO_ALIGN_ENABLE, value);
    }

    /** Blur nền desktop có đang bật không (default true). Các view blur tự fallback khi tắt. */
    public static boolean isDesktopBlurEnable(Context context) {
        return getBoolean(context, PREFER_DESKTOP_BLUR_ENABLE, true);
    }

    public static void setDesktopBlurEnable(Context context, boolean value) {
        putBoolean(context, PREFER_DESKTOP_BLUR_ENABLE, value);
    }

    /** Có ẩn tên app không (ON = ẩn). Default false = hiện tên. */
    public static boolean isHideAppLabel(Context context) {
        return getBoolean(context, PREFER_HIDE_APP_LABEL, false);
    }

    public static void setHideAppLabel(Context context, boolean value) {
        putBoolean(context, PREFER_HIDE_APP_LABEL, value);
    }

    public static boolean sLoopEnable;

    public static boolean isDesktopLoopEnable(Context context) {
        sLoopEnable = getBoolean(context, PREFER_DESKTOP_LOOP_ENABLE, Partner.getBoolean(context, Partner.DEF_DESKTOP_LOOP_ENABLE));
        return sLoopEnable;
    }

    public static void setDesktopLoopEnable(Context context, boolean value) {
        sLoopEnable = value;
        putBoolean(context, PREFER_DESKTOP_LOOP_ENABLE, value);
    }

    public static boolean isUseIconInApkEnable(Context context) {
        return getBoolean(context, PREFER_USE_ICON_IN_APK_ENABLE, Partner.getBoolean(context, Partner.DEF_USE_ICON_IN_APK_ENABLE, false));
    }

    public static String[] getNotUninstallAppList(Context context) {
        String[] notUninstallList = null;
        String notUninstallStr = getString(context, PREFER_NOT_UNINSTALL_APP_LIST, null);
        if (!TextUtils.isEmpty(notUninstallStr)) {
            try {
                notUninstallList = notUninstallStr.split(",");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return notUninstallList;
    }

    public static void setNotUninstallAppListStr(Context context, String listStr) {
        putString(context, PREFER_NOT_UNINSTALL_APP_LIST, listStr);
        AppInfo.refreshNotUninstallApp();
    }

    public static void setNetworkShortcutBlacklistStr(Context context, String listStr) {
        putString(context, PREFER_NETWORK_SHORTCUT_BLACK_LIST, listStr);
        InstallShortcutReceiver.updateNetworkShortcutBlacklist(context);

    }

    public static String[] getNetworkShortcutBlacklist(Context context) {
        String[] networkShortcutBlacklist = null;
        String shortcutBlacklistStr = getString(context, PREFER_NETWORK_SHORTCUT_BLACK_LIST, null);
        if (!TextUtils.isEmpty(shortcutBlacklistStr)) {
            try {
                networkShortcutBlacklist = shortcutBlacklistStr.split(",");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return networkShortcutBlacklist;
    }

    public static boolean isHotseatAppNameVisibility(Context context) {
        return false;
//        boolean labelShowEnable = Partner.getBoolean(context, Partner.DEF_HOTSEAT_LABEL_SHOW_ENABLE);
//        return labelShowEnable && getBoolean(context, PREFER_HOTSEAT_APP_NAME_VISIBILITY);
    }

    public static void setHotseatAppNameVisibility(Context context,boolean visibility){
        putBoolean(context,PREFER_HOTSEAT_APP_NAME_VISIBILITY,visibility);
    }

    public static boolean shouldResetDefaultWallpaper(Context context) {
        return getBoolean(context, PREFER_RESET_DEFAULT_WALLPAPER, true);
    }

    public static void setPreferResetDefaultWallpaper(Context context, boolean shouldReset) {
        putBoolean(context, PREFER_RESET_DEFAULT_WALLPAPER, shouldReset);
    }

    public static int getWorkspaceTextFont(Context context) {
        return getInt(context, PREFER_WORKSPACE_TEXT_FONT, 0);
    }

    public static void setWorkspaceTextFont(Context context, int fontType) {
        putInt(context, PREFER_WORKSPACE_TEXT_FONT, fontType);

        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getInvariantDeviceProfile().portraitProfile;
        grid.workspaceIconTextFont = getWorkspaceTextFont(context);
    }

    public static void setHiddenAppPassword(Context context, String password) {
        putString(context, PREFER_HIDDENAPP_PASSWORD, password);
    }

    public static String getHiddenAppPassword(Context context) {
        return getString(context, PREFER_HIDDENAPP_PASSWORD, "");
    }
}

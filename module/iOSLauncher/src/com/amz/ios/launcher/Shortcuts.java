package com.amz.ios.launcher;

import android.content.Context;

public class Shortcuts {
    private static final String TAG = "Shortcuts";

    public static final String SHORTCUT_ID = "shortcutId";
    public static final String SHORTCUT_ACTION = "com.ios.ioslite.shortcut";

    public static final int SHORTCUT_T9_SEARCH = 1;
    public static final int SHORTCUT_LOCKSCREEN = 2;
    public static final int SHORTCUT_OS_SETTING = 3;
    public static final int SHORTCUT_RESPONSE = 4;
    public static final int SHORTCUT_CHANGE_THEME = 5;
    public static final int SHORTCUT_THEMECULB = 100;
    public static final int SHORTCUT_SECURITY = 101;
    public static final int SHORTCUT_HEALTHYCENTER = 102;

    public static String getShortcutResName(int shortcutId) {
        switch (shortcutId) {
            case SHORTCUT_T9_SEARCH:
                return "lite_rom_t9_search";
            case SHORTCUT_LOCKSCREEN:
                return "ic_app_locker";
            case SHORTCUT_OS_SETTING:
                return "lite_rom_launcher_settings";
            case SHORTCUT_RESPONSE:
                return "lite_rom_feedback";
            case SHORTCUT_CHANGE_THEME:
                return "lite_rom_change_theme";
            case SHORTCUT_THEMECULB:
                return "ic_app_theme";
            case SHORTCUT_SECURITY:
                return "ic_app_security";
            default:
                return "";
        }
    }

    public static String getShortcutTitle(int shortcutId, Context context) {
        int resourceId = -1;
        switch (shortcutId) {
            case SHORTCUT_T9_SEARCH:
                resourceId = R.string.shortcut_search_app;
                break;
            case SHORTCUT_LOCKSCREEN:
                resourceId = R.string.shortcut_lock_screen;
                break;
            case SHORTCUT_OS_SETTING:
                resourceId = R.string.shortcut_os_setting;
                break;
            case SHORTCUT_RESPONSE:
                resourceId = R.string.shortcut_response;
                break;
            case SHORTCUT_CHANGE_THEME:
                resourceId = R.string.shortcut_change_theme;
                break;
            case SHORTCUT_THEMECULB:
                resourceId = R.string.themeclub_app_name;
                break;
            case SHORTCUT_SECURITY:
                resourceId = R.string.sc_app_name;
                break;
            default:
                return "";
        }
        return context.getString(resourceId);
    }
}

package com.amz.ios.ioslite.common.setting;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;

import com.amz.ios.ioslite.common.R;
import com.amz.ios.ioslite.common.launcher.LauncherSettingSubject;

/**
 * Created by huangshuai on 16-10-26.
 */
public class IOSSettings {
    private static final String TAG = "IOSSettings";
    private static final String SHARED_PREFS_FILE = "launcher_settings";


    private static final String AUTHORITY = "com.amz.ios.ioslite.common.setting".intern();

    private static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/settings");

    static final String METHOD_GET_BOOLEAN = "get_boolean_setting";
    static final String METHOD_SET_BOOLEAN = "set_boolean_setting";
    static final String METHOD_GET_STRING = "get_string_setting";
    static final String METHOD_SET_STRING = "set_string_setting";
    static final String METHOD_GET_INTEGER = "get_integer_setting";
    static final String METHOD_SET_INTEGER = "set_integer_setting";
    static final String METHOD_GET_FLOAT = "get_float_setting";
    static final String METHOD_SET_FLOAT = "set_float_setting";


    static final String EXTRA_VALUE = "value";
    static final String EXTRA_DEFAULT_VALUE = "default_value";


    /**
     * Common base for tables of name/value settings.
     */
    private static class NameValueTable implements BaseColumns {
        public static final String NAME = "name";

    }

    /**
     * Global config settings, containing preferences that plugin enable or disable;
     */
    public static final class Global extends NameValueTable {

        public static final String SUSPENDED_BALL_ENABLED = "global.suspended_ball_enable";
    }

    /**
     * Home Launcher settings, containing preferences that
     */
    public static final class Launcher extends NameValueTable {

        public static final String FIRST_RUN_APP = "launcher.first_run_app";


        /**
         * Save the app icon texe size on launcher.workspace;
         * <p>
         * it will be used by appwidget of app icon style ,such as com.ios.cleanwidget.CleanWidgetProvider;
         */
        public static final String LAUNCHER_APP_LABEL_SIZE_PX = "launcher.app_label_size_px";

        public static final String LAUNCHER_APP_ICON_SIZE_PX = "launcher.app_icon_size_px";

        public static final String LAUNCHER_APP_ICON_LABEL_PADDING_PX = "launcher.app_label_padding_px";


        /**
         * Launcher current applay theme package name;
         * <p>
         * it will be used by themeclub;
         */

        public static final String LAUNCHER_THEME_PACKAGE = "launcher.theme_package";

        /**
         * Launcher current applay wallpaper id;
         * <p>
         * it will be used by themeclub;
         */

        public static final String LAUNCHER_WALLPAPER_ID = "launcher.wallpaper_id";

        /**
         * Launcher wallpaper scroll;
         */
        public static final String LAUNCHER_WALLPAPER_SCROLL_ENABLE = "launcher_wallpaper_scroll_enabled";

        /**
         * Launcher workspace text color;
         */
        public static final String LAUNCHER_WORKSPACE_ICON_LABEL_COLOR = "launcher.workspace_label_color";
    }

    /**
     * Local Notification settings, containing preferences that
     */
    public static final class Notify extends NameValueTable {

        public static final String NOTIFY_DEFAULT_HOME = "notify.def_home";

        public static final String NOTIFY_LOW_BATTERY = "notify.low_battery";

        public static final String NOTIFY_LOW_MEMORY = "notify.low_memory";
        ;
        public static final String NOTIFY_NEW_WALLPAPER = "notify.new_wallpaper";
        ;
        public static final String NOTIFY_NEW_THEME = "notify.new_theme";
    }

    /**
     * Advertise
     */
    public static final class Advertise extends NameValueTable {
        public static final String AD_OFFLINE_MODE = "ad.offline_mode";
        public static final String AD_GLOBAL_SWITCH = "ad.global_switch";
        public static final String AD_TEST_MODE = "ad.test_mode";
    }


    public static Uri getUriFor(String name) {
        return Uri.withAppendedPath(CONTENT_URI, name);
    }


    /**
     * Convenience function for retrieving a single settings value
     * as an integer.
     *
     * @param name The name of the setting to retrieve.
     * @param def  Value to return if the setting is not defined.
     * @return The setting's current value, or 'def' if it is not defined
     * or not a valid integer.
     */
    public static int getInt(ContentResolver cr, String name, int def) {
        Bundle extras = new Bundle();
        extras.putInt(EXTRA_DEFAULT_VALUE, def);

        Bundle value = cr.call(
                CONTENT_URI,
                METHOD_GET_INTEGER,
                name, extras);
        return value.getInt(EXTRA_VALUE);
    }

    // default value -1;
    public static int getInt(ContentResolver cr, String name) {
        return getInt(cr, name, -1);
    }

    /**
     * Convenience function for updating a single settings value as an
     * integer.
     *
     * @param cr    The ContentResolver to access.
     * @param name  The name of the setting to modify.
     * @param value The new value for the setting.
     */
    public static void putInt(ContentResolver cr, String name, int value) {
        Bundle extras = new Bundle();
        extras.putInt(EXTRA_VALUE, value);
        cr.call(
                CONTENT_URI,
                METHOD_SET_INTEGER,
                name, extras);
    }


    /**
     * Convenience function for retrieving a single settings value
     * as boolean value.
     *
     * @param name The name of the setting to retrieve.
     * @param def  Value to return if the setting is not defined.
     * @return The setting's current value, or 'def' if it is not defined
     * or not a valid boolean.
     */
    public static boolean getBoolean(ContentResolver cr, String name, boolean def) {
        Bundle extras = new Bundle();
        extras.putBoolean(EXTRA_DEFAULT_VALUE, def);

        Bundle value = cr.call(
                CONTENT_URI,
                METHOD_GET_BOOLEAN,
                name, extras);
        return value.getBoolean(EXTRA_VALUE);
    }

    // default value false;
    public static boolean getBoolean(ContentResolver cr, String name) {
        return getBoolean(cr, name, false);
    }


    /**
     * Convenience function for updating a single settings value as an
     * boolean value.
     *
     * @param cr    The ContentResolver to access.
     * @param name  The name of the setting to modify.
     * @param value The new value for the setting.
     */
    public static void putBoolean(ContentResolver cr, String name, boolean value) {

        Bundle extras = new Bundle();
        extras.putBoolean(EXTRA_VALUE, value);
        cr.call(
                CONTENT_URI,
                METHOD_SET_BOOLEAN,
                name, extras);
    }


    /**
     * Convenience function for retrieving a single settings value
     * as string.
     *
     * @param name The name of the setting to retrieve.
     * @param def  Value to return if the setting is not defined.
     * @return The setting's current value, or 'def' if it is not defined
     * or not a valid string.
     */
    public static String getString(ContentResolver cr, String name, String def) {
        Bundle extras = new Bundle();
        extras.putString(EXTRA_DEFAULT_VALUE, def);

        Bundle value = cr.call(CONTENT_URI, METHOD_GET_STRING, name, extras);
        return value.getString(EXTRA_VALUE);
    }

    // default value null;
    public static String getString(ContentResolver cr, String name) {
        return getString(cr, name, "");
    }

    /**
     * Convenience function for updating a single settings value as a
     * string value.
     *
     * @param cr    The ContentResolver to access.
     * @param name  The name of the setting to modify.
     * @param value The new value for the setting.
     */
    public static void putString(ContentResolver cr, String name, String value) {
        Bundle extras = new Bundle();
        extras.putString(EXTRA_VALUE, value);
        cr.call(
                CONTENT_URI,
                METHOD_SET_STRING,
                name, extras);
    }

    public static float getFloat(ContentResolver cr, String name, float def) {
        Bundle extras = new Bundle();
        extras.putFloat(EXTRA_DEFAULT_VALUE, def);

        Bundle value = cr.call(
                CONTENT_URI,
                METHOD_GET_FLOAT,
                name, extras);
        return value.getFloat(EXTRA_VALUE);
    }

    // default value null;
    public static float getFloat(ContentResolver cr, String name) {
        return getFloat(cr, name, 0);
    }

    public static void putFloat(ContentResolver cr, String name, float value) {
        Bundle extras = new Bundle();
        extras.putFloat(EXTRA_VALUE, value);
        cr.call(
                CONTENT_URI,
                METHOD_SET_FLOAT,
                name, extras);
    }

    public static void setWallpaperId(Context context, int wallpaperId) {
        IOSSettings.putInt (context.getContentResolver(), IOSSettings.Launcher.LAUNCHER_WALLPAPER_ID, wallpaperId);
    }
}

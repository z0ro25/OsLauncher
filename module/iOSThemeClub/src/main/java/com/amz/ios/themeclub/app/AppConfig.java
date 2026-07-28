package com.amz.ios.themeclub.app;

import android.content.Context;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.setting.IOSSettings;

/**
 * Created by server on 16-11-16.
 */

public class AppConfig {
//    public final static int TAB_WALLPAPER = 0;

//    public final static int TAB_MINE = TAB_LOCK + 1;
    public final static int TAB_MINE = 0;
    public final static int TAB_WALLPAPER = TAB_MINE + 1;
    public final static int TAB_THEME = TAB_WALLPAPER + 1;
    public final static int TAB_LOCK = TAB_THEME+1;

    public final static String ENCODE_DECODE_KEY = "w_slx_001";
    public static String LOCK_ENCODE_DECODE_KEY = "w_cl_52c";
    public final static String CACHE_PATH = "/themes/download/cache/";
    public final static String HW_SERVER_URL = "http://beautyseas.dd351.com:2018";
    public final static String  CN_SERVER = "http://beauticenter.yy845.com";
    public static String SERVER_URL_FORMAL = "http://mhzx.yy845.com:6000";
    public static final String LOCKSCREEN_PACKAGE = "ios_lockscreen_package";
    public static final String FUN_UX_DIR = "fun_ux";
    public static final String FUN_UX_DEFAULT_NAME = "fun_ux.ux";
    public static final String FUN_UX_ASSET_NAME = "w.ux";
    public static final String THEMECLUB_PREVIEW_DEFAULT = "com.oslauncher.applauncher.themelauncher";
    public static final String ACTION_LOCKSCREEN_WALLPAPER_CHANGED = "WallpaperManager.ACTION_LOCKSCREEN_WALLPAPER_CHANGED";
    public static final String KEY_THEME_LOCKSCREEN_FUN_UX_VALUE = "key_theme_lockscreen_fun_ux_value";
    public static final String API_KEY = "MHZX_3b58249e479e44a2911600960f795dcd";
    public final static String TEST_SERVER_URL = "http://192.168.0.52:2017";
    public static final int COLUMN_NEWEEST = 2;
    public static final int COLUMN_MANIMALIST = 3;
    public final static String WALLPAPER_NATIVE_PATH = "/themes/download/";
    public static class MessageCode{
        public final static int MESSAGECODE_THEME_NEWEST = 100001;
        public final static int MESSAGECODE_THEME_SELECTION = 100006;
        public final static int MESSAGECODE_SELECTION_WALLPAPER = 100002;
        public final static int MESSAGECODE_NEWEST_WALLPAPER = 100003;
        public final static int MESSAGECODE_COMMON_RESOURCE = 100004;
        public final static int MESSAGECODE_DOWNLOAD_COUNTS = 100005;
        public final static int MESSAGECODE_CODE_ONE_THEME = 100008;
        public final static int MESSAGECODE_LOCKSCREEN_DETAIL = 100102;
        public final static int MESSAGECODE_LOCKSCREEN_NEWEST = 100101;
    }

    public static int isWallPaperScroolEnable(Context context) {
        return IOSSettings.getBoolean(context.getContentResolver(), IOSSettings.Launcher.LAUNCHER_WALLPAPER_SCROLL_ENABLE, Partner.getBoolean(context, Partner.DEF_WALLPAPER_SCROLL_ENABLED)) ? 2 : 1;
    }
}

package com.amz.ios.launcher;

import android.content.ComponentName;

import java.util.HashMap;

/**
 * IOS Shortcuts;
 */
public class IOSShortcut {

    /**
     * Activity Action: Creates a ios shortcut.
     * it only be used in this application AndroidManifest.xml;
     */
    public static final String ACTION_CREATE_IOS_SHORTCUT = "com.ios.ioslite.CREATE_SHORTCUT";

    public static final String CLASS_NAME_THEMECLUB = "com.amz.ios.themeclub.MainActivity";
    public static final String CLASS_NAME_IOS_SETTINGS = "com.ios.iossettings.SettingsActivity";
    public static final String CLASS_NAME_WALLPAPER_SETTINGS = "com.amz.ios.launcher.shortcut.Wallpaper";
    public static final String CLASS_NAME_THEME_SETTINGS = "com.amz.ios.launcher.shortcut.Theme";
    public static final String CLASS_NAME_IOS_CLUB = "com.ios.activitiycenter.IOSClubActivity";
    public static final String CLASS_NAME_POWER_SAVE = "com.amz.ios.launcher.shortcut.BatterySave";
    public static final String CLASS_NAME_IOS_DISCOVERY = "com.amz.ios.launcher.shortcut.Discovery";
    private static final HashMap<String,String> CLASS_TO_ICON_MAP;

    static {
        CLASS_TO_ICON_MAP = new HashMap<>();

        CLASS_TO_ICON_MAP.put(CLASS_NAME_THEMECLUB,"lite_rom_themeclub");
        CLASS_TO_ICON_MAP.put(CLASS_NAME_IOS_SETTINGS,"lite_rom_ios_settings");
        CLASS_TO_ICON_MAP.put(CLASS_NAME_WALLPAPER_SETTINGS,"lite_rom_wallpaper_setting");
        CLASS_TO_ICON_MAP.put(CLASS_NAME_THEME_SETTINGS,"lite_rom_themeclub");
        CLASS_TO_ICON_MAP.put(CLASS_NAME_IOS_CLUB,"lite_rom_iosclub");
        CLASS_TO_ICON_MAP.put(CLASS_NAME_POWER_SAVE,"lite_rom_power_save");
        CLASS_TO_ICON_MAP.put(CLASS_NAME_IOS_DISCOVERY,"lite_rom_discovery");
    }

    public static String getIconResName(ComponentName cn){
        return CLASS_TO_ICON_MAP.get(cn.getClassName());
    }
}

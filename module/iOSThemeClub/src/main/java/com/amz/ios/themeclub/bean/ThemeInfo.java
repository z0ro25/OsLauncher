package com.amz.ios.themeclub.bean;

import android.graphics.Bitmap;
import android.provider.BaseColumns;

import java.io.Serializable;

/**
 * Created by lideqian on 16-11-23.
 */
public class ThemeInfo implements Comparable<ThemeInfo>, Serializable {
    public static String PREFIX_OF_THEME_NAME = "com.ios.theme.";

    public String packageName;
    public String themePath;
    public String themeType;
    public String title;
    public String font;
    public String author;
    public String description;
    public String version;
    public Bitmap thumb;
    public Bitmap iconThumb;
    public long lastUpdateTime;

    @Override
    public int compareTo(ThemeInfo another) {
        if (another.lastUpdateTime > this.lastUpdateTime) {
            return 1;
        }
        return -1;
    }

    public static class Impl implements BaseColumns{
        public static String THEME_PACKAGE_NAME = "package_name";
        public static String THEME_PATH = "package_path";
        public static String THEME_TYPE = "package_type";
        public static String THEME_TITLE = "package_title";
        public static String THEME_FONT = "package_font";
        public static String THEME_AUTHOR = "package_author";
        public static String THEME_DESCRIPTION = "package_description";
        public static String THEME_VERSION = "package_version";
        public static String THEME_THUMB = "package_thumb";
        public static String THEME_ICONTHUMB = "package_iconthumb";
        public static String THEME_LASTUPDATETIME = "package_lastupdatetime";
    }

    @Override
    public String toString() {
        return "ThemeInfo{" +
                "packageName='" + packageName + '\'' +
                ", themePath='" + themePath + '\'' +
                ", themeType='" + themeType + '\'' +
                ", title='" + title + '\'' +
                ", font='" + font + '\'' +
                ", author='" + author + '\'' +
                ", description='" + description + '\'' +
                ", version='" + version + '\'' +
                ", thumb=" + thumb +
                ", iconThumb=" + iconThumb +
                ", lastUpdateTime=" + lastUpdateTime +
                '}';
    }
}

package com.amz.ios.search.config;

import com.amz.ios.ioslite.common.BuildConfig;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-17 下午4:40
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class MSCConfiguration {

    public static final boolean DEBUG = BuildConfig.LOG_ENABLE;

    public static final int HOT_APP_SHOW_LIMIT = 8;

    public static final int DEFUALT_ITEM_SHOW_NUMBER = 3;
    /**
     * control the max number Contact  should should be show
     */
    public static final int CONTACT_SHOW_NUMBER = DEFUALT_ITEM_SHOW_NUMBER;
    public static final int FILE_SHOW_NUMBER = 20;
    public static final int MUSIC_SHOW_NUMBER = DEFUALT_ITEM_SHOW_NUMBER;

    public static final int DEFAULT_WEIGHT_MAX_ITEM_NUMBER = 4;
    public static final int DEFAULT_WEIGHT_MIN_ITEM_NUMBER = 2;

    /**
     * about 1/OVER_SCROLL_PERCENT touch distance
     */
    public static final float OVER_SCROLL_PERCENT = 3 / 2;

    public static final int OVER_SCROLL_MAX_DISTANCE = 100;//dp

    /**
     * mopub ad key
     */
    public static String AD_KEY = "ca-app-pub-8845623033099788/6206582956";

    /**
     * item type
     */
    public interface MutiItemType {
        public static final int APP_TYPE = 0x2;
        public static final int CONTACT_TYPE = 0x3;
        public static final int MUSIC_TYPE = 0x4;
        public static final int FILE_TYPE = 0x5;
        public static final int NEWS_TYPE = 0x6;
        public static final int BOTTOM_TYPE = 0x7;
        public static final int AD_TYPE = 0x10;
    }
}

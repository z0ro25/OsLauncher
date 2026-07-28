package com.amz.newspage.newssource.config;

import android.content.Context;

import com.amz.ios.newssource.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Author       : yizhihao
 * Create time  : 2016-10-13 下午3:22
 */
public class NewsConfigration {

    private static final String TAG = NewsConfigration.class.getSimpleName();

    private static String[] sCHANNEL_KEYS;
    private static Map<String, Integer> sCHANNEL_NAMES;

    /**
     * default action
     *
     * @param context
     */
    public static void setScenario(Context context) {


        //channel name 只能为
        //local, world, business, entertainment, sports
        //中的一个或者多个，若为其他值将导致奔溃
        //具体细节请查看文档
        if (sCHANNEL_KEYS == null) {
            sCHANNEL_KEYS = context.getResources().getStringArray(R.array.news_channel_name);
            sCHANNEL_NAMES = new HashMap<>();
        }
    }

    /*
     * Info hub news
      * channel name
      * start
     */
    public static String[] channelKeys() {
        return sCHANNEL_KEYS;
    }

    public static Map<String, Integer> channelNames() {
        return sCHANNEL_NAMES;
    }
}

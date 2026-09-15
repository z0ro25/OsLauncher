package com.amz.newspage.newssource;

import android.app.Activity;

import java.io.Serializable;

/**
 * infohub news manager
 * Author     : ZoeMerlin
 * Creat time : 2017-02-22 15:51
 * Contact    :
 * 562536056@qq.com   ||   yizhihao.hut@gmail.com
 */
public interface IInfoHubNewStyleSetter {

    Serializable[] getNewsStyles();

    class DefaultInfoHubNewStyleSetter implements IInfoHubNewStyleSetter {
        private Activity mActivity;

        DefaultInfoHubNewStyleSetter(Activity activity) {
            mActivity = activity;
        }
        public final Serializable[] getNewsStyles() {
//            NewsStyle newsStyle = new NewsStyle();
//            NewsAdsStyle newsAdsStyle = new NewsAdsStyle();
//            NewsViewHolderStyle newsViewHolderStyle = new NewsViewHolderStyle();
//
//            DisplayMetrics metrics = new DisplayMetrics();
//            mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
//            newsViewHolderStyle.screen_width = metrics.widthPixels;
//            newsViewHolderStyle.screen_height = metrics.heightPixels;
//
//            MainActivityStyle mainActivityStyle = new MainActivityStyle();
//            mainActivityStyle.magic_indicator_id = R.id.infohub_magic_indicator;
//            mainActivityStyle.image_blank_drawable = R.drawable.infohub_blank;
            Serializable[] styles = {};
            return styles;
        }
    }
}

package com.amz.ios.search.http;

import android.content.Context;

import com.amz.ios.search.strategy.AppRequestStrategy;
import com.amz.ios.search.utils.MD5Utils;
import com.google.gson.annotations.SerializedName;

/**
 * Created by liaozhongjun on 2017/1/16.
 */

public class AppRecommendRequestBean {

    /**
     * common : {"language":"zh_CN","requestVersion":1,"sign":"111111111...","requestTime":"12322"}
     * tag : {"channel":"koobee","customer":"osdxx9","brand":"koobee","project":"osdxx9","country":"uk"}
     * from : 0
     * to : 64
     */

    private CommonBean common;
    private TagBean tag;
    private int from;

    @SerializedName("to")
    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public CommonBean getCommon() {
        return common;
    }

    public void setCommon(CommonBean common) {
        this.common = common;
    }

    public TagBean getTag() {
        return tag;
    }

    public void setTag(TagBean tag) {
        this.tag = tag;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }


    public static final AppRecommendRequestBean newAppRecommendRequestBean(CommonBean commonBean, TagBean tagBean, AppRequestStrategy stratery) {
        AppRecommendRequestBean appRecommendRequestBean = new AppRecommendRequestBean();
        appRecommendRequestBean.setCommon(commonBean);
        appRecommendRequestBean.setTag(tagBean);
        appRecommendRequestBean.setCount(stratery.to);
        appRecommendRequestBean.setFrom(stratery.from);
        commonBean.setSign(MD5Utils.string2MD5(stratery.from + "" + appRecommendRequestBean.getCount()));
        return appRecommendRequestBean;
    }


}

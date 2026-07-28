package com.amz.ios.search.http;

import android.content.Context;


/**
 * Created by liaozhongjun on 2017/1/16.
 */

public class CommonBean {

    /**
     * language : zh_CN
     * requestVersion : 1
     * sign : 111111111...
     * requestTime : 12322
     */

    private String language;
    private int appVersion;
    private String sign;
    private String requestTime;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(int requestVersion) {
        this.appVersion = requestVersion;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(String requestTime) {
        this.requestTime = requestTime;
    }


    public static final CommonBean newCommonBean(Context context) {
        CommonBean commonBean = new CommonBean();
        commonBean.setRequestTime("" + System.currentTimeMillis());
        commonBean.setAppVersion(1);
        commonBean.setSign("");
        commonBean.setLanguage(context.getResources().getConfiguration().locale.getLanguage());
        return commonBean;
    }
}

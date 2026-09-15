package com.ezt.ios.serverswitchcontrol.bean.request;


import android.content.Context;

import com.truongnt.ios.ioslite.common.util.encrypt.MD5Util;
import com.ezt.ios.serverswitchcontrol.CommonUUID;

public class RegistRequestBean {

    /**
     * "deviceId" : "5SFAA-C47862-2115",
     * "ip"       : "192.168.30.1",
     * "location"	: {},
     * "common"   : {},
     */
    private CommonBean common;
    private LocationBean location;
    private String deviceId;
    private String ip;

    public CommonBean getCommon() {
        return common;
    }

    public void setCommon(CommonBean common) {
        this.common = common;
    }

    public LocationBean getLocation() {
        return location;
    }

    public void setLocation(LocationBean location) {
        this.location = location;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public static RegistRequestBean newAppRecommendRequestBean(Context context, CommonBean commonBean, LocationBean locationBean, String ip, String key) {
        RegistRequestBean requestBean = new RegistRequestBean();
        requestBean.setCommon(commonBean);
        requestBean.setLocation(locationBean);
        requestBean.setDeviceId(CommonUUID.getDeviceUUID(context));
        requestBean.setIp(ip);
        commonBean.setSign(MD5Util.encypt(requestBean.getDeviceId() + key));
        return requestBean;
    }


}

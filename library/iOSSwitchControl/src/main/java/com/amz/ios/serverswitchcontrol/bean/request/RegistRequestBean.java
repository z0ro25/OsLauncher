package com.amz.ios.serverswitchcontrol.bean.request;


import android.content.Context;

import com.amz.ios.ioslite.common.util.DeviceInfoUtil;
import com.amz.ios.ioslite.common.util.encrypt.MD5Util;
import com.amz.ios.serverswitchcontrol.CommonUUID;

public class RegistRequestBean {

    /**
     * "deviceId" : "5SFAA-C47862-2115",
     * "imei"  	: 868331011992179,
     * "imsi"		: 460011418603055,
     * "ip"       : "192.168.30.1",
     * "location"	: {},
     * "common"   : {},
     */
    private CommonBean common;
    private LocationBean location;
    private String deviceId;
    private String imei;
    private String imsi;
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

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getImsi() {
        return imsi;
    }

    public void setImsi(String imsi) {
        this.imsi = imsi;
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
        requestBean.setImsi(DeviceInfoUtil.getImsi(context));
        requestBean.setImei(DeviceInfoUtil.getImei(context));
        requestBean.setDeviceId(CommonUUID.getDeviceUUID(context));
        requestBean.setIp(ip);
        commonBean.setSign(MD5Util.encypt(requestBean.getDeviceId() + key));
        return requestBean;
    }


}

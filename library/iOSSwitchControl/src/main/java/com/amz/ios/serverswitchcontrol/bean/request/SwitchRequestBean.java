package com.amz.ios.serverswitchcontrol.bean.request;


import android.content.Context;

import com.amz.ios.ioslite.common.util.DeviceInfoUtil;
import com.amz.ios.ioslite.common.util.encrypt.MD5Util;
import com.amz.ios.serverswitchcontrol.CommonUUID;

public class SwitchRequestBean {

    /**
     * "deviceId" : "5SFAA-C47862-2115",
     * "imei"  	: 868331011992179,
     * "imsi"		: 460011418603055,
     * "ip"       : "192.168.30.1",
     * "location"	: {},
     * "common"   : {},
     * "tags"		: {}
     */
    private CommonBean common;
    private TagBean tags;
    private LocationBean location;
    private String deviceId;
    private String imei;
    private String imsi;

    public CommonBean getCommon() {
        return common;
    }

    public void setCommon(CommonBean common) {
        this.common = common;
    }

    public TagBean getTags() {
        return tags;
    }

    public void setTags(TagBean tags) {
        this.tags = tags;
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

    public static SwitchRequestBean newAppRecommendRequestBean(Context context, CommonBean commonBean, TagBean tagBean, LocationBean locationBean, String key) {
        SwitchRequestBean requestBean = new SwitchRequestBean();
        requestBean.setCommon(commonBean);
        requestBean.setTags(tagBean);
        requestBean.setLocation(locationBean);
        requestBean.setImsi(DeviceInfoUtil.getImsi(context));
        requestBean.setImei(DeviceInfoUtil.getImei(context));
        requestBean.setDeviceId(CommonUUID.getDeviceUUID(context));
        commonBean.setSign(MD5Util.encypt(requestBean.getDeviceId() + key));
        return requestBean;
    }


}

package com.ezt.ios.serverswitchcontrol.bean.request;


import android.content.Context;

import com.truongnt.ios.ioslite.common.util.encrypt.MD5Util;
import com.ezt.ios.serverswitchcontrol.CommonUUID;

public class SwitchRequestBean {

    /**
     * "deviceId" : "5SFAA-C47862-2115",
     * "ip"       : "192.168.30.1",
     * "location"	: {},
     * "common"   : {},
     * "tags"		: {}
     */
    private CommonBean common;
    private TagBean tags;
    private LocationBean location;
    private String deviceId;

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

    public static SwitchRequestBean newAppRecommendRequestBean(Context context, CommonBean commonBean, TagBean tagBean, LocationBean locationBean, String key) {
        SwitchRequestBean requestBean = new SwitchRequestBean();
        requestBean.setCommon(commonBean);
        requestBean.setTags(tagBean);
        requestBean.setLocation(locationBean);
        requestBean.setDeviceId(CommonUUID.getDeviceUUID(context));
        commonBean.setSign(MD5Util.encypt(requestBean.getDeviceId() + key));
        return requestBean;
    }


}

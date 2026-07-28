package com.amz.ios.serverswitchcontrol.bean.request;

import android.content.Context;
import android.os.Build;

import com.amz.ios.ioslite.common.util.DeviceInfoUtil;

public class TagBean {

    /**
     * "channel"   :"",
     * "osVersion" :"",
     * "phoneModel":"",
     * "oneKey"    :"",
     */

    private String channel;
    private String osVersion;
    private String phoneModel;
    private String oneKey;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getPhoneModel() {
        return phoneModel;
    }

    public void setPhoneModel(String phoneModel) {
        this.phoneModel = phoneModel;
    }

    public String getOneKey() {
        return oneKey;
    }

    public void setOneKey(String oneKey) {
        this.oneKey = oneKey;
    }


    public static TagBean newTagBean(Context context) {
        TagBean tagBean = new TagBean();
        tagBean.setChannel(DeviceInfoUtil.getChannel(context));
        tagBean.setOsVersion(Build.VERSION.RELEASE);
        tagBean.setPhoneModel(DeviceInfoUtil.getPhoneModel());
        tagBean.setOneKey(null);
        return tagBean;
    }

}
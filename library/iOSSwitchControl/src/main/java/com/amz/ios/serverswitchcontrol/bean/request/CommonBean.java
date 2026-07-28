package com.amz.ios.serverswitchcontrol.bean.request;

import android.content.Context;

import com.amz.ios.ioslite.common.util.BuildUtil;

public class CommonBean {

    /**
     * "requestVersion":1,
     * "sign"          :"77963B7A931377AD4AB5AD6A9CD718AA"
     */

    private int requestVersion;
    private String sign;

    public int getRequestVersion() {
        return requestVersion;
    }

    public void setRequestVersion(int requestVersion) {
        this.requestVersion = requestVersion;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public static final CommonBean newCommonBean(Context context) {
        CommonBean commonBean = new CommonBean();
        commonBean.setRequestVersion(BuildUtil.getIOSVersionCode(context));
        commonBean.setSign("");
        return commonBean;
    }
}
